package org.example.Kangnaixi.tiandao.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * 通用的简易弹道渲染器，将弹道渲染为始终面向相机的贴图。
 *
 * @param <T> 任意可投掷实体
 */
public class BillboardProjectileRenderer<T extends ThrowableProjectile> extends EntityRenderer<T> {

    private final ResourceLocation texture;
    private final float scale;

    public BillboardProjectileRenderer(EntityRendererProvider.Context context, ResourceLocation texture, float scale) {
        super(context);
        this.texture = texture;
        this.scale = scale;
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix4f = pose.pose();
        Matrix3f matrix3f = pose.normal();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(texture));

        vertex(consumer, matrix4f, matrix3f, packedLight, 0.0F, 0.0F, 0.0F, 1.0F);
        vertex(consumer, matrix4f, matrix3f, packedLight, 1.0F, 0.0F, 1.0F, 1.0F);
        vertex(consumer, matrix4f, matrix3f, packedLight, 1.0F, 1.0F, 1.0F, 0.0F);
        vertex(consumer, matrix4f, matrix3f, packedLight, 0.0F, 1.0F, 0.0F, 0.0F);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f poseMatrix, Matrix3f normalMatrix,
                               int packedLight, float x, float y, float u, float v) {
        consumer.vertex(poseMatrix, x - 0.5F, y - 0.5F, 0.0F)
            .color(255, 255, 255, 255)
            .uv(u, v)
            .overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(packedLight)
            .normal(normalMatrix, 0.0F, 1.0F, 0.0F)
            .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture;
    }
}
