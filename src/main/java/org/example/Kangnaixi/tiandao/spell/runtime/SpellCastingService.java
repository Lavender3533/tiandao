package org.example.Kangnaixi.tiandao.spell.runtime;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.capability.ICultivation;
import org.example.Kangnaixi.tiandao.network.CultivationDataSyncPacket;
import org.example.Kangnaixi.tiandao.network.NetworkHandler;
import org.example.Kangnaixi.tiandao.spell.definition.SpellDefinition;
import org.example.Kangnaixi.tiandao.spell.runtime.calc.SpellSpiritCostCalculator;

/**
 * 负责将 SpellDefinition 作用到玩家身上。
 */
public final class SpellCastingService {

    private SpellCastingService() {}

    public static CastResult cast(ServerPlayer player, ICultivation cultivation, SpellDefinition definition) {
        ICultivation cult = cultivation;
        if (cult == null) {
            cult = player.getCapability(Tiandao.CULTIVATION_CAPABILITY).orElse(null);
            if (cult == null) {
                return CastResult.unknownFailure();
            }
        }

        String spellId = definition.getId().toString();
        int cooldownRemaining = cult.getSpellCooldownRemaining(spellId);
        if (cooldownRemaining > 0) {
            return CastResult.onCooldown(cooldownRemaining);
        }

        double spiritCost = SpellSpiritCostCalculator.compute(definition);
        if (cult.getSpiritPower() < spiritCost || !cult.consumeSpiritPower(spiritCost)) {
            player.sendSystemMessage(Component.literal("§c灵力不足，无法施展该术法！"));
            return CastResult.notEnoughSpirit(spiritCost, cult.getSpiritPower());
        }

        NetworkHandler.sendToPlayer(new CultivationDataSyncPacket(cult), player);

        SpellRuntimeContext context = SpellRuntimeContext.of(player, cult, definition);
        SpellRuntimeResult runtimeResult = SpellRuntimeEngine.evaluate(context);
        SpellRuntimeResult adjustedResult = overrideSpiritCost(runtimeResult, spiritCost);

        long cooldownEnd = System.currentTimeMillis() + Math.round(adjustedResult.numbers().cooldownSeconds() * 1000);
        cult.setSpellCooldown(spellId, cooldownEnd);

        if (adjustedResult.numbers().durationTicks() > 0) {
            long durationMs = Math.round(adjustedResult.numbers().durationTicks() / 20.0 * 1000);
            cult.activateSpell(spellId, System.currentTimeMillis() + durationMs);
        }

        applyImmediateEffects(player, adjustedResult);
        return CastResult.success(adjustedResult);
    }

    private static void applyImmediateEffects(ServerPlayer player, SpellRuntimeResult result) {
        // TODO: 结合 Carrier/Form 实际生成弹体/区域伤害，这里只作为占位逻辑
    }
    
    private static SpellRuntimeResult overrideSpiritCost(SpellRuntimeResult runtimeResult, double spiritCost) {
        SpellDefinition.Numbers numbers = runtimeResult.numbers();
        SpellDefinition.Numbers adjusted = new SpellDefinition.Numbers(
            numbers.baseDamage(),
            numbers.projectileSpeed(),
            numbers.areaRange(),
            numbers.channelTicks(),
            numbers.durationTicks(),
            numbers.cooldownSeconds(),
            spiritCost
        );
        return new SpellRuntimeResult(runtimeResult.definition(), adjusted, runtimeResult.effects(), runtimeResult.swordQiTriggered());
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

        public static CastResult unknownFailure() {
            return new CastResult(false, null, FailureReason.UNKNOWN, 0, 0, 0);
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
