package com.syang.symagic.registry;

import com.syang.symagic.SyMagic;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * The three staff enchantments. They are datapack enchantments
 * ({@code data/symagic/enchantment/*.json}) whose gameplay is applied here in code, because
 * "spell power" has no vanilla effect component to hang off.
 *
 * <p><b>They are obtained the vanilla way and only the vanilla way.</b> The staff carries an
 * enchantability value and sits in {@code #symagic:enchantable/staff}, and the three enchantments
 * are added to {@code #minecraft:non_treasure} — the tag vanilla folds into
 * {@code in_enchanting_table}, {@code tradeable} and {@code on_random_loot}. So they turn up at an
 * enchanting table, in a librarian's trades and in chest loot, exactly like Sharpness, and this mod
 * adds no way to craft them. That is the point: the enchanting table, villagers and loot chests
 * keep their jobs.
 */
public final class ModEnchantments {

    /** Item tag that gates every staff enchantment (populated in {@code ModItemTagsProvider}). */
    public static final TagKey<Item> STAFF_ENCHANTABLE =
            ItemTags.create(Identifier.fromNamespaceAndPath(SyMagic.MOD_ID, "enchantable/staff"));

    /** +15% spell damage / heal / effect duration per level (I–V). Sharpness, for magic. */
    public static final ResourceKey<Enchantment> SPELL_POWER = key("spell_power");

    /** −10% cooldown per level (I–IV), floored at −60%. */
    public static final ResourceKey<Enchantment> ALACRITY = key("alacrity");

    /** +20% area radius, projectile speed and blink range per level (I–III). */
    public static final ResourceKey<Enchantment> ARCANE_REACH = key("arcane_reach");

    private ModEnchantments() {
    }

    private static ResourceKey<Enchantment> key(String path) {
        return ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(SyMagic.MOD_ID, path));
    }

    /** Level of {@code key} on {@code stack}, or 0 if absent. Resolves the datapack enchantment holder. */
    public static int level(ServerLevel level, ItemStack stack, ResourceKey<Enchantment> key) {
        Holder<Enchantment> holder = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
        return EnchantmentHelper.getItemEnchantmentLevel(holder, stack);
    }

    /** Damage / heal / duration multiplier: 1.0 + 0.15·level. */
    public static float spellPower(ServerLevel level, ItemStack stack) {
        return 1.0F + 0.15F * level(level, stack, SPELL_POWER);
    }

    /** Cooldown multiplier: 1.0 − 0.10·level, floored at 0.40 (−60% cap). */
    public static float cooldown(ServerLevel level, ItemStack stack) {
        return Math.max(0.40F, 1.0F - 0.10F * level(level, stack, ALACRITY));
    }

    /** Area / projectile / range multiplier: 1.0 + 0.20·level. */
    public static float reach(ServerLevel level, ItemStack stack) {
        return 1.0F + 0.20F * level(level, stack, ARCANE_REACH);
    }
}
