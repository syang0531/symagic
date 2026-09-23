package com.syang.symagic.world.item;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.syang.symagic.registry.ModItems;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.LodestoneTracker;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * What a staff is carrying: the spellbooks slotted into it, in insertion order, and which one a
 * right-click casts. This is the value of the {@code symagic:staff_spells} data component, and the
 * only state the mod keeps on a staff.
 *
 * <p>The staff holds the books themselves, the way a 26.2 bundle does — as immutable
 * {@link ItemStackTemplate}s — so a book comes out exactly as it went in: a Recall book keeps its
 * lodestone, and any book keeps a name given at an anvil.
 *
 * <p>Always normalised on construction: only spellbooks, one of each, at most {@link #CAPACITY},
 * and {@code selected} inside the list. "One of each" means one per spell — except Recall, where it
 * means one per destination, so a staff can carry three ways home. A hand-edited or corrupted stack
 * therefore cannot hold more than a staff could ever be given.
 */
public record StaffSpells(List<ItemStackTemplate> books, int selected) {

    /**
     * Spellbooks per staff. Without a limit one staff would do everything and there would be no
     * choice left to make; a fourth spell means carrying a second staff.
     */
    public static final int CAPACITY = 3;

    public static final StaffSpells EMPTY = new StaffSpells(List.of(), 0);

    /**
     * One slot on disk. A plain book is saved as its spell id ({@code "ember"}) — which is also exactly
     * how 0.2.0 saved every slot, so staffs from 0.2.0 worlds load unchanged. A book that carries
     * something (a lodestone, a name) is saved whole.
     */
    private static final Codec<ItemStackTemplate> SLOT_CODEC = Codec.either(Spell.CODEC, ItemStackTemplate.CODEC).xmap(
            either -> either.map(spell -> new ItemStackTemplate(ModItems.spellbook(spell)), book -> book),
            book -> {
                Optional<Spell> spell = SpellbookItem.spellOf(book);
                return spell.isPresent() && book.components().isEmpty()
                        ? Either.left(spell.get())
                        : Either.right(book);
            });

    public static final Codec<StaffSpells> CODEC = RecordCodecBuilder.create(i -> i.group(
            SLOT_CODEC.listOf().fieldOf("spells").forGetter(StaffSpells::books),
            Codec.INT.optionalFieldOf("selected", 0).forGetter(StaffSpells::selected)
    ).apply(i, StaffSpells::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, StaffSpells> STREAM_CODEC = StreamCodec.composite(
            ItemStackTemplate.STREAM_CODEC.apply(ByteBufCodecs.list(CAPACITY)), StaffSpells::books,
            ByteBufCodecs.VAR_INT, StaffSpells::selected,
            StaffSpells::new);

    public StaffSpells {
        List<ItemStackTemplate> kept = new ArrayList<>();
        Set<SlotKey> seen = new HashSet<>();
        for (ItemStackTemplate book : books) {
            Optional<SlotKey> key = SlotKey.of(book);
            if (key.isPresent() && kept.size() < CAPACITY && seen.add(key.get())) {
                kept.add(book.withCount(1));
            }
        }
        books = List.copyOf(kept);
        selected = books.isEmpty() ? 0 : Math.clamp(selected, 0, books.size() - 1);
    }

    public boolean isEmpty() {
        return books.isEmpty();
    }

    public int size() {
        return books.size();
    }

    /** The book a right-click casts, or empty if the staff holds none. */
    public Optional<ItemStackTemplate> selectedBook() {
        return books.isEmpty() ? Optional.empty() : Optional.of(books.get(selected));
    }

    public Optional<Spell> selectedSpell() {
        return selectedBook().flatMap(SpellbookItem::spellOf);
    }

    /** Whether {@code book} can go in: it is a spellbook, there is a free slot, and its slot is not taken. */
    public boolean canAccept(ItemStack book) {
        if (book.isEmpty() || books.size() >= CAPACITY) {
            return false;
        }
        Optional<SlotKey> key = SlotKey.of(ItemStackTemplate.fromNonEmptyStack(book));
        return key.isPresent() && books.stream().map(SlotKey::of).noneMatch(key::equals);
    }

    /** Adds one of {@code book} at the end. The selection stays put, so a new book never hijacks a cast. */
    public StaffSpells with(ItemStack book) {
        if (!canAccept(book)) {
            return this;
        }
        List<ItemStackTemplate> next = new ArrayList<>(books);
        next.add(ItemStackTemplate.fromNonEmptyStack(book.copyWithCount(1)));
        return new StaffSpells(next, selected);
    }

    /** Removes the selected book — the one the tooltip marks, so what you see is what comes out. */
    public StaffSpells withoutSelected() {
        if (books.isEmpty()) {
            return this;
        }
        List<ItemStackTemplate> next = new ArrayList<>(books);
        next.remove(selected);
        // Stay on the same position, falling back to the new last slot when the removed one was last.
        return new StaffSpells(next, Math.min(selected, next.size() - 1));
    }

    /** Selects the next book, wrapping round. */
    public StaffSpells next() {
        return books.isEmpty() ? this : new StaffSpells(books, (selected + 1) % books.size());
    }

    /**
     * What makes two books the same for the "one of each" rule: the spell, plus — for Recall only —
     * the lodestone. Names do not count; two Ember books named differently are still two Ember books.
     */
    private record SlotKey(Spell spell, Optional<GlobalPos> destination) {

        static Optional<SlotKey> of(ItemStackTemplate book) {
            return SpellbookItem.spellOf(book).map(spell -> new SlotKey(spell, spell == Spell.RECALL
                    ? Optional.ofNullable(book.get(DataComponents.LODESTONE_TRACKER)).flatMap(LodestoneTracker::target)
                    : Optional.empty()));
        }
    }
}
