package com.syang.symagic.registry;

import com.syang.symagic.SyMagic;
import com.syang.symagic.world.item.ModStaff;
import com.syang.symagic.world.item.StaffItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

/**
 * Every item in the mod: one staff per {@link ModStaff}. There is nothing else — no rune, no tome,
 * no catalyst. A staff is crafted from vanilla items, repaired with the same vanilla item, and
 * enchanted at a vanilla enchanting table.
 */
public final class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SyMagic.MOD_ID);

    public static final Map<ModStaff, DeferredItem<StaffItem>> STAFFS = new EnumMap<>(ModStaff.class);

    static {
        for (ModStaff staff : ModStaff.values()) {
            STAFFS.put(staff, ITEMS.registerItem(staff.id(), props -> new StaffItem(staff, props
                    .durability(staff.durability())
                    // Repaired at an anvil with the material it was made from.
                    .repairable(staff.material())
                    // Non-zero enchantability is what puts the staff on an enchanting table at all.
                    .enchantable(staff.enchantmentValue()))));
        }
    }

    private ModItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
