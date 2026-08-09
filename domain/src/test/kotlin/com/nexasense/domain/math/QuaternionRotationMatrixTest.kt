package com.nexasense.domain.math

import com.nexasense.domain.engine.OrientationEngine
import com.nexasense.domain.model.Vec3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class QuaternionRotationMatrixTest {

    private val eps = 1e-2f

    @Test
    fun `identity quaternion produces identity matrix and north heading`() {
        val matrix = Quaternion.IDENTITY.toRotationMatrix()
        assertEquals(0f, OrientationEngine.azimuthDegrees(matrix), eps)
        assertEquals(1f, matrix[0, 0], eps)
        assertEquals(1f, matrix[1, 1], eps)
        assertEquals(1f, matrix[2, 2], eps)
    }

    @Test
    fun `yaw 90 degrees maps to heading 90`() {
        val heading = OrientationEngine.azimuthDegrees(Quaternion.fromYawDegrees(90f).toRotationMatrix())
        assertEquals(90f, heading, eps)
    }

    @Test
    fun `yaw 180 degrees maps to heading 180`() {
        val heading = OrientationEngine.azimuthDegrees(Quaternion.fromYawDegrees(180f).toRotationMatrix())
        assertEquals(180f, heading, eps)
    }

    @Test
    fun `yaw 270 degrees maps to heading 270`() {
        val heading = OrientationEngine.azimuthDegrees(Quaternion.fromYawDegrees(270f).toRotationMatrix())
        assertEquals(270f, heading, eps)
    }

    @Test
    fun `yaw 359_9 degrees stays near 360`() {
        val heading = OrientationEngine.azimuthDegrees(Quaternion.fromYawDegrees(359.9f).toRotationMatrix())
        assertEquals(359.9f, heading, 0.1f)
    }

    @Test
    fun `negative yaw maps to positive heading`() {
        val heading = OrientationEngine.azimuthDegrees(Quaternion.fromYawDegrees(-45f).toRotationMatrix())
        assertEquals(315f, heading, eps)
    }

    @Test
    fun `rotation vector reading matches yaw quaternion`() {
        // Heading 90° (top to East) is -90° about +Z: RV = (0,0,-sin45°).
        val heading = OrientationEngine.azimuthDegrees(
            Quaternion.fromRotationVector(0f, 0f, -0.70710677f).toRotationMatrix(),
        )
        assertEquals(90f, heading, eps)
    }

    @Test
    fun `rotation matrix maps device axes to world axes`() {
        val matrix = Quaternion.fromYawDegrees(90f).toRotationMatrix()
        // Device +Y (top) points East at heading 90°.
        val deviceTop = matrix.rotate(Vec3(0f, 1f, 0f))
        assertEquals(1f, deviceTop.x, eps)
        assertEquals(0f, deviceTop.y, eps)
        assertEquals(0f, deviceTop.z, eps)
        // Device +X (right) points South.
        val deviceRight = matrix.rotate(Vec3(1f, 0f, 0f))
        assertEquals(0f, deviceRight.x, eps)
        assertEquals(-1f, deviceRight.y, eps)
        assertEquals(0f, deviceRight.z, eps)
    }

    @Test
    fun `quaternion normalization preserves direction`() {
        val q = Quaternion(2f, 0f, 0f, 0f)
        val normalized = q.normalized()
        assertEquals(1f, sqrt(normalized.w * normalized.w), eps)
        assertTrue(normalized.w > 0f)
    }

    @Test
    fun `fromRotationVector derives w and normalizes`() {
        val q = Quaternion.fromRotationVector(0f, 0f, 0.70710677f)
        assertEquals(0.70710677f, q.w, eps)
        val length = sqrt(q.w * q.w + q.x * q.x + q.y * q.y + q.z * q.z)
        assertEquals(1f, length, eps)
    }

    @Test
    fun `rotation matrix is orthonormal`() {
        val matrix = Quaternion.fromYawDegrees(37f).toRotationMatrix()
        val row0 = Vec3(matrix[0, 0], matrix[0, 1], matrix[0, 2])
        val row1 = Vec3(matrix[1, 0], matrix[1, 1], matrix[1, 2])
        val row2 = Vec3(matrix[2, 0], matrix[2, 1], matrix[2, 2])
        assertEquals(1f, row0.magnitude, eps)
        assertEquals(1f, row1.magnitude, eps)
        assertEquals(1f, row2.magnitude, eps)
        assertEquals(0f, row0.dot(row1), eps)
        assertEquals(0f, row0.dot(row2), eps)
        assertEquals(0f, row1.dot(row2), eps)
    }
}
