package org.example.Kangnaixi.tiandao.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.example.Kangnaixi.tiandao.Tiandao;

import java.util.Random;

/**
 * 灵气聚集方块，定期为周围的玩家恢复灵力
 */
public class SpiritGatheringBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 4, 14);
    
    private final float energyAmount; // 每次恢复的灵力值
    private final int intervalTicks; // 恢复间隔（刻）
    private final int range; // 影响范围
    
    public SpiritGatheringBlock(float energyAmount, int intervalTicks, int range) {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.0f).lightLevel(state -> 10));
        this.energyAmount = energyAmount;
        this.intervalTicks = intervalTicks;
        this.range = range;
    }
    
    @Override
    public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        return SHAPE;
    }
    
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        // 方块放置时，开始定期恢复灵力
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, intervalTicks);
        }
    }
    
    public void tick(BlockState state, Level level, BlockPos pos, Random random) {
        if (!level.isClientSide) {
            // 为周围的玩家恢复灵力
            level.getEntitiesOfClass(net.minecraft.world.entity.player.Player.class, 
                new net.minecraft.world.phys.AABB(pos).inflate(range))
                .forEach(player -> {
                    // 使用 Capability 系统恢复灵力
                    player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
                        cultivation.addSpiritPower(energyAmount);
                    });
                });
            
            // 安排下一次恢复
            level.scheduleTick(pos, this, intervalTicks);
        }
    }
    
    /**
     * 获取每次恢复的灵力值
     */
    public float getEnergyAmount() {
        return energyAmount;
    }
    
    /**
     * 获取恢复间隔（刻）
     */
    public int getIntervalTicks() {
        return intervalTicks;
    }
    
    /**
     * 获取影响范围
     */
    public int getRange() {
        return range;
    }
    
    /**
     * 检查玩家是否在灵气聚集方块附近
     * @param level 世界对象
     * @param player 玩家对象
     * @param range 检查范围
     * @return 是否在灵气聚集方块附近
     */
    public static boolean isNearGatheringBlock(Level level, net.minecraft.world.entity.player.Player player, int range) {
        BlockPos playerPos = player.blockPosition();
        
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos checkPos = playerPos.offset(x, y, z);
                    if (level.getBlockState(checkPos).getBlock() instanceof SpiritGatheringBlock) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * 获取附近灵气聚集方块的总恢复量
     * @param level 世界对象
     * @param player 玩家对象
     * @param range 检查范围
     * @return 总恢复量
     */
    public static float getTotalEnergyAmount(Level level, net.minecraft.world.entity.player.Player player, int range) {
        float totalAmount = 0.0f;
        BlockPos playerPos = player.blockPosition();
        
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos checkPos = playerPos.offset(x, y, z);
                    BlockState state = level.getBlockState(checkPos);
                    if (state.getBlock() instanceof SpiritGatheringBlock gathering) {
                        totalAmount += gathering.getEnergyAmount();
                    }
                }
            }
        }
        
        return totalAmount;
    }
}