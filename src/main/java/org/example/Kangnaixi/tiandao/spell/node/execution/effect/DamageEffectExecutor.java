package org.example.Kangnaixi.tiandao.spell.node.execution.effect;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.example.Kangnaixi.tiandao.spell.node.NodeComponent;
import org.example.Kangnaixi.tiandao.spell.node.execution.ComponentExecutor;
import org.example.Kangnaixi.tiandao.spell.node.execution.NodeSpellContext;

/**
 * 伤害效果执行器
 * 对目标造成伤害
 */
public class DamageEffectExecutor implements ComponentExecutor {

    @Override
    public void execute(NodeComponent component, NodeSpellContext context) {
        // 获取伤害参数
        Double damage = component.getParameter("damage", Double.class);
        if (damage == null) damage = 5.0;

        // 属性匹配加成
        // TODO: 获取玩家灵根属性进行匹配

        DamageSource damageSource = context.getLevel().damageSources()
            .playerAttack(context.getCaster());

        // 对所有目标实体造成伤害
        for (Entity entity : context.getAffectedEntities()) {
            if (entity instanceof LivingEntity living) {
                living.hurt(damageSource, damage.floatValue());
            }
        }
    }

    @Override
    public String getId() {
        return "damage";
    }

    @Override
    public boolean requiresTargets() {
        return true;
    }
}
