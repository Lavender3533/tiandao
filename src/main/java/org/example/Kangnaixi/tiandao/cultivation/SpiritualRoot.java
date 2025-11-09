package org.example.Kangnaixi.tiandao.cultivation;

import net.minecraft.nbt.CompoundTag;
import java.util.Random;

/**
 * 灵根类
 * 存储玩家的灵根信息
 */
public class SpiritualRoot {
    private SpiritualRootType type;
    private SpiritualRootQuality quality;
    private float cultivationSpeedBonus;
    private String specialAbility;
    
    // 默认构造函数
    public SpiritualRoot() {
        this(SpiritualRootType.getRandomRoot(), SpiritualRootQuality.randomQuality());
    }
    
    // 指定灵根类型和品质的构造函数
    public SpiritualRoot(SpiritualRootType type, SpiritualRootQuality quality) {
        this.type = type;
        this.quality = quality;
        this.cultivationSpeedBonus = calculateCultivationSpeedBonus();
        this.specialAbility = generateSpecialAbility();
    }
    
    // 指定灵根类型的构造函数（随机品质）
    public SpiritualRoot(SpiritualRootType type) {
        this(type, SpiritualRootQuality.randomQuality());
    }
    
    // 从NBT数据加载的构造函数
    public SpiritualRoot(CompoundTag nbt) {
        this.type = SpiritualRootType.fromId(nbt.getString("Type"));
        this.quality = SpiritualRootQuality.fromId(nbt.getString("Quality"));
        this.cultivationSpeedBonus = nbt.getFloat("CultivationSpeedBonus");
        this.specialAbility = nbt.getString("SpecialAbility");
    }
    
    /**
     * 保存到NBT数据
     */
    public CompoundTag save() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("Type", this.type.getId());
        nbt.putString("Quality", this.quality.getId());
        nbt.putFloat("CultivationSpeedBonus", this.cultivationSpeedBonus);
        nbt.putString("SpecialAbility", this.specialAbility);
        return nbt;
    }
    
    /**
     * 随机生成灵根类型
     */
    private static SpiritualRootType generateRandomRootType() {
        Random random = new Random();
        float chance = random.nextFloat();
        
        if (chance < 0.05) {
            return SpiritualRootType.NONE; // 5%概率无灵根
        } else {
            // 95%概率获得五行灵根之一
            return SpiritualRootType.getRandomRoot();
        }
    }
    
    /**
     * 计算修炼速度加成
     */
    private float calculateCultivationSpeedBonus() {
        // 基础加成：无灵根为0.5，其他为1.0
        float baseBonus = this.type == SpiritualRootType.NONE ? 0.5f : 1.0f;
        
        // 品质加成
        float qualityBonus = this.quality.getCultivationBonus();
        
        return baseBonus * qualityBonus;
    }
    
    /**
     * 生成特殊能力描述
     */
    private String generateSpecialAbility() {
        if (this.type == SpiritualRootType.NONE) {
            return "无特殊能力";
        }
        
        // 根据灵根类型生成特殊能力
        switch (this.type) {
            case GOLD:
                return "金系功法修炼速度提升10%";
            case WOOD:
                return "木系功法修炼速度提升10%，治疗效果增强";
            case WATER:
                return "水系功法修炼速度提升10%，灵力恢复速度提升";
            case FIRE:
                return "火系功法修炼速度提升15%，攻击力增强";
            case EARTH:
                return "土系功法修炼速度提升5%，防御力增强";
            default:
                return "未知能力";
        }
    }
    
    // Getter方法
    public SpiritualRootType getType() {
        return type;
    }
    
    public SpiritualRootQuality getQuality() {
        return quality;
    }
    
    public float getCultivationSpeedBonus() {
        return cultivationSpeedBonus;
    }
    
    public String getSpecialAbility() {
        return specialAbility;
    }
    
    /**
     * 获取灵根对应的颜色
     * @return 灵根颜色的RGB值
     */
    public int getColor() {
        return type.getColor();
    }
    
    // Setter方法
    public void setType(SpiritualRootType type) {
        this.type = type;
        this.cultivationSpeedBonus = calculateCultivationSpeedBonus();
        this.specialAbility = generateSpecialAbility();
    }
    
    public void setQuality(SpiritualRootQuality quality) {
        this.quality = quality;
        this.cultivationSpeedBonus = calculateCultivationSpeedBonus();
    }
}