package com.syang.symagic.datagen;

import com.syang.symagic.SyMagic;
import com.syang.symagic.registry.ModItems;
import com.syang.symagic.world.item.ModStaff;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

import java.util.EnumMap;
import java.util.Map;

/**
 * Generates assets/symagic/lang/en_us.json. (Korean ko_kr.json is hand-written.)
 *
 * <p>Every staff needs a one-line description of its spell, because with one spell per staff the
 * tooltip is the only place the rules are written down.
 */
public class ModEnglishLangProvider extends LanguageProvider {

    /** The one line of tooltip that tells you what a staff does. */
    private static final Map<ModStaff, String> SPELLS = new EnumMap<>(ModStaff.class);

    static {
        SPELLS.put(ModStaff.EMBER, "Throws a small fireball that sets its target alight");
        SPELLS.put(ModStaff.FROST, "Fires a quick arrow that slows its target");
        SPELLS.put(ModStaff.LANTERN, "Night Vision for 60 seconds");
        SPELLS.put(ModStaff.DRIFT, "Slow Falling for 45 seconds");
        SPELLS.put(ModStaff.BOUND, "Jump Boost II for 45 seconds, and the landing cannot hurt you");
        SPELLS.put(ModStaff.BLINK, "Step to the block you are looking at");
        SPELLS.put(ModStaff.TIDE, "Water Breathing and Dolphin's Grace for 60 seconds");
        SPELLS.put(ModStaff.HEALING, "Restores 3 hearts");
        SPELLS.put(ModStaff.GUST, "A burst of wind that throws everything around you away");
        SPELLS.put(ModStaff.ECHO, "Nearby creatures glow through walls for 20 seconds");
    }

    public ModEnglishLangProvider(PackOutput output) {
        super(output, SyMagic.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("itemGroup.symagic.main", "SY Magic");

        for (ModStaff staff : ModStaff.values()) {
            add(ModItems.STAFFS.get(staff).get(), staff.displayName());
            add(staff.spellKey(), SPELLS.get(staff));
        }

        add("tooltip.symagic.staff.cooldown", "Cooldown: %ss");

        add("enchantment.symagic.spell_power", "Spell Power");
        add("enchantment.symagic.alacrity", "Alacrity");
        add("enchantment.symagic.arcane_reach", "Arcane Reach");
    }
}
