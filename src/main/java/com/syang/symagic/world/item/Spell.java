package com.syang.symagic.world.item;

import com.mojang.serialization.Codec;
import com.syang.symagic.SyMagic;
import com.syang.symagic.world.spell.SpellEffects;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.function.Supplier;

/**
 * The whole mod in one table: every spell, the vanilla material its spellbook is made from, and
 * what it does.
 *
 * <p><b>Design rule — the material is the spell.</b> A spellbook is a book plus one vanilla item,
 * and that item is chosen so the spell is guessable before you read a tooltip: a blaze rod throws
 * fire, packed ice throws frost, an ender pearl blinks, a ghast tear heals. That also means the
 * roster doubles as a progression curve, because each material lives somewhere different — amethyst
 * in a geode, prismarine in ocean ruins, a breeze rod in a trial chamber, an echo shard in an
 * ancient city. <b>Where you have been decides what you can cast</b>, and the mod never asks you to
 * farm a resource it invented.
 *
 * <p>A spell is cast from the one {@link StaffItem}, which holds up to {@link StaffSpells#CAPACITY}
 * spellbooks. Each spell has its own cooldown, so switching spells never waits on the last cast.
 *
 * <p>Balance: only two spells deal direct damage, and both are weaker than a melee weapon of the
 * same era. The rest buy mobility, vision or safety — magic is meant to complement a sword, not
 * replace it. The three enchantments in {@code ModEnchantments} scale casts.
 */
public enum Spell implements StringRepresentable {
    // id          display      material                    element        cd(t)  spell
    /** Blaze rod — a small fireball that ignites. The plain attack spell; damage is vanilla-fixed. */
    EMBER("ember", "Ember", () -> Items.BLAZE_ROD, Element.FIRE, 30, SpellEffects::firebolt),
    /** Packed ice — a fast, weak arrow that slows. The loose-mob clearer. */
    FROST("frost", "Frost", () -> Items.PACKED_ICE, Element.FROST, 20, SpellEffects::frostArrow),
    /** Amethyst shard — night vision. The first spellbook most players will own, and a dungeon staple. */
    LANTERN("lantern", "Lantern", () -> Items.AMETHYST_SHARD, Element.HOLY, 400, SpellEffects::nightVision),
    /** Phantom membrane — slow falling. Turns any drop into a shortcut. */
    DRIFT("drift", "Drift", () -> Items.PHANTOM_MEMBRANE, Element.STORM, 300, SpellEffects::slowFalling),
    /** Slime ball — jump boost, and the fall it causes cannot hurt you. */
    BOUNDING("bounding", "Bounding", () -> Items.SLIME_BALL, Element.NATURE, 300, SpellEffects::leap),
    /** Ender pearl — blink to where you are looking, without the pearl's damage or its throw arc. */
    BLINK("blink", "Blink", () -> Items.ENDER_PEARL, Element.SHADOW, 120, SpellEffects::blink),
    /** Prismarine shard — breathe and swim like the sea is yours. */
    TIDE("tide", "Tide", () -> Items.PRISMARINE_SHARD, Element.FROST, 400, SpellEffects::tide),
    /** Ghast tear — heal yourself. The brewing stand's regeneration base, held in the hand. */
    HEALING("healing", "Healing", () -> Items.GHAST_TEAR, Element.HOLY, 160, SpellEffects::heal),
    /** Breeze rod — a burst of wind that throws everything around you away. Control, not damage. */
    GUST("gust", "Gust", () -> Items.BREEZE_ROD, Element.STORM, 100, SpellEffects::gust),
    /** Echo shard — every living thing nearby glows through the walls for a while. */
    ECHO("echo", "Echo", () -> Items.ECHO_SHARD, Element.SHADOW, 400, SpellEffects::reveal);

    /** No spell may be cast faster than this, however enchanted (0.25 s). */
    public static final int MIN_COOLDOWN_TICKS = 5;

    /** Saved form: the spell's id, so a stack survives reordering this table. */
    public static final Codec<Spell> CODEC = StringRepresentable.fromEnum(Spell::values);

    /** Wire form: the ordinal. Client and server always run the same build of this table. */
    public static final StreamCodec<ByteBuf, Spell> STREAM_CODEC =
            ByteBufCodecs.idMapper(i -> values()[i], Spell::ordinal);

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
    private final SpellAction action;
    private final Identifier cooldownGroup;

    Spell(String id, String displayName, Supplier<Item> material, Element element,
          int cooldownTicks, SpellAction action) {
        this.id = id;
        this.displayName = displayName;
        this.material = material;
        this.element = element;
        this.cooldownTicks = cooldownTicks;
        this.action = action;
        this.cooldownGroup = Identifier.fromNamespaceAndPath(SyMagic.MOD_ID, "spell/" + id);
    }

    public String id() {
        return id;
    }

    /** English name, used by the data generator; Korean lives in ko_kr.json. */
    public String displayName() {
        return displayName;
    }

    /** Registry path of this spell's spellbook, e.g. {@code ember_spellbook}. */
    public String bookId() {
        return id + "_spellbook";
    }

    /** The vanilla item that, with a book, crafts this spell's spellbook — and names its spell. */
    public Item material() {
        return material.get();
    }

    public Element element() {
        return element;
    }

    public int cooldownTicks() {
        return cooldownTicks;
    }

    /**
     * The cooldown group this spell occupies, {@code symagic:spell/<id>}. Every spell has its own,
     * so the staff itself never goes on cooldown — see {@code SpellCooldowns} for why that matters.
     */
    public Identifier cooldownGroup() {
        return cooldownGroup;
    }

    /** Translation key for the spell's short name ("Ember"). */
    public String nameKey() {
        return "spell.symagic." + id;
    }

    /** Translation key for the one-line description of what the spell does. */
    public String descriptionKey() {
        return "spell.symagic." + id + ".desc";
    }

    public void cast(ServerLevel level, Player caster, ItemStack staff, float power) {
        action.cast(level, caster, staff, power);
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
