package com.syang.symagic.datagen;

import com.syang.symagic.SyMagic;
import com.syang.symagic.registry.ModEnchantments;
import com.syang.symagic.registry.ModItems;
import com.syang.symagic.world.item.ModStaff;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.data.ItemTagsProvider;

import java.util.concurrent.CompletableFuture;

/**
 * Puts every staff in the tags that decide what may be enchanted onto it.
 *
 * <p>Without these a modded item belongs to no {@code minecraft:enchantable/*} tag and is therefore
 * enchantable with <i>nothing</i> in survival (see {@code Enchantment#isSupportedItem}); NeoForge
 * does not fill them in for us. A staff gets three:
 *
 * <ul>
 *   <li>{@code #minecraft:enchantable/durability} — Unbreaking and Mending, which every tool wants.</li>
 *   <li>{@code #minecraft:enchantable/vanishing} — Curse of Vanishing.</li>
 *   <li>{@code #symagic:enchantable/staff} — the three staff enchantments, and only those.</li>
 * </ul>
 *
 * <p>Note what is deliberately absent: a staff is in no weapon or mining tag, so Sharpness and
 * Efficiency never appear on it. It is not a sword.
 */
public class ModItemTagsProvider extends ItemTagsProvider {

    public ModItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, SyMagic.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        for (ModStaff staff : ModStaff.values()) {
            Item item = ModItems.STAFFS.get(staff).get();
            add(ItemTags.DURABILITY_ENCHANTABLE, item);
            add(ItemTags.VANISHING_ENCHANTABLE, item);
            add(ModEnchantments.STAFF_ENCHANTABLE, item);
        }
    }

    private void add(TagKey<Item> tag, Item item) {
        tag(tag).add(key(item));
    }

    private static ResourceKey<Item> key(Item item) {
        return BuiltInRegistries.ITEM.getResourceKey(item).orElseThrow();
    }
}
