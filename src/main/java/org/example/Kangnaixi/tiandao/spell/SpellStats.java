package org.example.Kangnaixi.tiandao.spell;

import org.example.Kangnaixi.tiandao.cultivation.CultivationRealm;

/**
 * 术法基础属性
 * 包含伤害、速度、范围等数值属性
 */
public class SpellStats {

    private final double damage;
    private final double speed;
    private final double range;
    private final double cooldown;
    private final double spiritCost;
    private final int channelTicks;
    private final int durationTicks;
    private final CultivationRealm requiredRealm;
    private final int requiredStage;

    public SpellStats(double damage, double speed, double range,
                   double cooldown, double spiritCost,
                   int channelTicks, int durationTicks,
                   CultivationRealm requiredRealm, int requiredStage) {
        this.damage = damage;
        this.speed = speed;
        this.range = range;
        this.cooldown = cooldown;
        this.spiritCost = spiritCost;
        this.channelTicks = channelTicks;
        this.durationTicks = durationTicks;
        this.requiredRealm = requiredRealm;
        this.requiredStage = requiredStage;
    }

    /**
     * 获取最终伤害（包含属性加成）
     */
    public double getFinalDamage(SpellAttributes attributes) {
        double attributeScaling = attributes.getTotalDamageScaling();
        return damage * (1.0 + attributeScaling);
    }

    public double getDamage() {
        return damage;
    }

    public double getSpeed() {
        return speed;
    }

    public double getRange() {
        return range;
    }

    public double getCooldown() {
        return cooldown;
    }

    public double getSpiritCost() {
        return spiritCost;
    }

    public int getChannelTicks() {
        return channelTicks;
    }

    public int getDurationTicks() {
        return durationTicks;
    }

    public CultivationRealm getRequiredRealm() {
        return requiredRealm;
    }

    public int getRequiredStage() {
        return requiredStage;
    }

    /**
     * 检查是否需要引导
     */
    public boolean requiresChanneling() {
        return channelTicks > 0;
    }

    /**
     * 检查是否为持续性效果
     */
    public boolean isDurationBased() {
        return durationTicks > 0;
    }

    @Override
    public String toString() {
        return String.format("SpellStats[damage=%.1f, speed=%.1f, range=%.1f, cooldown=%.1f, spirit=%.1f]",
                           damage, speed, range, cooldown, spiritCost);
    }
}