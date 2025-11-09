package org.example.Kangnaixi.tiandao.cultivation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * 灵力数据类，存储玩家的灵力相关信息
 */
public class SpiritualEnergy {
    private float currentEnergy;
    private float maxEnergy;
    private float recoveryRate;
    private long lastUpdateTime;
    
    // 默认构造函数
    public SpiritualEnergy() {
        this.currentEnergy = 100.0f;
        this.maxEnergy = 100.0f;
        this.recoveryRate = 1.0f; // 每游戏分钟恢复1点灵力
        this.lastUpdateTime = System.currentTimeMillis();
    }
    
    // 从NBT数据加载
    public SpiritualEnergy(CompoundTag nbt) {
        this.currentEnergy = nbt.getFloat("CurrentEnergy");
        this.maxEnergy = nbt.getFloat("MaxEnergy");
        this.recoveryRate = nbt.getFloat("RecoveryRate");
        this.lastUpdateTime = nbt.getLong("LastUpdateTime");
    }
    
    // 保存到NBT数据
    public CompoundTag save() {
        CompoundTag nbt = new CompoundTag();
        nbt.putFloat("CurrentEnergy", this.currentEnergy);
        nbt.putFloat("MaxEnergy", this.maxEnergy);
        nbt.putFloat("RecoveryRate", this.recoveryRate);
        nbt.putLong("LastUpdateTime", this.lastUpdateTime);
        return nbt;
    }
    
    /**
     * 消耗灵力
     * @param amount 消耗量
     * @return 是否成功消耗（灵力是否足够）
     */
    public boolean consumeEnergy(float amount) {
        if (this.currentEnergy >= amount) {
            this.currentEnergy -= amount;
            return true;
        }
        return false;
    }
    
    /**
     * 恢复灵力
     * @param amount 恢复量
     */
    public void restoreEnergy(float amount) {
        this.currentEnergy = Math.min(this.currentEnergy + amount, this.maxEnergy);
    }
    
    /**
     * 增加灵力
     * @param amount 增加量
     */
    public void addEnergy(float amount) {
        this.currentEnergy = Math.min(this.currentEnergy + amount, this.maxEnergy);
    }
    
    /**
     * 更新灵力恢复
     * @param currentTime 当前时间
     */
    public void updateRecovery(long currentTime) {
        // 计算时间差（毫秒）
        long timeDiff = currentTime - this.lastUpdateTime;
        
        // 转换为游戏分钟（假设1分钟=1000毫秒，实际应根据游戏时间比例调整）
        float gameMinutesPassed = timeDiff / 1000.0f;
        
        // 计算恢复的灵力
        float energyRecovered = gameMinutesPassed * this.recoveryRate;
        
        // 更新当前灵力
        this.currentEnergy = Math.min(this.currentEnergy + energyRecovered, this.maxEnergy);
        
        // 更新最后更新时间
        this.lastUpdateTime = currentTime;
    }
    
    /**
     * 增加最大灵力值
     * @param amount 增加量
     */
    public void increaseMaxEnergy(float amount) {
        this.maxEnergy += amount;
        // 确保当前灵力不超过新的最大值
        this.currentEnergy = Math.min(this.currentEnergy, this.maxEnergy);
    }
    
    /**
     * 设置灵力恢复速度
     * @param rate 恢复速度（每游戏分钟恢复量）
     */
    public void setRecoveryRate(float rate) {
        this.recoveryRate = rate;
    }
    
    // Getter方法
    public float getCurrentEnergy() {
        return currentEnergy;
    }
    
    public float getMaxEnergy() {
        return maxEnergy;
    }
    
    public float getRecoveryRate() {
        return recoveryRate;
    }
    
    public long getLastUpdateTime() {
        return lastUpdateTime;
    }
    
    // Setter方法
    public void setCurrentEnergy(float currentEnergy) {
        this.currentEnergy = Math.min(currentEnergy, this.maxEnergy);
    }
    
    public void setMaxEnergy(float maxEnergy) {
        this.maxEnergy = maxEnergy;
    }
    
    public void setLastUpdateTime(long lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }
}