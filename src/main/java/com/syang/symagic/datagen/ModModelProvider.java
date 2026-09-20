package com.syang.symagic.datagen;

import com.syang.symagic.SyMagic;
import com.syang.symagic.registry.ModItems;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.data.PackOutput;

/**
 * Generates the item models and the client item definitions
 * ({@code assets/symagic/items/*.json}, required since 1.21.4) for every staff.
 *
 * <p>Staffs are {@code item/handheld} so they are held like a tool. Their textures are produced by
 * {@code tools/gen_textures.ps1} at {@code assets/symagic/textures/item/&lt;id&gt;.png}.
 */
public class ModModelProvider extends ModelProvider {

    public ModModelProvider(PackOutput output) {
        super(output, SyMagic.MOD_ID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        ModItems.STAFFS.values().forEach(i ->
                itemModels.generateFlatItem(i.get(), ModelTemplates.FLAT_HANDHELD_ITEM));
    }
}
