package com.syang.symagic.world.item;

import com.syang.symagic.SyMagic;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;

/**
 * Keeps spellbooks from disappearing when two staffs are combined.
 *
 * <p>An anvil may merge two staffs (durability and enchantments), and it consumes the right-hand one
 * — along with any spellbooks slotted into it. So the anvil refuses while the right-hand staff still
 * holds a book: take them out first, then combine. The left-hand staff keeps its books as usual.
 *
 * <p>The other two ways vanilla combines damaged tools, the crafting-grid repair and the grindstone,
 * are switched off for the staff altogether ({@code Item.Properties#setNoCombineRepair} in
 * {@code ModItems}); the grid one would wipe the books from <i>both</i> staffs.
 */
@EventBusSubscriber(modid = SyMagic.MOD_ID)
public final class StaffEvents {

    private StaffEvents() {
    }

    /** Fired on both sides; cancelling empties the result slot and zeroes the cost. */
    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack sacrificed = event.getRight();
        if (sacrificed.getItem() instanceof StaffItem && !StaffItem.spells(sacrificed).isEmpty()) {
            event.setCanceled(true);
        }
    }
}
