package org.example.Kangnaixi.tiandao.spell.runtime.engine.carrier;

import net.minecraft.world.entity.projectile.ThrownExperienceBottle;
import net.minecraft.world.phys.Vec3;
import org.example.Kangnaixi.tiandao.spell.runtime.AttributeType;
import org.example.Kangnaixi.tiandao.spell.runtime.CarrierType;
import org.example.Kangnaixi.tiandao.spell.runtime.engine.SpellContext;

import java.util.EnumMap;
import java.util.Map;

public final class CarrierExecutors {

    private static final Map<CarrierType, CarrierExecutor> EXECUTORS = new EnumMap<>(CarrierType.class);

    static {
        register(CarrierType.SWORD_QI, CarrierExecutors::spawnSwordQi);
        register(CarrierType.PROJECTILE, CarrierExecutors::spawnProjectile);
        register(CarrierType.FIELD, ctx -> ctx.put("carrier.field", Boolean.TRUE));
        register(CarrierType.WAVE, ctx -> ctx.put("carrier.wave", Boolean.TRUE));
        register(CarrierType.BUFF, ctx -> ctx.put("carrier.buff", Boolean.TRUE));
        register(CarrierType.GLYPH, ctx -> ctx.put("carrier.glyph", Boolean.TRUE));
    }

    public static void register(CarrierType type, CarrierExecutor executor) {
        EXECUTORS.put(type, executor);
    }

    public static CarrierExecutor get(CarrierType type) {
        return EXECUTORS.getOrDefault(type, CarrierExecutor.NO_OP);
    }

    private static void spawnSwordQi(SpellContext ctx) {
        boolean holdingSword = ctx.get("weapon.sword", Boolean.class).orElse(Boolean.FALSE);
        boolean swordIntent = ctx.spell().getAttributes().contains(AttributeType.SWORD_INTENT);

        double damage = ctx.baseDamage();
        if (holdingSword) {
            damage *= 1.3;
        }
        if (swordIntent) {
            damage *= 1.25;
        }
        ctx.setBaseDamage(damage);
        ctx.put("carrier.sword_qi", Boolean.TRUE);
        // 此处仅记录方向/速度, 具体实体可由调用方读取 ctx 后生成
    }

    private static void spawnProjectile(SpellContext ctx) {
        ctx.put("carrier.projectile", Boolean.TRUE);
        Vec3 dir = ctx.direction();
        if (!ctx.level().isClientSide()) {
            ThrownExperienceBottle placeholder = new ThrownExperienceBottle(ctx.level(), ctx.caster());
            placeholder.setPos(ctx.castPos());
            placeholder.shoot(dir.x, dir.y, dir.z, (float) ctx.projectileSpeed(), 0.0F);
            ctx.level().addFreshEntity(placeholder);
        }
    }

    private CarrierExecutors() {}
}
