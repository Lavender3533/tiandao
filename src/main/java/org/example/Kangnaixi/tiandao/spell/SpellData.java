package org.example.Kangnaixi.tiandao.spell;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import org.example.Kangnaixi.tiandao.capability.ICultivation;
import org.example.Kangnaixi.tiandao.cultivation.CultivationRealm;
import org.example.Kangnaixi.tiandao.cultivation.SubRealm;

/**
 * 术法数据类
 * 定义术法的基本属性和行为
 */
public class SpellData {
    
    private final String id;                    // 术法唯一标识符
    private final String name;                  // 术法名称
    private final String description;           // 术法描述
    private final CultivationRealm requiredRealm;  // 所需境界
    private final int requiredLevel;            // 所需境界层级
    private final double spiritCost;            // 灵力消耗（释放时）
    private final double maintenanceCost;       // 维持消耗（每秒）
    private final int cooldown;                 // 冷却时间（秒）
    private final int duration;                 // 持续时间（秒），0表示瞬发
    private final SpellType type;               // 术法类型
    
    private long cooldownEndTime;               // 冷却结束时间戳
    
    public enum SpellType {
        INSTANT,        // 瞬发术法
        DURATION,       // 持续性术法
        CHANNELING      // 引导术法（需要持续施法）
    }
    
    /**
     * 构造函数
     */
    public SpellData(String id, String name, String description,
                     CultivationRealm requiredRealm, int requiredLevel,
                     double spiritCost, double maintenanceCost,
                     int cooldown, int duration, SpellType type) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.requiredRealm = requiredRealm;
        this.requiredLevel = requiredLevel;
        this.spiritCost = spiritCost;
        this.maintenanceCost = maintenanceCost;
        this.cooldown = cooldown;
        this.duration = duration;
        this.type = type;
        this.cooldownEndTime = 0;
    }
    
    /**
     * 检查玩家是否满足释放条件
     */
    public boolean canCast(ICultivation cultivation) {
        // 检查境界要求
        CultivationRealm playerRealm = cultivation.getRealm();
        if (playerRealm.ordinal() < requiredRealm.ordinal()) {
            return false;
        }
        
        // 如果境界相同，检查小境界（将小境界映射到等级范围）
        if (playerRealm == requiredRealm) {
            int playerLevel = getSubRealmToLevel(cultivation.getSubRealm());
            if (playerLevel < requiredLevel) {
                return false;
            }
        }
        
        // 检查灵力是否足够
        if (cultivation.getSpiritPower() < spiritCost) {
            return false;
        }
        
        // 检查冷却时间
        if (isOnCooldown()) {
            return false;
        }
        
        return true;
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
     * 将等级要求转换为小境界显示
     */
    private String getSubRealmRequirementDisplay(int level) {
        if (level <= 3) {
            return "初期";
        } else if (level <= 6) {
            return "中期";
        } else {
            return "后期";
        }
    }
    
    /**
     * 检查是否在冷却中
     */
    public boolean isOnCooldown() {
        return System.currentTimeMillis() < cooldownEndTime;
    }
    
    /**
     * 获取剩余冷却时间（秒）
     */
    public int getCooldownRemaining() {
        if (!isOnCooldown()) {
            return 0;
        }
        return (int) ((cooldownEndTime - System.currentTimeMillis()) / 1000);
    }
    
    /**
     * 开始冷却
     */
    public void startCooldown() {
        this.cooldownEndTime = System.currentTimeMillis() + (cooldown * 1000L);
    }
    
    /**
     * 设置冷却结束时间
     */
    public void setCooldownEndTime(long endTime) {
        this.cooldownEndTime = endTime;
    }
    
    /**
     * 清除冷却
     */
    public void clearCooldown() {
        this.cooldownEndTime = 0;
    }
    
    /**
     * 释放术法（由具体实现类重写）
     * @return 是否成功释放
     */
    public boolean cast(ServerPlayer player, ICultivation cultivation) {
        // 基类只处理通用逻辑
        if (!canCast(cultivation)) {
            return false;
        }
        
        // 扣除灵力
        cultivation.consumeSpiritPower(spiritCost);
        
        // 开始冷却
        startCooldown();
        
        return true;
    }
    
    /**
     * Tick更新（用于持续性术法）
     */
    public void onTick(ServerPlayer player, ICultivation cultivation) {
        // 由子类实现
    }
    
    /**
     * 术法结束时调用
     */
    public void onEnd(ServerPlayer player, ICultivation cultivation) {
        // 由子类实现
    }
    
    /**
     * 序列化到NBT
     */
    public CompoundTag toNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("id", id);
        nbt.putLong("cooldownEndTime", cooldownEndTime);
        return nbt;
    }
    
    /**
     * 从NBT反序列化
     */
    public static SpellData fromNBT(CompoundTag nbt, SpellRegistry registry) {
        String id = nbt.getString("id");
        SpellData spell = registry.getSpellById(id);
        if (spell != null) {
            spell.setCooldownEndTime(nbt.getLong("cooldownEndTime"));
        }
        return spell;
    }
    
    // Getters
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public CultivationRealm getRequiredRealm() {
        return requiredRealm;
    }
    
    public int getRequiredLevel() {
        return requiredLevel;
    }
    
    public double getSpiritCost() {
        return spiritCost;
    }
    
    public double getMaintenanceCost() {
        return maintenanceCost;
    }
    
    public int getCooldown() {
        return cooldown;
    }
    
    public int getDuration() {
        return duration;
    }
    
    public SpellType getType() {
        return type;
    }
    
    public long getCooldownEndTime() {
        return cooldownEndTime;
    }
    
    /**
     * 获取术法完整说明
     */
    public String getFullDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("§e").append(name).append("\n\n");
        sb.append("§7").append(description).append("\n\n");
        // 显示解锁条件（将等级映射回小境界显示）
        String realmRequirement = getSubRealmRequirementDisplay(requiredLevel);
        sb.append("§b解锁条件: §f").append(requiredRealm.getDisplayName())
          .append(" ").append(realmRequirement).append("\n");
        sb.append("§b灵力消耗: §f").append((int) spiritCost);
        if (maintenanceCost > 0) {
            sb.append(" + ").append((int) maintenanceCost).append("/秒");
        }
        sb.append("\n");
        sb.append("§b冷却时间: §f").append(cooldown).append("秒\n");
        if (duration > 0) {
            sb.append("§b持续时间: §f").append(duration).append("秒\n");
        }
        return sb.toString();
    }
}

