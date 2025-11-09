package org.example.Kangnaixi.tiandao.cultivation;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;

/**
 * 小境界枚举
 * 每个大境界分为三个小境界：初期、中期、后期
 */
public enum SubRealm {
    EARLY("early", "初期", 0xCCCCCC),
    MIDDLE("middle", "中期", 0xFFFFFF),
    LATE("late", "后期", 0xFFFF00);
    
    private final String id;
    private final String displayName;
    private final int color;
    
    SubRealm(String id, String displayName, int color) {
        this.id = id;
        this.displayName = displayName;
        this.color = color;
    }
    
    /**
     * 获取小境界的ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * 获取小境界的显示名称
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * 获取小境界的显示组件（用于UI显示）
     */
    public MutableComponent getDisplayComponent() {
        return Component.literal(displayName).withStyle(style -> style.withColor(TextColor.fromRgb(color)));
    }
    
    /**
     * 获取小境界的颜色
     */
    public int getColor() {
        return color;
    }
    
    /**
     * 获取下一个小境界
     */
    public SubRealm getNext() {
        int nextIndex = this.ordinal() + 1;
        if (nextIndex < values().length) {
            return values()[nextIndex];
        }
        return null; // 已经是最后一个小境界，返回null
    }
    
    /**
     * 根据ID获取小境界
     */
    public static SubRealm fromId(String id) {
        for (SubRealm subRealm : values()) {
            if (subRealm.id.equals(id)) {
                return subRealm;
            }
        }
        return EARLY; // 默认返回初期
    }
    
    /**
     * 将旧等级转换为小境界
     * 等级 1-3 → 初期
     * 等级 4-6 → 中期
     * 等级 7-9 → 后期
     */
    public static SubRealm fromLegacyLevel(int level) {
        if (level <= 0) {
            return EARLY;
        } else if (level <= 3) {
            return EARLY;
        } else if (level <= 6) {
            return MIDDLE;
        } else {
            return LATE;
        }
    }
    
    /**
     * 将小境界转换为对应的等级范围（用于兼容性）
     * 初期 → 1-3
     * 中期 → 4-6
     * 后期 → 7-9
     */
    public int toLegacyLevelMin() {
        switch (this) {
            case EARLY:
                return 1;
            case MIDDLE:
                return 4;
            case LATE:
                return 7;
            default:
                return 1;
        }
    }
    
    public int toLegacyLevelMax() {
        switch (this) {
            case EARLY:
                return 3;
            case MIDDLE:
                return 6;
            case LATE:
                return 9;
            default:
                return 3;
        }
    }
}

