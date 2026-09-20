package com.syang.symagic.world.item;

import net.minecraft.ChatFormatting;

/**
 * The six elements. With one spell per staff an element is pure flavour — it colours the tooltip
 * and groups the staffs in the creative tab — but it keeps the roster legible: you can tell at a
 * glance that the blaze-rod staff and the ghast-tear staff do very different things.
 */
public enum Element {
    FIRE("Fire", "🔥", ChatFormatting.RED),
    FROST("Frost", "❄", ChatFormatting.AQUA),
    STORM("Storm", "⚡", ChatFormatting.YELLOW),
    HOLY("Holy", "✨", ChatFormatting.WHITE),
    NATURE("Nature", "☘", ChatFormatting.GREEN),
    SHADOW("Shadow", "🌑", ChatFormatting.DARK_PURPLE);

    private final String displayName;
    private final String glyph;
    private final ChatFormatting color;

    Element(String displayName, String glyph, ChatFormatting color) {
        this.displayName = displayName;
        this.glyph = glyph;
        this.color = color;
    }

    public String displayName() {
        return displayName;
    }

    /** A short emoji glyph for tooltip flavour. */
    public String glyph() {
        return glyph;
    }

    /** Themed text colour used in tooltips. */
    public ChatFormatting color() {
        return color;
    }
}
