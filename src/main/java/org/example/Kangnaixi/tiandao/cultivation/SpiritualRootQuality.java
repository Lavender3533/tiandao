package org.example.Kangnaixi.tiandao.cultivation;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;

/**
 * 灵根品质枚举
 */
public enum SpiritualRootQuality {
    POOR("poor", "劣质", 0x808080, 0.5f, 0.5f),        // 灰色，30%概率
    NORMAL("normal", "普通", 0xCCCCCC, 1.0f, 1.0f),    // 白色，50%概率
    GOOD("good", "优质", 0x55FF55, 1.5f, 1.5f),        // 绿色，15%概率
    EXCELLENT("excellent", "极品", 0xAA00FF, 2.0f, 2.0f), // 紫色，4%概率
    PERFECT("perfect", "天灵根", 0xFFD700, 3.0f, 3.0f); // 金色，1%概率
    
    private final String id;
    private final String displayName;
    private final int color; // 显示颜色
    private final float qualityMultiplier; // 品质倍数，影响修炼速度
    private final float spiritRecoveryBonus; // 灵力恢复加成
    
    SpiritualRootQuality(String id, String displayName, int color, float qualityMultiplier, float spiritRecoveryBonus) {
        this.id = id;
        this.displayName = displayName;
        this.color = color;
        this.qualityMultiplier = qualityMultiplier;
        this.spiritRecoveryBonus = spiritRecoveryBonus;
    }
    
    /**
     * 获取品质的ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * 获取品质的显示名称
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * 获取品质的显示组件（用于UI显示）
     */
    public MutableComponent getDisplayComponent() {
        return Component.literal(displayName).withStyle(style -> style.withColor(TextColor.fromRgb(color)));
    }
    
    /**
     * 获取品质的颜色
     */
    public int getColor() {
        return color;
    }
    
    /**
     * 获取品质倍数
     */
    public float getQualityMultiplier() {
        return qualityMultiplier;
    }
    
    /**
     * 获取修炼速度加成
     */
    public float getCultivationBonus() {
        return qualityMultiplier;
    }
    
    /**
     * 获取灵力恢复加成
     */
    public float getSpiritRecoveryBonus() {
        return spiritRecoveryBonus;
    }
    
    /**
     * 根据ID获取品质
     */
    public static SpiritualRootQuality fromId(String id) {
        for (SpiritualRootQuality quality : values()) {
            if (quality.id.equals(id)) {
                return quality;
            }
        }
        return NORMAL; // 默认返回普通品质
    }
    
    /**
     * 随机获取一个品质
     * 概率分布：劣质30%，普通50%，优质15%，极品4%，天灵根1%
     */
    public static SpiritualRootQuality randomQuality() {
        double random = Math.random();
        if (random < 0.30) {
            return POOR; // 30%概率
        } else if (random < 0.80) {
            return NORMAL; // 50%概率
        } else if (random < 0.95) {
            return GOOD; // 15%概率
        } else if (random < 0.99) {
            return EXCELLENT; // 4%概率
        } else {
            return PERFECT; // 1%概率
        }
    }
}