package com.syang.symagic.registry;

import com.syang.symagic.SyMagic;
import com.syang.symagic.world.item.ModStaff;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** One tab holding the staffs, in the roster's own order (attack first, then support, then movement). */
public final class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SyMagic.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.symagic.main"))
                    .icon(() -> new ItemStack(ModItems.STAFFS.get(ModStaff.LANTERN).get()))
                    .displayItems((params, output) -> ModItems.STAFFS.values().forEach(i -> output.accept(i.get())))
                    .build());

    private ModCreativeTabs() {
    }

    public static void register(IEventBus modBus) {
        TABS.register(modBus);
    }
}
