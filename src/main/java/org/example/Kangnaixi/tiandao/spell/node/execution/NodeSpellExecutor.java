package org.example.Kangnaixi.tiandao.spell.node.execution;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.spell.debug.SpellDebugConfig;
import org.example.Kangnaixi.tiandao.spell.node.NodeComponent;`nimport org.example.Kangnaixi.tiandao.spell.node.execution.ComponentExecutor;`nimport org.example.Kangnaixi.tiandao.spell.node.execution.ExecutorRegistry;
import org.example.Kangnaixi.tiandao.spell.node.NodeSpell;
import org.example.Kangnaixi.tiandao.spell.node.SpellNode;
import org.example.Kangnaixi.tiandao.spell.node.TriggerType;
import org.example.Kangnaixi.tiandao.spell.node.target.SpellTargetSet;
import org.example.Kangnaixi.tiandao.spell.node.target.TargetStage;
import org.example.Kangnaixi.tiandao.spell.node.target.TargetStageRegistry;

/**
 * Node spell execution pipeline based on TargetStage.
 */
public class NodeSpellExecutor {

    private static NodeSpellExecutor INSTANCE;

    public static NodeSpellExecutor getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new NodeSpellExecutor();
        }
        return INSTANCE;
    }

    public ExecutionResult execute(NodeSpell spell, Player caster) {
        NodeSpell.ValidationResult validation = spell.validate();
        if (!validation.isValid()) {
            return new ExecutionResult(false, validation.getMessage(), null);
        }

        NodeSpellContext context = new NodeSpellContext(caster);
        try {
            executeTrigger(spell.getTriggerType(), context);
            initializeDefaultTargets(context);
            runNodes(spell, context, 0);
            return new ExecutionResult(true, "Spell executed", context);
        } catch (Exception e) {
            Tiandao.LOGGER.error("Node spell execution failed: {}", spell.getName(), e);
            return new ExecutionResult(false, e.getMessage(), context);
        }
    }

    public ExecutionResult executeRemaining(NodeSpell spell, NodeSpellContext context, int startIndex) {
        try {
            context.setNodesExecuted(startIndex);
            runNodes(spell, context, startIndex);
            return new ExecutionResult(true, "Remaining nodes executed", context);
        } catch (Exception e) {
            Tiandao.LOGGER.error("Remaining node execution failed: {}", spell.getName(), e);
            return new ExecutionResult(false, e.getMessage(), context);
        }
    }

    private void runNodes(NodeSpell spell, NodeSpellContext context, int startIndex) {
        for (int i = startIndex; i < spell.getNodes().size(); i++) {
            executeNode(spell.getNodes().get(i), context);
        }
    }

    private void initializeDefaultTargets(NodeSpellContext context) {
        SpellTargetSet initial = new SpellTargetSet();
        initial.addEntityTarget(context.getCaster(), 1.0);
        initial.addPointTarget(context.getCaster().position());
        initial.setSource(context.getCaster());
        context.replaceTargets(initial);
    }

    private void executeTrigger(TriggerType triggerType, NodeSpellContext context) {
        switch (triggerType) {
            case SELF -> {
                context.setPosition(context.getCaster().position());
                context.setDirection(context.getCaster().getLookAngle());
            }
            case TOUCH -> {
                var start = context.getCaster().getEyePosition();
                var direction = context.getCaster().getLookAngle();
                var end = start.add(direction.scale(3.0));
                var clipContext = new net.minecraft.world.level.ClipContext(
                    start,
                    end,
                    net.minecraft.world.level.ClipContext.Block.COLLIDER,
                    net.minecraft.world.level.ClipContext.Fluid.NONE,
                    context.getCaster()
                );
                var hit = context.getLevel().clip(clipContext);
                context.setPosition(hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS ? end : hit.getLocation());
                context.setDirection(direction);
            }
            case PROJECTILE -> {
                context.setPosition(context.getCaster().position());
                context.setDirection(context.getCaster().getLookAngle());
                Tiandao.LOGGER.debug("PROJECTILE trigger currently behaves like SELF; projectile nodes handle spawning.");
            }
        }
        context.addSpiritCost(triggerType.getSpiritCost());
    }

    private void executeNode(SpellNode node, NodeSpellContext context) {
        context.incrementNodesExecuted();

        if (!node.getProjectileComponents().isEmpty()) {
            NodeComponent projectile = node.getProjectileComponents().get(0);
            applyTargetStage(projectile, context);
        }

        inOrder(node.getEffectComponents()).forEach(component -> executeComponent(component, context));
        inOrder(node.getModifierComponents()).forEach(component -> executeComponent(component, context));

        context.addSpiritCost(node.calculateSpiritCost());
    }

    private java.util.List<NodeComponent> inOrder(java.util.List<NodeComponent> list) {
        return list == null ? java.util.Collections.emptyList() : list;
    }

    private void applyTargetStage(NodeComponent projectile, NodeSpellContext context) {
        TargetStage stage = null;
        if (projectile.getStageId() != null && !projectile.getStageId().isBlank()) {
            stage = TargetStageRegistry.getInstance().getStage(projectile.getStageId());
        }
        if (stage == null) {
            stage = TargetStageRegistry.getInstance().getStage(projectile.getId());
        }
        if (stage == null) {
            Tiandao.LOGGER.warn("Projectile {} has no TargetStage registered", projectile.getId());
            return;
        }

        TargetStage.TargetStageResult result = stage.resolve(projectile, context, context.getCurrentTargets(), context.getModifiers());
        SpellTargetSet newTargets = result.isEmpty() ? new SpellTargetSet() : result.outputs().get(0);
        context.replaceTargets(newTargets);
        if (SpellDebugConfig.isShowTargets()) {
            debugTargets(newTargets, context);
        }
    }

    private void executeComponent(NodeComponent component, NodeSpellContext context) {
        ComponentExecutor executor = ExecutorRegistry.getInstance().getExecutor(component.getId());
        if (executor == null) {
            Tiandao.LOGGER.warn("No executor registered for component {}", component.getId());
            return;
        }
        if (executor.requiresTargets() && context.getAffectedEntityCount() == 0) {
            Tiandao.LOGGER.debug("Component {} requires targets but none available", component.getId());
            return;
        }
        executor.execute(component, context);
    }

    private void debugTargets(SpellTargetSet targets, NodeSpellContext context) {
        if (!(context.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        targets.getPointTargets().forEach(point ->
            serverLevel.sendParticles(ParticleTypes.END_ROD, point.x, point.y + 0.1, point.z,
                4, 0.05, 0.05, 0.05, 0.02));
        targets.toLivingEntities().forEach(entity ->
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(),
                3, 0.1, 0.1, 0.1, 0.02));
    }

    public record ExecutionResult(boolean success, String message, NodeSpellContext context) {
    }
}

