package org.example.Kangnaixi.tiandao.cultivation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * 境界数据类，存储玩家的境界信息
 */
public class CultivationRealmData {
    private CultivationRealm currentRealm;
    private int realmLevel; // 境界内等级
    private float cultivationProgress; // 修炼进度
    private float breakthroughRequirement; // 突破所需进度
    private long lastCultivationTime; // 上次修炼时间
    
    // 默认构造函数
    public CultivationRealmData() {
        this.currentRealm = CultivationRealm.MORTAL;
        this.realmLevel = 0;
        this.cultivationProgress = 0.0f;
        this.breakthroughRequirement = currentRealm.getBreakthroughRequirement();
        this.lastCultivationTime = System.currentTimeMillis();
    }
    
    // 从NBT数据加载
    public CultivationRealmData(CompoundTag nbt) {
        this.currentRealm = CultivationRealm.fromId(nbt.getString("CurrentRealm"));
        this.realmLevel = nbt.getInt("RealmLevel");
        this.cultivationProgress = nbt.getFloat("CultivationProgress");
        this.breakthroughRequirement = nbt.getFloat("BreakthroughRequirement");
        this.lastCultivationTime = nbt.getLong("LastCultivationTime");
    }
    
    // 保存到NBT数据
    public CompoundTag save() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("CurrentRealm", this.currentRealm.getId());
        nbt.putInt("RealmLevel", this.realmLevel);
        nbt.putFloat("CultivationProgress", this.cultivationProgress);
        nbt.putFloat("BreakthroughRequirement", this.breakthroughRequirement);
        nbt.putLong("LastCultivationTime", this.lastCultivationTime);
        return nbt;
    }
    
    /**
     * 增加修炼进度
     * @param amount 增加量
     * @return 是否达到突破要求
     */
    public boolean addCultivationProgress(float amount) {
        this.cultivationProgress += amount;
        this.lastCultivationTime = System.currentTimeMillis();
        
        // 检查是否达到突破要求
        return this.cultivationProgress >= this.breakthroughRequirement;
    }
    
    /**
     * 尝试突破到下一境界或等级
     * @return 是否突破成功
     */
    public boolean tryBreakthrough() {
        if (this.cultivationProgress < this.breakthroughRequirement) {
            return false; // 进度不足，无法突破
        }
        
        // 检查是否可以提升境界内等级
        if (this.realmLevel < this.currentRealm.getMaxLevel()) {
            this.realmLevel++;
            this.cultivationProgress = 0.0f;
            this.breakthroughRequirement = calculateNextLevelRequirement();
            return true;
        }
        
        // 检查是否可以突破到下一境界
        if (this.currentRealm != CultivationRealm.IMMORTAL) {
            this.currentRealm = this.currentRealm.getNextRealm();
            this.realmLevel = 0;
            this.cultivationProgress = 0.0f;
            this.breakthroughRequirement = this.currentRealm.getBreakthroughRequirement();
            return true;
        }
        
        return false; // 已经是最高境界，无法突破
    }
    
    /**
     * 计算下一等级所需的进度
     */
    private float calculateNextLevelRequirement() {
        // 境界内等级提升所需进度随等级提高而增加
        return this.currentRealm.getBreakthroughRequirement() * (1.0f + this.realmLevel * 0.2f);
    }
    
    /**
     * 获取当前境界的最大灵力值
     * @param player 玩家对象，用于获取灵根加成
     * @return 最大灵力值
     */
    public float getMaxEnergy(Player player) {
        // 基础灵力值
        float baseEnergy = this.currentRealm.getBaseMaxEnergy();
        
        // 境界倍数加成
        float realmMultiplier = this.currentRealm.getEnergyMultiplier();
        
        // 等级加成（每级增加10%）
        float levelBonus = 1.0f + (this.realmLevel * 0.1f);
        
        // 灵根加成
        float rootBonus = SpiritualRootManager.getCultivationSpeedBonus(player);
        
        return baseEnergy * realmMultiplier * levelBonus * rootBonus;
    }
    
    /**
     * 获取修炼速度加成
     * @return 修炼速度加成倍数
     */
    public float getCultivationSpeedBonus() {
        // 境界越高，修炼速度越快
        return 1.0f + (this.currentRealm.ordinal() * 0.1f);
    }
    
    // Getter方法
    public CultivationRealm getCurrentRealm() {
        return currentRealm;
    }
    
    public int getRealmLevel() {
        return realmLevel;
    }
    
    public float getCultivationProgress() {
        return cultivationProgress;
    }
    
    public float getBreakthroughRequirement() {
        return breakthroughRequirement;
    }
    
    public long getLastCultivationTime() {
        return lastCultivationTime;
    }
    
    // Setter方法
    public void setCurrentRealm(CultivationRealm currentRealm) {
        this.currentRealm = currentRealm;
    }
    
    public void setRealmLevel(int realmLevel) {
        this.realmLevel = Math.max(0, Math.min(realmLevel, this.currentRealm.getMaxLevel()));
    }
    
    public void setCultivationProgress(float cultivationProgress) {
        this.cultivationProgress = cultivationProgress;
    }
    
    public void setBreakthroughRequirement(float breakthroughRequirement) {
        this.breakthroughRequirement = breakthroughRequirement;
    }
    
    public void setLastCultivationTime(long lastCultivationTime) {
        this.lastCultivationTime = lastCultivationTime;
    }
}