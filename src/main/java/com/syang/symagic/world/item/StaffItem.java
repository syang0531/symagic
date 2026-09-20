package com.syang.symagic.world.item;

import com.syang.symagic.registry.ModEnchantments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * A staff. Right-click casts its one spell, paying a cooldown and 1 durability; that is the whole
 * item. The spell never changes, so nothing is stored on the stack and no data component is needed.
 *
 * <p>The repair material is set through {@code Item.Properties#repairable} at registration
 * (see {@code ModItems}), which since 1.21.2 is a data component rather than an item override.
 */
public class StaffItem extends Item {

    private final ModStaff staff;

    public StaffItem(ModStaff staff, Properties properties) {
        super(properties);
        this.staff = staff;
    }

    public ModStaff staff() {
        return staff;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }

        if (level instanceof ServerLevel serverLevel) {
            // Spell Power scales the cast; Alacrity shortens the cooldown; Arcane Reach is read
            // inside the individual spells, which are the only ones that know what "reach" means.
            float power = ModEnchantments.spellPower(serverLevel, stack);
            staff.cast(serverLevel, player, stack, power);

            int cooldown = Math.round(staff.cooldownTicks() * ModEnchantments.cooldown(serverLevel, stack));
            player.getCooldowns().addCooldown(stack, Math.max(ModStaff.MIN_COOLDOWN_TICKS, cooldown));
            stack.hurtAndBreak(1, player, hand);

            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.0F);
        }

        // SUCCESS swings the arm client-side; nothing more to do here.
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        Element element = staff.element();
        tooltip.accept(Component.literal(element.glyph() + " ")
                .append(Component.translatable(staff.spellKey()))
                .withStyle(element.color()));
        tooltip.accept(Component.translatable("tooltip.symagic.staff.cooldown",
                String.format("%.2f", staff.cooldownTicks() / 20.0F)).withStyle(ChatFormatting.DARK_GRAY));
    }
}
