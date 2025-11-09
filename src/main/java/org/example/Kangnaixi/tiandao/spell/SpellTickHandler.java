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
 * 术法Tick处理器
 * 处理持续性术法的效果和冷却时间
 */
@Mod.EventBusSubscriber(modid = Tiandao.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SpellTickHandler {
    
    /**
     * 服务器Tick事件
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        // 只在tick结束时处理
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        
        // 遍历所有玩家
        event.getServer().getAllLevels().forEach(level -> {
            level.players().forEach(playerEntity -> {
                if (playerEntity instanceof ServerPlayer) {
                    ServerPlayer serverPlayer = (ServerPlayer) playerEntity;
                    serverPlayer.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
                        // 处理激活的术法
                        processActiveSpells(serverPlayer, cultivation);
                        
                        // 清理过期的冷却
                        cleanupExpiredCooldowns(cultivation);
                    });
                }
            });
        });
    }
    
    /**
     * 处理激活的持续性术法
     */
    private static void processActiveSpells(ServerPlayer player, ICultivation cultivation) {
        Map<String, Long> activeSpells = cultivation.getActiveSpells();
        long currentTime = System.currentTimeMillis();
        
        // 使用迭代器避免ConcurrentModificationException
        Iterator<Map.Entry<String, Long>> iterator = activeSpells.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            String spellId = entry.getKey();
            Long endTime = entry.getValue();
            
            // 检查是否已过期
            if (endTime <= currentTime) {
                // 术法效果结束
                SpellData spell = SpellRegistry.getInstance().getSpellById(spellId);
                if (spell != null) {
                    spell.onEnd(player, cultivation);
                }
                
                // 从激活列表中移除
                cultivation.deactivateSpell(spellId);
                
                Tiandao.LOGGER.debug("玩家 {} 的术法 {} 效果自然结束",
                    player.getName().getString(), spellId);
            } else {
                // 术法仍在激活中，调用onTick
                SpellData spell = SpellRegistry.getInstance().getSpellById(spellId);
                if (spell != null) {
                    spell.onTick(player, cultivation);
                }
            }
        }
    }
    
    /**
     * 清理已过期的冷却时间
     */
    private static void cleanupExpiredCooldowns(ICultivation cultivation) {
        Map<String, Long> cooldowns = cultivation.getSpellCooldowns();
        long currentTime = System.currentTimeMillis();
        
        // 清理过期的冷却
        cooldowns.entrySet().removeIf(entry -> entry.getValue() <= currentTime);
    }
}

