package org.example.Kangnaixi.tiandao.handwheel;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.example.Kangnaixi.tiandao.starchart.StarNode;
import org.example.Kangnaixi.tiandao.starchart.StarNodeCategory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 手盘组合数据 - 包含3个核心槽位 + 多个调制槽位
 *
 * 槽位结构（星盘关联）：
 * - EFFECT: 效果槽位（必填）- 伤害/点燃/冰冻/治疗等
 * - FORM: 形态槽位（必填）- 球体/光束/场域等
 * - MODIFIER: 调制槽位（可选，最多3个）- 强度+/距离+等
 *
 * 注：源（灵力来源）由手盘中心独立选择，不在组合中
 */
public class HandWheelCombination {
    private static final int MAX_MODIFIERS = 3;

    private final HandWheelSlot effectSlot;
    private final HandWheelSlot formSlot;
    private final List<HandWheelSlot> modifierSlots;

    public HandWheelCombination() {
        this.effectSlot = new HandWheelSlot(StarNodeCategory.EFFECT);
        this.formSlot = new HandWheelSlot(StarNodeCategory.FORM);
        this.modifierSlots = new ArrayList<>();
        for (int i = 0; i < MAX_MODIFIERS; i++) {
            modifierSlots.add(new HandWheelSlot(StarNodeCategory.MODIFIER));
        }
    }

    // ========== 槽位访问 ==========

    public HandWheelSlot getEffectSlot() {
        return effectSlot;
    }

    public HandWheelSlot getFormSlot() {
        return formSlot;
    }

    public List<HandWheelSlot> getModifierSlots() {
        return modifierSlots;
    }

    public HandWheelSlot getModifierSlot(int index) {
        if (index < 0 || index >= modifierSlots.size()) return null;
        return modifierSlots.get(index);
    }

    /**
     * 根据类别获取对应槽位
     */
    @Nullable
    public HandWheelSlot getSlotByCategory(StarNodeCategory category) {
        return switch (category) {
            case EFFECT -> effectSlot;
            case FORM -> formSlot;
            case MODIFIER -> getFirstEmptyModifierSlot();
            case BLUEPRINT -> null;
        };
    }

    @Nullable
    private HandWheelSlot getFirstEmptyModifierSlot() {
        for (HandWheelSlot slot : modifierSlots) {
            if (slot.isEmpty()) return slot;
        }
        return modifierSlots.isEmpty() ? null : modifierSlots.get(0);
    }

    // ========== 组合操作 ==========

    /**
     * 设置指定类别的节点
     * @return true if set successfully
     */
    public boolean setSlot(StarNodeCategory category, String nodeId) {
        HandWheelSlot slot = getSlotByCategory(category);
        if (slot == null) return false;
        return slot.setNode(nodeId);
    }

    /**
     * 清空指定类别的槽位
     */
    public void clearSlot(StarNodeCategory category) {
        if (category == StarNodeCategory.MODIFIER) {
            modifierSlots.forEach(HandWheelSlot::clear);
        } else {
            HandWheelSlot slot = getSlotByCategory(category);
            if (slot != null) slot.clear();
        }
    }

    /**
     * 清空所有槽位
     */
    public void clearAll() {
        effectSlot.clear();
        formSlot.clear();
        modifierSlots.forEach(HandWheelSlot::clear);
    }

    // ========== 验证 ==========

    /**
     * 验证组合是否完整有效
     * 现在只需要效果和形态两个必填槽位
     */
    public ValidationResult validate() {
        if (effectSlot.isEmpty()) {
            return new ValidationResult(false, "缺少效果节点");
        }
        if (formSlot.isEmpty()) {
            return new ValidationResult(false, "缺少形态节点");
        }
        return new ValidationResult(true, "组合有效");
    }

    public boolean isValid() {
        return validate().isValid();
    }

    /**
     * 获取当前已填充的增强节点列表
     */
    public List<StarNode> getActiveModifiers() {
        List<StarNode> result = new ArrayList<>();
        for (HandWheelSlot slot : modifierSlots) {
            StarNode node = slot.getNode();
            if (node != null) {
                result.add(node);
            }
        }
        return result;
    }

    /**
     * 获取所有已填充的节点
     */
    public Map<StarNodeCategory, StarNode> getAllNodes() {
        Map<StarNodeCategory, StarNode> result = new EnumMap<>(StarNodeCategory.class);
        if (effectSlot.getNode() != null) result.put(StarNodeCategory.EFFECT, effectSlot.getNode());
        if (formSlot.getNode() != null) result.put(StarNodeCategory.FORM, formSlot.getNode());
        return result;
    }

    // ========== 序列化 ==========

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.put("effect", effectSlot.serializeNBT());
        tag.put("form", formSlot.serializeNBT());

        ListTag modList = new ListTag();
        for (HandWheelSlot slot : modifierSlots) {
            modList.add(slot.serializeNBT());
        }
        tag.put("modifiers", modList);

        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        if (tag.contains("effect")) effectSlot.deserializeNBT(tag.getCompound("effect"));
        if (tag.contains("form")) formSlot.deserializeNBT(tag.getCompound("form"));

        if (tag.contains("modifiers", Tag.TAG_LIST)) {
            ListTag modList = tag.getList("modifiers", Tag.TAG_COMPOUND);
            for (int i = 0; i < modList.size() && i < modifierSlots.size(); i++) {
                modifierSlots.get(i).deserializeNBT(modList.getCompound(i));
            }
        }
    }

    // ========== 内部类 ==========

    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }

    @Override
    public String toString() {
        return String.format("HandWheelCombination[%s + %s + %d mods]",
            effectSlot, formSlot,
            getActiveModifiers().size());
    }
}
