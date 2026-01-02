package org.example.Kangnaixi.tiandao.client.event;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.client.KeyBindings;
import org.example.Kangnaixi.tiandao.client.starchart.StarChartClientManager;
import org.example.Kangnaixi.tiandao.handwheel.HandWheelCombination;
import org.example.Kangnaixi.tiandao.handwheel.HandWheelLinkHandler;
import org.example.Kangnaixi.tiandao.handwheel.HandWheelManager;
import org.example.Kangnaixi.tiandao.network.NetworkHandler;
import org.example.Kangnaixi.tiandao.network.packet.C2SHandWheelCompilePacket;
import org.lwjgl.glfw.GLFW;

/**
 * 手盘按键处理器
 *
 * 按键绑定：
 * - R键：星盘联动（注视节点时填充到手盘）
 * - Enter键：编译当前组合
 * - Delete键：清空手盘
 */
@OnlyIn(Dist.CLIENT)
public class HandWheelKeyHandler {

    private static boolean wasRKeyDown = false;
    private static boolean wasEnterDown = false;
    private static boolean wasDeleteDown = false;

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            return;
        }

        // 只在星盘开启时处理
        StarChartClientManager starChart = StarChartClientManager.getInstance();
        if (!starChart.isEnabled()) {
            return;
        }

        // R键 - 星盘联动（只在按下时触发，使用事件参数而非轮询）
        if (event.getKey() == GLFW.GLFW_KEY_R && event.getAction() == GLFW.GLFW_PRESS) {
            Tiandao.LOGGER.info("[HandWheel] R键按下，星盘开启状态: {}", starChart.isEnabled());
            handleLinkKey();
            return; // 已处理，不再传递
        }

        // 保留原有的状态追踪（用于其他按键）
        boolean isRKeyDown = GLFW.glfwGetKey(mc.getWindow().getWindow(), GLFW.GLFW_KEY_R) == GLFW.GLFW_PRESS;
        wasRKeyDown = isRKeyDown;

        // Enter键 - 编译组合
        boolean isEnterDown = GLFW.glfwGetKey(mc.getWindow().getWindow(), GLFW.GLFW_KEY_ENTER) == GLFW.GLFW_PRESS;
        if (isEnterDown && !wasEnterDown) {
            handleCompileKey();
        }
        wasEnterDown = isEnterDown;

        // Delete键 - 清空手盘
        boolean isDeleteDown = GLFW.glfwGetKey(mc.getWindow().getWindow(), GLFW.GLFW_KEY_DELETE) == GLFW.GLFW_PRESS;
        if (isDeleteDown && !wasDeleteDown) {
            handleClearKey();
        }
        wasDeleteDown = isDeleteDown;
    }

    /**
     * 处理星盘→手盘联动
     */
    private static void handleLinkKey() {
        HandWheelLinkHandler linkHandler = HandWheelLinkHandler.getInstance();

        // 自动启用联动模式
        if (!linkHandler.isLinkModeEnabled()) {
            linkHandler.setLinkModeEnabled(true);
        }

        boolean success = linkHandler.tryLinkGazedNode();

        if (success) {
            Minecraft mc = Minecraft.getInstance();
            // 可以显示提示
            // mc.player.displayClientMessage(Component.literal("§a节点已填充"), true);
        }
    }

    /**
     * 处理编译组合
     */
    private static void handleCompileKey() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        HandWheelManager handWheel = HandWheelManager.getClientInstance();
        HandWheelCombination combination = handWheel.getCombination();

        // 验证组合
        HandWheelCombination.ValidationResult validation = combination.validate();
        if (!validation.isValid()) {
            mc.player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§c" + validation.getMessage()),
                true
            );
            return;
        }

        // 发送编译请求到服务端
        C2SHandWheelCompilePacket packet = new C2SHandWheelCompilePacket(combination, null);
        NetworkHandler.sendToServer(packet);

        mc.player.displayClientMessage(
            net.minecraft.network.chat.Component.literal("§e正在编译术法组合..."),
            true
        );

        Tiandao.LOGGER.info("发送手盘编译请求");
    }

    /**
     * 处理清空手盘
     */
    private static void handleClearKey() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        HandWheelManager handWheel = HandWheelManager.getClientInstance();
        handWheel.clearAll();

        mc.player.displayClientMessage(
            net.minecraft.network.chat.Component.literal("§7手盘已清空"),
            true
        );
    }

    /**
     * 获取当前手盘状态描述（用于HUD显示）
     */
    public static String getHandWheelStatusText() {
        HandWheelManager handWheel = HandWheelManager.getClientInstance();
        HandWheelCombination combo = handWheel.getCombination();

        StringBuilder sb = new StringBuilder();
        sb.append("§6[手盘] ");

        if (combo.getEffectSlot().isEmpty()) {
            sb.append("§7效果:空 ");
        } else {
            sb.append("§a效果:").append(combo.getEffectSlot().getNode().getName()).append(" ");
        }

        if (combo.getFormSlot().isEmpty()) {
            sb.append("§7形态:空 ");
        } else {
            sb.append("§e形态:").append(combo.getFormSlot().getNode().getName()).append(" ");
        }

        int modCount = combo.getActiveModifiers().size();
        if (modCount > 0) {
            sb.append("§d+").append(modCount).append("调制");
        }

        return sb.toString();
    }
}
