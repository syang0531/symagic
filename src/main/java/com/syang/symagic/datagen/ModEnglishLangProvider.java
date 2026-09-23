package com.syang.symagic.datagen;

import com.syang.symagic.SyMagic;
import com.syang.symagic.registry.ModItems;
import com.syang.symagic.world.item.Spell;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

import java.util.EnumMap;
import java.util.Map;

/**
 * Generates assets/symagic/lang/en_us.json. (Korean ko_kr.json is hand-written.)
 *
 * <p>Every spell needs a one-line description, because the tooltip is the only place its rules are
 * written down — on the spellbook, and on the staff for the selected spell.
 */
public class ModEnglishLangProvider extends LanguageProvider {

    /** The one line of tooltip that tells you what a spell does. */
    private static final Map<Spell, String> DESCRIPTIONS = new EnumMap<>(Spell.class);

    static {
        DESCRIPTIONS.put(Spell.EMBER, "Throws a small fireball that sets its target alight");
        DESCRIPTIONS.put(Spell.FROST, "Fires a quick arrow that slows its target");
        DESCRIPTIONS.put(Spell.LANTERN, "Night Vision for 60 seconds");
        DESCRIPTIONS.put(Spell.DRIFT, "Slow Falling for 45 seconds");
        DESCRIPTIONS.put(Spell.BOUNDING, "Jump Boost II for 45 seconds, and the landing cannot hurt you");
        DESCRIPTIONS.put(Spell.BLINK, "Step to the block you are looking at");
        DESCRIPTIONS.put(Spell.TIDE, "Water Breathing and Dolphin's Grace for 60 seconds");
        DESCRIPTIONS.put(Spell.HEALING, "Restores 3 hearts");
        DESCRIPTIONS.put(Spell.GUST, "A burst of wind that throws everything around you away");
        DESCRIPTIONS.put(Spell.ECHO, "Nearby creatures glow through walls for 20 seconds");
        DESCRIPTIONS.put(Spell.RECALL, "Return to the lodestone this book is bound to");
    }

    public ModEnglishLangProvider(PackOutput output) {
        super(output, SyMagic.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("itemGroup.symagic.main", "SY Magic");

        add(ModItems.STAFF.get(), "Staff");
        add("tooltip.symagic.staff.empty_slot", "Empty slot");
        add("tooltip.symagic.staff.insert_hint", "Click a spellbook onto this staff to slot it in");
        add("tooltip.symagic.staff.cycle_hint", "Sneak + use: next spell");
        add("message.symagic.staff.empty", "No spellbook in this staff");

        for (Spell spell : Spell.values()) {
            add(ModItems.spellbook(spell), spell.displayName() + " Spellbook");
            add(spell.nameKey(), spell.displayName());
            add(spell.descriptionKey(), DESCRIPTIONS.get(spell));
        }
        add("tooltip.symagic.spellbook.cooldown", "Cooldown: %ss");
        add("tooltip.symagic.spellbook.hint", "Slot into a staff to cast");
        add("tooltip.symagic.spellbook.hold", "Hold use for %ss to cast");

        add("tooltip.symagic.recall.destination", "→ %s, %s, %s · %s");
        add("tooltip.symagic.recall.unbound", "Not bound — use it on a lodestone");
        add("message.symagic.recall.bound", "Bound to this lodestone");
        add("message.symagic.recall.unbound", "This Recall spellbook is not bound to a lodestone");
        add("message.symagic.recall.other_dimension", "That lodestone is in another dimension");
        add("message.symagic.recall.missing", "The lodestone is gone");
        add("message.symagic.recall.blocked", "Something is in the way on top of the lodestone");
        add("symagic.dimension.overworld", "Overworld");
        add("symagic.dimension.the_nether", "Nether");
        add("symagic.dimension.the_end", "The End");

        add("enchantment.symagic.spell_power", "Spell Power");
        add("enchantment.symagic.alacrity", "Alacrity");
        add("enchantment.symagic.arcane_reach", "Arcane Reach");
    }
}
