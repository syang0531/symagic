package com.syang.symagic.datagen;

import com.syang.symagic.SyMagic;
import com.syang.symagic.registry.ModItems;
import com.syang.symagic.world.item.ModStaff;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;

import java.util.concurrent.CompletableFuture;

/**
 * One crafting recipe per staff: two sticks on a diagonal shaft with the staff's material as the
 * head. Every ingredient is vanilla, so a staff is craftable the moment you find its material and
 * the mod never gates itself behind a machine of its own.
 *
 * <p>Since 1.21.4 a {@link RecipeProvider} is created per run by a {@link Runner}; the provider
 * itself only holds the registries and the output.
 */
public class ModRecipeProvider extends RecipeProvider {

    /** 'X' = the staff's material, 'S' = stick. Reads as a staff rather than a sword. */
    private static final String[] STAFF = {"  X", " S ", "S  "};

    protected ModRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        super(registries, output);
    }

    @Override
    protected void buildRecipes() {
        for (ModStaff staff : ModStaff.values()) {
            ShapedRecipeBuilder builder = ShapedRecipeBuilder.shaped(
                    this.items, RecipeCategory.TOOLS, ModItems.STAFFS.get(staff).get());
            for (String row : STAFF) {
                builder.pattern(row);
            }
            builder.define('X', staff.material());
            builder.define('S', Items.STICK);
            builder.unlockedBy("has_material", has(staff.material()));
            builder.save(this.output, key(staff.id()));
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
