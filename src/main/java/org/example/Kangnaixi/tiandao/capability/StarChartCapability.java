package org.example.Kangnaixi.tiandao.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.HashSet;
import java.util.Set;

/**
 * 星盘数据实现类
 * 建议位置: org.example.Kangnaixi.tiandao.capability
 */
public class StarChartCapability implements IStarChartData {
    private final Set<String> unlockedNodes = new HashSet<>();

    @Override
    public Set<String> getUnlockedNodes() {
        return new HashSet<>(unlockedNodes);
    }

    @Override
    public boolean unlockNode(String nodeId) {
        return unlockedNodes.add(nodeId);
    }

    @Override
    public boolean isNodeUnlocked(String nodeId) {
        return unlockedNodes.contains(nodeId);
    }

    @Override
    public void clearAll() {
        unlockedNodes.clear();
    }

    /**
     * 序列化到NBT
     */
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        ListTag list = new ListTag();
        for (String nodeId : unlockedNodes) {
            list.add(StringTag.valueOf(nodeId));
        }
        nbt.put("UnlockedNodes", list);
        return nbt;
    }

    /**
     * 从NBT反序列化
     */
    public void deserializeNBT(CompoundTag nbt) {
        unlockedNodes.clear();
        if (nbt.contains("UnlockedNodes", Tag.TAG_LIST)) {
            ListTag list = nbt.getList("UnlockedNodes", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                unlockedNodes.add(list.getString(i));
            }
        }
    }

    /**
     * 复制数据（用于同步）
     */
    public void copyFrom(StarChartCapability other) {
        this.unlockedNodes.clear();
        this.unlockedNodes.addAll(other.unlockedNodes);
    }
}
