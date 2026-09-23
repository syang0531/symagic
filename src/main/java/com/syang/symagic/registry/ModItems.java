package com.syang.symagic.registry;

import com.syang.symagic.SyMagic;
import com.syang.symagic.world.item.RecallSpellbookItem;
import com.syang.symagic.world.item.Spell;
import com.syang.symagic.world.item.SpellbookItem;
import com.syang.symagic.world.item.StaffItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

/**
 * Every item in the mod: the one staff, and one spellbook per {@link Spell}. There is nothing else —
 * no rune, no catalyst, no tiered staff. The staff is crafted from vanilla items, repaired with gold
 * and enchanted at a vanilla enchanting table; a spellbook is a book plus one vanilla material.
 */
public final class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SyMagic.MOD_ID);

    public static final DeferredItem<StaffItem> STAFF = ITEMS.registerItem("staff", props -> new StaffItem(props
            .durability(StaffItem.DURABILITY)
            // Repaired at an anvil with the ingot it was made from, like any golden tool.
            .repairable(Items.GOLD_INGOT)
            // Non-zero enchantability is what puts the staff on an enchanting table at all.
            .enchantable(StaffItem.ENCHANTABILITY)
            // No grid repair and no grindstone combining: both build the result from scratch or from
            // one input only, so the spellbooks in the other staff (or in both) would silently vanish.
            // Repair is gold at an anvil, or Mending. See StaffEvents for the anvil-combine case.
            .setNoCombineRepair()));

    public static final Map<Spell, DeferredItem<SpellbookItem>> SPELLBOOKS = new EnumMap<>(Spell.class);

    static {
        for (Spell spell : Spell.values()) {
            // One per stack, like an enchanted book: it is a thing you own, not a consumable.
            // Recall is the one book with behaviour of its own: it binds to a lodestone.
            SPELLBOOKS.put(spell, ITEMS.registerItem(spell.bookId(), props -> spell == Spell.RECALL
                    ? new RecallSpellbookItem(props.stacksTo(1))
                    : new SpellbookItem(spell, props.stacksTo(1))));
        }
    }

    private ModItems() {
    }

    public static Item spellbook(Spell spell) {
        return SPELLBOOKS.get(spell).get();
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
