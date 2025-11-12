package org.example.Kangnaixi.tiandao.capability;

import net.minecraft.world.entity.player.Player;
import org.example.Kangnaixi.tiandao.cultivation.SpiritualRoot;
import org.example.Kangnaixi.tiandao.cultivation.SpiritualRootType;
import org.example.Kangnaixi.tiandao.cultivation.CultivationRealm;
import org.example.Kangnaixi.tiandao.cultivation.SubRealm;
import org.example.Kangnaixi.tiandao.cultivation.TechniqueChecker;
import org.example.Kangnaixi.tiandao.spell.blueprint.SpellBlueprint;

/**
 * 修仙能力实现类
 */
public class CultivationCapability implements ICultivation {
    
    private SpiritualRoot spiritualRoot = new SpiritualRoot(SpiritualRootType.NONE);
    private CultivationRealm realm = CultivationRealm.MORTAL;
    private int level = 1; // 已弃用，保留用于数据迁移
    private SubRealm subRealm = SubRealm.EARLY; // 小境界
    private int legacyLevel = 1; // 迁移前的等级（备份）
    private int foundation = 100; // 根基值（0-100）
    private double cultivationProgress = 0.0;
    private double spiritPower = 100.0;
    private double maxSpiritPower = 100.0;
    private double environmentalDensity = 1.0; // 当前环境灵力密度系数
    private double intensityBonus = 1.0; // 灵力强度加成系数（用于客户端预测）
    private boolean rootAssigned = false; // 是否已分配灵根
    
    // 修炼系统相关字段
    private boolean practicing = false; // 是否正在修炼
    private String currentPracticeMethod = ""; // 当前修炼方式ID
    private int cultivationExperience = 0; // 修炼经验
    private long lastCombatTime = 0; // 上次受伤时间
    private double timeAcceleration = 1.0; // 时间加速倍数（打坐时）
    private long practiceStartTime = 0; // 打坐开始时间（游戏tick）
    
    // 功法系统相关字段
    private java.util.List<org.example.Kangnaixi.tiandao.technique.TechniqueData> learnedTechniques = new java.util.ArrayList<>(); // 已学习的功法列表
    private org.example.Kangnaixi.tiandao.technique.TechniqueData equippedTechnique = null; // 当前装备的功法
    private long techniqueEquipTime = 0; // 功法装备时间戳（游戏tick）
    
    // 术法系统相关字段
    private java.util.List<String> unlockedSpells = new java.util.ArrayList<>(); // 已解锁的术法ID列表
    private String[] spellHotbar = new String[4]; // 术法快捷栏（4个槽位）
    private java.util.Map<String, Long> spellCooldowns = new java.util.HashMap<>(); // 术法冷却时间 <术法ID, 冷却结束时间戳>
    private java.util.Map<String, Long> activeSpells = new java.util.HashMap<>(); // 激活的持续性术法 <术法ID, 效果结束时间戳>
    private final java.util.List<SpellBlueprint> knownBlueprints = new java.util.ArrayList<>(); // 已掌握的术法蓝图
    
    @Override
    public SpiritualRootType getSpiritualRoot() {
        return spiritualRoot.getType();
    }
    
    @Override
    public void setSpiritualRoot(SpiritualRootType root) {
        // 如果类型改变，创建新的SpiritualRoot对象，保持原有品质
        if (this.spiritualRoot.getType() != root) {
            this.spiritualRoot = new SpiritualRoot(root, this.spiritualRoot.getQuality());
        }
    }
    
    /**
     * 获取完整的灵根对象
     */
    public SpiritualRoot getSpiritualRootObject() {
        return spiritualRoot;
    }
    
    /**
     * 设置完整的灵根对象
     */
    public void setSpiritualRootObject(SpiritualRoot spiritualRoot) {
        this.spiritualRoot = spiritualRoot;
    }
    
    @Override
    public CultivationRealm getRealm() {
        return realm;
    }
    
    @Override
    public void setRealm(CultivationRealm realm) {
        this.realm = realm;
    }
    
    @Override
    public int getLevel() {
        return level;
    }
    
    @Override
    @Deprecated
    public void setLevel(int level) {
        this.level = level;
        // 同步更新小境界（用于兼容性）
        if (level > 0) {
            this.subRealm = SubRealm.fromLegacyLevel(level);
        }
    }
    
    @Override
    public SubRealm getSubRealm() {
        return subRealm;
    }
    
    @Override
    public void setSubRealm(SubRealm subRealm) {
        this.subRealm = subRealm != null ? subRealm : SubRealm.EARLY;
    }
    
    @Override
    public int getFoundation() {
        return foundation;
    }
    
    @Override
    public void setFoundation(int foundation) {
        this.foundation = Math.max(0, Math.min(100, foundation));
    }
    
    @Override
    public void addFoundation(int amount) {
        setFoundation(this.foundation + amount);
    }
    
    @Override
    public void reduceFoundation(int amount) {
        setFoundation(this.foundation - amount);
    }
    
    @Override
    public double getCultivationProgress() {
        return cultivationProgress;
    }
    
    @Override
    public void setCultivationProgress(double progress) {
        this.cultivationProgress = progress;
    }
    
    @Override
    public void addCultivationProgress(double amount) {
        this.cultivationProgress += amount;
        
        // 检查是否可以升级
        double requiredProgress = realm.getRequiredProgress(level);
        if (cultivationProgress >= requiredProgress) {
            tryLevelUp();
        }
    }
    
    @Override
    public double getSpiritPower() {
        return spiritPower;
    }
    
    @Override
    public void setSpiritPower(double power) {
        this.spiritPower = Math.min(power, maxSpiritPower);
    }
    
    @Override
    public double getMaxSpiritPower() {
        return maxSpiritPower;
    }
    
    @Override
    public void setMaxSpiritPower(double maxPower) {
        this.maxSpiritPower = maxPower;
        if (spiritPower > maxSpiritPower) {
            spiritPower = maxSpiritPower;
        }
    }
    
    @Override
    public void addSpiritPower(double amount) {
        setSpiritPower(spiritPower + amount);
    }
    
    @Override
    public boolean consumeSpiritPower(double amount) {
        if (spiritPower >= amount) {
            spiritPower -= amount;
            return true;
        }
        return false;
    }
    
    @Override
    public boolean tryBreakthrough() {
        // 新的突破逻辑：基于小境界系统
        // 首先检查是否可以突破小境界
        SubRealm nextSubRealm = subRealm.getNext();
        if (nextSubRealm != null) {
            // 可以突破到下一个小境界
            // 检查根基值
            if (foundation >= 50) {
                subRealm = nextSubRealm;
                cultivationExperience = 0; // 重置经验
                return true;
            }
            return false; // 根基不足
        } else {
            // 已经是后期，可以尝试突破大境界
            CultivationRealm nextRealm = realm.getNext();
            if (nextRealm != null) {
                // 检查根基值
                if (foundation >= 30) {
                    realm = nextRealm;
                    subRealm = SubRealm.EARLY; // 进入下一境界的初期
                    cultivationExperience = 0; // 重置经验
                    maxSpiritPower = realm.getBaseSpiritPower();
                    spiritPower = maxSpiritPower;
                    return true;
                }
                return false; // 根基不足
            }
        }
        return false; // 无法突破
    }
    
    @Override
    @Deprecated
    public boolean tryLevelUp() {
        // 旧方法，保留用于兼容性
        // 新系统使用小境界突破
        if (level < realm.getMaxLevel()) {
            level++;
            cultivationProgress = 0.0;
            maxSpiritPower = realm.getBaseSpiritPower() + (level - 1) * 50;
            // 同步更新小境界
            subRealm = SubRealm.fromLegacyLevel(level);
            return true;
        }
        return false;
    }
    
    @Override
    public double getCultivationBonus() {
        double bonus = 1.0;
        
        // 灵根加成
        if (spiritualRoot.getType() != SpiritualRootType.NONE) {
            bonus *= spiritualRoot.getQuality().getCultivationBonus();
        }
        
        // 境界加成
        bonus *= realm.getCultivationBonus();
        
        // 根基影响（根基值越低，修炼速度越慢）
        bonus *= org.example.Kangnaixi.tiandao.cultivation.FoundationSystem.getCultivationSpeedMultiplier(foundation);
        
        return bonus;
    }
    
    @Override
    public double getSpiritPowerRecoveryRate() {
        double rate = 1.0;
        
        // 灵根加成
        if (spiritualRoot.getType() != SpiritualRootType.NONE) {
            // 灵根类型的恢复加成
            rate *= spiritualRoot.getType().getRecoveryBonus();
            // 灵根品质的恢复加成
            rate *= spiritualRoot.getQuality().getSpiritRecoveryBonus();
        }
        
        // 境界加成
        rate *= realm.getSpiritRecoveryBonus();
        
        // 根基影响（根基值越低，灵力恢复速度越慢）
        rate *= org.example.Kangnaixi.tiandao.cultivation.FoundationSystem.getSpiritRecoveryMultiplier(foundation);
        
        return rate;
    }
    
    @Override
    public double getEnvironmentalDensity() {
        return environmentalDensity;
    }
    
    @Override
    public void setEnvironmentalDensity(double density) {
        this.environmentalDensity = density;
    }
    
    @Override
    public double getIntensityBonus() {
        return intensityBonus;
    }
    
    @Override
    public void setIntensityBonus(double bonus) {
        this.intensityBonus = bonus;
    }
    
    /**
     * 计算基于灵力强度的恢复加成
     * @param player 玩家对象
     * @return 恢复加成倍数
     */
    public double getIntensityBasedRecoveryBonus(Player player) {
        // 基础强度为1.0
        double intensity = 1.0;
        
        // 根据当前灵力与最大灵力的比例计算强度
        double currentPower = spiritPower;
        double maxPower = maxSpiritPower;
        
        if (maxPower > 0) {
            double powerRatio = currentPower / maxPower;
            
            // 灵力越低，恢复速度越快（模拟身体对灵力亏空的自我调节）
            // 当灵力低于30%时，恢复速度提高50%
            if (powerRatio < 0.3) {
                intensity *= 1.5;
            }
            // 当灵力低于60%时，恢复速度提高20%
            else if (powerRatio < 0.6) {
                intensity *= 1.2;
            }
            // 当灵力高于90%时，恢复速度略微降低（模拟饱和效应）
            else if (powerRatio > 0.9) {
                intensity *= 0.9;
            }
        }
        
        // 功法等级加成（如果装备了功法）
        if (TechniqueChecker.hasBasicTechniqueEquipped(player)) {
        int techniqueLevel = TechniqueChecker.getTechniqueLevel(player);
        if (techniqueLevel > 0) {
            intensity *= (1.0 + techniqueLevel * 0.1); // 每级功法提供10%加成
            }
        }
        
        return intensity;
    }
    
    @Override
    public void copyFrom(ICultivation other) {
        // 复制灵根对象
        if (other instanceof CultivationCapability) {
            CultivationCapability otherCap = (CultivationCapability) other;
            SpiritualRoot otherRoot = otherCap.getSpiritualRootObject();
            if (otherRoot != null) {
                // 创建新的SpiritualRoot对象以避免引用问题
                this.spiritualRoot = new SpiritualRoot(otherRoot.getType(), otherRoot.getQuality());
            } else {
                // 如果没有灵根对象，创建默认的
                this.spiritualRoot = new SpiritualRoot(other.getSpiritualRoot());
            }
            
            // 复制已分配标记
            this.rootAssigned = otherCap.hasRootAssigned();
        } else {
            // 兼容性处理
            this.spiritualRoot = new SpiritualRoot(other.getSpiritualRoot());
            this.rootAssigned = other.hasRootAssigned();
        }
        
        // 复制其他数据
        this.realm = other.getRealm();
        this.level = other.getLevel(); // 保留用于兼容性
        this.subRealm = other.getSubRealm();
        this.foundation = other.getFoundation();
        this.cultivationProgress = other.getCultivationProgress();
        this.spiritPower = other.getSpiritPower();
        this.maxSpiritPower = other.getMaxSpiritPower();
        this.timeAcceleration = other.getTimeAcceleration();
        this.knownBlueprints.clear();
        for (SpellBlueprint blueprint : other.getKnownBlueprints()) {
            this.knownBlueprints.add(SpellBlueprint.fromNBT(blueprint.toNBT()));
        }
    }
    
    @Override
    public boolean hasRootAssigned() {
        return rootAssigned;
    }
    
    @Override
    public void setRootAssigned(boolean assigned) {
        this.rootAssigned = assigned;
    }
    
    // === 修炼系统方法实现 ===
    
    @Override
    public boolean isPracticing() {
        return practicing;
    }
    
    @Override
    public void setPracticing(boolean practicing) {
        this.practicing = practicing;
    }
    
    @Override
    public String getCurrentPracticeMethod() {
        return currentPracticeMethod;
    }
    
    @Override
    public void setCurrentPracticeMethod(String methodId) {
        this.currentPracticeMethod = methodId != null ? methodId : "";
    }
    
    @Override
    public int getCultivationExperience() {
        return cultivationExperience;
    }
    
    @Override
    public void setCultivationExperience(int experience) {
        this.cultivationExperience = Math.max(0, experience);
    }
    
    @Override
    public void addCultivationExperience(int amount) {
        this.cultivationExperience += amount;
        if (this.cultivationExperience < 0) {
            this.cultivationExperience = 0;
        }
    }
    
    @Override
    @Deprecated
    public int getRequiredExperienceForLevel() {
        // 旧公式：100 + (level - 1) * 50
        // 保留用于兼容性
        return 100 + (level - 1) * 50;
    }
    
    @Override
    public int getRequiredExperienceForSubRealm() {
        // 新公式：基础值 × 倍数
        // 基础值 = 100 × (境界序号 + 1)
        // 初期 = 基础值 × 1.0
        // 中期 = 基础值 × 2.0
        // 后期 = 基础值 × 4.0
        
        if (realm.isSpecialRealm()) {
            return 0; // 特殊境界无经验需求
        }
        
        int baseExp = realm.getBaseExperience();
        if (baseExp == 0) {
            return 0;
        }
        
        switch (subRealm) {
            case EARLY:
                return baseExp; // × 1.0
            case MIDDLE:
                return baseExp * 2; // × 2.0
            case LATE:
                return baseExp * 4; // × 4.0
            default:
                return baseExp;
        }
    }
    
    @Override
    public double getTimeAcceleration() {
        return timeAcceleration;
    }
    
    @Override
    public void setTimeAcceleration(double multiplier) {
        this.timeAcceleration = Math.max(1.0, Math.min(3.0, multiplier));
    }
    
    @Override
    public long getPracticeStartTime() {
        return practiceStartTime;
    }
    
    @Override
    public void setPracticeStartTime(long time) {
        this.practiceStartTime = time;
    }
    
    @Override
    public long getLastCombatTime() {
        return lastCombatTime;
    }
    
    @Override
    public void setLastCombatTime(long time) {
        this.lastCombatTime = time;
    }
    
    @Override
    public boolean isInCombat() {
        // 战斗状态判定：5秒内受伤（100 ticks = 5秒）
        long currentTime = System.currentTimeMillis() / 50; // 转换为游戏tick
        return (currentTime - lastCombatTime) < 100;
    }
    
    // === 功法管理方法实现 ===
    
    @Override
    public java.util.List<org.example.Kangnaixi.tiandao.technique.TechniqueData> getLearnedTechniques() {
        return new java.util.ArrayList<>(learnedTechniques);
    }
    
    @Override
    public boolean learnTechnique(org.example.Kangnaixi.tiandao.technique.TechniqueData technique) {
        if (technique == null) {
            return false;
        }
        
        // 检查是否已经学习
        if (hasTechnique(technique.getId())) {
            return false;
        }
        
        // 创建副本并添加到列表
        learnedTechniques.add(new org.example.Kangnaixi.tiandao.technique.TechniqueData(technique));
        return true;
    }
    
    @Override
    public boolean forgetTechnique(String techniqueId) {
        if (techniqueId == null) {
            return false;
        }
        
        // 如果正装备该功法，先卸下
        if (equippedTechnique != null && equippedTechnique.getId().equals(techniqueId)) {
            unequipTechnique();
        }
        
        // 从列表中移除
        return learnedTechniques.removeIf(t -> t.getId().equals(techniqueId));
    }
    
    @Override
    public boolean hasTechnique(String techniqueId) {
        if (techniqueId == null) {
            return false;
        }
        return learnedTechniques.stream().anyMatch(t -> t.getId().equals(techniqueId));
    }
    
    @Override
    public org.example.Kangnaixi.tiandao.technique.TechniqueData getTechniqueById(String techniqueId) {
        if (techniqueId == null) {
            return null;
        }
        return learnedTechniques.stream()
            .filter(t -> t.getId().equals(techniqueId))
            .findFirst()
            .orElse(null);
    }
    
    @Override
    public org.example.Kangnaixi.tiandao.technique.TechniqueData getEquippedTechnique() {
        return equippedTechnique;
    }
    
    @Override
    public boolean equipTechnique(String techniqueId) {
        if (techniqueId == null) {
            return false;
        }
        
        // 获取要装备的功法
        org.example.Kangnaixi.tiandao.technique.TechniqueData technique = getTechniqueById(techniqueId);
        if (technique == null) {
            return false; // 没有学习该功法
        }
        
        // 装备功法
        this.equippedTechnique = technique;
        this.techniqueEquipTime = System.currentTimeMillis() / 50; // 记录装备时间（游戏tick）
        
        return true;
    }
    
    @Override
    public void unequipTechnique() {
        this.equippedTechnique = null;
        this.techniqueEquipTime = 0;
    }
    
    @Override
    public boolean hasEquippedTechnique() {
        return equippedTechnique != null;
    }
    
    @Override
    public long getTechniqueEquipTime() {
        return techniqueEquipTime;
    }
    
    @Override
    public void setTechniqueEquipTime(long time) {
        this.techniqueEquipTime = time;
    }
    
    @Override
    public long getTechniqueEquipDuration() {
        if (equippedTechnique == null) {
            return 0;
        }
        long currentTime = System.currentTimeMillis() / 50; // 当前游戏tick
        return currentTime - techniqueEquipTime;
    }
    
    // ==================== 术法系统实现 ====================
    
    @Override
    public java.util.List<String> getUnlockedSpells() {
        return new java.util.ArrayList<>(unlockedSpells);
    }
    
    @Override
    public void unlockSpell(String spellId) {
        if (!unlockedSpells.contains(spellId)) {
            unlockedSpells.add(spellId);
        }
    }
    
    @Override
    public boolean hasSpell(String spellId) {
        return unlockedSpells.contains(spellId);
    }
    
    @Override
    public String[] getSpellHotbar() {
        return spellHotbar.clone(); // 返回副本
    }
    
    @Override
    public void setSpellHotbar(int slot, String spellId) {
        if (slot >= 0 && slot < 4) {
            spellHotbar[slot] = spellId;
        }
    }
    
    @Override
    public java.util.Map<String, Long> getSpellCooldowns() {
        return new java.util.HashMap<>(spellCooldowns);
    }
    
    @Override
    public void setSpellCooldown(String spellId, long endTime) {
        if (endTime <= System.currentTimeMillis()) {
            spellCooldowns.remove(spellId);
        } else {
            spellCooldowns.put(spellId, endTime);
        }
    }
    
    @Override
    public int getSpellCooldownRemaining(String spellId) {
        Long endTime = spellCooldowns.get(spellId);
        if (endTime == null || endTime <= System.currentTimeMillis()) {
            return 0;
        }
        return (int) ((endTime - System.currentTimeMillis()) / 1000);
    }
    
    @Override
    public java.util.Map<String, Long> getActiveSpells() {
        return new java.util.HashMap<>(activeSpells);
    }
    
    @Override
    public void activateSpell(String spellId, long endTime) {
        activeSpells.put(spellId, endTime);
    }
    
    @Override
    public void deactivateSpell(String spellId) {
        activeSpells.remove(spellId);
    }
    
    @Override
    public boolean isSpellActive(String spellId) {
        Long endTime = activeSpells.get(spellId);
        if (endTime == null) {
            return false;
        }
        // 检查是否过期
        if (endTime <= System.currentTimeMillis()) {
            activeSpells.remove(spellId);
            return false;
        }
        return true;
    }

    // ==================== 术法蓝图 ====================

    @Override
    public void clearBlueprints() {
        this.knownBlueprints.clear();
    }

    @Override
    public java.util.List<SpellBlueprint> getKnownBlueprints() {
        return new java.util.ArrayList<>(knownBlueprints);
    }

    @Override
    public void learnBlueprint(SpellBlueprint blueprint) {
        if (blueprint == null) {
            return;
        }
        if (!knowsBlueprint(blueprint.getId())) {
            this.knownBlueprints.add(SpellBlueprint.fromNBT(blueprint.toNBT()));
        }
    }

    @Override
    public boolean knowsBlueprint(String blueprintId) {
        if (blueprintId == null) {
            return false;
        }
        for (SpellBlueprint blueprint : knownBlueprints) {
            if (blueprint.getId().equals(blueprintId)) {
                return true;
            }
        }
        return false;
    }
}
