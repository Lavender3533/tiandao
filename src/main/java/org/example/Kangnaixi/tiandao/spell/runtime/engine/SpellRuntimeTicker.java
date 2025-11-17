package org.example.Kangnaixi.tiandao.spell.runtime.engine;

import net.minecraft.server.level.ServerLevel;
import org.example.Kangnaixi.tiandao.spell.runtime.engine.form.FormExecutors;

import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

public final class SpellRuntimeTicker {

    private static final CopyOnWriteArrayList<SpellContext> ACTIVE = new CopyOnWriteArrayList<>();

    private SpellRuntimeTicker() {}

    public static void add(SpellContext ctx) {
        ACTIVE.add(ctx);
    }

    public static void tick(ServerLevel level) {
        Iterator<SpellContext> iterator = ACTIVE.iterator();
        while (iterator.hasNext()) {
            SpellContext ctx = iterator.next();
            if (ctx.isFinished()) {
                iterator.remove();
                continue;
            }
            if (ctx.level() != level) {
                continue;
            }
            boolean keepRunning = FormExecutors.get(ctx.spell().getForm()).tick(ctx);
            if (!keepRunning || ctx.isFinished()) {
                ctx.finish();
                ctx.finalizeExecution();
                iterator.remove();
            }
        }
    }

    public static void clear() {
        ACTIVE.clear();
    }
}
