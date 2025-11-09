package org.example.Kangnaixi.tiandao.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.example.Kangnaixi.tiandao.Tiandao;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 修仙能力提供者
 */
public class CultivationProvider implements ICapabilitySerializable<CompoundTag> {
    
    private final CultivationCapability cultivation = new CultivationCapability();
    private final LazyOptional<ICultivation> cultivationOptional = LazyOptional.of(() -> cultivation);
    private final CultivationStorage storage = new CultivationStorage();
    
    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return cap == Tiandao.CULTIVATION_CAPABILITY ? cultivationOptional.cast() : LazyOptional.empty();
    }
    
    @Override
    public CompoundTag serializeNBT() {
        if (Tiandao.CULTIVATION_CAPABILITY == null) {
            return new CompoundTag();
        }
        return storage.writeNBT(cultivation);
    }
    
    @Override
    public void deserializeNBT(CompoundTag nbt) {
        if (Tiandao.CULTIVATION_CAPABILITY != null) {
            storage.readNBT(cultivation, nbt);
        }
    }
}