package org.example.Kangnaixi.tiandao.spell.runtime;

import net.minecraft.server.level.ServerPlayer;
import org.example.Kangnaixi.tiandao.capability.ICultivation;
import org.example.Kangnaixi.tiandao.spell.definition.SpellDefinition;

/**
 * 负责将 SpellDefinition 作用到玩家身上。
 */
public final class SpellCastingService {

    private SpellCastingService() {}

    public static CastResult cast(ServerPlayer player, ICultivation cultivation, SpellDefinition definition) {
        String spellId = definition.getId().toString();
        int cooldownRemaining = cultivation.getSpellCooldownRemaining(spellId);
        if (cooldownRemaining > 0) {
            return CastResult.onCooldown(cooldownRemaining);
        }

        SpellRuntimeContext context = SpellRuntimeContext.of(player, cultivation, definition);
        SpellRuntimeResult runtimeResult = SpellRuntimeEngine.evaluate(context);
        double spiritCost = runtimeResult.numbers().spiritCost();

        if (cultivation.getSpiritPower() < spiritCost) {
            return CastResult.notEnoughSpirit(spiritCost, cultivation.getSpiritPower());
        }

        if (!cultivation.consumeSpiritPower(spiritCost)) {
            return CastResult.notEnoughSpirit(spiritCost, cultivation.getSpiritPower());
        }

        long cooldownEnd = System.currentTimeMillis() + Math.round(runtimeResult.numbers().cooldownSeconds() * 1000);
        cultivation.setSpellCooldown(spellId, cooldownEnd);

        if (runtimeResult.numbers().durationTicks() > 0) {
            long durationMs = Math.round(runtimeResult.numbers().durationTicks() / 20.0 * 1000);
            cultivation.activateSpell(spellId, System.currentTimeMillis() + durationMs);
        }

        applyImmediateEffects(player, runtimeResult);
        return CastResult.success(runtimeResult);
    }

    private static void applyImmediateEffects(ServerPlayer player, SpellRuntimeResult result) {
        // TODO: 结合 Carrier/Form 实际生成弹体/区域伤害，这里只作为占位逻辑
    }

    public static final class CastResult {
        public enum FailureReason {
            COOLDOWN,
            SPIRIT,
            UNKNOWN
        }

        private final boolean success;
        private final SpellRuntimeResult runtimeResult;
        private final FailureReason failureReason;
        private final double expectedSpirit;
        private final double currentSpirit;
        private final int cooldownRemaining;

        private CastResult(boolean success,
                           SpellRuntimeResult runtimeResult,
                           FailureReason failureReason,
                           double expectedSpirit,
                           double currentSpirit,
                           int cooldownRemaining) {
            this.success = success;
            this.runtimeResult = runtimeResult;
            this.failureReason = failureReason;
            this.expectedSpirit = expectedSpirit;
            this.currentSpirit = currentSpirit;
            this.cooldownRemaining = cooldownRemaining;
        }

        public static CastResult success(SpellRuntimeResult runtimeResult) {
            return new CastResult(true, runtimeResult, null, 0, 0, 0);
        }

        public static CastResult onCooldown(int remainingSeconds) {
            return new CastResult(false, null, FailureReason.COOLDOWN, 0, 0, remainingSeconds);
        }

        public static CastResult notEnoughSpirit(double expected, double current) {
            return new CastResult(false, null, FailureReason.SPIRIT, expected, current, 0);
        }

        public boolean success() {
            return success;
        }

        public SpellRuntimeResult runtimeResult() {
            return runtimeResult;
        }

        public FailureReason failureReason() {
            return failureReason;
        }

        public double expectedSpirit() {
            return expectedSpirit;
        }

        public double currentSpirit() {
            return currentSpirit;
        }

        public int cooldownRemaining() {
            return cooldownRemaining;
        }
    }
}
