package org.example.Kangnaixi.tiandao.spell.node.execution.effect;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.example.Kangnaixi.tiandao.spell.node.NodeComponent;
import org.example.Kangnaixi.tiandao.spell.node.execution.ComponentExecutor;
import org.example.Kangnaixi.tiandao.spell.node.execution.NodeSpellContext;

/**
 * 推动效果执行器
 * 击退目标实体
 */
public class PushEffectExecutor implements ComponentExecutor {

    @Override
    public void execute(NodeComponent component, NodeSpellContext context) {
        // 获取推力参数
        Double force = component.getParameter("force", Double.class);
        if (force == null) force = 1.5;

        Vec3 center = context.getPosition();

        // 推动所有目标实体
        for (Entity entity : context.getAffectedEntities()) {
            if (entity instanceof LivingEntity living) {
                // 计算推动方向（从中心向外）
                Vec3 pushDirection = entity.position().subtract(center).normalize();

                // 应用速度
                Vec3 velocity = pushDirection.scale(force);
                entity.setDeltaMovement(entity.getDeltaMovement().add(velocity));

                // 标记为需要同步速度
                entity.hurtMarked = true;
            }
        }
    }

    @Override
    public String getId() {
        return "push";
    }

    @Override
    public boolean requiresTargets() {
        return true;
    }
}
