package org.example.Kangnaixi.tiandao.client.mindsea;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.List;

/**
 * 识海内视模式 - 环绕式光球渲染器
 * 4组光球环绕玩家，底部槽位
 */
public class MindSeaHoloRenderer {

    // 布局参数
    private static final float ORB_RADIUS = 0.1f;         // 光球半径
    private static final float SLOT_WIDTH = 0.5f;         // 底部槽位宽度
    private static final float SLOT_HEIGHT = 0.2f;        // 底部槽位高度
    private static final float SLOT_SPACING = 0.1f;       // 槽位间距
    private static final float BOTTOM_SLOT_Y = -0.6f;     // 底部槽位Y位置（相对头部）

    // 视觉参数
    private static final float MAX_ALPHA = 0.35f;
    private static final int PARTICLE_COUNT = 20;

    // 粒子缓存
    private static final float[][] floatingParticles = new float[PARTICLE_COUNT][4];
    private static boolean particlesInitialized = false;

    /**
     * 主渲染入口
     */
    public static void render(PoseStack poseStack, float partialTick, Camera camera) {
        MindSeaHoloState state = MindSeaHoloState.getInstance();
        if (!state.isEnabled()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        if (!particlesInitialized) {
            initFloatingParticles();
            particlesInitialized = true;
        }

        // 设置渲染状态
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.depthMask(false);

        poseStack.pushPose();

        // 计算玩家头部位置
        Vec3 cameraPos = camera.getPosition();
        Vec3 playerHeadPos = mc.player.getEyePosition(partialTick).add(0, 0.3, 0); // 略高于视线

        poseStack.translate(
            playerHeadPos.x - cameraPos.x,
            playerHeadPos.y - cameraPos.y,
            playerHeadPos.z - cameraPos.z
        );

        // 渲染层级
        renderFloatingParticles(poseStack, partialTick);
        renderOrbGroups(poseStack, state, mc.player.getYRot(), camera);
        renderConnectionLines(poseStack, state, camera);
        renderBottomSlots(poseStack, state, camera);

        poseStack.popPose();

        // 恢复渲染状态
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    /**
     * 初始化漂浮粒子
     */
    private static void initFloatingParticles() {
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            float angle = (float) (Math.random() * Math.PI * 2);
            float radius = (float) (Math.random() * 3);
            floatingParticles[i][0] = (float) Math.cos(angle) * radius;
            floatingParticles[i][1] = (float) (Math.random() * 1.2 - 0.6);
            floatingParticles[i][2] = (float) Math.sin(angle) * radius;
            floatingParticles[i][3] = (float) (Math.random() * Math.PI * 2);
        }
    }

    /**
     * 渲染漂浮粒子
     */
    private static void renderFloatingParticles(PoseStack poseStack, float partialTick) {
        long time = System.currentTimeMillis();
        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i < PARTICLE_COUNT; i++) {
            float phase = floatingParticles[i][3];
            float t = time / 5000.0f + phase;

            float x = floatingParticles[i][0] + (float) Math.sin(t) * 0.1f;
            float y = floatingParticles[i][1] + (float) Math.sin(t * 0.7) * 0.05f;
            float z = floatingParticles[i][2] + (float) Math.cos(t * 0.6) * 0.1f;

            float brightness = (float) (0.2 + 0.3 * Math.sin(t * 2 + i));
            float size = 0.015f;
            int alpha = (int) (MAX_ALPHA * 100 * brightness);

            int r = (i % 2 == 0) ? 64 : 176;
            int g = 224;
            int b = (i % 2 == 0) ? 208 : 255;

            // Billboard朝向相机的小方块
            buffer.vertex(matrix, x - size, y - size, z).color(r, g, b, alpha).endVertex();
            buffer.vertex(matrix, x - size, y + size, z).color(r, g, b, alpha).endVertex();
            buffer.vertex(matrix, x + size, y + size, z).color(r, g, b, alpha).endVertex();
            buffer.vertex(matrix, x + size, y - size, z).color(r, g, b, alpha).endVertex();
        }

        BufferUploader.drawWithShader(buffer.end());
    }

    /**
     * 渲染4组环绕光球（固定在世界空间，不随玩家转动）
     */
    private static void renderOrbGroups(PoseStack poseStack, MindSeaHoloState state, float playerYaw, Camera camera) {
        List<MindSeaHoloState.GroupData> groups = state.getGroups();
        float radius = state.getRadius();

        for (int groupIdx = 0; groupIdx < groups.size(); groupIdx++) {
            MindSeaHoloState.GroupData group = groups.get(groupIdx);
            List<MindSeaHoloState.OrbData> orbs = group.getOrbs();

            // 计算组内光球的弧线分布
            float arcStart = group.getBaseYaw() - group.getArcSpan() / 2;

            for (int orbIdx = 0; orbIdx < orbs.size(); orbIdx++) {
                MindSeaHoloState.OrbData orb = orbs.get(orbIdx);

                // 计算光球在弧线上的位置
                float arcProgress = orbs.size() > 1 ? (float) orbIdx / (orbs.size() - 1) : 0.5f;
                float orbYaw = arcStart + arcProgress * group.getArcSpan();
                // 不加playerYaw，固定在世界空间
                float worldYaw = orbYaw;

                // 极坐标转世界坐标（固定方向）
                float radians = (float) Math.toRadians(worldYaw);
                float x = (float) -Math.sin(radians) * radius;
                float z = (float) Math.cos(radians) * radius;
                float y = 0; // 与头部同高

                boolean isHovered = (groupIdx == state.getHoveredGroup() && orbIdx == state.getHoveredOrbIndex());
                renderOrb(poseStack, orb, x, y, z, isHovered, camera);
                renderOrbLabel(poseStack, orb.getDisplayName(), x, y + 0.15f, z, orb.getColor(), camera);
            }
        }
    }

    /**
     * 渲染单个光球
     */
    private static void renderOrb(PoseStack poseStack, MindSeaHoloState.OrbData orb, float x, float y, float z,
                                   boolean isHovered, Camera camera) {
        // 轻微漂浮
        long time = System.currentTimeMillis();
        float floatOffset = (float) Math.sin(time / 1000.0 + x * 10 + z * 10) * 0.02f;
        y += floatOffset;

        float radius = ORB_RADIUS;
        if (isHovered) {
            radius *= 1.15f;
        }

        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        int r = (orb.getColor() >> 16) & 0xFF;
        int g = (orb.getColor() >> 8) & 0xFF;
        int b = orb.getColor() & 0xFF;

        // 核心（亮）
        int coreAlpha = (int) (MAX_ALPHA * 220);
        if (isHovered) coreAlpha = (int) (MAX_ALPHA * 255);

        float coreRadius = radius * 0.6f;

        // 朝向相机的billboard四边形
        Vec3 cameraPos = camera.getPosition();
        Vec3 orbPos = new Vec3(x, y, z);

        buffer.vertex(matrix, x - coreRadius, y - coreRadius, z).color(r, g, b, coreAlpha).endVertex();
        buffer.vertex(matrix, x - coreRadius, y + coreRadius, z).color(r, g, b, coreAlpha).endVertex();
        buffer.vertex(matrix, x + coreRadius, y + coreRadius, z).color(r, g, b, coreAlpha).endVertex();
        buffer.vertex(matrix, x + coreRadius, y - coreRadius, z).color(r, g, b, coreAlpha).endVertex();

        // 外层光晕
        int glowAlpha = (int) (MAX_ALPHA * 90);
        if (isHovered) glowAlpha = (int) (MAX_ALPHA * 150);

        buffer.vertex(matrix, x - radius, y - radius, z).color(r, g, b, glowAlpha).endVertex();
        buffer.vertex(matrix, x - radius, y + radius, z).color(r, g, b, glowAlpha).endVertex();
        buffer.vertex(matrix, x + radius, y + radius, z).color(r, g, b, glowAlpha).endVertex();
        buffer.vertex(matrix, x + radius, y - radius, z).color(r, g, b, glowAlpha).endVertex();

        BufferUploader.drawWithShader(buffer.end());

        // hover描边
        if (isHovered) {
            buffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
            int borderAlpha = (int) (MAX_ALPHA * 255);

            buffer.vertex(matrix, x - radius, y, z).color(r, g, b, borderAlpha).endVertex();
            buffer.vertex(matrix, x, y + radius, z).color(r, g, b, borderAlpha).endVertex();
            buffer.vertex(matrix, x, y + radius, z).color(r, g, b, borderAlpha).endVertex();
            buffer.vertex(matrix, x + radius, y, z).color(r, g, b, borderAlpha).endVertex();
            buffer.vertex(matrix, x + radius, y, z).color(r, g, b, borderAlpha).endVertex();
            buffer.vertex(matrix, x, y - radius, z).color(r, g, b, borderAlpha).endVertex();
            buffer.vertex(matrix, x, y - radius, z).color(r, g, b, borderAlpha).endVertex();
            buffer.vertex(matrix, x - radius, y, z).color(r, g, b, borderAlpha).endVertex();

            BufferUploader.drawWithShader(buffer.end());
        }
    }

    /**
     * 渲染光球标签
     */
    private static void renderOrbLabel(PoseStack poseStack, String label, float x, float y, float z, int color, Camera camera) {
        Minecraft mc = Minecraft.getInstance();

        poseStack.pushPose();
        poseStack.translate(x, y, z);

        // Billboard旋转（朝向相机）
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-camera.getYRot()));
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(camera.getXRot()));

        float scale = 0.012f;
        poseStack.scale(-scale, -scale, scale);

        Matrix4f matrix = poseStack.last().pose();
        int textWidth = mc.font.width(label);

        int alpha = (int) (0.9f * 255);
        int textColor = (color & 0x00FFFFFF) | (alpha << 24);

        mc.font.drawInBatch(label, -textWidth / 2.0f, -mc.font.lineHeight / 2.0f, textColor, false,
                           matrix, mc.renderBuffers().bufferSource(),
                           net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, 15728880);
        mc.renderBuffers().bufferSource().endBatch();

        poseStack.popPose();
    }

    /**
     * 渲染连接线（从选中的光球到对应槽位）
     */
    private static void renderConnectionLines(PoseStack poseStack, MindSeaHoloState state, Camera camera) {
        List<MindSeaHoloState.GroupData> groups = state.getGroups();
        MindSeaHoloState.OrbData[] selected = state.getSelectedOrbs();
        float radius = state.getRadius();

        poseStack.pushPose();

        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();

        // 使用DEBUG_LINES模式绘制多段线条以实现渐变
        buffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

        for (int groupIdx = 0; groupIdx < groups.size(); groupIdx++) {
            MindSeaHoloState.OrbData selectedOrb = selected[groupIdx];
            if (selectedOrb == null) continue;

            MindSeaHoloState.GroupData group = groups.get(groupIdx);
            List<MindSeaHoloState.OrbData> orbs = group.getOrbs();

            // 找到选中光球的位置
            int selectedOrbIdx = -1;
            for (int i = 0; i < orbs.size(); i++) {
                if (orbs.get(i).getId().equals(selectedOrb.getId())) {
                    selectedOrbIdx = i;
                    break;
                }
            }

            if (selectedOrbIdx == -1) continue;

            // 计算光球位置（世界空间）
            float arcStart = group.getBaseYaw() - group.getArcSpan() / 2;
            float arcProgress = orbs.size() > 1 ? (float) selectedOrbIdx / (orbs.size() - 1) : 0.5f;
            float orbYaw = arcStart + arcProgress * group.getArcSpan();
            float worldYaw = orbYaw;

            float radians = (float) Math.toRadians(worldYaw);
            float orbX = (float) -Math.sin(radians) * radius;
            float orbZ = (float) Math.cos(radians) * radius;
            float orbY = 0;

            // 计算槽位位置（billboard空间，需转换到世界空间）
            float totalWidth = 5 * SLOT_WIDTH + 4 * SLOT_SPACING;
            float startX = -totalWidth / 2;
            float slotX = startX + groupIdx * (SLOT_WIDTH + SLOT_SPACING) + SLOT_WIDTH / 2;

            // 槽位在billboard空间的位置，需要根据相机旋转转换
            float slotY = BOTTOM_SLOT_Y;

            // 计算从相机视角的偏移向量（简化：假设槽位朝向相机）
            org.joml.Vector3f cameraLookVec = camera.getLookVector();
            Vec3 cameraLook = new Vec3(cameraLookVec.x, cameraLookVec.y, cameraLookVec.z);
            Vec3 cameraRight = new Vec3(-cameraLook.z, 0, cameraLook.x).normalize();
            Vec3 cameraUp = cameraRight.cross(cameraLook);

            // 槽位世界位置 = billboard中心 + 右向量*slotX + 上向量*slotY
            float slotWorldX = (float) (cameraRight.x * slotX + cameraUp.x * slotY);
            float slotWorldY = (float) (cameraRight.y * slotX + cameraUp.y * slotY);
            float slotWorldZ = (float) (cameraRight.z * slotX + cameraUp.z * slotY);

            // 绘制渐变线条（分段实现fade效果）
            int segments = 8;
            int r = (selectedOrb.getColor() >> 16) & 0xFF;
            int g = (selectedOrb.getColor() >> 8) & 0xFF;
            int b = selectedOrb.getColor() & 0xFF;

            for (int i = 0; i < segments; i++) {
                float t1 = (float) i / segments;
                float t2 = (float) (i + 1) / segments;

                // 线性插值位置
                float x1 = orbX + (slotWorldX - orbX) * t1;
                float y1 = orbY + (slotWorldY - orbY) * t1;
                float z1 = orbZ + (slotWorldZ - orbZ) * t1;

                float x2 = orbX + (slotWorldX - orbX) * t2;
                float y2 = orbY + (slotWorldY - orbY) * t2;
                float z2 = orbZ + (slotWorldZ - orbZ) * t2;

                // 计算fade alpha（两端fade out）
                float fade1 = (float) (Math.sin(t1 * Math.PI) * 0.6);
                float fade2 = (float) (Math.sin(t2 * Math.PI) * 0.6);

                int alpha1 = (int) (MAX_ALPHA * 255 * fade1);
                int alpha2 = (int) (MAX_ALPHA * 255 * fade2);

                buffer.vertex(matrix, x1, y1, z1).color(r, g, b, alpha1).endVertex();
                buffer.vertex(matrix, x2, y2, z2).color(r, g, b, alpha2).endVertex();
            }
        }

        BufferUploader.drawWithShader(buffer.end());
        poseStack.popPose();
    }

    /**
     * 渲染底部5个槽位（Billboard朝向相机）
     */
    private static void renderBottomSlots(PoseStack poseStack, MindSeaHoloState state, Camera camera) {
        poseStack.pushPose();

        // 底部槽位位置
        poseStack.translate(0, BOTTOM_SLOT_Y, 0);

        // Billboard旋转
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-camera.getYRot()));
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(camera.getXRot()));

        MindSeaHoloState.OrbData[] selected = state.getSelectedOrbs();

        // 5个槽位，居中排列
        float totalWidth = 5 * SLOT_WIDTH + 4 * SLOT_SPACING;
        float startX = -totalWidth / 2;

        for (int i = 0; i < 5; i++) {
            float slotX = startX + i * (SLOT_WIDTH + SLOT_SPACING) + SLOT_WIDTH / 2;
            renderSlot(poseStack, slotX, 0, selected[i], state.getGroups().get(i).getTitle());
        }

        poseStack.popPose();
    }

    /**
     * 渲染单个槽位
     */
    private static void renderSlot(PoseStack poseStack, float x, float y, MindSeaHoloState.OrbData orb, String title) {
        Minecraft mc = Minecraft.getInstance();
        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();

        float hw = SLOT_WIDTH / 2;
        float hh = SLOT_HEIGHT / 2;

        // 槽位框（如果有选中的光球则高亮）
        boolean hasSelection = (orb != null);
        int bgAlpha = hasSelection ? (int) (MAX_ALPHA * 120) : (int) (MAX_ALPHA * 70);
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        if (hasSelection) {
            // 高亮背景
            buffer.vertex(matrix, x - hw, y - hh, 0).color(80, 120, 110, bgAlpha).endVertex();
            buffer.vertex(matrix, x - hw, y + hh, 0).color(80, 120, 110, bgAlpha).endVertex();
            buffer.vertex(matrix, x + hw, y + hh, 0).color(80, 120, 110, bgAlpha).endVertex();
            buffer.vertex(matrix, x + hw, y - hh, 0).color(80, 120, 110, bgAlpha).endVertex();
        } else {
            // 普通背景
            buffer.vertex(matrix, x - hw, y - hh, 0).color(50, 50, 50, bgAlpha).endVertex();
            buffer.vertex(matrix, x - hw, y + hh, 0).color(50, 50, 50, bgAlpha).endVertex();
            buffer.vertex(matrix, x + hw, y + hh, 0).color(50, 50, 50, bgAlpha).endVertex();
            buffer.vertex(matrix, x + hw, y - hh, 0).color(50, 50, 50, bgAlpha).endVertex();
        }
        BufferUploader.drawWithShader(buffer.end());

        // 槽位边框（选中时更亮）
        buffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        int borderAlpha = hasSelection ? (int) (MAX_ALPHA * 220) : (int) (MAX_ALPHA * 150);
        int br = hasSelection ? 150 : 100;
        int bg = hasSelection ? 230 : 200;
        int bb = hasSelection ? 200 : 180;

        buffer.vertex(matrix, x - hw, y + hh, 0.001f).color(br, bg, bb, borderAlpha).endVertex();
        buffer.vertex(matrix, x + hw, y + hh, 0.001f).color(br, bg, bb, borderAlpha).endVertex();
        buffer.vertex(matrix, x + hw, y + hh, 0.001f).color(br, bg, bb, borderAlpha).endVertex();
        buffer.vertex(matrix, x + hw, y - hh, 0.001f).color(br, bg, bb, borderAlpha).endVertex();
        buffer.vertex(matrix, x + hw, y - hh, 0.001f).color(br, bg, bb, borderAlpha).endVertex();
        buffer.vertex(matrix, x - hw, y - hh, 0.001f).color(br, bg, bb, borderAlpha).endVertex();
        buffer.vertex(matrix, x - hw, y - hh, 0.001f).color(br, bg, bb, borderAlpha).endVertex();
        buffer.vertex(matrix, x - hw, y + hh, 0.001f).color(br, bg, bb, borderAlpha).endVertex();
        BufferUploader.drawWithShader(buffer.end());

        // 渲染组标题（槽位上方）
        poseStack.pushPose();
        poseStack.translate(x, y - hh - 0.08f, 0.002f);
        float titleScale = 0.008f;
        poseStack.scale(-titleScale, -titleScale, titleScale);

        Matrix4f titleMatrix = poseStack.last().pose();
        int titleWidth = mc.font.width(title);
        int titleAlpha = (int) (0.8f * 255);
        int titleColor = (100 << 16) | (200 << 8) | 180 | (titleAlpha << 24);

        mc.font.drawInBatch(title, -titleWidth / 2.0f, 0, titleColor, false,
                           titleMatrix, mc.renderBuffers().bufferSource(),
                           net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, 15728880);
        mc.renderBuffers().bufferSource().endBatch();
        poseStack.popPose();

        // 如果有选中的光球，渲染小图标
        if (orb != null) {
            renderOrbInSlot(poseStack, x, y, orb);
        }
    }

    /**
     * 渲染槽位中的小光球
     */
    private static void renderOrbInSlot(PoseStack poseStack, float x, float y, MindSeaHoloState.OrbData orb) {
        float miniRadius = 0.07f;
        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        int r = (orb.getColor() >> 16) & 0xFF;
        int g = (orb.getColor() >> 8) & 0xFF;
        int b = orb.getColor() & 0xFF;
        int alpha = (int) (MAX_ALPHA * 220);

        buffer.vertex(matrix, x - miniRadius, y - miniRadius, 0.002f).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, x - miniRadius, y + miniRadius, 0.002f).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, x + miniRadius, y + miniRadius, 0.002f).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, x + miniRadius, y - miniRadius, 0.002f).color(r, g, b, alpha).endVertex();

        BufferUploader.drawWithShader(buffer.end());
    }
}
