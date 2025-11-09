package org.example.Kangnaixi.tiandao.client.event;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.Kangnaixi.tiandao.Tiandao;

/**
 * 客户端事件处理器，用于处理灵力粒子效果
 * 临时禁用以修复sprite初始化崩溃问题
 */
@Mod.EventBusSubscriber(modid = Tiandao.MODID, value = Dist.CLIENT)
public class ParticleEventHandler {
    
    /**
     * 在渲染世界最后阶段生成灵力粒子
     * 临时禁用以修复sprite初始化崩溃问题
     */
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        // 临时禁用粒子效果以避免崩溃
        // TODO: 修复 SpiritualEnergyParticle sprites 初始化问题后重新启用
        
        /* 原代码（已完全禁用）
        private static final Random random = new Random();
        private static int particleTimer = 0;
        
        // 只在AFTER_WEATHER阶段渲染粒子
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            return;
        }
        
        // 检查配置是否启用粒子效果
        if (!CultivationConfig.SHOW_SPIRIT_PARTICLES.get()) {
            return;
        }
        
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        
        if (player == null) {
            return;
        }
        
        // 获取玩家的修仙能力
        player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
            // 检查玩家是否有灵根
            SpiritualRootType rootType = cultivation.getSpiritualRoot();
            if (rootType == SpiritualRootType.NONE) {
                return;
            }
            
            // 检查玩家是否有足够的灵力
            if (cultivation.getCurrentSpiritPower() <= 0) {
                return;
            }
            
            // 每隔一定时间生成粒子
            particleTimer++;
            if (particleTimer % CultivationConfig.PARTICLE_SPAWN_RATE.get() != 0) {
                return;
            }
            
            // 获取粒子类型
            var particleType = ModParticles.getParticleTypeForRoot(rootType);
            if (particleType == null) {
                return;
            }
            
            // 在玩家周围生成粒子
            spawnSpiritualParticles(player, particleType);
        });
    
        // spawnSpiritualParticles方法:
        // 在玩家周围生成灵力粒子
    private static void spawnSpiritualParticles(Player player, SimpleParticleType particleType) {
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        
        // 在玩家周围随机位置生成粒子
        for (int i = 0; i < CultivationConfig.PARTICLE_COUNT.get(); i++) {
            double offsetX = (random.nextDouble() - 0.5) * CultivationConfig.PARTICLE_SPREAD.get();
            double offsetY = random.nextDouble() * CultivationConfig.PARTICLE_HEIGHT.get();
            double offsetZ = (random.nextDouble() - 0.5) * CultivationConfig.PARTICLE_SPREAD.get();
            
            // 粒子运动速度
            double motionX = (random.nextDouble() - 0.5) * 0.05;
            double motionY = random.nextDouble() * 0.1;
            double motionZ = (random.nextDouble() - 0.5) * 0.05;
            
            // 生成粒子
            player.level().addParticle(
                particleType,
                x + offsetX, 
                y + offsetY, 
                z + offsetZ,
                motionX, 
                motionY, 
                motionZ
            );
        }
        }
        */
    }
}