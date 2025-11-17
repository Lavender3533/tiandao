package org.example.Kangnaixi.tiandao.spell.runtime.engine;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.phys.Vec3;
import org.example.Kangnaixi.tiandao.spell.runtime.CarrierType;

import java.util.EnumMap;
import java.util.Map;

/**
 * 术法载体执行器注册器
 */
public final class CarrierExecutors {

    private static final Map<CarrierType, CarrierExecutor> EXECUTORS = new EnumMap<>(CarrierType.class);

    static {
        // 注册基础实现（复用旧的 SpellExecutor 逻辑）
        register(CarrierType.SWORD_QI, CarrierExecutors::spawnSwordQi);
        register(CarrierType.PROJECTILE, CarrierExecutors::spawnProjectile);
        register(CarrierType.FIELD, CarrierExecutors::spawnField);
        register(CarrierType.BUFF, CarrierExecutors::applyBuff);

        // 未实现的载体
        register(CarrierType.WAVE, CarrierExecutor.NO_OP);
        register(CarrierType.GLYPH, CarrierExecutor.NO_OP);
    }

    /**
     * 注册载体执行器
     */
    public static void register(CarrierType type, CarrierExecutor executor) {
        EXECUTORS.put(type, executor);
    }

    /**
     * 获取载体执行器
     */
    public static CarrierExecutor get(CarrierType type) {
        return EXECUTORS.getOrDefault(type, CarrierExecutor.NO_OP);
    }

    // ========== 基础实现（复用旧逻辑）==========

    /**
     * 生成剑气（使用旧的 SpellExecutor 逻辑）
     */
    private static void spawnSwordQi(SpellContext ctx) {
        ServerPlayer player = ctx.getCaster();
        ServerLevel level = ctx.getLevel();
        Vec3 dir = ctx.getDirection();
        double damage = ctx.getDamage();

        // 复用旧逻辑：生成小型火球作为剑气
        SmallFireball swordQi = new SmallFireball(level, player, dir.x, dir.y, dir.z);
        swordQi.setPos(player.getX(), player.getEyeY() - 0.1D, player.getZ());

        // 应用伤害和射程
        swordQi.xPower *= ctx.getRange();
        swordQi.yPower *= ctx.getRange();
        swordQi.zPower *= ctx.getRange();

        level.addFreshEntity(swordQi);
        ctx.putData("carrier.sword_qi", true);
    }

    /**
     * 生成抛射物（使用旧的 SpellExecutor 逻辑）
     */
    private static void spawnProjectile(SpellContext ctx) {
        ServerPlayer player = ctx.getCaster();
        ServerLevel level = ctx.getLevel();
        Vec3 dir = ctx.getDirection();

        // 复用旧逻辑：生成小型火球
        SmallFireball fireball = new SmallFireball(level, player, dir.x, dir.y, dir.z);
        fireball.setPos(player.getX(), player.getEyeY(), player.getZ());
        level.addFreshEntity(fireball);

        ctx.putData("carrier.projectile", true);
    }

    /**
     * 生成力场（使用旧的 SpellExecutor 逻辑）
     */
    private static void spawnField(SpellContext ctx) {
        ServerPlayer player = ctx.getCaster();
        ServerLevel level = ctx.getLevel();

        // 复用旧逻辑：生成范围效果云
        AreaEffectCloud cloud = new AreaEffectCloud(level, player.getX(), player.getY(), player.getZ());
        cloud.setRadius((float) Math.max(1.5F, ctx.getRange()));
        cloud.setDuration((int) Math.max(40, ctx.getSpell().getBaseCooldown()));

        level.addFreshEntity(cloud);
        ctx.putData("carrier.field", true);
    }

    /**
     * 应用增益（标记为增益类型）
     */
    private static void applyBuff(SpellContext ctx) {
        ctx.putData("carrier.buff", true);
        // 增益效果将由 EffectExecutor 具体处理
    }

    private CarrierExecutors() {}
}
