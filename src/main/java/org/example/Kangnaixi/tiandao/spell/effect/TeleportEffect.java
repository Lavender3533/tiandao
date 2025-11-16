package org.example.Kangnaixi.tiandao.spell.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.example.Kangnaixi.tiandao.spell.rune.RuneContext;

/**
 * 传送效果执行器
 * 将施法者或目标传送到指定位置
 */
public class TeleportEffect implements EffectExecutor {

    private final double maxDistance;

    public TeleportEffect(double maxDistance) {
        this.maxDistance = maxDistance;
    }

    @Override
    public void execute(RuneContext context, double power) {
        // 传送距离受power影响
        double distance = maxDistance * power;

        // 如果有目标实体，传送目标实体到施法者附近
        if (!context.getAffectedEntities().isEmpty()) {
            teleportEntitiesToCaster(context);
        } else {
            // 否则传送施法者向前
            teleportCasterForward(context, distance);
        }
    }

    /**
     * 传送施法者向前
     */
    private void teleportCasterForward(RuneContext context, double distance) {
        Entity caster = context.getCaster();

        // 获取视线方向
        Vec3 lookAngle = caster.getLookAngle();
        Vec3 startPos = caster.getEyePosition();
        Vec3 endPos = startPos.add(lookAngle.scale(distance));

        // 检测视线上的方块
        ClipContext clipContext = new ClipContext(
            startPos,
            endPos,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            caster
        );

        BlockHitResult hitResult = context.getLevel().clip(clipContext);

        // 计算传送目标位置
        Vec3 targetPos;
        if (hitResult.getType() != BlockHitResult.Type.MISS) {
            // 如果碰到方块，传送到方块前面
            targetPos = hitResult.getLocation();
            // 稍微往后一点，避免卡在方块里
            targetPos = targetPos.subtract(lookAngle.scale(0.5));
        } else {
            // 如果没有碰到，传送到最远距离
            targetPos = endPos;
        }

        // 确保传送位置安全（不在方块内）
        BlockPos blockPos = BlockPos.containing(targetPos);
        if (!isSafePosition(context.getLevel(), blockPos)) {
            // 尝试向上寻找安全位置
            for (int i = 0; i < 5; i++) {
                blockPos = blockPos.above();
                if (isSafePosition(context.getLevel(), blockPos)) {
                    targetPos = Vec3.atCenterOf(blockPos);
                    break;
                }
            }
        }

        // 执行传送
        teleportEntity(caster, targetPos, context);
    }

    /**
     * 传送目标实体到施法者附近
     */
    private void teleportEntitiesToCaster(RuneContext context) {
        Entity caster = context.getCaster();
        Vec3 casterPos = caster.position();

        for (Entity entity : context.getAffectedEntities()) {
            if (entity instanceof LivingEntity && entity != caster) {
                // 计算实体到施法者的方向
                Vec3 direction = casterPos.subtract(entity.position()).normalize();
                // 传送到施法者前方2格
                Vec3 targetPos = casterPos.add(direction.scale(2.0));

                // 执行传送
                teleportEntity(entity, targetPos, context);
            }
        }
    }

    /**
     * 执行实体传送
     */
    private void teleportEntity(Entity entity, Vec3 targetPos, RuneContext context) {
        // 播放传送音效（出发位置）
        context.getLevel().playSound(
            null,
            entity.getX(), entity.getY(), entity.getZ(),
            SoundEvents.ENDERMAN_TELEPORT,
            SoundSource.PLAYERS,
            1.0F, 1.0F
        );

        // 执行传送
        if (entity instanceof ServerPlayer player) {
            // 玩家传送
            player.teleportTo(targetPos.x, targetPos.y, targetPos.z);
        } else {
            // 其他实体传送
            entity.teleportTo(targetPos.x, targetPos.y, targetPos.z);
        }

        // 播放传送音效（目标位置）
        context.getLevel().playSound(
            null,
            targetPos.x, targetPos.y, targetPos.z,
            SoundEvents.ENDERMAN_TELEPORT,
            SoundSource.PLAYERS,
            1.0F, 1.0F
        );

        // TODO: 添加传送粒子效果
        // if (context.getLevel() instanceof ServerLevel serverLevel) {
        //     serverLevel.sendParticles(
        //         ParticleTypes.PORTAL,
        //         targetPos.x, targetPos.y + 1, targetPos.z,
        //         20, 0.5, 1.0, 0.5, 0.1
        //     );
        // }
    }

    /**
     * 检查位置是否安全（不在方块内）
     */
    private boolean isSafePosition(net.minecraft.world.level.Level level, BlockPos pos) {
        // 检查脚下和头顶是否有空间
        return !level.getBlockState(pos).isSuffocating(level, pos)
            && !level.getBlockState(pos.above()).isSuffocating(level, pos.above());
    }

    @Override
    public String getName() {
        return "传送";
    }

    @Override
    public String getDescription() {
        return "传送施法者向前，或将目标传送到施法者附近";
    }

    @Override
    public boolean requiresTarget() {
        return false; // 传送不需要目标，可以直接传送施法者
    }
}
