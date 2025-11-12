package org.example.Kangnaixi.tiandao.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.cultivation.SpiritualRoot;
import org.example.Kangnaixi.tiandao.cultivation.SpiritualRootType;
import org.example.Kangnaixi.tiandao.cultivation.CultivationRealm;
import org.example.Kangnaixi.tiandao.cultivation.SubRealm;
import org.example.Kangnaixi.tiandao.spell.blueprint.SpellBlueprint;

/**
 * 修仙能力接口
 */
public interface ICultivation {
    
    /**
     * 获取灵根类型
     */
    SpiritualRootType getSpiritualRoot();
    
    /**
     * 设置灵根类型
     */
    void setSpiritualRoot(SpiritualRootType root);
    
    /**
     * 获取完整的灵根对象
     */
    SpiritualRoot getSpiritualRootObject();
    
    /**
     * 设置完整的灵根对象
     */
    void setSpiritualRootObject(SpiritualRoot spiritualRoot);
    
    /**
     * 获取境界
     */
    CultivationRealm getRealm();
    
    /**
     * 设置境界
     */
    void setRealm(CultivationRealm realm);
    
    /**
     * 获取等级（已弃用，保留用于兼容性和数据迁移）
     * @deprecated 使用新的小境界系统，此方法仅用于向后兼容
     */
    @Deprecated
    int getLevel();
    
    /**
     * 设置等级（已弃用，保留用于兼容性和数据迁移）
     * @deprecated 使用新的小境界系统，此方法仅用于向后兼容
     */
    @Deprecated
    void setLevel(int level);
    
    /**
     * 获取小境界
     */
    SubRealm getSubRealm();
    
    /**
     * 设置小境界
     */
    void setSubRealm(SubRealm subRealm);
    
    /**
     * 获取根基值（0-100）
     */
    int getFoundation();
    
    /**
     * 设置根基值（0-100）
     */
    void setFoundation(int foundation);
    
    /**
     * 增加根基值
     */
    void addFoundation(int amount);
    
    /**
     * 减少根基值
     */
    void reduceFoundation(int amount);
    
    /**
     * 获取修炼进度
     */
    double getCultivationProgress();
    
    /**
     * 设置修炼进度
     */
    void setCultivationProgress(double progress);
    
    /**
     * 增加修炼进度
     */
    void addCultivationProgress(double amount);
    
    /**
     * 获取灵力
     */
    double getSpiritPower();
    
    /**
     * 获取当前灵力（别名方法）
     */
    default double getCurrentSpiritPower() {
        return getSpiritPower();
    }
    
    /**
     * 设置灵力
     */
    void setSpiritPower(double power);
    
    /**
     * 获取最大灵力
     */
    double getMaxSpiritPower();
    
    /**
     * 设置最大灵力
     */
    void setMaxSpiritPower(double maxPower);
    
    /**
     * 增加灵力
     */
    void addSpiritPower(double amount);
    
    /**
     * 消耗灵力
     */
    boolean consumeSpiritPower(double amount);
    
    /**
     * 尝试突破境界
     */
    boolean tryBreakthrough();
    
    /**
     * 尝试升级
     */
    boolean tryLevelUp();
    
    /**
     * 获取修炼速度加成
     */
    double getCultivationBonus();
    
    /**
     * 获取基础灵力恢复速度（不含环境密度和强度加成）
     */
    double getSpiritPowerRecoveryRate();
    
    /**
     * 获取当前环境灵力密度系数
     */
    double getEnvironmentalDensity();
    
    /**
     * 设置当前环境灵力密度系数
     */
    void setEnvironmentalDensity(double density);
    
    /**
     * 获取灵力强度加成系数（用于客户端预测）
     */
    double getIntensityBonus();
    
    /**
     * 设置灵力强度加成系数
     */
    void setIntensityBonus(double bonus);
    
    /**
     * 从另一个能力复制数据
     */
    void copyFrom(ICultivation other);
    
    /**
     * 检查是否已分配灵根
     */
    boolean hasRootAssigned();
    
    /**
     * 设置灵根已分配标记
     */
    void setRootAssigned(boolean assigned);
    
    // === 修炼系统相关接口 ===
    
    /**
     * 检查玩家是否正在修炼
     */
    boolean isPracticing();
    
    /**
     * 设置修炼状态
     */
    void setPracticing(boolean practicing);
    
    /**
     * 获取当前使用的修炼方式ID
     */
    String getCurrentPracticeMethod();
    
    /**
     * 设置当前修炼方式
     */
    void setCurrentPracticeMethod(String methodId);
    
    /**
     * 获取修炼经验
     */
    int getCultivationExperience();
    
    /**
     * 设置修炼经验
     */
    void setCultivationExperience(int experience);
    
    /**
     * 增加修炼经验
     */
    void addCultivationExperience(int amount);
    
    /**
     * 获取当前层级所需的经验（已弃用，保留用于兼容性）
     * @deprecated 使用新的小境界系统，此方法仅用于向后兼容
     */
    @Deprecated
    int getRequiredExperienceForLevel();
    
    /**
     * 获取当前小境界所需的经验
     */
    int getRequiredExperienceForSubRealm();
    
    /**
     * 获取时间加速倍数（打坐时）
     */
    double getTimeAcceleration();
    
    /**
     * 设置时间加速倍数
     */
    void setTimeAcceleration(double multiplier);
    
    /**
     * 获取打坐开始时间（游戏tick）
     */
    long getPracticeStartTime();
    
    /**
     * 设置打坐开始时间（游戏tick）
     */
    void setPracticeStartTime(long time);
    
    /**
     * 获取上次受到伤害的时间（游戏时间tick）
     */
    long getLastCombatTime();
    
    /**
     * 设置上次受到伤害的时间
     */
    void setLastCombatTime(long time);
    
    /**
     * 检查玩家是否在战斗状态（5秒内受伤）
     */
    boolean isInCombat();
    
    // === 功法管理接口 ===
    
    /**
     * 获取已学习的功法列表
     */
    java.util.List<org.example.Kangnaixi.tiandao.technique.TechniqueData> getLearnedTechniques();
    
    /**
     * 学习新功法
     */
    boolean learnTechnique(org.example.Kangnaixi.tiandao.technique.TechniqueData technique);
    
    /**
     * 遗忘功法
     */
    boolean forgetTechnique(String techniqueId);
    
    /**
     * 检查是否已学习指定功法
     */
    boolean hasTechnique(String techniqueId);
    
    /**
     * 根据ID获取已学习的功法
     */
    org.example.Kangnaixi.tiandao.technique.TechniqueData getTechniqueById(String techniqueId);
    
    /**
     * 获取当前装备的功法
     */
    org.example.Kangnaixi.tiandao.technique.TechniqueData getEquippedTechnique();
    
    /**
     * 装备功法
     * @return 是否成功装备
     */
    boolean equipTechnique(String techniqueId);
    
    /**
     * 卸下当前功法
     */
    void unequipTechnique();
    
    /**
     * 检查是否有装备功法
     */
    boolean hasEquippedTechnique();
    
    /**
     * 获取功法装备时间戳（游戏tick）
     */
    long getTechniqueEquipTime();
    
    /**
     * 设置功法装备时间戳
     */
    void setTechniqueEquipTime(long time);
    
    /**
     * 获取功法装备持续时间（游戏tick）
     */
    long getTechniqueEquipDuration();
    
    // ==================== 术法系统 ====================
    
    /**
     * 获取已解锁的术法ID列表
     */
    java.util.List<String> getUnlockedSpells();
    
    /**
     * 解锁术法
     */
    void unlockSpell(String spellId);
    
    /**
     * 检查术法是否已解锁
     */
    boolean hasSpell(String spellId);
    
    /**
     * 获取术法快捷栏（4个槽位）
     * 返回数组，元素为术法ID，null表示空槽位
     */
    String[] getSpellHotbar();
    
    /**
     * 设置术法快捷栏槽位
     * @param slot 槽位索引 (0-3)
     * @param spellId 术法ID，null表示清空
     */
    void setSpellHotbar(int slot, String spellId);
    
    /**
     * 获取术法冷却结束时间
     * @return Map<术法ID, 冷却结束时间戳>
     */
    java.util.Map<String, Long> getSpellCooldowns();
    
    /**
     * 设置术法冷却
     */
    void setSpellCooldown(String spellId, long endTime);
    
    /**
     * 获取术法冷却剩余时间（秒）
     */
    int getSpellCooldownRemaining(String spellId);
    
    /**
     * 获取当前激活的持续性术法
     * @return Map<术法ID, 效果结束时间戳>
     */
    java.util.Map<String, Long> getActiveSpells();
    
    /**
     * 激活持续性术法
     */
    void activateSpell(String spellId, long endTime);
    
    /**
     * 取消持续性术法
     */
    void deactivateSpell(String spellId);
    
    /**
     * 检查术法是否激活
     */
    boolean isSpellActive(String spellId);

    /**
     * 获取已掌握的术法蓝图
     */
    java.util.List<SpellBlueprint> getKnownBlueprints();

    /**
     * 学习新的术法蓝图
     */
    void learnBlueprint(SpellBlueprint blueprint);

    /**
     * 清空已掌握的蓝图（用于读取 NBT 前重置）
     */
    void clearBlueprints();

    /**
     * 查询是否已掌握指定蓝图
     */
    boolean knowsBlueprint(String blueprintId);
}
