package org.example.Kangnaixi.tiandao.cultivation;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * 功法检查类
 * 用于检查玩家是否装备了基础功法
 */
public class TechniqueChecker {
    
    /**
     * 检查玩家是否装备了基础功法
     * @param player 玩家对象
     * @return 是否装备了基础功法
     */
    public static boolean hasBasicTechniqueEquipped(Player player) {
        // 检查主手和副手
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        
        if (isBasicTechnique(mainHand) || isBasicTechnique(offHand)) {
            return true;
        }
        
        // 检查盔甲槽
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ARMOR) {
                ItemStack armor = player.getItemBySlot(slot);
                if (isBasicTechnique(armor)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * 检查物品是否是基础功法
     * @param stack 物品堆
     * @return 是否是基础功法
     */
    private static boolean isBasicTechnique(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        
        // 检查物品的NBT标签是否有"technique"标签
        if (stack.hasTag() && stack.getTag().contains("technique")) {
            String techniqueType = stack.getTag().getString("technique");
            // 检查是否是基础功法类型
            return "basic".equals(techniqueType) || "foundation".equals(techniqueType);
        }
        
        // 检查物品ID是否包含功法相关关键词
        String itemId = stack.getItem().getDescriptionId().toLowerCase();
        return itemId.contains("technique") || itemId.contains("cultivation") || 
               itemId.contains("manual") || itemId.contains("scripture");
    }
    
    /**
     * 获取玩家装备的功法等级（预留接口）
     * @param player 玩家对象
     * @return 功法等级，未装备返回0
     */
    public static int getTechniqueLevel(Player player) {
        // 检查主手和副手
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        
        int level = getTechniqueLevel(mainHand);
        if (level > 0) return level;
        
        level = getTechniqueLevel(offHand);
        if (level > 0) return level;
        
        // 检查盔甲槽
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ARMOR) {
                ItemStack armor = player.getItemBySlot(slot);
                level = getTechniqueLevel(armor);
                if (level > 0) return level;
            }
        }
        
        return 0;
    }
    
    /**
     * 获取物品的功法等级
     * @param stack 物品堆
     * @return 功法等级
     */
    private static int getTechniqueLevel(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) {
            return 0;
        }
        
        if (stack.getTag().contains("technique_level")) {
            return stack.getTag().getInt("technique_level");
        }
        
        return 0;
    }
}