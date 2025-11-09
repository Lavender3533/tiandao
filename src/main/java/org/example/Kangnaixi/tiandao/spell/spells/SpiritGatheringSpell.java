package org.example.Kangnaixi.tiandao.spell.spells;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.capability.ICultivation;
import org.example.Kangnaixi.tiandao.cultivation.CultivationRealm;
import org.example.Kangnaixi.tiandao.cultivation.SpiritualRootType;
import org.example.Kangnaixi.tiandao.spell.SpellData;

/**
 * 聚灵术
 * 打坐聚集周围灵气，10秒内灵力恢复速度+200%
 */
public class SpiritGatheringSpell extends SpellData {
    
    private double lastX = 0;
    private double lastY = 0;
    private double lastZ = 0;
    
    public SpiritGatheringSpell() {
        super(
            "spirit_gathering",
            "聚灵术",
            "打坐聚集周围灵气，10秒内灵力恢复速度+200%。\n移动将中断效果。",
            CultivationRealm.QI_CONDENSATION,
            9,
            5.0,   // 消耗5灵力
            0.0,   // 无维持消耗
            120,   // 120秒冷却
            10,    // 持续10秒
            SpellType.DURATION
        );
    }
    
    @Override
    public boolean cast(ServerPlayer player, ICultivation cultivation) {
        // 检查基础条件
        if (!super.cast(player, cultivation)) {
            return false;
        }
        
        // 记录玩家位置
        lastX = player.getX();
        lastY = player.getY();
        lastZ = player.getZ();
        
        // 强制玩家蹲下（打坐状态）
        player.setShiftKeyDown(true);
        
        // 激活术法状态
        long endTime = System.currentTimeMillis() + (10 * 1000);
        cultivation.activateSpell(getId(), endTime);
        
        // 播放音效
        player.playSound(SoundEvents.PORTAL_TRIGGER, 0.5f, 1.0f);
        
        // 粒子效果已移除，等待后续更好的渲染方式
        // spawnGatheringParticles(player, cultivation);
        
        player.sendSystemMessage(
            net.minecraft.network.chat.Component.literal("§a开始聚灵，灵力恢复速度+200%！")
        );
        
        Tiandao.LOGGER.info("玩家 {} 释放聚灵术，开始加速恢复灵力",
            player.getName().getString());
        
        return true;
    }
    
    @Override
    public void onTick(ServerPlayer player, ICultivation cultivation) {
        // 每tick检查玩家是否移动
        double currentX = player.getX();
        double currentY = player.getY();
        double currentZ = player.getZ();
        
        double distanceMoved = Math.sqrt(
            Math.pow(currentX - lastX, 2) +
            Math.pow(currentY - lastY, 2) +
            Math.pow(currentZ - lastZ, 2)
        );
        
        // 如果移动距离超过0.1格，中断效果
        if (distanceMoved > 0.1) {
            cultivation.deactivateSpell(getId());
            player.sendSystemMessage(
                net.minecraft.network.chat.Component.literal("§c移动了！聚灵术效果中断！")
            );
            onEnd(player, cultivation);
            return;
        }
        
        // 每秒恢复灵力（+200%意味着3倍恢复）
        if (player.tickCount % 20 == 0) {
            double baseRecovery = cultivation.getSpiritPowerRecoveryRate();
            double boostedRecovery = baseRecovery * 3.0; // 总计300%（基础100% + 额外200%）
            
            cultivation.addSpiritPower(boostedRecovery);
            
            // 粒子效果已移除，等待后续更好的渲染方式
            // spawnGatheringParticles(player, cultivation);
            
            // 每2秒播放一次音效
            if (player.tickCount % 40 == 0) {
                player.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.3f, 1.2f);
            }
        }
        
        // 保持蹲下状态
        if (!player.isShiftKeyDown()) {
            player.setShiftKeyDown(true);
        }
        
        // 更新位置
        lastX = currentX;
        lastY = currentY;
        lastZ = currentZ;
    }
    
    /**
     * 生成聚灵粒子效果
     */
    private void spawnGatheringParticles(ServerPlayer player, ICultivation cultivation) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        
        // 获取玩家灵根类型，生成对应颜色的粒子
        SpiritualRootType rootType = cultivation.getSpiritualRoot();
        
        double centerX = player.getX();
        double centerY = player.getY() + 0.5;
        double centerZ = player.getZ();
        
        // 从周围向玩家汇聚灵气粒子
        for (int i = 0; i < 10; i++) {
            // 在3-5格的球形范围内随机生成起始位置
            double distance = 3.0 + Math.random() * 2.0;
            double theta = Math.random() * Math.PI * 2; // 水平角度
            double phi = Math.random() * Math.PI; // 垂直角度
            
            double startX = centerX + distance * Math.sin(phi) * Math.cos(theta);
            double startY = centerY + distance * Math.cos(phi) * 0.5;
            double startZ = centerZ + distance * Math.sin(phi) * Math.sin(theta);
            
            // 计算指向玩家的速度向量
            double velocityX = (centerX - startX) * 0.1;
            double velocityY = (centerY - startY) * 0.1;
            double velocityZ = (centerZ - startZ) * 0.1;
            
            // 根据灵根类型选择粒子颜色
            net.minecraft.core.particles.ParticleOptions particleType;
            if (rootType != SpiritualRootType.NONE) {
                // 使用DUST粒子，根据灵根颜色
                int color = rootType.getColor();
                float red = ((color >> 16) & 0xFF) / 255.0f;
                float green = ((color >> 8) & 0xFF) / 255.0f;
                float blue = (color & 0xFF) / 255.0f;
                
                particleType = new net.minecraft.core.particles.DustParticleOptions(
                    new org.joml.Vector3f(red, green, blue),
                    1.0f
                );
            } else {
                // 凡人使用白色粒子
                particleType = net.minecraft.core.particles.ParticleTypes.END_ROD;
            }
            
            serverLevel.sendParticles(
                particleType,
                startX, startY, startZ,
                0,
                velocityX, velocityY, velocityZ,
                0.5
            );
        }
        
        // 在玩家周围生成螺旋上升的粒子
        double angle = (player.tickCount * 0.2) % (Math.PI * 2);
        for (int i = 0; i < 3; i++) {
            double spiralAngle = angle + (i * Math.PI * 2 / 3);
            double spiralRadius = 0.8;
            double spiralHeight = (player.tickCount % 40) * 0.05;
            
            double offsetX = Math.cos(spiralAngle) * spiralRadius;
            double offsetZ = Math.sin(spiralAngle) * spiralRadius;
            
            serverLevel.sendParticles(
                net.minecraft.core.particles.ParticleTypes.ENCHANT,
                centerX + offsetX,
                centerY + spiralHeight,
                centerZ + offsetZ,
                1,
                0.0, 0.0, 0.0,
                0.01
            );
        }
    }
    
    @Override
    public void onEnd(ServerPlayer player, ICultivation cultivation) {
        // 解除蹲下状态
        player.setShiftKeyDown(false);
        
        // 播放结束音效
        player.playSound(SoundEvents.FIRE_EXTINGUISH, 0.5f, 1.0f);
        
        player.sendSystemMessage(
            net.minecraft.network.chat.Component.literal("§e聚灵术效果结束")
        );
        
        Tiandao.LOGGER.info("玩家 {} 的聚灵术效果结束", player.getName().getString());
    }
}

