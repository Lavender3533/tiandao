package org.example.Kangnaixi.tiandao.starchart;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * 星盘节点库
 *
 * 节点分类：
 * - Effect（效果）：术法最终显现的效果
 * - Form（形态）：术法的空间形状
 * - Modifier（调制）：附加增强项
 * - Blueprint（成品）：已组合的术法
 *
 * 注：源（灵力来源）在手盘中单独选择，不在星盘中
 */
public class StarTestNodes {
    private static final List<StarNode> NODES = new ArrayList<>();

    // 图标占位符
    private static final ResourceLocation ICON_DEFAULT = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/nether_star.png");

    static {
        initNodes();
    }

    private static void initNodes() {
        // === EFFECT（效果）===
        NODES.add(new StarNode("effect_damage", "伤害", ICON_DEFAULT, StarNodeCategory.EFFECT, 0, 0));
        NODES.add(new StarNode("effect_ignite", "点燃", ICON_DEFAULT, StarNodeCategory.EFFECT, 0, 0));
        NODES.add(new StarNode("effect_knockback", "击退", ICON_DEFAULT, StarNodeCategory.EFFECT, 0, 0));
        NODES.add(new StarNode("effect_freeze", "冰冻", ICON_DEFAULT, StarNodeCategory.EFFECT, 0, 0));
        NODES.add(new StarNode("effect_heal", "治疗", ICON_DEFAULT, StarNodeCategory.EFFECT, 0, 0));
        NODES.add(new StarNode("effect_pull", "引力吸附", ICON_DEFAULT, StarNodeCategory.EFFECT, 0, 0));

        // === FORM（形态）===
        NODES.add(new StarNode("form_sphere", "球体", ICON_DEFAULT, StarNodeCategory.FORM, 0, 0));
        NODES.add(new StarNode("form_beam", "光束", ICON_DEFAULT, StarNodeCategory.FORM, 0, 0));
        NODES.add(new StarNode("form_area", "场域", ICON_DEFAULT, StarNodeCategory.FORM, 0, 0));
        NODES.add(new StarNode("form_cone", "锥形", ICON_DEFAULT, StarNodeCategory.FORM, 0, 0));
        NODES.add(new StarNode("form_burst", "爆裂", ICON_DEFAULT, StarNodeCategory.FORM, 0, 0));
        NODES.add(new StarNode("form_storm", "风暴", ICON_DEFAULT, StarNodeCategory.FORM, 0, 0));
        NODES.add(new StarNode("form_wave", "波纹", ICON_DEFAULT, StarNodeCategory.FORM, 0, 0));

        // === MODIFIER（调制）===
        NODES.add(new StarNode("mod_power_up", "强度+", ICON_DEFAULT, StarNodeCategory.MODIFIER, 0, 0));
        NODES.add(new StarNode("mod_range_up", "距离+", ICON_DEFAULT, StarNodeCategory.MODIFIER, 0, 0));
        NODES.add(new StarNode("mod_duration_up", "持续+", ICON_DEFAULT, StarNodeCategory.MODIFIER, 0, 0));
        NODES.add(new StarNode("mod_area_up", "范围+", ICON_DEFAULT, StarNodeCategory.MODIFIER, 0, 0));
        NODES.add(new StarNode("mod_bounce", "弹射+", ICON_DEFAULT, StarNodeCategory.MODIFIER, 0, 0));
        NODES.add(new StarNode("mod_cost_down", "减耗", ICON_DEFAULT, StarNodeCategory.MODIFIER, 0, 0));

        // === BLUEPRINT（成品）===
        NODES.add(new StarNode("blueprint_fireball", "火球术", ICON_DEFAULT, StarNodeCategory.BLUEPRINT, 0, 0));
        NODES.add(new StarNode("blueprint_ice_lance", "冰枪术", ICON_DEFAULT, StarNodeCategory.BLUEPRINT, 0, 0));
        NODES.add(new StarNode("blueprint_healing", "治愈术", ICON_DEFAULT, StarNodeCategory.BLUEPRINT, 0, 0));
    }

    /**
     * 获取所有测试节点
     */
    public static List<StarNode> getAllNodes() {
        return new ArrayList<>(NODES);
    }

    /**
     * 根据ID查找节点
     */
    public static StarNode getNodeById(String id) {
        return NODES.stream()
                .filter(node -> node.getId().equals(id))
                .findFirst()
                .orElse(null);
    }
}
