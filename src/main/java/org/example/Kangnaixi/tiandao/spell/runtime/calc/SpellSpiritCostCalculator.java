package org.example.Kangnaixi.tiandao.spell.runtime.calc;

import org.example.Kangnaixi.tiandao.spell.definition.SpellDefinition;

/**
 * 简化版灵力消耗估算器，基于 SpellDefinition 的骨架组件/属性/效果计算最终施法成本。
 */
public final class SpellSpiritCostCalculator {

    private static final double BASE_COST = 10.0;

    private SpellSpiritCostCalculator() {}

    public static double compute(SpellDefinition definition) {
        if (definition == null) {
            return BASE_COST;
        }
        double cost = BASE_COST;
        cost += sourceCost(definition.getSource());
        cost += carrierCost(definition.getCarrier());
        cost += formCost(definition);
        cost += definition.getAttributes().size() * 2.0;
        cost += definition.getEffects().size() * 2.0;
        return Math.max(1.0, cost);
    }

    private static double sourceCost(SpellDefinition.Component component) {
        if (component == null) {
            return 0.0;
        }
        String path = path(component);
        return switch (path) {
            case "finger", "handseal", "mantra" -> 0.0; // 指诀
            case "seal" -> 5.0; // 法印
            case "weapon", "artifact" -> 10.0; // 法器
            case "talisman" -> 8.0; // 符箓
            case "array", "formation" -> 12.0; // 阵盘
            default -> 4.0;
        };
    }

    private static double carrierCost(SpellDefinition.Component component) {
        if (component == null) {
            return 0.0;
        }
        String path = path(component);
        return switch (path) {
            case "sword_qi", "sword_qi_arc", "sword_qi_ray" -> 8.0;
            case "projectile", "bolt" -> 5.0;
            case "shockwave", "wave" -> 6.0;
            case "field", "domain" -> 12.0;
            case "ground_spike", "glyph" -> 10.0;
            case "buff" -> 8.0;
            default -> 5.0;
        };
    }

    private static double formCost(SpellDefinition definition) {
        SpellDefinition.Component form = definition.getForm();
        if (form == null) {
            return 0.0;
        }
        String path = path(form);
        return switch (path) {
            case "instant" -> 0.0;
            case "channel", "channeled" -> {
                double seconds = definition.getBaseStats().channelTicks() / 20.0;
                yield Math.max(1.0, Math.ceil(seconds)) * 8.0;
            }
            case "delay", "delayed" -> 3.0;
            case "combo" -> {
                double segments = form.getParameter("segments", 0);
                if (segments <= 0) {
                    segments = form.getParameter("stages", 0);
                }
                if (segments <= 0) {
                    segments = form.getParameter("hits", 1);
                }
                yield Math.max(1.0, segments) * 4.0;
            }
            case "duration", "sustained" -> 6.0;
            case "mark_detonate", "detonate" -> 6.0;
            default -> 2.0;
        };
    }

    private static String path(SpellDefinition.Component component) {
        if (component == null || component.id() == null) {
            return "";
        }
        return component.id().getPath().toLowerCase();
    }
}
