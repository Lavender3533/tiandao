package org.example.Kangnaixi.tiandao.spell.effect;

import org.example.Kangnaixi.tiandao.Tiandao;

import javax.annotation.Nullable;
import java.util.*;

/**
 * 效果注册表 - 管理所有效果执行器
 */
public class EffectRegistry {

    private static EffectRegistry instance;
    private final Map<String, EffectExecutor> effects = new LinkedHashMap<>();

    private EffectRegistry() {
        registerDefaultEffects();
    }

    public static EffectRegistry getInstance() {
        if (instance == null) {
            instance = new EffectRegistry();
        }
        return instance;
    }

    /**
     * 注册默认效果
     */
    private void registerDefaultEffects() {
        registerEffect("damage", new DamageEffect(5.0));
        registerEffect("heal", new HealEffect(5.0));
        registerEffect("push", new PushEffect(1.5));
        registerEffect("explosion", new ExplosionEffect(3.0));  // 爆炸威力3.0
        registerEffect("teleport", new TeleportEffect(10.0));   // 最大传送距离10格

        Tiandao.LOGGER.info("已注册 {} 个默认效果", effects.size());
    }

    /**
     * 注册效果
     */
    public void registerEffect(String id, EffectExecutor effect) {
        if (effects.containsKey(id)) {
            Tiandao.LOGGER.warn("效果 {} 已存在，将被覆盖", id);
        }
        effects.put(id, effect);
        Tiandao.LOGGER.debug("注册效果: {} ({})", effect.getName(), id);
    }

    /**
     * 根据ID获取效果
     */
    @Nullable
    public EffectExecutor getEffect(String id) {
        return effects.get(id);
    }

    /**
     * 获取所有效果
     */
    public Collection<EffectExecutor> getAllEffects() {
        return Collections.unmodifiableCollection(effects.values());
    }

    /**
     * 检查效果是否存在
     */
    public boolean hasEffect(String id) {
        return effects.containsKey(id);
    }

    /**
     * 清空注册表
     */
    public void clear() {
        effects.clear();
        Tiandao.LOGGER.info("效果注册表已清空");
    }
}
