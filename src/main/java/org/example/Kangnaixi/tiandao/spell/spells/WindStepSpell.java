package org.example.Kangnaixi.tiandao.spell.spells;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.capability.ICultivation;
import org.example.Kangnaixi.tiandao.cultivation.CultivationRealm;
import org.example.Kangnaixi.tiandao.spell.SpellData;

/**
 * 御风术
 * 借助风之力瞬间移动8格距离
 */
public class WindStepSpell extends SpellData {
    
    private static final double TELEPORT_DISTANCE = 8.0;
    
    public WindStepSpell() {
        super(
            "wind_step",
            "御风术",
            "借助风之力瞬间移动8格距离。\n移动后2秒内免疫掉落伤害。",
            CultivationRealm.QI_CONDENSATION,
            5,
            15.0,  // 消耗15灵力
            0.0,   // 无维持消耗
            10,    // 10秒冷却
            0,     // 瞬发
            SpellType.INSTANT
        );
    }
    
    @Override
    public boolean cast(ServerPlayer player, ICultivation cultivation) {
        // 检查基础条件
        if (!super.cast(player, cultivation)) {
            return false;
        }
        
        // 粒子效果已移除，等待后续更好的渲染方式
        // spawnWindParticles(player, player.position());
        
        // 计算瞬移目标位置
        Vec3 targetPos = calculateTeleportPosition(player);
        if (targetPos == null) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c目标位置不安全，无法瞬移！"));
            // 返还灵力和冷却（失败了）
            cultivation.addSpiritPower(getSpiritCost());
            clearCooldown();
            return false;
        }
        
        // 执行瞬移
        player.teleportTo(targetPos.x, targetPos.y, targetPos.z);
        
        // 应用掉落保护buff（2秒）
        MobEffectInstance slowFalling = new MobEffectInstance(
            MobEffects.SLOW_FALLING,
            40, // 2秒
            0,
            false,
            false,
            true
        );
        player.addEffect(slowFalling);
        
        // 粒子效果已移除，等待后续更好的渲染方式
        // spawnWindParticles(player, targetPos);
        
        // 播放音效
        player.playSound(SoundEvents.ENDER_PEARL_THROW, 0.5f, 1.5f);
        
        Tiandao.LOGGER.info("玩家 {} 使用御风术瞬移至 {}", 
            player.getName().getString(), targetPos);
        
        return true;
    }
    
    /**
     * 计算瞬移目标位置
     */
    private Vec3 calculateTeleportPosition(ServerPlayer player) {
        // 获取玩家视角方向
        Vec3 lookVec = player.getLookAngle();
        Vec3 startPos = player.position();
        
        // 计算目标位置（视角方向8格）
        Vec3 targetPos = startPos.add(
            lookVec.x * TELEPORT_DISTANCE,
            lookVec.y * TELEPORT_DISTANCE,
            lookVec.z * TELEPORT_DISTANCE
        );
        
        // 安全性检查
        ServerLevel level = (ServerLevel) player.level();
        
        // 检查目标位置的方块
        BlockPos targetBlockPos = new BlockPos((int)targetPos.x, (int)targetPos.y, (int)targetPos.z);
        
        // 如果目标在方块内，尝试向上调整
        for (int yOffset = 0; yOffset <= 5; yOffset++) {
            BlockPos checkPos = targetBlockPos.above(yOffset);
            
            // 检查脚下有方块，头部和身体位置为空气
            BlockPos feetPos = checkPos;
            BlockPos bodyPos = checkPos.above();
            BlockPos headPos = checkPos.above(2);
            
            BlockState feetState = level.getBlockState(feetPos);
            BlockState bodyState = level.getBlockState(bodyPos);
            BlockState headState = level.getBlockState(headPos);
            
            // 如果找到合适的位置
            if (!feetState.isAir() && bodyState.isAir() && headState.isAir()) {
                // 站在方块上方
                return new Vec3(targetPos.x, feetPos.getY() + 1.0, targetPos.z);
            }
        }
        
        // 如果向上找不到，尝试向下
        for (int yOffset = 0; yOffset >= -10; yOffset--) {
            BlockPos checkPos = targetBlockPos.above(yOffset);
            
            BlockPos feetPos = checkPos;
            BlockPos bodyPos = checkPos.above();
            BlockPos headPos = checkPos.above(2);
            
            BlockState feetState = level.getBlockState(feetPos);
            BlockState bodyState = level.getBlockState(bodyPos);
            BlockState headState = level.getBlockState(headPos);
            
            if (!feetState.isAir() && bodyState.isAir() && headState.isAir()) {
                return new Vec3(targetPos.x, feetPos.getY() + 1.0, targetPos.z);
            }
        }
        
        // 检查是否在虚空
        if (targetPos.y < level.getMinBuildHeight()) {
            return null;
        }
        
        // 如果都不行，就返回原目标位置（玩家会悬空，但有缓降效果）
        return targetPos;
    }
    
    /**
     * 生成风粒子效果
     */
    private void spawnWindParticles(ServerPlayer player, Vec3 position) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        
        double centerX = position.x;
        double centerY = position.y + 0.5;
        double centerZ = position.z;
        
        // 生成绿色风旋效果
        for (int i = 0; i < 30; i++) {
            double angle = (Math.PI * 2 * i) / 30;
            double radius = 1.0 + (i * 0.05);
            double height = i * 0.1;
            
            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            
            // 使用HAPPY_VILLAGER粒子（绿色）
            serverLevel.sendParticles(
                net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                centerX + offsetX,
                centerY + height,
                centerZ + offsetZ,
                1,
                0.0, 0.0, 0.0,
                0.01
            );
        }
        
        // 添加一些额外的云雾效果
        for (int i = 0; i < 20; i++) {
            double randomX = (Math.random() - 0.5) * 2;
            double randomY = Math.random() * 2;
            double randomZ = (Math.random() - 0.5) * 2;
            
            serverLevel.sendParticles(
                net.minecraft.core.particles.ParticleTypes.CLOUD,
                centerX + randomX,
                centerY + randomY,
                centerZ + randomZ,
                1,
                0.0, 0.1, 0.0,
                0.02
            );
        }
    }
}

