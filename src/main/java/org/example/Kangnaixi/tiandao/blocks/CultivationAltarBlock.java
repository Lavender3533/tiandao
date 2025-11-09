package org.example.Kangnaixi.tiandao.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import org.example.Kangnaixi.tiandao.menu.CultivationMenuProvider;
import org.example.Kangnaixi.tiandao.blockentity.CultivationAltarBlockEntity;

/**
 * 修炼台方块类，用于加速修炼
 */
public class CultivationAltarBlock extends Block implements EntityBlock {
    private final float cultivationBonus; // 修炼加成倍数
    private final float energyRegenBonus; // 灵力恢复加成倍数
    
    public CultivationAltarBlock(float cultivationBonus, float energyRegenBonus) {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.0f));
        this.cultivationBonus = cultivationBonus;
        this.energyRegenBonus = energyRegenBonus;
    }
    
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CultivationAltarBlockEntity(pos, state);
    }
    
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        
        if (player instanceof ServerPlayer serverPlayer) {
            // 打开修炼界面
            serverPlayer.openMenu(new CultivationMenuProvider(pos));
            
            // 发送修炼加成信息
            player.sendSystemMessage(Component.literal("修炼台提供修炼加成: +" + (cultivationBonus * 100) + "%"));
            player.sendSystemMessage(Component.literal("修炼台提供灵力恢复加成: +" + (energyRegenBonus * 100) + "%"));
        }
        
        return InteractionResult.CONSUME;
    }
    
    /**
     * 获取修炼加成倍数
     */
    public float getCultivationBonus() {
        return cultivationBonus;
    }
    
    /**
     * 获取灵力恢复加成倍数
     */
    public float getEnergyRegenBonus() {
        return energyRegenBonus;
    }
    
    /**
     * 检查玩家是否在修炼台附近
     * @param level 世界对象
     * @param player 玩家对象
     * @param range 检查范围
     * @return 是否在修炼台附近
     */
    public static boolean isNearAltar(Level level, Player player, int range) {
        BlockPos playerPos = player.blockPosition();
        
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos checkPos = playerPos.offset(x, y, z);
                    if (level.getBlockState(checkPos).getBlock() instanceof CultivationAltarBlock) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * 获取附近修炼台的总加成
     * @param level 世界对象
     * @param player 玩家对象
     * @param range 检查范围
     * @return 总修炼加成倍数
     */
    public static float getTotalCultivationBonus(Level level, Player player, int range) {
        float totalBonus = 0.0f;
        BlockPos playerPos = player.blockPosition();
        
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos checkPos = playerPos.offset(x, y, z);
                    BlockState state = level.getBlockState(checkPos);
                    if (state.getBlock() instanceof CultivationAltarBlock altar) {
                        totalBonus += altar.getCultivationBonus();
                    }
                }
            }
        }
        
        return totalBonus;
    }
    
    /**
     * 获取附近修炼台的总灵力恢复加成
     * @param level 世界对象
     * @param player 玩家对象
     * @param range 检查范围
     * @return 总灵力恢复加成倍数
     */
    public static float getTotalEnergyRegenBonus(Level level, Player player, int range) {
        float totalBonus = 0.0f;
        BlockPos playerPos = player.blockPosition();
        
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos checkPos = playerPos.offset(x, y, z);
                    BlockState state = level.getBlockState(checkPos);
                    if (state.getBlock() instanceof CultivationAltarBlock altar) {
                        totalBonus += altar.getEnergyRegenBonus();
                    }
                }
            }
        }
        
        return totalBonus;
    }
}