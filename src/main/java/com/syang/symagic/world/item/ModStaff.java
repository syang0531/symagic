package com.syang.symagic.world.item;

import com.syang.symagic.world.spell.SpellEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.function.Supplier;

/**
 * The whole mod in one table: every staff, its crafting material, and the single spell it casts.
 *
 * <p><b>Design rule — the material is the spell.</b> A staff is two sticks plus one vanilla item,
 * and that item is chosen so the spell is guessable before you read a tooltip: a blaze rod throws
 * fire, packed ice throws frost, an ender pearl blinks, a ghast tear heals. That also means the
 * roster doubles as a progression curve, because each material lives somewhere different — amethyst
 * in a geode, prismarine in ocean ruins, a breeze rod in a trial chamber, an echo shard in an
 * ancient city. <b>Where you have been decides what you can cast</b>, and the mod never asks you to
 * farm a resource it invented.
 *
 * <p>There is no binding, no spell slot and no selection: the staff casts on right-click, pays
 * {@code cooldown} ticks and 1 durability, and is repaired at an anvil with its own material.
 *
 * <p>Balance: only two staffs deal direct damage, and both are weaker than a melee weapon of the
 * same era. The rest buy mobility, vision or safety — magic is meant to complement a sword, not
 * replace it. The three enchantments in {@code ModEnchantments} scale casts.
 */
public enum ModStaff {
    // id              display          ko-note        material                    element  cd(t) dur ench  spell
    /** Blaze rod — a small fireball that ignites. The plain attack staff; damage is vanilla-fixed. */
    EMBER("ember_staff", "Ember Staff", () -> Items.BLAZE_ROD, Element.FIRE, 30, 200, 12, SpellEffects::firebolt),
    /** Packed ice — a fast, weak arrow that slows. The loose-mob clearer. */
    FROST("frost_staff", "Frost Staff", () -> Items.PACKED_ICE, Element.FROST, 20, 180, 12, SpellEffects::frostArrow),
    /** Amethyst shard — night vision. The first staff most players will own, and a dungeon staple. */
    LANTERN("lantern_staff", "Lantern Staff", () -> Items.AMETHYST_SHARD, Element.HOLY, 400, 250, 22, SpellEffects::nightVision),
    /** Phantom membrane — slow falling. Turns any drop into a shortcut. */
    DRIFT("drift_staff", "Drift Staff", () -> Items.PHANTOM_MEMBRANE, Element.STORM, 300, 200, 14, SpellEffects::slowFalling),
    /** Slime ball — jump boost, and the fall it causes cannot hurt you. */
    BOUND("bounding_staff", "Bounding Staff", () -> Items.SLIME_BALL, Element.NATURE, 300, 160, 12, SpellEffects::leap),
    /** Ender pearl — blink to where you are looking, without the pearl's damage or its throw arc. */
    BLINK("blink_staff", "Blink Staff", () -> Items.ENDER_PEARL, Element.SHADOW, 120, 200, 16, SpellEffects::blink),
    /** Prismarine shard — breathe and swim like the sea is yours. */
    TIDE("tide_staff", "Tide Staff", () -> Items.PRISMARINE_SHARD, Element.FROST, 400, 240, 14, SpellEffects::tide),
    /** Ghast tear — heal yourself. The brewing stand's regeneration base, held in the hand. */
    HEALING("healing_staff", "Healing Staff", () -> Items.GHAST_TEAR, Element.HOLY, 160, 160, 18, SpellEffects::heal),
    /** Breeze rod — a burst of wind that throws everything around you away. Control, not damage. */
    GUST("gust_staff", "Gust Staff", () -> Items.BREEZE_ROD, Element.STORM, 100, 240, 14, SpellEffects::gust),
    /** Echo shard — every living thing nearby glows through the walls for a while. */
    ECHO("echo_staff", "Echo Staff", () -> Items.ECHO_SHARD, Element.SHADOW, 400, 300, 18, SpellEffects::reveal);

    /** No staff may cast faster than this, however enchanted (0.25 s). */
    public static final int MIN_COOLDOWN_TICKS = 5;

    /** The behaviour of a spell — invoked server-side with an already-computed power multiplier. */
    @FunctionalInterface
    public interface SpellAction {
        void cast(ServerLevel level, Player caster, ItemStack staff, float power);
    }

    private final String id;
    private final String displayName;
    private final Supplier<Item> material;
    private final Element element;
    private final int cooldownTicks;
    private final int durability;
    private final int enchantmentValue;
    private final SpellAction action;

    ModStaff(String id, String displayName, Supplier<Item> material, Element element,
             int cooldownTicks, int durability, int enchantmentValue, SpellAction action) {
        this.id = id;
        this.displayName = displayName;
        this.material = material;
        this.element = element;
        this.cooldownTicks = cooldownTicks;
        this.durability = durability;
        this.enchantmentValue = enchantmentValue;
        this.action = action;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    /** The vanilla item that crafts and repairs this staff — and names its spell. */
    public Item material() {
        return material.get();
    }

    public Element element() {
        return element;
    }

    public int cooldownTicks() {
        return cooldownTicks;
    }

    public int durability() {
        return durability;
    }

    public int enchantmentValue() {
        return enchantmentValue;
    }

    /** Translation key for the one-line spell description shown in the tooltip. */
    public String spellKey() {
        return "tooltip.symagic." + id + ".spell";
    }

    public void cast(ServerLevel level, Player caster, ItemStack staff, float power) {
        action.cast(level, caster, staff, power);
    }
}
