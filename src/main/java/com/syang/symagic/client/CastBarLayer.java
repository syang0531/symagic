package com.syang.symagic.client;

import com.syang.symagic.SyMagic;
import com.syang.symagic.world.item.Spell;
import com.syang.symagic.world.item.StaffItem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.GuiLayer;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.Optional;

/**
 * A cast bar under the crosshair while a held spell (Recall) gathers.
 *
 * <p>It is vanilla's own attack-indicator bar — the same two sprites, the same 16 x 4 size, drawn
 * the same way ({@code Hud} fills the progress sprite to a width of {@code progress * 17}) and
 * through the same crosshair pipeline, which inverts what is behind it so the bar reads on sky and
 * stone alike. It sits a few pixels under where the attack indicator would be, so the two never
 * overlap.
 */
@EventBusSubscriber(modid = SyMagic.MOD_ID, value = Dist.CLIENT)
public final class CastBarLayer implements GuiLayer {

    private static final Identifier ID = Identifier.fromNamespaceAndPath(SyMagic.MOD_ID, "cast_bar");
    private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("hud/crosshair_attack_indicator_background");
    private static final Identifier PROGRESS = Identifier.withDefaultNamespace("hud/crosshair_attack_indicator_progress");

    /** Below the attack indicator ({@code guiHeight / 2 - 7 + 16}) by a bar and a gap. */
    private static final int BELOW_ATTACK_INDICATOR = 6;

    @SubscribeEvent
    public static void register(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, ID, new CastBarLayer());
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.gui.hud.isHidden() || !player.isUsingItem()) {
            return;
        }
        ItemStack using = player.getUseItem();
        if (!(using.getItem() instanceof StaffItem)) {
            return;
        }
        Optional<Spell> held = StaffItem.spells(using).selectedSpell().filter(Spell::isHeld);
        if (held.isEmpty()) {
            return;
        }

        float elapsed = player.getTicksUsingItem() + deltaTracker.getGameTimeDeltaPartialTick(true);
        float progress = Mth.clamp(elapsed / held.get().useTicks(), 0.0F, 1.0F);

        int x = graphics.guiWidth() / 2 - 8;
        int y = graphics.guiHeight() / 2 - 7 + 16 + BELOW_ATTACK_INDICATOR;
        graphics.blitSprite(RenderPipelines.CROSSHAIR, BACKGROUND, x, y, 16, 4);
        graphics.blitSprite(RenderPipelines.CROSSHAIR, PROGRESS, 16, 4, 0, 0, x, y, Math.min(16, (int) (progress * 17.0F)), 4);
    }
}
