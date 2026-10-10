#!/usr/bin/env python3
"""Assemble docs/logo.svg and the Android adaptive-icon foreground from vectorize.swift output.
Usage: assemble.py <vectorize-out-dir> <repo-root> [svg-only-dir]"""
import re
import sys
from pathlib import Path

out = Path(sys.argv[1])
repo = Path(sys.argv[2])
report = (out / "report.txt").read_text()
cx, cy = (float(v) for v in re.search(r"center (\S+) (\S+)", report).groups())
r_max = float(re.search(r"rMax (\S+)", report).group(1))
b = [float(v) for v in re.search(r"bounds (\S+) (\S+) (\S+) (\S+)", report).groups()]
art_w = b[2] - b[0]

layers = [
    ("tanLight", "#DCC7A9"),
    ("tanDark", "#C7AE8A"),
    ("espresso", "#4E2F1B"),
]
paths = {n: (out / f"{n}.path").read_text() for n, _ in layers}

# --- docs/logo.svg: squircle on the brand gradient, art fitted with margin
logo_scale = 84.0 / art_w
svg_paths = "\n".join(
    f'      <path fill="{col}" fill-rule="evenodd" d="{paths[n]}"/>' for n, col in layers
)
svg = f"""<svg xmlns="http://www.w3.org/2000/svg" width="128" height="128" viewBox="0 0 108 108">
  <defs>
    <linearGradient id="bg" x1="0%" y1="0%" x2="0%" y2="100%">
      <stop offset="0%" stop-color="#F6ECDD"/>
      <stop offset="100%" stop-color="#EEDFC8"/>
    </linearGradient>
    <clipPath id="mask"><rect x="0" y="0" width="108" height="108" rx="24"/></clipPath>
  </defs>
  <g clip-path="url(#mask)">
    <rect x="0" y="0" width="108" height="108" fill="url(#bg)"/>
    <g transform="translate(54,54) scale({logo_scale:.5f}) translate({-cx:.1f},{-cy:.1f})">
{svg_paths}
    </g>
  </g>
</svg>
"""

# --- adaptive-icon foreground: keep the art inside the launcher's safe circle
SAFE_RADIUS = 35.0  # of 54; the guaranteed-visible circle is 33, the max-visible one 36
fg_scale = SAFE_RADIUS / r_max
vd_paths = "\n".join(
    f'''        <path
            android:fillColor="{col}"
            android:fillType="evenOdd"
            android:pathData="{paths[n]}" />'''
    for n, col in layers
)
vd = f"""<!-- RouteForge app icon foreground: a faceless blacksmith silhouette mid-strike, hammering a
     half-forged map pin on an anvil. Traced from docs/ source art into three flat layers
     (painted back to front, each extended 1px under the one above so no seams show), using
     the app's logo palette. The art is scaled to sit inside the adaptive-icon safe circle. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <group
        android:translateX="54"
        android:translateY="54"
        android:scaleX="{fg_scale:.5f}"
        android:scaleY="{fg_scale:.5f}">
        <group
            android:translateX="{-cx:.1f}"
            android:translateY="{-cy:.1f}">
{vd_paths}
        </group>
    </group>
</vector>
"""

target = Path(sys.argv[3]) if len(sys.argv) > 3 else None
if target:
    target.mkdir(parents=True, exist_ok=True)
    (target / "logo.svg").write_text(svg)
    (target / "ic_launcher_foreground.xml").write_text(vd)
else:
    (repo / "docs" / "logo.svg").write_text(svg)
    (repo / "app/src/main/res/drawable/ic_launcher_foreground.xml").write_text(vd)
print(f"logo scale {logo_scale:.5f}, foreground scale {fg_scale:.5f}, art width {art_w * fg_scale:.1f}/108")
