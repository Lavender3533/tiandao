package org.example.Kangnaixi.tiandao.spell.blueprint;

import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.cultivation.CultivationRealm;
import org.example.Kangnaixi.tiandao.starchart.StarNode;
import org.example.Kangnaixi.tiandao.starchart.StarNodeCategory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 术法蓝图构建器 - 从StarNode组合生成SpellBlueprint
 *
 * 流程：
 * 1. 设置效果/形态节点（必填）
 * 2. 添加调制节点（可选）
 * 3. 自动计算属性（威力、消耗、冷却等）
 * 4. 生成唯一ID和名称
 * 5. build() 输出 SpellBlueprint
 *
 * 注：源（灵力来源）由手盘中心独立选择，不参与蓝图构建
 */
public class SpellBlueprintBuilder {

    // 核心节点（2必填）
    private StarNode effectNode;
    private StarNode formNode;
    private final List<StarNode> modifiers = new ArrayList<>();

    // 创建者信息
    private UUID creatorId;
    private String creatorName;

    // 可选覆盖
    private String customName;
    private String customDescription;
    private CultivationRealm overrideRealm;

    // ========== 节点设置（流式API） ==========

    public SpellBlueprintBuilder setEffectNode(StarNode node) {
        if (node != null && node.getCategory() != StarNodeCategory.EFFECT) {
            throw new BuildException("效果节点类别不正确: " + node.getCategory());
        }
        this.effectNode = node;
        return this;
    }

    public SpellBlueprintBuilder setFormNode(StarNode node) {
        if (node != null && node.getCategory() != StarNodeCategory.FORM) {
            throw new BuildException("形态节点类别不正确: " + node.getCategory());
        }
        this.formNode = node;
        return this;
    }

    public SpellBlueprintBuilder addModifier(StarNode node) {
        if (node != null) {
            if (node.getCategory() != StarNodeCategory.MODIFIER) {
                throw new BuildException("调制节点类别不正确: " + node.getCategory());
            }
            modifiers.add(node);
        }
        return this;
    }

    public SpellBlueprintBuilder clearModifiers() {
        modifiers.clear();
        return this;
    }

    // ========== 创建者信息 ==========

    public SpellBlueprintBuilder setCreatorId(UUID creatorId) {
        this.creatorId = creatorId;
        return this;
    }

    public SpellBlueprintBuilder setCreatorName(String creatorName) {
        this.creatorName = creatorName;
        return this;
    }

    // ========== 可选覆盖 ==========

    public SpellBlueprintBuilder setCustomName(String name) {
        this.customName = name;
        return this;
    }

    public SpellBlueprintBuilder setCustomDescription(String description) {
        this.customDescription = description;
        return this;
    }

    public SpellBlueprintBuilder setOverrideRealm(CultivationRealm realm) {
        this.overrideRealm = realm;
        return this;
    }

    // ========== 构建 ==========

    /**
     * 构建 SpellBlueprint
     * @throws BuildException 如果缺少必要节点
     */
    public SpellBlueprint build() throws BuildException {
        // === DEBUG LOG: 打印输入节点 ===
        Tiandao.LOGGER.info("[BlueprintBuilder] ========== 开始构建 ==========");
        Tiandao.LOGGER.info("[BlueprintBuilder] 输入节点:");
        Tiandao.LOGGER.info("  EFFECT: {} (bindingKey={})",
            effectNode != null ? effectNode.getName() : "null",
            effectNode != null ? effectNode.getBindingKey() : "null");
        Tiandao.LOGGER.info("  FORM: {} (bindingKey={})",
            formNode != null ? formNode.getName() : "null",
            formNode != null ? formNode.getBindingKey() : "null");
        if (!modifiers.isEmpty()) {
            StringBuilder modStr = new StringBuilder("  MODIFIERS: ");
            for (StarNode mod : modifiers) {
                modStr.append(mod.getName()).append(" (").append(mod.getBindingKey()).append("), ");
            }
            Tiandao.LOGGER.info(modStr.toString());
        } else {
            Tiandao.LOGGER.info("  MODIFIERS: 无");
        }

        // 1. 验证必填节点
        validate();

        // 2. 生成ID
        String id = generateId();

        // 3. 生成名称和描述
        String name = generateName();
        String description = generateDescription();

        // 4. 映射属性（基于 bindingKey）
        SpellBlueprint.ElementType element = mapEffectToElement(effectNode);
        SpellBlueprint.EffectType effectType = mapEffectToType(effectNode);
        SpellBlueprint.TargetingType targeting = mapFormToTargeting(formNode);  // 现在从形态推断目标方式
        SpellBlueprint.ShapeType shape = mapFormToShape(formNode);

        // === DEBUG LOG: 打印映射结果 ===
        Tiandao.LOGGER.info("[BlueprintBuilder] 映射结果:");
        Tiandao.LOGGER.info("  {} ({}) → Element={}, EffectType={}",
            effectNode.getName(), effectNode.getBindingKey(), element, effectType);
        Tiandao.LOGGER.info("  {} ({}) → Targeting={}, Shape={}",
            formNode.getName(), formNode.getBindingKey(), targeting, shape);

        // 5. 计算数值
        double basePower = calculateBasePower();
        double spiritCost = calculateSpiritCost();
        double cooldown = calculateCooldown();
        double range = calculateRange();
        double areaRadius = calculateAreaRadius();

        // 6. 计算境界需求
        CultivationRealm requiredRealm = overrideRealm != null
            ? overrideRealm
            : calculateRequiredRealm();

        // 7. 构建高级配置
        SpellBlueprint.AdvancedData advancedData = buildAdvancedData(shape);

        // 8. 创建Blueprint
        SpellBlueprint blueprint = new SpellBlueprint(
            id,
            name,
            description,
            element,
            effectType,
            targeting,
            basePower,
            spiritCost,
            cooldown,
            range,
            areaRadius,
            requiredRealm,
            0,
            advancedData
        );

        // === DEBUG LOG: 打印最终蓝图 ===
        Tiandao.LOGGER.info("[BlueprintBuilder] ========== 构建完成 ==========");
        Tiandao.LOGGER.info("[BlueprintBuilder] 最终蓝图:");
        Tiandao.LOGGER.info("  ID: {}", id);
        Tiandao.LOGGER.info("  名称: {}", name);
        Tiandao.LOGGER.info("  描述: {}", description);
        Tiandao.LOGGER.info("  Element={}, EffectType={}, Targeting={}, Shape={}",
            element, effectType, targeting, shape);
        Tiandao.LOGGER.info("  威力={}, 消耗={}, 冷却={}s, 范围={}, 半径={}",
            basePower, spiritCost, cooldown, range, areaRadius);
        Tiandao.LOGGER.info("  境界需求: {}", requiredRealm);

        return blueprint;
    }

    private void validate() throws BuildException {
        if (effectNode == null) {
            throw new BuildException("缺少效果节点");
        }
        if (formNode == null) {
            throw new BuildException("缺少形态节点");
        }
    }

    // ========== ID与名称生成（基于 bindingKey） ==========

    private String generateId() {
        String timestamp = Long.toHexString(System.currentTimeMillis()).substring(4);
        String effectPart = effectNode.getBindingKey().replace("effect_", "");
        String formPart = formNode.getBindingKey().replace("form_", "");
        return String.format("tiandao:spell_%s_%s_%s", effectPart, formPart, timestamp);
    }

    private String generateName() {
        if (customName != null && !customName.isEmpty()) {
            return customName;
        }

        // 根据节点组合生成名称
        String effectName = getShortName(effectNode);
        String formName = getShortName(formNode);

        // 效果+形态 组合命名
        return effectName + formName + "术";
    }

    private String generateDescription() {
        if (customDescription != null && !customDescription.isEmpty()) {
            return customDescription;
        }

        String formDesc = getFormDescription(formNode);
        String effectDesc = getEffectDescription(effectNode);

        return String.format("释放%s，%s", formDesc, effectDesc);
    }

    private String getShortName(StarNode node) {
        String name = node.getName();
        // 去除常见后缀
        return name.replace("伤害", "")
                   .replace("治疗", "愈")
                   .replace("击退", "击")
                   .replace("点燃", "焰")
                   .replace("冰冻", "冰")
                   .replace("引力吸附", "引");
    }

    private String getFormDescription(StarNode node) {
        return switch (node.getBindingKey()) {
            case "form_sphere" -> "球体法术";
            case "form_beam" -> "光束法术";
            case "form_area" -> "范围法术";
            case "form_cone" -> "锥形法术";
            case "form_burst" -> "爆裂法术";
            case "form_storm" -> "风暴法术";
            case "form_wave" -> "波纹法术";
            default -> "法术";
        };
    }

    private String getEffectDescription(StarNode node) {
        return switch (node.getBindingKey()) {
            case "effect_damage" -> "对目标造成伤害";
            case "effect_ignite" -> "点燃目标";
            case "effect_knockback" -> "击退目标";
            case "effect_freeze" -> "冻结目标";
            case "effect_heal" -> "恢复生命值";
            case "effect_pull" -> "将目标拉向自身";
            default -> "产生效果";
        };
    }

    // ========== 属性映射（基于 bindingKey） ==========

    private SpellBlueprint.ElementType mapEffectToElement(StarNode node) {
        String key = node.getBindingKey();
        return switch (key) {
            case "effect_ignite" -> SpellBlueprint.ElementType.FIRE;
            case "effect_freeze" -> SpellBlueprint.ElementType.WATER;
            case "effect_heal" -> SpellBlueprint.ElementType.WOOD;
            case "effect_knockback", "effect_pull" -> SpellBlueprint.ElementType.WIND;
            case "effect_damage" -> SpellBlueprint.ElementType.VOID; // 纯伤害无元素
            default -> SpellBlueprint.ElementType.VOID;
        };
    }

    private SpellBlueprint.EffectType mapEffectToType(StarNode node) {
        String key = node.getBindingKey();
        return switch (key) {
            case "effect_damage", "effect_ignite", "effect_freeze" -> SpellBlueprint.EffectType.DAMAGE;
            case "effect_heal" -> SpellBlueprint.EffectType.HEALING;
            case "effect_knockback", "effect_pull" -> SpellBlueprint.EffectType.CONTROL;
            default -> SpellBlueprint.EffectType.UTILITY;
        };
    }

    /**
     * 从形态推断目标方式（原来从载体推断，现在简化为从形态推断）
     */
    private SpellBlueprint.TargetingType mapFormToTargeting(StarNode node) {
        String key = node.getBindingKey();
        return switch (key) {
            case "form_sphere", "form_burst" -> SpellBlueprint.TargetingType.DIRECTIONAL_RELEASE;
            case "form_beam", "form_cone" -> SpellBlueprint.TargetingType.TARGET_ENTITY;
            case "form_area", "form_storm", "form_wave" -> SpellBlueprint.TargetingType.AREA_RELEASE;
            default -> SpellBlueprint.TargetingType.DIRECTIONAL_RELEASE;
        };
    }

    private SpellBlueprint.ShapeType mapFormToShape(StarNode node) {
        String key = node.getBindingKey();
        return switch (key) {
            case "form_sphere" -> SpellBlueprint.ShapeType.SPHERE;
            case "form_beam" -> SpellBlueprint.ShapeType.LINE;
            case "form_area" -> SpellBlueprint.ShapeType.TARGET_AREA;
            case "form_cone" -> SpellBlueprint.ShapeType.CONE;
            case "form_burst", "form_storm", "form_wave" -> SpellBlueprint.ShapeType.SPHERE;
            default -> SpellBlueprint.ShapeType.PROJECTILE;
        };
    }

    // ========== 数值计算（基于 bindingKey） ==========

    private double calculateBasePower() {
        double base = 5.0;

        // 效果节点基础威力
        base += switch (effectNode.getBindingKey()) {
            case "effect_damage" -> 8.0;
            case "effect_ignite" -> 6.0;
            case "effect_freeze" -> 4.0;
            case "effect_knockback" -> 3.0;
            case "effect_heal" -> 6.0;
            case "effect_pull" -> 2.0;
            default -> 5.0;
        };

        // 形态节点威力加成
        base *= switch (formNode.getBindingKey()) {
            case "form_burst", "form_storm" -> 1.3;
            case "form_beam" -> 1.2;
            case "form_area" -> 0.8; // 范围类单体威力降低
            default -> 1.0;
        };

        // 增强节点加成
        for (StarNode mod : modifiers) {
            if (mod.getBindingKey().equals("mod_power_up")) {
                base *= 1.25;
            }
        }

        return Math.round(base * 10) / 10.0;
    }

    private double calculateSpiritCost() {
        double cost = 20.0;

        // 基础消耗（按效果类型）
        cost += switch (effectNode.getBindingKey()) {
            case "effect_damage" -> 15.0;
            case "effect_ignite" -> 12.0;
            case "effect_freeze" -> 14.0;
            case "effect_heal" -> 18.0;
            default -> 10.0;
        };

        // 形态消耗加成
        cost *= switch (formNode.getBindingKey()) {
            case "form_storm" -> 1.5;
            case "form_area" -> 1.3;
            case "form_burst" -> 1.2;
            default -> 1.0;
        };

        // 增强节点影响
        for (StarNode mod : modifiers) {
            switch (mod.getBindingKey()) {
                case "mod_power_up" -> cost *= 1.2;
                case "mod_range_up" -> cost *= 1.15;
                case "mod_area_up" -> cost *= 1.25;
                case "mod_cost_down" -> cost *= 0.8;
            }
        }

        return Math.round(cost * 10) / 10.0;
    }

    private double calculateCooldown() {
        double cooldown = 3.0;

        // 基础冷却
        cooldown += switch (effectNode.getBindingKey()) {
            case "effect_damage" -> 2.0;
            case "effect_heal" -> 4.0;
            case "effect_freeze" -> 3.0;
            default -> 1.0;
        };

        // 形态影响
        cooldown *= switch (formNode.getBindingKey()) {
            case "form_storm" -> 1.8;
            case "form_burst" -> 1.3;
            default -> 1.0;
        };

        // 增强节点
        for (StarNode mod : modifiers) {
            if (mod.getBindingKey().equals("mod_duration_up")) {
                cooldown *= 1.2;
            }
        }

        return Math.round(cooldown * 10) / 10.0;
    }

    private double calculateRange() {
        double range = 10.0;

        // 形态影响（原来从载体推断，现在从形态推断）
        range *= switch (formNode.getBindingKey()) {
            case "form_beam" -> 2.0;      // 光束类射程远
            case "form_sphere" -> 1.5;    // 球体类中等
            case "form_area", "form_storm" -> 0.8;  // 范围类近距
            case "form_wave" -> 1.2;      // 波纹类中等
            default -> 1.0;
        };

        // 调制节点
        for (StarNode mod : modifiers) {
            if (mod.getBindingKey().equals("mod_range_up")) {
                range *= 1.4;
            }
        }

        return Math.round(range * 10) / 10.0;
    }

    private double calculateAreaRadius() {
        double radius = 2.0;

        // 形态影响
        radius *= switch (formNode.getBindingKey()) {
            case "form_area", "form_storm" -> 2.5;
            case "form_burst", "form_wave" -> 2.0;
            case "form_cone" -> 1.5;
            case "form_sphere" -> 1.2;
            default -> 1.0;
        };

        // 增强节点
        for (StarNode mod : modifiers) {
            if (mod.getBindingKey().equals("mod_area_up")) {
                radius *= 1.5;
            }
        }

        return Math.round(radius * 10) / 10.0;
    }

    private CultivationRealm calculateRequiredRealm() {
        int complexity = 1 + modifiers.size();

        // 复杂组合需要更高境界
        if (complexity >= 4) {
            return CultivationRealm.GOLDEN_CORE;
        } else if (complexity >= 2) {
            return CultivationRealm.FOUNDATION_BUILDING;
        } else {
            return CultivationRealm.QI_CONDENSATION;
        }
    }

    // ========== 高级配置构建 ==========

    private SpellBlueprint.AdvancedData buildAdvancedData(SpellBlueprint.ShapeType shape) {
        SpellBlueprint.AdvancedData data = new SpellBlueprint.AdvancedData();

        // 设置形状配置
        SpellBlueprint.ShapeConfig shapeConfig = new SpellBlueprint.ShapeConfig();
        shapeConfig.setType(shape);
        shapeConfig.setRadius(calculateAreaRadius());
        shapeConfig.setLength(calculateRange());

        if (shape == SpellBlueprint.ShapeType.CONE) {
            shapeConfig.setAngle(45.0);
        }

        data.setShape(shapeConfig);

        // 设置元素变体
        List<SpellBlueprint.ElementType> elements = new ArrayList<>();
        elements.add(mapEffectToElement(effectNode));
        data.setElements(elements);

        // 设置需求配置
        SpellBlueprint.RequirementConfig requirements = new SpellBlueprint.RequirementConfig();
        requirements.setBaseSpiritCost(calculateSpiritCost());
        requirements.setBaseCooldown(calculateCooldown());
        requirements.setComplexity(1.0 + modifiers.size() * 0.3);
        requirements.setMinRealm(calculateRequiredRealm());
        data.setRequirements(requirements);

        return data;
    }

    // ========== 异常类 ==========

    public static class BuildException extends RuntimeException {
        public BuildException(String message) {
            super(message);
        }
    }
}
