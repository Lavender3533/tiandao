package org.example.Kangnaixi.tiandao.practice;

import net.minecraft.server.level.ServerPlayer;
import org.example.Kangnaixi.tiandao.capability.ICultivation;

/**
 * 修炼方式接口
 * 定义不同修炼方式的通用行为
 */
public interface PracticeMethod {
    
    /**
     * 获取修炼方式的唯一标识符
     * @return 修炼方式ID（如 "meditation", "alchemy", "combat"）
     */
    String getId();
    
    /**
     * 获取修炼方式的显示名称
     * @return 显示名称（如 "打坐修炼"）
     */
    String getDisplayName();
    
    /**
     * 检查玩家是否可以开始此修炼方式
     * @param player 玩家
     * @param cultivation 玩家的修仙能力
     * @return true如果可以开始，false否则
     */
    boolean canStart(ServerPlayer player, ICultivation cultivation);
    
    /**
     * 获取不能开始的原因提示
     * @param player 玩家
     * @param cultivation 玩家的修仙能力
     * @return 提示消息，如果可以开始则返回null
     */
    String getCannotStartReason(ServerPlayer player, ICultivation cultivation);
    
    /**
     * 当玩家开始修炼时调用
     * @param player 玩家
     * @param cultivation 玩家的修仙能力
     */
    void onStart(ServerPlayer player, ICultivation cultivation);
    
    /**
     * 每tick调用一次（服务器端）
     * @param player 玩家
     * @param cultivation 玩家的修仙能力
     * @return true继续修炼，false自动停止
     */
    boolean onTick(ServerPlayer player, ICultivation cultivation);
    
    /**
     * 当玩家停止修炼时调用
     * @param player 玩家
     * @param cultivation 玩家的修仙能力
     * @param reason 停止原因（"manual", "move", "hurt", "full"等）
     */
    void onStop(ServerPlayer player, ICultivation cultivation, String reason);
    
    /**
     * 获取修炼经验获取速率（每秒）
     * @param player 玩家
     * @param cultivation 玩家的修仙能力
     * @return 每秒获得的经验点数
     */
    double getExperienceRate(ServerPlayer player, ICultivation cultivation);
    
    /**
     * 获取灵力恢复速度加成倍率
     * @param player 玩家
     * @param cultivation 玩家的修仙能力
     * @return 恢复速度倍率（1.0 = 无加成，2.0 = 双倍恢复）
     */
    double getSpiritRecoveryBonus(ServerPlayer player, ICultivation cultivation);
    
    /**
     * 检查是否应该自动停止修炼
     * @param player 玩家
     * @param cultivation 玩家的修仙能力
     * @return 如果应该停止返回停止原因，否则返回null
     */
    String shouldAutoStop(ServerPlayer player, ICultivation cultivation);
}

