package org.example.Kangnaixi.tiandao.spell.runtime;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.phys.Vec3;
import org.example.Kangnaixi.tiandao.capability.ICultivation;

/**
 * 旧版 Spell 对象的执行器，负责根据载体/效果生成对应实体，同时统一本地灵力扣除。
 */
public final class SpellExecutor {

    private static final double MIN_COST = 5.0;
    private static final double MAX_COST = 320.0;

    private SpellExecutor() {}

    public static CastResult cast(ServerPlayer player, ICultivation cultivation, Spell spell) {
        if (spell == null) {
            return CastResult.failure(CastResult.FailureReason.UNKNOWN, 0, cultivation.getSpiritPower());
        }
        double spiritCost = estimateSpiritCost(spell);
        if (cultivation.getSpiritPower() < spiritCost || !cultivation.consumeSpiritPower(spiritCost)) {
            return CastResult.failure(CastResult.FailureReason.SPIRIT, spiritCost, cultivation.getSpiritPower());
        }

        switch (spell.getCarrier()) {
            case SWORD_QI -> fireSwordQi(player, spell);
            case PROJECTILE -> fireProjectile(player, spell);
            case FIELD -> spawnField(player, spell);
            case BUFF -> applyBuff(player, spell);
            default -> fireProjectile(player, spell);
        }
        return CastResult.success(spiritCost);
    }

    public static double estimateSpiritCost(Spell spell) {
        double cost = Math.max(MIN_COST, spell.getBaseSpiritCost());
        cost += spell.getAttributes().size() * 3.5;
        cost += spell.getEffects().size() * 4.0;
        switch (spell.getCarrier()) {
            case FIELD -> cost += 12.0;
            case BUFF -> cost += 8.0;
            case SWORD_QI -> cost += 6.0;
            default -> cost += 4.0;
        }
        switch (spell.getForm()) {
            case CHANNEL -> cost *= 1.2;
            case DURATION -> cost *= 1.15;
            case COMBO, MARK_DETONATE -> cost *= 1.1;
            default -> {
            }
        }
        return Math.min(MAX_COST, cost);
    }

    private static void fireSwordQi(ServerPlayer player, Spell spell) {
        ServerLevel level = player.serverLevel();
        Vec3 dir = player.getLookAngle().normalize();
        SmallFireball swordQi = new SmallFireball(level, player, dir.x, dir.y, dir.z);
        swordQi.setPos(player.getX(), player.getEyeY() - 0.1D, player.getZ());
        double damage = spell.getBaseDamage();
        if (spell.getAttributes().contains(AttributeType.SWORD_INTENT)) {
            damage *= 1.25;
        }
        swordQi.xPower *= spell.getBaseRange();
        swordQi.yPower *= spell.getBaseRange();
        swordQi.zPower *= spell.getBaseRange();
        level.addFreshEntity(swordQi);
    }

    private static void fireProjectile(ServerPlayer player, Spell spell) {
        ServerLevel level = player.serverLevel();
        Vec3 dir = player.getLookAngle();
        SmallFireball fireball = new SmallFireball(level, player, dir.x, dir.y, dir.z);
        fireball.setPos(player.getX(), player.getEyeY(), player.getZ());
        level.addFreshEntity(fireball);
    }

    private static void spawnField(ServerPlayer player, Spell spell) {
        ServerLevel level = player.serverLevel();
        AreaEffectCloud cloud = new AreaEffectCloud(level, player.getX(), player.getY(), player.getZ());
        cloud.setRadius((float) Math.max(1.5F, spell.getBaseRange()));
        if (spell.getEffects().contains(EffectType.SHIELD)) {
            cloud.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 1));
        }
        if (spell.getEffects().contains(EffectType.AOE_UP)) {
            cloud.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 0));
        }
        cloud.setDuration((int) Math.max(40, spell.getBaseCooldown()));
        level.addFreshEntity(cloud);
    }

    private static void applyBuff(ServerPlayer player, Spell spell) {
        if (spell.getEffects().contains(EffectType.HEAL_UP)) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
        }
        if (spell.getEffects().contains(EffectType.MOVE_SPEED)) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 0));
        }
    }

    public record CastResult(boolean success,
                             double spiritCost,
                             double currentSpirit,
                             FailureReason failureReason) {

        public static CastResult success(double spiritCost) {
            return new CastResult(true, spiritCost, 0, null);
        }

        public static CastResult failure(FailureReason reason, double expected, double current) {
            return new CastResult(false, expected, current, reason);
        }

        public enum FailureReason {
            SPIRIT,
            UNKNOWN
        }
    }
}
