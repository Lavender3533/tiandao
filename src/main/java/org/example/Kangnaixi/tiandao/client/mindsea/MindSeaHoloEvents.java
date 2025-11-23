package org.example.Kangnaixi.tiandao.client.mindsea;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.client.KeyBindings;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * 识海内视模式 - 事件处理器（视线射线拾取）
 * 鼠标视线射线检测最近光球，左键选中
 */
@Mod.EventBusSubscriber(modid = Tiandao.MOD_ID, value = Dist.CLIENT)
public class MindSeaHoloEvents {

    /**
     * 3D渲染事件处理
     */
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        MindSeaHoloState state = MindSeaHoloState.getInstance();
        if (!state.isEnabled()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.gameRenderer == null) {
            return;
        }

        try {
            MindSeaHoloRenderer.render(
                event.getPoseStack(),
                event.getPartialTick(),
                mc.gameRenderer.getMainCamera()
            );
        } catch (Exception e) {
            Tiandao.LOGGER.error("识海全息渲染时发生错误", e);
        }
    }

    /**
     * 客户端Tick事件处理
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        MindSeaHoloState state = MindSeaHoloState.getInstance();

        // 处理B键切换
        while (KeyBindings.TOGGLE_MINDSEA_HOLO.consumeClick()) {
            state.toggle();

            if (state.isEnabled()) {
                // 释放鼠标，显示指针
                mc.mouseHandler.releaseMouse();
            } else {
                // 重新抓取鼠标，隐藏指针
                mc.mouseHandler.grabMouse();
            }

            String message = state.isEnabled()
                ? "§6【识海拼装】§f已开启 - 视角+鼠标悬停选中，左键确认，滚轮远近，ESC退出"
                : "§6【识海拼装】§f已关闭";
            mc.player.sendSystemMessage(Component.literal(message));
        }

        if (!state.isEnabled()) {
            return;
        }

        // ESC退出
        if (mc.screen != null) {
            state.disable();
            // 重新抓取鼠标
            mc.mouseHandler.grabMouse();
            return;
        }

        updateRaycastHover(state, mc);
    }

    /**
     * 使用视线射线检测hover的光球
     */
    private static void updateRaycastHover(MindSeaHoloState state, Minecraft mc) {
        if (mc.player == null) {
            state.setHoveredOrb(-1, -1);
            return;
        }

        // 玩家视线射线
        Vec3 eyePos = mc.player.getEyePosition(1.0f);
        Vec3 lookVec = mc.player.getLookAngle();
        float maxDist = 10f; // 最大检测距离

        // 玩家头部位置（光球中心）
        Vec3 playerHeadPos = eyePos.add(0, 0.3, 0);

        // 遍历所有光球，找到最近的（光球固定在世界空间）
        List<MindSeaHoloState.GroupData> groups = state.getGroups();
        float radius = state.getRadius();

        float closestDist = Float.MAX_VALUE;
        int closestGroup = -1;
        int closestOrb = -1;

        for (int groupIdx = 0; groupIdx < groups.size(); groupIdx++) {
            MindSeaHoloState.GroupData group = groups.get(groupIdx);
            List<MindSeaHoloState.OrbData> orbs = group.getOrbs();

            float arcStart = group.getBaseYaw() - group.getArcSpan() / 2;

            for (int orbIdx = 0; orbIdx < orbs.size(); orbIdx++) {
                // 计算光球世界位置（固定方向，不随玩家转动）
                float arcProgress = orbs.size() > 1 ? (float) orbIdx / (orbs.size() - 1) : 0.5f;
                float orbYaw = arcStart + arcProgress * group.getArcSpan();
                // 不加playerYaw，固定在世界空间
                float worldYaw = orbYaw;

                float radians = (float) Math.toRadians(worldYaw);
                float x = (float) -Math.sin(radians) * radius;
                float z = (float) Math.cos(radians) * radius;
                float y = 0;

                Vec3 orbPos = playerHeadPos.add(x, y, z);

                // 计算射线到光球的最短距离
                float dist = rayToPointDistance(eyePos, lookVec, orbPos, maxDist);

                if (dist < 0.15f && dist < closestDist) { // 检测半径0.15格
                    closestDist = dist;
                    closestGroup = groupIdx;
                    closestOrb = orbIdx;
                }
            }
        }

        state.setHoveredOrb(closestGroup, closestOrb);
    }

    /**
     * 计算射线到点的最短距离
     */
    private static float rayToPointDistance(Vec3 rayStart, Vec3 rayDir, Vec3 point, float maxDist) {
        Vec3 diff = point.subtract(rayStart);
        double t = diff.dot(rayDir);

        if (t < 0 || t > maxDist) {
            return Float.MAX_VALUE;
        }

        Vec3 closestPoint = rayStart.add(rayDir.scale(t));
        return (float) closestPoint.distanceTo(point);
    }

    /**
     * 鼠标滚轮事件处理
     */
    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        MindSeaHoloState state = MindSeaHoloState.getInstance();
        if (!state.isEnabled()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            return;
        }

        double scrollDelta = event.getScrollDelta();
        state.adjustRadius((float) scrollDelta * -0.2f);
        event.setCanceled(true);
    }

    /**
     * 鼠标按键事件处理
     */
    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        MindSeaHoloState state = MindSeaHoloState.getInstance();
        if (!state.isEnabled()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            return;
        }

        // 左键确认
        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT && event.getAction() == GLFW.GLFW_PRESS) {
            int group = state.getHoveredGroup();
            int orb = state.getHoveredOrbIndex();

            if (group != -1 && orb != -1) {
                state.selectOrb(group, orb);

                MindSeaHoloState.GroupData groupData = state.getGroups().get(group);
                MindSeaHoloState.OrbData orbData = groupData.getOrbs().get(orb);

                mc.player.sendSystemMessage(Component.literal(
                    "§6【已选】§f" + groupData.getTitle() + " → " + orbData.getDisplayName()
                ));
                event.setCanceled(true);
            }
        }
    }
}
