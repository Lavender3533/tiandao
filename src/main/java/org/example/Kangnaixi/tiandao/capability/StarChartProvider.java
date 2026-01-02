package org.example.Kangnaixi.tiandao.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 星盘数据Provider（附加到玩家）
 * 建议位置: org.example.Kangnaixi.tiandao.capability
 */
public class StarChartProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    private final StarChartCapability data = new StarChartCapability();
    private final LazyOptional<IStarChartData> optional = LazyOptional.of(() -> data);

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == org.example.Kangnaixi.tiandao.Tiandao.STAR_CHART_CAP) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return data.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        data.deserializeNBT(nbt);
    }

    /**
     * 获取内部数据实例（用于网络同步）
     */
    public StarChartCapability getData() {
        return data;
    }

    public void invalidate() {
        optional.invalidate();
    }
}
