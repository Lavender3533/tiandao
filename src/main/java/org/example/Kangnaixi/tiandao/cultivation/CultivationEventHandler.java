package org.example.Kangnaixi.tiandao.cultivation;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 修仙系统事件处理器（已废弃，使用 CultivationEvents + Capability 系统）
 * 
 * 此类使用旧的 PersistentData 系统，已被新的 Capability 系统替代。
 * 为避免数据冲突，暂时禁用此事件处理器。
 */
// @Mod.EventBusSubscriber(modid = Tiandao.MODID) // 已禁用，避免与 Capability 系统冲突
public class CultivationEventHandler {
    
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        
        // 加载灵力数据
        SpiritualEnergyManager.loadPlayerEnergy(player);
        
        // 加载灵根数据
        SpiritualRootManager.loadPlayerSpiritualRoot(player);
        
        // 初始化境界数据
        CultivationRealmManager.loadPlayerRealmData(player, player.getPersistentData());
        
        // 更新最大灵力值
        CultivationRealmData realmData = CultivationRealmManager.getPlayerRealmData(player);
        SpiritualEnergyManager.updateMaxEnergy(player, realmData.getMaxEnergy(player));
    }
    
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        
        // 保存灵力数据
        SpiritualEnergyManager.savePlayerEnergy(player);
        
        // 保存灵根数据
        SpiritualRootManager.savePlayerSpiritualRoot(player);
        
        // 保存境界数据
        CultivationRealmManager.savePlayerRealmData(player, player.getPersistentData());
        
        // 移除玩家数据
        SpiritualEnergyManager.removePlayerEnergy(player.getUUID());
        SpiritualRootManager.removePlayerSpiritualRoot(player.getUUID());
        CultivationRealmManager.removePlayerRealmData(player.getUUID());
    }
    
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Entity original = event.getOriginal();
        Player player = event.getEntity();
        
        if (original instanceof Player originalPlayer) {
            // 复制灵力数据
            SpiritualEnergy originalEnergy = SpiritualEnergyManager.getPlayerEnergy(originalPlayer);
            if (originalEnergy != null) {
                SpiritualEnergyManager.setPlayerEnergy(player, originalEnergy);
            }
            
            // 复制灵根数据
            SpiritualRoot originalRoot = SpiritualRootManager.getPlayerSpiritualRoot(originalPlayer);
            if (originalRoot != null) {
                SpiritualRootManager.setPlayerSpiritualRoot(player, originalRoot);
            }
            
            // 复制境界数据
            CultivationRealmData originalRealm = CultivationRealmManager.getPlayerRealmData(originalPlayer);
            if (originalRealm != null) {
                CultivationRealmManager.setPlayerRealm(player, originalRealm.getCurrentRealm(), originalRealm.getRealmLevel());
            }
        }
    }
    
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        
        // 重新计算最大灵力值
        CultivationRealmData realmData = CultivationRealmManager.getPlayerRealmData(player);
        SpiritualEnergyManager.updateMaxEnergy(player, realmData.getMaxEnergy(player));
    }
}