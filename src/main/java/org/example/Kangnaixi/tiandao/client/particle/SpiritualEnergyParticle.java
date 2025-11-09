package org.example.Kangnaixi.tiandao.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.example.Kangnaixi.tiandao.cultivation.SpiritualRootType;

/**
 * 灵力粒子效果类
 * 根据灵根类型显示不同颜色的粒子
 */
@OnlyIn(Dist.CLIENT)
public class SpiritualEnergyParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    
    protected SpiritualEnergyParticle(ClientLevel level, double x, double y, double z, 
                                    double xSpeed, double ySpeed, double zSpeed, 
                                    SpriteSet sprites, SpiritualRootType rootType) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.sprites = sprites;
        
        // 设置粒子颜色
        int color = rootType.getColor();
        this.setColor((color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F, (color & 255) / 255.0F);
        
        // 设置粒子大小和生命周期
        this.quadSize = 0.3F + this.random.nextFloat() * 0.2F;
        this.lifetime = 40 + this.random.nextInt(20);
        
        // 设置粒子运动
        this.xd = xSpeed + (this.random.nextDouble() - 0.5) * 0.1;
        this.yd = ySpeed + (this.random.nextDouble() - 0.5) * 0.1;
        this.zd = zSpeed + (this.random.nextDouble() - 0.5) * 0.1;
        
        // 设置透明度变化
        this.alpha = 0.8F;
        
        // 选择随机精灵（仅当sprites不为null时）
        if (sprites != null) {
        this.setSpriteFromAge(sprites);
        }
    }
    
    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
    
    @Override
    public void tick() {
        super.tick();
        // 随时间减小透明度
        this.alpha = (float)(40 - this.age) / 40.0F * 0.8F;
        // 随时间减小大小
        this.quadSize *= 0.98F;
        // 更新精灵（仅当sprites不为null时）
        if (sprites != null) {
        this.setSpriteFromAge(sprites);
        }
    }
    
    /**
     * 灵力粒子提供者类
     */
    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final SpiritualRootType rootType;
        
        public Provider(SpriteSet sprites, SpiritualRootType rootType) {
            this.sprites = sprites;
            this.rootType = rootType;
        }
        
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, 
                                     double x, double y, double z, 
                                     double xSpeed, double ySpeed, double zSpeed) {
            return new SpiritualEnergyParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, 
                                             sprites, rootType);
        }
    }
}