package org.example.Kangnaixi.tiandao.spell.runtime.hotbar;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.example.Kangnaixi.tiandao.Tiandao;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 术法快捷栏能力提供者
 * 负责序列化和提供Capability实例
 */
public class SpellHotbarProvider implements ICapabilityProvider, ICapabilitySerializable<CompoundTag> {

    private final SpellHotbarCapability backend = new SpellHotbarCapability();
    private final LazyOptional<ISpellHotbar> optional = LazyOptional.of(() -> backend);

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == Tiandao.SPELL_HOTBAR_CAP) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return backend.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        backend.deserializeNBT(nbt);
    }

    /**
     * 使Capability失效（在玩家移除时调用）
     */
    public void invalidate() {
        optional.invalidate();
    }
}
