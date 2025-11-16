package org.example.Kangnaixi.tiandao.spell.node.execution;

/**
 * 同一术法执行链共享的修饰数据。
 * Modifier 组件将值写入此对象，TargetStage 和 Effect 可从中读取。
 */
public class SpellExecutionModifiers {

    private double damageMultiplier = 1.0;
    private double healMultiplier = 1.0;
    private double areaScale = 1.0;

    private int maxTargets = Integer.MAX_VALUE;
    private int bounceCount = 0;
    private int penetration = 0;

    private int travelDelayTicks = 0;
    private int durationTicks = 0;

    public double getDamageMultiplier() {
        return damageMultiplier;
    }

    public void multiplyDamage(double multiplier) {
        this.damageMultiplier *= multiplier;
    }

    public double getHealMultiplier() {
        return healMultiplier;
    }

    public void multiplyHeal(double multiplier) {
        this.healMultiplier *= multiplier;
    }

    public double getAreaScale() {
        return areaScale;
    }

    public void scaleArea(double multiplier) {
        this.areaScale *= multiplier;
    }

    public int getMaxTargets() {
        return maxTargets;
    }

    public void setMaxTargets(int maxTargets) {
        this.maxTargets = Math.max(1, maxTargets);
    }

    public int getBounceCount() {
        return bounceCount;
    }

    public void setBounceCount(int bounceCount) {
        this.bounceCount = Math.max(0, bounceCount);
    }

    public int getPenetration() {
        return penetration;
    }

    public void setPenetration(int penetration) {
        this.penetration = Math.max(0, penetration);
    }

    public int getTravelDelayTicks() {
        return travelDelayTicks;
    }

    public void addTravelDelay(int ticks) {
        this.travelDelayTicks += Math.max(0, ticks);
    }

    public int getDurationTicks() {
        return durationTicks;
    }

    public void setDurationTicks(int durationTicks) {
        this.durationTicks = Math.max(0, durationTicks);
    }

    public void reset() {
        damageMultiplier = 1.0;
        healMultiplier = 1.0;
        areaScale = 1.0;
        maxTargets = Integer.MAX_VALUE;
        bounceCount = 0;
        penetration = 0;
        travelDelayTicks = 0;
        durationTicks = 0;
    }
}

