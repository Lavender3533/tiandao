package org.example.Kangnaixi.tiandao.spell.effect;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.example.Kangnaixi.tiandao.spell.rune.RuneContext;

/**
 * 伤害效果执行器
 */
public class DamageEffect implements EffectExecutor {

    private final double baseDamage;

    public DamageEffect(double baseDamage) {
        this.baseDamage = baseDamage;
    }

    @Override
    public void execute(RuneContext context, double power) {
        double finalDamage = baseDamage * power;

        // 对所有受影响的实体造成伤害
        for (Entity entity : context.getAffectedEntities()) {
            if (entity instanceof LivingEntity livingEntity) {
                // 创建伤害源
                DamageSource damageSource = context.getLevel().damageSources()
                    .playerAttack(context.getCaster());

                // 造成伤害
                livingEntity.hurt(damageSource, (float) finalDamage);

                // TODO: 添加粒子效果
                // TODO: 添加音效
            }
        }
    }

    @Override
    public String getName() {
        return "伤害";
    }

    @Override
    public String getDescription() {
        return "对目标造成 " + baseDamage + " 点物理伤害";
    }

    @Override
    public boolean requiresTarget() {
        return true;
    }
}
