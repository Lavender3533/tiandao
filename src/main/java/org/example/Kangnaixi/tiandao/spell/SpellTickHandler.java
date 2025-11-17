package org.example.Kangnaixi.tiandao.spell;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.capability.ICultivation;

import java.util.Iterator;
import java.util.Map;

/**
 * 负责在服务器 Tick 中清理术法冷却与持续状态。
 */
@Mod.EventBusSubscriber(modid = Tiandao.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SpellTickHandler {

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        event.getServer().getAllLevels().forEach(level -> {
            level.players().forEach(serverPlayer ->
                serverPlayer.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
                    processActiveSpells(serverPlayer, cultivation);
                    cleanupExpiredCooldowns(cultivation);
                })
            );
        });
    }

    private static void processActiveSpells(ServerPlayer player, ICultivation cultivation) {
        Map<String, Long> activeSpells = cultivation.getActiveSpells();
        long currentTime = System.currentTimeMillis();

        Iterator<Map.Entry<String, Long>> iterator = activeSpells.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            if (entry.getValue() <= currentTime) {
                String spellId = entry.getKey();
                cultivation.deactivateSpell(spellId);
                Tiandao.LOGGER.debug("玩家 {} 的术法 {} 效果结束", player.getName().getString(), spellId);
            }
        }
    }

    private static void cleanupExpiredCooldowns(ICultivation cultivation) {
        Map<String, Long> cooldowns = cultivation.getSpellCooldowns();
        long currentTime = System.currentTimeMillis();
        cooldowns.entrySet().removeIf(entry -> entry.getValue() <= currentTime);
    }
}
