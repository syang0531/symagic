package com.syang.symagic.registry;

import com.syang.symagic.SyMagic;
import com.syang.symagic.world.item.StaffSpells;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The one data component: which spellbooks a staff carries. It is saved with the stack and synced
 * to the client, which needs it for the tooltip and the cooldown overlay.
 */
public final class ModDataComponents {

    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, SyMagic.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<StaffSpells>> STAFF_SPELLS =
            COMPONENTS.registerComponentType("staff_spells", builder -> builder
                    .persistent(StaffSpells.CODEC)
                    .networkSynchronized(StaffSpells.STREAM_CODEC)
                    .cacheEncoding());

    private ModDataComponents() {
    }

    public static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
    }
}
