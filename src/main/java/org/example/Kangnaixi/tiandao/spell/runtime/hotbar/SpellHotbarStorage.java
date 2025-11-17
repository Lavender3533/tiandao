package org.example.Kangnaixi.tiandao.spell.runtime.hotbar;

import net.minecraft.nbt.CompoundTag;

/**
 * 术法快捷栏存储类
 * 负责序列化和反序列化
 */
public class SpellHotbarStorage {

    private final SpellHotbarCapability capability;

    public SpellHotbarStorage(SpellHotbarCapability capability) {
        this.capability = capability;
    }

    public CompoundTag serializeNBT() {
        return capability.serializeNBT();
    }

    public void deserializeNBT(CompoundTag nbt) {
        capability.deserializeNBT(nbt);
    }
}
