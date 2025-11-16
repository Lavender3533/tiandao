package org.example.Kangnaixi.tiandao.spell.node.execution.effect;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.example.Kangnaixi.tiandao.spell.node.NodeComponent;
import org.example.Kangnaixi.tiandao.spell.node.execution.ComponentExecutor;
import org.example.Kangnaixi.tiandao.spell.node.execution.NodeSpellContext;

/**
 * 治疗效果执行器
 * 恢复目标生命值
 */
public class HealEffectExecutor implements ComponentExecutor {

    @Override
    public void execute(NodeComponent component, NodeSpellContext context) {
        // 获取治疗量参数
        Double healing = component.getParameter("healing", Double.class);
        if (healing == null) healing = 5.0;

        // 属性匹配加成（木属性）
        // TODO: 获取玩家灵根属性，木属性额外加成

        // 治疗所有目标实体
        for (Entity entity : context.getAffectedEntities()) {
            if (entity instanceof LivingEntity living) {
                living.heal(healing.floatValue());
            }
        }
    }

    @Override
    public String getId() {
        return "heal";
    }

    @Override
    public boolean requiresTargets() {
        return true;
    }
}
