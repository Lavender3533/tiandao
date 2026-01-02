package org.example.Kangnaixi.tiandao.spell.runtime.engine;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.capability.ICultivation;
import org.example.Kangnaixi.tiandao.spell.blueprint.SpellBlueprint;

import java.util.List;

/**
 * Blueprint专用执行器 - 将SpellBlueprint转化为实际效果
 *
 * 执行流程：
 * 1. 前置检查（灵力、冷却、境界）
 * 2. 消耗资源
 * 3. 根据Targeting确定目标
 * 4. 根据Shape确定范围
 * 5. 根据EffectType应用效果
 * 6. 播放视觉/音效反馈
 */
public class BlueprintExecutor {

    /**
     * 执行蓝图术法
     *
     * @param player    施法者
     * @param blueprint 术法蓝图
     * @return 执行结果
     */
    public static ExecutionResult execute(ServerPlayer player, SpellBlueprint blueprint) {
        if (player == null || blueprint == null) {
            return ExecutionResult.failure("参数无效");
        }

        ServerLevel level = player.serverLevel();

        // 1. 获取修炼数据
        ICultivation cultivation = player.getCapability(Tiandao.CULTIVATION_CAPABILITY).orElse(null);
        if (cultivation == null) {
            return ExecutionResult.failure("无法获取修炼数据");
        }

        // 2. 前置检查
        ExecutionResult preCheck = performPreChecks(player, cultivation, blueprint);
        if (!preCheck.isSuccess()) {
            return preCheck;
        }

        // 3. 消耗灵力
        double cost = blueprint.getSpiritCost();
        if (!cultivation.consumeSpiritPower(cost)) {
            return ExecutionResult.failure("灵力不足");
        }

        // 4. 设置冷却
        long cooldownEndTime = System.currentTimeMillis() + (long)(blueprint.getCooldownSeconds() * 1000);
        cultivation.setSpellCooldown(blueprint.getId(), cooldownEndTime);

        try {
            // 5. 执行效果
            executeEffect(player, level, blueprint);

            // 6. 记录日志
            Tiandao.LOGGER.info("玩家 {} 施放术法: {} (消耗: {}, 冷却: {}s)",
                player.getScoreboardName(),
                blueprint.getName(),
                String.format("%.1f", cost),
                blueprint.getCooldownSeconds());

            return ExecutionResult.success(blueprint.getName(), cost);

        } catch (Exception e) {
            Tiandao.LOGGER.error("术法执行异常: " + blueprint.getId(), e);
            // 回退灵力消耗
            cultivation.addSpiritPower(cost);
            return ExecutionResult.failure("执行异常: " + e.getMessage());
        }
    }

    /**
     * 前置检查
     */
    private static ExecutionResult performPreChecks(ServerPlayer player, ICultivation cultivation, SpellBlueprint blueprint) {
        // 检查境界要求
        if (cultivation.getRealm().ordinal() < blueprint.getRequiredRealm().ordinal()) {
            return ExecutionResult.failure("境界不足，需要: " + blueprint.getRequiredRealm().getDisplayName());
        }

        // 检查灵力
        if (cultivation.getSpiritPower() < blueprint.getSpiritCost()) {
            return ExecutionResult.failure(String.format("灵力不足，需要: %.1f，当前: %.1f",
                blueprint.getSpiritCost(), cultivation.getSpiritPower()));
        }

        // 检查冷却
        int cooldownRemaining = cultivation.getSpellCooldownRemaining(blueprint.getId());
        if (cooldownRemaining > 0) {
            return ExecutionResult.failure("术法冷却中，剩余: " + cooldownRemaining + "秒");
        }

        return ExecutionResult.success("检查通过", 0);
    }

    /**
     * 执行具体效果
     */
    private static void executeEffect(ServerPlayer player, ServerLevel level, SpellBlueprint blueprint) {
        Vec3 playerPos = player.position();
        Vec3 lookVec = player.getLookAngle();

        // 根据目标类型获取目标
        List<LivingEntity> targets = findTargets(player, level, blueprint, playerPos, lookVec);

        // 根据效果类型应用效果
        double power = blueprint.getBasePower();

        switch (blueprint.getEffectType()) {
            case DAMAGE -> applyDamageEffect(player, targets, power, blueprint);
            case HEALING -> applyHealingEffect(player, targets, power, blueprint);
            case CONTROL -> applyControlEffect(player, targets, power, blueprint);
            case UTILITY -> applyUtilityEffect(player, level, blueprint);
            case SUMMON -> applySummonEffect(player, level, blueprint);
        }

        // 播放视觉效果
        playVisualEffects(player, level, blueprint, playerPos, lookVec);

        // 播放音效
        playSoundEffects(player, level, blueprint);
    }

    /**
     * 查找目标实体
     */
    private static List<LivingEntity> findTargets(ServerPlayer player, ServerLevel level,
                                                   SpellBlueprint blueprint, Vec3 pos, Vec3 look) {
        double range = blueprint.getRange();
        double radius = blueprint.getAreaRadius();

        AABB searchBox;

        switch (blueprint.getTargeting()) {
            case SELF -> {
                // 自身为中心
                searchBox = new AABB(pos.subtract(radius, radius, radius),
                                     pos.add(radius, radius, radius));
            }
            case TARGET_ENTITY, DIRECTIONAL_RELEASE -> {
                // 前方区域
                Vec3 targetPos = pos.add(look.scale(range / 2));
                searchBox = new AABB(targetPos.subtract(radius, radius, radius),
                                     targetPos.add(radius, radius, radius));
            }
            case TARGET_BLOCK -> {
                // 视线终点
                Vec3 targetPos = pos.add(look.scale(range));
                searchBox = new AABB(targetPos.subtract(radius, radius, radius),
                                     targetPos.add(radius, radius, radius));
            }
            case AREA_RELEASE -> {
                // 周围区域
                searchBox = new AABB(pos.subtract(radius, radius, radius),
                                     pos.add(radius, radius, radius));
            }
            default -> {
                searchBox = new AABB(pos.subtract(range, range, range),
                                     pos.add(range, range, range));
            }
        }

        return level.getEntitiesOfClass(LivingEntity.class, searchBox, entity -> {
            // 排除施法者（除非是自我治疗类）
            if (entity == player && blueprint.getEffectType() != SpellBlueprint.EffectType.HEALING) {
                return false;
            }
            // 排除无敌实体
            return !entity.isInvulnerable();
        });
    }

    // ========== 效果应用方法 ==========

    private static void applyDamageEffect(ServerPlayer player, List<LivingEntity> targets,
                                          double power, SpellBlueprint blueprint) {
        for (LivingEntity target : targets) {
            if (target == player) continue;

            // 计算伤害
            float damage = (float) power;

            // 元素加成（示例）
            switch (blueprint.getElement()) {
                case FIRE -> damage *= 1.1f; // 火焰额外10%
                case WATER -> target.setTicksFrozen(target.getTicksFrozen() + 40); // 冻结效果
            }

            // 应用伤害
            target.hurt(player.damageSources().playerAttack(player), damage);

            Tiandao.LOGGER.debug("术法伤害: {} 对 {} 造成 {} 点伤害",
                blueprint.getName(), target.getName().getString(), damage);
        }
    }

    private static void applyHealingEffect(ServerPlayer player, List<LivingEntity> targets,
                                           double power, SpellBlueprint blueprint) {
        for (LivingEntity target : targets) {
            // 治疗效果通常包括自己
            float healAmount = (float) power;
            target.heal(healAmount);

            Tiandao.LOGGER.debug("术法治疗: {} 对 {} 恢复 {} 点生命",
                blueprint.getName(), target.getName().getString(), healAmount);
        }

        // 如果没有目标，治疗自己
        if (targets.isEmpty() || !targets.contains(player)) {
            player.heal((float) power);
        }
    }

    private static void applyControlEffect(ServerPlayer player, List<LivingEntity> targets,
                                           double power, SpellBlueprint blueprint) {
        Vec3 playerPos = player.position();

        for (LivingEntity target : targets) {
            if (target == player) continue;

            // 计算方向
            Vec3 targetPos = target.position();
            Vec3 direction = playerPos.subtract(targetPos).normalize();

            // 根据元素类型应用不同控制效果
            switch (blueprint.getElement()) {
                case WIND -> {
                    // 击退
                    double knockbackStrength = power * 0.3;
                    target.setDeltaMovement(target.getDeltaMovement()
                        .add(direction.reverse().scale(knockbackStrength)));
                    target.hurtMarked = true;
                }
                default -> {
                    // 默认拉拽
                    double pullStrength = power * 0.2;
                    target.setDeltaMovement(target.getDeltaMovement()
                        .add(direction.scale(pullStrength)));
                    target.hurtMarked = true;
                }
            }
        }
    }

    private static void applyUtilityEffect(ServerPlayer player, ServerLevel level, SpellBlueprint blueprint) {
        // 实用效果（如护盾、传送等）
        // 根据具体蓝图配置实现
        Tiandao.LOGGER.debug("实用术法: {}", blueprint.getName());
    }

    private static void applySummonEffect(ServerPlayer player, ServerLevel level, SpellBlueprint blueprint) {
        // 召唤效果
        // 根据具体蓝图配置实现
        Tiandao.LOGGER.debug("召唤术法: {}", blueprint.getName());
    }

    // ========== 视觉/音效 ==========

    private static void playVisualEffects(ServerPlayer player, ServerLevel level,
                                          SpellBlueprint blueprint, Vec3 pos, Vec3 look) {
        // 根据元素类型选择粒子
        var particleOptions = switch (blueprint.getElement()) {
            case FIRE -> ParticleTypes.FLAME;
            case WATER -> ParticleTypes.DRIPPING_WATER;
            case EARTH -> ParticleTypes.SMOKE;
            case WOOD -> ParticleTypes.HAPPY_VILLAGER;
            case METAL -> ParticleTypes.CRIT;
            case LIGHTNING -> ParticleTypes.ELECTRIC_SPARK;
            case WIND -> ParticleTypes.CLOUD;
            case VOID -> ParticleTypes.PORTAL;
        };

        // 在施法位置生成粒子
        double radius = blueprint.getAreaRadius();
        for (int i = 0; i < 20; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * radius;
            double offsetY = level.random.nextDouble() * 2;
            double offsetZ = (level.random.nextDouble() - 0.5) * radius;

            level.sendParticles(particleOptions,
                pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                1, 0.0, 0.0, 0.0, 0.1);
        }
    }

    private static void playSoundEffects(ServerPlayer player, ServerLevel level, SpellBlueprint blueprint) {
        // 根据效果类型播放音效
        var sound = switch (blueprint.getEffectType()) {
            case DAMAGE -> SoundEvents.BLAZE_SHOOT;
            case HEALING -> SoundEvents.PLAYER_LEVELUP;
            case CONTROL -> SoundEvents.ENDER_DRAGON_FLAP;
            case UTILITY -> SoundEvents.ENCHANTMENT_TABLE_USE;
            case SUMMON -> SoundEvents.EVOKER_PREPARE_SUMMON;
        };

        level.playSound(null, player.blockPosition(), sound,
            SoundSource.PLAYERS, 0.8f, 1.0f + level.random.nextFloat() * 0.2f);
    }

    // ========== 结果类 ==========

    public static class ExecutionResult {
        private final boolean success;
        private final String message;
        private final double spiritCost;

        private ExecutionResult(boolean success, String message, double spiritCost) {
            this.success = success;
            this.message = message;
            this.spiritCost = spiritCost;
        }

        public static ExecutionResult success(String message, double spiritCost) {
            return new ExecutionResult(true, message, spiritCost);
        }

        public static ExecutionResult failure(String message) {
            return new ExecutionResult(false, message, 0);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public double getSpiritCost() {
            return spiritCost;
        }
    }
}
