package org.example.Kangnaixi.tiandao.spell.builder;

import org.example.Kangnaixi.tiandao.cultivation.CultivationRealm;
import org.example.Kangnaixi.tiandao.spell.blueprint.SpellBlueprint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 根据 Form + Effect + Augment 组合计算数值并生成蓝图高级数据。
 */
public final class SpellComponentAssembler {

    private SpellComponentAssembler() {
    }

    public static Result assemble(FormDefinition form,
                                  FormParameters parameters,
                                  EffectDefinition effect,
                                  List<AugmentStack> augments) {
        double radius = parameters.radius() > 0 ? parameters.radius() : form.getBaseRadius();
        double durationSeconds = parameters.durationSeconds() > 0
            ? parameters.durationSeconds()
            : form.getBaseDurationSeconds();
        double distance = parameters.distance() > 0 ? parameters.distance() : form.getBaseDistance();
        double angle = parameters.angle();
        double tickRate = form.getTickRate();
        boolean movementLock = parameters.movementLock();

        double complexity = form.getComplexityWeight() + effect.getComplexityWeight();
        double spiritCost = effect.getBaseSpiritCost();
        double cooldown = effect.getBaseCooldown();
        double intensityMultiplier = 1.0;
        String colorHex = "#FFFFFF";

        for (AugmentStack stack : augments) {
            AugmentDefinition augment = stack.definition();
            int level = Math.min(stack.stacks(), Math.max(1, augment.getMaxStacks()));
            if (!augment.getAllowedForms().isEmpty()
                && form.getId() != null
                && !augment.getAllowedForms().contains(form.getId())) {
                continue;
            }
            if (!augment.getAllowedEffects().isEmpty()
                && effect.getId() != null
                && !augment.getAllowedEffects().contains(effect.getId())) {
                continue;
            }
            complexity += augment.getComplexityWeight() * level;
            spiritCost += effect.getBaseSpiritCost() * augment.getSpiritCostMultiplier() * level;
            cooldown += effect.getBaseCooldown() * augment.getCooldownMultiplier() * level;
            switch (augment.getTargetParam()) {
                case "durationSeconds" -> durationSeconds += augment.getStepValue() * level;
                case "radius" -> radius += augment.getStepValue() * level;
                case "distance" -> distance += augment.getStepValue() * level;
                case "intensity" -> intensityMultiplier += augment.getStepValue() * level;
                case "color" -> colorHex = "#55FFAA";
                default -> {
                }
            }
        }

        SpellBlueprint.ShapeType shapeType = mapShapeType(parameters.shapeType());
        SpellBlueprint.TargetingType targetingType = mapTargeting(parameters.targetingType());
        SpellBlueprint.EffectType effectType = mapEffectType(effect.getId());
        SpellBlueprint.ElementType elementType = mapElement(effect.getId());

        SpellBlueprint.AdvancedData advanced = new SpellBlueprint.AdvancedData();
        advanced.setElements(Collections.singletonList(elementType));
        SpellBlueprint.ShapeConfig shape = new SpellBlueprint.ShapeConfig();
        shape.setType(shapeType);
        shape.setRadius(radius);
        shape.setLength(distance);
        shape.setAngle(angle);
        shape.setWidth(0.0);
        advanced.setShape(shape);

        SpellBlueprint.SegmentConfig segment = new SpellBlueprint.SegmentConfig();
        segment.setName(effect.getId());
        segment.setPowerMultiplier(intensityMultiplier);
        segment.setDurationTicks((int) Math.max(20, durationSeconds * 20));
        segment.setComplexityWeight(effect.getComplexityWeight());
        advanced.setSegments(Collections.singletonList(segment));

        List<SpellBlueprint.StatusEffectConfig> statusConfigs = new ArrayList<>();
        for (EffectDefinition.StatusEffect status : effect.getStatusEffects()) {
            SpellBlueprint.StatusEffectConfig cfg = new SpellBlueprint.StatusEffectConfig();
            cfg.setEffectId(status.getEffectId());
            cfg.setAmplifier(status.getAmplifier());
            cfg.setDurationTicks(status.getDurationTicks());
            cfg.setAmbient(status.isAmbient());
            cfg.setVisible(status.isVisible());
            statusConfigs.add(cfg);
        }
        advanced.setStatusEffects(statusConfigs);
        advanced.setVisualLayers(Collections.emptyList());
        advanced.setAudioCues(Collections.emptyList());
        advanced.setRequirements(buildRequirement(form, effect, spiritCost, cooldown, complexity));
        double range = distance > 0 ? distance : radius;
        double basePower = effect.getBaseValues().getOrDefault("intensity",
            effect.getBaseValues().getOrDefault("regenMultiplier",
                effect.getBaseValues().getOrDefault("absorptionHearts", 1.0)));

        return new Result(
            spiritCost,
            cooldown,
            range,
            radius,
            durationSeconds,
            complexity,
            basePower,
            tickRate,
            movementLock,
            elementType,
            effectType,
            targetingType,
            form.getUnlockRealm(),
            form.getMinSubRealmLevel(),
            advanced,
            parameters
        );
    }

    private static SpellBlueprint.RequirementConfig buildRequirement(FormDefinition form,
                                                                     EffectDefinition effect,
                                                                     double spiritCost,
                                                                     double cooldown,
                                                                     double complexity) {
        SpellBlueprint.RequirementConfig cfg = new SpellBlueprint.RequirementConfig();
        cfg.setBaseSpiritCost(spiritCost);
        cfg.setBaseCooldown(cooldown);
        cfg.setComplexity(complexity);
        cfg.setOverloadFactor(Math.max(1.0, complexity / 4.0));
        cfg.setMinRealm(form.getUnlockRealm());
        cfg.setMinSubRealmLevel(form.getMinSubRealmLevel());
        return cfg;
    }

    private static SpellBlueprint.ShapeType mapShapeType(String shapeName) {
        if (shapeName == null) {
            return SpellBlueprint.ShapeType.SELF_AURA;
        }
        return switch (shapeName.toUpperCase(Locale.ROOT)) {
            case "LINE" -> SpellBlueprint.ShapeType.LINE;
            case "SPHERE" -> SpellBlueprint.ShapeType.SPHERE;
            case "CONE" -> SpellBlueprint.ShapeType.CONE;
            case "PROJECTILE" -> SpellBlueprint.ShapeType.PROJECTILE;
            case "TARGET_AREA" -> SpellBlueprint.ShapeType.TARGET_AREA;
            case "SELF_AURA" -> SpellBlueprint.ShapeType.SELF_AURA;
            default -> SpellBlueprint.ShapeType.SELF_AURA;
        };
    }

    private static SpellBlueprint.TargetingType mapTargeting(String targetingName) {
        if (targetingName == null) {
            return SpellBlueprint.TargetingType.SELF;
        }
        return switch (targetingName.toUpperCase(Locale.ROOT)) {
            case "SELF" -> SpellBlueprint.TargetingType.SELF;
            case "TARGET_ENTITY" -> SpellBlueprint.TargetingType.TARGET_ENTITY;
            case "TARGET_BLOCK" -> SpellBlueprint.TargetingType.TARGET_BLOCK;
            case "DIRECTIONAL_RELEASE" -> SpellBlueprint.TargetingType.DIRECTIONAL_RELEASE;
            case "AREA_RELEASE" -> SpellBlueprint.TargetingType.AREA_RELEASE;
            default -> SpellBlueprint.TargetingType.SELF;
        };
    }

    private static SpellBlueprint.EffectType mapEffectType(String effectId) {
        if (effectId == null) {
            return SpellBlueprint.EffectType.UTILITY;
        }
        String id = effectId.toLowerCase(Locale.ROOT);
        if (id.contains("shield")) {
            return SpellBlueprint.EffectType.UTILITY;
        }
        if (id.contains("regen") || id.contains("heal")) {
            return SpellBlueprint.EffectType.HEALING;
        }
        if (id.contains("sense")) {
            return SpellBlueprint.EffectType.UTILITY;
        }
        if (id.contains("wind") || id.contains("blink")) {
            return SpellBlueprint.EffectType.UTILITY;
        }
        return SpellBlueprint.EffectType.UTILITY;
    }

    private static SpellBlueprint.ElementType mapElement(String effectId) {
        if (effectId == null) {
            return SpellBlueprint.ElementType.VOID;
        }
        String id = effectId.toLowerCase(Locale.ROOT);
        if (id.contains("wind")) {
            return SpellBlueprint.ElementType.WIND;
        }
        if (id.contains("sense")) {
            return SpellBlueprint.ElementType.LIGHTNING;
        }
        if (id.contains("shield")) {
            return SpellBlueprint.ElementType.EARTH;
        }
        if (id.contains("spirit") || id.contains("regen")) {
            return SpellBlueprint.ElementType.WOOD;
        }
        return SpellBlueprint.ElementType.VOID;
    }

    public record AugmentStack(AugmentDefinition definition, int stacks) {
    }

    public record Result(double spiritCost,
                         double cooldown,
                         double range,
                         double areaRadius,
                         double durationSeconds,
                         double complexity,
                         double basePower,
                         double tickRate,
                         boolean movementLock,
                         SpellBlueprint.ElementType elementType,
                         SpellBlueprint.EffectType effectType,
                         SpellBlueprint.TargetingType targetingType,
                         CultivationRealm requiredRealm,
                         int requiredSubRealmLevel,
                         SpellBlueprint.AdvancedData advancedData,
                         FormParameters parameters) {
    }

    public record FormParameters(String shapeType,
                                 String targetingType,
                                 double radius,
                                 double distance,
                                 double durationSeconds,
                                 double angle,
                                 boolean movementLock) {
    }
}
