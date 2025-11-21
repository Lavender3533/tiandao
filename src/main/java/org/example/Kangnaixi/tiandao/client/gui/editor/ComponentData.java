package org.example.Kangnaixi.tiandao.client.gui.editor;

import java.util.ArrayList;
import java.util.List;

/**
 * 组件数据 - 存储组件的显示信息
 */
public class ComponentData {
    private final String id;              // 组件ID（如 "tiandao:source/finger"）
    private final String displayName;     // 显示名称（如 "指诀"）
    private final String icon;            // 图标（Unicode字符或文本）
    private final String shortDesc;       // 简短描述（1-2行）
    private final List<String> tooltipLines; // 详细说明（Tooltip）
    private final String spiritImpact;    // 灵力影响说明

    public ComponentData(String id, String displayName, String icon, String shortDesc,
                        List<String> tooltipLines, String spiritImpact) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
        this.shortDesc = shortDesc;
        this.tooltipLines = tooltipLines != null ? tooltipLines : new ArrayList<>();
        this.spiritImpact = spiritImpact;
    }

    // 快捷构造方法
    public static ComponentData create(String id, String displayName, String icon, String shortDesc) {
        return new ComponentData(id, displayName, icon, shortDesc, new ArrayList<>(), "无影响");
    }

    public static ComponentData create(String id, String displayName, String icon, String shortDesc,
                                      String spiritImpact) {
        return new ComponentData(id, displayName, icon, shortDesc, new ArrayList<>(), spiritImpact);
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIcon() {
        return icon;
    }

    public String getShortDesc() {
        return shortDesc;
    }

    public List<String> getTooltipLines() {
        return tooltipLines;
    }

    public String getSpiritImpact() {
        return spiritImpact;
    }

    // 添加Tooltip行
    public ComponentData withTooltip(String... lines) {
        List<String> newTooltip = new ArrayList<>(this.tooltipLines);
        for (String line : lines) {
            newTooltip.add(line);
        }
        return new ComponentData(id, displayName, icon, shortDesc, newTooltip, spiritImpact);
    }

    @Override
    public String toString() {
        return "ComponentData{" +
                "id='" + id + '\'' +
                ", displayName='" + displayName + '\'' +
                '}';
    }
}
