package org.example.Kangnaixi.tiandao.spell.builder;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 定义 Effect（术法核心效果）组件。
 */
public class EffectDefinition {

    private String id = "";
    private String displayName = "";
    private String description = "";
    private List<String> allowedForms = Collections.emptyList();
    private double baseSpiritCost = 0.0;
    private double baseCooldown = 0.0;
    private double complexityWeight = 1.0;
    private Map<String, Double> baseValues = Collections.emptyMap();
    private List<StatusEffect> statusEffects = Collections.emptyList();

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getAllowedForms() {
        return allowedForms == null ? Collections.emptyList() : Collections.unmodifiableList(allowedForms);
    }

    public double getBaseSpiritCost() {
        return baseSpiritCost;
    }

    public double getBaseCooldown() {
        return baseCooldown;
    }

    public double getComplexityWeight() {
        return complexityWeight;
    }

    public Map<String, Double> getBaseValues() {
        return baseValues == null ? Collections.emptyMap() : Collections.unmodifiableMap(baseValues);
    }

    public List<StatusEffect> getStatusEffects() {
        return statusEffects == null ? Collections.emptyList() : Collections.unmodifiableList(statusEffects);
    }

    public static class StatusEffect {
        private String effectId = "";
        private int amplifier = 0;
        private int durationTicks = 0;
        private boolean ambient = false;
        private boolean visible = true;

        public String getEffectId() {
            return effectId;
        }

        public int getAmplifier() {
            return amplifier;
        }

        public int getDurationTicks() {
            return durationTicks;
        }

        public boolean isAmbient() {
            return ambient;
        }

        public boolean isVisible() {
            return visible;
        }
    }
}
