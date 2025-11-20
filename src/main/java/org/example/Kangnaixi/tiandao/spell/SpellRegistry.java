package org.example.Kangnaixi.tiandao.spell;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.capability.ICultivation;
import org.example.Kangnaixi.tiandao.cultivation.SubRealm;
import org.example.Kangnaixi.tiandao.spell.definition.SpellDefinition;
import org.example.Kangnaixi.tiandao.spell.definition.SpellDefinitionExporter;
import org.example.Kangnaixi.tiandao.spell.definition.SpellDefinitionLoader;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SpellDefinition 统一注册表。
 * 负责：
 * 1. 从 JSON 加载术法定义（内置 + 玩家创建）
 * 2. 保存术法定义到配置文件
 * 3. 注册/查询/卸载术法
 * 4. Schema 校验和冲突检测
 */
public class SpellRegistry {

    private static SpellRegistry instance;

    // 所有术法（内置 + 玩家创建）
    private final Map<ResourceLocation, SpellDefinition> spells = new LinkedHashMap<>();

    // 术法来源追踪（用于冲突日志）
    private final Map<ResourceLocation, SpellSource> spellSources = new LinkedHashMap<>();

    private final SpellDefinitionLoader loader = new SpellDefinitionLoader();

    /**
     * 术法来源
     */
    public enum SpellSource {
        BUILTIN,        // 内置术法（mod 自带）
        PLAYER_CREATED, // 玩家创建
        CONFIG_FILE     // 配置文件
    }

    private SpellRegistry() {
        reload();
    }

    public static SpellRegistry getInstance() {
        if (instance == null) {
            instance = new SpellRegistry();
        }
        return instance;
    }

    /**
     * 注册术法（带来源追踪）
     */
    public void registerSpell(SpellDefinition spell, SpellSource source) {
        ResourceLocation id = spell.getId();

        // 冲突检测
        if (spells.containsKey(id)) {
            SpellSource existingSource = spellSources.get(id);
            Tiandao.LOGGER.warn("术法 ID 冲突: {} 已存在", id);
            Tiandao.LOGGER.warn("  原来源: {} | 新来源: {}", existingSource, source);
            Tiandao.LOGGER.warn("  原术法: {}", spells.get(id).getMetadata().displayName());
            Tiandao.LOGGER.warn("  新术法: {}", spell.getMetadata().displayName());
            Tiandao.LOGGER.warn("  将覆盖原术法");
        }

        // Schema 校验
        if (!validateSpellDefinition(spell)) {
            Tiandao.LOGGER.error("术法定义校验失败: {}", id);
            return;
        }

        spells.put(id, spell);
        spellSources.put(id, source);
        Tiandao.LOGGER.info("注册术法: {} ({}) [来源: {}]",
            spell.getMetadata().displayName(), id, source);
    }

    /**
     * 兼容旧代码的注册方法（默认来源为 CONFIG_FILE）
     */
    public void registerSpell(SpellDefinition spell) {
        registerSpell(spell, SpellSource.CONFIG_FILE);
    }

    public SpellDefinition getSpell(ResourceLocation id) {
        return spells.get(id);
    }

    public SpellDefinition getSpellById(String id) {
        ResourceLocation identifier = ResourceLocation.tryParse(id);
        if (identifier == null) {
            return null;
        }
        return spells.get(identifier);
    }

    public Collection<SpellDefinition> getAllSpells() {
        return Collections.unmodifiableCollection(spells.values());
    }

    public Set<String> getAllSpellIds() {
        return spells.keySet().stream()
            .map(ResourceLocation::toString)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public List<SpellDefinition> getUnlockedSpells(ICultivation cultivation) {
        return spells.values().stream()
            .filter(spell -> {
                SpellDefinition.Metadata metadata = spell.getMetadata();
                if (metadata.requiredRealm() == null) {
                    return true;
                }
                if (cultivation.getRealm().ordinal() > metadata.requiredRealm().ordinal()) {
                    return true;
                }
                if (cultivation.getRealm() == metadata.requiredRealm()) {
                    return getSubRealmToLevel(cultivation.getSubRealm()) >= metadata.requiredStage();
                }
                return false;
            })
            .collect(Collectors.toList());
    }

    private int getSubRealmToLevel(SubRealm subRealm) {
        switch (subRealm) {
            case MIDDLE:
                return 4;
            case LATE:
                return 7;
            case EARLY:
            default:
                return 1;
        }
    }

    public boolean hasSpell(String id) {
        return getSpellById(id) != null;
    }

    public int getSpellCount() {
        return spells.size();
    }

    /**
     * 统一的保存接口：保存术法定义到配置文件并注册
     * @param definition 术法定义
     * @param source 术法来源
     * @param allowOverwrite 是否允许覆盖已存在的术法（默认true）
     * @return 注册结果
     */
    public RegisterResult saveAndRegister(SpellDefinition definition, SpellSource source, boolean allowOverwrite) {
        ResourceLocation id = definition.getId();

        // Schema 校验
        if (!validateSpellDefinition(definition)) {
            return RegisterResult.failure(id.toString(), "Schema 校验失败");
        }

        // 冲突检测
        if (spells.containsKey(id)) {
            SpellSource existingSource = spellSources.get(id);
            SpellDefinition existingSpell = spells.get(id);

            // 记录详细的冲突日志
            Tiandao.LOGGER.warn("==================== 术法 ID 冲突检测 ====================");
            Tiandao.LOGGER.warn("冲突ID: {}", id);
            Tiandao.LOGGER.warn("已存在术法: {} (来源: {})", existingSpell.getMetadata().displayName(), existingSource);
            Tiandao.LOGGER.warn("新术法: {} (来源: {})", definition.getMetadata().displayName(), source);

            if (!allowOverwrite) {
                String errorMsg = String.format(
                    "术法ID已存在：%s (已有术法: %s)。请使用不同的ID或点击'生成'按钮创建新ID。",
                    id, existingSpell.getMetadata().displayName()
                );
                Tiandao.LOGGER.warn("拒绝覆盖: {}", errorMsg);
                Tiandao.LOGGER.warn("=====================================================");
                return RegisterResult.failure(id.toString(), errorMsg);
            }

            Tiandao.LOGGER.warn("允许覆盖: 将使用新术法覆盖原术法");
            Tiandao.LOGGER.warn("=====================================================");
        }

        // 保存到配置文件（仅玩家创建的术法）
        if (source == SpellSource.PLAYER_CREATED) {
            try {
                JsonObject json = definition.toJson();
                SpellDefinitionExporter.saveToConfig(id.toString(), json);
                Tiandao.LOGGER.info("术法定义已保存到配置文件: {} -> {}_xxx.json", id,
                    id.toString().replace(':', '_').replace('/', '_'));
            } catch (IOException e) {
                Tiandao.LOGGER.error("保存术法定义失败: {}", id, e);
                return RegisterResult.failure(id.toString(), "保存文件失败: " + e.getMessage());
            }
        }

        // 注册到内存
        registerSpell(definition, source);

        return RegisterResult.success(id.toString());
    }

    /**
     * 统一的保存接口：保存术法定义到配置文件并注册（默认不允许覆盖）
     */
    public RegisterResult saveAndRegister(SpellDefinition definition, SpellSource source) {
        return saveAndRegister(definition, source, false);
    }

    /**
     * Schema 校验
     */
    private boolean validateSpellDefinition(SpellDefinition definition) {
        if (definition == null) {
            Tiandao.LOGGER.error("术法定义为 null");
            return false;
        }

        if (definition.getId() == null) {
            Tiandao.LOGGER.error("术法 ID 为 null");
            return false;
        }

        if (definition.getSource() == null) {
            Tiandao.LOGGER.error("术法 {} 缺少 source 组件", definition.getId());
            return false;
        }

        if (definition.getCarrier() == null) {
            Tiandao.LOGGER.error("术法 {} 缺少 carrier 组件", definition.getId());
            return false;
        }

        if (definition.getForm() == null) {
            Tiandao.LOGGER.error("术法 {} 缺少 form 组件", definition.getId());
            return false;
        }

        if (definition.getMetadata() == null) {
            Tiandao.LOGGER.error("术法 {} 缺少 metadata", definition.getId());
            return false;
        }

        if (definition.getBaseStats() == null) {
            Tiandao.LOGGER.error("术法 {} 缺少 base_stats", definition.getId());
            return false;
        }

        // 数值合理性校验
        if (definition.getBaseStats().baseDamage() < 0) {
            Tiandao.LOGGER.warn("术法 {} 的伤害为负数: {}", definition.getId(),
                definition.getBaseStats().baseDamage());
        }

        if (definition.getBaseStats().spiritCost() < 0) {
            Tiandao.LOGGER.warn("术法 {} 的灵力消耗为负数: {}", definition.getId(),
                definition.getBaseStats().spiritCost());
        }

        return true;
    }

    /**
     * 获取术法来源
     */
    public SpellSource getSpellSource(ResourceLocation id) {
        return spellSources.getOrDefault(id, SpellSource.CONFIG_FILE);
    }

    /**
     * 获取术法来源（字符串 ID）
     */
    public SpellSource getSpellSource(String id) {
        ResourceLocation identifier = ResourceLocation.tryParse(id);
        if (identifier == null) {
            return null;
        }
        return getSpellSource(identifier);
    }

    public void clear() {
        spells.clear();
        spellSources.clear();
        Tiandao.LOGGER.info("术法注册表已清空");
    }

    public void reload() {
        clear();
        List<SpellDefinition> loaded = loader.loadAll();
        loaded.forEach(spell -> registerSpell(spell, SpellSource.CONFIG_FILE));
        Tiandao.LOGGER.info("加载 {} 条术法定义", spells.size());
    }

    /**
     * 注册结果
     */
    public static class RegisterResult {
        private final boolean success;
        private final String spellId;
        private final String errorMessage;

        private RegisterResult(boolean success, String spellId, String errorMessage) {
            this.success = success;
            this.spellId = spellId;
            this.errorMessage = errorMessage;
        }

        public static RegisterResult success(String spellId) {
            return new RegisterResult(true, spellId, null);
        }

        public static RegisterResult failure(String spellId, String errorMessage) {
            return new RegisterResult(false, spellId, errorMessage);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getSpellId() {
            return spellId;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
