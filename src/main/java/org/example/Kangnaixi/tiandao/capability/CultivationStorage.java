package org.example.Kangnaixi.tiandao.capability;

import net.minecraft.nbt.CompoundTag;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.cultivation.SpiritualRootType;
import org.example.Kangnaixi.tiandao.cultivation.SpiritualRoot;
import org.example.Kangnaixi.tiandao.cultivation.SpiritualRootQuality;
import org.example.Kangnaixi.tiandao.cultivation.CultivationRealm;
import org.example.Kangnaixi.tiandao.cultivation.SubRealm;
import javax.annotation.Nullable;

/**
 * 修仙能力存储
 */
public class CultivationStorage {
    
    @Nullable
    public CompoundTag writeNBT(CultivationCapability instance) {
        CompoundTag tag = new CompoundTag();
        
        // 保存灵根对象
        SpiritualRoot spiritualRoot = instance.getSpiritualRootObject();
        if (spiritualRoot != null) {
            tag.putString("spiritualRoot", spiritualRoot.getType().toString());
            tag.putString("spiritualRootQuality", spiritualRoot.getQuality().toString());
        }
        
        // 保存境界和等级
        tag.putString("realm", instance.getRealm().toString());
        tag.putInt("level", instance.getLevel()); // 保留用于兼容性
        
        // 保存小境界
        tag.putString("subRealm", instance.getSubRealm().getId());
        
        // 保存根基值
        tag.putInt("foundation", instance.getFoundation());
        
        // 保存legacyLevel（用于数据迁移备份）
        if (instance.getLevel() > 0) {
            tag.putInt("legacyLevel", instance.getLevel());
        }
        
        // 保存时间加速倍数
        tag.putDouble("timeAcceleration", instance.getTimeAcceleration());
        
        // 保存打坐开始时间
        tag.putLong("practiceStartTime", instance.getPracticeStartTime());
        
        // 保存修炼进度
        tag.putDouble("cultivationProgress", instance.getCultivationProgress());
        
        // 保存灵力
        double spiritPower = instance.getSpiritPower();
        double maxSpiritPower = instance.getMaxSpiritPower();
        tag.putDouble("spiritPower", spiritPower);
        tag.putDouble("maxSpiritPower", maxSpiritPower);
        
        // 保存已分配标记
        tag.putBoolean("rootAssigned", instance.hasRootAssigned());
        
        // 保存修炼系统数据
        tag.putBoolean("practicing", instance.isPracticing());
        tag.putString("currentPracticeMethod", instance.getCurrentPracticeMethod());
        tag.putInt("cultivationExperience", instance.getCultivationExperience());
        tag.putLong("lastCombatTime", instance.getLastCombatTime());
        
        // 保存功法系统数据
        // 保存已学习的功法列表
        net.minecraft.nbt.ListTag learnedTechniquesList = new net.minecraft.nbt.ListTag();
        for (org.example.Kangnaixi.tiandao.technique.TechniqueData technique : instance.getLearnedTechniques()) {
            learnedTechniquesList.add(technique.toNBT());
        }
        tag.put("learnedTechniques", learnedTechniquesList);
        
        // 保存当前装备的功法
        if (instance.getEquippedTechnique() != null) {
            tag.put("equippedTechnique", instance.getEquippedTechnique().toNBT());
            tag.putLong("techniqueEquipTime", instance.getTechniqueEquipTime());
        }
        
        // 保存术法系统数据
        // 保存已解锁的术法列表
        net.minecraft.nbt.ListTag unlockedSpellsList = new net.minecraft.nbt.ListTag();
        for (String spellId : instance.getUnlockedSpells()) {
            CompoundTag spellTag = new CompoundTag();
            spellTag.putString("id", spellId);
            unlockedSpellsList.add(spellTag);
        }
        tag.put("unlockedSpells", unlockedSpellsList);
        
        // 保存术法快捷栏
        net.minecraft.nbt.ListTag spellHotbarList = new net.minecraft.nbt.ListTag();
        String[] hotbar = instance.getSpellHotbar();
        for (int i = 0; i < hotbar.length; i++) {
            CompoundTag slotTag = new CompoundTag();
            slotTag.putInt("slot", i);
            if (hotbar[i] != null) {
                slotTag.putString("spellId", hotbar[i]);
            }
            spellHotbarList.add(slotTag);
        }
        tag.put("spellHotbar", spellHotbarList);
        
        // 保存术法冷却时间
        CompoundTag cooldownsTag = new CompoundTag();
        for (java.util.Map.Entry<String, Long> entry : instance.getSpellCooldowns().entrySet()) {
            cooldownsTag.putLong(entry.getKey(), entry.getValue());
        }
        tag.put("spellCooldowns", cooldownsTag);
        
        // 保存激活的持续性术法
        CompoundTag activeSpellsTag = new CompoundTag();
        for (java.util.Map.Entry<String, Long> entry : instance.getActiveSpells().entrySet()) {
            activeSpellsTag.putLong(entry.getKey(), entry.getValue());
        }
        tag.put("activeSpells", activeSpellsTag);
        
        Tiandao.LOGGER.info("保存修仙数据到NBT: 灵力={}/{}, 境界={}, 已分配灵根={}, 修炼经验={}, 已学功法数={}, 已解锁术法数={}", 
            spiritPower, maxSpiritPower, instance.getRealm(), instance.hasRootAssigned(), 
            instance.getCultivationExperience(), instance.getLearnedTechniques().size(), instance.getUnlockedSpells().size());
        
        return tag;
    }
    
    public void readNBT(CultivationCapability instance, CompoundTag nbt) {
        Tiandao.LOGGER.info("开始从NBT读取修仙数据，NBT内容: {}", nbt.toString());
        
        // 读取灵根对象
        if (nbt.contains("spiritualRoot")) {
            SpiritualRootType type = SpiritualRootType.valueOf(nbt.getString("spiritualRoot"));
            
            // 如果有品质数据，则创建完整的灵根对象
            if (nbt.contains("spiritualRootQuality")) {
                SpiritualRootQuality quality = SpiritualRootQuality.valueOf(nbt.getString("spiritualRootQuality"));
                SpiritualRoot spiritualRoot = new SpiritualRoot(type, quality);
                instance.setSpiritualRootObject(spiritualRoot);
            } else {
                // 兼容旧数据，只设置类型
                instance.setSpiritualRoot(type);
            }
        }
        
        // 读取境界和等级
        boolean needsMigration = false;
        if (nbt.contains("realm")) {
            instance.setRealm(CultivationRealm.valueOf(nbt.getString("realm")));
        }
        
        // 数据迁移：从旧等级系统迁移到新小境界系统
        if (nbt.contains("level") && !nbt.contains("subRealm")) {
            // 旧数据格式，需要迁移
            int oldLevel = nbt.getInt("level");
            needsMigration = true;
            
            // 迁移等级到小境界
            SubRealm migratedSubRealm = SubRealm.fromLegacyLevel(oldLevel);
            instance.setSubRealm(migratedSubRealm);
            
            // 保存legacyLevel作为备份
            instance.setLevel(oldLevel);
            
            Tiandao.LOGGER.info("数据迁移: 玩家等级 {} 迁移到小境界 {}", oldLevel, migratedSubRealm.getDisplayName());
        } else if (nbt.contains("subRealm")) {
            // 新数据格式，直接读取小境界
            String subRealmId = nbt.getString("subRealm");
            instance.setSubRealm(SubRealm.fromId(subRealmId));
        } else {
            // 没有小境界数据，使用默认值
            instance.setSubRealm(SubRealm.EARLY);
        }
        
        // 读取legacyLevel（备份）
        if (nbt.contains("legacyLevel")) {
            instance.setLevel(nbt.getInt("legacyLevel"));
        } else if (nbt.contains("level")) {
            instance.setLevel(nbt.getInt("level"));
        }
        
        // 读取根基值
        if (nbt.contains("foundation")) {
            instance.setFoundation(nbt.getInt("foundation"));
        } else {
            // 首次获得灵根时初始化为100
            if (instance.hasRootAssigned() && instance.getFoundation() == 0) {
                instance.setFoundation(100);
            }
        }
        
        // 读取时间加速倍数
        if (nbt.contains("timeAcceleration")) {
            instance.setTimeAcceleration(nbt.getDouble("timeAcceleration"));
        } else {
            instance.setTimeAcceleration(1.0);
        }
        
        // 读取打坐开始时间
        if (nbt.contains("practiceStartTime")) {
            instance.setPracticeStartTime(nbt.getLong("practiceStartTime"));
        } else {
            instance.setPracticeStartTime(0);
        }
        
        if (needsMigration) {
            Tiandao.LOGGER.info("数据迁移完成: 境界={}, 小境界={}, 根基值={}", 
                instance.getRealm().getDisplayName(), 
                instance.getSubRealm().getDisplayName(),
                instance.getFoundation());
        }
        
        // 读取修炼进度
        if (nbt.contains("cultivationProgress")) {
            instance.setCultivationProgress(nbt.getDouble("cultivationProgress"));
        }
        
        // 读取灵力 - 重要：必须先设置maxSpiritPower，再设置spiritPower
        // 因为setSpiritPower会将值限制在maxSpiritPower范围内
        double spiritPower = 100.0; // 默认值
        double maxSpiritPower = 100.0; // 默认值
        
        // 先读取并设置最大灵力
        if (nbt.contains("maxSpiritPower")) {
            maxSpiritPower = nbt.getDouble("maxSpiritPower");
            instance.setMaxSpiritPower(maxSpiritPower);
        }
        
        // 再读取并设置当前灵力（这样才不会被clamp）
        if (nbt.contains("spiritPower")) {
            spiritPower = nbt.getDouble("spiritPower");
            instance.setSpiritPower(spiritPower);
        }
        
        // 读取已分配标记
        if (nbt.contains("rootAssigned")) {
            instance.setRootAssigned(nbt.getBoolean("rootAssigned"));
        }
        
        // 读取修炼系统数据
        if (nbt.contains("practicing")) {
            instance.setPracticing(nbt.getBoolean("practicing"));
        }
        if (nbt.contains("currentPracticeMethod")) {
            instance.setCurrentPracticeMethod(nbt.getString("currentPracticeMethod"));
        }
        if (nbt.contains("cultivationExperience")) {
            instance.setCultivationExperience(nbt.getInt("cultivationExperience"));
        }
        if (nbt.contains("lastCombatTime")) {
            instance.setLastCombatTime(nbt.getLong("lastCombatTime"));
        }
        
        // 读取功法系统数据
        // 读取已学习的功法列表
        if (nbt.contains("learnedTechniques")) {
            net.minecraft.nbt.ListTag learnedTechniquesList = nbt.getList("learnedTechniques", 10); // 10 = CompoundTag
            for (int i = 0; i < learnedTechniquesList.size(); i++) {
                CompoundTag techniqueTag = learnedTechniquesList.getCompound(i);
                org.example.Kangnaixi.tiandao.technique.TechniqueData technique = 
                    org.example.Kangnaixi.tiandao.technique.TechniqueData.fromNBT(techniqueTag);
                instance.learnTechnique(technique);
            }
        }
        
        // 读取当前装备的功法
        if (nbt.contains("equippedTechnique")) {
            CompoundTag equippedTag = nbt.getCompound("equippedTechnique");
            org.example.Kangnaixi.tiandao.technique.TechniqueData technique = 
                org.example.Kangnaixi.tiandao.technique.TechniqueData.fromNBT(equippedTag);
            
            // 先学习功法（如果还没学习），然后装备
            if (!instance.hasTechnique(technique.getId())) {
                instance.learnTechnique(technique);
            }
            instance.equipTechnique(technique.getId());
            
            // 恢复装备时间
            if (nbt.contains("techniqueEquipTime")) {
                instance.setTechniqueEquipTime(nbt.getLong("techniqueEquipTime"));
            }
        }
        
        // 读取术法系统数据
        // 读取已解锁的术法列表
        if (nbt.contains("unlockedSpells")) {
            net.minecraft.nbt.ListTag unlockedSpellsList = nbt.getList("unlockedSpells", 10); // 10 = CompoundTag
            for (int i = 0; i < unlockedSpellsList.size(); i++) {
                CompoundTag spellTag = unlockedSpellsList.getCompound(i);
                String spellId = spellTag.getString("id");
                instance.unlockSpell(spellId);
            }
        }
        
        // 读取术法快捷栏
        if (nbt.contains("spellHotbar")) {
            net.minecraft.nbt.ListTag spellHotbarList = nbt.getList("spellHotbar", 10); // 10 = CompoundTag
            for (int i = 0; i < spellHotbarList.size(); i++) {
                CompoundTag slotTag = spellHotbarList.getCompound(i);
                int slot = slotTag.getInt("slot");
                String spellId = slotTag.contains("spellId") ? slotTag.getString("spellId") : null;
                instance.setSpellHotbar(slot, spellId);
            }
        }
        
        // 读取术法冷却时间
        if (nbt.contains("spellCooldowns")) {
            CompoundTag cooldownsTag = nbt.getCompound("spellCooldowns");
            for (String key : cooldownsTag.getAllKeys()) {
                long endTime = cooldownsTag.getLong(key);
                instance.setSpellCooldown(key, endTime);
        }
        }
        
        // 读取激活的持续性术法
        if (nbt.contains("activeSpells")) {
            CompoundTag activeSpellsTag = nbt.getCompound("activeSpells");
            for (String key : activeSpellsTag.getAllKeys()) {
                long endTime = activeSpellsTag.getLong(key);
                instance.activateSpell(key, endTime);
            }
        }
        
        Tiandao.LOGGER.info("从NBT加载修仙数据完成: 灵力={}/{}, 境界={}, 修炼经验={}, 已学功法数={}, 已解锁术法数={}, 实例={}", 
            spiritPower, maxSpiritPower, instance.getRealm(), instance.getCultivationExperience(), 
            instance.getLearnedTechniques().size(), instance.getUnlockedSpells().size(), System.identityHashCode(instance));
    }
}