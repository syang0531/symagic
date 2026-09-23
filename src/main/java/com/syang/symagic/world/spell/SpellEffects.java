package com.syang.symagic.world.spell;

import com.syang.symagic.registry.ModEnchantments;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Server-side cast behaviours, one per {@link com.syang.symagic.world.item.Spell}.
 *
 * <p>Each method matches {@code Spell.SpellAction} — {@code (ServerLevel, Player, ItemStack
 * staff, float power)} — and is referenced by method handle from the spell table, so the roster
 * stays a single readable list. {@code staff} is the staff doing the casting; its enchantments
 * are what the spell reads.
 *
 * <p><b>Balance.</b> {@code power} is the Spell Power multiplier; it scales damage, healing and
 * buff duration. Only {@link #firebolt} and {@link #frostArrow} deal damage at all, and both are
 * deliberately worse than a contemporary sword: a staff is a tool you reach for when a sword will
 * not do, not a better sword. Radii, projectile speed and blink range scale with Arcane Reach via
 * {@link ModEnchantments#reach}.
 */
public final class SpellEffects {

    private SpellEffects() {
    }

    // ---------------------------------------------------------------- attack

    /**
     * Firebolt — the vanilla small fireball: about 5 fire damage, and it sets the target alight.
     * Its damage is fixed by vanilla and does not scale, which keeps the cheapest attack spell an
     * early-game tool rather than a late-game answer.
     */
    public static void firebolt(ServerLevel level, Player caster, ItemStack staff, float power) {
        float reach = ModEnchantments.reach(level, staff);
        Vec3 view = caster.getViewVector(1.0F);
        SmallFireball fireball = new SmallFireball(level, caster, view.scale(reach));
        Vec3 eye = caster.getEyePosition();
        fireball.setPos(eye.x + view.x, eye.y - 0.1 + view.y, eye.z + view.z);
        level.addFreshEntity(fireball);
    }

    /**
     * Frost Arrow — a quick arrow for 5x power that slows what it hits for 3 s.
     *
     * <p>Vanilla computes an arrow's damage as {@code ceil(flight speed x baseDamage)}, so the
     * launch speed is divided back out: Arcane Reach then buys range and travel time, never damage.
     */
    public static void frostArrow(ServerLevel level, Player caster, ItemStack staff, float power) {
        float velocity = 3.0F * ModEnchantments.reach(level, staff);
        Arrow arrow = new Arrow(level, caster, new ItemStack(Items.ARROW), null);
        arrow.setBaseDamage((5.0 * power) / velocity);
        arrow.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 0));
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        arrow.shootFromRotation(caster, caster.getXRot(), caster.getYRot(), 0.0F, velocity, 1.0F);
        level.addFreshEntity(arrow);
    }

    // ---------------------------------------------------------------- support

    /** Heal — restores 6x power health to the caster. */
    public static void heal(ServerLevel level, Player caster, ItemStack staff, float power) {
        caster.heal(6.0F * power);
        level.sendParticles(ParticleTypes.HEART, caster.getX(), caster.getY() + 1.0, caster.getZ(), 8, 0.4, 0.6, 0.4, 0.0);
    }

    /** Night Vision for 60 s. The reason to carry a staff into a dark maze. */
    public static void nightVision(ServerLevel level, Player caster, ItemStack staff, float power) {
        caster.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, duration(1200, power), 0));
        sparkle(level, caster);
    }

    /** Slow Falling for 45 s — any drop becomes a shortcut. */
    public static void slowFalling(ServerLevel level, Player caster, ItemStack staff, float power) {
        caster.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, duration(900, power), 0));
        sparkle(level, caster);
    }

    /** Jump Boost II for 45 s, plus enough Slow Falling that the jump cannot hurt you landing. */
    public static void leap(ServerLevel level, Player caster, ItemStack staff, float power) {
        int ticks = duration(900, power);
        caster.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, ticks, 1));
        caster.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, ticks, 0));
        sparkle(level, caster);
    }

    /** Water Breathing and Dolphin's Grace for 60 s. */
    public static void tide(ServerLevel level, Player caster, ItemStack staff, float power) {
        int ticks = duration(1200, power);
        caster.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, ticks, 0));
        caster.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, ticks, 0));
        level.sendParticles(ParticleTypes.BUBBLE, caster.getX(), caster.getY() + 1.0, caster.getZ(), 20, 0.5, 0.8, 0.5, 0.0);
    }

    // ---------------------------------------------------------------- movement and control

    /**
     * Blink — step to the block you are looking at, up to 16x reach away, without the ender pearl's
     * damage or its arc. Fall distance is cleared, so it also saves you from a drop.
     */
    public static void blink(ServerLevel level, Player caster, ItemStack staff, float power) {
        double range = 16.0 * ModEnchantments.reach(level, staff);
        HitResult hit = caster.pick(range, 1.0F, false);
        // Step back along the view ray so we never land inside the block we aimed at.
        Vec3 landing = hit.getLocation().subtract(caster.getViewVector(1.0F).scale(0.6));

        Vec3 from = caster.position();
        level.sendParticles(ParticleTypes.PORTAL, from.x, from.y + 1.0, from.z, 24, 0.3, 0.6, 0.3, 0.4);
        caster.teleportTo(landing.x, landing.y, landing.z);
        caster.resetFallDistance();
        level.sendParticles(ParticleTypes.PORTAL, landing.x, landing.y + 1.0, landing.z, 24, 0.3, 0.6, 0.3, 0.4);
    }

    /**
     * Gust — a ring of wind that throws every other creature within 5x reach blocks away from you
     * and lifts you a little. It deals no damage: this spell buys you distance, not a kill.
     */
    public static void gust(ServerLevel level, Player caster, ItemStack staff, float power) {
        double radius = 5.0 * ModEnchantments.reach(level, staff);
        Vec3 origin = caster.position();
        AABB area = caster.getBoundingBox().inflate(radius);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area, e -> e != caster && e.isAlive())) {
            Vec3 away = entity.position().subtract(origin);
            double dist = away.length();
            if (dist < 1.0E-4) {
                away = caster.getViewVector(1.0F);
                dist = 1.0;
            }
            // Full strength at the caster, tapering to nothing at the edge of the ring.
            double strength = 1.4 * power * (1.0 - Math.min(1.0, dist / radius));
            Vec3 push = away.scale(1.0 / dist).scale(strength);
            entity.push(push.x, 0.45 * strength, push.z);
            entity.hurtMarked = true;
        }
        caster.push(0.0, 0.5, 0.0);
        caster.hurtMarked = true;
        caster.resetFallDistance();
        level.sendParticles(ParticleTypes.GUST, origin.x, origin.y + 0.5, origin.z, 12, radius * 0.4, 0.4, radius * 0.4, 0.0);
    }

    /** Reveal — every living thing within 24x reach blocks glows through the walls for 20 s. */
    public static void reveal(ServerLevel level, Player caster, ItemStack staff, float power) {
        double radius = 24.0 * ModEnchantments.reach(level, staff);
        int ticks = duration(400, power);
        AABB area = caster.getBoundingBox().inflate(radius);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area, e -> e != caster && e.isAlive())) {
            entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks, 0));
        }
        level.sendParticles(ParticleTypes.SCULK_SOUL, caster.getX(), caster.getY() + 1.0, caster.getZ(), 16, 0.6, 0.8, 0.6, 0.0);
    }

    // ---------------------------------------------------------------- helpers

    /** Spell Power lengthens a buff as well as strengthening a hit. */
    private static int duration(int baseTicks, float power) {
        return Math.round(baseTicks * power);
    }

    private static void sparkle(ServerLevel level, Player caster) {
        level.sendParticles(ParticleTypes.END_ROD, caster.getX(), caster.getY() + 1.0, caster.getZ(), 12, 0.4, 0.6, 0.4, 0.01);
    }
}
