package org.example.Kangnaixi.tiandao.cultivation;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.capability.ICultivation;
import org.example.Kangnaixi.tiandao.network.CultivationDataSyncPacket;
import org.example.Kangnaixi.tiandao.network.NetworkHandler;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 统一的境界突破辅助工具。
 * 负责在经验满足条件时执行突破与提示，并限制失败提示的频率。
 */
public final class BreakthroughHelper {

    private static final long FAILURE_COOLDOWN_TICKS = 200; // 约10秒
    private static final Map<UUID, Long> LAST_FAILURE_TICK = new ConcurrentHashMap<>();

    private BreakthroughHelper() {
    }

    /**
     * 检查并尝试执行自动突破。
     *
     * @return true 表示已经完成突破。
     */
    public static boolean attemptBreakthrough(ServerPlayer player, ICultivation cultivation) {
        int requiredExp = cultivation.getRequiredExperienceForSubRealm();
        if (requiredExp <= 0) {
            return false;
        }

        int currentExp = cultivation.getCultivationExperience();
        if (currentExp < requiredExp) {
            return false;
        }

        SubRealm oldSubRealm = cultivation.getSubRealm();
        CultivationRealm oldRealm = cultivation.getRealm();
        double oldMaxSpiritPower = cultivation.getMaxSpiritPower();

        boolean success = cultivation.tryBreakthrough();

        if (success) {
            SubRealm newSubRealm = cultivation.getSubRealm();
            CultivationRealm newRealm = cultivation.getRealm();

            if (newRealm == oldRealm && newSubRealm != oldSubRealm) {
                sendSmallRealmSuccess(player, newRealm, newSubRealm);
            } else if (newRealm != oldRealm) {
                sendMajorRealmSuccess(player, oldRealm, oldSubRealm, newRealm, newSubRealm, oldMaxSpiritPower,
                    cultivation.getMaxSpiritPower());
            }

            LAST_FAILURE_TICK.remove(player.getUUID());
            NetworkHandler.sendToPlayer(new CultivationDataSyncPacket(cultivation), player);
            return true;
        }

        maybeNotifyFailure(player, cultivation, oldRealm, currentExp, requiredExp);
        NetworkHandler.sendToPlayer(new CultivationDataSyncPacket(cultivation), player);
        return false;
    }

    private static void sendSmallRealmSuccess(ServerPlayer player, CultivationRealm realm, SubRealm subRealm) {
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§a§l━━━ 小境界突破 ━━━"));
        player.sendSystemMessage(Component.literal("§e突破至 §b" + realm.getDisplayName() + " " + subRealm.getDisplayName()));
        player.sendSystemMessage(Component.literal("§a§l━━━━━━━━━━━━━━"));
        player.sendSystemMessage(Component.literal(""));

        Tiandao.LOGGER.info("玩家 {} 小境界突破 -> {} {}", player.getName().getString(),
            realm.getDisplayName(), subRealm.getDisplayName());
    }

    private static void sendMajorRealmSuccess(ServerPlayer player,
                                              CultivationRealm oldRealm,
                                              SubRealm oldSubRealm,
                                              CultivationRealm newRealm,
                                              SubRealm newSubRealm,
                                              double oldMaxSpiritPower,
                                              double newMaxSpiritPower) {
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§6§l━━━ 大境界突破 ━━━"));
        player.sendSystemMessage(Component.literal("§e突破至 §b" + newRealm.getDisplayName() + " " + newSubRealm.getDisplayName()));
        player.sendSystemMessage(Component.literal("§7境界: " + oldRealm.getDisplayName() + " " + oldSubRealm.getDisplayName()
            + " §f→ §b" + newRealm.getDisplayName() + " " + newSubRealm.getDisplayName()));
        player.sendSystemMessage(Component.literal("§7最大灵力: " + String.format("%.0f", oldMaxSpiritPower)
            + " §f→ §a" + String.format("%.0f", newMaxSpiritPower)));
        player.sendSystemMessage(Component.literal("§6§l━━━━━━━━━━━━━━"));
        player.sendSystemMessage(Component.literal(""));

        Tiandao.LOGGER.info("玩家 {} 大境界突破 -> {} {}", player.getName().getString(),
            newRealm.getDisplayName(), newSubRealm.getDisplayName());
    }

    private static void maybeNotifyFailure(ServerPlayer player, ICultivation cultivation,
                                           CultivationRealm realm, int currentExp, int requiredExp) {
        long currentTick = player.level().getGameTime();
        UUID uuid = player.getUUID();
        long lastTick = LAST_FAILURE_TICK.getOrDefault(uuid, Long.MIN_VALUE);
        if (currentTick - lastTick < FAILURE_COOLDOWN_TICKS) {
            return;
        }

        LAST_FAILURE_TICK.put(uuid, currentTick);

        int foundation = cultivation.getFoundation();
        SubRealm currentSubRealm = cultivation.getSubRealm();
        SubRealm nextSubRealm = currentSubRealm.getNext();

        if (nextSubRealm != null) {
            if (foundation < 50) {
                player.sendSystemMessage(Component.literal("§c根基不稳，无法突破小境界！根基值: " + foundation + "/50"));
                Tiandao.LOGGER.warn("玩家 {} 突破失败：根基不足 (<50)，经验 {}/{}", player.getName().getString(), currentExp, requiredExp);
                return;
            }
        } else {
            CultivationRealm nextRealm = realm.getNext();
            if (nextRealm != null) {
                if (foundation < 30) {
                    player.sendSystemMessage(Component.literal("§c根基严重受损，无法突破大境界！根基值: " + foundation + "/30"));
                    Tiandao.LOGGER.warn("玩家 {} 突破失败：根基不足 (<30)，经验 {}/{}", player.getName().getString(), currentExp, requiredExp);
                    return;
                }
            } else {
                player.sendSystemMessage(Component.literal("§7已达到最高境界，无需突破。"));
                return;
            }
        }

        player.sendSystemMessage(Component.literal("§e修炼经验已满，准备条件即可突破。"));
    }
}
