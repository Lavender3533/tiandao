package org.example.Kangnaixi.tiandao.spell;

import java.util.*;

/**
 * 术法效果系统
 * 包含各种附加效果如破甲、击退、治疗等
 */
public class SpellEffects {

    private final Map<String, SpellEffect> effects;

    public SpellEffects(List<SpellEffect> effectList) {
        this.effects = new HashMap<>();
        for (SpellEffect effect : effectList) {
            effects.put(effect.getId(), effect);
        }
    }

    /**
     * 检查是否具有特定效果
     */
    public boolean hasEffect(String effectId) {
        return effects.containsKey(effectId);
    }

    /**
     * 获取效果数量
     */
    public int getCount() {
        return effects.size();
    }

    /**
     * 获取效果强度
     */
    public double getEffectPower(String effectId) {
        SpellEffect effect = effects.get(effectId);
        return effect != null ? effect.getPower() : 0.0;
    }

    /**
     * 获取所有效果
     */
    public Collection<SpellEffect> getAll() {
        return effects.values();
    }

    /**
     * 检查是否具有控制类效果
     */
    public boolean hasControlEffects() {
        return hasEffect("armor_break") || hasEffect("knockback");
    }

    /**
     * 检查是否具有回复类效果
     */
    public boolean hasHealingEffects() {
        return hasEffect("heal_up") || hasEffect("shield") || hasEffect("lifesteal");
    }

    /**
     * 单个效果定义
     */
    public static class SpellEffect {
        private final String id;
        private final String displayName;
        private final double power;

        public SpellEffect(String id, String displayName, double power) {
            this.id = id;
            this.displayName = displayName;
            this.power = power;
        }

        public String getId() {
            return id;
        }

        public String getDisplayName() {
            return displayName;
        }

        public double getPower() {
            return power;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            SpellEffect that = (SpellEffect) obj;
            return id.equals(that.id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }
    }

    // 预定义效果常量
    public static final class EffectIds {
        // 控制类/输出类
        public static final String ARMOR_BREAK = "armor_break";
        public static final String KNOCKBACK = "knockback";
        public static final String AOE_UP = "aoe_up";
        public static final String DOT = "dot";

        // 回复与辅助
        public static final String HEAL_UP = "heal_up";
        public static final String SHIELD = "shield";
        public static final String MOVE_SPEED = "move_speed";
        public static final String LIFESTEAL = "lifesteal";
    }

    @Override
    public String toString() {
        return "SpellEffects" + effects.keySet();
    }
}