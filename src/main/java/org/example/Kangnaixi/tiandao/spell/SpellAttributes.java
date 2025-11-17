package org.example.Kangnaixi.tiandao.spell;

import java.util.*;

/**
 * 术法属性系统
 * 包含五行、阴阳、意境等道性属性
 */
public class SpellAttributes {

    private final Map<String, SpellAttribute> attributes;

    public SpellAttributes(List<SpellAttribute> attributeList) {
        this.attributes = new HashMap<>();
        for (SpellAttribute attr : attributeList) {
            attributes.put(attr.getId(), attr);
        }
    }

    /**
     * 检查是否具有特定属性
     */
    public boolean hasAttribute(String attributeId) {
        return attributes.containsKey(attributeId);
    }

    /**
     * 获取属性数量
     */
    public int getCount() {
        return attributes.size();
    }

    /**
     * 获取属性加成值
     */
    public double getAttributeBonus(String attributeId) {
        SpellAttribute attr = attributes.get(attributeId);
        return attr != null ? attr.getMagnitude() : 0.0;
    }

    /**
     * 获取所有属性
     */
    public Collection<SpellAttribute> getAll() {
        return attributes.values();
    }

    /**
     * 获取伤害缩放总和
     */
    public double getTotalDamageScaling() {
        return attributes.values().stream()
            .mapToDouble(SpellAttribute::getDamageScaling)
            .sum();
    }

    /**
     * 检查属性组合（用于特殊效果）
     */
    public boolean hasAttributeCombination(String... attributeIds) {
        for (String id : attributeIds) {
            if (!hasAttribute(id)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 单个属性定义
     */
    public static class SpellAttribute {
        private final String id;
        private final String displayName;
        private final String type; // element, yin_yang, intent
        private final double magnitude;
        private final double damageScaling;

        public SpellAttribute(String id, String displayName, String type,
                            double magnitude, double damageScaling) {
            this.id = id;
            this.displayName = displayName;
            this.type = type;
            this.magnitude = magnitude;
            this.damageScaling = damageScaling;
        }

        public String getId() {
            return id;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getType() {
            return type;
        }

        public double getMagnitude() {
            return magnitude;
        }

        public double getDamageScaling() {
            return damageScaling;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            SpellAttribute that = (SpellAttribute) obj;
            return id.equals(that.id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }
    }

    // 预定义属性常量
    public static final class AttributeIds {
        // 五行
        public static final String METAL = "metal";
        public static final String WOOD = "wood";
        public static final String WATER = "water";
        public static final String FIRE = "fire";
        public static final String EARTH = "earth";

        // 阴阳
        public static final String YANG = "yang";
        public static final String YIN = "yin";

        // 意境
        public static final String THUNDER = "thunder";
        public static final String SWORD = "sword";
        public static final String WIND = "wind";
        public static final String WOOD_SPIRIT = "wood_spirit";
    }

    @Override
    public String toString() {
        return "SpellAttributes" + attributes.keySet();
    }
}