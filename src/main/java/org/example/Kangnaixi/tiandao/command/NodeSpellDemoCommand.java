package org.example.Kangnaixi.tiandao.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.example.Kangnaixi.tiandao.spell.node.*;
import org.example.Kangnaixi.tiandao.spell.node.execution.NodeSpellExecutor;

/**
 * 节点术法演示命令 - 展示真正的投射方式
 * /nodedemo fireball - 火球术（弹道 + 伤害爆炸）
 * /nodedemo aoe_heal - 范围治疗（范围 + 治疗）
 * /nodedemo laser - 激光射线（射线 + 伤害）
 */
public class NodeSpellDemoCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("nodedemo")
                .then(Commands.literal("fireball").executes(NodeSpellDemoCommand::demoFireball))
                .then(Commands.literal("aoe_heal").executes(NodeSpellDemoCommand::demoAoeHeal))
                .then(Commands.literal("laser").executes(NodeSpellDemoCommand::demoLaser))
        );
    }

    /**
     * 演示1：火球术
     * 概念：玩家 → 发射弹道 → 命中后造成伤害和爆炸
     *
     * 当前实现的简化版：
     * 节点1: [无] (自身起点，隐式)
     * 节点2: [圆形] (模拟弹道命中后的爆炸范围，暂时用圆形代替真正的弹道)
     * 节点3: [伤害 + 爆炸]
     */
    private static int demoFireball(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        player.sendSystemMessage(Component.literal("§6===== 演示：火球术 ====="));
        player.sendSystemMessage(Component.literal("§7概念：玩家 → 弹道 → 伤害+爆炸"));
        player.sendSystemMessage(Component.literal("§e当前简化实现：用圆形范围模拟弹道命中"));

        try {
            NodeSpell spell = new NodeSpell("fireball", "§c火球术");
            spell.setTriggerType(TriggerType.SELF);

            // 节点1：投射方式 - 用圆形模拟弹道命中点的小范围
            SpellNode node1 = new SpellNode(1, 1);
            NodeComponent circle = ComponentRegistry.getInstance().getComponent("circle").copy();
            circle.setParameter("radius", 3.0);  // 小范围，模拟弹道命中后的爆炸半径
            node1.addComponent(circle);

            // 节点2：效果 - 伤害 + 爆炸
            SpellNode node2 = new SpellNode(2, 2);
            NodeComponent damage = ComponentRegistry.getInstance().getComponent("damage").copy();
            damage.setParameter("damage", 6.0);
            NodeComponent explosion = ComponentRegistry.getInstance().getComponent("explosion").copy();
            explosion.setParameter("power", 2.0);

            node2.addComponent(damage);
            node2.addComponent(explosion);

            spell.addNode(node1);
            spell.addNode(node2);

            player.sendSystemMessage(Component.literal("§a正在施放..."));
            var result = NodeSpellExecutor.getInstance().execute(spell, player);

            if (result.isSuccess()) {
                player.sendSystemMessage(Component.literal("§a✓ 施放成功！"));
                player.sendSystemMessage(Component.literal("§7影响: " + result.getContext().getAffectedEntityCount() + " 个实体"));
            } else {
                player.sendSystemMessage(Component.literal("§c✗ 施放失败: " + result.getMessage()));
            }

            return 1;
        } catch (Exception e) {
            player.sendSystemMessage(Component.literal("§c错误: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 演示2：范围治疗
     * 概念：玩家 → 范围（以自己为中心） → 治疗周围队友
     *
     * 节点1: [圆形范围8格]
     * 节点2: [治疗10点]
     */
    private static int demoAoeHeal(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        player.sendSystemMessage(Component.literal("§6===== 演示：范围治疗 ====="));
        player.sendSystemMessage(Component.literal("§7概念：玩家 → 范围 → 治疗"));

        try {
            NodeSpell spell = new NodeSpell("aoe_heal", "§a范围治疗");
            spell.setTriggerType(TriggerType.SELF);

            // 节点1：投射方式 - 范围
            SpellNode node1 = new SpellNode(1, 1);
            NodeComponent circle = ComponentRegistry.getInstance().getComponent("circle").copy();
            circle.setParameter("radius", 8.0);
            node1.addComponent(circle);

            // 节点2：效果 - 治疗
            SpellNode node2 = new SpellNode(2, 1);
            NodeComponent heal = ComponentRegistry.getInstance().getComponent("heal").copy();
            heal.setParameter("healing", 10.0);
            node2.addComponent(heal);

            spell.addNode(node1);
            spell.addNode(node2);

            player.sendSystemMessage(Component.literal("§a正在施放..."));
            var result = NodeSpellExecutor.getInstance().execute(spell, player);

            if (result.isSuccess()) {
                player.sendSystemMessage(Component.literal("§a✓ 施放成功！"));
                player.sendSystemMessage(Component.literal("§7治疗: " + result.getContext().getAffectedEntityCount() + " 个实体"));
            } else {
                player.sendSystemMessage(Component.literal("§c✗ 施放失败: " + result.getMessage()));
            }

            return 1;
        } catch (Exception e) {
            player.sendSystemMessage(Component.literal("§c错误: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 演示3：激光射线
     * 概念：玩家 → 射线 → 伤害路径上的所有敌人
     *
     * 节点1: [直线射线15格]
     * 节点2: [伤害8点]
     */
    private static int demoLaser(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        player.sendSystemMessage(Component.literal("§6===== 演示：激光射线 ====="));
        player.sendSystemMessage(Component.literal("§7概念：玩家 → 射线 → 伤害"));

        try {
            NodeSpell spell = new NodeSpell("laser", "§e激光射线");
            spell.setTriggerType(TriggerType.SELF);

            // 节点1：投射方式 - 射线
            SpellNode node1 = new SpellNode(1, 1);
            NodeComponent line = ComponentRegistry.getInstance().getComponent("line").copy();
            line.setParameter("distance", 15.0);
            node1.addComponent(line);

            // 节点2：效果 - 伤害
            SpellNode node2 = new SpellNode(2, 1);
            NodeComponent damage = ComponentRegistry.getInstance().getComponent("damage").copy();
            damage.setParameter("damage", 8.0);
            node2.addComponent(damage);

            spell.addNode(node1);
            spell.addNode(node2);

            player.sendSystemMessage(Component.literal("§a正在施放..."));
            var result = NodeSpellExecutor.getInstance().execute(spell, player);

            if (result.isSuccess()) {
                player.sendSystemMessage(Component.literal("§a✓ 施放成功！"));
                player.sendSystemMessage(Component.literal("§7命中: " + result.getContext().getAffectedEntityCount() + " 个实体"));
            } else {
                player.sendSystemMessage(Component.literal("§c✗ 施放失败: " + result.getMessage()));
            }

            return 1;
        } catch (Exception e) {
            player.sendSystemMessage(Component.literal("§c错误: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }
}
