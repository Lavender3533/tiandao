package org.example.Kangnaixi.tiandao.spell.runtime.hotbar;

import net.minecraft.nbt.CompoundTag;

/**
 * 术法快捷栏能力实现
 * 管理9个技能槽位和当前激活的槽位
 */
public class SpellHotbarCapability implements ISpellHotbar {

    private static final int HOTBAR_SIZE = 9;

    // 9个槽位，存储术法ID（null表示空槽位）
    private final String[] slots = new String[HOTBAR_SIZE];

    // 当前激活的槽位索引（0~8）
    private int activeIndex = 0;

    @Override
    public String[] getSlots() {
        return slots.clone();
    }

    @Override
    public String getSlot(int index) {
        if (index < 0 || index >= HOTBAR_SIZE) {
            return null;
        }
        return slots[index];
    }

    @Override
    public void setSlot(int index, String spellId) {
        if (index >= 0 && index < HOTBAR_SIZE) {
            slots[index] = spellId;
        }
    }

    @Override
    public int getActiveIndex() {
        return activeIndex;
    }

    @Override
    public void setActiveIndex(int index) {
        if (index >= 0 && index < HOTBAR_SIZE) {
            this.activeIndex = index;
        }
    }

    @Override
    public String getActiveSpellId() {
        return slots[activeIndex];
    }

    @Override
    public void clearSlot(int index) {
        if (index >= 0 && index < HOTBAR_SIZE) {
            slots[index] = null;
        }
    }

    @Override
    public void clearAll() {
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            slots[i] = null;
        }
        activeIndex = 0;
    }

    @Override
    public void copyFrom(ISpellHotbar other) {
        String[] otherSlots = other.getSlots();
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            this.slots[i] = otherSlots[i];
        }
        this.activeIndex = other.getActiveIndex();
    }

    /**
     * 序列化到NBT
     */
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();

        // 保存激活索引
        tag.putInt("ActiveIndex", activeIndex);

        // 保存所有槽位
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            if (slots[i] != null && !slots[i].isEmpty()) {
                tag.putString("Slot" + i, slots[i]);
            }
        }

        return tag;
    }

    /**
     * 从NBT反序列化
     */
    public void deserializeNBT(CompoundTag tag) {
        // 清空现有数据
        clearAll();

        // 读取激活索引
        if (tag.contains("ActiveIndex")) {
            activeIndex = tag.getInt("ActiveIndex");
            // 确保索引在有效范围内
            if (activeIndex < 0 || activeIndex >= HOTBAR_SIZE) {
                activeIndex = 0;
            }
        }

        // 读取所有槽位
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            String key = "Slot" + i;
            if (tag.contains(key)) {
                slots[i] = tag.getString(key);
            }
        }
    }
}
