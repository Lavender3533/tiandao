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
import org.example.Kangnaixi.tiandao.client.starchart.StarChartClientManager;
import org.example.Kangnaixi.tiandao.starchart.StarNodeCategory;
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

    // ========== 物品角度分布（局部坐标系：120度均布，3扇区） ==========
    private static final float[] SECTOR_ANGLES = {
        0f,      // 0 - Right (右侧) - 调制
        120f,    // 1 - Top-Left (左上) - 形态
        240f     // 2 - Bottom-Left (左下) - 效果
    };

    // 颜色
    private static final int COLOR_RING  = 0xFF60AAFF;
    private static final int COLOR_CROSS = 0x4080BBFF;  // 降低分割线透明度（从 0xFF 改为 0x40）
    private static final int COLOR_SECTOR_NORMAL = 0xA060AAFF;  // 增加扇区不透明度（从 0x40 改为 0xA0）
    private static final int COLOR_SECTOR_HOVER = 0xCC60FFAA;
    private static final int COLOR_SOURCE_CENTER = 0xFFFFAA00;  // 中心源的颜色（金色）

    // 状态
    private static boolean enabled = false;

    // ========== 滚轮选择状态 (Scroll Selection State) ==========
    private static int selectedIndex = 0;            // 当前选中的扇区索引 (0-2)

    // ========== 源选择状态 (Source Selection State) ==========
    private static int selectedSourceIndex = 0;      // 当前选中的源类型索引

    // ========== 摩天轮动画 (Ferris Wheel Animation) ==========
    private static float currentAngle = 0f;          // 当前盘子旋转角度（平滑插值）
    private static float targetAngle = 0f;           // 目标旋转角度
    private static final float ROTATION_SPEED = 0.15f; // 旋转平滑速度

    // ========== 花朵绽放动画 (Bloom Animation) ==========
    private static float[] itemBloomProgress = {0f, 0f, 0f}; // 每个扇区的绽放进度 (0-1)，改为3个

    // ========== 源切换动画 ==========
    private static float sourceBloomProgress = 0f;   // 源图标绽放进度

    // 手盘在世界空间的位置（缓存）
    private static Vec3 handWheelWorldPos = Vec3.ZERO;

    // 手盘外圈扇区数据（3个，不含源）
    public enum WheelSector {
        FORM("形态", new ItemStack(Items.BOOK), 0xFF87CEEB, StarNodeCategory.FORM),               // 形态 - 书 - 天蓝色
        EFFECT("效果", new ItemStack(Items.DIAMOND_SWORD), 0xFFFF6347, StarNodeCategory.EFFECT),  // 效果 - 钻石剑 - 番茄红
        MODIFIER("调制", new ItemStack(Items.GLOWSTONE_DUST), 0xFF9370DB, StarNodeCategory.MODIFIER); // 调制 - 萤石粉 - 紫色

        private final String name;
        private final ItemStack icon;
        private final int color;
        private final StarNodeCategory linkedCategory;  // 关联的星宫类别

        WheelSector(String name, ItemStack icon, int color, StarNodeCategory linkedCategory) {
            this.name = name;
            this.icon = icon;
            this.color = color;
            this.linkedCategory = linkedCategory;
        }

        public String getName() { return name; }
        public ItemStack getIcon() { return icon; }
        public int getColor() { return color; }
        public StarNodeCategory getLinkedCategory() { return linkedCategory; }
    }

    // 中心源类型
    public enum SourceType {
        SELF("自身", new ItemStack(Items.NETHER_STAR), 0xFFFFAA00),        // 自身灵力 - 下界之星 - 金色
        ARRAY("阵盘", new ItemStack(Items.LODESTONE), 0xFF00AAFF),         // 阵盘 - 磁石 - 蓝色
        ITEM("灵物", new ItemStack(Items.AMETHYST_SHARD), 0xFFAA00FF);     // 背包灵物 - 紫水晶碎片 - 紫色

        private final String name;
        private final ItemStack icon;
        private final int color;

        SourceType(String name, ItemStack icon, int color) {
            this.name = name;
            this.icon = icon;
            this.color = color;
        }

        public String getName() { return name; }
        public ItemStack getIcon() { return icon; }
        public int getColor() { return color; }
    }

    // 外圈扇区数组（3个，120度均布）
    private static final WheelSector[] SECTORS = {
        WheelSector.MODIFIER,   // 0 - Right (0°)
        WheelSector.FORM,       // 1 - Top-Left (120°)
        WheelSector.EFFECT      // 2 - Bottom-Left (240°)
    };

    // 源类型数组
    private static final SourceType[] SOURCES = SourceType.values();

    // 动画时间
    private static long animationStartTime = System.currentTimeMillis();

    private SpellHandStarRenderer() {}

    public static boolean toggle() {
        Minecraft mc = Minecraft.getInstance();
        enabled = !enabled;

        StarChartClientManager starChart = StarChartClientManager.getInstance();

        if (enabled) {
            // 打开手盘：重置选择状态和动画
            animationStartTime = System.currentTimeMillis();
            selectedIndex = 0;
            selectedSourceIndex = 0;
            // 初始状态：让选中扇区立即显示在右侧（0°/3点钟方向）
            targetAngle = -selectedIndex * 120f;  // 3扇区，每个120度
            currentAngle = targetAngle;  // 无动画，直接到位

            // ========== 同步开关：联动打开星盘 ==========
            if (!starChart.isEnabled()) {
                starChart.toggle();
                Tiandao.LOGGER.info("HandWheel opened → StarChart synced open");
            }

            Tiandao.LOGGER.info("SpellHandStarRenderer opened: scroll to select");
        } else {
            // ========== 同步开关：联动关闭星盘 ==========
            if (starChart.isEnabled()) {
                starChart.toggle();
                Tiandao.LOGGER.info("HandWheel closed → StarChart synced close");
            }

            Tiandao.LOGGER.info("SpellHandStarRenderer closed: selected sector = " + selectedIndex);
        }

        return enabled;
    }

    public static boolean isEnabled() { return enabled; }
    public static int getSelectedIndex() { return selectedIndex; }
    public static int getSelectedSourceIndex() { return selectedSourceIndex; }
    public static SourceType getSelectedSource() { return SOURCES[selectedSourceIndex]; }

    /**
     * 处理鼠标滚轮事件
     *
     * 交互模式：
     * - 普通滚轮：切换外圈扇区（效果/形态/调制），联动星盘
     * - 蹲下+滚轮：切换中心源类型（自身/阵盘/灵物），不联动星盘
     *
     * @param scrollDelta 滚轮增量（正数=向上滚，负数=向下滚）
     * @return true表示事件已处理
     */
    public static boolean handleMouseScroll(double scrollDelta) {
        if (!enabled) return false;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;

        // 检测是否蹲下
        boolean isSneaking = mc.player.isShiftKeyDown();

        if (isSneaking) {
            // ========== 蹲下+滚轮：切换源类型 ==========
            return handleSourceScroll(scrollDelta, mc);
        } else {
            // ========== 普通滚轮：切换外圈扇区 ==========
            return handleSectorScroll(scrollDelta, mc);
        }
    }

    /**
     * 处理外圈扇区滚轮切换
     */
    private static boolean handleSectorScroll(double scrollDelta, Minecraft mc) {
        if (scrollDelta > 0) {
            selectedIndex = Math.floorMod(selectedIndex - 1, 3);  // 3扇区
        } else if (scrollDelta < 0) {
            selectedIndex = Math.floorMod(selectedIndex + 1, 3);
        } else {
            return false;
        }

        // 更新目标旋转角度：3扇区，每个120度
        targetAngle = -selectedIndex * 120f;

        // 播放UI点击音效
        mc.player.playSound(
            net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(),
            0.5f, 1.2f + selectedIndex * 0.1f
        );

        // 联动星盘
        triggerStarChartFocus();

        Tiandao.LOGGER.debug("Sector scroll: index={}, angle={}", selectedIndex, targetAngle);
        return true;
    }

    /**
     * 处理中心源类型滚轮切换
     */
    private static boolean handleSourceScroll(double scrollDelta, Minecraft mc) {
        if (scrollDelta > 0) {
            selectedSourceIndex = Math.floorMod(selectedSourceIndex - 1, SOURCES.length);
        } else if (scrollDelta < 0) {
            selectedSourceIndex = Math.floorMod(selectedSourceIndex + 1, SOURCES.length);
        } else {
            return false;
        }

        // 重置源绽放动画
        sourceBloomProgress = 0f;

        // 播放不同的音效（区分源切换）
        mc.player.playSound(
            net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,
            0.4f, 1.5f + selectedSourceIndex * 0.2f
        );

        Tiandao.LOGGER.debug("Source scroll: {} (index={})",
            SOURCES[selectedSourceIndex].getName(), selectedSourceIndex);
        return true;
    }

    /**
     * 触发星盘聚焦到当前选中扇区对应的星宫
     *
     * 联动逻辑：
     * - 如果星盘已开启，自动进入聚焦状态展开对应星宫
     * - 如果星盘未开启，不做任何操作（手盘可独立使用）
     */
    private static void triggerStarChartFocus() {
        StarChartClientManager starChart = StarChartClientManager.getInstance();

        // 只在星盘已开启时联动
        if (!starChart.isEnabled()) {
            return;
        }

        WheelSector currentSector = SECTORS[selectedIndex];
        StarNodeCategory targetCategory = currentSector.getLinkedCategory();

        if (targetCategory != null) {
            // 触发星盘聚焦到对应星宫
            starChart.enterFocusState(targetCategory);
            Tiandao.LOGGER.debug("HandWheel→StarChart linked: {} → {}",
                currentSector.getName(), targetCategory.getDisplayName());
        }
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

        // 绘制3个扇区背景（120度均布）
        for (int i = 0; i < 3; i++) {
            drawSector3D_120(poseStack, WHEEL_BASE_RADIUS, i, COLOR_SECTOR_NORMAL);
        }

        // 绘制圆环和三分割线
        drawCircle3D(poseStack, WHEEL_BASE_RADIUS, SEGMENTS, COLOR_RING);
        drawTriLines(poseStack, WHEEL_BASE_RADIUS, COLOR_CROSS);

        // ========== 步骤4：渲染外圈物品图标（3个扇区） ==========
        ItemRenderer itemRenderer = mc.getItemRenderer();

        for (int i = 0; i < 3; i++) {
            WheelSector sector = SECTORS[i];
            boolean isSelected = (i == selectedIndex);

            // 更新花朵绽放进度
            float targetProgress = isSelected ? 1.0f : 0.0f;
            float bloomSpeed = 0.1f;
            itemBloomProgress[i] += (targetProgress - itemBloomProgress[i]) * bloomSpeed;

            // 渲染外圈物品（120度均布）
            renderFerrisWheelItem_120(poseStack, bufferSource, itemRenderer, sector, i, isSelected, currentAngle, itemBloomProgress[i]);
        }

        // ========== 步骤5：渲染中心源图标 ==========
        sourceBloomProgress += (1.0f - sourceBloomProgress) * 0.1f;  // 源始终显示
        renderCenterSource(poseStack, bufferSource, itemRenderer, currentAngle);

        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();

        poseStack.popPose();

        // Flush buffers
        bufferSource.endBatch();
    }

    /**
     * 获取扇区中心角度（3扇区，120度均布）
     */
    private static double getSectorAngle_120(int sector) {
        return sector * Math.PI * 2.0 / 3.0;  // 0°, 120°, 240°
    }

    /**
     * 绘制扇形（3扇区，120度 + 重叠消除缝隙）
     */
    private static void drawSector3D_120(PoseStack poseStack, float radius, int sector, int argb) {
        double centerAngle = getSectorAngle_120(sector);
        // 增加扇区角度范围：从 ±60° 改为 ±61°，让扇区稍微重叠消除缝隙
        double startAngle = centerAngle - Math.PI / 3.0 - 0.02;  // -60° - 1°
        double endAngle = centerAngle + Math.PI / 3.0 + 0.02;    // +60° + 1°

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

        // 中心点（从圆心开始，不留空）
        builder.vertex(pose, 0f, 0f, 0f).color(r, g, b, a * 0.8f).endVertex();

        // 扇形边缘
        int arcSegments = SEGMENTS / 3;
        for (int i = 0; i <= arcSegments; i++) {
            double t = (double) i / arcSegments;
            double angle = startAngle + (endAngle - startAngle) * t;
            float x = (float) Math.cos(angle) * radius;
            float y = (float) Math.sin(angle) * radius;
            builder.vertex(pose, x, y, 0f).color(r, g, b, a).endVertex();
        }

        tesselator.end();
    }

    /**
     * 绘制三分割线（替代十字线）
     */
    private static void drawTriLines(PoseStack poseStack, float radius, int argb) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.lineWidth(1.0f);  // 减小线宽（从 2.0f 改为 1.0f）

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.getBuilder();
        builder.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR);

        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        float a = ((argb >> 24) & 0xFF) / 255f;
        Matrix4f pose = poseStack.last().pose();

        // 从中心向外画3条线（60°, 180°, 300°）
        float innerRadius = 0.08f;
        for (int i = 0; i < 3; i++) {
            double angle = i * Math.PI * 2.0 / 3.0 + Math.PI / 3.0;  // 偏移60度，在扇区边界
            float x1 = (float) Math.cos(angle) * innerRadius;
            float y1 = (float) Math.sin(angle) * innerRadius;
            float x2 = (float) Math.cos(angle) * radius;
            float y2 = (float) Math.sin(angle) * radius;
            builder.vertex(pose, x1, y1, 0f).color(r, g, b, a).endVertex();
            builder.vertex(pose, x2, y2, 0f).color(r, g, b, a).endVertex();
        }

        tesselator.end();
        RenderSystem.lineWidth(1.0f);
    }

    /**
     * 渲染外圈物品（120度均布）
     */
    private static void renderFerrisWheelItem_120(PoseStack poseStack, MultiBufferSource bufferSource,
                                                   ItemRenderer itemRenderer, WheelSector sector,
                                                   int sectorIndex, boolean isSelected, float wheelAngle, float bloomProgress) {
        poseStack.pushPose();

        // 1. 定位到扇区角度（120度均布）
        float sectorAngle = sectorIndex * 120f;
        poseStack.mulPose(Axis.ZP.rotationDegrees(sectorAngle));

        // 2. 向外推到轨道半径
        float easeProgress = 1.0f - (float)Math.pow(1.0f - bloomProgress, BLOOM_EASE_POWER);
        float orbitRadius = ITEM_ORBIT_INNER + (ITEM_ORBIT_OUTER - ITEM_ORBIT_INNER) * easeProgress;
        poseStack.translate(orbitRadius, 0, 0.01f);

        // 3. 摩天轮修正：抵消盘子和扇区旋转
        poseStack.mulPose(Axis.ZP.rotationDegrees(-(wheelAngle + sectorAngle)));

        // 4. 渲染文字（选中时）
        if (isSelected) {
            poseStack.pushPose();
            poseStack.translate(0.3, 0, -0.01);  // Z 改为负数，让文字在物品后面
            poseStack.mulPose(Axis.YP.rotationDegrees(180f));
            poseStack.mulPose(Axis.ZP.rotationDegrees(180f));
            float textScale = 0.02f;
            poseStack.scale(-textScale, textScale, textScale);

            Minecraft mc = Minecraft.getInstance();
            Font font = mc.font;
            String text = sector.getName();
            Matrix4f matrix = poseStack.last().pose();
            float textWidth = font.width(text);
            font.drawInBatch(text, -textWidth / 2f, -font.lineHeight / 2f, 0xFFFFFF00, false,
                            matrix, bufferSource, Font.DisplayMode.NORMAL, 0, 0xF000F0);
            poseStack.popPose();
        }

        // 5. 缩放和渲染物品
        float animatedScale = ITEM_SCALE_SMALL + (ITEM_SCALE_LARGE - ITEM_SCALE_SMALL) * easeProgress;
        poseStack.scale(animatedScale, animatedScale, animatedScale);
        itemRenderer.renderStatic(sector.getIcon(), ItemDisplayContext.GUI, 15728880,
            OverlayTexture.NO_OVERLAY, poseStack, bufferSource, null, 0);

        poseStack.popPose();
    }

    /**
     * 渲染中心源图标
     *
     * 不显示文字标签 - 通过图标区分：
     * - 下界之星 = 自身灵力
     * - 磁石 = 阵盘
     * - 紫水晶碎片 = 灵物
     */
    private static void renderCenterSource(PoseStack poseStack, MultiBufferSource bufferSource,
                                           ItemRenderer itemRenderer, float wheelAngle) {
        SourceType source = SOURCES[selectedSourceIndex];

        poseStack.pushPose();

        // 1. 中心位置
        poseStack.translate(0, 0, 0.02f);

        // 2. 抵消盘子旋转，保持源图标正立
        poseStack.mulPose(Axis.ZP.rotationDegrees(-wheelAngle));

        // 3. 渲染源图标（不显示文字，更简洁）
        float iconScale = 0.20f;  // 稍微放大一点，更醒目
        poseStack.scale(iconScale, iconScale, iconScale);
        itemRenderer.renderStatic(source.getIcon(), ItemDisplayContext.GUI, 15728880,
            OverlayTexture.NO_OVERLAY, poseStack, bufferSource, null, 0);

        poseStack.popPose();
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
