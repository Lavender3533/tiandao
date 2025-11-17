package org.example.Kangnaixi.tiandao.spell.runtime.engine;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.example.Kangnaixi.tiandao.spell.runtime.Spell;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 术法运行时上下文
 * 包含施法者、世界、术法定义以及执行过程中的共享数据
 */
public class SpellContext {

    // 基础信息
    private final ServerLevel level;
    private final ServerPlayer caster;
    private final Spell spell;

    // 位置与方向
    private final Vec3 origin;
    private final Vec3 direction;

    // 术法参数
    private double damage;
    private double range;
    private double speed;
    private double radius;

    // 共享数据字典
    private final Map<String, Object> data;

    /**
     * 创建术法上下文
     */
    public SpellContext(ServerLevel level, ServerPlayer caster, Spell spell) {
        this.level = level;
        this.caster = caster;
        this.spell = spell;
        this.origin = caster.position().add(0, caster.getEyeHeight(), 0);
        this.direction = caster.getLookAngle().normalize();
        this.data = new HashMap<>();

        // 初始化基础参数
        this.damage = spell.getBaseDamage();
        this.range = spell.getBaseRange();
        this.speed = 1.0; // 默认速度
        this.radius = 1.0; // 默认半径
    }

    // ========== Getters ==========

    public ServerLevel getLevel() {
        return level;
    }

    public ServerPlayer getCaster() {
        return caster;
    }

    public Spell getSpell() {
        return spell;
    }

    public Vec3 getOrigin() {
        return origin;
    }

    public Vec3 getDirection() {
        return direction;
    }

    public double getDamage() {
        return damage;
    }

    public double getRange() {
        return range;
    }

    public double getSpeed() {
        return speed;
    }

    public double getRadius() {
        return radius;
    }

    // ========== Setters ==========

    public void setDamage(double damage) {
        this.damage = damage;
    }

    public void setRange(double range) {
        this.range = range;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    // ========== 数据字典操作 ==========

    /**
     * 存储数据到上下文
     */
    public void putData(String key, Object value) {
        data.put(key, value);
    }

    /**
     * 获取数据（原始类型）
     */
    public Object getData(String key) {
        return data.get(key);
    }

    /**
     * 获取数据（带类型转换）
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getData(String key, Class<T> type) {
        Object value = data.get(key);
        if (value != null && type.isInstance(value)) {
            return Optional.of((T) value);
        }
        return Optional.empty();
    }

    /**
     * 检查是否包含指定键
     */
    public boolean hasData(String key) {
        return data.containsKey(key);
    }

    /**
     * 移除数据
     */
    public void removeData(String key) {
        data.remove(key);
    }

    /**
     * 清空所有数据
     */
    public void clearData() {
        data.clear();
    }
}
