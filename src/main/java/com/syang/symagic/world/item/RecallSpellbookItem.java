package com.syang.symagic.world.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * The Recall spellbook — this mod's answer to a portal network, built out of what vanilla already has.
 *
 * <ul>
 *   <li>The waypoint is a <b>lodestone</b>. Use the book on one to bind it, exactly as a compass
 *       binds (same sound); use it on another to rebind.</li>
 *   <li>The binding is vanilla's own {@code minecraft:lodestone_tracker} component, the one a lodestone
 *       compass carries — dimension and position. No component of our own.</li>
 *   <li>The name is the book's <b>anvil name</b>; the staff shows it on the slot.</li>
 *   <li>The list is the staff's three slots. Casting is in {@code SpellEffects#recall}.</li>
 * </ul>
 *
 * <p>Only a loose book binds. A book in a staff cannot be pointed at a lodestone — using the staff
 * on a lodestone simply casts.
 */
public class RecallSpellbookItem extends SpellbookItem {

    public RecallSpellbookItem(Properties properties) {
        super(Spell.RECALL, properties);
    }

    /** Where {@code book} leads, if it is bound: the dimension and the lodestone's position. */
    public static Optional<GlobalPos> destination(ItemStackTemplate book) {
        LodestoneTracker tracker = book.get(DataComponents.LODESTONE_TRACKER);
        return tracker == null ? Optional.empty() : tracker.target();
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!level.getBlockState(pos).is(Blocks.LODESTONE)) {
            return super.useOn(context);
        }

        // A spellbook stacks to one, so the book in hand is always the one to bind — no splitting.
        context.getItemInHand().set(DataComponents.LODESTONE_TRACKER,
                new LodestoneTracker(Optional.of(GlobalPos.of(level.dimension(), pos)), true));
        level.playSound(null, pos, SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.PLAYERS, 1.0F, 1.0F);
        Player player = context.getPlayer();
        if (player != null && !level.isClientSide()) {
            player.sendOverlayMessage(Component.translatable("message.symagic.recall.bound").withStyle(ChatFormatting.GRAY));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void appendBookDetails(ItemStackTemplate book, Consumer<Component> tooltip) {
        tooltip.accept(destinationLine(book));
    }

    /** "→ 120, 64, -30 · Overworld", or how to bind an unbound book. */
    public static Component destinationLine(ItemStackTemplate book) {
        return destination(book)
                .<Component>map(target -> {
                    BlockPos pos = target.pos();
                    Identifier dimension = target.dimension().identifier();
                    return Component.translatable("tooltip.symagic.recall.destination", pos.getX(), pos.getY(), pos.getZ(),
                                    Component.translatableWithFallback("symagic.dimension." + dimension.getPath(), dimension.toString()))
                            .withStyle(ChatFormatting.GRAY);
                })
                .orElseGet(() -> Component.translatable("tooltip.symagic.recall.unbound").withStyle(ChatFormatting.GRAY));
    }
}
