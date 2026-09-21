# -*- coding: utf-8 -*-
"""Generates the CurseForge project logo.

Output: docs/curseforge/logo.png and src/main/resources/logo.png (512x512; CurseForge wants
at least 400x400). Run: python tools/gen_logo.py

Drawn from primitives, like the SY Dungeon and SY Village logos. The picture is the mod in
one look: a hooked staff on the diagonal with a lit orb in its crook - the same silhouette
the item textures use, so the icon and the item in your hand read as the same object.

Composition rule: it must survive the 64px gallery thumbnail, so there are three masses
(dark field, warm diagonal, bright orb) and nothing finer than a few pixels at full size.
"""
import os

from PIL import Image, ImageDraw, ImageFilter

SS = 4
S = 512 * SS

BG_CENTER = (48, 40, 86)
BG_EDGE = (11, 9, 22)
HALO = (150, 92, 226)
WOOD_LIT = (176, 124, 70)
WOOD_DARK = (74, 47, 28)
WOOD_EDGE = (214, 168, 104)
ORB_CORE = (255, 253, 250)
ORB_RIM = (146, 82, 212)
SPARK = (230, 208, 255)

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = [os.path.join(ROOT, 'docs', 'curseforge', 'logo.png'),
       os.path.join(ROOT, 'src', 'main', 'resources', 'logo.png')]


def lerp(a, b, t):
    return tuple(round(x + (y - x) * t) for x, y in zip(a, b))


def px(v):
    """Logo-space (512) to supersampled canvas."""
    return round(v * SS)


def background():
    """Indigo, brightest behind the orb and falling off to the corners."""
    img = Image.new('RGB', (S, S), BG_EDGE)
    d = ImageDraw.Draw(img)
    cx, cy = px(150), px(168)
    r_max = px(620)
    # Concentric rings from the outside in; at SS=4 the banding is invisible after downsampling.
    for i in range(160, -1, -1):
        t = i / 160.0
        r = round(r_max * t)
        d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=lerp(BG_CENTER, BG_EDGE, t))
    return img


def halo(img):
    """The orb's light, laid under the staff so the wood sits inside it."""
    mask = Image.new('L', (S, S), 0)
    d = ImageDraw.Draw(mask)
    cx, cy, r = px(150), px(168), px(120)
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=235)
    mask = mask.filter(ImageFilter.GaussianBlur(px(46)))
    img.paste(Image.new('RGB', (S, S), HALO), (0, 0), mask)


def staff(d):
    """A tapering shaft: thicker at the head, thinner at the butt, so it reads as carved."""
    d.polygon([(px(168), px(205)), (px(206), px(186)),
               (px(404), px(452)), (px(376), px(468))], fill=WOOD_DARK)
    # Lit side, offset toward the orb.
    d.polygon([(px(172), px(203)), (px(196), px(191)),
               (px(396), px(458)), (px(380), px(466))], fill=WOOD_LIT)
    d.polygon([(px(176), px(200)), (px(188), px(194)),
               (px(390), px(459)), (px(384), px(462))], fill=WOOD_EDGE)


def crook(d):
    """The open curl that cradles the orb. Open at the lower right, where the shaft meets it."""
    box = [px(62), px(78), px(248), px(264)]
    d.arc(box, start=305, end=210, fill=WOOD_DARK, width=px(32))
    inner = [px(70), px(86), px(240), px(256)]
    d.arc(inner, start=310, end=200, fill=WOOD_LIT, width=px(13))


def orb(img):
    """Bright core, violet rim. The one thing the 64px thumbnail must keep."""
    layer = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    cx, cy, r = px(155), px(171), px(62)
    hx, hy = px(139), px(155)   # highlight sits up and to the left
    for i in range(120, -1, -1):
        t = i / 120.0
        rr = round(r * t)
        # Interpolate the centre toward the highlight as the rings shrink.
        x = round(cx + (hx - cx) * (1 - t))
        y = round(cy + (hy - cy) * (1 - t))
        d.ellipse([x - rr, y - rr, x + rr, y + rr], fill=lerp(ORB_CORE, ORB_RIM, t) + (255,))
    img.paste(layer, (0, 0), layer)


def sparks(d):
    """Thinning out along the shaft: the cast that just went off."""
    for x, y, r, a in ((250, 120, 9, 205), (286, 196, 7, 190), (96, 262, 8, 195),
                       (212, 300, 6, 175), (322, 120, 5, 160), (150, 330, 5, 165),
                       (60, 150, 6, 180), (330, 262, 4, 150)):
        d.ellipse([px(x - r), px(y - r), px(x + r), px(y + r)], fill=SPARK + (a,))


def main():
    img = background()
    halo(img)
    img = img.convert('RGBA')
    d = ImageDraw.Draw(img)
    staff(d)
    crook(d)
    orb(img)
    sparks(ImageDraw.Draw(img, 'RGBA'))

    img = img.convert('RGB').resize((512, 512), Image.LANCZOS)
    for path in OUT:
        os.makedirs(os.path.dirname(path), exist_ok=True)
        img.save(path)
        print('wrote', path)


if __name__ == '__main__':
    main()
