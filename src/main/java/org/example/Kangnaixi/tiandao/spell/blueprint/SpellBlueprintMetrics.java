package org.example.Kangnaixi.tiandao.spell.blueprint;

import org.example.Kangnaixi.tiandao.cultivation.CultivationRealm;

import javax.annotation.Nullable;

/**
 * Shared math helpers for blueprint metrics so client + server stay in sync.
 */
public final class SpellBlueprintMetrics {

    private SpellBlueprintMetrics() {
    }

    public static double computeComplexity(@Nullable SpellBlueprint.AdvancedData data) {
        if (data == null) {
            return 1.0;
        }
        double configured = data.getRequirements().getComplexity();
        if (configured > 0) {
            return configured;
        }
        double total = 0.0;
        for (SpellBlueprint.SegmentConfig segment : data.getSegments()) {
            total += Math.max(0.1, segment.getComplexityWeight());
        }
        return Math.max(1.0, total);
    }

    public static double estimateManaCost(@Nullable SpellBlueprint.AdvancedData data, double fallback) {
        double baseCost = fallback;
        if (data != null && data.getRequirements().getBaseSpiritCost() > 0) {
            baseCost = data.getRequirements().getBaseSpiritCost();
        }
        double complexity = computeComplexity(data);
        return baseCost * (1.0 + complexity / 10.0);
    }

    public static double estimateCooldown(@Nullable SpellBlueprint.AdvancedData data, double fallback) {
        double baseCooldown = fallback;
        if (data != null && data.getRequirements().getBaseCooldown() > 0) {
            baseCooldown = data.getRequirements().getBaseCooldown();
        }
        double complexity = computeComplexity(data);
        double rarityModifier = 2.0;
        return Math.min(180.0, baseCooldown + complexity * rarityModifier);
    }

    public static double estimateOverloadThreshold(@Nullable SpellBlueprint.AdvancedData data,
                                                   @Nullable SpellBlueprint fallbackBlueprint) {
        CultivationRealm realm = fallbackBlueprint != null
            ? fallbackBlueprint.getRequiredRealm()
            : CultivationRealm.QI_CONDENSATION;
        int subLevel = fallbackBlueprint != null ? fallbackBlueprint.getRequiredSubRealmLevel() : 0;
        if (data != null) {
            realm = data.getRequirements().getMinRealm();
            subLevel = data.getRequirements().getMinSubRealmLevel();
        }
        double base;
        switch (realm) {
            case FOUNDATION_BUILDING -> base = 20.0;
            case GOLDEN_CORE -> base = 35.0;
            case NASCENT_SOUL -> base = 50.0;
            case SPIRITUAL_TRANSFORMATION -> base = 65.0;
            case VOID_TUNING, BODY_INTEGRATION, MAHAYANA, TRIBULATION, IMMORTAL -> base = 80.0;
            default -> base = 10.0;
        }
        return base * Math.max(1.0, 1 + subLevel * 0.1);
    }
}
