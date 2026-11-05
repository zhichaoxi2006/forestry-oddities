#!/usr/bin/env python3
"""Build the "Forestry Oddities" banner logo from the Forestry: CE logo.

The Forestry: CE logo (`forestry-ce-logo-banner.png`, 785x199) is a hand-drawn
pixel-art wordmark: a crowned queen bee on the left, the gold "Forestry"
lettering in the middle, a golden tree on the right, and a small gold caption
("--Community Edition--") under the lettering.

This script derives this mod's logo from it. The bee, the "Forestry" wordmark
and the tree are copied pixel for pixel, so the two logos belong to the same
family; the caption is re-set to "Oddities" in the very style the CE caption
uses. That style is 1:1 pixel art and fully describable, so the caption is not
hand-painted:

    body    the letter shape, filled with the caption's vertical gold gradient
    bevel   a 1px pale-cream rim on the top/left edges, the embossed highlight.
            It only exists on glyphs thick enough to carry one, exactly like
            CE: the i's dot and stem have none while the o, the t and the
            dashes all do.
    outline a 1px dark-brown contour around the body
    halo    a 1px pale-cream halo around the contour, which is what keeps the
            caption readable on dark backgrounds

The CE caption font only contains the letters of "Community Edition", so the
"Oddities" letters that it does not contain are built from the ones that are
there: the capital O is the capital C with its aperture closed, the e is the o
with a crossbar and an open lower right, and the s is drawn in the same blocky
idiom. Every glyph is then painted by the one routine above, so the whole word
shares a single shading model.

Usage:
    python make_logo.py                      # write the mod's logo into src/
    python make_logo.py --preview-dir out    # plus 4x zoom and dark preview
"""

from __future__ import annotations

import argparse
import os
from collections import deque

from PIL import Image

# --- the CE caption palette -------------------------------------------------
CREAM = (255, 252, 185)  # halo + embossed highlight
DARK = (91, 41, 3)       # contour
YELLOW = (247, 212, 47)  # the body, i.e. the lit face of the gold
ORANGE = (218, 129, 0)   # the body's shaded underside

# --- the parts of the CE banner that are copied verbatim -------------------
CANVAS = (785, 199)
BEE_BOX = (8, 0, 128, 199)
# The wordmark is cropped above the caption band: the CE caption lives inside
# the wordmark's horizontal span and is replaced by this mod's own.
TITLE_BOX = (142, 0, 668, 149)
TREE_BOX = (679, 0, 776, 199)

# The CE caption's content box, and the two dashes that flank it.
CAPTION_BAND = (140, 150, 668, 199)
LEFT_DASH_BOX = (144, 173, 251, 178)   # body only, without contour and halo
RIGHT_DASH_BOX = (556, 173, 663, 178)
CAPTION_CENTER = 404                    # CE centres the caption under the title
BASELINE = 186                          # last body row of the caption

# Body bounding boxes (x0, y0, x1, y1 - exclusive) of the CE caption letters
# that "Oddities" reuses.
GLYPH_BOXES = {
    "C": (257, 159, 280, 187),
    "d": (471, 158, 487, 187),
    "t": (497, 161, 506, 187),
    "i": (491, 158, 494, 187),   # dot and stem, the gap between them included
    "o": (515, 165, 531, 187),
}

# The i is two disconnected pieces: the dot above and the stem below.
I_DOT_ROWS = (158, 164)      # y0 <= y < y1
I_STEM_ROWS = (165, 187)

# Caption layout. The number after a glyph is the body gap that follows it:
# the CE caption sets its letters 4px apart, and 3px after a t, whose crossbar
# is what reaches into the gap.
CAPTION = [
    ("O", 4),
    ("d", 4),
    ("d", 4),
    ("i", 4),
    ("t", 3),
    ("i", 4),
    ("e", 4),
    ("s", 0),
]

# The dashes are stretched so the caption still spans the width of the
# "Forestry" lettering, the way CE's caption does. CE sets its dashes right up
# against the caption's first and last letter; a slightly wider gap keeps the
# shorter word readable at the sizes the logo is shown in.
DASH_GAP = 10


def snap(color):
    """Snap an RGB colour to the nearest tone of the caption palette."""
    tones = [YELLOW, ORANGE, CREAM, DARK]
    return min(tones, key=lambda t: sum((a - b) ** 2 for a, b in zip(t, color)))


# --- masks -----------------------------------------------------------------
Mask = set  # {(x, y), ...}


def dilate(mask: Mask, r: int) -> Mask:
    out = set(mask)
    for _ in range(r):
        grown = set(out)
        for x, y in out:
            for dx in (-1, 0, 1):
                for dy in (-1, 0, 1):
                    grown.add((x + dx, y + dy))
        out = grown
    return out


def erode(mask: Mask, r: int) -> Mask:
    out = set(mask)
    for _ in range(r):
        shrunk = set()
        for x, y in out:
            if all((x + dx, y + dy) in out
                   for dx in (-1, 0, 1) for dy in (-1, 0, 1)):
                shrunk.add((x, y))
        out = shrunk
    return out


def is_thick(shape: Mask) -> bool:
    """Whether a shape is thick enough to carry the embossed highlight.

    The CE highlight is the inner edge of a bevel, so it disappears on strokes
    only a couple of pixels thick: the i's dot and stem have none, while the o,
    the d and the dashes all do. Eroding by 2 tells the two apart.
    """
    return bool(erode(shape, 2))


def paint(shape: Mask, thick: bool | None = None):
    """Paint one shape with the CE caption treatment, in canvas coordinates.

    The caption's gold is lit from the top left: the edge a stroke turns to the
    light is pale cream, its underside is orange, and the rest is the bright
    yellow that most of the caption is made of.
    """
    if thick is None:
        thick = is_thick(shape)
    outline = dilate(shape, 1) - shape
    halo = dilate(shape, 2) - dilate(shape, 1)
    out = {}
    for (x, y) in shape:
        top_edge = (x, y - 1) not in shape
        bottom_edge = (x, y + 1) not in shape
        left_edge = (x - 1, y) not in shape
        if thick and top_edge:
            out[(x, y)] = CREAM
        elif bottom_edge:
            out[(x, y)] = ORANGE
        elif thick and left_edge:
            out[(x, y)] = CREAM
        else:
            out[(x, y)] = YELLOW
    for p in outline:
        out[p] = DARK
    for p in halo:
        out[p] = CREAM
    return out


def round_corners(shape: Mask, r: int) -> Mask:
    """Chamfer the four corners of a shape, the way the caption rounds them."""
    w = max(x for x, _ in shape) + 1
    h = max(y for _, y in shape) + 1
    out = set()
    for (x, y) in shape:
        if (x + y < r) or ((w - 1 - x) + y < r):
            continue
        if (x + (h - 1 - y) < r) or ((w - 1 - x) + (h - 1 - y) < r):
            continue
        out.add((x, y))
    return out


# --- reading the CE caption ------------------------------------------------
def read_caption_bodies(img: Image.Image) -> dict:
    """Split the CE caption into a body/contour/halo classification.

    The caption is layered outwards from the body as dark contour then pale
    cream halo, so the halo can be flooded from the transparent background
    (crossing cream only) and the contour can be flooded from the halo
    (crossing dark only). What is left is a letter body -- its gold fill plus
    the cream highlight the bevel puts along the body's own edges.
    """
    x0, y0, x1, y1 = CAPTION_BAND
    px = img.load()
    ink = {}
    for y in range(y0, y1):
        for x in range(x0, x1):
            p = px[x, y]
            if p[3]:
                ink[(x, y)] = p[:3]

    def flood(start, colour):
        seen = set()
        found = set()
        q = deque(start)
        while q:
            c = q.popleft()
            if c in seen:
                continue
            seen.add(c)
            x, y = c
            for n in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
                if not (x0 <= n[0] < x1 and y0 <= n[1] < y1) or n in seen:
                    continue
                if ink.get(n) == colour:
                    found.add(n)
                    q.append(n)
        return found

    empty = {(x, y) for y in range(y0, y1) for x in range(x0, x1) if (x, y) not in ink}
    halo = flood(empty, CREAM)
    contour = flood(halo, DARK)
    return {p: c for p, c in ink.items() if p not in halo and p not in contour}


def glyph_shapes(img: Image.Image) -> dict:
    """Body silhouettes of the CE letters, in local (glyph box) coordinates."""
    body = read_caption_bodies(img)
    shapes = {}
    for key, (bx0, by0, bx1, by1) in GLYPH_BOXES.items():
        shapes[key] = {(x - bx0, y - by0)
                       for (x, y) in body if bx0 <= x < bx1 and by0 <= y < by1}
    return shapes


def box_size(key):
    bx0, by0, bx1, by1 = GLYPH_BOXES[key]
    return bx1 - bx0, by1 - by0


# --- the letters the CE caption does not contain ---------------------------
def make_O(c: Mask) -> Mask:
    """The capital O: the capital C with its aperture closed.

    The C's fill is an open ring, so uniting it with its own horizontal mirror
    closes the aperture while keeping the C's stroke weight, corner radii and
    height -- which is what an O of the same font looks like.
    """
    w = max(x for x, _ in c) + 1
    return set(c) | {(w - 1 - x, y) for (x, y) in c}


def make_e(o: Mask) -> Mask:
    """The lowercase e: the o with a crossbar and an open lower right.

    At this size the caption's outline is two pixels deep on every edge, so a
    glyph can only carry one counter: the e is a bowl with a crossbar and the
    whole lower right of the ring cut away, which is how a bold pixel font
    draws an e.
    """
    w = max(x for x, _ in o) + 1
    h = max(y for _, y in o) + 1
    bar_top = h // 2 - 3
    bar_bottom = bar_top + 2
    out = set(o)
    for y in range(bar_top, bar_bottom + 1):
        out |= {(x, y) for x in range(w)}
    cut = range(bar_bottom + 1, h - 5)
    return round_corners({(x, y) for (x, y) in out
                          if not (y in cut and x >= w - 5)}, 2)


def make_s(w: int, h: int) -> Mask:
    """The lowercase s, drawn in the caption's blocky idiom.

    Five strokes: a full-width top bar and bottom bar, a short upper-left
    stroke, the middle bar, and a short lower-right stroke.
    """
    stroke = 4
    out = set()
    for y0, y1 in [(0, stroke), (h // 2 - 2, h // 2 + 2), (h - stroke, h)]:
        for y in range(y0, y1):
            out |= {(x, y) for x in range(w)}
    for y in range(stroke, h // 2 - 1):
        out |= {(x, y) for x in range(stroke + 1)}
    for y in range(h // 2 + 2, h - stroke):
        out |= {(x, y) for x in range(w - stroke - 1, w)}
    return round_corners(out, 3)


def glyph_pieces(shapes: dict) -> dict:
    """Every caption glyph as (pieces, width, height).

    A piece is a body silhouette in glyph-local coordinates; most glyphs have
    one, the i has two (dot and stem).
    """
    a, b = I_DOT_ROWS
    c, d = I_STEM_ROWS
    bx0, by0 = GLYPH_BOXES["i"][:2]
    i_shape = shapes["i"]
    dot = {(x, y) for (x, y) in i_shape if a - by0 <= y < b - by0}
    stem = {(x, y) for (x, y) in i_shape if c - by0 <= y < d - by0}

    w_o, h_o = box_size("o")
    w_c, h_c = box_size("C")
    return {
        "O": ([make_O(shapes["C"])], w_c, h_c),
        "d": ([shapes["d"]], *box_size("d")),
        "t": ([shapes["t"]], *box_size("t")),
        "i": ([dot, stem], *box_size("i")),
        "e": ([make_e(shapes["o"])], w_o, h_o),
        "s": ([make_s(w_o, h_o)], w_o, h_o),
    }


def build_caption(img: Image.Image):
    """Lay out and paint the new caption; return (pixels, left, right)."""
    pieces = glyph_pieces(glyph_shapes(img))

    total = sum(pieces[k][1] for k, _ in CAPTION) + sum(g for _, g in CAPTION)
    left = CAPTION_CENTER - total // 2
    x = left
    ink = {}
    for key, gap in CAPTION:
        shapes, w, h = pieces[key]
        top = BASELINE - h + 1
        for shape in shapes:
            if not shape:
                continue
            placed = {(x + lx, top + ly) for (lx, ly) in shape}
            ink.update(paint(placed))
        x += w + gap
    return ink, left, left + total


def dash_profile(img: Image.Image) -> list:
    """The CE dash's pixel rows, top to bottom, as (y, colour).

    The dash is a flat bar with square ends, so stretching it is a matter of
    repeating one column of it; its exact shading is read from the source so
    the stretched dashes stay pixel-identical to CE's.
    """
    px = img.load()
    sample_x = 200
    rows = []
    for y in range(165, 185):
        p = px[sample_x, y]
        if p[3]:
            rows.append((y, p[:3]))
    return rows


def dash(x_from: int, x_to: int, profile) -> dict:
    return {(x, y): colour for x in range(x_from, x_to) for y, colour in profile}


def build(source: str, out: str, preview_dir: str | None, scale: int = 1):
    img = Image.open(source).convert("RGBA")
    canvas = Image.new("RGBA", CANVAS, (0, 0, 0, 0))

    # 1. The three parts of the CE banner that are copied verbatim.
    for box in (BEE_BOX, TITLE_BOX, TREE_BOX):
        canvas.paste(img.crop(box), box[:2])

    # 2. The new caption: "-- Oddities --", in the CE caption's own style.
    ink, word_left, word_right = build_caption(img)
    profile = dash_profile(img)
    ink.update(dash(LEFT_DASH_BOX[0], word_left - DASH_GAP, profile))
    ink.update(dash(word_right + DASH_GAP, RIGHT_DASH_BOX[2], profile))

    px = canvas.load()
    for (x, y), colour in ink.items():
        if 0 <= x < CANVAS[0] and 0 <= y < CANVAS[1]:
            px[x, y] = (*colour, 255)

    # A pixel-art logo only scales in whole steps; nearest neighbour keeps every
    # pixel square.
    if scale > 1:
        canvas = canvas.resize((canvas.width * scale, canvas.height * scale), Image.NEAREST)

    os.makedirs(os.path.dirname(os.path.abspath(out)), exist_ok=True)
    canvas.save(out)
    print(f"wrote {out} ({canvas.width}x{canvas.height})")

    if preview_dir:
        os.makedirs(preview_dir, exist_ok=True)
        band = canvas.crop((140, 145, 670, 199))
        band.resize((band.width * 4, band.height * 4), Image.NEAREST).save(
            os.path.join(preview_dir, "caption_4x.png"))
        for name, bg in (("on_dark", (24, 24, 28, 255)), ("on_pale", (245, 245, 245, 255))):
            plate = Image.new("RGBA", CANVAS, bg)
            plate.alpha_composite(canvas)
            plate.save(os.path.join(preview_dir, f"{name}.png"))
        print(f"wrote previews into {preview_dir}")


# --- the square project icon ----------------------------------------------
# Modrinth and CurseForge show a square icon next to the project, under the
# project's own name, and Forestry: CE's module artwork is a transparent square
# with the art filling most of it. The default icon follows that: the queen bee,
# big, and nothing else. `bee-name` adds the caption under her, which reads as a
# logo at page size but turns to mush in a 64px list.
ICON_SIZE = 512
ICON_DASH = 24   # dash length in caption pixels, so the caption reads as a line
ICON_VARIANTS = {
    "bee": dict(elements=["bee"], scale=4, gap_x=0, gap_y=0, caption=None),
    "bee-name": dict(elements=["bee"], scale=3, gap_x=0, gap_y=18, caption=2),
    "pair": dict(elements=["bee", "tree"], scale=2, gap_x=28, gap_y=20, caption=2),
}


def crop_ink(img: Image.Image, box) -> Image.Image:
    """The part of `box` that has pixels in it."""
    region = img.crop(box)
    return region.crop(region.getbbox())


def ink_to_image(ink) -> Image.Image:
    xs = [x for x, _ in ink]
    ys = [y for _, y in ink]
    x0, x1 = min(xs), max(xs) + 1
    y0, y1 = min(ys), max(ys) + 1
    im = Image.new("RGBA", (x1 - x0, y1 - y0), (0, 0, 0, 0))
    px = im.load()
    for (x, y), colour in ink.items():
        px[x - x0, y - y0] = (*colour, 255)
    return im


def icon_caption(img: Image.Image) -> Image.Image:
    """The caption for the icon: the same "-- Oddities --" line, shorter dashes."""
    ink, left, right = build_caption(img)
    profile = dash_profile(img)
    ink.update(dash(left - DASH_GAP - ICON_DASH, left - DASH_GAP, profile))
    ink.update(dash(right + DASH_GAP, right + DASH_GAP + ICON_DASH, profile))
    return ink_to_image(ink)


def build_icon(source: str, out: str, variant: str = "bee", scale: int = 1):
    img = Image.open(source).convert("RGBA")
    spec = ICON_VARIANTS[variant]
    el_scale, gap_x, gap_y, cap_scale = spec["scale"], spec["gap_x"], spec["gap_y"], spec["caption"]
    boxes = {"bee": BEE_BOX, "tree": TREE_BOX}

    art = []
    for name in spec["elements"]:
        tile = crop_ink(img, boxes[name])
        art.append(tile.resize((tile.width * el_scale, tile.height * el_scale),
                               Image.NEAREST))
    caption = None
    if cap_scale:
        caption = icon_caption(img)
        caption = caption.resize((caption.width * cap_scale, caption.height * cap_scale),
                                 Image.NEAREST)

    art_w = sum(t.width for t in art) + gap_x * (len(art) - 1)
    art_h = max(t.height for t in art)
    width = max(art_w, caption.width if caption else 0)
    height = art_h + (gap_y + caption.height if caption else 0)
    if width > ICON_SIZE or height > ICON_SIZE:
        raise SystemExit(f"icon content {width}x{height} does not fit {ICON_SIZE}px")

    canvas = Image.new("RGBA", (ICON_SIZE, ICON_SIZE), (0, 0, 0, 0))
    x = (ICON_SIZE - art_w) // 2
    y = (ICON_SIZE - height) // 2
    for tile in art:
        canvas.alpha_composite(tile, (x, y + (art_h - tile.height) // 2))
        x += tile.width + gap_x
    if caption:
        canvas.alpha_composite(caption, ((ICON_SIZE - caption.width) // 2,
                                         y + art_h + gap_y))

    if scale > 1:
        canvas = canvas.resize((canvas.width * scale, canvas.height * scale), Image.NEAREST)

    os.makedirs(os.path.dirname(os.path.abspath(out)), exist_ok=True)
    canvas.save(out)
    print(f"wrote {out} ({canvas.width}x{canvas.height}, {width}x{height} of content)")


def main():
    here = os.path.dirname(os.path.abspath(__file__))
    repo = os.path.dirname(os.path.dirname(here))
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--source", default=os.path.join(here, "forestry-ce-logo-banner.png"),
                    help="the Forestry: CE banner logo the caption style comes from")
    ap.add_argument("--out", default=os.path.join(
        repo, "src", "main", "resources", "forestry-oddities-logo-banner.png"),
        help="the logo to write")
    ap.add_argument("--preview-dir", default=None,
                    help="also write zoomed/preview images here")
    ap.add_argument("--scale", type=int, default=1,
                    help="whole-number scale factor, for storefront artwork")
    ap.add_argument("--icon-out", default=None,
                    help="also write the square project icon here")
    ap.add_argument("--icon-variant", default="bee", choices=sorted(ICON_VARIANTS),
                    help="which elements the square icon shows")
    ap.add_argument("--icon-scale", type=int, default=1,
                    help="whole-number scale factor for the icon")
    args = ap.parse_args()
    build(args.source, args.out, args.preview_dir, args.scale)
    if args.icon_out:
        build_icon(args.source, args.icon_out, args.icon_variant, args.icon_scale)


if __name__ == "__main__":
    main()
