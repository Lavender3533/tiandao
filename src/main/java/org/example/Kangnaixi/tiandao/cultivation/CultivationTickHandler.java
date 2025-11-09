package org.example.Kangnaixi.tiandao.cultivation;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.blocks.CultivationAltarBlock;
import org.example.Kangnaixi.tiandao.blocks.SpiritGatheringBlock;
import org.example.Kangnaixi.tiandao.capability.CultivationCapability;
import org.example.Kangnaixi.tiandao.network.CultivationDataSyncPacket;
import org.example.Kangnaixi.tiandao.network.NetworkHandler;

/**
 * 游戏刻事件处理器，用于定期更新修仙系统数据
 */
@Mod.EventBusSubscriber(modid = Tiandao.MODID)
public class CultivationTickHandler {
    
    // 每秒20个游戏刻，每分钟1200个游戏刻
    private static final int CULTIVATION_INTERVAL = 20 * 30; // 每30秒增加一次修炼进度
    private static final int SYNC_INTERVAL = 20; // 每1秒同步一次HUD数据（保证实时性）
    private static int tickCounter = 0;
    private static int syncCounter = 0;
    
    /**
     * 服务器刻事件处理
     * 定期更新所有在线玩家的灵力恢复
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        // 只在刻开始时处理
        if (event.phase == TickEvent.Phase.START) {
            tickCounter++;
            syncCounter++;
            
            // 每秒同步一次HUD数据（确保客户端和服务器数据一致）
            if (syncCounter >= SYNC_INTERVAL) {
                syncCounter = 0;
                syncAllPlayersData(event.getServer());
            }
            
            // 每tick更新灵力恢复（实时计算）
            for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
                player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
                    // 检查玩家是否有灵根，无灵根者无法恢复灵力
                    SpiritualRootType rootType = cultivation.getSpiritualRoot();
                    if (rootType == SpiritualRootType.NONE) {
                        return; // 凡人无法感知和恢复灵力
                    }
                    
                    // 获取当前和最大灵力
                    double currentPower = cultivation.getCurrentSpiritPower();
                    double maxPower = cultivation.getMaxSpiritPower();
                    
                    // 只有当灵力未满时才恢复
                    if (currentPower < maxPower) {
                        // 计算基础恢复速率
                        double baseRecoveryRate = cultivation.getSpiritPowerRecoveryRate();
                        
                        // 计算强度加成
                        double intensityBonus = 1.0;
                        if (cultivation instanceof CultivationCapability) {
                            CultivationCapability cap = (CultivationCapability) cultivation;
                            intensityBonus = cap.getIntensityBasedRecoveryBonus(player);
                        }
                        
                        // 每20tick（1秒）计算一次环境密度，实时响应环境变化
                        if (tickCounter % 20 == 0) {
                            double environmentalDensity = SpiritualDensityCalculator.calculateTotalDensity(player);
                            cultivation.setEnvironmentalDensity(environmentalDensity);
                        }
                        double environmentalDensity = cultivation.getEnvironmentalDensity();
                        
                        // 保存强度加成
                        cultivation.setIntensityBonus(intensityBonus);
                        
                        // 每tick恢复的灵力 = (每分钟恢复) / (20 * 60)
                        // 每分钟恢复 = 基础恢复速率 × 强度加成 × 环境密度
                        double recoveryPerMinute = baseRecoveryRate * intensityBonus * environmentalDensity;
                        double recoveryPerTick = recoveryPerMinute / 1200.0; // 1分钟 = 1200 ticks
                        
                        // 恢复灵力（不超过最大值）
                        double newPower = Math.min(currentPower + recoveryPerTick, maxPower);
                        cultivation.setSpiritPower(newPower);
                        
                        // 如果装备了功法，每次恢复灵力时增加功法经验
                        // 每秒恢复时增加1点经验
                        if (tickCounter % 20 == 0 && cultivation.hasEquippedTechnique()) {
                            org.example.Kangnaixi.tiandao.technique.TechniqueData technique = cultivation.getEquippedTechnique();
                            if (technique != null && !technique.isMaxLevel()) {
                                boolean leveledUp = technique.addExperience(1);
                                
                                if (leveledUp) {
                                    // 功法升级提示
                                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                        "§6§l【功法升级】§e " + technique.getName() + " §7提升至 §a" + technique.getLevel() + "级！"
                                    ));
                                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                        "§7修炼效率: §a" + String.format("%.1f%%", technique.getEfficiencyBonus() * 100)
                                    ));
                                    
                                    // 播放升级音效（可选）
                                    player.playSound(net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, 0.5f, 1.0f);
                                    
                                    // 同步到客户端
                                    NetworkHandler.sendToPlayer(new CultivationDataSyncPacket(cultivation), player);
                                    
                                    Tiandao.LOGGER.info("玩家 {} 的功法 {} 升级至 {}级", 
                                        player.getName().getString(), technique.getName(), technique.getLevel());
                                }
                            }
                        }
                    }
                });
            }
            
            // 每30秒增加一次修炼进度
            if (tickCounter % CULTIVATION_INTERVAL == 0) {
                updateAllPlayersCultivation(event.getServer());
            }
            
            // 防止tickCounter溢出，每小时重置一次
            if (tickCounter >= 72000) { // 1小时 = 72000 ticks
                tickCounter = 0;
            }
        }
    }
    
    /**
     * 更新所有玩家的修炼进度
     */
    private static void updateAllPlayersCultivation(net.minecraft.server.MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            updatePlayerCultivation(player);
        }
    }
    
    /**
     * 同步所有玩家的修仙数据到客户端
     * 每秒执行一次，确保HUD数据与服务器保持一致
     */
    private static void syncAllPlayersData(net.minecraft.server.MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
                // 只同步，不修改数据
                NetworkHandler.sendToPlayer(new CultivationDataSyncPacket(cultivation), player);
            });
        }
        // 降低日志频率，避免刷屏
        // Tiandao.LOGGER.debug("定期同步所有玩家的修仙数据到客户端");
    }
    
    /**
     * 更新单个玩家的修炼进度
     */
    private static void updatePlayerCultivation(Player player) {
        Level level = player.level();
        
        // 计算基础修炼进度
        float baseProgress = 1.0f;
        
        // 计算修炼台加成
        float altarBonus = CultivationAltarBlock.getTotalCultivationBonus(level, player, 5);
        
        // 计算灵气聚集方块加成
        boolean nearGatheringBlock = SpiritGatheringBlock.isNearGatheringBlock(level, player, 5);
        float gatheringBonus = nearGatheringBlock ? 0.5f : 0.0f;
        
        // 计算总修炼进度
        float totalProgress = baseProgress + altarBonus + gatheringBonus;
        
        // 增加修炼进度
        if (totalProgress > 0) {
            boolean canBreakthrough = CultivationRealmManager.addCultivationProgress(player, totalProgress);
            
            // 检查是否可以突破
            if (canBreakthrough) {
                CultivationRealmManager.BreakthroughResult result = CultivationRealmManager.tryBreakthrough(player);
                if (result.isSuccess()) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(result.getMessage()));
                }
            }
        }
    }
}