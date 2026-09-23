package com.syang.symagic.world.item;

import com.syang.symagic.registry.ModDataComponents;
import com.syang.symagic.registry.ModEnchantments;
import com.syang.symagic.world.spell.SpellCooldowns;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.ItemUseAnimation;
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
 *   <li><b>Use</b> casts the selected spell: that spell's cooldown, and 1 durability. A held spell
 *       (Recall) is drawn like a bow instead — hold use for its {@link Spell#useTicks()} and it goes
 *       off; let go early and nothing happens and nothing is charged.</li>
 *   <li><b>Sneak + use</b> selects the next spellbook. It works while any spell is cooling down,
 *       because the staff itself is never on cooldown (see {@code SpellCooldowns}).</li>
 *   <li><b>In an inventory</b> it takes and gives spellbooks exactly the way a 26.2 bundle takes and
 *       gives items: left-click a book onto it (or it onto a book) to slot it; right-click it with an
 *       empty cursor to take the selected book out. The book comes out as it went in.</li>
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

        Optional<ItemStackTemplate> selected = spells.selectedBook();
        if (selected.isEmpty()) {
            if (!level.isClientSide()) {
                player.sendOverlayMessage(selectedMessage(spells));
            }
            return InteractionResult.FAIL;
        }

        ItemStackTemplate book = selected.get();
        Spell spell = SpellbookItem.spellOf(book).orElseThrow();
        if (SpellCooldowns.isOnCooldown(player, spell)) {
            return InteractionResult.FAIL;
        }

        if (spell.isHeld()) {
            // Start drawing; the cast happens in finishUsingItem. Checked first so an unbound Recall
            // book says so now rather than after three seconds of holding.
            if (!spell.ready(level, player, book)) {
                return InteractionResult.FAIL;
            }
            player.startUsingItem(hand);
            return InteractionResult.CONSUME;
        }

        if (level instanceof ServerLevel serverLevel) {
            castSelected(serverLevel, player, stack, hand);
        }
        // SUCCESS swings the arm client-side; nothing more to do here.
        return InteractionResult.SUCCESS;
    }

    /**
     * Casts the selected book and, if it went off, charges it: the spell's cooldown (shortened by
     * Alacrity) and 1 durability. Shared by instant spells and by a completed hold.
     */
    private static void castSelected(ServerLevel level, Player player, ItemStack stack, InteractionHand hand) {
        Optional<ItemStackTemplate> selected = spells(stack).selectedBook();
        if (selected.isEmpty()) {
            return;
        }
        ItemStackTemplate book = selected.get();
        Spell spell = SpellbookItem.spellOf(book).orElseThrow();
        if (SpellCooldowns.isOnCooldown(player, spell)) {
            return;
        }

        // Spell Power scales the cast; Arcane Reach is read inside the individual spells, which are
        // the only ones that know what "reach" means.
        float power = ModEnchantments.spellPower(level, stack);
        if (!spell.cast(level, player, stack, book, power)) {
            return;
        }

        int cooldown = Math.round(spell.cooldownTicks() * ModEnchantments.cooldown(level, stack));
        SpellCooldowns.start(player, spell, Math.max(Spell.MIN_COOLDOWN_TICKS, cooldown));
        stack.hurtAndBreak(1, player, hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.0F);
    }

    /** The selected spell if it is a held one — the only case in which the staff is "used" over time. */
    private static Optional<Spell> heldSpell(ItemStack stack) {
        return spells(stack).selectedSpell().filter(Spell::isHeld);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return heldSpell(stack).map(Spell::useTicks).orElse(0);
    }

    /** The staff raised overhead while a held spell gathers. */
    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return heldSpell(stack).isPresent() ? ItemUseAnimation.TRIDENT : ItemUseAnimation.NONE;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int ticksRemaining) {
        if (level instanceof ServerLevel serverLevel && ticksRemaining % 4 == 0) {
            serverLevel.sendParticles(ParticleTypes.PORTAL, entity.getX(), entity.getY() + 1.0, entity.getZ(),
                    6, 0.4, 0.6, 0.4, 0.3);
        }
    }

    /** The hold completed. Re-read everything: the selection or cooldowns may have moved meanwhile. */
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (level instanceof ServerLevel serverLevel && entity instanceof Player player && heldSpell(stack).isPresent()) {
            castSelected(serverLevel, player, stack, entity.getUsedItemHand());
        }
        return stack;
    }

    /** The action-bar line: which spell is up, or how to load an empty staff. */
    private static Component selectedMessage(StaffSpells spells) {
        return spells.selectedBook()
                .<Component>map(book -> SpellbookItem.slotName(book)
                        .withStyle(SpellbookItem.spellOf(book).orElseThrow().element().color()))
                .orElseGet(() -> Component.translatable("message.symagic.staff.empty").withStyle(ChatFormatting.GRAY));
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

        if (clickAction == ClickAction.PRIMARY && other.getItem() instanceof SpellbookItem) {
            if (slot.allowModification(player) && spells.canAccept(other)) {
                setSpells(self, spells.with(other));
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
                ItemStackTemplate removed = spells.selectedBook().orElseThrow();
                setSpells(self, spells.withoutSelected());
                carriedItem.set(removed.create());
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

        if (clickAction == ClickAction.PRIMARY && other.getItem() instanceof SpellbookItem) {
            ItemStack taken = spells.canAccept(other) ? slot.safeTake(1, 1, player) : ItemStack.EMPTY;
            if (!taken.isEmpty()) {
                setSpells(self, spells.with(taken));
                playInsertSound(player);
            } else {
                playInsertFailSound(player);
            }
            broadcastChangesOnContainerMenu(player);
            return true;
        }

        if (clickAction == ClickAction.SECONDARY && other.isEmpty() && !spells.isEmpty()) {
            ItemStackTemplate removed = spells.selectedBook().orElseThrow();
            if (slot.safeInsert(removed.create()).isEmpty()) {
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
        List<ItemStackTemplate> books = spells.books();

        for (int i = 0; i < books.size(); i++) {
            ItemStackTemplate book = books.get(i);
            Spell spell = SpellbookItem.spellOf(book).orElseThrow();
            boolean selected = i == spells.selected();
            tooltip.accept(Component.literal(selected ? "▶ " : "   ")
                    .append(SpellbookItem.slotName(book))
                    .withStyle(selected ? spell.element().color() : ChatFormatting.GRAY));
            if (selected) {
                tooltip.accept(Component.literal("     ")
                        .append(Component.translatable(spell.descriptionKey()))
                        .withStyle(ChatFormatting.DARK_GRAY));
                SpellbookItem.appendDetails(book, line -> tooltip.accept(Component.literal("     ").append(line)));
            }
        }
        for (int i = books.size(); i < StaffSpells.CAPACITY; i++) {
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
