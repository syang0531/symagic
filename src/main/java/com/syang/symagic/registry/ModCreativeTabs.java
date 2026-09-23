package com.syang.symagic.registry;

import com.syang.symagic.SyMagic;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** One tab: the staff, then the spellbooks in the roster's own order (attack, support, movement). */
public final class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SyMagic.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.symagic.main"))
                    .icon(() -> new ItemStack(ModItems.STAFF.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.STAFF.get());
                        ModItems.SPELLBOOKS.values().forEach(book -> output.accept(book.get()));
                    })
                    .build());

    private ModCreativeTabs() {
    }

    public static void register(IEventBus modBus) {
        TABS.register(modBus);
    }
}
