package org.example.Kangnaixi.tiandao.spell.runtime.engine;

import org.example.Kangnaixi.tiandao.spell.runtime.SourceType;

import java.util.EnumMap;
import java.util.Map;

/**
 * 术法来源执行器注册器
 */
public final class SourceExecutors {

    private static final Map<SourceType, SourceExecutor> EXECUTORS = new EnumMap<>(SourceType.class);

    static {
        // 注册基础实现
        register(SourceType.FINGER, SourceExecutor.NO_OP);
        register(SourceType.SEAL, SourceExecutors::applySeal);
        register(SourceType.WEAPON_SWORD, SourceExecutors::applyWeaponSword);
        register(SourceType.TALISMAN, SourceExecutor.NO_OP);
        register(SourceType.ARRAY, SourceExecutor.NO_OP);
        register(SourceType.RUNE_CORE, SourceExecutor.NO_OP);
    }

    /**
     * 注册来源执行器
     */
    public static void register(SourceType type, SourceExecutor executor) {
        EXECUTORS.put(type, executor);
    }

    /**
     * 获取来源执行器
     */
    public static SourceExecutor get(SourceType type) {
        return EXECUTORS.getOrDefault(type, SourceExecutor.NO_OP);
    }

    // ========== 基础实现 ==========

    /**
     * 法印来源：基础术法，无特殊加成
     */
    private static void applySeal(SpellContext ctx) {
        ctx.putData("source.seal", true);
        // 法印术法，基础施法方式，无特殊效果
    }

    /**
     * 武器-剑来源：剑类武器施法，增加伤害
     */
    private static void applyWeaponSword(SpellContext ctx) {
        ctx.putData("source.weapon_sword", true);
        // 检查是否持剑
        boolean holdingSword = ctx.getCaster().getMainHandItem().toString().toLowerCase().contains("sword");
        if (holdingSword) {
            // 持剑时伤害提升30%
            ctx.setDamage(ctx.getDamage() * 1.3);
            ctx.putData("weapon.sword", true);
        }
    }

    private SourceExecutors() {}
}
