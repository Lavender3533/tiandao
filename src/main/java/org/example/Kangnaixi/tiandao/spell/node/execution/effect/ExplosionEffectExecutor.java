package org.example.Kangnaixi.tiandao.spell.node.execution.effect;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.example.Kangnaixi.tiandao.spell.node.NodeComponent;
import org.example.Kangnaixi.tiandao.spell.node.execution.ComponentExecutor;
import org.example.Kangnaixi.tiandao.spell.node.execution.NodeSpellContext;

/**
 * 爆炸效果执行器
 * 在位置产生爆炸
 */
public class ExplosionEffectExecutor implements ComponentExecutor {

    @Override
    public void execute(NodeComponent component, NodeSpellContext context) {
        // 获取爆炸威力参数
        Double power = component.getParameter("power", Double.class);
        if (power == null) power = 3.0;

        // 如果有目标实体，在每个实体位置爆炸
        if (!context.getAffectedEntities().isEmpty()) {
            for (Entity entity : context.getAffectedEntities()) {
                context.getLevel().explode(
                    context.getCaster(),
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    power.floatValue(),
                    net.minecraft.world.level.Level.ExplosionInteraction.MOB
                );
            }
        } else {
            // 否则在当前位置爆炸
            Vec3 pos = context.getPosition();
            context.getLevel().explode(
                context.getCaster(),
                pos.x,
                pos.y,
                pos.z,
                power.floatValue(),
                net.minecraft.world.level.Level.ExplosionInteraction.MOB
            );
        }
    }

    @Override
    public String getId() {
        return "explosion";
    }

    @Override
    public boolean requiresTargets() {
        return false;  // 可以无目标爆炸
    }
}
