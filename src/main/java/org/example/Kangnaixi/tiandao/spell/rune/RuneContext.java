package org.example.Kangnaixi.tiandao.spell.rune;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 符文执行上下文 - 存储符文执行过程中的状态
 */
public class RuneContext {

    private final Player caster;
    private final Level level;
    private Vec3 position;
    private Vec3 direction;
    @Nullable
    private Entity target;
    private final List<Entity> affectedEntities = new ArrayList<>();
    private final Map<String, Object> variables = new HashMap<>();

    public RuneContext(Player caster, Level level) {
        this.caster = caster;
        this.level = level;
        this.position = caster.position();
        this.direction = caster.getLookAngle();
    }

    // Getters and Setters
    public Player getCaster() { return caster; }
    public Level getLevel() { return level; }
    public Vec3 getPosition() { return position; }
    public void setPosition(Vec3 position) { this.position = position; }
    public Vec3 getDirection() { return direction; }
    public void setDirection(Vec3 direction) { this.direction = direction; }
    @Nullable
    public Entity getTarget() { return target; }
    public void setTarget(@Nullable Entity target) { this.target = target; }
    public List<Entity> getAffectedEntities() { return affectedEntities; }

    // 变量存储
    public void setVariable(String key, Object value) {
        variables.put(key, value);
    }

    @Nullable
    public Object getVariable(String key) {
        return variables.get(key);
    }

    public boolean hasVariable(String key) {
        return variables.containsKey(key);
    }
}
