package org.example.Kangnaixi.tiandao.spell.runtime;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.example.Kangnaixi.tiandao.Tiandao;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PlayerSpellsProvider implements ICapabilityProvider, ICapabilitySerializable<CompoundTag> {

    private final PlayerSpellsCapability backend = new PlayerSpellsCapability();
    private final LazyOptional<IPlayerSpells> optional = LazyOptional.of(() -> backend);

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return cap == Tiandao.PLAYER_SPELLS_CAP ? optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return backend.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        backend.deserializeNBT(nbt);
    }
}
