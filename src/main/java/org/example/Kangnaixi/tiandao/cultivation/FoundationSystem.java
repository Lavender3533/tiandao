package org.example.Kangnaixi.tiandao.cultivation;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.capability.ICultivation;

/**
 * 根基系统管理器：负责处理根基受损、恢复与相关判定
 */
public class FoundationSystem {

    /**
     * 玩家死亡时的根基惩罚
     */
    public static void onPlayerDeath(ServerPlayer player) {
        player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
            int currentFoundation = cultivation.getFoundation();
            int reduction = 10;

            cultivation.reduceFoundation(reduction);
            int newFoundation = cultivation.getFoundation();

            player.sendSystemMessage(Component.literal(
                String.format("§c根基受损！根基值: %d → %d (-%d)", currentFoundation, newFoundation, reduction)
            ), false);

            Tiandao.LOGGER.info("玩家 {} 死亡，根基值减少: {} → {}",
                player.getName().getString(), currentFoundation, newFoundation);
        });
    }

    /**
     * 玩家重伤（生命值低于30%）时的根基惩罚
     */
    public static void onPlayerCriticalHealth(ServerPlayer player) {
        player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
            long currentTime = System.currentTimeMillis();
            long lastCriticalTime = cultivation.getLastCombatTime();
            long cooldown = 30_000L; // 30 秒

            if (currentTime - lastCriticalTime <= cooldown) {
                return;
            }

            int currentFoundation = cultivation.getFoundation();
            int reduction = 5;

            cultivation.reduceFoundation(reduction);
            cultivation.setLastCombatTime(currentTime);

            int newFoundation = cultivation.getFoundation();
            player.sendSystemMessage(Component.literal(
                String.format("§e重伤导致根基受损！根基值: %d → %d (-%d)", currentFoundation, newFoundation, reduction)
            ), false);

            Tiandao.LOGGER.info("玩家 {} 重伤，根基值减少: {} → {}",
                player.getName().getString(), currentFoundation, newFoundation);
        });
    }

    /**
     * 突破失败时的根基惩罚
     */
    public static void onBreakthroughFailed(ServerPlayer player, boolean isMajorBreakthrough) {
        player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
            int currentFoundation = cultivation.getFoundation();
            int reduction = isMajorBreakthrough ? 10 : 3;

            cultivation.reduceFoundation(reduction);
            int newFoundation = cultivation.getFoundation();

            String prefix = isMajorBreakthrough ? "§c突破失败，根基严重受损！" : "§e突破失败，根基受损！";
            player.sendSystemMessage(Component.literal(
                String.format("%s根基值: %d → %d (-%d)", prefix, currentFoundation, newFoundation, reduction)
            ), false);

            Tiandao.LOGGER.info("玩家 {} 突破失败（{}），根基值减少: {} → {}",
                player.getName().getString(),
                isMajorBreakthrough ? "大境界" : "小境界",
                currentFoundation, newFoundation);
        });
    }

    /**
     * 是否可突破小境界
     */
    public static boolean canBreakthroughSubRealm(ICultivation cultivation) {
        return cultivation.getFoundation() >= 50;
    }

    /**
     * 是否可突破大境界
     */
    public static boolean canBreakthroughMajorRealm(ICultivation cultivation) {
        return cultivation.getFoundation() >= 30;
    }

    /**
     * 带颜色代码的根基状态描述（保留旧接口）
     */
    public static String getFoundationStatus(int foundation) {
        if (foundation >= 100) {
            return "§a稳固";
        } else if (foundation >= 80) {
            return "§e良好";
        } else if (foundation >= 50) {
            return "§6不稳";
        } else if (foundation >= 30) {
            return "§c受损";
        } else {
            return "§4严重受损";
        }
    }

    /**
     * 提供HUD/命令使用的根基描述（纯文本 + 颜色值）
     */
    public static FoundationDescriptor describeFoundation(int foundation) {
        int clamped = Math.max(0, Math.min(100, foundation));
        if (clamped >= 100) {
            return new FoundationDescriptor("稳固", 0x00FF00);
        } else if (clamped >= 80) {
            return new FoundationDescriptor("良好", 0xFFFF55);
        } else if (clamped >= 50) {
            return new FoundationDescriptor("不稳", 0xFFAA00);
        } else if (clamped >= 30) {
            return new FoundationDescriptor("受损", 0xFF5555);
        } else {
            return new FoundationDescriptor("严重受损", 0xAA0000);
        }
    }

    /**
     * 根基对修炼速度的影响
     */
    public static double getCultivationSpeedMultiplier(int foundation) {
        if (foundation < 30) {
            return 0.5;
        } else if (foundation < 50) {
            return 0.75;
        } else if (foundation < 80) {
            return 0.9;
        } else {
            return 1.0;
        }
    }

    /**
     * 根基对灵力恢复速度的影响
     */
    public static double getSpiritRecoveryMultiplier(int foundation) {
        if (foundation < 30) {
            return 0.7;
        } else if (foundation < 50) {
            return 0.85;
        } else {
            return 1.0;
        }
    }

    /**
     * 根基展示信息
     */
    public record FoundationDescriptor(String label, int color) {}
}
