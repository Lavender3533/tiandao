package org.example.Kangnaixi.tiandao.starchart;

/**
 * 星盘节点分类（灵脉线 + 成品术法）
 *
 * 布局（4类别弧形）：
 * - EFFECT (左): 效果灵脉线
 * - FORM (中左): 形态灵脉线
 * - MODIFIER (中右): 调制灵脉线
 * - BLUEPRINT (右): 成品术法存储区
 *
 * 注：源（灵力来源）在手盘中心单独选择，不在星盘中
 */
public enum StarNodeCategory {
    EFFECT("效果", 0xFF6347),      // 番茄红
    FORM("形态", 0x87CEEB),        // 天蓝色
    MODIFIER("调制", 0x9370DB),    // 紫色
    BLUEPRINT("成品", 0x00FF7F);   // 春绿色

    private final String displayName;
    private final int color;

    StarNodeCategory(String displayName, int color) {
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
