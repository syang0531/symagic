package com.syang.symagic.world.item;

import com.syang.symagic.registry.ModDataComponents;
import com.syang.symagic.registry.ModEnchantments;
import com.syang.symagic.registry.ModItems;
import com.syang.symagic.world.spell.SpellCooldowns;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * The staff — there is only one, single-tier like the bow, crossbow, trident and mace. It holds up to
 * {@link StaffSpells#CAPACITY} spellbooks and casts whichever is selected.
 *
 * <ul>
 *   <li><b>Use</b> casts the selected spell: that spell's cooldown, and 1 durability.</li>
 *   <li><b>Sneak + use</b> selects the next spellbook. It works while any spell is cooling down,
 *       because the staff itself is never on cooldown (see {@code SpellCooldowns}).</li>
 *   <li><b>In an inventory</b> it takes and gives spellbooks exactly the way a 26.2 bundle takes and
 *       gives items: left-click a book onto it (or it onto a book) to slot it; right-click it with an
 *       empty cursor to take the selected book out.</li>
 * </ul>
 *
 * <p>Enchantments sit on the staff and apply to every spell in it, so there is one thing to invest
 * in. Durability, repair material and enchantability are set at registration in {@code ModItems}.
 */
public class StaffItem extends Item {

    /** A mace's worth: the one staff takes every cast. Unbreaking and Mending apply. */
    public static final int DURABILITY = 500;

    /** Gold's value — the most enchantable vanilla material, for the item you enchant. */
    public static final int ENCHANTABILITY = 22;

    public StaffItem(Properties properties) {
        super(properties);
    }

    public static StaffSpells spells(ItemStack staff) {
        return staff.getOrDefault(ModDataComponents.STAFF_SPELLS.get(), StaffSpells.EMPTY);
    }

    private static void setSpells(ItemStack staff, StaffSpells spells) {
        staff.set(ModDataComponents.STAFF_SPELLS.get(), spells);
    }

    // ---------------------------------------------------------------- casting and switching

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        StaffSpells spells = spells(stack);

        if (player.isSecondaryUseActive()) {
            // Only the server changes the stack; the client picks the change up from the slot sync,
            // so a prediction can never cycle twice.
            if (!level.isClientSide()) {
                if (spells.size() >= 2) {
                    spells = spells.next();
                    setSpells(stack, spells);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.8F, 1.2F);
                }
                player.sendOverlayMessage(selectedMessage(spells));
            }
            return InteractionResult.CONSUME;
        }

        Optional<Spell> selected = spells.selectedSpell();
        if (selected.isEmpty()) {
            if (!level.isClientSide()) {
                player.sendOverlayMessage(selectedMessage(spells));
            }
            return InteractionResult.FAIL;
        }

        Spell spell = selected.get();
        if (SpellCooldowns.isOnCooldown(player, spell)) {
            return InteractionResult.FAIL;
        }

        if (level instanceof ServerLevel serverLevel) {
            // Spell Power scales the cast; Alacrity shortens the cooldown; Arcane Reach is read
            // inside the individual spells, which are the only ones that know what "reach" means.
            float power = ModEnchantments.spellPower(serverLevel, stack);
            spell.cast(serverLevel, player, stack, power);

            int cooldown = Math.round(spell.cooldownTicks() * ModEnchantments.cooldown(serverLevel, stack));
            SpellCooldowns.start(player, spell, Math.max(Spell.MIN_COOLDOWN_TICKS, cooldown));
            stack.hurtAndBreak(1, player, hand);

            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.0F);
        }

        // SUCCESS swings the arm client-side; nothing more to do here.
        return InteractionResult.SUCCESS;
    }

    /** The action-bar line: which spell is up, or how to load an empty staff. */
    private static Component selectedMessage(StaffSpells spells) {
        return spells.selectedSpell()
                .<Component>map(spell -> spellName(spell).withStyle(spell.element().color()))
                .orElseGet(() -> Component.translatable("message.symagic.staff.empty").withStyle(ChatFormatting.GRAY));
    }

    private static MutableComponent spellName(Spell spell) {
        return Component.literal(spell.element().glyph() + " ").append(Component.translatable(spell.nameKey()));
    }

    // ---------------------------------------------------------------- slotting spellbooks, bundle-style

    /** The cursor holds {@code other} and clicks on the staff's slot. Mirrors {@code BundleItem}. */
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack self, ItemStack other, Slot slot, ClickAction clickAction,
                                            Player player, SlotAccess carriedItem) {
        if (self.getCount() != 1) {
            return false;
        }
        StaffSpells spells = spells(self);

        if (clickAction == ClickAction.PRIMARY && other.getItem() instanceof SpellbookItem book) {
            if (slot.allowModification(player) && spells.canAccept(book.spell())) {
                setSpells(self, spells.with(book.spell()));
                other.shrink(1);
                playInsertSound(player);
            } else {
                playInsertFailSound(player);
            }
            broadcastChangesOnContainerMenu(player);
            return true;
        }

        if (clickAction == ClickAction.SECONDARY && other.isEmpty() && !spells.isEmpty()) {
            if (slot.allowModification(player)) {
                Spell removed = spells.selectedSpell().orElseThrow();
                setSpells(self, spells.withoutSelected());
                carriedItem.set(new ItemStack(ModItems.spellbook(removed)));
                playRemoveOneSound(player);
            }
            broadcastChangesOnContainerMenu(player);
            return true;
        }

        return false;
    }

    /** The cursor holds the staff and clicks on {@code slot}. Mirrors {@code BundleItem}. */
    @Override
    public boolean overrideStackedOnOther(ItemStack self, Slot slot, ClickAction clickAction, Player player) {
        if (self.getCount() != 1) {
            return false;
        }
        StaffSpells spells = spells(self);
        ItemStack other = slot.getItem();

        if (clickAction == ClickAction.PRIMARY && other.getItem() instanceof SpellbookItem book) {
            if (spells.canAccept(book.spell()) && !slot.safeTake(1, 1, player).isEmpty()) {
                setSpells(self, spells.with(book.spell()));
                playInsertSound(player);
            } else {
                playInsertFailSound(player);
            }
            broadcastChangesOnContainerMenu(player);
            return true;
        }

        if (clickAction == ClickAction.SECONDARY && other.isEmpty() && !spells.isEmpty()) {
            Spell removed = spells.selectedSpell().orElseThrow();
            if (slot.safeInsert(new ItemStack(ModItems.spellbook(removed))).isEmpty()) {
                setSpells(self, spells.withoutSelected());
                playRemoveOneSound(player);
            }
            broadcastChangesOnContainerMenu(player);
            return true;
        }

        return false;
    }

    private static void playInsertSound(Entity entity) {
        entity.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 0.8F + entity.level().getRandom().nextFloat() * 0.4F);
    }

    private static void playInsertFailSound(Entity entity) {
        entity.playSound(SoundEvents.BUNDLE_INSERT_FAIL, 1.0F, 1.0F);
    }

    private static void playRemoveOneSound(Entity entity) {
        entity.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8F, 0.8F + entity.level().getRandom().nextFloat() * 0.4F);
    }

    private static void broadcastChangesOnContainerMenu(Player player) {
        AbstractContainerMenu menu = player.containerMenu;
        if (menu != null) {
            menu.slotsChanged(player.getInventory());
        }
    }

    // ---------------------------------------------------------------- tooltip

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        StaffSpells spells = spells(stack);
        List<Spell> list = spells.spells();

        for (int i = 0; i < list.size(); i++) {
            Spell spell = list.get(i);
            boolean selected = i == spells.selected();
            tooltip.accept(Component.literal(selected ? "▶ " : "   ")
                    .append(spellName(spell))
                    .withStyle(selected ? spell.element().color() : ChatFormatting.GRAY));
            if (selected) {
                tooltip.accept(Component.literal("     ")
                        .append(Component.translatable(spell.descriptionKey()))
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        for (int i = list.size(); i < StaffSpells.CAPACITY; i++) {
            tooltip.accept(Component.literal("   ○ ")
                    .append(Component.translatable("tooltip.symagic.staff.empty_slot"))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }

        if (spells.isEmpty()) {
            tooltip.accept(Component.translatable("tooltip.symagic.staff.insert_hint").withStyle(ChatFormatting.DARK_GRAY));
        } else if (spells.size() >= 2) {
            tooltip.accept(Component.translatable("tooltip.symagic.staff.cycle_hint").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
