package org.example.Kangnaixi.tiandao.cultivation;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import java.util.Random;

/**
 * 灵根类型枚举
 */
public enum SpiritualRootType {
    NONE("none", "无灵根", 0.0f, 0x808080), // 灰色
    GOLD("gold", "金灵根", 1.1f, 0xFFD700), // 金色
    WOOD("wood", "木灵根", 1.2f, 0x228B22), // 森林绿
    WATER("water", "水灵根", 1.15f, 0x4169E1), // 皇家蓝
    FIRE("fire", "火灵根", 1.05f, 0xFF4500), // 橙红色
    EARTH("earth", "土灵根", 1.0f, 0x8B4513); // 鞍褐色
    //暂时先硬编码，以后再改用配置文件加载
    private final String id;
    private final String displayName;
    private final float recoveryBonus; // 灵根类型的恢复加成
    private final int color; // 灵根类型对应的颜色
    
    SpiritualRootType(String id, String displayName, float recoveryBonus, int color) {
        this.id = id;
        this.displayName = displayName;
        this.recoveryBonus = recoveryBonus;
        this.color = color;
    }
    
    /**
     * 获取灵根类型的ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * 获取灵根类型的显示名称
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * 获取灵根类型的显示组件（用于UI显示）
     */
    public MutableComponent getDisplayComponent() {
        return Component.literal(displayName);
    }
    
    /**
     * 获取灵根类型的恢复加成
     */
    public float getRecoveryBonus() {
        return recoveryBonus;
    }
    
    /**
     * 获取灵根类型对应的颜色
     */
    public int getColor() {
        return color;
    }
    
    /**
     * 根据ID获取灵根类型
     */
    public static SpiritualRootType fromId(String id) {
        for (SpiritualRootType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return NONE; // 默认返回无灵根
    }
    
    /**
     * 随机获取一个灵根类型（排除NONE）
     */
    public static SpiritualRootType getRandomRoot() {
        Random random = new Random();
        // 获取所有非NONE的灵根类型
        SpiritualRootType[] roots = new SpiritualRootType[] {
            GOLD, WOOD, WATER, FIRE, EARTH
        };
        
        // 随机选择一个
        return roots[random.nextInt(roots.length)];
    }
}