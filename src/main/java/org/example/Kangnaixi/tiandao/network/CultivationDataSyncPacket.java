package org.example.Kangnaixi.tiandao.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.capability.CultivationCapability;
import org.example.Kangnaixi.tiandao.capability.ICultivation;
import org.example.Kangnaixi.tiandao.cultivation.CultivationRealm;
import org.example.Kangnaixi.tiandao.cultivation.SpiritualRoot;
import org.example.Kangnaixi.tiandao.cultivation.SpiritualRootQuality;
import org.example.Kangnaixi.tiandao.cultivation.SpiritualRootType;

import java.util.function.Supplier;

/**
 * 修仙数据同步数据包
 * 用于将服务器端的修仙数据同步到客户端
 */
public class CultivationDataSyncPacket {
    private final String realmName;
    private final int level;
    private final double cultivationProgress;
    private final double spiritPower;
    private final double maxSpiritPower;
    private final String spiritualRootType;
    private final String spiritualRootQuality;
    private final double environmentalDensity;
    private final double intensityBonus;
    // 修炼系统数据
    private final boolean practicing;
    private final String currentPracticeMethod;
    private final int cultivationExperience;
    private final long lastCombatTime;
    
    public CultivationDataSyncPacket(ICultivation cultivation) {
        this.realmName = cultivation.getRealm().name();
        this.level = cultivation.getLevel();
        this.cultivationProgress = cultivation.getCultivationProgress();
        this.spiritPower = cultivation.getSpiritPower();
        this.maxSpiritPower = cultivation.getMaxSpiritPower();
        this.spiritualRootType = cultivation.getSpiritualRoot().name();
        this.environmentalDensity = cultivation.getEnvironmentalDensity();
        this.intensityBonus = cultivation.getIntensityBonus();
        
        // 修炼系统数据
        this.practicing = cultivation.isPracticing();
        this.currentPracticeMethod = cultivation.getCurrentPracticeMethod();
        this.cultivationExperience = cultivation.getCultivationExperience();
        this.lastCombatTime = cultivation.getLastCombatTime();
        
        if (cultivation instanceof CultivationCapability) {
            SpiritualRoot root = ((CultivationCapability) cultivation).getSpiritualRootObject();
            this.spiritualRootQuality = root != null && root.getQuality() != null ? 
                root.getQuality().name() : SpiritualRootQuality.NORMAL.name();
        } else {
            this.spiritualRootQuality = SpiritualRootQuality.NORMAL.name();
        }
    }
    
    public CultivationDataSyncPacket(FriendlyByteBuf buf) {
        this.realmName = buf.readUtf();
        this.level = buf.readInt();
        this.cultivationProgress = buf.readDouble();
        this.spiritPower = buf.readDouble();
        this.maxSpiritPower = buf.readDouble();
        this.spiritualRootType = buf.readUtf();
        this.spiritualRootQuality = buf.readUtf();
        this.environmentalDensity = buf.readDouble();
        this.intensityBonus = buf.readDouble();
        // 修炼系统数据
        this.practicing = buf.readBoolean();
        this.currentPracticeMethod = buf.readUtf();
        this.cultivationExperience = buf.readInt();
        this.lastCombatTime = buf.readLong();
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(realmName);
        buf.writeInt(level);
        buf.writeDouble(cultivationProgress);
        buf.writeDouble(spiritPower);
        buf.writeDouble(maxSpiritPower);
        buf.writeUtf(spiritualRootType);
        buf.writeUtf(spiritualRootQuality);
        buf.writeDouble(environmentalDensity);
        buf.writeDouble(intensityBonus);
        // 修炼系统数据
        buf.writeBoolean(practicing);
        buf.writeUtf(currentPracticeMethod);
        buf.writeInt(cultivationExperience);
        buf.writeLong(lastCombatTime);
    }
    
    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // 客户端处理
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
                    // 更新境界和等级
                    try {
                        CultivationRealm realm = CultivationRealm.valueOf(realmName);
                        cultivation.setRealm(realm);
                    } catch (IllegalArgumentException e) {
                        Tiandao.LOGGER.error("无效的境界名称: " + realmName, e);
                    }
                    
                    cultivation.setLevel(level);
                    cultivation.setCultivationProgress(cultivationProgress);
                    
                    // 更新灵力（关键！）
                    cultivation.setMaxSpiritPower(maxSpiritPower);
                    cultivation.setSpiritPower(spiritPower);
                    
                    // 更新环境密度和强度加成
                    cultivation.setEnvironmentalDensity(environmentalDensity);
                    cultivation.setIntensityBonus(intensityBonus);
                    
                    // 更新修炼系统数据
                    cultivation.setPracticing(practicing);
                    cultivation.setCurrentPracticeMethod(currentPracticeMethod);
                    cultivation.setCultivationExperience(cultivationExperience);
                    cultivation.setLastCombatTime(lastCombatTime);
                    
                    Tiandao.LOGGER.debug("客户端接收数据: 灵力={}/{}, 境界={} {}, 修炼中={}, 经验={}, 环境密度={}, 强度加成={}", 
                        spiritPower, maxSpiritPower, realmName, level, practicing, cultivationExperience, environmentalDensity, intensityBonus);
                    
                    // 更新灵根
                    try {
                        SpiritualRootType rootType = SpiritualRootType.valueOf(spiritualRootType);
                        cultivation.setSpiritualRoot(rootType);
                        
                        // 更新灵根品质
                        if (cultivation instanceof CultivationCapability) {
                            SpiritualRootQuality quality = SpiritualRootQuality.valueOf(spiritualRootQuality);
                            SpiritualRoot root = new SpiritualRoot(rootType, quality);
                            ((CultivationCapability) cultivation).setSpiritualRootObject(root);
                        }
                    } catch (IllegalArgumentException e) {
                        Tiandao.LOGGER.error("无效的灵根类型或品质: " + spiritualRootType + "/" + spiritualRootQuality, e);
                    }
                    
                    Tiandao.LOGGER.debug("客户端修仙数据已同步: 灵力=" + spiritPower + "/" + maxSpiritPower);
                });
            }
        });
        context.setPacketHandled(true);
    }
}

