package com.syang.symagic;

import com.syang.symagic.registry.ModCreativeTabs;
import com.syang.symagic.registry.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SY Magic — main mod entrypoint.
 *
 * <p>One staff, one spell. A staff is crafted from two sticks and a single vanilla material whose
 * theme <i>is</i> the spell (a blaze rod throws fire, an ender pearl blinks), so there is nothing
 * to bind, select or unlock: craft it and right-click. The mod adds no machine, no currency and no
 * recipe type — every material is vanilla, and the three staff enchantments are ordinary
 * enchantments you find at an enchanting table, in a librarian's trades or in chest loot.
 *
 * <p>Minecraft 26.2 / NeoForge 26.2 (Java 25). Depends on no other mod.
 */
@Mod(SyMagic.MOD_ID)
public class SyMagic {

    public static final String MOD_ID = "symagic";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public SyMagic(IEventBus modBus, ModContainer container) {
        ModItems.register(modBus);
        ModCreativeTabs.register(modBus);

        LOGGER.info("SY Magic loaded.");
    }
}
