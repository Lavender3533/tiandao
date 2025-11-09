package org.example.Kangnaixi.tiandao.technique;

import net.minecraft.nbt.CompoundTag;
import org.example.Kangnaixi.tiandao.cultivation.CultivationRealm;
import org.example.Kangnaixi.tiandao.cultivation.SpiritualRootType;

/**
 * 功法数据模型
 * 存储功法的所有属性和状态
 */
public class TechniqueData {
    
    private final String id;                    // 功法ID（唯一标识）
    private final String name;                  // 功法名称
    private final String description;           // 功法描述
    private final SpiritualRootType requiredRoot; // 需要的灵根类型
    private final CultivationRealm requiredRealm; // 需要的境界
    private final int requiredLevel;            // 需要的境界层级
    
    // 可变状态
    private int level;                          // 当前等级（1-10）
    private int experience;                     // 当前经验值
    private int maxExperience;                  // 当前等级所需经验
    
    /**
     * 完整构造函数
     */
    public TechniqueData(String id, String name, String description,
                         SpiritualRootType requiredRoot,
                         CultivationRealm requiredRealm, int requiredLevel,
                         int level, int experience) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.requiredRoot = requiredRoot;
        this.requiredRealm = requiredRealm;
        this.requiredLevel = requiredLevel;
        this.level = level;
        this.experience = experience;
        this.maxExperience = calculateMaxExperience(level);
    }
    
    /**
     * 创建新学习的功法（1级，0经验）
     */
    public TechniqueData(String id, String name, String description,
                         SpiritualRootType requiredRoot,
                         CultivationRealm requiredRealm, int requiredLevel) {
        this(id, name, description, requiredRoot, requiredRealm, requiredLevel, 1, 0);
    }
    
    /**
     * 复制构造函数
     */
    public TechniqueData(TechniqueData other) {
        this(other.id, other.name, other.description, other.requiredRoot,
             other.requiredRealm, other.requiredLevel, other.level, other.experience);
    }
    
    // === Getters ===
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public SpiritualRootType getRequiredRoot() {
        return requiredRoot;
    }
    
    public CultivationRealm getRequiredRealm() {
        return requiredRealm;
    }
    
    public int getRequiredLevel() {
        return requiredLevel;
    }
    
    public int getLevel() {
        return level;
    }
    
    public int getExperience() {
        return experience;
    }
    
    public int getMaxExperience() {
        return maxExperience;
    }
    
    /**
     * 获取经验百分比（0-100）
     */
    public double getExperiencePercentage() {
        if (maxExperience <= 0) return 0;
        return (double) experience / maxExperience * 100.0;
    }
    
    /**
     * 是否达到最大等级
     */
    public boolean isMaxLevel() {
        return level >= 10;
    }
    
    // === 功法效率系统 ===
    
    /**
     * 获取功法的效率加成
     * 1级 = 1.0 (100%)
     * 10级 = 2.0 (200%)
     * 线性增长：每级 +11.1%
     * 
     * @return 效率倍数（1.0 - 2.0）
     */
    public double getEfficiencyBonus() {
        return getEfficiencyBonus(this.level);
    }
    
    /**
     * 获取指定等级的效率加成
     */
    public static double getEfficiencyBonus(int level) {
        if (level <= 0) return 1.0;
        if (level >= 10) return 2.0;
        // 线性插值：1.0 + (level - 1) * 0.111...
        return 1.0 + (level - 1) * (1.0 / 9.0);
    }
    
    // === 经验和升级系统 ===
    
    /**
     * 计算指定等级所需的最大经验
     * 1级：100经验
     * 2级：150经验
     * ...
     * 10级：550经验
     * 公式：100 + (level - 1) * 50
     */
    private int calculateMaxExperience(int level) {
        if (level >= 10) return Integer.MAX_VALUE; // 最大等级不再需要经验
        return 100 + (level - 1) * 50;
    }
    
    /**
     * 增加经验值
     * @param amount 经验数量
     * @return 是否升级
     */
    public boolean addExperience(int amount) {
        if (isMaxLevel()) {
            return false; // 已达最大等级
        }
        
        this.experience += amount;
        
        // 检查是否可以升级
        if (this.experience >= this.maxExperience) {
            return levelUp();
        }
        
        return false;
    }
    
    /**
     * 检查是否可以升级
     */
    public boolean canLevelUp() {
        return !isMaxLevel() && experience >= maxExperience;
    }
    
    /**
     * 升级
     * @return 是否成功升级
     */
    public boolean levelUp() {
        if (!canLevelUp()) {
            return false;
        }
        
        // 升级
        this.level++;
        
        // 扣除经验并保留溢出部分
        int overflow = this.experience - this.maxExperience;
        this.maxExperience = calculateMaxExperience(this.level);
        this.experience = Math.max(0, overflow);
        
        return true;
    }
    
    /**
     * 设置等级（用于命令和调试）
     */
    public void setLevel(int level) {
        this.level = Math.max(1, Math.min(10, level));
        this.maxExperience = calculateMaxExperience(this.level);
        this.experience = 0;
    }
    
    /**
     * 设置经验（用于命令和调试）
     */
    public void setExperience(int experience) {
        this.experience = Math.max(0, experience);
    }
    
    // === NBT序列化 ===
    
    /**
     * 保存到NBT
     */
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        
        // 基本信息
        tag.putString("id", id);
        tag.putString("name", name);
        tag.putString("description", description);
        tag.putString("requiredRoot", requiredRoot.getId());
        tag.putString("requiredRealm", requiredRealm.getId());
        tag.putInt("requiredLevel", requiredLevel);
        
        // 当前状态
        tag.putInt("level", level);
        tag.putInt("experience", experience);
        tag.putInt("maxExperience", maxExperience);
        
        return tag;
    }
    
    /**
     * 从NBT加载
     */
    public static TechniqueData fromNBT(CompoundTag tag) {
        String id = tag.getString("id");
        String name = tag.getString("name");
        String description = tag.getString("description");
        SpiritualRootType requiredRoot = SpiritualRootType.fromId(tag.getString("requiredRoot"));
        CultivationRealm requiredRealm = CultivationRealm.fromId(tag.getString("requiredRealm"));
        int requiredLevel = tag.getInt("requiredLevel");
        int level = tag.getInt("level");
        int experience = tag.getInt("experience");
        
        return new TechniqueData(id, name, description, requiredRoot, 
                                 requiredRealm, requiredLevel, level, experience);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof TechniqueData)) return false;
        TechniqueData other = (TechniqueData) obj;
        return this.id.equals(other.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
    
    @Override
    public String toString() {
        return String.format("Technique{id=%s, name=%s, level=%d, exp=%d/%d}",
                             id, name, level, experience, maxExperience);
    }
}

