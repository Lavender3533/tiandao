package org.example.Kangnaixi.tiandao.practice;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.capability.ICultivation;
import org.example.Kangnaixi.tiandao.cultivation.CultivationRealm;
import org.example.Kangnaixi.tiandao.cultivation.SpiritualRootType;
import org.example.Kangnaixi.tiandao.network.CultivationDataSyncPacket;
import org.example.Kangnaixi.tiandao.network.NetworkHandler;

/**
 * 打坐修炼实现
 * 最基础的修炼方式
 */
public class MeditationPractice implements PracticeMethod {
    
    @Override
    public String getId() {
        return "meditation";
    }
    
    @Override
    public String getDisplayName() {
        return "打坐修炼";
    }
    
    @Override
    public boolean canStart(ServerPlayer player, ICultivation cultivation) {
        return getCannotStartReason(player, cultivation) == null;
    }
    
    @Override
    public String getCannotStartReason(ServerPlayer player, ICultivation cultivation) {
        // 检查灵根
        if (cultivation.getSpiritualRoot() == SpiritualRootType.NONE) {
            return "凡人无法修炼，需要灵根";
        }
        
        // 检查功法 - 暂时禁用
        // if (!TechniqueChecker.hasBasicTechniqueEquipped(player)) {
        //     return "需要装备功法才能修炼";
        // }
        
        // 检查灵力
        if (cultivation.getSpiritPower() <= 0) {
            return "灵力不足，无法开始修炼";
        }
        
        // 检查战斗状态
        if (cultivation.isInCombat()) {
            return "战斗状态无法修炼，请等待5秒";
        }
        
        return null; // 可以开始
    }
    
    @Override
    public void onStart(ServerPlayer player, ICultivation cultivation) {
        // 设置修炼状态
        cultivation.setPracticing(true);
        cultivation.setCurrentPracticeMethod(getId());
        
        // 强制蹲下
        player.setShiftKeyDown(true);
        
        // 初始化时间加速
        cultivation.setTimeAcceleration(1.0);
        
        // 记录打坐开始时间
        cultivation.setPracticeStartTime(player.level().getGameTime());
        
        // 发送开始消息
        player.sendSystemMessage(Component.literal("§a开始打坐修炼..."));
        player.sendSystemMessage(Component.literal("§7灵力恢复速度提升，时间流逝加速"));
        
        Tiandao.LOGGER.info("玩家 {} 开始打坐修炼", player.getName().getString());
    }
    
    @Override
    public boolean onTick(ServerPlayer player, ICultivation cultivation) {
        // 每tick执行一次（20 ticks = 1秒）
        
        // 检查自动停止条件
        String stopReason = shouldAutoStop(player, cultivation);
        if (stopReason != null) {
            onStop(player, cultivation, stopReason);
            return false;
        }
        
        // 应用灵力恢复加成（在 CultivationTickHandler 中处理）
        // 打坐不再直接获得经验，经验通过消耗灵气获得
        
        // 更新时间加速（每tick更新一次）
        updateTimeAcceleration(player, cultivation);
        
        // 打坐时，即使灵力满了也要消耗灵气来获得经验（每秒处理一次）
        if (player.tickCount % 20 == 0) {
            processMeditationSpiritConsumption(player, cultivation);
        }
        
        // 检查是否达到突破条件（每秒检查一次）
        if (player.tickCount % 20 == 0) {
            int currentExp = cultivation.getCultivationExperience();
            int requiredExp = cultivation.getRequiredExperienceForSubRealm();
            
            if (currentExp >= requiredExp && requiredExp > 0) {
                attemptBreakthrough(player, cultivation);
            }
        }
        
        // 打坐修炼时也增加功法经验（每秒2点，比被动恢复快）
        if (player.tickCount % 20 == 0) {
            if (cultivation.hasEquippedTechnique()) {
                org.example.Kangnaixi.tiandao.technique.TechniqueData technique = cultivation.getEquippedTechnique();
                if (technique != null && !technique.isMaxLevel()) {
                    boolean leveledUp = technique.addExperience(2);
                    
                    if (leveledUp) {
                        player.sendSystemMessage(Component.literal(""));
                        player.sendSystemMessage(Component.literal("§6§l【功法升级】§e " + technique.getName() + " §7提升至 §a" + technique.getLevel() + "级！"));
                        player.sendSystemMessage(Component.literal("§7修炼效率: §a" + String.format("%.1f%%", technique.getEfficiencyBonus() * 100)));
                        player.sendSystemMessage(Component.literal(""));
                        
                        player.playSound(net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, 0.5f, 1.2f);
                        
                        Tiandao.LOGGER.info("玩家 {} 的功法 {} 在打坐中升级至 {}级", 
                            player.getName().getString(), technique.getName(), technique.getLevel());
                    }
                }
            }
        }
        
        // 粒子效果已移除，等待后续更好的渲染方式
        
        return true; // 继续修炼
    }
    
    /**
     * 处理打坐时的灵气消耗（用于获得经验）
     * 打坐时，基于恢复速度计算消耗的灵气，即使灵力满了也能获得经验
     */
    private void processMeditationSpiritConsumption(ServerPlayer player, ICultivation cultivation) {
        // 打坐时，每秒基于恢复速度计算应该消耗的灵气来获得经验
        // 核心逻辑：将恢复的灵气转化为经验，即使灵力满了也能获得经验
        
        // 计算基础恢复速度倍数（getSpiritPowerRecoveryRate 返回的是倍数，不是实际恢复量）
        double baseRecoveryRate = cultivation.getSpiritPowerRecoveryRate();
        
        // 计算强度加成
        double intensityBonus = 1.0;
        if (cultivation instanceof org.example.Kangnaixi.tiandao.capability.CultivationCapability) {
            org.example.Kangnaixi.tiandao.capability.CultivationCapability cap = 
                (org.example.Kangnaixi.tiandao.capability.CultivationCapability) cultivation;
            intensityBonus = cap.getIntensityBasedRecoveryBonus(player);
        }
        
        // 环境密度加成
        double environmentalDensity = cultivation.getEnvironmentalDensity();
        
        // 打坐恢复加成（+100%，即2倍）
        double meditationBonus = getSpiritRecoveryBonus(player, cultivation);
        
        // 时间加速加成
        double timeAcceleration = cultivation.getTimeAcceleration();
        
        // 计算每秒恢复的灵力
        // 参考 CultivationTickHandler 中的计算方式（它使用了环境密度）：
        // 每分钟恢复 = baseRecoveryRate * intensityBonus * environmentalDensity
        // 每秒恢复 = (baseRecoveryRate * intensityBonus * environmentalDensity) / 60
        // 但打坐时有额外的加成：meditationBonus 和 timeAcceleration
        // 所以打坐时每秒恢复 = (baseRecoveryRate * intensityBonus * environmentalDensity * meditationBonus * timeAcceleration) / 60
        // 
        // 但是，CultivationEvents 使用的是每0.1秒恢复的机制：
        // 每0.1秒恢复 = 0.1 * intensityBonus * baseRecoveryRate * meditationBonus * timeAcceleration
        // 每秒恢复 = 10 * 0.1 * intensityBonus * baseRecoveryRate * meditationBonus * timeAcceleration
        //          = intensityBonus * baseRecoveryRate * meditationBonus * timeAcceleration
        // 
        // 为了统一，我们使用与 CultivationEvents 相同的计算方式，但加上环境密度
        // 每秒恢复 = intensityBonus * baseRecoveryRate * environmentalDensity * meditationBonus * timeAcceleration
        double recoveryPerSecond = intensityBonus * baseRecoveryRate * environmentalDensity * meditationBonus * timeAcceleration;
        
        // 功法效率加成（影响转化效率，但不影响恢复速度）
        double techniqueBonus = 1.0;
        if (cultivation.hasEquippedTechnique()) {
            org.example.Kangnaixi.tiandao.technique.TechniqueData technique = cultivation.getEquippedTechnique();
            if (technique != null) {
                techniqueBonus = technique.getEfficiencyBonus();
            }
        }
        
        // 打坐时，将恢复的灵气转化为经验
        // 消耗量 = 恢复速度 × 功法效率（功法可以提高转化效率，但不能超过恢复速度）
        // 如果功法效率 > 1.0，意味着可以消耗更多，但这需要从现有灵力中扣除
        double consumptionPerSecond = recoveryPerSecond * Math.min(techniqueBonus, 2.0); // 最多消耗2倍恢复速度
        
        // 调试日志：输出计算值（仅在前几次调用时）
        if (player.tickCount % 400 == 0) { // 每20秒输出一次
            Tiandao.LOGGER.info("打坐消耗计算: 基础恢复={}, 强度加成={}, 环境密度={}, 打坐加成={}, 时间加速={}, 每秒恢复={}, 每秒消耗={}",
                baseRecoveryRate, intensityBonus, environmentalDensity, meditationBonus, timeAcceleration, recoveryPerSecond, consumptionPerSecond);
        }
        
        // 如果计算出的消耗量大于0，就转化为经验
        if (consumptionPerSecond > 0.001) { // 降低阈值，确保即使很小的消耗也能触发
            double currentSpirit = cultivation.getSpiritPower();
            double maxSpirit = cultivation.getMaxSpiritPower();
            boolean isSpiritFull = Math.abs(currentSpirit - maxSpirit) < 0.01; // 考虑浮点数误差
            
            // 核心逻辑：打坐时，将恢复的灵气转化为经验
            // 即使灵力满了，也能通过消耗"恢复的灵气"来获得经验
            
            double actualConsumption = 0.0;
            double spiritToConsume = 0.0; // 实际需要消耗的现有灵力
            
            if (isSpiritFull) {
                // 情况1：灵力已满
                // 为了视觉效果，我们消耗一小部分灵力（消耗速度的一半），然后再恢复
                // 这样用户能看到灵力有变化，但仍然能持续获得经验
                // 消耗量 = min(消耗速度, 恢复速度的一半)
                spiritToConsume = Math.min(consumptionPerSecond, recoveryPerSecond * 0.5);
                if (spiritToConsume > 0.1) { // 至少消耗0.1点，确保有视觉效果
                    cultivation.consumeSpiritPower(spiritToConsume);
                }
                // 实际消耗量用于经验转化（基于恢复速度）
                actualConsumption = consumptionPerSecond;
            } else if (currentSpirit >= consumptionPerSecond) {
                // 情况2：灵力未满但有足够的现有灵力
                // 消耗现有灵力
                actualConsumption = consumptionPerSecond;
                spiritToConsume = consumptionPerSecond;
                cultivation.consumeSpiritPower(spiritToConsume);
            } else if (currentSpirit > 0) {
                // 情况3：灵力不足但有剩余
                // 消耗现有所有灵力
                spiritToConsume = currentSpirit;
                cultivation.consumeSpiritPower(spiritToConsume);
                
                // 剩余部分视为"消耗恢复的灵气"
                double remainingConsumption = consumptionPerSecond - spiritToConsume;
                if (remainingConsumption > 0) {
                    // 这部分不会实际扣除现有灵力，但会转化为经验
                    actualConsumption = consumptionPerSecond; // 总消耗量
                } else {
                    actualConsumption = spiritToConsume;
                }
            } else {
                // 情况4：灵力为0
                // 直接消耗"恢复的灵气"（不实际扣除，但转化为经验）
                actualConsumption = consumptionPerSecond;
                spiritToConsume = 0.0; // 不消耗现有灵力（因为已经是0）
            }
            
            // 无论哪种情况，都触发经验转化
            // 这样即使灵力满了，也能通过消耗"恢复的灵气"来获得经验
            if (actualConsumption > 0.001) { // 降低阈值，确保即使很小的消耗也能触发
                // 调试日志：输出实际消耗（每20秒输出一次）
                if (player.tickCount % 400 == 0) {
                    Tiandao.LOGGER.info("打坐经验转化: 灵力={}/{}, 满={}, 实际消耗={}",
                        String.format("%.1f", currentSpirit), String.format("%.1f", maxSpirit), isSpiritFull, String.format("%.2f", actualConsumption));
                }
                org.example.Kangnaixi.tiandao.cultivation.ExperienceConversionSystem.onSpiritConsumed(player, actualConsumption);
            } else {
                // 如果消耗量太小，输出警告
                if (player.tickCount % 400 == 0) {
                    Tiandao.LOGGER.warn("打坐消耗量过小: consumptionPerSecond={}, actualConsumption={}, 可能无法获得经验",
                        String.format("%.2f", consumptionPerSecond), String.format("%.2f", actualConsumption));
                }
            }
        } else {
            // 如果计算出的消耗量太小，输出警告
            if (player.tickCount % 400 == 0) {
                Tiandao.LOGGER.warn("打坐消耗计算过小: consumptionPerSecond={}, 可能无法获得经验。恢复速率={}, 强度加成={}, 环境密度={}, 打坐加成={}, 时间加速={}",
                    String.format("%.2f", consumptionPerSecond), String.format("%.2f", baseRecoveryRate),
                    String.format("%.2f", intensityBonus), String.format("%.2f", environmentalDensity),
                    String.format("%.2f", meditationBonus), String.format("%.2f", timeAcceleration));
            }
        }
    }
    
    /**
     * 更新时间加速
     */
    private void updateTimeAcceleration(ServerPlayer player, ICultivation cultivation) {
        // 计算打坐时间（秒）
        // 简化处理：使用玩家tick计数（但这不准确，因为tick是全局的）
        // 更好的方法是存储打坐开始时间
        
        // 临时方案：假设每tick增加一点加速进度
        // 实际应该使用打坐持续时间
        double currentAcceleration = cultivation.getTimeAcceleration();
        double targetAcceleration = calculateTargetAcceleration(player, cultivation);
        
        // 平滑过渡到目标加速倍数
        if (Math.abs(currentAcceleration - targetAcceleration) > 0.01) {
            double newAcceleration = currentAcceleration + (targetAcceleration - currentAcceleration) * 0.05;
            cultivation.setTimeAcceleration(newAcceleration);
        }
    }
    
    /**
     * 计算目标时间加速倍数
     */
    private double calculateTargetAcceleration(ServerPlayer player, ICultivation cultivation) {
        long startTime = cultivation.getPracticeStartTime();
        
        if (startTime == 0) {
            return 1.0; // 未开始打坐
        }
        
        // 计算打坐持续时间（秒）
        long currentTime = player.level().getGameTime();
        long practiceDuration = currentTime - startTime;
        double practiceSeconds = practiceDuration / 20.0; // 转换为秒（1秒=20tick）
        
        // 加速曲线：前10秒→1.5倍，前30秒→2.0倍，前60秒→3.0倍
        // 公式：时间倍数 = 1.0 + (打坐时间 / 60) × 2.0
        double targetAcceleration = 1.0 + (practiceSeconds / 60.0) * 2.0;
        
        // 限制最大加速倍数为3.0
        return Math.min(3.0, Math.max(1.0, targetAcceleration));
    }
    
    @Override
    public void onStop(ServerPlayer player, ICultivation cultivation, String reason) {
        // 重置时间加速
        cultivation.setTimeAcceleration(1.0);
        cultivation.setPracticeStartTime(0);
        
        // 清除修炼状态
        cultivation.setPracticing(false);
        cultivation.setCurrentPracticeMethod("");
        
        // 取消蹲下（如果是自动退出）
        if (!reason.equals("manual")) {
            player.setShiftKeyDown(false);
        }
        
        // 发送停止消息
        switch (reason) {
            case "manual":
                player.sendSystemMessage(Component.literal("§7停止修炼"));
                break;
            case "move":
                player.sendSystemMessage(Component.literal("§c移动打断了修炼"));
                break;
            case "hurt":
                player.sendSystemMessage(Component.literal("§c受到伤害，修炼中断！"));
                break;
            case "full":
                player.sendSystemMessage(Component.literal("§a灵力已满，自动停止修炼"));
                break;
            default:
                player.sendSystemMessage(Component.literal("§7修炼结束"));
        }
        
        Tiandao.LOGGER.info("玩家 {} 停止打坐修炼，原因: {}", player.getName().getString(), reason);
    }
    
    @Override
    public double getExperienceRate(ServerPlayer player, ICultivation cultivation) {
        // 基础经验：1点/秒
        double baseExp = 1.0;
        
        // 环境密度加成
        double densityBonus = cultivation.getEnvironmentalDensity();
        
        // 功法效率加成
        double techniqueBonus = 1.0;
        if (cultivation.hasEquippedTechnique()) {
            org.example.Kangnaixi.tiandao.technique.TechniqueData technique = cultivation.getEquippedTechnique();
            if (technique != null) {
                techniqueBonus = technique.getEfficiencyBonus();
            }
        }
        
        // 最终经验 = 基础 × 环境 × 功法效率
        return baseExp * densityBonus * techniqueBonus;
    }
    
    @Override
    public double getSpiritRecoveryBonus(ServerPlayer player, ICultivation cultivation) {
        // 打坐修炼提供 +100% 灵力恢复速度
        return 2.0; // 2倍恢复
    }
    
    @Override
    public String shouldAutoStop(ServerPlayer player, ICultivation cultivation) {
        // 检查移动 - 使用更宽松的阈值，避免误判
        // 只检测明显的移动（速度 > 0.1），忽略微小的物理抖动
        double movementSpeed = player.getDeltaMovement().length();
        if (movementSpeed > 0.1) {
            // 进一步检查：如果是水平移动（X或Z轴），才判定为移动
            // 这样可以忽略垂直方向的微小变化（比如蹲下时的位置微调）
            double horizontalMovement = Math.sqrt(
                player.getDeltaMovement().x * player.getDeltaMovement().x +
                player.getDeltaMovement().z * player.getDeltaMovement().z
            );
            if (horizontalMovement > 0.05) {
                return "move";
            }
        }
        
        // 注意：灵力满了之后不自动停止修炼
        // 继续修炼可以获得修炼经验，用于突破
        
        // 检查是否受伤（在其他地方处理）
        
        return null; // 无需自动停止
    }
    
    /**
     * 尝试突破（使用新的小境界系统）
     */
    private void attemptBreakthrough(ServerPlayer player, ICultivation cultivation) {
        // 检查经验是否达到要求
        int currentExp = cultivation.getCultivationExperience();
        int requiredExp = cultivation.getRequiredExperienceForSubRealm();
        
        if (currentExp < requiredExp || requiredExp <= 0) {
            return; // 经验不足或无需经验，无法突破
        }
        
        // 保存突破前的状态
        org.example.Kangnaixi.tiandao.cultivation.SubRealm oldSubRealm = cultivation.getSubRealm();
        org.example.Kangnaixi.tiandao.cultivation.CultivationRealm oldRealm = cultivation.getRealm();
        double oldMaxSpiritPower = cultivation.getMaxSpiritPower();
        
        // 使用统一的突破方法（会自动检查根基值）
        boolean success = cultivation.tryBreakthrough();
        
        if (success) {
            // 突破成功，发送消息
            org.example.Kangnaixi.tiandao.cultivation.SubRealm newSubRealm = cultivation.getSubRealm();
            org.example.Kangnaixi.tiandao.cultivation.CultivationRealm newRealm = cultivation.getRealm();
            
            // 判断是小境界还是大境界突破
            if (newRealm == oldRealm && newSubRealm != oldSubRealm) {
                // 小境界突破
                player.sendSystemMessage(Component.literal(""));
                player.sendSystemMessage(Component.literal("§a§l━━━ 小境界突破！ ━━━"));
                player.sendSystemMessage(Component.literal("§e恭喜！您已突破至 §b" + newRealm.getDisplayName() + " " + newSubRealm.getDisplayName()));
                player.sendSystemMessage(Component.literal("§a§l━━━━━━━━━━━━"));
                player.sendSystemMessage(Component.literal(""));
                
                Tiandao.LOGGER.info("玩家 {} 突破至 {} {}", 
                    player.getName().getString(), 
                    newRealm.getDisplayName(), 
                    newSubRealm.getDisplayName());
            } else if (newRealm != oldRealm) {
                // 大境界突破
                player.sendSystemMessage(Component.literal(""));
                player.sendSystemMessage(Component.literal("§6§l━━━ 大境界突破！ ━━━"));
                player.sendSystemMessage(Component.literal("§e恭喜！您已突破至 §b" + newRealm.getDisplayName() + " " + newSubRealm.getDisplayName()));
                player.sendSystemMessage(Component.literal("§7境界: " + oldRealm.getDisplayName() + " " + oldSubRealm.getDisplayName() + " → §b" + newRealm.getDisplayName() + " " + newSubRealm.getDisplayName()));
                player.sendSystemMessage(Component.literal("§7最大灵力: " + String.format("%.0f", oldMaxSpiritPower) + " → §a" + String.format("%.0f", cultivation.getMaxSpiritPower())));
                player.sendSystemMessage(Component.literal("§6§l━━━━━━━━━━━━"));
                player.sendSystemMessage(Component.literal(""));
                
                Tiandao.LOGGER.info("玩家 {} 突破至 {} {}", 
                    player.getName().getString(), 
                    newRealm.getDisplayName(), 
                    newSubRealm.getDisplayName());
            }
        } else {
            // 突破失败，检查原因并提示（避免刷屏，每10秒提示一次）
            int foundation = cultivation.getFoundation();
            org.example.Kangnaixi.tiandao.cultivation.SubRealm currentSubRealm = cultivation.getSubRealm();
            org.example.Kangnaixi.tiandao.cultivation.SubRealm nextSubRealm = currentSubRealm.getNext();
            
            if (player.tickCount % 200 == 0) { // 每10秒提示一次
                if (nextSubRealm != null) {
                    // 小境界突破失败
                    if (foundation < 50) {
                        player.sendSystemMessage(Component.literal("§c根基不稳，无法突破小境界！根基值: " + foundation + "/50"));
                        Tiandao.LOGGER.warn("玩家 {} 突破失败：根基不足 ({} < 50), 经验: {}/{}", 
                            player.getName().getString(), foundation, currentExp, requiredExp);
                    }
                } else {
                    // 大境界突破失败
                    org.example.Kangnaixi.tiandao.cultivation.CultivationRealm nextRealm = oldRealm.getNext();
                    if (nextRealm != null && foundation < 30) {
                        player.sendSystemMessage(Component.literal("§c根基严重受损，无法突破大境界！根基值: " + foundation + "/30"));
                        Tiandao.LOGGER.warn("玩家 {} 突破失败：根基不足 ({} < 30), 经验: {}/{}", 
                            player.getName().getString(), foundation, currentExp, requiredExp);
                    }
                }
            }
        }
        
        // 同步到客户端
        NetworkHandler.sendToPlayer(new CultivationDataSyncPacket(cultivation), player);
    }
    
    /**
     * 生成突破成功的粒子特效 - 华丽的爆发效果
     * 已移除，等待后续更好的渲染方式
     */
    @Deprecated
    private void spawnBreakthroughParticles(ServerPlayer player, CultivationRealm newRealm) {
        // 粒子效果已移除，等待后续更好的渲染方式
        // 原代码已删除
    }
    
    /**
     * 生成升级的粒子特效 - 简单的向上升腾效果
     */
    private void spawnLevelUpParticles(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        
        // 金色粒子向上升腾
        net.minecraft.core.particles.DustParticleOptions dustOptions = 
            new net.minecraft.core.particles.DustParticleOptions(
                new org.joml.Vector3f(1.0f, 0.84f, 0.0f), // 金色
                1.5f
            );
        
        // 生成向上的粒子流
        for (int i = 0; i < 20; i++) {
            double offsetX = (serverLevel.random.nextDouble() - 0.5) * 0.5;
            double offsetZ = (serverLevel.random.nextDouble() - 0.5) * 0.5;
            
            serverLevel.sendParticles(
                dustOptions,
                x + offsetX, y, z + offsetZ,
                0,
                0, 0.3, 0,
                0.3
            );
        }
    }
    
    /**
     * 生成打坐修炼的粒子效果 - 从四面八方汇聚灵气
     */
    private void spawnMeditationParticles(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        
        player.getCapability(org.example.Kangnaixi.tiandao.Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
            // 获取玩家的灵根类型
            SpiritualRootType rootType = cultivation.getSpiritualRoot();
            
            // 凡人无法修炼，不显示粒子
            if (rootType == SpiritualRootType.NONE) {
                return;
            }
            
            // 玩家中心位置
            double centerX = player.getX();
            double centerY = player.getY() + 1.0; // 胸部高度
            double centerZ = player.getZ();
            
            // 从四面八方生成汇聚的粒子
            // 每0.5秒生成5-8个粒子
            int particleCount = 5 + serverLevel.random.nextInt(4);
            
            for (int i = 0; i < particleCount; i++) {
                // 获取灵根颜色
                int color = rootType.getColor();
                float red = ((color >> 16) & 0xFF) / 255.0f;
                float green = ((color >> 8) & 0xFF) / 255.0f;
                float blue = (color & 0xFF) / 255.0f;
                
                // 在玩家周围2-3格的球形范围内随机生成起始位置
                double distance = 2.0 + serverLevel.random.nextDouble() * 1.0; // 2-3格
                double theta = serverLevel.random.nextDouble() * Math.PI * 2; // 水平角度
                double phi = serverLevel.random.nextDouble() * Math.PI; // 垂直角度
                
                double startX = centerX + distance * Math.sin(phi) * Math.cos(theta);
                double startY = centerY + distance * Math.cos(phi) * 0.5; // Y轴范围缩小一点
                double startZ = centerZ + distance * Math.sin(phi) * Math.sin(theta);
                
                // 计算指向玩家的速度向量
                double velocityX = (centerX - startX) * 0.15; // 0.15 是速度系数
                double velocityY = (centerY - startY) * 0.15;
                double velocityZ = (centerZ - startZ) * 0.15;
                
                // 生成 DUST 粒子（支持自定义颜色）
                net.minecraft.core.particles.DustParticleOptions dustOptions = 
                    new net.minecraft.core.particles.DustParticleOptions(
                        new org.joml.Vector3f(red, green, blue), 
                        1.0f // 粒子大小
                    );
                
                serverLevel.sendParticles(
                    dustOptions,
                    startX, startY, startZ, // 起始位置
                    0, // 粒子数量（0表示使用速度模式）
                    velocityX, velocityY, velocityZ, // 速度向量
                    0.8 // 速度倍率
                );
            }
        });
    }
}

