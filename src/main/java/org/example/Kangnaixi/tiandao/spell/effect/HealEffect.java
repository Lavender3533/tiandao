package org.example.Kangnaixi.tiandao.spell.effect;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.example.Kangnaixi.tiandao.spell.rune.RuneContext;

/**
 * 治疗效果执行器
 */
public class HealEffect implements EffectExecutor {

    private final double baseHealing;

    public HealEffect(double baseHealing) {
        this.baseHealing = baseHealing;
    }

    @Override
    public void execute(RuneContext context, double power) {
        double finalHealing = baseHealing * power;

        // 对所有受影响的实体进行治疗
        for (Entity entity : context.getAffectedEntities()) {
            if (entity instanceof LivingEntity livingEntity) {
                // 恢复生命值
                livingEntity.heal((float) finalHealing);

                // TODO: 添加治疗粒子效果
                // TODO: 添加音效
            }
        }
    }

    @Override
    public String getName() {
        return "治疗";
    }

    @Override
    public String getDescription() {
        return "恢复目标 " + baseHealing + " 点生命值";
    }

    @Override
    public boolean requiresTarget() {
        return true;
    }
}
