package org.example.Kangnaixi.tiandao.client.starchart;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.capability.IStarChartData;
import org.example.Kangnaixi.tiandao.starchart.StarNode;
import org.example.Kangnaixi.tiandao.starchart.StarNodeCategory;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

/**
 * 星盘3D渲染器 - V3.0 聚焦展开系统
 */
public class StarChartRenderer {

    private static final Minecraft mc = Minecraft.getInstance();
    private static final float BASE_SCALE = 0.55f;  // 增大基础缩放，更清晰

    /**
     * 渲染星盘
     */
    public static void render(RenderLevelStageEvent event) {
        StarChartClientManager manager = StarChartClientManager.getInstance();

        if (!manager.isEnabled() || mc.player == null) {
            return;
        }

        if (!mc.options.getCameraType().isFirstPerson()) {
            return;
        }

        float partialTick = event.getPartialTick();
        Vec3 playerPos = mc.player.getEyePosition(partialTick);
        Vec3 forwardDir = mc.player.getLookAngle();

        manager.calculateLayout(playerPos, forwardDir);

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        IStarChartData starChartData = mc.player.getCapability(Tiandao.STAR_CHART_CAP).orElse(null);
        if (starChartData == null) {
            return;
        }

        net.minecraft.client.Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();

        // 渲染连线
        renderLines(poseStack, manager, cameraPos);

        // 渲染所有可见节点
        for (StarNodeInstance instance : manager.getInstances()) {
            if (instance.isVisible() && instance.getCurrentAlpha() > 0.01f) {
                renderNode(poseStack, bufferSource, instance, starChartData, cameraPos, manager);
            }
        }

        // 渲染所有可见节点的文字标签
        for (StarNodeInstance instance : manager.getInstances()) {
            if (instance.isVisible() && instance.getCurrentAlpha() > 0.3f) {
                boolean isSelected = instance == manager.getGazedNode();
                renderNodeLabel(poseStack, bufferSource, instance, cameraPos, isSelected);
            }
        }

        bufferSource.endBatch();
    }

    /**
     * 渲染连线
     */
    private static void renderLines(PoseStack poseStack, StarChartClientManager manager, Vec3 cameraPos) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.lineWidth(2.0f);  // 设置线条粗细

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR);

        Matrix4f matrix = poseStack.last().pose();

        if (manager.getCurrentState() == StarChartClientManager.State.OVERVIEW) {
            // 总览状态：主节点之间的五角连线
            renderOverviewLines(buffer, matrix, manager, cameraPos);
        } else {
            // 聚焦状态：主节点到子节点的连线
            renderFocusedLines(buffer, matrix, manager, cameraPos);
        }

        tesselator.end();

        RenderSystem.lineWidth(1.0f);  // 恢复默认线宽
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    /**
     * 渲染总览状态的弧形连线（4类别）
     */
    private static void renderOverviewLines(BufferBuilder buffer, Matrix4f matrix,
                                            StarChartClientManager manager, Vec3 cameraPos) {
        // 弧形连线：从左到右依次连接（4类别）
        // EFFECT → FORM → MODIFIER → BLUEPRINT
        StarNodeCategory[] order = {
            StarNodeCategory.EFFECT,
            StarNodeCategory.FORM,
            StarNodeCategory.MODIFIER,
            StarNodeCategory.BLUEPRINT
        };

        // 依次连接相邻节点
        for (int i = 0; i < order.length - 1; i++) {
            StarNodeInstance node1 = manager.getMasterNode(order[i]);
            StarNodeInstance node2 = manager.getMasterNode(order[i + 1]);

            if (node1 == null || node2 == null) continue;
            if (!node1.isVisible() || !node2.isVisible()) continue;

            Vector3f pos1 = node1.getWorldPos();
            Vector3f pos2 = node2.getWorldPos();

            float alpha = (node1.getCurrentAlpha() + node2.getCurrentAlpha()) / 2f * 0.35f;

            float x1 = (float) (pos1.x - cameraPos.x);
            float y1 = (float) (pos1.y - cameraPos.y);
            float z1 = (float) (pos1.z - cameraPos.z);

            float x2 = (float) (pos2.x - cameraPos.x);
            float y2 = (float) (pos2.y - cameraPos.y);
            float z2 = (float) (pos2.z - cameraPos.z);

            buffer.vertex(matrix, x1, y1, z1).color(0.9f, 0.8f, 0.6f, alpha).endVertex();
            buffer.vertex(matrix, x2, y2, z2).color(0.9f, 0.8f, 0.6f, alpha).endVertex();
        }
    }

    /**
     * 渲染聚焦状态的连线
     */
    private static void renderFocusedLines(BufferBuilder buffer, Matrix4f matrix,
                                           StarChartClientManager manager, Vec3 cameraPos) {
        StarNodeCategory focusedCat = manager.getFocusedCategory();
        if (focusedCat == null) return;

        StarNodeInstance masterNode = manager.getMasterNode(focusedCat);
        if (masterNode == null || !masterNode.isVisible()) return;

        List<StarNodeInstance> children = manager.getChildNodes(focusedCat);
        if (children == null) return;

        Vector3f masterPos = masterNode.getWorldPos();
        int color = focusedCat.getColor();
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        for (StarNodeInstance child : children) {
            if (!child.isVisible() || child.getCurrentAlpha() < 0.1f) continue;

            Vector3f childPos = child.getWorldPos();
            float alpha = child.getCurrentAlpha() * 0.6f;

            float x1 = (float) (masterPos.x - cameraPos.x);
            float y1 = (float) (masterPos.y - cameraPos.y);
            float z1 = (float) (masterPos.z - cameraPos.z);

            float x2 = (float) (childPos.x - cameraPos.x);
            float y2 = (float) (childPos.y - cameraPos.y);
            float z2 = (float) (childPos.z - cameraPos.z);

            buffer.vertex(matrix, x1, y1, z1).color(r, g, b, alpha).endVertex();
            buffer.vertex(matrix, x2, y2, z2).color(r, g, b, alpha).endVertex();
        }
    }

    /**
     * 渲染单个节点
     */
    private static void renderNode(PoseStack poseStack, MultiBufferSource bufferSource,
                                   StarNodeInstance instance, IStarChartData starChartData,
                                   Vec3 cameraPos, StarChartClientManager manager) {
        float scale = instance.getCurrentScale();
        float alpha = instance.getCurrentAlpha();

        // 子节点需要考虑展开进度
        if (!instance.isMasterNode()) {
            float expandProgress = instance.getExpandProgress();
            scale *= expandProgress;
            alpha *= expandProgress;
        }

        if (scale < 0.01f || alpha < 0.01f) {
            return;
        }

        poseStack.pushPose();

        Vector3f worldPos = instance.getWorldPos();

        double relativeX = worldPos.x - cameraPos.x;
        double relativeY = worldPos.y - cameraPos.y;
        double relativeZ = worldPos.z - cameraPos.z;
        poseStack.translate(relativeX, relativeY, relativeZ);

        applyBillboardRotation(poseStack);

        float finalScale = BASE_SCALE * scale;

        boolean unlocked = starChartData.isNodeUnlocked(instance.getNode().getId());
        if (!unlocked && !instance.isMasterNode()) {
            finalScale *= 0.85f;
            alpha *= 0.7f;
        }

        poseStack.scale(finalScale, finalScale, finalScale);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);

        ItemStack displayItem = instance.getDisplayItem();
        if (!displayItem.isEmpty()) {
            ItemRenderer itemRenderer = mc.getItemRenderer();
            itemRenderer.renderStatic(
                displayItem,
                ItemDisplayContext.GROUND,
                15728880,
                0,
                poseStack,
                bufferSource,
                mc.level,
                0
            );
        }

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    private static void applyBillboardRotation(PoseStack poseStack) {
        if (mc.gameRenderer.getMainCamera() == null) {
            return;
        }

        Quaternionf cameraRotation = mc.gameRenderer.getMainCamera().rotation();
        Matrix4f matrix = poseStack.last().pose();
        matrix.rotate(cameraRotation);
    }

    /**
     * 渲染节点的文字标签
     */
    private static void renderNodeLabel(PoseStack poseStack, MultiBufferSource bufferSource,
                                        StarNodeInstance instance, Vec3 cameraPos, boolean isSelected) {
        StarNode node = instance.getNode();
        Vector3f worldPos = instance.getWorldPos();

        poseStack.pushPose();

        // 移动到节点上方
        double relativeX = worldPos.x - cameraPos.x;
        double relativeY = worldPos.y - cameraPos.y + 0.35;  // 在节点上方
        double relativeZ = worldPos.z - cameraPos.z;
        poseStack.translate(relativeX, relativeY, relativeZ);

        // Billboard旋转面向相机
        applyBillboardRotation(poseStack);

        // 缩小文字，选中时稍大
        float textScale = isSelected ? 0.014f : 0.01f;
        poseStack.scale(-textScale, -textScale, textScale);

        Font font = mc.font;
        float alpha = instance.getCurrentAlpha();
        int bgColor = (int)(alpha * 0.5f * 255) << 24;  // 半透明背景

        // 节点名称
        String name = node.getName();
        int nameWidth = font.width(name);
        int nameColor = isSelected ? 0xFFFF00 : 0xFFFFFF;  // 选中时黄色
        font.drawInBatch(
            name,
            -nameWidth / 2f, 0,
            nameColor,
            false,
            poseStack.last().pose(),
            bufferSource,
            Font.DisplayMode.NORMAL,
            bgColor,
            15728880
        );

        poseStack.popPose();
    }
}
