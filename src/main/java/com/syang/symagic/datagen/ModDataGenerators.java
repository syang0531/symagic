package com.syang.symagic.datagen;

import com.syang.symagic.SyMagic;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * Hooks the data providers to {@code ./gradlew runData}. Output lands in
 * src/generated/resources, which build.gradle adds as a resource root.
 *
 * <p>NeoForge runs client <b>and</b> server providers in the single {@code clientData} run
 * (there is no separate server run configured), so everything is added on the Client event.
 */
@EventBusSubscriber(modid = SyMagic.MOD_ID)
public final class ModDataGenerators {

    private ModDataGenerators() {
    }

    @SubscribeEvent
    public static void onGatherData(GatherDataEvent.Client event) {
        event.createProvider(ModModelProvider::new);
        event.createProvider(ModEnglishLangProvider::new);
        event.createProvider(ModRecipeProvider.Runner::new);
        event.createProvider(ModItemTagsProvider::new);
    }
}
