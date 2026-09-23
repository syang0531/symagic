package com.syang.symagic.world.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * A spellbook: a book plus one vanilla material, holding the spell that material names. On its own
 * it does nothing — it is slotted into a {@link StaffItem}, which casts it.
 *
 * <p>Not an enchanted book and deliberately not shiny: enchantments stay the enchanting table's job.
 * Most spellbooks carry no state; the Recall book ({@link RecallSpellbookItem}) carries a lodestone.
 * Either way the staff keeps the whole book, so what goes in comes out.
 */
public class SpellbookItem extends Item {

    private final Spell spell;

    public SpellbookItem(Spell spell, Properties properties) {
        super(properties);
        this.spell = spell;
    }

    public Spell spell() {
        return spell;
    }

    public static Optional<Spell> spellOf(ItemStackTemplate book) {
        return book.item().value() instanceof SpellbookItem item ? Optional.of(item.spell()) : Optional.empty();
    }

    public static Optional<Spell> spellOf(ItemStack book) {
        return book.getItem() instanceof SpellbookItem item ? Optional.of(item.spell()) : Optional.empty();
    }

    /**
     * How a slotted book is named on the staff: an anvil name if it has one (that is how a Recall book's
     * destination gets its name), otherwise the spell's own name. Always led by the element glyph.
     */
    public static MutableComponent slotName(ItemStackTemplate book) {
        Spell spell = spellOf(book).orElseThrow();
        Component custom = book.get(DataComponents.CUSTOM_NAME);
        return Component.literal(spell.element().glyph() + " ")
                .append(custom != null ? custom : Component.translatable(spell.nameKey()));
    }

    /** Extra lines under the spell's description, for a book that has something to say (Recall). */
    protected void appendBookDetails(ItemStackTemplate book, Consumer<Component> tooltip) {
    }

    /** {@link #appendBookDetails} for any book, whichever spellbook it is. */
    public static void appendDetails(ItemStackTemplate book, Consumer<Component> tooltip) {
        if (book.item().value() instanceof SpellbookItem item) {
            item.appendBookDetails(book, tooltip);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        Element element = spell.element();
        tooltip.accept(Component.literal(element.glyph() + " ")
                .append(Component.translatable(spell.descriptionKey()))
                .withStyle(element.color()));
        appendBookDetails(ItemStackTemplate.fromNonEmptyStack(stack), tooltip);
        tooltip.accept(Component.translatable("tooltip.symagic.spellbook.cooldown",
                String.format("%.2f", spell.cooldownTicks() / 20.0F)).withStyle(ChatFormatting.DARK_GRAY));
        if (spell.isHeld()) {
            tooltip.accept(Component.translatable("tooltip.symagic.spellbook.hold",
                    String.format("%.0f", spell.useTicks() / 20.0F)).withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.accept(Component.translatable("tooltip.symagic.spellbook.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
