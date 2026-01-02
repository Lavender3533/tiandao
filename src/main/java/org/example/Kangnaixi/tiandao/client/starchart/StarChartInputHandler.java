package org.example.Kangnaixi.tiandao.client.starchart;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.client.event.InputEvent;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.capability.IStarChartData;
import org.example.Kangnaixi.tiandao.network.NetworkHandler;
import org.example.Kangnaixi.tiandao.network.packet.C2SUnlockStarNodePacket;
import org.lwjgl.glfw.GLFW;

/**
 * 星盘输入处理器 - V3.0 聚焦展开系统
 *
 * 交互：
 * - 注视主节点 0.8秒 → 进入聚焦状态
 * - 注视空白区域 0.5秒 → 退出聚焦状态
 * - 按 ESC / 右键 → 退出聚焦状态
 * - 左键点击子节点 → 解锁/选择
 */
public class StarChartInputHandler {

    private static final Minecraft mc = Minecraft.getInstance();

    /**
     * 处理鼠标点击事件
     */
    public static void onMouseClick(InputEvent.MouseButton.Pre event) {
        StarChartClientManager manager = StarChartClientManager.getInstance();
        if (!manager.isEnabled() || mc.player == null) {
            return;
        }

        int button = event.getButton();
        int action = event.getAction();

        // 右键点击 → 退出聚焦
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && action == GLFW.GLFW_PRESS) {
            if (manager.getCurrentState() == StarChartClientManager.State.FOCUSED) {
                manager.exitFocusState();
                playClickSound();
                event.setCanceled(true);
                return;
            }
        }

        // 左键点击
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && action == GLFW.GLFW_PRESS) {
            StarNodeInstance gazedNode = manager.getGazedNode();

            if (gazedNode != null) {
                if (manager.getCurrentState() == StarChartClientManager.State.FOCUSED) {
                    // 聚焦状态：点击子节点
                    if (!gazedNode.isMasterNode()) {
                        handleChildNodeClick(gazedNode);
                        event.setCanceled(true);
                    }
                }
            }
        }
    }

    /**
     * 处理键盘事件
     */
    public static void onKeyPress(InputEvent.Key event) {
        StarChartClientManager manager = StarChartClientManager.getInstance();
        if (!manager.isEnabled() || mc.player == null) {
            return;
        }

        // ESC 键退出聚焦
        if (event.getKey() == GLFW.GLFW_KEY_ESCAPE && event.getAction() == GLFW.GLFW_PRESS) {
            if (manager.getCurrentState() == StarChartClientManager.State.FOCUSED) {
                manager.exitFocusState();
                playClickSound();
                // 不消费事件，让 ESC 可以继续关闭星盘
            }
        }
    }

    /**
     * 处理子节点点击
     */
    private static void handleChildNodeClick(StarNodeInstance node) {
        IStarChartData starChartData = mc.player.getCapability(Tiandao.STAR_CHART_CAP).orElse(null);
        if (starChartData == null) return;

        String nodeId = node.getNode().getId();
        boolean unlocked = starChartData.isNodeUnlocked(nodeId);

        if (!unlocked) {
            // 发送解锁请求
            NetworkHandler.sendToServer(new C2SUnlockStarNodePacket(nodeId));
            playUnlockSound();
        } else {
            // 已解锁的节点 → 可以触发其他操作（选择、详情等）
            playClickSound();
        }
    }

    private static void playClickSound() {
        if (mc.level != null && mc.player != null) {
            mc.level.playSound(
                mc.player,
                mc.player.blockPosition(),
                SoundEvents.UI_BUTTON_CLICK.value(),
                SoundSource.MASTER,
                0.5f,
                1.0f
            );
        }
    }

    private static void playUnlockSound() {
        if (mc.level != null && mc.player != null) {
            mc.level.playSound(
                mc.player,
                mc.player.blockPosition(),
                SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS,
                0.8f,
                1.2f
            );
        }
    }

    /**
     * @deprecated 使用 StarChartClientManager.getGazedNode() 代替
     */
    @Deprecated
    public static StarNodeInstance getHoveredNode() {
        return StarChartClientManager.getInstance().getGazedNode();
    }
}
