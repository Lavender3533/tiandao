package org.example.Kangnaixi.tiandao.cultivation;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;

/**
 * 修仙境界枚举
 */
public enum CultivationRealm {
    MORTAL("mortal", "凡人", 0, 0xCCCCCC, 100, 1.0f),
    QI_CONDENSATION("qi_condensation", "练气", 9, 0x55FF55, 200, 1.2f),
    FOUNDATION_BUILDING("foundation_building", "筑基", 9, 0x5555FF, 500, 1.5f),
    GOLDEN_CORE("golden_core", "金丹", 9, 0xFFD700, 1000, 2.0f),
    NASCENT_SOUL("nascent_soul", "元婴", 9, 0xFF55FF, 2000, 3.0f),
    SPIRITUAL_TRANSFORMATION("spiritual_transformation", "化神", 9, 0xFF5555, 5000, 5.0f),
    VOID_TUNING("void_tuning", "炼虚", 9, 0x55FFFF, 10000, 8.0f),
    BODY_INTEGRATION("body_integration", "合体", 9, 0xFFAA00, 20000, 12.0f),
    MAHAYANA("mahayana", "大乘", 9, 0xFFFFFF, 50000, 20.0f),
    TRIBULATION("tribulation", "渡劫", 9, 0xFF0000, 100000, 30.0f),
    IMMORTAL("immortal", "仙人", 0, 0x00FF00, Float.MAX_VALUE, 50.0f);
    
    private final String id;
    private final String displayName;
    private final int maxLevel; // 境界内最大等级
    private final int color; // 显示颜色
    private final float baseMaxEnergy; // 基础最大灵力值
    private final float energyMultiplier; // 灵力倍数，影响灵力上限
    
    CultivationRealm(String id, String displayName, int maxLevel, int color, float baseMaxEnergy, float energyMultiplier) {
        this.id = id;
        this.displayName = displayName;
        this.maxLevel = maxLevel;
        this.color = color;
        this.baseMaxEnergy = baseMaxEnergy;
        this.energyMultiplier = energyMultiplier;
    }
    
    /**
     * 获取境界的ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * 获取境界的显示名称
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * 获取境界的显示组件（用于UI显示）
     */
    public MutableComponent getDisplayComponent() {
        return Component.literal(displayName).withStyle(style -> style.withColor(TextColor.fromRgb(color)));
    }
    
    /**
     * 获取境界的颜色
     */
    public int getColor() {
        return color;
    }
    
    /**
     * 获取境界内最大等级
     */
    public int getMaxLevel() {
        return maxLevel;
    }
    
    /**
     * 获取基础最大灵力值
     */
    public float getBaseMaxEnergy() {
        return baseMaxEnergy;
    }
    
    /**
     * 获取灵力倍数
     */
    public float getEnergyMultiplier() {
        return energyMultiplier;
    }
    
    /**
     * 获取下一个境界
     */
    public CultivationRealm getNext() {
        int nextIndex = this.ordinal() + 1;
        if (nextIndex < values().length) {
            return values()[nextIndex];
        }
        return null; // 已经是最高境界，返回null
    }
    
    /**
     * 获取下一个境界
     */
    public CultivationRealm getNextRealm() {
        int nextIndex = this.ordinal() + 1;
        if (nextIndex < values().length) {
            return values()[nextIndex];
        }
        return this; // 已经是最高境界，返回自身
    }
    
    /**
     * 获取前一个境界
     */
    public CultivationRealm getPreviousRealm() {
        int prevIndex = this.ordinal() - 1;
        if (prevIndex >= 0) {
            return values()[prevIndex];
        }
        return this; // 已经是最低境界，返回自身
    }
    
    /**
     * 获取基础灵力值
     */
    public double getBaseSpiritPower() {
        return baseMaxEnergy;
    }
    
    /**
     * 获取修炼速度加成
     */
    public float getCultivationBonus() {
        return 1.0f + (this.ordinal() * 0.1f); // 每个境界增加10%修炼速度
    }
    
    /**
     * 获取灵力恢复加成
     */
    public float getSpiritRecoveryBonus() {
        return 1.0f + (this.ordinal() * 0.05f); // 每个境界增加5%灵力恢复速度
    }
    
    /**
     * 获取升级所需进度（已弃用，保留用于兼容性）
     * @deprecated 使用新的小境界系统，此方法仅用于向后兼容
     */
    @Deprecated
    public double getRequiredProgress(int level) {
        // 基础需求随等级提升而增加
        return 100 * (1 + level * 0.2);
    }
    
    /**
     * 根据ID获取境界
     */
    public static CultivationRealm fromId(String id) {
        for (CultivationRealm realm : values()) {
            if (realm.id.equals(id)) {
                return realm;
            }
        }
        return MORTAL; // 默认返回凡人境界
    }
    
    /**
     * 计算突破到下一境界所需的经验值（已弃用，保留用于兼容性）
     * @deprecated 使用新的小境界系统，此方法仅用于向后兼容
     */
    @Deprecated
    public float getBreakthroughRequirement() {
        // 基础需求随境界提升而指数增长
        return (float) (1000 * Math.pow(2.0, this.ordinal()));
    }
    
    /**
     * 获取基础经验值（用于计算小境界经验需求）
     * 基础值 = 100 × (境界序号 + 1)
     */
    public int getBaseExperience() {
        if (this == MORTAL) {
            return 0; // 凡人无经验需求
        }
        return 100 * (this.ordinal());
    }
    
    /**
     * 检查是否为特殊境界（凡人、渡劫、仙人）
     */
    public boolean isSpecialRealm() {
        return this == MORTAL || this == TRIBULATION || this == IMMORTAL;
    }
}