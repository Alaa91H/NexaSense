package com.nexasense.domain.math

import com.nexasense.domain.model.EllipsoidCorrection
import com.nexasense.domain.model.MagnetometerCalibration
import com.nexasense.domain.model.Vec3
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Pure math for magnetometer calibration.
 *
 * Two tiers, both computed locally from the collected samples:
 *
 * 1. **Axis-aligned (min/max)** — hard-iron offsets from per-axis min/max and
 *    per-axis scales from the half-ranges. Simple, robust, O(1) memory; cannot
 *    correct soft-iron distortion that couples axes (e.g. rotated soft iron).
 * 2. **Full ellipsoid fit (least squares)** — fits the general quadric
 *    `m^T P m + q^T m + r = 0` to the samples (P symmetric 3x3) and extracts
 *    the hard-iron offset `b = -0.5 * P^-1 * q` and the soft-iron matrix as the
 *    Cholesky factor of P (rescaled to preserve field magnitude). This corrects
 *    soft iron in **any orientation** — including rotated coupling the
 *    axis-aligned model cannot see. Falls back to tier 1 when the fit is
 *    degenerate (too few samples, planar/linear data, non-positive-definite P).
 *
 * The residual orthogonal ambiguity inherent to ellipsoid fitting (the
 * ellipsoid alone cannot fix the full rotation) is accepted practice and
 * documented in docs/calibration.md.
 */
object MagneticCalibrationMath {

    /** Maximum number of samples retained for the ellipsoid fit. */
    private const val MAX_RETAINED_SAMPLES = 512

    /** Minimum samples for the 9-unknown least-squares fit. */
    private const val MIN_ELLIPSOID_SAMPLES = 12

    /**
     * Maximum accepted relative spread of the corrected magnitudes. A fit that
     * cannot bring the data onto a sphere (degenerate geometry) is rejected
     * and the axis-aligned fallback is used instead.
     */
    private const val MAX_RELATIVE_SPREAD = 0.35f

    /** Tracks the collected samples and derives the calibration. */
    class Sampler(
        private val minSamplesForCalibration: Int = 60,
        private val coverageThreshold: Float = 0.35f,
    ) {
        private var minX = Float.MAX_VALUE
        private var minY = Float.MAX_VALUE
        private var minZ = Float.MAX_VALUE
        private var maxX = -Float.MAX_VALUE
        private var maxY = -Float.MAX_VALUE
        private var maxZ = -Float.MAX_VALUE
        private var count = 0

        private val samples = ArrayDeque<Vec3>()

        /** Returns true if the sample was accepted (finite and non-zero). */
        fun addSample(v: Vec3): Boolean {
            if (v.isInvalid) return false
            val m = v.magnitude
            if (m <= 0f) return false
            count++
            if (v.x < minX) minX = v.x
            if (v.y < minY) minY = v.y
            if (v.z < minZ) minZ = v.z
            if (v.x > maxX) maxX = v.x
            if (v.y > maxY) maxY = v.y
            if (v.z > maxZ) maxZ = v.z
            if (samples.size >= MAX_RETAINED_SAMPLES) samples.removeFirst()
            samples.addLast(v)
            return true
        }

        val sampleCount: Int get() = count

        fun reset() {
            minX = Float.MAX_VALUE
            minY = Float.MAX_VALUE
            minZ = Float.MAX_VALUE
            maxX = -Float.MAX_VALUE
            maxY = -Float.MAX_VALUE
            maxZ = -Float.MAX_VALUE
            count = 0
            samples.clear()
        }

        fun build(): MagnetometerCalibration {
            if (count == 0) return MagnetometerCalibration.NONE

            val halfX = (maxX - minX) / 2f
            val halfY = (maxY - minY) / 2f
            val halfZ = (maxZ - minZ) / 2f
            val maxHalf = maxOf(halfX, halfY, halfZ)
            if (maxHalf <= 0f) return MagnetometerCalibration.NONE

            val scaleX = maxHalf / halfX
            val scaleY = maxHalf / halfY
            val scaleZ = maxHalf / halfZ

            val coverage = (minOf(halfX, halfY, halfZ) / maxHalf).coerceIn(0f, 1f)
            val isCalibrated = count >= minSamplesForCalibration && coverage >= coverageThreshold

            val base = MagnetometerCalibration(
                offsetX = (minX + maxX) / 2f,
                offsetY = (minY + maxY) / 2f,
                offsetZ = (minZ + maxZ) / 2f,
                scaleX = scaleX,
                scaleY = scaleY,
                scaleZ = scaleZ,
                sampleCount = count,
                coverage = coverage,
                isCalibrated = isCalibrated,
            )

            if (!isCalibrated) return base
            val fit = ellipsoidFit(samples.toList()) ?: return base
            return base.copy(ellipsoid = fit)
        }
    }

    /**
     * Least-squares ellipsoid fit (hard + soft iron).
     *
     * Fits `m^T P m + q^T m = 1` (P symmetric 3x3, q 3x1) over all samples by
     * solving the 9-unknown linear least-squares problem. To keep the algebraic
     * fit well-conditioned the data is first centered at its centroid **and
     * normalized to unit radius** (the Hartley normalization — the classic
     * remedy for the ill-conditioning of direct ellipsoid fitting). The
     * equations are additionally column-scaled before the Gaussian solve.
     *
     * From P and q:
     *  - hard-iron offset `b = -0.5 * P^-1 * q` (in the normalized frame,
     *    then mapped back with the centroid and radius)
     *  - soft-iron matrix = transpose of the Cholesky factor L of P
     *    (P = L L^T). The whitening transform is L^T — `|L^T v|^2 = v^T P v` —
     *    so the **transpose** is stored (row-major) as the correction matrix.
     *    The sign of the solution is chosen so P is positive definite, and the
     *    matrix is rescaled so the corrected magnitudes match the raw field
     *    strength.
     *
     * Returns null when the geometry is degenerate (too few samples, planar or
     * linear data, singular/non-positive-definite P) — callers fall back to the
     * axis-aligned model.
     */
    fun ellipsoidFit(samples: List<Vec3>): EllipsoidCorrection? {
        if (samples.size < MIN_ELLIPSOID_SAMPLES) return null
        for (s in samples) if (s.isInvalid) return null

        // Reject planar/linear data: the fit needs genuine 3D span. A circle in
        // a plane would otherwise admit a "cylinder" correction with an
        // arbitrary scale along the missing axis.
        var minX = Double.MAX_VALUE
        var minY = Double.MAX_VALUE
        var minZ = Double.MAX_VALUE
        var maxX = -Double.MAX_VALUE
        var maxY = -Double.MAX_VALUE
        var maxZ = -Double.MAX_VALUE
        for (s in samples) {
            if (s.x < minX) minX = s.x.toDouble()
            if (s.y < minY) minY = s.y.toDouble()
            if (s.z < minZ) minZ = s.z.toDouble()
            if (s.x > maxX) maxX = s.x.toDouble()
            if (s.y > maxY) maxY = s.y.toDouble()
            if (s.z > maxZ) maxZ = s.z.toDouble()
        }
        val halfX = (maxX - minX) / 2.0
        val halfY = (maxY - minY) / 2.0
        val halfZ = (maxZ - minZ) / 2.0
        val maxHalf = maxOf(halfX, halfY, halfZ)
        if (maxHalf <= 0.0) return null
        if (minOf(halfX, halfY, halfZ) / maxHalf < 0.1) return null

        // Centroid and normalization radius (mean distance from the centroid).
        var meanX = 0.0
        var meanY = 0.0
        var meanZ = 0.0
        for (s in samples) {
            meanX += s.x
            meanY += s.y
            meanZ += s.z
        }
        meanX /= samples.size
        meanY /= samples.size
        meanZ /= samples.size
        var radius = 0.0
        for (s in samples) {
            val dx = s.x.toDouble() - meanX
            val dy = s.y.toDouble() - meanY
            val dz = s.z.toDouble() - meanZ
            radius += sqrt(dx * dx + dy * dy + dz * dz)
        }
        radius /= samples.size
        if (!radius.isFinite() || radius <= 0.0) return null

        // Normal equations on the normalized data (unit-scale ellipsoid).
        val aTa = Array(9) { DoubleArray(9) }
        val aTOne = DoubleArray(9)
        for (s in samples) {
            val x = (s.x.toDouble() - meanX) / radius
            val y = (s.y.toDouble() - meanY) / radius
            val z = (s.z.toDouble() - meanZ) / radius
            val row = doubleArrayOf(
                x * x, y * y, z * z,
                2.0 * x * y, 2.0 * x * z, 2.0 * y * z,
                x, y, z,
            )
            for (i in 0 until 9) {
                aTOne[i] += row[i]
                for (j in 0 until 9) aTa[i][j] += row[i] * row[j]
            }
        }

        // Column scaling for conditioning, then solve (A^T A) v = A^T 1.
        val norms = DoubleArray(9) { j -> sqrt(aTa[j][j]) }
        for (j in 0 until 9) {
            if (norms[j] <= 0.0) return null
            aTOne[j] /= norms[j]
            for (i in 0 until 9) aTa[i][j] /= norms[i] * norms[j]
        }
        val w = solveLinear(aTa, aTOne) ?: return null
        val v = DoubleArray(9) { w[it] / norms[it] }
        for (value in v) if (!value.isFinite()) return null

        // P (symmetric) and q from the solution; b is sign/scale invariant.
        var p = arrayOf(
            doubleArrayOf(v[0], v[3], v[4]),
            doubleArrayOf(v[3], v[1], v[5]),
            doubleArrayOf(v[4], v[5], v[2]),
        )
        val q = doubleArrayOf(v[6], v[7], v[8])

        var l = cholesky3x3(p)
        if (l == null) {
            // The least-squares sign is arbitrary; flip and retry so P is PD
            // (handles strong hard iron, where the natural sign is negative).
            p = arrayOf(
                doubleArrayOf(-v[0], -v[3], -v[4]),
                doubleArrayOf(-v[3], -v[1], -v[5]),
                doubleArrayOf(-v[4], -v[5], -v[2]),
            )
            l = cholesky3x3(p) ?: return null
        }

        val pInv = invert3x3(p) ?: return null
        // Offset in the normalized frame, mapped back to real units.
        val offsetX = meanX + radius * -0.5 * (pInv[0][0] * q[0] + pInv[0][1] * q[1] + pInv[0][2] * q[2])
        val offsetY = meanY + radius * -0.5 * (pInv[1][0] * q[0] + pInv[1][1] * q[1] + pInv[1][2] * q[2])
        val offsetZ = meanZ + radius * -0.5 * (pInv[2][0] * q[0] + pInv[2][1] * q[1] + pInv[2][2] * q[2])

        // The whitening transform is L^T (|L^T v|^2 = v^T P v), so the
        // correction matrix is the transpose of the Cholesky factor.
        val lt = arrayOf(
            doubleArrayOf(l[0][0], l[1][0], l[2][0]),
            doubleArrayOf(l[0][1], l[1][1], l[2][1]),
            doubleArrayOf(l[0][2], l[1][2], l[2][2]),
        )

        // Rescale so corrected magnitudes match the raw field strength: the
        // normalized fit fixes no absolute scale, so normalize by the mean
        // corrected magnitude (which is ~1 for a sphere in normalized units).
        var meanCorrected = 0.0
        for (s in samples) {
            val dx = (s.x.toDouble() - offsetX) / radius
            val dy = (s.y.toDouble() - offsetY) / radius
            val dz = (s.z.toDouble() - offsetZ) / radius
            meanCorrected += correctedMagnitude(lt, dx, dy, dz)
        }
        meanCorrected /= samples.size
        if (!meanCorrected.isFinite() || meanCorrected <= 0.0) return null
        val scale = 1.0 / meanCorrected

        // Sanity: the corrected magnitudes must actually be a sphere. A fit
        // that leaves a large relative spread (planar/linear data) is rejected.
        var minMag = Double.MAX_VALUE
        var maxMag = -Double.MAX_VALUE
        for (s in samples) {
            val dx = (s.x.toDouble() - offsetX) / radius
            val dy = (s.y.toDouble() - offsetY) / radius
            val dz = (s.z.toDouble() - offsetZ) / radius
            val mag = correctedMagnitude(lt, dx, dy, dz)
            if (mag < minMag) minMag = mag
            if (mag > maxMag) maxMag = mag
        }
        val spread = (maxMag - minMag).coerceAtLeast(0.0)
        if (!spread.isFinite() || spread > MAX_RELATIVE_SPREAD) return null

        return EllipsoidCorrection(
            offsetX = offsetX.toFloat(),
            offsetY = offsetY.toFloat(),
            offsetZ = offsetZ.toFloat(),
            softIron = floatArrayOf(
                (lt[0][0] * scale).toFloat(), (lt[0][1] * scale).toFloat(), (lt[0][2] * scale).toFloat(),
                (lt[1][0] * scale).toFloat(), (lt[1][1] * scale).toFloat(), (lt[1][2] * scale).toFloat(),
                (lt[2][0] * scale).toFloat(), (lt[2][1] * scale).toFloat(), (lt[2][2] * scale).toFloat(),
            ),
        )
    }

    /** Applies the calibration to a raw reading. */
    fun apply(
        raw: Vec3,
        calibration: MagnetometerCalibration,
    ): Vec3 {
        if (!calibration.isCalibrated) return raw
        val e = calibration.ellipsoid
        if (e != null) {
            val dx = raw.x - e.offsetX
            val dy = raw.y - e.offsetY
            val dz = raw.z - e.offsetZ
            val m = e.softIron
            return Vec3(
                x = m[0] * dx + m[1] * dy + m[2] * dz,
                y = m[3] * dx + m[4] * dy + m[5] * dz,
                z = m[6] * dx + m[7] * dy + m[8] * dz,
            )
        }
        return Vec3(
            x = (raw.x - calibration.offsetX) * calibration.scaleX,
            y = (raw.y - calibration.offsetY) * calibration.scaleY,
            z = (raw.z - calibration.offsetZ) * calibration.scaleZ,
        )
    }

    /** RMS residual of the corrected samples around their mean magnitude (fit quality). */
    fun fitQuality(samples: List<Vec3>): Float {
        if (samples.isEmpty()) return 0f
        var sum = 0f
        var sumSq = 0f
        for (s in samples) {
            val m = s.magnitude
            sum += m
            sumSq += m * m
        }
        val mean = sum / samples.size
        val variance = (sumSq / samples.size) - mean * mean
        return if (variance > 0f) sqrt(variance) else 0f
    }

    private fun correctedMagnitude(l: Array<DoubleArray>, dx: Double, dy: Double, dz: Double): Double {
        val cx = l[0][0] * dx + l[0][1] * dy + l[0][2] * dz
        val cy = l[1][0] * dx + l[1][1] * dy + l[1][2] * dz
        val cz = l[2][0] * dx + l[2][1] * dy + l[2][2] * dz
        return sqrt(cx * cx + cy * cy + cz * cz)
    }

    /** Solves A x = b for a square matrix via Gauss-Jordan with partial pivoting. */
    private fun solveLinear(a: Array<DoubleArray>, b: DoubleArray): DoubleArray? {
        val n = b.size
        val m = Array(n) { i -> DoubleArray(n + 1) { j -> if (j < n) a[i][j] else b[i] } }
        for (col in 0 until n) {
            var pivotRow = col
            var pivot = abs(m[col][col])
            for (row in col + 1 until n) {
                val candidate = abs(m[row][col])
                if (candidate > pivot) {
                    pivot = candidate
                    pivotRow = row
                }
            }
            if (pivot <= 1e-12) return null
            if (pivotRow != col) {
                val tmp = m[col]
                m[col] = m[pivotRow]
                m[pivotRow] = tmp
            }
            val divisor = m[col][col]
            for (j in 0..n) m[col][j] /= divisor
            for (row in 0 until n) {
                if (row == col) continue
                val factor = m[row][col]
                if (factor == 0.0) continue
                for (j in 0..n) m[row][j] -= factor * m[col][j]
            }
        }
        return DoubleArray(n) { m[it][n] }
    }

    /** Closed-form 3x3 inverse; null when singular. */
    private fun invert3x3(a: Array<DoubleArray>): Array<DoubleArray>? {
        val a00 = a[0][0]; val a01 = a[0][1]; val a02 = a[0][2]
        val a10 = a[1][0]; val a11 = a[1][1]; val a12 = a[1][2]
        val a20 = a[2][0]; val a21 = a[2][1]; val a22 = a[2][2]

        val det = a00 * (a11 * a22 - a12 * a21) -
            a01 * (a10 * a22 - a12 * a20) +
            a02 * (a10 * a21 - a11 * a20)
        if (abs(det) <= 1e-12 || !det.isFinite()) return null

        val invDet = 1.0 / det
        return arrayOf(
            doubleArrayOf(
                (a11 * a22 - a12 * a21) * invDet,
                (a02 * a21 - a01 * a22) * invDet,
                (a01 * a12 - a02 * a11) * invDet,
            ),
            doubleArrayOf(
                (a12 * a20 - a10 * a22) * invDet,
                (a00 * a22 - a02 * a20) * invDet,
                (a02 * a10 - a00 * a12) * invDet,
            ),
            doubleArrayOf(
                (a10 * a21 - a11 * a20) * invDet,
                (a01 * a20 - a00 * a21) * invDet,
                (a00 * a11 - a01 * a10) * invDet,
            ),
        )
    }

    /** Lower-triangular Cholesky factor (P = L L^T); null when not positive definite. */
    private fun cholesky3x3(p: Array<DoubleArray>): Array<DoubleArray>? {
        val l = Array(3) { DoubleArray(3) }
        l[0][0] = sqrt(p[0][0])
        if (!l[0][0].isFinite() || l[0][0] <= 1e-12) return null
        l[1][0] = p[1][0] / l[0][0]
        l[2][0] = p[2][0] / l[0][0]

        val diag11 = p[1][1] - l[1][0] * l[1][0]
        l[1][1] = sqrt(diag11)
        if (!l[1][1].isFinite() || l[1][1] <= 1e-12) return null
        l[2][1] = (p[2][1] - l[2][0] * l[1][0]) / l[1][1]

        val diag22 = p[2][2] - l[2][0] * l[2][0] - l[2][1] * l[2][1]
        l[2][2] = sqrt(diag22)
        if (!l[2][2].isFinite() || l[2][2] <= 1e-12) return null
        return l
    }
}
