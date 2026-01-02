package org.example.Kangnaixi.tiandao.starchart;

import net.minecraft.resources.ResourceLocation;

/**
 * 星盘节点数据
 *
 * 字段说明：
 * - id: 唯一标识符，同时作为 bindingKey 用于蓝图映射
 *       格式: "{category}_{effect}" 如 "effect_damage", "carrier_projectile"
 * - name: 显示名称（中文）
 * - category: 节点分类（EFFECT/CARRIER/FORM/MODIFIER/BLUEPRINT）
 */
public class StarNode {
    private final String id;          // 唯一ID，同时作为 bindingKey
    private final String name;        // 显示名称
    private final ResourceLocation icon;
    private final StarNodeCategory category;
    private final float x;
    private final float y;

    public StarNode(String id, String name, ResourceLocation icon, StarNodeCategory category, float x, float y) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.category = category;
        this.x = x;
        this.y = y;
    }

    public String getId() {
        return id;
    }

    /**
     * 获取绑定键 - 用于 HandWheel 和 SpellBlueprintBuilder 的统一映射
     * 当前实现直接返回 id，保持一致性
     */
    public String getBindingKey() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ResourceLocation getIcon() {
        return icon;
    }

    public StarNodeCategory getCategory() {
        return category;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public int getScreenX(int centerX) {
        return centerX + (int) x;
    }

    public int getScreenY(int centerY) {
        return centerY + (int) y;
    }

    /**
     * 调试用：完整信息输出
     */
    @Override
    public String toString() {
        return String.format("StarNode[id=%s, name=%s, category=%s, bindingKey=%s]",
            id, name, category, getBindingKey());
    }
}
