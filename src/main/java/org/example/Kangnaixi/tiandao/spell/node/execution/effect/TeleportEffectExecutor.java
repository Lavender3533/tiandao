package org.example.Kangnaixi.tiandao.spell.node.execution.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.example.Kangnaixi.tiandao.spell.node.NodeComponent;
import org.example.Kangnaixi.tiandao.spell.node.execution.ComponentExecutor;
import org.example.Kangnaixi.tiandao.spell.node.execution.NodeSpellContext;

/**
 * 传送效果执行器
 * 传送施法者或目标实体
 */
public class TeleportEffectExecutor implements ComponentExecutor {

    @Override
    public void execute(NodeComponent component, NodeSpellContext context) {
        // 获取传送距离参数
        Double distance = component.getParameter("distance", Double.class);
        if (distance == null) distance = 10.0;

        // 如果有目标实体，传送目标到施法者附近
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
    private void teleportCasterForward(NodeSpellContext context, double distance) {
        Entity caster = context.getCaster();
        Vec3 start = caster.getEyePosition();
        Vec3 direction = context.getDirection();
        Vec3 end = start.add(direction.scale(distance));

        // 射线检测
        ClipContext clipContext = new ClipContext(
            start,
            end,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            caster
        );

        BlockHitResult hitResult = context.getLevel().clip(clipContext);

        Vec3 targetPos;
        if (hitResult.getType() != BlockHitResult.Type.MISS) {
            // 碰到方块，传送到方块前
            targetPos = hitResult.getLocation().subtract(direction.scale(0.5));
        } else {
            targetPos = end;
        }

        // 确保位置安全
        BlockPos blockPos = BlockPos.containing(targetPos);
        if (!isSafePosition(context, blockPos)) {
            // 尝试向上找安全位置
            for (int i = 1; i <= 5; i++) {
                blockPos = blockPos.above(i);
                if (isSafePosition(context, blockPos)) {
                    targetPos = Vec3.atCenterOf(blockPos);
                    break;
                }
            }
        }

        // 执行传送
        teleportEntity(caster, targetPos, context);
    }

    /**
     * 传送目标到施法者附近
     */
    private void teleportEntitiesToCaster(NodeSpellContext context) {
        Entity caster = context.getCaster();
        Vec3 casterPos = caster.position();

        for (Entity entity : context.getAffectedEntities()) {
            if (entity instanceof LivingEntity && entity != caster) {
                // 传送到施法者前方2格
                Vec3 targetPos = casterPos.add(context.getDirection().scale(2.0));
                teleportEntity(entity, targetPos, context);
            }
        }
    }

    /**
     * 执行实体传送
     */
    private void teleportEntity(Entity entity, Vec3 targetPos, NodeSpellContext context) {
        // 播放音效（出发位置）
        context.getLevel().playSound(
            null,
            entity.getX(), entity.getY(), entity.getZ(),
            SoundEvents.ENDERMAN_TELEPORT,
            SoundSource.PLAYERS,
            1.0F, 1.0F
        );

        // 执行传送
        if (entity instanceof ServerPlayer player) {
            player.teleportTo(targetPos.x, targetPos.y, targetPos.z);
        } else {
            entity.teleportTo(targetPos.x, targetPos.y, targetPos.z);
        }

        // 播放音效（目标位置）
        context.getLevel().playSound(
            null,
            targetPos.x, targetPos.y, targetPos.z,
            SoundEvents.ENDERMAN_TELEPORT,
            SoundSource.PLAYERS,
            1.0F, 1.0F
        );
    }

    /**
     * 检查位置是否安全
     */
    private boolean isSafePosition(NodeSpellContext context, BlockPos pos) {
        return !context.getLevel().getBlockState(pos).isSuffocating(context.getLevel(), pos)
            && !context.getLevel().getBlockState(pos.above()).isSuffocating(context.getLevel(), pos.above());
    }

    @Override
    public String getId() {
        return "teleport";
    }

    @Override
    public boolean requiresTargets() {
        return false;  // 可以无目标传送自己
    }
}
