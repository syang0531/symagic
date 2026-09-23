package com.syang.symagic.client;

import com.syang.symagic.SyMagic;
import com.syang.symagic.registry.ModItems;
import com.syang.symagic.world.item.Spell;
import com.syang.symagic.world.item.StaffItem;
import com.syang.symagic.world.spell.SpellCooldowns;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.IItemDecorator;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;

import java.util.Optional;

/**
 * Draws the white cooldown sweep over a staff in the hotbar and inventory — for the <i>selected</i>
 * spell.
 *
 * <p>Vanilla draws this overlay only for a stack's own cooldown group, and the staff deliberately
 * has none (see {@code SpellCooldowns}). So this reads the selected spell's group and draws the same
 * rectangle vanilla's {@code GuiGraphicsExtractor#itemCooldown} would, in the same colour. Switch
 * spells and the sweep switches with them.
 */
@EventBusSubscriber(modid = SyMagic.MOD_ID, value = Dist.CLIENT)
public final class StaffCooldownDecorator implements IItemDecorator {

    @SubscribeEvent
    public static void register(RegisterItemDecorationsEvent event) {
        event.register(ModItems.STAFF.get(), new StaffCooldownDecorator());
    }

    @Override
    public boolean render(GuiGraphicsExtractor graphics, Font font, ItemStack stack, int x, int y) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return false;
        }

        Optional<Spell> spell = StaffItem.spells(stack).selectedSpell();
        if (spell.isEmpty()) {
            return false;
        }

        float cooldown = SpellCooldowns.percent(player, spell.get(),
                minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true));
        if (cooldown > 0.0F) {
            int top = y + Mth.floor(16.0F * (1.0F - cooldown));
            int bottom = top + Mth.ceil(16.0F * cooldown);
            graphics.fill(RenderPipelines.GUI, x, top, x + 16, bottom, Integer.MAX_VALUE);
        }
        return false;
    }
}
