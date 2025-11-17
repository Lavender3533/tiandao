package org.example.Kangnaixi.tiandao.spell;

import net.minecraft.server.level.ServerPlayer;
import org.example.Kangnaixi.tiandao.capability.ICultivation;
import java.util.List;
import java.util.Map;

/**
 * 术法执行上下文
 * 包含施法时的所有状态信息和计算后的属性
 */
public class SpellExecutionContext {

    private final ServerPlayer player;
    private final ICultivation cultivation;
    private final ModularSpell spell;

    // 计算后的属性
    private final double finalDamage;
    private final double finalSpeed;
    private final double finalRange;

    // 强化状态
    private final boolean swordEnhanced;

    public SpellExecutionContext(ServerPlayer player, ICultivation cultivation,
                               ModularSpell spell, double finalDamage,
                               double finalSpeed, double finalRange, boolean swordEnhanced) {
        this.player = player;
        this.cultivation = cultivation;
        this.spell = spell;
        this.finalDamage = finalDamage;
        this.finalSpeed = finalSpeed;
        this.finalRange = finalRange;
        this.swordEnhanced = swordEnhanced;
    }

    // Getter方法
    public ServerPlayer getPlayer() {
        return player;
    }

    public ICultivation getCultivation() {
        return cultivation;
    }

    public ModularSpell getSpell() {
        return spell;
    }

    public double getDamage() {
        return finalDamage;
    }

    public double getSpeed() {
        return finalSpeed;
    }

    public double getRange() {
        return finalRange;
    }

    public boolean isSwordEnhanced() {
        return swordEnhanced;
    }

    /**
     * 获取属性加成
     */
    public double getAttributeBonus(String attributeId) {
        return spell.getAttributes().getAttributeBonus(attributeId);
    }

    /**
     * 检查是否具有特定效果
     */
    public boolean hasEffect(String effectId) {
        return spell.getEffects().hasEffect(effectId);
    }

    /**
     * 获取效果强度
     */
    public double getEffectPower(String effectId) {
        return spell.getEffects().getEffectPower(effectId);
    }
}