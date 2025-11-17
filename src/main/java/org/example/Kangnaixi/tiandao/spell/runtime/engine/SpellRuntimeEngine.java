package org.example.Kangnaixi.tiandao.spell.runtime.engine;

import org.example.Kangnaixi.tiandao.spell.runtime.Spell;
import org.example.Kangnaixi.tiandao.spell.runtime.engine.attribute.AttributeExecutors;
import org.example.Kangnaixi.tiandao.spell.runtime.engine.carrier.CarrierExecutors;
import org.example.Kangnaixi.tiandao.spell.runtime.engine.effect.EffectExecutors;
import org.example.Kangnaixi.tiandao.spell.runtime.engine.form.FormExecutors;
import org.example.Kangnaixi.tiandao.spell.runtime.engine.source.SourceExecutors;

/**
 * 新版运行时引擎骨架: Source → Carrier → Form → Attributes → Effects → Finalize.
 * 该流程不依赖 JSON 术法, 可以直接由 GUI 保存后的 {@link Spell} 运行.
 */
public final class SpellRuntimeEngine {

    private SpellRuntimeEngine() {}

    public static SpellContext execute(Spell spell, net.minecraft.server.level.ServerPlayer player) {
        SpellContext ctx = new SpellContext(spell, player);

        SourceExecutors.get(spell.getSource()).beginCast(ctx);
        ctx.setCarrierExecutor(CarrierExecutors.get(spell.getCarrier()));

        spell.getAttributes().forEach(attr -> AttributeExecutors.get(attr).apply(ctx));
        spell.getEffects().forEach(effect -> EffectExecutors.get(effect).apply(ctx));

        FormExecutors.get(spell.getForm()).applyFormBehavior(ctx);

        if (ctx.isFinished()) {
            ctx.finalizeExecution();
        } else {
            SpellRuntimeTicker.add(ctx);
        }
        return ctx;
    }
}
