package org.example.Kangnaixi.tiandao.spell.runtime;

import org.example.Kangnaixi.tiandao.spell.definition.SpellDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * 根据 SpellDefinition + 当前玩家状态，计算最终的施法数值。
 */
public final class SpellRuntimeEngine {

    private SpellRuntimeEngine() {}

    public static SpellRuntimeResult evaluate(SpellRuntimeContext context) {
        SpellDefinition definition = context.definition();
        SpellRuntimeNumbers runtimeNumbers = SpellRuntimeNumbers.fromDefinition(definition.getBaseStats());

        // 属性层影响
        for (SpellDefinition.Attribute attribute : definition.getAttributes()) {
            double damageBonus = attribute.scalingOrDefault("damage", 0);
            if (damageBonus != 0) {
                runtimeNumbers.scaleDamage(1.0 + damageBonus);
            }
            double speedBonus = attribute.scalingOrDefault("speed", 0);
            if (speedBonus != 0) {
                runtimeNumbers.scaleSpeed(1.0 + speedBonus);
            }
            double rangeBonus = attribute.scalingOrDefault("range", 0);
            if (rangeBonus != 0) {
                runtimeNumbers.scaleRange(1.0 + rangeBonus);
            }
            double costBonus = attribute.scalingOrDefault("spirit_cost", 0);
            if (costBonus != 0) {
                runtimeNumbers.scaleSpiritCost(1.0 + costBonus);
            }
        }

        List<SpellDefinition.Effect> finalEffects = new ArrayList<>(definition.getEffects());
        boolean swordQi = false;

        if (context.shouldApplySwordQi()) {
            swordQi = true;
            definition.getSwordQiOverride().ifPresent(override -> {
                runtimeNumbers.scaleDamage(override.damageMultiplier());
                runtimeNumbers.scaleSpeed(override.speedMultiplier());
                runtimeNumbers.scaleRange(override.rangeMultiplier());
                finalEffects.addAll(override.extraEffects());
            });
        }

        return new SpellRuntimeResult(definition, runtimeNumbers.freeze(), finalEffects, swordQi);
    }
}
