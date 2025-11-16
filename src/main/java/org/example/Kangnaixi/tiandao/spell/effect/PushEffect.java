package org.example.Kangnaixi.tiandao.spell.effect;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.example.Kangnaixi.tiandao.spell.rune.RuneContext;

/**
 * 推动效果执行器
 */
public class PushEffect implements EffectExecutor {

    private final double baseForce;

    public PushEffect(double baseForce) {
        this.baseForce = baseForce;
    }

    @Override
    public void execute(RuneContext context, double power) {
        double finalForce = baseForce * power;
        Vec3 casterPos = context.getCaster().position();

        // 推开所有受影响的实体
        for (Entity entity : context.getAffectedEntities()) {
            if (entity instanceof LivingEntity) {
                // 计算推动方向（从施法者指向目标）
                Vec3 pushDirection = entity.position().subtract(casterPos).normalize();

                // 应用推动力
                Vec3 pushVelocity = pushDirection.scale(finalForce);
                entity.setDeltaMovement(entity.getDeltaMovement().add(pushVelocity));
                entity.hurtMarked = true;

                // TODO: 添加推动粒子效果
                // TODO: 添加音效
            }
        }
    }

    @Override
    public String getName() {
        return "推动";
    }

    @Override
    public String getDescription() {
        return "将目标推开，力度 " + baseForce;
    }

    @Override
    public boolean requiresTarget() {
        return true;
    }
}
