package org.example.Kangnaixi.tiandao.spell.builder;

import java.util.Collections;
import java.util.List;

/**
 * 定义增幅（Augment）组件。
 */
public class AugmentDefinition {

    private String id = "";
    private String displayName = "";
    private String description = "";
    private String targetParam = "";
    private double stepValue = 0.0;
    private int maxStacks = 0;
    private double complexityWeight = 0.0;
    private double spiritCostMultiplier = 0.0;
    private double cooldownMultiplier = 0.0;
    private List<String> allowedForms = Collections.emptyList();
    private List<String> allowedEffects = Collections.emptyList();

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getTargetParam() {
        return targetParam;
    }

    public double getStepValue() {
        return stepValue;
    }

    public int getMaxStacks() {
        return maxStacks;
    }

    public double getComplexityWeight() {
        return complexityWeight;
    }

    public double getSpiritCostMultiplier() {
        return spiritCostMultiplier;
    }

    public double getCooldownMultiplier() {
        return cooldownMultiplier;
    }

    public List<String> getAllowedForms() {
        return allowedForms == null ? Collections.emptyList() : Collections.unmodifiableList(allowedForms);
    }

    public List<String> getAllowedEffects() {
        return allowedEffects == null ? Collections.emptyList() : Collections.unmodifiableList(allowedEffects);
    }
}
