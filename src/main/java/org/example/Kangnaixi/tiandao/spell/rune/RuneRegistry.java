package org.example.Kangnaixi.tiandao.spell.rune;

import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.cultivation.CultivationRealm;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 符文注册表 - 管理所有可用的符文
 */
public class RuneRegistry {

    private static RuneRegistry instance;
    private final Map<String, Rune> runes = new LinkedHashMap<>();

    private RuneRegistry() {
    }

    public static RuneRegistry getInstance() {
        if (instance == null) {
            instance = new RuneRegistry();
        }
        return instance;
    }

    /**
     * 注册符文
     */
    public void registerRune(Rune rune) {
        if (runes.containsKey(rune.getId())) {
            Tiandao.LOGGER.warn("符文 {} 已存在，将被覆盖", rune.getId());
        }
        runes.put(rune.getId(), rune);
        Tiandao.LOGGER.debug("注册符文: {} ({})", rune.getName(), rune.getId());
    }

    /**
     * 根据ID获取符文
     */
    @Nullable
    public Rune getRuneById(String id) {
        return runes.get(id);
    }

    /**
     * 获取所有符文
     */
    public Collection<Rune> getAllRunes() {
        return Collections.unmodifiableCollection(runes.values());
    }

    /**
     * 根据分类获取符文
     */
    public List<Rune> getRunesByCategory(Rune.RuneCategory category) {
        return runes.values().stream()
            .filter(rune -> rune.getCategory() == category)
            .collect(Collectors.toList());
    }

    /**
     * 根据等阶获取符文
     */
    public List<Rune> getRunesByTier(Rune.RuneTier tier) {
        return runes.values().stream()
            .filter(rune -> rune.getTier() == tier)
            .collect(Collectors.toList());
    }

    /**
     * 获取玩家已解锁的符文
     */
    public List<Rune> getUnlockedRunes(CultivationRealm playerRealm, int playerLevel) {
        return runes.values().stream()
            .filter(rune -> isRuneUnlocked(rune, playerRealm, playerLevel))
            .collect(Collectors.toList());
    }

    /**
     * 检查符文是否解锁
     */
    public boolean isRuneUnlocked(Rune rune, CultivationRealm playerRealm, int playerLevel) {
        CultivationRealm requiredRealm = CultivationRealm.valueOf(rune.getUnlockRealm());

        // 境界高于要求
        if (playerRealm.ordinal() > requiredRealm.ordinal()) {
            return true;
        }

        // 境界相同，检查层级
        if (playerRealm == requiredRealm && playerLevel >= rune.getUnlockLevel()) {
            return true;
        }

        return false;
    }

    /**
     * 检查符文是否存在
     */
    public boolean hasRune(String id) {
        return runes.containsKey(id);
    }

    /**
     * 获取符文数量
     */
    public int getRuneCount() {
        return runes.size();
    }

    /**
     * 清空注册表
     */
    public void clear() {
        runes.clear();
        Tiandao.LOGGER.info("符文注册表已清空");
    }

    /**
     * 重新加载符文
     */
    public void reload() {
        clear();
        Tiandao.LOGGER.info("符文注册表已重新加载");
    }

    /**
     * 获取触发符文列表（用于GUI）
     */
    public List<Rune> getTriggerRunes() {
        return getRunesByCategory(Rune.RuneCategory.TRIGGER);
    }

    /**
     * 获取形状符文列表（用于GUI）
     */
    public List<Rune> getShapeRunes() {
        return getRunesByCategory(Rune.RuneCategory.SHAPE);
    }

    /**
     * 获取效果符文列表（用于GUI）
     */
    public List<Rune> getEffectRunes() {
        return getRunesByCategory(Rune.RuneCategory.EFFECT);
    }
}
