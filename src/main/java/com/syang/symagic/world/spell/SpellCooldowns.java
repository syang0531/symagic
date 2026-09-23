package com.syang.symagic.world.spell;

import com.syang.symagic.world.item.Spell;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.UseCooldown;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Per-spell cooldowns, kept in the player's vanilla {@code ItemCooldowns} under one group per spell
 * ({@link Spell#cooldownGroup()}). Vanilla syncs those groups to the client on its own, so the
 * hotbar overlay can read them there too.
 *
 * <p><b>Why the staff itself never goes on cooldown.</b> The obvious route — a
 * {@code use_cooldown} component on the staff naming the selected spell's group — breaks the design
 * twice over:
 *
 * <ol>
 *   <li>{@code ServerPlayerGameMode#useItem} (and the client's {@code MultiPlayerGameMode}) drop a
 *       use request for a stack that is on cooldown before {@code Item#use} ever runs. The staff would
 *       refuse sneak-use while its selected spell cooled down, so you could not switch from Blink to
 *       Healing straight after blinking — the one thing a multi-spell staff is for.</li>
 *   <li>After a successful use, vanilla re-applies that component's fixed duration over whatever
 *       cooldown the item set, which would throw Alacrity away.</li>
 * </ol>
 *
 * <p>So the staff's own group ({@code symagic:staff}) stays clear, and each cast puts only its spell's
 * group on cooldown.
 *
 * <p>{@code ItemCooldowns} only answers questions about stacks, not bare groups, so each spell keeps
 * a <i>probe</i>: a stack whose only job is to carry a {@code use_cooldown} naming that group. It is
 * never given to anyone and never changed after it is built.
 */
public final class SpellCooldowns {

    private static final Map<Spell, ItemStack> PROBES = new EnumMap<>(Spell.class);

    static {
        for (Spell spell : Spell.values()) {
            ItemStack probe = new ItemStack(Items.STICK);
            probe.set(DataComponents.USE_COOLDOWN, new UseCooldown(1.0F, Optional.of(spell.cooldownGroup())));
            PROBES.put(spell, probe);
        }
    }

    private SpellCooldowns() {
    }

    /** Puts {@code spell} on cooldown for {@code ticks}. Server-side; vanilla forwards it to the client. */
    public static void start(Player player, Spell spell, int ticks) {
        player.getCooldowns().addCooldown(spell.cooldownGroup(), ticks);
    }

    public static boolean isOnCooldown(Player player, Spell spell) {
        return percent(player, spell, 0.0F) > 0.0F;
    }

    /** Fraction of {@code spell}'s cooldown still to run, 1 → 0; the same number vanilla draws its overlay from. */
    public static float percent(Player player, Spell spell, float partialTick) {
        return player.getCooldowns().getCooldownPercent(PROBES.get(spell), partialTick);
    }
}
