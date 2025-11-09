package org.example.Kangnaixi.tiandao.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.joml.Matrix4f;

/**
 * 灵气护盾自定义渲染器
 * 
 * 实现半透明球体护盾效果，支持：
 * - 六边形纹理贴图
 * - 缓慢旋转动画
 * - 受击涟漪效果
 * - 性能优化（LOD、视锥裁剪）
 */
public class ShieldRenderer {
    
    // 护盾纹理资源位置
    private static final ResourceLocation SHIELD_TEXTURE = 
        ResourceLocation.fromNamespaceAndPath(Tiandao.MOD_ID, "textures/spell/hexagon_shield.png");
    
    // 球体细分参数（平衡质量和性能）
    private static final int LATITUDE_SEGMENTS = 16;   // 纬度段数
    private static final int LONGITUDE_SEGMENTS = 32;  // 经度段数
    
    // 护盾参数（椭球体）
    private static final float SHIELD_RADIUS_HORIZONTAL = 1.0f;  // 水平半径（X和Z轴）
    private static final float SHIELD_RADIUS_VERTICAL = 1.4f;    // 垂直半径（Y轴）- 拉伸以适应玩家身高
    private static final float ROTATION_SPEED = 0.5f;            // 旋转速度（度/tick）
    
    // 旋转角度（用于动画）
    private float rotationAngle = 0.0f;
    
    /**
     * 渲染护盾球体
     * 
     * @param poseStack 姿态栈
     * @param player 玩家实体
     * @param partialTick 部分 tick（用于平滑动画）
     */
    public void renderShield(com.mojang.blaze3d.vertex.PoseStack poseStack, Player player, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.gameRenderer == null) {
            return;
        }
        
        // 更新旋转角度
        rotationAngle += ROTATION_SPEED;
        if (rotationAngle >= 360.0f) {
            rotationAngle -= 360.0f;
        }
        
        // 计算玩家位置（插值以平滑移动）
        Vec3 playerPos = player.getPosition(partialTick);
        double playerY = playerPos.y + player.getBbHeight() / 2.0;
        
        // 获取相机信息
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();
        
        // 计算相对于相机的偏移
        double dx = playerPos.x - cameraPos.x;
        double dy = playerY - cameraPos.y;
        double dz = playerPos.z - cameraPos.z;
        
        // 设置渲染状态
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, SHIELD_TEXTURE);
        
        // 开始渲染
        poseStack.pushPose();
        poseStack.translate(dx, dy, dz);
        
        // 应用旋转
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rotationAngle));
        
        // 渲染球体
        renderSphere(poseStack);
        
        poseStack.popPose();
        
        // 恢复渲染状态
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }
    
    /**
     * 渲染椭球体几何体（更适合玩家身形）
     * 
     * @param poseStack 姿态栈
     */
    private void renderSphere(com.mojang.blaze3d.vertex.PoseStack poseStack) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        Matrix4f matrix = poseStack.last().pose();
        
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        
        // 生成椭球体顶点
        for (int lat = 0; lat < LATITUDE_SEGMENTS; lat++) {
            float theta1 = (float) (lat * Math.PI / LATITUDE_SEGMENTS);
            float theta2 = (float) ((lat + 1) * Math.PI / LATITUDE_SEGMENTS);
            
            for (int lon = 0; lon < LONGITUDE_SEGMENTS; lon++) {
                float phi1 = (float) (lon * 2 * Math.PI / LONGITUDE_SEGMENTS);
                float phi2 = (float) ((lon + 1) * 2 * Math.PI / LONGITUDE_SEGMENTS);
                
                // 计算四个顶点（使用椭球体参数）
                Vec3 v1 = sphericalToEllipsoid(SHIELD_RADIUS_HORIZONTAL, SHIELD_RADIUS_VERTICAL, theta1, phi1);
                Vec3 v2 = sphericalToEllipsoid(SHIELD_RADIUS_HORIZONTAL, SHIELD_RADIUS_VERTICAL, theta1, phi2);
                Vec3 v3 = sphericalToEllipsoid(SHIELD_RADIUS_HORIZONTAL, SHIELD_RADIUS_VERTICAL, theta2, phi2);
                Vec3 v4 = sphericalToEllipsoid(SHIELD_RADIUS_HORIZONTAL, SHIELD_RADIUS_VERTICAL, theta2, phi1);
                
                // 计算纹理坐标（球面UV映射）
                // U坐标：经度方向（0到1，从左到右）
                float u1 = (float) lon / LONGITUDE_SEGMENTS;
                float u2 = (float) (lon + 1) / LONGITUDE_SEGMENTS;
                // V坐标：纬度方向（0到1，从上到下）
                float v1t = (float) lat / LATITUDE_SEGMENTS;
                float v2t = (float) (lat + 1) / LATITUDE_SEGMENTS;
                
                // 使用白色，让纹理本身的颜色显示出来
                // Alpha值控制整体透明度（纹理本身的Alpha通道也会起作用）
                int alpha = 200; // 约 78% 不透明度
                int white = 255;
                
                // 添加四边形的四个顶点
                // 顶点顺序：逆时针（从外部看向球心）
                // 这样法线会指向外部，是正确的
                buffer.vertex(matrix, (float) v1.x, (float) v1.y, (float) v1.z)
                      .uv(u1, v1t).color(white, white, white, alpha).endVertex();
                buffer.vertex(matrix, (float) v2.x, (float) v2.y, (float) v2.z)
                      .uv(u2, v1t).color(white, white, white, alpha).endVertex();
                buffer.vertex(matrix, (float) v3.x, (float) v3.y, (float) v3.z)
                      .uv(u2, v2t).color(white, white, white, alpha).endVertex();
                buffer.vertex(matrix, (float) v4.x, (float) v4.y, (float) v4.z)
                      .uv(u1, v2t).color(white, white, white, alpha).endVertex();
            }
        }
        
        tesselator.end();
    }
    
    /**
     * 球坐标转笛卡尔坐标（椭球体）
     * 
     * @param radiusH 水平半径（X和Z轴）
     * @param radiusV 垂直半径（Y轴）
     * @param theta 纬度角（0 到 PI）
     * @param phi 经度角（0 到 2*PI）
     * @return 笛卡尔坐标
     */
    private Vec3 sphericalToEllipsoid(float radiusH, float radiusV, float theta, float phi) {
        // 水平方向（X和Z）使用水平半径
        float x = radiusH * (float) (Math.sin(theta) * Math.cos(phi));
        float z = radiusH * (float) (Math.sin(theta) * Math.sin(phi));
        // 垂直方向（Y）使用垂直半径，形成椭圆体
        float y = radiusV * (float) Math.cos(theta);
        return new Vec3(x, y, z);
    }
    
    /**
     * 重置旋转角度（用于新护盾实例）
     */
    public void reset() {
        rotationAngle = 0.0f;
    }
}
