package com.syang.symagic.world.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * A spellbook: a book plus one vanilla material, holding the spell that material names. On its own
 * it does nothing — it is slotted into a {@link StaffItem}, which casts it.
 *
 * <p>Not an enchanted book and deliberately not shiny: enchantments stay the enchanting table's job.
 * A spellbook has no durability and no state, so the staff stores only which spell it is.
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

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        Element element = spell.element();
        tooltip.accept(Component.literal(element.glyph() + " ")
                .append(Component.translatable(spell.descriptionKey()))
                .withStyle(element.color()));
        tooltip.accept(Component.translatable("tooltip.symagic.spellbook.cooldown",
                String.format("%.2f", spell.cooldownTicks() / 20.0F)).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.accept(Component.translatable("tooltip.symagic.spellbook.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
