# RouteForge logo: prompts and process

The logo is a faceless blacksmith silhouette mid-strike, hammering a half-forged map pin on an
anvil. The art was generated in Gemini from the prompts below, then traced into vectors.

Palette (the app's logo palette, see `RouteForgeTheme.kt`): `#F6ECDD` cream (background),
`#DCC7A9` tan, `#C7AE8A` dark tan, `#4E2F1B` espresso. No other colors.

## Prompts (run in one Gemini conversation, in order)

### 1. Master prompt

```
Create a flat 2D vector-style app icon illustration, square 1:1 canvas.

BACKGROUND: a perfectly flat, solid warm cream color #F6ECDD filling the
entire square edge to edge. No gradient, no vignette, no texture, no
rounded corners, no border, no frame, no drop shadow, no ground line.

SUBJECT (all of it must fit inside the central 60% of the canvas, with
generous empty cream margin on all sides):
A chunky, cartoon-proportioned blacksmith shown as a solid SILHOUETTE with
NO face, NO eyes, NO mouth and no facial features at all. Round oversized
head, wide stocky torso, thick arms. The silhouette is dark espresso brown
#4E2F1B, filled flat with no outline. Seen in side profile on the left of
the composition, facing right. A simple apron is a band across the torso in
warm tan #DCC7A9. The blacksmith holds a large, simple sledgehammer
(straight handle, rounded-rectangle head, espresso brown) raised high behind
the head, ready to strike.

On the right of the silhouette, an anvil: simple, rounded, chunky
cartoon anvil in tan #DCC7A9 with a thin 1px-equivalent outline in darker
tan #C7AE8A.

On top of the anvil sits a map location pin (teardrop) that is HALF-FORGED:
the upper rounded part is finished, smooth and solid espresso brown
#4E2F1B with a small round hole in the center in cream #F6ECDD; the lower
pointed tip is still raw, in tan #DCC7A9 with a thin darker-tan #C7AE8A
outline, slightly rough and irregular, as if not yet hammered into shape.
The pin must be clearly readable and at least as large as the blacksmith's
head.

Two or three small simple spark shapes (tiny diamonds or dots) in espresso
#4E2F1B and darker tan #C7AE8A float in the air above the anvil.

COLORS: use ONLY these four flat colors: #F6ECDD, #DCC7A9, #C7AE8A,
#4E2F1B. No other colors, no orange, no red, no fire glow, no metallic
shine.

STYLE: clean, minimal, flat vector illustration; solid fills and crisp
sharp edges; rounded friendly shapes with exaggerated cartoon proportions;
shapes clearly separated by gaps so each one reads on its own; minimal
detail so it stays legible at 48x48 pixels. Warm, cozy, handcrafted
feeling, like a shadow-free app icon.

DO NOT INCLUDE: any text, letters, numbers, logos or watermark; faces;
gradients, shadows, glows, highlights, 3D rendering, bevels or texture;
outlines on the blacksmith silhouette; flames, fire, gears, tools other
than the hammer and anvil; a map, road or background scenery; a phone
mockup, device frame, or rounded-square app-icon container.
```

### 2. Pose: a powerful downward strike

```
Keep this exact illustration — the same flat style, the same four-color
palette (#F6ECDD, #DCC7A9, #C7AE8A, #4E2F1B), the same cream background, the
same anvil, the same half-forged location pin, and the same faceless
espresso silhouette with the tan apron — and change ONLY the blacksmith's
pose and the action:

The blacksmith is now in the middle of a powerful downward strike. His
whole body leans forward toward the anvil, weight shifted onto the front
leg, back leg stretched behind him. The sledgehammer is no longer raised
behind his head: it is swung down in a strong diagonal, and its head is
hitting the raw, rough tan tip of the location pin right at the moment of
impact. The arm is extended forward and down along the line of the swing.

Add one thin curved motion arc (a slim crescent shape in darker tan
#C7AE8A) trailing behind the hammer to show the speed and force of the
swing.

Replace the floating sparks with a small burst of 4 to 5 spark shapes
radiating outward from the point of impact, in espresso #4E2F1B and darker
tan #C7AE8A. Make the sparks slightly larger than before so they stay
readable at very small sizes.

Keep the pin at least as large as the blacksmith's head. Keep the whole
composition inside the central 60% of the square with wide cream margins.
Keep it flat: solid colors only, no gradients, no shadows, no outline on the
silhouette, no text, no face.
```

### 3. Head and apron: bare head, leather apron

Tried and rejected: flat cap (reads as a baker), Norse helmet (not the vibe), headband (reads as a
sushi chef). The bare head won.

```
Keep this exact illustration unchanged — the same pose, the same swing and
impact, the same motion arc, the same sparks, the same anvil, the same
half-forged location pin, the same flat style, the same cream background and
the same four-color palette (#F6ECDD, #DCC7A9, #C7AE8A, #4E2F1B). Do not
change the pin or the anvil in any way. Keep the hammer exactly as it is in
this image.

Change ONLY the blacksmith's head and apron:

- Remove the flat cap completely. The head is a plain, bare, perfectly round,
  featureless espresso #4E2F1B shape: no hat, no headband, no cloth, no hair,
  no helmet, no horns, no beard, no face, no accessories of any kind.
- Recolor the apron from light tan to the darker tan #C7AE8A so it reads as
  a leather smith's apron, and add a simple shoulder strap in the same color
  going up over the shoulder. Keep it a plain flat shape with no pattern.

Make the curved motion arc slightly thicker so it stays visible at very
small sizes. Keep everything inside the central 60% of the square with wide
cream margins. The silhouette stays solid flat espresso with no outline.
No text, no gradients, no shadows, no extra colors.
```

## Correction prompts (paste in the same conversation)

- **Text or watermark:** "Remove all text, letters and watermark. Keep everything else identical."
- **Gradient, shadow or glow:** "Make every area a single flat solid color. Remove all gradients, shadows, glows and highlights. Keep the composition identical."
- **Wrong color:** "Recolor strictly using only #F6ECDD, #DCC7A9, #C7AE8A and #4E2F1B. Remove any other color."
- **Detail on the silhouette:** "The blacksmith must be a solid featureless silhouette. Remove the face and all interior detail."
- **Subject too big or cropped:** "Scale the whole subject down so it occupies only the central 60% of the square, with wide cream margins."
- **Too much detail:** "Simplify. Fewer, larger shapes; remove small details so it reads clearly at 48 pixels."
- **Container or mockup:** "Remove the rounded-square container and any shadow. Output only the illustration on a flat full-bleed cream square."

## From image to app icon

The source image is `Gemini_Generated_Image_cdcckzcdcckzcdcc.jpeg` (2048x2048). To regenerate the
vector assets after replacing it (macOS, Swift toolchain, no third-party dependencies):

```sh
cd docs/logo-tools
swiftc -O vectorize.swift -o /tmp/vectorize && swiftc -O render.swift -o /tmp/render
mkdir -p /tmp/logo-out
/tmp/vectorize ../Gemini_Generated_Image_cdcckzcdcckzcdcc.jpeg /tmp/logo-out 0.7
python3 -I assemble.py /tmp/logo-out ../..      # writes docs/logo.svg + ic_launcher_foreground.xml
/tmp/render ../logo.svg /tmp/logo-preview.png 1024
```

What the tracer does: classifies each flat-color pixel into the four palette colors (edge pixels
take the nearest flat neighbor's color, which removes JPEG anti-alias fringes), traces each color
region's boundary, smooths and simplifies it (tolerance 0.7 px at 2048), and fits cubic Beziers.
Output is three layers painted back to front (tan, dark tan, espresso), each extended 1px under the
layer above so no seams show. The colors are snapped to the exact palette hex values, not the
slightly different ones Gemini produced.

The launcher foreground is scaled so the art sits inside a circle of radius 35 (of 54) around the
art's center, inside the adaptive-icon visible area. `docs/logo.svg` uses the same art, larger, on
the cream gradient in a rounded square.
