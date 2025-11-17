package org.example.Kangnaixi.tiandao.spell.runtime.engine.source;

import net.minecraft.world.phys.Vec3;
import org.example.Kangnaixi.tiandao.spell.runtime.SourceType;
import org.example.Kangnaixi.tiandao.spell.runtime.engine.SpellContext;

import java.util.EnumMap;
import java.util.Map;

public final class SourceExecutors {

    private static final Map<SourceType, SourceExecutor> EXECUTORS = new EnumMap<>(SourceType.class);

    static {
        register(SourceType.FINGER, SourceExecutors::applyFinger);
        register(SourceType.WEAPON_SWORD, SourceExecutors::applyWeaponSword);
        register(SourceType.SEAL, ctx -> ctx.put("casting_circle", Boolean.TRUE));
        register(SourceType.TALISMAN, ctx -> ctx.put("talisman_consumed", Boolean.TRUE));
    }

    public static void register(SourceType type, SourceExecutor executor) {
        EXECUTORS.put(type, executor);
    }

    public static SourceExecutor get(SourceType type) {
        return EXECUTORS.getOrDefault(type, SourceExecutor.NO_OP);
    }

    private static void applyFinger(SpellContext ctx) {
        ctx.setCastPos(ctx.caster().getEyePosition());
        ctx.setDirection(ctx.caster().getLookAngle());
    }

    private static void applyWeaponSword(SpellContext ctx) {
        boolean holdingSword = ctx.caster().getMainHandItem().is(org.example.Kangnaixi.tiandao.spell.tag.SpellTags.SWORD_WEAPONS);
        ctx.put("weapon.sword", holdingSword);
        Vec3 pos = ctx.caster().position().add(0, 1.2, 0);
        ctx.setCastPos(pos);
        ctx.setDirection(ctx.caster().getLookAngle());
    }

    private SourceExecutors() {}
}
