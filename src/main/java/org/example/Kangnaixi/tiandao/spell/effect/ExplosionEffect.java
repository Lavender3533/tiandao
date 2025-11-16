package org.example.Kangnaixi.tiandao.spell.effect;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.example.Kangnaixi.tiandao.spell.rune.RuneContext;

/**
 * 爆炸效果执行器
 * 在指定位置产生爆炸
 */
public class ExplosionEffect implements EffectExecutor {

    private final double baseExplosionPower;

    public ExplosionEffect(double baseExplosionPower) {
        this.baseExplosionPower = baseExplosionPower;
    }

    @Override
    public void execute(RuneContext context, double power) {
        Level level = context.getLevel();

        // 获取爆炸位置
        Vec3 position;
        if (context.hasVariable("target_position")) {
            position = (Vec3) context.getVariable("target_position");
        } else {
            position = context.getPosition();
        }

        // 计算最终爆炸威力
        float explosionPower = (float) (baseExplosionPower * power);

        // 如果影响了实体，在实体位置爆炸
        if (!context.getAffectedEntities().isEmpty()) {
            for (Entity entity : context.getAffectedEntities()) {
                // 在每个实体位置产生爆炸
                level.explode(
                    context.getCaster(),          // 爆炸源实体
                    entity.getX(),                // X坐标
                    entity.getY(),                // Y坐标
                    entity.getZ(),                // Z坐标
                    explosionPower,               // 爆炸威力
                    Level.ExplosionInteraction.MOB // 爆炸交互模式（生物模式，破坏方块但不掉落）
                );
            }
        } else {
            // 如果没有实体，在指定位置爆炸
            level.explode(
                context.getCaster(),
                position.x,
                position.y,
                position.z,
                explosionPower,
                Level.ExplosionInteraction.MOB
            );
        }
    }

    @Override
    public String getName() {
        return "爆炸";
    }

    @Override
    public String getDescription() {
        return "在目标位置产生爆炸，造成范围伤害";
    }

    @Override
    public boolean requiresTarget() {
        return false; // 爆炸不需要目标，可以在空地产生
    }
}
