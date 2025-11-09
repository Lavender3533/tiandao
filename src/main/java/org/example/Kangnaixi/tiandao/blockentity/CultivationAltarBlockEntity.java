package org.example.Kangnaixi.tiandao.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.example.Kangnaixi.tiandao.Tiandao;

/**
 * 修炼台方块实体类
 */
public class CultivationAltarBlockEntity extends BlockEntity {
    public CultivationAltarBlockEntity(BlockPos pos, BlockState state) {
        super(Tiandao.CULTIVATION_ALTAR_BLOCK_ENTITY.get(), pos, state);
    }
    
    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
    }
    
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
    }
    
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }
    
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}