package org.example.Kangnaixi.tiandao.spell.runtime.engine;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.example.Kangnaixi.tiandao.spell.runtime.Spell;
import org.example.Kangnaixi.tiandao.spell.runtime.engine.carrier.CarrierExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 运行时上下文, Source/Carrier/Form/Attribute/Effect 共享的数据载体.
 * 支持 Tick 状态，以驱动延迟/引导/持续技能。
 */
public class SpellContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpellContext.class);

    private final Spell spell;
    private final ServerPlayer caster;
    private final ServerLevel level;

    private Vec3 castPos;
    private Vec3 direction;
    private double baseDamage;
    private double spiritCost;
    private double cooldownSeconds;
    private double projectileSpeed;
    private double range;

    private final Map<String, Object> data = new HashMap<>();
    private CarrierExecutor carrierExecutor;

    private int ticksExisted = 0;
    private int maxDuration = 0;
    private boolean channeling = false;
    private boolean delayed = false;
    private int delayTicks = 0;
    private boolean durationSkill = false;
    private boolean finished = false;

    public SpellContext(Spell spell, ServerPlayer caster) {
        this.spell = spell;
        this.caster = caster;
        this.level = caster.serverLevel();

        this.castPos = caster.position();
        this.direction = caster.getLookAngle();
        this.baseDamage = spell.getBaseDamage();
        this.spiritCost = spell.getBaseSpiritCost();
        this.cooldownSeconds = spell.getBaseCooldown();
        this.projectileSpeed = spell.getBaseRange();
        this.range = spell.getBaseRange();
    }

    public Spell spell() { return spell; }
    public ServerPlayer caster() { return caster; }
    public ServerLevel level() { return level; }

    public Vec3 castPos() { return castPos; }
    public void setCastPos(Vec3 castPos) { this.castPos = castPos; }

    public Vec3 direction() { return direction; }
    public void setDirection(Vec3 direction) { this.direction = direction; }

    public double baseDamage() { return baseDamage; }
    public void setBaseDamage(double baseDamage) { this.baseDamage = baseDamage; }

    public double spiritCost() { return spiritCost; }
    public void setSpiritCost(double spiritCost) { this.spiritCost = spiritCost; }

    public double cooldownSeconds() { return cooldownSeconds; }
    public void setCooldownSeconds(double cooldownSeconds) { this.cooldownSeconds = cooldownSeconds; }

    public double projectileSpeed() { return projectileSpeed; }
    public void setProjectileSpeed(double projectileSpeed) { this.projectileSpeed = projectileSpeed; }

    public double range() { return range; }
    public void setRange(double range) { this.range = range; }

    public int ticksExisted() { return ticksExisted; }
    public void incrementTicks() { this.ticksExisted++; }
    public void resetTicks() { this.ticksExisted = 0; }

    public int maxDuration() { return maxDuration; }
    public void setMaxDuration(int maxDuration) { this.maxDuration = maxDuration; }

    public boolean channeling() { return channeling; }
    public void setChanneling(boolean channeling) { this.channeling = channeling; }

    public boolean delayed() { return delayed; }
    public void setDelayed(boolean delayed) { this.delayed = delayed; }

    public int delayTicks() { return delayTicks; }
    public void setDelayTicks(int delayTicks) { this.delayTicks = delayTicks; }

    public boolean durationSkill() { return durationSkill; }
    public void setDurationSkill(boolean durationSkill) { this.durationSkill = durationSkill; }

    public void put(String key, Object value) {
        data.put(key, value);
    }

    public <T> Optional<T> get(String key, Class<T> type) {
        Object value = data.get(key);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(type.cast(value));
    }

    public boolean has(String key) {
        return data.containsKey(key);
    }

    public void setCarrierExecutor(CarrierExecutor executor) {
        this.carrierExecutor = executor;
    }

    public void spawnCarrier() {
        if (carrierExecutor != null) {
            carrierExecutor.createCarrier(this);
        }
    }

    public void finish() {
        this.finished = true;
    }

    public boolean isFinished() {
        return finished;
    }

    public void finalizeExecution() {
        LOGGER.debug("Spell [{}] cast by {} -> damage={}, speed={}, range={}, flags={}",
            spell.getId(), caster.getGameProfile().getName(),
            baseDamage, projectileSpeed, range, data.keySet());
    }
}
