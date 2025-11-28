package org.example.Kangnaixi.tiandao.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Dual-View Anchored Spell Hand Wheel Renderer (双视角锚定术法手盘渲染器)
 *
 * 渲染在世界空间，支持第一人称和第三人称不同的锚定模式
 *
 * 技术要点：
 * - 【第一人称】屏幕右下角 - 像拿地图一样固定在相机坐标系
 *   * 位置：CameraPos + Right*0.5 + Down*0.4 + Forward*0.8
 *   * 旋转：跟随相机 + 45度翘起
 * - 【第三人称】角色右手旁 - 悬浮在世界空间中
 *   * 位置：PlayerPos + (0,1.0,0) + BodyRight*0.5 + BodyForward*0.4
 *   * 旋转：跟随身体Yaw + 60度倾斜
 * - 卫星物品垂直矫正 - 物品始终垂直于地面（全息投影效果）
 * - 射线交互检测 - 支持鼠标光标点击交互
 */
@Mod.EventBusSubscriber(modid = Tiandao.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SpellHandStarRenderer {

    // ========== 双视角锚定参数 (Dual-View Anchor Parameters) ==========
    // 第一人称 - 相机坐标系偏移
    private static final float FP_OFFSET_RIGHT = 0.8f;           // 右移（屏幕右侧，完全不挡视野）
    private static final float FP_OFFSET_DOWN = -0.6f;           // 下移（沉入屏幕下方）
    private static final float FP_OFFSET_FORWARD = 2.0f;         // 前推（大剑压迫感距离）
    private static final float FP_TILT_ANGLE = 45f;              // 倾斜角度（看手心）
    private static final float FP_SCALE = 1.2f;                  // 第一人称专用缩放（补偿推远的透视缩小）

    // 第三人称 - 世界坐标系偏移
    private static final float TP_OFFSET_HEIGHT = 1.0f;          // 腰部高度
    private static final float TP_OFFSET_RIGHT = 0.5f;           // 右侧距离
    private static final float TP_OFFSET_FORWARD = 0.4f;         // 前方距离
    private static final float TP_TILT_ANGLE = 60f;              // 倾斜角度（盘面朝上）
    private static final float TP_SCALE = 0.8f;                  // 第三人称缩放

    // ========== 视觉布局参数 (Visual Layout) ==========
    private static final float WHEEL_BASE_RADIUS = 0.4f;         // 中心圆盘基础半径
    private static final float ITEM_ORBIT_INNER = 0.28f;         // 物品内轨道半径（手盘内，未选中）
    private static final float ITEM_ORBIT_OUTER = 0.55f;         // 物品外轨道半径（手盘外，选中）
    private static final float ITEM_SCALE_SMALL = 0.12f;         // 物品未选中缩放（很小）
    private static final float ITEM_SCALE_LARGE = 0.22f;         // 物品选中放大（绽放）
    private static final float ITEM_COLLISION_RADIUS = 0.25f;    // 射线碰撞判定半径

    // ========== 动画参数 (Animation) ==========
    private static final int ITEM_BLOOM_DURATION = 250;          // 花朵绽放动画时长(ms)
    private static final float BLOOM_EASE_POWER = 2.0f;          // 绽放ease-out强度

    // ========== 渲染参数 ==========
    private static final int SEGMENTS = 64;                      // 圆环细分数
    private static final float TEXT_SCALE = 0.015f;              // 文字缩放

    // ========== 物品角度分布（局部坐标系：90度均布） ==========
    private static final float[] SECTOR_ANGLES = {
        0f,      // 0 - Right (右侧)
        90f,     // 1 - Top (上方)
        180f,    // 2 - Left (左侧)
        270f     // 3 - Bottom (下方)
    };

    // 颜色
    private static final int COLOR_RING  = 0xFF60AAFF;
    private static final int COLOR_CROSS = 0xFF80BBFF;
    private static final int COLOR_SECTOR_NORMAL = 0x4060AAFF;
    private static final int COLOR_SECTOR_HOVER = 0xCC60FFAA;

    // 状态
    private static boolean enabled = false;

    // ========== 滚轮选择状态 (Scroll Selection State) ==========
    private static int selectedIndex = 0;            // 当前选中的扇区索引 (0-3)

    // ========== 摩天轮动画 (Ferris Wheel Animation) ==========
    private static float currentAngle = 0f;          // 当前盘子旋转角度（平滑插值）
    private static float targetAngle = 0f;           // 目标旋转角度
    private static final float ROTATION_SPEED = 0.15f; // 旋转平滑速度

    // ========== 花朵绽放动画 (Bloom Animation) ==========
    private static float[] itemBloomProgress = {0f, 0f, 0f, 0f}; // 每个扇区的绽放进度 (0-1)

    // 手盘在世界空间的位置（缓存）
    private static Vec3 handWheelWorldPos = Vec3.ZERO;

    // 手盘扇区数据
    public enum WheelSector {
        CORE("源", new ItemStack(Items.MAGMA_CREAM), 0xFFFF5555),      // Core - 岩浆膏 - 红色
        FORM("形态", new ItemStack(Items.BOOK), 0xFF5555FF),           // Form - 书 - 蓝色
        EFFECT("效果", new ItemStack(Items.DIAMOND_SWORD), 0xFF55FF55), // Effect - 钻石剑 - 绿色
        MOD("增强", new ItemStack(Items.GLOWSTONE_DUST), 0xFFFFFF55);  // Mod - 萤石粉 - 黄色

        private final String name;
        private final ItemStack icon;
        private final int color;

        WheelSector(String name, ItemStack icon, int color) {
            this.name = name;
            this.icon = icon;
            this.color = color;
        }

        public String getName() { return name; }
        public ItemStack getIcon() { return icon; }
        public int getColor() { return color; }
    }

    // 扇区数组（按照角度顺序：右上左下）
    // 映射：sectorIndex * 90° = 实际角度
    private static final WheelSector[] SECTORS = {
        WheelSector.MOD,      // 0 - Right (0°)
        WheelSector.FORM,     // 1 - Top (90°)
        WheelSector.EFFECT,   // 2 - Left (180°)
        WheelSector.CORE      // 3 - Bottom (270°)
    };

    // 动画时间
    private static long animationStartTime = System.currentTimeMillis();

    private SpellHandStarRenderer() {}

    public static boolean toggle() {
        Minecraft mc = Minecraft.getInstance();
        enabled = !enabled;

        if (enabled) {
            // 打开手盘：重置选择状态和动画
            animationStartTime = System.currentTimeMillis();
            selectedIndex = 0;
            // 初始状态：让选中扇区立即显示在右侧（0°/3点钟方向）
            targetAngle = -selectedIndex * 90f;  // -0 = 0°
            currentAngle = targetAngle;  // 无动画，直接到位
            Tiandao.LOGGER.info("SpellHandStarRenderer opened: scroll to select");
        } else {
            // 关闭手盘
            Tiandao.LOGGER.info("SpellHandStarRenderer closed: selected sector = " + selectedIndex);
        }

        return enabled;
    }

    public static boolean isEnabled() { return enabled; }
    public static int getSelectedIndex() { return selectedIndex; }

    /**
     * 处理鼠标滚轮事件 - 切换选择扇区并触发旋转动画
     *
     * 摩天轮模式：
     * - 改变selectedIndex，更新targetAngle
     * - 使用Math.floorMod确保索引在0-3范围内（解决负数问题）
     * - 渲染时盘子会平滑旋转到目标角度，让选中项转到最上方
     *
     * @param scrollDelta 滚轮增量（正数=向上滚，负数=向下滚）
     * @return true表示事件已处理
     */
    public static boolean handleMouseScroll(double scrollDelta) {
        if (!enabled) return false;

        Minecraft mc = Minecraft.getInstance();

        if (scrollDelta > 0) {
            // 滚轮向上 - 逆时针选择上一个（减少索引）
            selectedIndex = Math.floorMod(selectedIndex - 1, 4);
        } else if (scrollDelta < 0) {
            // 滚轮向下 - 顺时针选择下一个（增加索引）
            selectedIndex = Math.floorMod(selectedIndex + 1, 4);
        } else {
            return false;
        }

        // 更新目标旋转角度：让选中的扇区旋转到右侧（0°/3点钟方向）
        targetAngle = -selectedIndex * 90f;

        // 播放UI点击音效（音调随扇区变化）
        mc.player.playSound(
            net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(),
            0.5f,  // 音量
            1.2f + selectedIndex * 0.1f  // 音调
        );

        Tiandao.LOGGER.debug("Scroll selection changed: selectedIndex={}, targetAngle={}", selectedIndex, targetAngle);
        return true;
    }

    /**
     * 处理鼠标点击 - 确认选择
     */
    public static boolean handleClick() {
        if (!enabled) return false;

        Minecraft mc = Minecraft.getInstance();
        WheelSector selected = SECTORS[selectedIndex];

        mc.player.playSound(
            net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,
            0.8f,
            1.0f
        );

        Tiandao.LOGGER.info("Confirmed selection: {} ({})", selected.getName(), selectedIndex);
        return true;
    }

    /**
     * 世界空间渲染 - 双视角锚定模式 (Dual-View Anchoring Mode)
     *
     * 数学模型：
     * 【第一人称】屏幕右下角 - 像拿地图一样
     * 1. 位置 = CameraPos + CameraRight*0.5 + CameraDown*0.4 + CameraForward*0.8
     * 2. 旋转 = 跟随相机 + X轴倾斜45度
     *
     * 【第三人称】角色右手旁 - 世界空间悬浮
     * 1. 位置 = PlayerPos + (0,1.0,0) + BodyRight*0.5 + BodyForward*0.4
     * 2. 旋转 = 跟随身体Yaw
     */
    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        if (!enabled) return;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Camera camera = event.getCamera();
        PoseStack poseStack = event.getPoseStack();
        float partialTick = event.getPartialTick();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        // ========== 双视角判断 ==========
        boolean isFirstPerson = mc.options.getCameraType().isFirstPerson();

        // 用于射线检测的局部坐标系基向量
        Vec3 wheelRight, wheelUp, wheelForward;
        float rotationYaw;  // Y轴旋转角度
        float rotationPitch = 0f;  // X轴倾斜角度

        if (isFirstPerson) {
            // ========== 第一人称模式：屏幕右下角（相机坐标系） ==========

            // 1.1 获取相机坐标系基向量
            Vec3 cameraPos = camera.getPosition();
            Vec3 cameraLook = new Vec3(camera.getLookVector());
            Vec3 cameraUp = new Vec3(camera.getUpVector());
            Vec3 cameraRight = cameraLook.cross(cameraUp).normalize();

            // 1.2 计算手盘世界位置：右下前
            handWheelWorldPos = cameraPos
                .add(cameraRight.scale(FP_OFFSET_RIGHT))      // 右移
                .add(cameraUp.scale(FP_OFFSET_DOWN))          // 下移
                .add(cameraLook.scale(FP_OFFSET_FORWARD));    // 前推

            // 1.3 记录局部坐标系（用于射线检测）
            wheelRight = cameraRight;
            wheelUp = cameraUp;
            wheelForward = cameraLook;

            // 1.4 旋转参数：使用相机朝向
            rotationYaw = -camera.getYRot();
            rotationPitch = FP_TILT_ANGLE;  // 稍微翘起，像看手心

        } else {
            // ========== 第三人称模式：角色右手旁（世界坐标系） ==========

            // 2.1 玩家身体位置和朝向
            Vec3 playerPos = mc.player.getPosition(partialTick);
            float bodyYaw = net.minecraft.util.Mth.lerp(partialTick, mc.player.yBodyRotO, mc.player.yBodyRot);
            float bodyYawRad = (float) Math.toRadians(-bodyYaw);

            // 2.2 计算身体坐标系基向量
            Vec3 bodyForward = new Vec3(Math.sin(bodyYawRad), 0, Math.cos(bodyYawRad));
            Vec3 bodyRight = new Vec3(Math.cos(bodyYawRad), 0, -Math.sin(bodyYawRad));
            Vec3 bodyUp = new Vec3(0, 1, 0);

            // 2.3 计算手盘世界位置：腰部 + 右侧 + 稍前
            handWheelWorldPos = playerPos
                .add(0, TP_OFFSET_HEIGHT, 0)                  // 腰部高度
                .add(bodyRight.scale(TP_OFFSET_RIGHT))        // 右侧
                .add(bodyForward.scale(TP_OFFSET_FORWARD));   // 稍靠前

            // 2.4 记录局部坐标系（用于射线检测）
            wheelRight = bodyRight;
            wheelUp = bodyUp;
            wheelForward = bodyForward;

            // 2.5 旋转参数：跟随身体
            rotationYaw = -bodyYaw + 180f;
            rotationPitch = TP_TILT_ANGLE;  // 第三人称稍微倾斜更多
        }

        // ========== 步骤2：更新摩天轮动画（最短路径插值） ==========
        // 关键：处理角度周期性，避免从 0° -> 270° 时走长路径
        // 例如：0 -> 3 应该逆时针转 -90°，而不是顺时针转 270°

        // 计算角度差
        float angleDiff = targetAngle - currentAngle;

        // 标准化到 [-180, 180] 范围（最短路径）
        while (angleDiff < -180f) angleDiff += 360f;
        while (angleDiff > 180f) angleDiff -= 360f;

        // 平滑插值：使用差值而不是直接 lerp 目标角度
        currentAngle = net.minecraft.util.Mth.lerp(ROTATION_SPEED, currentAngle, currentAngle + angleDiff);

        // ========== 步骤3：渲染手盘背景（Background Layer with Rotation） ==========
        poseStack.pushPose();

        // 3.1 平移到手盘中心（相对于渲染相机）
        Vec3 cameraPos = camera.getPosition();
        Vec3 renderOffset = handWheelWorldPos.subtract(cameraPos);
        poseStack.translate(renderOffset.x, renderOffset.y, renderOffset.z);

        // 3.2 旋转1：Y轴旋转（跟随相机或身体）
        poseStack.mulPose(Axis.YP.rotationDegrees(rotationYaw));

        // 3.3 旋转2：X轴倾斜（盘面朝上）
        poseStack.mulPose(Axis.XP.rotationDegrees(rotationPitch));

        // 3.4 旋转3：Z轴旋转（摩天轮动画 - 让选中项转到左侧）
        poseStack.mulPose(Axis.ZP.rotationDegrees(currentAngle));

        // 3.5 缩放：根据视角应用不同的缩放（第一人称更大以补偿推远的透视）
        float scale = isFirstPerson ? FP_SCALE : TP_SCALE;
        poseStack.scale(scale, scale, scale);

        // ========== 渲染盘子几何体 ==========
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);

        // 绘制4个扇区背景（统一颜色，不高亮，避免眼晕）
        for (int i = 0; i < 4; i++) {
            drawSector3D(poseStack, WHEEL_BASE_RADIUS, i, COLOR_SECTOR_NORMAL);
        }

        // 绘制圆环和十字分割线
        drawCircle3D(poseStack, WHEEL_BASE_RADIUS, SEGMENTS, COLOR_RING);
        drawCrossLines(poseStack, WHEEL_BASE_RADIUS, COLOR_CROSS);

        // ========== 步骤4：摩天轮渲染物品图标（Ferris Wheel Rendering） ==========
        // 关键：物品跟随盘子旋转，但通过反向修正保持正立
        ItemRenderer itemRenderer = mc.getItemRenderer();

        // 渲染4个物品 + 选中项的文字（寄生模式）
        for (int i = 0; i < 4; i++) {
            WheelSector sector = SECTORS[i];
            boolean isSelected = (i == selectedIndex);

            // 更新花朵绽放进度：选中=1.0，未选中=0.0，平滑过渡（阻尼效果）
            float targetProgress = isSelected ? 1.0f : 0.0f;
            float bloomSpeed = 0.1f; // 绽放速度（降低速度，增强阻尼感）
            itemBloomProgress[i] += (targetProgress - itemBloomProgress[i]) * bloomSpeed;

            // 寄生渲染：文字直接在物品坐标系内偏移，不重复计算角度
            renderFerrisWheelItem(poseStack, bufferSource, itemRenderer, sector, i, isSelected, currentAngle, itemBloomProgress[i]);
        }

        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();

        poseStack.popPose();

        // Flush buffers
        bufferSource.endBatch();
    }

    /**
     * 获取扇区中心角度（与物品角度映射一致）
     */
    private static double getSectorAngle(int sector) {
        switch (sector) {
            case 0: return 0.0;                 // Right (0°)
            case 1: return Math.PI / 2.0;       // Top (90°)
            case 2: return Math.PI;             // Left (180°)
            case 3: return Math.PI * 3.0 / 2;   // Bottom (270°)
            default: return 0.0;
        }
    }

    /**
     * 绘制扇形高亮
     */
    private static void drawSector3D(PoseStack poseStack, float radius, int sector, int argb) {
        double startAngle = getSectorAngle(sector) - Math.PI / 4.0;
        double endAngle = startAngle + Math.PI / 2.0;

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableCull();

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.getBuilder();
        builder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);

        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        float a = ((argb >> 24) & 0xFF) / 255f;
        Matrix4f pose = poseStack.last().pose();

        // 中心点
        builder.vertex(pose, 0f, 0f, 0f).color(r, g, b, a * 0.9f).endVertex();

        // 扇形边缘
        int arcSegments = SEGMENTS / 4;
        for (int i = 0; i <= arcSegments; i++) {
            double t = (double) i / arcSegments;
            double angle = startAngle + (endAngle - startAngle) * t;
            float x = (float) Math.cos(angle) * radius;
            float y = (float) Math.sin(angle) * radius;
            builder.vertex(pose, x, y, 0f).color(r, g, b, a * 0.5f).endVertex();
        }

        tesselator.end();
    }

    /**
     * 绘制圆环
     */
    private static void drawCircle3D(PoseStack poseStack, float radius, int segments, int argb) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableCull();
        RenderSystem.lineWidth(3.0f);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.getBuilder();
        builder.begin(VertexFormat.Mode.LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        float a = ((argb >> 24) & 0xFF) / 255f;
        Matrix4f pose = poseStack.last().pose();

        for (int i = 0; i <= segments; i++) {
            double angle = (Math.PI * 2 * i) / segments;
            float x = (float) Math.cos(angle) * radius;
            float y = (float) Math.sin(angle) * radius;
            builder.vertex(pose, x, y, 0f).color(r, g, b, a).endVertex();
        }

        tesselator.end();
        RenderSystem.lineWidth(1.0f);
    }

    /**
     * 绘制十字分割线
     */
    private static void drawCrossLines(PoseStack poseStack, float radius, int argb) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.lineWidth(2.0f);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.getBuilder();
        builder.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR);

        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        float a = ((argb >> 24) & 0xFF) / 255f;
        Matrix4f pose = poseStack.last().pose();

        // 横线
        builder.vertex(pose, -radius, 0f, 0f).color(r, g, b, a).endVertex();
        builder.vertex(pose,  radius, 0f, 0f).color(r, g, b, a).endVertex();
        // 竖线
        builder.vertex(pose, 0f, -radius, 0f).color(r, g, b, a).endVertex();
        builder.vertex(pose, 0f,  radius, 0f).color(r, g, b, a).endVertex();

        tesselator.end();
        RenderSystem.lineWidth(1.0f);
    }

    /**
     * 寄生式渲染物品+文字 - Parasitic Rendering with Bloom Animation
     *
     * 关键：文字不再单独计算位置，而是直接寄生在物品坐标系内
     *
     * 数学原理：
     * - 物品先跟着盘子转动（继承了父PoseStack的currentAngle旋转）
     * - 反向旋转 -(wheelAngle + sectorAngle) 让物品竖直
     * - 此时坐标系已经正立，文字只需简单偏移 (0, 0.35, 0) 即可显示在物品上方
     *
     * @param wheelAngle 当前盘子的旋转角度（摩天轮的转动量）
     * @param bloomProgress 花朵绽放进度 (0=缩小, 1=放大)
     */
    private static void renderFerrisWheelItem(PoseStack poseStack, MultiBufferSource bufferSource,
                                              ItemRenderer itemRenderer, WheelSector sector,
                                              int sectorIndex, boolean isSelected, float wheelAngle, float bloomProgress) {
        poseStack.pushPose();

        // 1. 定位到扇区角度（物品在盘子上的固定位置）
        float sectorAngle = sectorIndex * 90f;  // 0→0°, 1→90°, 2→180°, 3→270°
        poseStack.mulPose(Axis.ZP.rotationDegrees(sectorAngle));

        // 2. 向外推到轨道半径（根据绽放进度动态插值）
        // ease-out曲线
        float easeProgress = 1.0f - (float)Math.pow(1.0f - bloomProgress, BLOOM_EASE_POWER);
        // 轨道半径：从内轨道(0.28)移动到外轨道(0.55)
        float orbitRadius = ITEM_ORBIT_INNER + (ITEM_ORBIT_OUTER - ITEM_ORBIT_INNER) * easeProgress;
        poseStack.translate(orbitRadius, 0, 0.01f);

        // 3. === 摩天轮修正 (Ferris Wheel Counter-Rotation) ===
        // 抵消两部分旋转：
        // A. 盘子当前的转动 (wheelAngle)
        // B. 物品所在的扇区角度 (sectorAngle)
        // 让物品始终保持正立（头朝上）
        poseStack.mulPose(Axis.ZP.rotationDegrees(-(wheelAngle + sectorAngle)));

        // === 此时坐标系已经正立，原点在物品中心 ===

        // 4. === 寄生式文字渲染（在缩放之前，避免继承物品缩放） ===
        if (isSelected) {
            poseStack.pushPose();

            // A. 局部偏移 (Local Offset) - 统一使用X轴径向外推
            // 和源、形态一样的逻辑
            poseStack.translate(0.3, 0, 0.03);

            // B. Billboard 面向玩家 (Face Player)
            poseStack.mulPose(Axis.YP.rotationDegrees(180f));
            poseStack.mulPose(Axis.ZP.rotationDegrees(180f));

            // C. 缩放与绘制 - 关键：使用负X缩放修正镜像！
            float textScale = 0.02f;
            poseStack.scale(-textScale, textScale, textScale);  // 负X修正镜像

            Minecraft mc = Minecraft.getInstance();
            Font font = mc.font;
            String text = sector.getName();
            Matrix4f matrix = poseStack.last().pose();
            float textWidth = font.width(text);

            // 绘制文字（黄色高亮）
            font.drawInBatch(text, -textWidth / 2f, -font.lineHeight / 2f, 0xFFFFFF00, false,
                            matrix, bufferSource, Font.DisplayMode.NORMAL, 0, 0xF000F0);

            poseStack.popPose();
        }

        // 5. 花朵绽放动画缩放：选中=放大，未选中=缩小
        // 缩放因子：从小(0.12)到大(0.22)，使用前面计算的 easeProgress
        float animatedScale = ITEM_SCALE_SMALL + (ITEM_SCALE_LARGE - ITEM_SCALE_SMALL) * easeProgress;

        poseStack.scale(animatedScale, animatedScale, animatedScale);

        // 6. 渲染物品
        itemRenderer.renderStatic(
            sector.getIcon(),
            ItemDisplayContext.GUI,  // GUI模式，适合2D图标
            15728880,  // Full brightness
            OverlayTexture.NO_OVERLAY,
            poseStack,
            bufferSource,
            null,
            0
        );

        poseStack.popPose();
    }

    /**
     * 分离渲染物品图标 - Decoupled Rendering（已废弃，保留作为参考）
     *
     * 关键：物品不继承盘子的旋转，独立渲染在固定位置
     *
     * 布局：
     * - index 0: 上方 (90度)
     * - index 1: 右方 (0度)
     * - index 2: 下方 (270度)
     * - index 3: 左方 (180度)
     *
     * 选中效果：缩放放大，未选中缩小
     */
    private static void renderDecoupledItem(PoseStack poseStack, MultiBufferSource bufferSource,
                                           ItemRenderer itemRenderer, WheelSector sector,
                                           int sectorIndex, boolean isSelected) {
        poseStack.pushPose();

        // 1. 固定角度分布（上右下左）
        float angle = sectorIndex * 90f;  // 0→0°, 1→90°, 2→180°, 3→270°

        // 2. 绕Z轴旋转到对应扇区
        poseStack.mulPose(Axis.ZP.rotationDegrees(angle));

        // 3. 向外推（半径缩小，防止溢出）
        float iconRadius = 0.4f;
        poseStack.translate(iconRadius, 0, 0.01f);

        // 4. 反向旋转Z轴 - 让物品立刻回正，头朝上
        poseStack.mulPose(Axis.ZP.rotationDegrees(-angle));

        // === 关键：现在物品是正立的，不受盘子旋转影响 ===

        // 5. 选中高亮：通过缩放表现
        float iconScale;
        if (isSelected) {
            iconScale = 0.35f;  // 选中放大
        } else {
            iconScale = 0.25f;  // 未选中缩小
        }
        poseStack.scale(iconScale, iconScale, iconScale);

        // 6. 渲染物品
        itemRenderer.renderStatic(
            sector.getIcon(),
            ItemDisplayContext.GUI,  // GUI模式，适合2D图标
            15728880,  // Full brightness
            OverlayTexture.NO_OVERLAY,
            poseStack,
            bufferSource,
            null,
            0
        );

        poseStack.popPose();
    }

    /**
     * 分离渲染文字标签 - Decoupled Rendering
     *
     * 只在选中项显示，不受盘子旋转影响
     */
    private static void renderDecoupledLabel(PoseStack poseStack, MultiBufferSource bufferSource,
                                            String text, int sectorIndex) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        poseStack.pushPose();

        // 1. 绕Z轴旋转定位（与物品位置对应）
        float angle = sectorIndex * 90f;
        poseStack.mulPose(Axis.ZP.rotationDegrees(angle));

        // 2. 向外推到物品外侧
        float labelRadius = 0.6f;
        poseStack.translate(labelRadius, 0, 0.02f);

        // 3. 反向旋转Z轴（让文字正立）
        poseStack.mulPose(Axis.ZP.rotationDegrees(-angle));

        // 4. 旋转修正：面向玩家 + 修正上下颠倒 + 修正左右镜像
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0f));

        float labelScale = TEXT_SCALE * 1.3f;
        poseStack.scale(-labelScale, labelScale, labelScale);

        Matrix4f matrix = poseStack.last().pose();
        float textWidth = font.width(text);

        // 绘制文字（黄色高亮）
        font.drawInBatch(text, -textWidth / 2f, 0f, 0xFFFFFF00, false,
                        matrix, bufferSource, Font.DisplayMode.NORMAL, 0, 0xF000F0);

        poseStack.popPose();
    }

    /**
     * 摩天轮渲染文字标签 - 文字跟随物品运动
     *
     * 关键：文字是物品的附属节点，使用和物品完全相同的摩天轮逻辑
     * 像摩天轮座舱一样，跟着轮子转，但自身反向旋转保持正立
     *
     * @param wheelAngle 当前盘子的旋转角度（摩天轮的转动量）
     */
    private static void renderFerrisWheelLabel(PoseStack poseStack, MultiBufferSource bufferSource,
                                               String text, int sectorIndex, float wheelAngle) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        poseStack.pushPose();

        // --- A. 定位阶段 (Positioning) ---
        // 1. 旋转到扇区角度 (0, 90, 180, 270)
        float sectorAngle = sectorIndex * 90f;
        poseStack.mulPose(Axis.ZP.rotationDegrees(sectorAngle));

        // 2. 向外推到文字轨道 (比物品半径稍大，例如 0.65)
        // Z轴微调 0.03 防止遮挡
        poseStack.translate(0.65f, 0, 0.03f);

        // --- B. 姿态修正阶段 (Orientation Fix) ---
        // 3. 摩天轮反向旋转 (关键！)
        // 必须抵消：盘子的当前转动(wheelAngle) + 扇区角度(sectorAngle)
        // 结果：无论盘子转到哪，文字坐标系永远相对于屏幕"水平正立"
        poseStack.mulPose(Axis.ZP.rotationDegrees(-(wheelAngle + sectorAngle)));

        // 4. Billboard 面向玩家 (Face Player)
        // 修正 Minecraft 文字渲染的默认倒置和背面剔除问题
        poseStack.mulPose(Axis.YP.rotationDegrees(180f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(180f));

        // --- C. 绘制阶段 (Draw) ---
        // 5. 缩放
        float s = 0.02f;
        poseStack.scale(s, s, s);

        // 6. 绘制文字 (居中)
        Matrix4f matrix = poseStack.last().pose();
        float textWidth = font.width(text);
        int color = 0xFFFFFFFF;  // 白色

        font.drawInBatch(text, -textWidth / 2f, -font.lineHeight / 2f, color, false,
                        matrix, bufferSource, Font.DisplayMode.NORMAL, 0, 0xF000F0);

        poseStack.popPose();
    }

    /**
     * 渲染扇区文字标签 - 仅选中时显示 + 归位动画抵消（已废弃，保留作为参考）
     */
    private static void renderSectorLabel(PoseStack poseStack, MultiBufferSource bufferSource,
                                         String text, int sectorIndex, float wheelAngle) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        poseStack.pushPose();

        // 1. 绕Z轴旋转定位（与物品位置对应）
        float angle = sectorIndex * 90f;
        poseStack.mulPose(Axis.ZP.rotationDegrees(angle));

        // 2. 向外推到物品外侧
        float labelRadius = ITEM_ORBIT_OUTER + 0.25f;
        poseStack.translate(labelRadius, 0, 0.03f);

        // 3. 反向旋转Z轴
        poseStack.mulPose(Axis.ZP.rotationDegrees(-angle));

        // 4. 抵消盘子的归位旋转（让文字始终保持正立）
        poseStack.mulPose(Axis.ZP.rotationDegrees(-wheelAngle));

        // 5. 旋转修正：面向玩家 + 修正上下颠倒 + 修正左右镜像
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0f));

        float labelScale = TEXT_SCALE * 1.3f;
        poseStack.scale(-labelScale, labelScale, labelScale);

        Matrix4f matrix = poseStack.last().pose();
        float textWidth = font.width(text);

        // 绘制文字（黄色高亮）
        font.drawInBatch(text, -textWidth / 2f, 0f, 0xFFFFFF00, false,
                        matrix, bufferSource, Font.DisplayMode.NORMAL, 0, 0xF000F0);

        poseStack.popPose();
    }

    /**
     * 绘制3D文字（备用，保留原有逻辑）
     */
    private static void draw3DText(PoseStack poseStack, MultiBufferSource bufferSource,
                                   String text, double angle, float radius, float scale, int color) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        float x = (float) Math.cos(angle) * radius;
        float y = (float) Math.sin(angle) * radius;

        poseStack.pushPose();
        poseStack.translate(x, y, 0.001f);  // 略微提升避免Z-fighting

        // 1. 面向玩家（Y轴旋转180度）
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));

        // 2. 修正Minecraft文字渲染的上下颠倒问题（Z轴旋转180度）
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0f));

        // 3. 缩放：负X修正左右镜像，Y和Z保持正数
        poseStack.scale(-scale, scale, scale);

        Matrix4f matrix = poseStack.last().pose();
        float textWidth = font.width(text);

        // 绘制文字（带背景提高可读性）
        font.drawInBatch(text, -textWidth / 2f, 0f, color, false,
                        matrix, bufferSource, Font.DisplayMode.NORMAL, 0, 0xF000F0);

        poseStack.popPose();
    }
}
