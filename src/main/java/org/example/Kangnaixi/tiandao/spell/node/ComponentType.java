package org.example.Kangnaixi.tiandao.spell.node;

/**
 * 组件类型
 */
public enum ComponentType {
    /**
     * 投射组件 - 定义传播或命中方式
     */
    PROJECTILE("投射", 0xFF4FC3F7),

    /**
     * 效果组件 - 对目标施加效果
     */
    EFFECT("效果", 0xFFFF6B6B),

    /**
     * 修饰组件 - 修改术法行为（增强、延时等）
     */
    MODIFIER("修饰", 0xFFFFD93D);

    private final String displayName;
    private final int color;

    ComponentType(String displayName, int color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getColor() {
        return color;
    }
}
