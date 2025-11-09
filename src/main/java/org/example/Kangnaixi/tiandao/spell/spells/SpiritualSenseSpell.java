package org.example.Kangnaixi.tiandao.spell.spells;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.capability.ICultivation;
import org.example.Kangnaixi.tiandao.cultivation.CultivationRealm;
import org.example.Kangnaixi.tiandao.spell.SpellData;

import java.util.List;

/**
 * 灵识探查术法
 * 释放灵识感知周围32格内的所有生物
 */
public class SpiritualSenseSpell extends SpellData {
    
    private static final double SENSE_RADIUS = 32.0;
    
    public SpiritualSenseSpell() {
        super(
            "spiritual_sense",
            "灵识探查",
            "释放灵识感知周围32格内的所有生物。\n高亮显示生物并穿墙可见，持续20秒。",
            CultivationRealm.QI_CONDENSATION,
            7,
            10.0,  // 消耗10灵力
            2.0,   // 每秒消耗2灵力
            60,    // 60秒冷却
            20,    // 持续20秒
            SpellType.DURATION
        );
    }
    
    @Override
    public boolean cast(ServerPlayer player, ICultivation cultivation) {
        // 检查基础条件
        if (!super.cast(player, cultivation)) {
            return false;
        }
        
        // 应用夜视效果（帮助看清周围）
        MobEffectInstance nightVision = new MobEffectInstance(
            MobEffects.NIGHT_VISION,
            400, // 20秒
            0,
            false,
            false,
            true
        );
        player.addEffect(nightVision);
        
        // 激活术法状态
        long endTime = System.currentTimeMillis() + (20 * 1000);
        cultivation.activateSpell(getId(), endTime);
        
        // 应用发光效果到周围实体
        applyGlowingToNearbyEntities(player);
        
        // 播放音效
        player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.5f, 0.8f);
        
        // 粒子效果已移除，等待后续更好的渲染方式
        // spawnSenseParticles(player);
        
        Tiandao.LOGGER.info("玩家 {} 释放灵识探查，感知半径: {}格",
            player.getName().getString(), SENSE_RADIUS);
        
        return true;
    }
    
    /**
     * 应用发光效果到周围实体
     */
    private void applyGlowingToNearbyEntities(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        
        // 获取周围32格内的所有实体
        AABB searchBox = new AABB(
            player.getX() - SENSE_RADIUS,
            player.getY() - SENSE_RADIUS,
            player.getZ() - SENSE_RADIUS,
            player.getX() + SENSE_RADIUS,
            player.getY() + SENSE_RADIUS,
            player.getZ() + SENSE_RADIUS
        );
        
        List<Entity> entities = serverLevel.getEntities(player, searchBox);
        
        int glowingCount = 0;
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity livingEntity) {
                // 应用发光效果（20秒）
                MobEffectInstance glowing = new MobEffectInstance(
                    MobEffects.GLOWING,
                    400, // 20秒
                    0,
                    false,
                    false,
                    true
                );
                livingEntity.addEffect(glowing);
                glowingCount++;
            }
        }
        
        player.sendSystemMessage(
            net.minecraft.network.chat.Component.literal(
                String.format("§e灵识探查：感知到 §a%d §e个生物", glowingCount)
            )
        );
    }
    
    @Override
    public void onTick(ServerPlayer player, ICultivation cultivation) {
        // 每秒检查一次
        if (player.tickCount % 20 == 0) {
            // 消耗维持灵力
            double maintenanceCost = getMaintenanceCost();
            if (!cultivation.consumeSpiritPower(maintenanceCost)) {
                // 灵力不足，结束术法
                cultivation.deactivateSpell(getId());
                player.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal("§c灵力不足，灵识探查效果结束！")
                );
                onEnd(player, cultivation);
                return;
            }
            
            // 触发经验转化（维持消耗的灵气转化为经验）
            org.example.Kangnaixi.tiandao.cultivation.ExperienceConversionSystem.onSpiritConsumed(player, maintenanceCost);
            
            // 重新应用发光效果（保持效果持续）
            applyGlowingToNearbyEntities(player);
            
            // 粒子效果已移除，等待后续更好的渲染方式
            // if (player.tickCount % 40 == 0) { // 每2秒一次
            //     spawnSenseParticles(player);
            // }
        }
    }
    
    /**
     * 生成灵识粒子效果
     */
    private void spawnSenseParticles(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        
        double centerX = player.getX();
        double centerY = player.getY() + 1.5; // 眼睛位置
        double centerZ = player.getZ();
        
        // 生成金色光芒从眼睛向外扩散
        for (int i = 0; i < 15; i++) {
            double angle = (Math.PI * 2 * i) / 15;
            double radius = 0.5 + (Math.random() * 0.5);
            
            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            
            // 使用ENCHANT粒子（金色）
            serverLevel.sendParticles(
                net.minecraft.core.particles.ParticleTypes.ENCHANT,
                centerX + offsetX,
                centerY,
                centerZ + offsetZ,
                1,
                0.0, 0.0, 0.0,
                0.5
            );
        }
        
        // 添加向外扩散的波纹效果
        for (int ring = 1; ring <= 3; ring++) {
            double ringRadius = ring * 2.0;
            int particleCount = ring * 12;
            
            for (int i = 0; i < particleCount; i++) {
                double angle = (Math.PI * 2 * i) / particleCount;
                
                double offsetX = Math.cos(angle) * ringRadius;
                double offsetZ = Math.sin(angle) * ringRadius;
                
                serverLevel.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.WAX_ON,
                    centerX + offsetX,
                    centerY,
                    centerZ + offsetZ,
                    1,
                    0.0, 0.0, 0.0,
                    0.01
                );
            }
        }
    }
    
    @Override
    public void onEnd(ServerPlayer player, ICultivation cultivation) {
        // 术法结束时播放音效
        player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.3f, 0.6f);
        
        Tiandao.LOGGER.info("玩家 {} 的灵识探查效果结束", player.getName().getString());
    }
}

