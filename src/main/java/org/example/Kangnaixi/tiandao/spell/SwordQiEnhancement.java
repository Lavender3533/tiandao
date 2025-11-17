package org.example.Kangnaixi.tiandao.spell;

/**
 * 剑修强化配置
 * 当满足条件时提供额外的属性加成
 */
public class SwordQiEnhancement {

    private final double damageMultiplier;
    private final double speedMultiplier;
    private final double rangeMultiplier;
    private final boolean requiresSword;
    private final boolean requiresSwordIntent;

    public SwordQiEnhancement(double damageMultiplier, double speedMultiplier, double rangeMultiplier,
                             boolean requiresSword, boolean requiresSwordIntent) {
        this.damageMultiplier = damageMultiplier;
        this.speedMultiplier = speedMultiplier;
        this.rangeMultiplier = rangeMultiplier;
        this.requiresSword = requiresSword;
        this.requiresSwordIntent = requiresSwordIntent;
    }

    public double getDamageMultiplier() {
        return damageMultiplier;
    }

    public double getSpeedMultiplier() {
        return speedMultiplier;
    }

    public double getRangeMultiplier() {
        return rangeMultiplier;
    }

    public boolean requiresSword() {
        return requiresSword;
    }

    public boolean requiresSwordIntent() {
        return requiresSwordIntent;
    }

    /**
     * 创建默认的剑修强化配置
     */
    public static SwordQiEnhancement createDefault() {
        return new SwordQiEnhancement(1.3, 1.2, 1.1, true, true);
    }

    /**
     * 创建无强化配置
     */
    public static SwordQiEnhancement createNone() {
        return new SwordQiEnhancement(1.0, 1.0, 1.0, false, false);
    }

    @Override
    public String toString() {
        return String.format("SwordQiEnhancement[damage=%.1f, speed=%.1f, range=%.1f]",
                           damageMultiplier, speedMultiplier, rangeMultiplier);
    }
}