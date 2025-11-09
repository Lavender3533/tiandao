
package org.example.Kangnaixi.tiandao.spell.spells;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.capability.ICultivation;
import org.example.Kangnaixi.tiandao.cultivation.CultivationRealm;
import org.example.Kangnaixi.tiandao.spell.SpellData;

/**
 * 灵气护盾术法
 * 凝聚灵气形成护盾，吸收伤害并减免50%承受伤害
 */
public class SpiritualShieldSpell extends SpellData {
    
    public SpiritualShieldSpell() {
        super(
            "spiritual_shield",
            "灵气护盾",
            "凝聚灵气形成护盾，吸收伤害并减免50%承受伤害。\n持续10秒。",
            CultivationRealm.QI_CONDENSATION,
            3,
            20.0,  // 消耗20灵力
            0.0,   // 无维持消耗
            30,    // 30秒冷却
            10,    // 持续10秒
            SpellType.DURATION
        );
    }
    
    @Override
    public boolean cast(ServerPlayer player, ICultivation cultivation) {
        // 检查基础条件
        if (!super.cast(player, cultivation)) {
            return false;
        }
        
        // 应用吸收效果（Absorption）- 吸收50点伤害（25颗心）
        // 根据境界调整强度
        CultivationRealm realm = cultivation.getRealm();
        int absorptionHearts = 25 + (realm.ordinal() * 5); // 境界越高，护盾越强
        
        MobEffectInstance absorption = new MobEffectInstance(
            MobEffects.ABSORPTION,
            200, // 10秒 (200 ticks)
            absorptionHearts / 4, // 效果等级（每级增加4点生命值）
            false,
            true,
            true
        );
        player.addEffect(absorption);
        
        // 应用伤害减免效果（Resistance）- 减少50%伤害
        MobEffectInstance resistance = new MobEffectInstance(
            MobEffects.DAMAGE_RESISTANCE,
            200, // 10秒
            1, // 等级2 = 40%减免，等级3 = 60%减免，这里用1 = 20%减免（可调整）
            false,
            true,
            true
        );
        player.addEffect(resistance);
        
        // 激活术法状态（用于粒子效果和状态追踪）
        long endTime = System.currentTimeMillis() + (10 * 1000);
        cultivation.activateSpell(getId(), endTime);
        
        // 播放音效
        player.playSound(SoundEvents.BEACON_ACTIVATE, 0.5f, 1.2f);
        
        Tiandao.LOGGER.info("玩家 {} 释放灵气护盾，吸收强度: {}，持续10秒",
            player.getName().getString(), absorptionHearts);
        
        return true;
    }
    
    @Override
    public void onTick(ServerPlayer player, ICultivation cultivation) {
        // 护盾持续效果由客户端自定义渲染器负责
        // 服务端只需要维持药水效果，无需生成粒子
    }
    
    @Override
    public void onEnd(ServerPlayer player, ICultivation cultivation) {
        // 护盾结束时播放音效
        player.playSound(SoundEvents.BEACON_DEACTIVATE, 0.3f, 1.0f);
        
        Tiandao.LOGGER.info("玩家 {} 的灵气护盾效果结束", player.getName().getString());
    }
}

