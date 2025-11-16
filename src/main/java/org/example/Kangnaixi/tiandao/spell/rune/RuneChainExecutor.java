package org.example.Kangnaixi.tiandao.spell.rune;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * 符文链执行器 - 按顺序执行符文序列
 */
public class RuneChainExecutor {

    /**
     * 执行符文链
     * @param runes 符文序列
     * @param caster 施法者
     * @return 执行结果
     */
    public static ExecutionResult execute(List<Rune> runes, Player caster) {
        if (runes == null || runes.isEmpty()) {
            return ExecutionResult.failure("符文链为空");
        }

        if (caster == null) {
            return ExecutionResult.failure("施法者为空");
        }

        // 验证符文链结构
        ValidationResult validation = validate(runes);
        if (!validation.isValid()) {
            return ExecutionResult.failure(validation.getErrorMessage());
        }

        // 创建执行上下文
        Level level = caster.level();
        RuneContext context = new RuneContext(caster, level);

        try {
            // 按顺序执行每个符文
            for (int i = 0; i < runes.size(); i++) {
                Rune rune = runes.get(i);

                // 调试信息
                if (!caster.level().isClientSide) {
                    caster.sendSystemMessage(Component.literal("§7执行符文: §b" + rune.getName()));
                }

                // 执行符文
                rune.execute(context);

                // 如果是弹道模式，将符文链传递给弹道实体并停止后续执行
                if (context.hasVariable("is_projectile") && context.hasVariable("projectile_entity")) {
                    Object projectileObj = context.getVariable("projectile_entity");
                    if (projectileObj instanceof org.example.Kangnaixi.tiandao.spell.entity.SpellProjectileEntity projectile) {
                        projectile.setRuneChain(runes);
                    }
                    break; // 停止执行后续符文
                }

                // 可以在这里添加符文间的延迟或动画
            }

            return ExecutionResult.success(context);

        } catch (Exception e) {
            return ExecutionResult.failure("符文执行错误: " + e.getMessage());
        }
    }

    /**
     * 验证符文链的合法性
     */
    public static ValidationResult validate(List<Rune> runes) {
        if (runes.isEmpty()) {
            return ValidationResult.invalid("符文链不能为空");
        }

        // 检查第一个符文必须是触发符文（inputs = 0）
        Rune first = runes.get(0);
        if (first.getInputs() != 0) {
            return ValidationResult.invalid("第一个符文必须是触发符文（如：自身、触摸、弹道）");
        }

        // 检查符文之间的连接是否合法
        for (int i = 0; i < runes.size() - 1; i++) {
            Rune current = runes.get(i);
            Rune next = runes.get(i + 1);

            if (!current.canConnectTo(next)) {
                return ValidationResult.invalid(
                    String.format("符文 '%s' 无法连接到 '%s'",
                        current.getName(), next.getName())
                );
            }
        }

        // 检查必须有至少一个效果符文
        boolean hasEffect = runes.stream()
            .anyMatch(rune -> rune.getCategory() == Rune.RuneCategory.EFFECT);

        if (!hasEffect) {
            return ValidationResult.invalid("符文链必须包含至少一个效果符文");
        }

        return ValidationResult.valid();
    }

    /**
     * 计算符文链的总消耗
     */
    public static double calculateTotalCost(List<Rune> runes) {
        return runes.stream()
            .mapToDouble(Rune::getSpiritCost)
            .sum();
    }

    /**
     * 计算符文链的最大冷却时间
     */
    public static double calculateCooldown(List<Rune> runes) {
        return runes.stream()
            .mapToDouble(Rune::getCooldown)
            .max()
            .orElse(1.0);
    }

    // ===== 内部类 =====

    /**
     * 执行结果
     */
    public static class ExecutionResult {
        private final boolean success;
        private final String message;
        private final RuneContext context;

        private ExecutionResult(boolean success, String message, RuneContext context) {
            this.success = success;
            this.message = message;
            this.context = context;
        }

        public static ExecutionResult success(RuneContext context) {
            return new ExecutionResult(true, "执行成功", context);
        }

        public static ExecutionResult failure(String message) {
            return new ExecutionResult(false, message, null);
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public RuneContext getContext() { return context; }
    }

    /**
     * 验证结果
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, "");
        }

        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }

        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
    }
}
