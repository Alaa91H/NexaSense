"""Fetch Material Symbols (Google 2026 icon set) and write Android vector drawables.

Source: https://github.com/google/material-design-icons (symbols/web/<name>/<style>/*_24px.svg)
Each SVG is a 24dp icon designed on a 960x960 grid (y in [-960, 0]). We embed the
path data verbatim inside a <group> that scales it by 1/40 and translates it down
by 24, mapping the 960-grid onto a 24x24 viewport.
"""

import os
import re
import sys
import urllib.request

BASE = "https://raw.githubusercontent.com/google/material-design-icons/master/symbols/web"
OUT = os.path.join("presentation", "src", "main", "res", "drawable")

# (icon name, style dir, output file name)
ICONS = [
    ("arrow_back", "outlined", "ic_arrow_back"),
    ("refresh", "outlined", "ic_refresh"),
    ("restart_alt", "outlined", "ic_restart_alt"),
    ("palette", "outlined", "ic_palette"),
    ("translate", "outlined", "ic_translate"),
    ("north_east", "outlined", "ic_north_east"),
    ("mosque", "outlined", "ic_mosque"),
    ("tune", "outlined", "ic_tune"),
    ("sensors", "outlined", "ic_sensors"),
    ("compass_calibration", "outlined", "ic_compass_calibration"),
    ("location_on", "outlined", "ic_location_on"),
    ("vibration", "outlined", "ic_vibration"),
    ("volume_up", "outlined", "ic_volume_up"),
    ("screen_lock_portrait", "outlined", "ic_screen_lock_portrait"),
    ("chevron_right", "outlined", "ic_chevron_right"),
    ("explore", "outlined", "ic_explore"),
    ("straighten", "outlined", "ic_straighten"),
    ("settings", "outlined", "ic_settings"),
    # Filled variants for the bottom navigation (Google pattern: the selected
    # tab shows the filled symbol, unselected tabs the outlined one).
    ("explore", "filled", "ic_explore_filled"),
    ("straighten", "filled", "ic_straighten_filled"),
    ("settings", "filled", "ic_settings_filled"),
    # Per-row icons for the inner settings rows (Google Settings style:
    # every row has its own Material Symbol in a tonal container).
    ("power_settings_new", "outlined", "ic_power_settings_new"),
    ("navigation", "outlined", "ic_navigation"),
    ("view_agenda", "outlined", "ic_view_agenda"),
    ("route", "outlined", "ic_route"),
    ("style", "outlined", "ic_style"),
    ("label", "outlined", "ic_label"),
    ("linear_scale", "outlined", "ic_linear_scale"),
    ("pin", "outlined", "ic_pin"),
    ("info", "outlined", "ic_info"),
    ("speed", "outlined", "ic_speed"),
    ("visibility_off", "outlined", "ic_visibility_off"),
    ("notifications", "outlined", "ic_notifications"),
    ("search", "outlined", "ic_search"),
    ("close", "outlined", "ic_close"),
    # About screen rows (Google Settings "About" style).
    ("verified", "outlined", "ic_verified"),
    ("code", "outlined", "ic_code"),
    ("cloud_off", "outlined", "ic_cloud_off"),
]


def fetch(url: str) -> str:
    req = urllib.request.Request(url, headers={"User-Agent": "curl/8"})
    with urllib.request.urlopen(req, timeout=30) as resp:
        return resp.read().decode("utf-8")


def style_dir(style: str) -> str:
    # Material Symbols ship outlined/rounded/sharp as separate dirs; the
    # filled variants live in the outlined dir under a `_fill1` filename.
    return "materialsymbolsoutlined" if style == "filled" else f"materialsymbols{style}"


def svg_file(name: str, style: str) -> str:
    return f"{name}_fill1_24px.svg" if style == "filled" else f"{name}_24px.svg"


def build_drawable(paths: list[str]) -> str:
    path_tags = "".join(
        f'        <path\n            android:fillColor="#FF000000"\n            android:pathData="{p}"/>\n'
        for p in paths
    )
    return (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    android:width="24dp"\n'
        '    android:height="24dp"\n'
        '    android:viewportWidth="24"\n'
        '    android:viewportHeight="24">\n'
        "    <!-- Material Symbol (Google 2026 icon set), 24dp design on a 960 grid. -->\n"
        "    <group\n"
        '        android:scaleX="0.025"\n'
        '        android:scaleY="0.025"\n'
        '        android:translateY="24">\n'
        + path_tags +
        "    </group>\n"
        "</vector>\n"
    )


def main() -> int:
    os.makedirs(OUT, exist_ok=True)
    failures = []
    for name, style, out_name in ICONS:
        url = f"{BASE}/{name}/{style_dir(style)}/{svg_file(name, style)}"
        try:
            svg = fetch(url)
        except Exception as exc:  # noqa: BLE001 - report and continue
            failures.append(f"{name}/{style}: {exc}")
            continue
        paths = re.findall(r"<path[^>]*\bd=\"([^\"]*)\"", svg)
        if not paths:
            failures.append(f"{name}/{style}: no <path d> found")
            continue
        target = os.path.join(OUT, f"{out_name}.xml")
        with open(target, "w", encoding="utf-8", newline="\n") as fh:
            fh.write(build_drawable(paths))
        print(f"OK   {target} ({len(paths)} path(s))")
    if failures:
        print("\nFAILURES:")
        for f in failures:
            print("  -", f)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
