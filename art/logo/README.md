# Forestry Oddities logo

`src/main/resources/forestry-oddities-logo-banner.png` — the mod's logo, declared as
`logoFile` in `META-INF/neoforge.mods.toml` and therefore shown next to the mod in the mods
screen. 785×199, transparent, pixel art at 1:1, exactly the size and format Forestry: CE's own
banner logo uses.

It is derived from Forestry: CE's banner logo (`forestry-ce-logo-banner.png`, kept here as the
source the script reads):

| Part | Source |
|---|---|
| the crowned queen bee | copied pixel for pixel from CE's logo |
| the gold "Forestry" lettering | copied pixel for pixel from CE's logo |
| the golden tree | copied pixel for pixel from CE's logo |
| the caption | re-set from `—Community Edition—` to `—Oddities—` in CE's caption style |

The caption is not hand-painted. CE's caption is 1:1 pixel art with a completely regular
structure, so `make_logo.py` reads that structure out of the CE banner and re-applies it:

* **body** — the glyph shape, in the bright gold the caption is mostly made of;
* **bevel** — a 1px pale cream highlight on the top and left edges, which only exists on glyphs
  thick enough to carry one (the `i`'s dot and stem have none, the `o`, the `t` and the dashes
  do), exactly as in CE;
* **outline** — a 1px dark brown contour;
* **underside** — a 1px orange bottom edge, and the pale cream halo around the contour that keeps
  the caption readable on dark backgrounds.

The caption font only contains the letters of "Community Edition", so the letters the mod's name
needs and it lacks are built from the letters it has: the capital `O` is the capital `C` with its
aperture closed by mirroring, the `e` is the `o` with a crossbar and an open lower right, and the
`s` is drawn in the same blocky idiom. The dashes are CE's own dash rows, stretched to frame the
shorter word so the caption still spans the width of the "Forestry" lettering.

## The square project icon

`forestry-oddities-icon.png` — 512×512, transparent, the queen bee at 4×: what Modrinth and
CurseForge want as a project icon. Both sites show the icon right next to the project's name and
render it as small as 64 px in listings, so the icon is one bold mark and carries no text of its
own; that is also how Forestry: CE's own module artwork works (its butterflies and farms icons are
square, transparent and wordless).

`forestry-oddities-icon-with-name.png` — the same bee at 3× with the `— Oddities —` caption at 2×
under her. Nicer at page size, busier in a list; use it where the icon is shown large.

## Regenerating

```powershell
python art\logo\make_logo.py                                     # -> src/main/resources/
python art\logo\make_logo.py --preview-dir out                    # + 4x zoom, dark and pale previews
python art\logo\make_logo.py --scale 2 --out big.png              # storefront artwork
python art\logo\make_logo.py --out tmp.png --icon-out icon.png    # the square project icon
```

Needs Python 3 and Pillow. `--source` points at the CE banner if it ever moves. The icon takes
`--icon-variant bee` (the default, the bee alone), `bee-name` (bee + caption) or `pair` (bee and
tree side by side, no caption), and `--icon-scale` for a larger storefront copy. Every part is an
integer multiple of the original pixel art and is scaled with nearest neighbour, so no pixel is
ever cut in half.

`forestry-oddities-logo-banner-2x.png` is the banner at 2×, for CurseForge/Modrinth artwork.

## Licensing

CE's logo art is part of Forestry: Community Edition, which is LGPL-3.0 — the same licence this mod
uses — so reusing and re-labelling it here is compatible. Keep that in mind before copying these
files into a differently licensed project.
