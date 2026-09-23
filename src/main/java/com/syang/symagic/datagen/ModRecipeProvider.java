package com.syang.symagic.datagen;

import com.syang.symagic.SyMagic;
import com.syang.symagic.registry.ModItems;
import com.syang.symagic.world.item.Spell;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;

import java.util.concurrent.CompletableFuture;

/**
 * Twelve ordinary crafting recipes, every ingredient vanilla:
 *
 * <ul>
 *   <li><b>The staff</b> — two sticks on a diagonal shaft; at its head two gold ingots cradle an
 *       amethyst shard in the corner, a gold setting holding a gem, which is what the texture shows.
 *       Gold is the most enchantable vanilla material, and the staff is the thing you enchant.
 *       Sticks and one gold ingot on that diagonal are the vanilla golden spear.</li>
 *   <li><b>A spellbook</b> — a book and the spell's material, shapeless. The material is still the
 *       spell, so the roster is still a map of where you have been.</li>
 * </ul>
 *
 * <p>No recipe type of our own: slotting a book into a staff happens in the inventory, bundle-style,
 * not at a crafting grid.
 *
 * <p>Since 1.21.4 a {@link RecipeProvider} is created per run by a {@link Runner}; the provider
 * itself only holds the registries and the output.
 */
public class ModRecipeProvider extends RecipeProvider {

    /** 'A' = amethyst shard, 'G' = gold ingot, 'S' = stick. Checked against every vanilla shaped recipe. */
    private static final String[] STAFF = {" GA", " SG", "S  "};

    protected ModRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        super(registries, output);
    }

    @Override
    protected void buildRecipes() {
        ShapedRecipeBuilder staff = ShapedRecipeBuilder.shaped(this.items, RecipeCategory.TOOLS, ModItems.STAFF.get());
        for (String row : STAFF) {
            staff.pattern(row);
        }
        staff.define('A', Items.AMETHYST_SHARD)
                .define('G', Items.GOLD_INGOT)
                .define('S', Items.STICK)
                .unlockedBy("has_gold_ingot", has(Items.GOLD_INGOT))
                .save(this.output, key("staff"));

        for (Spell spell : Spell.values()) {
            ShapelessRecipeBuilder.shapeless(this.items, RecipeCategory.TOOLS, ModItems.spellbook(spell))
                    .requires(Items.BOOK)
                    .requires(spell.material())
                    .unlockedBy("has_material", has(spell.material()))
                    .save(this.output, key(spell.bookId()));
        }
    }

    private static ResourceKey<Recipe<?>> key(String path) {
        return ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(SyMagic.MOD_ID, path));
    }

    /** What the data generator actually instantiates; it hands each run a fresh provider. */
    public static final class Runner extends RecipeProvider.Runner {

        public Runner(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
            return new ModRecipeProvider(registries, output);
        }

        @Override
        public String getName() {
            return "SyMagic recipes";
        }
    }
}
