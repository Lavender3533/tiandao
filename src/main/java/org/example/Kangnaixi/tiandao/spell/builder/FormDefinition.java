package org.example.Kangnaixi.tiandao.spell.builder;

import org.example.Kangnaixi.tiandao.cultivation.CultivationRealm;

import java.util.Collections;
import java.util.List;

/**
 * 定义 Form（形状/触发）组件的基础数据。
 */
public class FormDefinition {

    private String id = "";
    private String displayName = "";
    private String description = "";
    private CultivationRealm unlockRealm = CultivationRealm.QI_CONDENSATION;
    private int minSubRealmLevel = 0;
    private String shape = "SELF_AURA";
    private String targeting = "SELF";
    private List<String> targetingOptions = Collections.emptyList();
    private double baseRadius = 0.0;
    private double baseDurationSeconds = 0.0;
    private double baseDistance = 0.0;
    private double baseAngle = 0.0;
    private double tickRate = 20.0;
    private boolean movementLock = false;
    private double complexityWeight = 1.0;
    private List<String> allowedEffects = Collections.emptyList();
    private List<String> defaultAugments = Collections.emptyList();
    private List<String> tags = Collections.emptyList();
    private double minRadius = 0.0;
    private double maxRadius = 0.0;
    private double radiusStep = 1.0;
    private double minDistance = 0.0;
    private double maxDistance = 0.0;
    private double distanceStep = 1.0;
    private double minAngle = 0.0;
    private double maxAngle = 0.0;
    private double angleStep = 5.0;

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public CultivationRealm getUnlockRealm() {
        return unlockRealm == null ? CultivationRealm.QI_CONDENSATION : unlockRealm;
    }

    public int getMinSubRealmLevel() {
        return minSubRealmLevel;
    }

    public String getShape() {
        return shape == null ? "SELF_AURA" : shape;
    }

    public String getTargeting() {
        return targeting == null ? "SELF" : targeting;
    }

    public List<String> getTargetingOptions() {
        if (targetingOptions == null || targetingOptions.isEmpty()) {
            return Collections.singletonList(getTargeting());
        }
        return Collections.unmodifiableList(targetingOptions);
    }

    public double getBaseRadius() {
        return baseRadius;
    }

    public double getBaseDurationSeconds() {
        return baseDurationSeconds;
    }

    public double getBaseDistance() {
        return baseDistance;
    }

    public double getBaseAngle() {
        return baseAngle;
    }

    public double getTickRate() {
        return tickRate;
    }

    public boolean isMovementLock() {
        return movementLock;
    }

    public double getComplexityWeight() {
        return complexityWeight;
    }

    public List<String> getAllowedEffects() {
        return allowedEffects == null ? Collections.emptyList() : Collections.unmodifiableList(allowedEffects);
    }

    public List<String> getDefaultAugments() {
        return defaultAugments == null ? Collections.emptyList() : Collections.unmodifiableList(defaultAugments);
    }

    public List<String> getTags() {
        return tags == null ? Collections.emptyList() : Collections.unmodifiableList(tags);
    }

    public double getMinRadius() {
        return minRadius;
    }

    public double getMaxRadius() {
        return maxRadius > 0 ? maxRadius : baseRadius;
    }

    public double getRadiusStep() {
        return radiusStep > 0 ? radiusStep : 1.0;
    }

    public double getMinDistance() {
        return minDistance;
    }

    public double getMaxDistance() {
        return maxDistance > 0 ? maxDistance : baseDistance;
    }

    public double getDistanceStep() {
        return distanceStep > 0 ? distanceStep : 1.0;
    }

    public double getMinAngle() {
        return minAngle;
    }

    public double getMaxAngle() {
        return maxAngle > 0 ? maxAngle : baseAngle;
    }

    public double getAngleStep() {
        return angleStep > 0 ? angleStep : 5.0;
    }
}
