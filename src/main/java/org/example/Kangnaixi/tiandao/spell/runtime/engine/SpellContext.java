package org.example.Kangnaixi.tiandao.spell.runtime.engine;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.example.Kangnaixi.tiandao.spell.runtime.Spell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 运行时上下文, Source/Carrier/Form/Attribute/Effect 共享的数据载体.
 * 任何阶段都可以通过 {@link #put(String, Object)} 传递额外参数, 并通过 {@link #get(String, Class)} 读取.
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

    public SpellContext(Spell spell, ServerPlayer caster) {
        this.spell = spell;
        this.caster = caster;
        this.level = caster.serverLevel();

        this.castPos = caster.position();
        this.direction = caster.getLookAngle();
        this.baseDamage = spell.getBaseDamage();
        this.spiritCost = spell.getBaseSpiritCost();
        this.cooldownSeconds = spell.getBaseCooldown();
        this.projectileSpeed = spell.getBaseRange(); // 先借用范围字段做默认速度
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

    /**
     * 执行收尾: 默认只打印调试信息, 真正的生成实体/播粒子交给各模块在流程中完成.
     * 这么做可以保证流程扩展时不会忘记最后状态.
     */
    public void finalizeExecution() {
        LOGGER.debug("Spell [{}] cast by {} -> damage={}, speed={}, range={}, flags={}",
            spell.getId(), caster.getGameProfile().getName(),
            baseDamage, projectileSpeed, range, data.keySet());
    }
}
