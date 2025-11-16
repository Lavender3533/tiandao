package org.example.Kangnaixi.tiandao.spell.effect;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.example.Kangnaixi.tiandao.spell.rune.RuneContext;

import java.util.List;

/**
 * 效果执行器接口
 * 所有术法效果都实现这个接口
 */
public interface EffectExecutor {

    /**
     * 执行效果
     * @param context 符文执行上下文
     * @param power 效果威力倍数
     */
    void execute(RuneContext context, double power);

    /**
     * 获取效果名称
     */
    String getName();

    /**
     * 获取效果描述
     */
    String getDescription();

    /**
     * 是否需要目标实体
     */
    default boolean requiresTarget() {
        return true;
    }

    /**
     * 效果类型
     */
    enum EffectType {
        DAMAGE,      // 伤害
        HEAL,        // 治疗
        PUSH,        // 推动
        EXPLOSION,   // 爆炸
        TELEPORT,    // 传送
        BUFF,        // 增益
        DEBUFF       // 减益
    }
}
