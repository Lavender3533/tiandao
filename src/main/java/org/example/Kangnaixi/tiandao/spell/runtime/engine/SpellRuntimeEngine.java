package org.example.Kangnaixi.tiandao.spell.runtime.engine;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.cultivation.ExperienceConversionSystem;
import org.example.Kangnaixi.tiandao.network.CultivationDataSyncPacket;
import org.example.Kangnaixi.tiandao.network.NetworkHandler;
import org.example.Kangnaixi.tiandao.spell.runtime.AttributeType;
import org.example.Kangnaixi.tiandao.spell.runtime.EffectType;
import org.example.Kangnaixi.tiandao.spell.runtime.Spell;
import org.example.Kangnaixi.tiandao.spell.runtime.SpellExecutor;

/**
 * 新版模块化术法运行时引擎。
 */
public final class SpellRuntimeEngine {

    private SpellRuntimeEngine() {}

    public static boolean canCast(ServerPlayer player, Spell spell) {
        // TODO 施法前检查（灵力、冷却、境界等）
        return true;
    }

    public static void execute(ServerPlayer player, Spell spell) {
        if (player == null || spell == null) {
            return;
        }

        try {
            SpellContext ctx = new SpellContext(player.serverLevel(), player, spell);

            SourceExecutors.get(spell.getSource()).apply(ctx);
            CarrierExecutors.get(spell.getCarrier()).apply(ctx);
            FormExecutors.get(spell.getForm()).apply(ctx);

            for (AttributeType attribute : spell.getAttributes()) {
                AttributeExecutors.get(attribute).apply(ctx);
            }
            for (EffectType effect : spell.getEffects()) {
                EffectExecutors.get(effect).apply(ctx);
            }

            Tiandao.LOGGER.debug("Spell [{}] executed by {} -> damage={}, range={}, speed={}",
                spell.getId(), player.getScoreboardName(), ctx.getDamage(), ctx.getRange(), ctx.getSpeed());
        } catch (Exception e) {
            Tiandao.LOGGER.error("术法执行失败: " + spell.getId(), e);
            player.sendSystemMessage(Component.literal("§c术法执行出错: " + e.getMessage()));
        }
    }

    /**
     * 从快捷栏施法。
     */
    public static boolean castFromHotbar(ServerPlayer player) {
        if (player == null) {
            return false;
        }

        return player.getCapability(Tiandao.SPELL_HOTBAR_CAP).map(hotbar -> {
            String spellId = hotbar.getActiveSpellId();
            if (spellId == null || spellId.isEmpty()) {
                player.sendSystemMessage(Component.literal("§c当前槽位没有绑定术法。"));
                return false;
            }

            return player.getCapability(Tiandao.PLAYER_SPELLS_CAP).map(spells -> {
                Spell spell = spells.getSpells().stream()
                    .filter(s -> s.getId().equals(spellId))
                    .findFirst()
                    .orElse(null);

                if (spell == null) {
                    player.sendSystemMessage(Component.literal(
                        "§c未找到术法 " + spellId + " §7(请先学习该术法)"
                    ));
                    return false;
                }

                if (!canCast(player, spell)) {
                    player.sendSystemMessage(Component.literal("§c当前无法施放该术法。"));
                    return false;
                }

                boolean paid = player.getCapability(Tiandao.CULTIVATION_CAPABILITY).map(cultivation -> {
                    double spiritCost = SpellExecutor.estimateSpiritCost(spell);
                    if (cultivation.getSpiritPower() < spiritCost || !cultivation.consumeSpiritPower(spiritCost)) {
                        player.sendSystemMessage(Component.literal(
                            "§c灵力不足！需要 " + String.format("%.1f", spiritCost)
                                + " 当前 " + String.format("%.1f", cultivation.getSpiritPower())
                        ));
                        return false;
                    }
                    ExperienceConversionSystem.onSpiritConsumed(player, spiritCost);
                    NetworkHandler.sendToPlayer(new CultivationDataSyncPacket(cultivation), player);
                    return true;
                }).orElse(false);

                if (!paid) {
                    return false;
                }

                execute(player, spell);
                player.sendSystemMessage(Component.literal("§a施放术法: §e" + spell.getName()));
                Tiandao.LOGGER.info("成功施放术法: {}", spell.getName());
                return true;

            }).orElse(false);

        }).orElse(false);
    }
}
