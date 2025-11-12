package org.example.Kangnaixi.tiandao.spell;

import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.capability.ICultivation;
import org.example.Kangnaixi.tiandao.cultivation.CultivationRealm;
import org.example.Kangnaixi.tiandao.cultivation.SubRealm;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 术法注册表
 * 管理所有可用的术法
 */
public class SpellRegistry {
    
    private static SpellRegistry instance;
    private final Map<String, SpellData> spells = new LinkedHashMap<>();
    
    private SpellRegistry() {
        registerDefaultSpells();
    }
    
    public static SpellRegistry getInstance() {
        if (instance == null) {
            instance = new SpellRegistry();
        }
        return instance;
    }
    
    /**
     * 注册默认术法
     */
    private void registerDefaultSpells() {
        // 新的术法系统将在配置接入前保持空表
        Tiandao.LOGGER.info("当前没有默认注册的术法，等待简化术法系统接管");
    }
    
    /**
     * 注册术法
     */
    public void registerSpell(SpellData spell) {
        if (spells.containsKey(spell.getId())) {
            Tiandao.LOGGER.warn("术法 {} 已存在，将被覆盖", spell.getId());
        }
        spells.put(spell.getId(), spell);
        Tiandao.LOGGER.debug("注册术法: {} ({})", spell.getName(), spell.getId());
    }
    
    /**
     * 根据ID获取术法
     */
    public SpellData getSpellById(String id) {
        return spells.get(id);
    }
    
    /**
     * 获取所有术法
     */
    public Collection<SpellData> getAllSpells() {
        return Collections.unmodifiableCollection(spells.values());
    }
    
    /**
     * 获取所有术法ID
     */
    public Set<String> getAllSpellIds() {
        return new LinkedHashSet<>(spells.keySet());
    }
    
    /**
     * 获取玩家已解锁的术法（基于境界和小境界）
     */
    public List<SpellData> getUnlockedSpells(ICultivation cultivation) {
        CultivationRealm playerRealm = cultivation.getRealm();
        SubRealm playerSubRealm = cultivation.getSubRealm();
        
        // 将小境界映射到等级范围
        int playerLevel = getSubRealmToLevel(playerSubRealm);
        
        return spells.values().stream()
            .filter(spell -> {
                // 境界高于要求
                if (playerRealm.ordinal() > spell.getRequiredRealm().ordinal()) {
                    return true;
                }
                // 境界相同，检查层级（通过小境界映射）
                if (playerRealm == spell.getRequiredRealm() && playerLevel >= spell.getRequiredLevel()) {
                    return true;
                }
                return false;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * 将小境界映射到等级范围
     * 初期 -> 1-3
     * 中期 -> 4-6
     * 后期 -> 7-9
     */
    private int getSubRealmToLevel(SubRealm subRealm) {
        switch (subRealm) {
            case EARLY:
                return 1; // 初期对应最低等级要求
            case MIDDLE:
                return 4; // 中期对应中等等级要求
            case LATE:
                return 7; // 后期对应最高等级要求
            default:
                return 1;
        }
    }
    
    /**
     * 检查术法是否存在
     */
    public boolean hasSpell(String id) {
        return spells.containsKey(id);
    }
    
    /**
     * 获取术法数量
     */
    public int getSpellCount() {
        return spells.size();
    }
    
    /**
     * 清空注册表（用于重载）
     */
    public void clear() {
        spells.clear();
        Tiandao.LOGGER.info("术法注册表已清空");
    }
    
    /**
     * 重新加载术法
     */
    public void reload() {
        clear();
        registerDefaultSpells();
        Tiandao.LOGGER.info("术法注册表已重新加载");
    }
}

