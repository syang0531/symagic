package com.syang.symagic.world.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

/**
 * What a staff is carrying: the spells of the spellbooks slotted into it, in insertion order, and
 * which one a right-click casts. This is the value of the {@code symagic:staff_spells} data
 * component, and the only state the mod keeps on a stack.
 *
 * <p>The staff stores <i>spells</i>, not the book stacks themselves. A spellbook has no state of its
 * own (no durability, no enchantments), so taking one out simply makes a fresh book of that spell.
 *
 * <p>Always normalised on construction: at most {@link #CAPACITY} spells, no spell twice, and
 * {@code selected} inside the list. A hand-edited or corrupted stack therefore cannot hold more
 * than a staff could ever be given.
 */
public record StaffSpells(List<Spell> spells, int selected) {

    /**
     * Spellbooks per staff. Without a limit one staff would do everything and there would be no
     * choice left to make; a fourth spell means carrying a second staff.
     */
    public static final int CAPACITY = 3;

    public static final StaffSpells EMPTY = new StaffSpells(List.of(), 0);

    public static final Codec<StaffSpells> CODEC = RecordCodecBuilder.create(i -> i.group(
            Spell.CODEC.listOf().fieldOf("spells").forGetter(StaffSpells::spells),
            Codec.INT.optionalFieldOf("selected", 0).forGetter(StaffSpells::selected)
    ).apply(i, StaffSpells::new));

    public static final StreamCodec<ByteBuf, StaffSpells> STREAM_CODEC = StreamCodec.composite(
            Spell.STREAM_CODEC.apply(ByteBufCodecs.list(CAPACITY)), StaffSpells::spells,
            ByteBufCodecs.VAR_INT, StaffSpells::selected,
            StaffSpells::new);

    public StaffSpells {
        List<Spell> distinct = new ArrayList<>(new LinkedHashSet<>(spells));
        spells = List.copyOf(distinct.subList(0, Math.min(distinct.size(), CAPACITY)));
        selected = spells.isEmpty() ? 0 : Math.clamp(selected, 0, spells.size() - 1);
    }

    public boolean isEmpty() {
        return spells.isEmpty();
    }

    public int size() {
        return spells.size();
    }

    /** The spell a right-click casts, or empty if no spellbook is slotted. */
    public Optional<Spell> selectedSpell() {
        return spells.isEmpty() ? Optional.empty() : Optional.of(spells.get(selected));
    }

    /** Whether a spellbook of {@code spell} can go in: there is a free slot and it is not already here. */
    public boolean canAccept(Spell spell) {
        return spells.size() < CAPACITY && !spells.contains(spell);
    }

    /** Adds {@code spell} at the end. The selection stays where it was, so a new book never hijacks a cast. */
    public StaffSpells with(Spell spell) {
        if (!canAccept(spell)) {
            return this;
        }
        List<Spell> next = new ArrayList<>(spells);
        next.add(spell);
        return new StaffSpells(next, selected);
    }

    /** Removes the selected spell — the one the tooltip marks, so what you see is what comes out. */
    public StaffSpells withoutSelected() {
        if (spells.isEmpty()) {
            return this;
        }
        List<Spell> next = new ArrayList<>(spells);
        next.remove(selected);
        // Stay on the same position, falling back to the new last slot when the removed one was last.
        return new StaffSpells(next, Math.min(selected, next.size() - 1));
    }

    /** Selects the next spell, wrapping round. */
    public StaffSpells next() {
        return spells.isEmpty() ? this : new StaffSpells(spells, (selected + 1) % spells.size());
    }
}
