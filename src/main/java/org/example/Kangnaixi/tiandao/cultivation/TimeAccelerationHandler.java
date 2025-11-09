package org.example.Kangnaixi.tiandao.cultivation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.Kangnaixi.tiandao.Tiandao;

import java.util.List;

/**
 * 时间加速处理器
 * 处理打坐时的时间加速效果
 */
@Mod.EventBusSubscriber(modid = Tiandao.MODID)
public class TimeAccelerationHandler {
    
    private static final int UPDATE_RADIUS = 8; // 影响范围：8格
    private static final int UPDATE_INTERVAL = 5; // 每5tick更新一次（避免性能问题）
    
    /**
     * 服务器Tick事件
     * 对正在打坐的玩家应用时间加速效果
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        
        // 每5tick执行一次（避免性能问题）
        if (event.getServer().getTickCount() % UPDATE_INTERVAL != 0) {
            return;
        }
        
        // 遍历所有玩家
        for (net.minecraft.server.level.ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
                // 只处理正在打坐的玩家
                if (!cultivation.isPracticing()) {
                    return;
                }
                
                double timeAcceleration = cultivation.getTimeAcceleration();
                
                // 如果时间加速倍数大于1.0，应用加速效果
                if (timeAcceleration > 1.0) {
                    applyTimeAcceleration(player, timeAcceleration);
                }
            });
        }
    }
    
    /**
     * 应用时间加速效果
     * @param player 玩家
     * @param acceleration 加速倍数
     */
    private static void applyTimeAcceleration(ServerPlayer player, double acceleration) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        
        BlockPos playerPos = player.blockPosition();
        
        // 计算需要额外执行的tick次数（向下取整）
        int extraTicks = (int) Math.floor(acceleration - 1.0);
        
        // 对玩家周围的区块应用加速效果
        for (int i = 0; i < extraTicks; i++) {
            // 加速作物生长（随机tick）
            accelerateCropGrowth(serverLevel, playerPos);
            
            // 加速生物AI（实体tick）
            accelerateEntityAI(serverLevel, playerPos);
        }
        
        // 如果有小数部分，按概率执行额外tick
        double fractionalPart = acceleration - 1.0 - extraTicks;
        if (Math.random() < fractionalPart) {
            accelerateCropGrowth(serverLevel, playerPos);
            accelerateEntityAI(serverLevel, playerPos);
        }
    }
    
    /**
     * 加速作物生长
     */
    private static void accelerateCropGrowth(ServerLevel level, BlockPos centerPos) {
        // 在玩家周围随机选择一些方块进行随机tick
        int blocksToTick = 10; // 每次加速tick处理10个方块
        
        for (int i = 0; i < blocksToTick; i++) {
            // 在影响范围内随机选择位置
            int offsetX = level.random.nextInt(UPDATE_RADIUS * 2 + 1) - UPDATE_RADIUS;
            int offsetY = level.random.nextInt(5) - 2; // Y轴范围：±2格
            int offsetZ = level.random.nextInt(UPDATE_RADIUS * 2 + 1) - UPDATE_RADIUS;
            
            BlockPos pos = centerPos.offset(offsetX, offsetY, offsetZ);
            BlockState state = level.getBlockState(pos);
            Block block = state.getBlock();
            
            // 只对可以随机tick的方块进行处理（作物、树苗等）
            if (block instanceof BonemealableBlock bonemealable) {
                // 执行随机tick（模拟自然生长）
                if (level.random.nextInt(10) == 0) { // 10%概率触发
                    // 使用新的API：performBonemeal
                    // 或者直接调用block的随机tick
                    try {
                        // 尝试触发生长（如果方块支持）
                        if (bonemealable.isValidBonemealTarget(level, pos, state, false)) {
                            // 模拟随机tick效果，通过随机触发生长
                            if (level.random.nextInt(5) == 0) {
                                bonemealable.performBonemeal(level, level.random, pos, state);
                            }
                        }
                    } catch (Exception e) {
                        // 如果失败，忽略（某些方块可能不支持）
                        Tiandao.LOGGER.debug("无法加速方块生长: {}", block.getName().getString());
                    }
                }
            }
        }
    }
    
    /**
     * 加速生物AI
     * 注意：直接调用 entity.tick() 可能导致问题，这里改为更安全的实现
     */
    private static void accelerateEntityAI(ServerLevel level, BlockPos centerPos) {
        // 获取玩家周围的所有实体
        net.minecraft.world.phys.AABB aabb = new net.minecraft.world.phys.AABB(centerPos).inflate(UPDATE_RADIUS);
        
        // 使用 getEntitiesOfClass 方法获取所有实体类型
        // 由于直接 tick 实体可能导致问题，这里暂时只影响某些特定的实体行为
        // 例如：加速被动生物的生长、加速动物的繁殖等
        
        // 获取所有生物实体（不包括玩家）
        List<net.minecraft.world.entity.LivingEntity> livingEntities = 
            level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, aabb, 
                entity -> !(entity instanceof ServerPlayer) && entity.isAlive() && !entity.isRemoved());
        
        // 对每个生物执行加速效果（但不直接 tick，避免影响移动和 AI）
        // 可以通过其他方式加速，例如：加速生命恢复、加速效果持续时间等
        for (net.minecraft.world.entity.LivingEntity entity : livingEntities) {
            if (entity.isAlive() && !entity.isRemoved()) {
                // 暂时不执行额外的 tick，因为可能导致问题
                // 时间加速主要通过加速作物生长和恢复速度来体现
                // TODO: 实现更安全的时间加速机制
            }
        }
    }
}

