# -*- coding: utf-8 -*-
"""Generates the item textures: the staff and one spellbook per spell (16x16).

Output: src/main/resources/assets/symagic/textures/item/{staff,<spell>_spellbook}.png
Run: python tools/gen_textures.py

Drawn pixel by pixel. The staff follows vanilla's tool conventions (see STAFF below): a gold
setting holding an amethyst, on a wooden shaft that leans the way every vanilla tool does. A
spellbook is one book shape, its cover tinted with the colour of the
material that makes it (blaze rod = ember orange, packed ice = pale blue, ...) and marked with
its element's colour, so the ten books read apart in a hotbar while staying obviously one family.

To add a spell: one entry in SPELLS, then rerun.
"""
import os

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'symagic', 'textures', 'item')

# Per spell: the cover colour, read off its crafting material, and its element, which colours the
# emblem. Keys match Spell#id(); elements match Spell.java. The emblem is what tells the pale
# covers apart - phantom membrane and ghast tear are nearly the same beige.
SPELLS = {
    'ember': (0xE2762B, 'fire'),        # blaze rod
    'frost': (0xA8D8F0, 'frost'),       # packed ice
    'lantern': (0xB569D6, 'holy'),      # amethyst shard
    'drift': (0xD8D2C0, 'storm'),       # phantom membrane
    'bounding': (0x7BD46B, 'nature'),   # slime ball
    'blink': (0x2FA88C, 'shadow'),      # ender pearl
    'tide': (0x73B9AE, 'frost'),        # prismarine shard
    'healing': (0xEDE7DA, 'holy'),      # ghast tear
    'gust': (0xC8E4EC, 'storm'),        # breeze rod
    'echo': (0x1F6D74, 'shadow'),       # echo shard
    'recall': (0x8C9097, 'shadow'),     # compass
}

# Softer versions of the tooltip colours in Element.java.
ELEMENTS = {
    'fire': (255, 196, 64),
    'frost': (96, 214, 255),
    'storm': (255, 226, 60),
    'holy': (255, 250, 214),
    'nature': (212, 255, 120),
    'shadow': (196, 110, 255),
}

PAGE = {'mid': (239, 229, 204), 'sh': (206, 192, 160)}


def rgb(hex_colour):
    return (hex_colour >> 16) & 0xFF, (hex_colour >> 8) & 0xFF, hex_colour & 0xFF


def scale(colour, k):
    return tuple(max(0, min(255, round(c * k))) for c in colour)


def blend(a, b, t):
    return tuple(round(x + (y - x) * t) for x, y in zip(a, b))


def canvas():
    return Image.new('RGBA', (16, 16), (0, 0, 0, 0))


def put(img, x, y, colour):
    if 0 <= x < 16 and 0 <= y < 16:
        img.putpixel((x, y), colour + (255,))


# The staff, placed pixel by pixel in vanilla's conventions: handle at the bottom-left, head at the
# top-right (every vanilla tool and weapon leans that way), and a shaft built like the vanilla stick -
# a three-pixel band whose upper-left edge is a dark mid tone and whose lower-right edge is the
# darkest tone, so it reads against any background. The head is a gold setting holding an amethyst,
# which is what the staff is crafted from.
#
# STAFF below was drawn by hand in an image editor and transcribed here pixel for pixel (every colour
# is one of STAFF_PALETTE), so the script reproduces it exactly. The same picture is used in the
# inventory and in the hand. To change it: edit the PNG, then transcribe it back into STAFF - or
# edit STAFF and rerun.
STAFF_PALETTE = {
    # wood
    'a': (78, 57, 23),     # upper-left edge
    'c': (146, 108, 45),   # core, light
    'd': (110, 81, 33),    # core, mid
    'b': (38, 27, 11),     # lower-right edge, darkest
    # gold
    'O': (92, 62, 12),     # outline
    'S': (196, 136, 22),   # shadow
    'G': (236, 190, 56),   # mid
    'L': (254, 244, 138),  # light
    # amethyst
    'u': (104, 56, 170),   # shadow
    'V': (174, 108, 232),  # mid
    'v': (240, 218, 255),  # glint
}
STAFF = [
    "................",
    "...........OOO..",
    "..........OLLGO.",
    ".........OLvVVSO",
    ".........OLVVuSO",
    ".........OGVuuSO",
    ".........cOSSSO.",
    "........aSbOOO..",
    ".......adb......",
    "......acb.......",
    ".....adb........",
    "....acb.........",
    "...adb..........",
    "..acb...........",
    ".adb............",
    ".bb.............",
]


def staff(rows):
    img = canvas()
    for y, row in enumerate(rows):
        assert len(row) == 16, (y, len(row))
        for x, ch in enumerate(row):
            if ch != '.':
                put(img, x, y, STAFF_PALETTE[ch])
    return img


def spellbook(colour, element):
    """A closed book face-on: tinted cover, darker spine on the left, cream pages on the right and
    bottom edges, and a diamond in the element's colour on the cover, outlined so it shows on the
    palest covers too."""
    hi, mid, sh, dk = scale(colour, 1.15), colour, scale(colour, 0.78), scale(colour, 0.58)
    darkest = scale(colour, 0.42)
    emblem, emblem_core = ELEMENTS[element], (255, 255, 255)
    img = canvas()

    for y in range(2, 14):
        for x in range(3, 13):
            put(img, x, y, mid)
    for x in range(4, 13):
        put(img, x, 2, hi)            # top edge catches the light
        put(img, x, 13, sh)           # bottom edge
    for y in range(2, 14):
        put(img, 3, y, dk)            # spine
        put(img, 4, y, sh)
        put(img, 12, y, sh)           # right edge of the cover
    for y in (4, 11):
        put(img, 3, y, darkest)       # bands across the spine
        put(img, 4, y, dk)

    # Pages showing past the cover on the right and along the bottom.
    for y in range(3, 14):
        put(img, 13, y, PAGE['mid'])
    for x in range(5, 14):
        put(img, x, 14, PAGE['sh'])
    put(img, 13, 14, PAGE['sh'])

    # The emblem: a small diamond, brightest at its centre, ringed in the cover's darkest shade.
    for x, y in ((8, 4), (7, 5), (9, 5), (6, 6), (10, 6), (5, 7), (11, 7),
                 (6, 8), (10, 8), (7, 9), (9, 9), (8, 10)):
        put(img, x, y, darkest)
    for x, y in ((8, 5), (7, 6), (8, 6), (9, 6), (6, 7), (7, 7), (9, 7), (10, 7),
                 (7, 8), (8, 8), (9, 8), (8, 9)):
        put(img, x, y, emblem)
    put(img, 8, 7, emblem_core)
    return img


def main():
    os.makedirs(OUT, exist_ok=True)
    written = []
    path = os.path.join(OUT, 'staff.png')
    staff(STAFF).save(path)
    written.append(path)
    for spell, (colour, element) in SPELLS.items():
        path = os.path.join(OUT, spell + '_spellbook.png')
        spellbook(rgb(colour), element).save(path)
        written.append(path)
    print('wrote %d textures to %s' % (len(written), OUT))


if __name__ == '__main__':
    main()
