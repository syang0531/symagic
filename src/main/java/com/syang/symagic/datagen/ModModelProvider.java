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
 * ({@code assets/symagic/items/*.json}, required since 1.21.4) for the staff and every spellbook.
 *
 * <p>The staff is {@code item/handheld} so it is held like a tool, with one picture for the hand and
 * the inventory alike. Spellbooks are plain {@code item/generated}, like a vanilla book. Textures come from {@code tools/gen_textures.py} at
 * {@code assets/symagic/textures/item/&lt;id&gt;.png}.
 */
public class ModModelProvider extends ModelProvider {

    public ModModelProvider(PackOutput output) {
        super(output, SyMagic.MOD_ID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        itemModels.generateFlatItem(ModItems.STAFF.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        ModItems.SPELLBOOKS.values().forEach(book ->
                itemModels.generateFlatItem(book.get(), ModelTemplates.FLAT_ITEM));
    }
}
