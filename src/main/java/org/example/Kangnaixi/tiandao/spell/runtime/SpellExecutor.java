package org.example.Kangnaixi.tiandao.spell.runtime;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.phys.Vec3;

/**
 * 极简的施法执行器，根据载体/属性进行分支，方便未来挂接 GUI。
 */
public final class SpellExecutor {

    private SpellExecutor() {}

    public static void cast(ServerPlayer player, Spell spell) {
        if (spell == null) {
            return;
        }
        switch (spell.getCarrier()) {
            case SWORD_QI -> fireSwordQi(player, spell);
            case PROJECTILE -> fireProjectile(player, spell);
            case FIELD -> spawnField(player, spell);
            case BUFF -> applyBuff(player, spell);
            default -> fireProjectile(player, spell); // fallback
        }
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
}
