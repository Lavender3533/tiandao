package org.example.Kangnaixi.tiandao.spell.runtime;

import org.example.Kangnaixi.tiandao.spell.definition.SpellDefinition;

/**
 * 可变的术法数值包，用于在施法过程中应用倍率。
 */
public final class SpellRuntimeNumbers {

    private double damage;
    private double projectileSpeed;
    private double range;
    private int channelTicks;
    private int durationTicks;
    private double cooldownSeconds;
    private double spiritCost;

    private SpellRuntimeNumbers() {}

    public static SpellRuntimeNumbers fromDefinition(SpellDefinition.Numbers numbers) {
        SpellRuntimeNumbers runtime = new SpellRuntimeNumbers();
        runtime.damage = numbers.baseDamage();
        runtime.projectileSpeed = numbers.projectileSpeed();
        runtime.range = numbers.areaRange();
        runtime.channelTicks = numbers.channelTicks();
        runtime.durationTicks = numbers.durationTicks();
        runtime.cooldownSeconds = numbers.cooldownSeconds();
        runtime.spiritCost = numbers.spiritCost();
        return runtime;
    }

    public void scaleDamage(double multiplier) {
        damage *= multiplier;
    }

    public void addDamage(double add) {
        damage += add;
    }

    public void scaleSpeed(double multiplier) {
        projectileSpeed *= multiplier;
    }

    public void scaleRange(double multiplier) {
        range *= multiplier;
    }

    public void scaleCooldown(double multiplier) {
        cooldownSeconds *= multiplier;
    }

    public void scaleSpiritCost(double multiplier) {
        spiritCost *= multiplier;
    }

    public double getDamage() {
        return damage;
    }

    public double getProjectileSpeed() {
        return projectileSpeed;
    }

    public double getRange() {
        return range;
    }

    public int getChannelTicks() {
        return channelTicks;
    }

    public int getDurationTicks() {
        return durationTicks;
    }

    public double getCooldownSeconds() {
        return cooldownSeconds;
    }

    public double getSpiritCost() {
        return spiritCost;
    }

    public SpellDefinition.Numbers freeze() {
        return new SpellDefinition.Numbers(damage, projectileSpeed, range, channelTicks,
            durationTicks, cooldownSeconds, spiritCost);
    }
}
