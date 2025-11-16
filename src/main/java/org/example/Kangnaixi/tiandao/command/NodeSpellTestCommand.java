package org.example.Kangnaixi.tiandao.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.example.Kangnaixi.tiandao.spell.node.*;
import org.example.Kangnaixi.tiandao.spell.node.execution.NodeSpellExecutor;
import org.example.Kangnaixi.tiandao.item.SpellJadeSlipItem;

/**
 * 节点术法测试命令
 * /nodespell test1 - 测试基础圆形伤害术
 * /nodespell test2 - 测试圆形治疗术
 * /nodespell test3 - 测试复合效果（圆形范围：治疗+推动）
 * /nodespell test4 - 测试流水线（圆形筛选 → 扇形过滤 → 伤害）
 * /nodespell test5 - 测试爆炸术（扇形范围爆炸）
 * /nodespell list - 列出所有可用组件
 * /nodespell save - 保存测试1的术法到玉简
 */
public class NodeSpellTestCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("nodespell")
                .then(Commands.literal("test1").executes(NodeSpellTestCommand::test1))
                .then(Commands.literal("test2").executes(NodeSpellTestCommand::test2))
                .then(Commands.literal("test3").executes(NodeSpellTestCommand::test3))
                .then(Commands.literal("test4").executes(NodeSpellTestCommand::test4))
                .then(Commands.literal("test5").executes(NodeSpellTestCommand::test5))
                .then(Commands.literal("list").executes(NodeSpellTestCommand::listComponents))
                .then(Commands.literal("save").executes(NodeSpellTestCommand::saveToSlip))
        );
    }

    /**
     * 测试1：基础圆形伤害术
     * 节点1: [圆形(半径5) + 伤害(5)]
     */
    private static int test1(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        player.sendSystemMessage(Component.literal("§6===== 测试1: 基础圆形伤害术 ====="));

        try {
            // 创建术法
            NodeSpell spell = new NodeSpell("test1", "基础圆形伤害术");
            spell.setTriggerType(TriggerType.SELF);

            // 节点1: 圆形 + 伤害
            SpellNode node1 = new SpellNode(1, 2);
            NodeComponent circle = ComponentRegistry.getInstance().getComponent("circle").copy();
            circle.setParameter("radius", 5.0);
            NodeComponent damage = ComponentRegistry.getInstance().getComponent("damage").copy();
            damage.setParameter("damage", 5.0);

            node1.addComponent(circle);
            node1.addComponent(damage);
            spell.addNode(node1);

            // 显示信息
            player.sendSystemMessage(Component.literal("§b触发方式: §f" + spell.getTriggerType().getDisplayName()));
            player.sendSystemMessage(Component.literal("§b节点数量: §f" + spell.getNodes().size()));
            player.sendSystemMessage(Component.literal("§b灵力消耗: §f" + String.format("%.1f", spell.calculateTotalSpiritCost())));
            player.sendSystemMessage(Component.literal("§b冷却时间: §f" + String.format("%.1f", spell.calculateCooldown()) + "秒"));

            // 执行
            var result = NodeSpellExecutor.getInstance().execute(spell, player);

            if (result.isSuccess()) {
                player.sendSystemMessage(Component.literal("§a执行成功！"));
                if (result.getContext() != null) {
                    player.sendSystemMessage(Component.literal("§7影响实体数: " + result.getContext().getAffectedEntityCount()));
                }
            } else {
                player.sendSystemMessage(Component.literal("§c执行失败: " + result.getMessage()));
            }

            return 1;
        } catch (Exception e) {
            player.sendSystemMessage(Component.literal("§c错误: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 测试2：圆形治疗术
     * 节点1: [圆形(半径8) + 治疗(10)]
     */
    private static int test2(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        player.sendSystemMessage(Component.literal("§6===== 测试2: 圆形治疗术 ====="));

        try {
            NodeSpell spell = new NodeSpell("test2", "圆形治疗术");
            spell.setTriggerType(TriggerType.SELF);

            SpellNode node1 = new SpellNode(1, 2);
            NodeComponent circle = ComponentRegistry.getInstance().getComponent("circle").copy();
            circle.setParameter("radius", 8.0);
            NodeComponent heal = ComponentRegistry.getInstance().getComponent("heal").copy();
            heal.setParameter("healing", 10.0);

            node1.addComponent(circle);
            node1.addComponent(heal);
            spell.addNode(node1);

            player.sendSystemMessage(Component.literal("§b灵力消耗: §f" + String.format("%.1f", spell.calculateTotalSpiritCost())));

            var result = NodeSpellExecutor.getInstance().execute(spell, player);

            if (result.isSuccess()) {
                player.sendSystemMessage(Component.literal("§a执行成功！影响实体: " + result.getContext().getAffectedEntityCount()));
            } else {
                player.sendSystemMessage(Component.literal("§c执行失败: " + result.getMessage()));
            }

            return 1;
        } catch (Exception e) {
            player.sendSystemMessage(Component.literal("§c错误: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * 测试3：复合效果
     * 节点1: [圆形(半径6) + 治疗(5) + 推动(2.0)]
     */
    private static int test3(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        player.sendSystemMessage(Component.literal("§6===== 测试3: 复合效果（治疗+推动） ====="));

        try {
            NodeSpell spell = new NodeSpell("test3", "复合治疗推动术");
            spell.setTriggerType(TriggerType.SELF);

            // 节点1: 3个槽位 - 圆形、治疗、推动
            SpellNode node1 = new SpellNode(1, 3);
            NodeComponent circle = ComponentRegistry.getInstance().getComponent("circle").copy();
            circle.setParameter("radius", 6.0);
            NodeComponent heal = ComponentRegistry.getInstance().getComponent("heal").copy();
            heal.setParameter("healing", 5.0);
            NodeComponent push = ComponentRegistry.getInstance().getComponent("push").copy();
            push.setParameter("force", 2.0);

            node1.addComponent(circle);
            node1.addComponent(heal);
            node1.addComponent(push);
            spell.addNode(node1);

            player.sendSystemMessage(Component.literal("§b槽位惩罚演示: 3槽位 = +10消耗"));
            player.sendSystemMessage(Component.literal("§b灵力消耗: §f" + String.format("%.1f", spell.calculateTotalSpiritCost())));

            var result = NodeSpellExecutor.getInstance().execute(spell, player);

            if (result.isSuccess()) {
                player.sendSystemMessage(Component.literal("§a执行成功！实体被治疗并推开！"));
                player.sendSystemMessage(Component.literal("§7影响实体数: " + result.getContext().getAffectedEntityCount()));
            } else {
                player.sendSystemMessage(Component.literal("§c执行失败: " + result.getMessage()));
            }

            return 1;
        } catch (Exception e) {
            player.sendSystemMessage(Component.literal("§c错误: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * 测试4：流水线执行
     * 节点1: [圆形(半径10)] - 筛选大范围实体
     * 节点2: [扇形(45度)] - 进一步筛选前方实体
     * 节点3: [伤害(8)] - 对最终筛选的实体造成伤害
     */
    private static int test4(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        player.sendSystemMessage(Component.literal("§6===== 测试4: 流水线执行 ====="));

        try {
            NodeSpell spell = new NodeSpell("test4", "流水线伤害术");
            spell.setTriggerType(TriggerType.SELF);

            // 节点1: 圆形筛选
            SpellNode node1 = new SpellNode(1, 1);
            NodeComponent circle = ComponentRegistry.getInstance().getComponent("circle").copy();
            circle.setParameter("radius", 10.0);
            node1.addComponent(circle);

            // 节点2: 扇形进一步筛选
            SpellNode node2 = new SpellNode(2, 1);
            NodeComponent cone = ComponentRegistry.getInstance().getComponent("cone").copy();
            cone.setParameter("distance", 10.0);
            cone.setParameter("angle", 45.0);
            node2.addComponent(cone);

            // 节点3: 造成伤害
            SpellNode node3 = new SpellNode(3, 1);
            NodeComponent damage = ComponentRegistry.getInstance().getComponent("damage").copy();
            damage.setParameter("damage", 8.0);
            node3.addComponent(damage);

            spell.addNode(node1);
            spell.addNode(node2);
            spell.addNode(node3);

            player.sendSystemMessage(Component.literal("§b流程: 圆形筛选 → 扇形筛选 → 伤害"));
            player.sendSystemMessage(Component.literal("§b灵力消耗: §f" + String.format("%.1f", spell.calculateTotalSpiritCost())));

            var result = NodeSpellExecutor.getInstance().execute(spell, player);

            if (result.isSuccess()) {
                player.sendSystemMessage(Component.literal("§a执行成功！"));
                player.sendSystemMessage(Component.literal("§7影响实体数: " + result.getContext().getAffectedEntityCount()));
            } else {
                player.sendSystemMessage(Component.literal("§c执行失败: " + result.getMessage()));
            }

            return 1;
        } catch (Exception e) {
            player.sendSystemMessage(Component.literal("§c错误: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * 测试5：爆炸术
     * 节点1: [扇形(距离15, 角度60) + 爆炸(威力4)]
     */
    private static int test5(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        player.sendSystemMessage(Component.literal("§6===== 测试5: 扇形爆炸术 ====="));

        try {
            NodeSpell spell = new NodeSpell("test5", "扇形爆炸术");
            spell.setTriggerType(TriggerType.SELF);

            SpellNode node1 = new SpellNode(1, 2);
            NodeComponent cone = ComponentRegistry.getInstance().getComponent("cone").copy();
            cone.setParameter("distance", 15.0);
            cone.setParameter("angle", 60.0);
            NodeComponent explosion = ComponentRegistry.getInstance().getComponent("explosion").copy();
            explosion.setParameter("power", 4.0);

            node1.addComponent(cone);
            node1.addComponent(explosion);
            spell.addNode(node1);

            player.sendSystemMessage(Component.literal("§c警告: 将在前方扇形范围产生爆炸！"));
            player.sendSystemMessage(Component.literal("§b灵力消耗: §f" + String.format("%.1f", spell.calculateTotalSpiritCost())));

            var result = NodeSpellExecutor.getInstance().execute(spell, player);

            if (result.isSuccess()) {
                player.sendSystemMessage(Component.literal("§a执行成功！"));
                player.sendSystemMessage(Component.literal("§7爆炸点数: " + result.getContext().getAffectedEntityCount()));
            } else {
                player.sendSystemMessage(Component.literal("§c执行失败: " + result.getMessage()));
            }

            return 1;
        } catch (Exception e) {
            player.sendSystemMessage(Component.literal("§c错误: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * 列出所有可用组件
     */
    private static int listComponents(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        player.sendSystemMessage(Component.literal("§6===== 可用组件列表 ====="));

        // 形状组件
        player.sendSystemMessage(Component.literal("§b【形状组件】"));
        for (NodeComponent component : ComponentRegistry.getInstance().getProjectileComponents()) {
            player.sendSystemMessage(Component.literal(String.format(
                "  §f%s §7(%s) - §e%.1f灵力",
                component.getName(),
                component.getId(),
                component.getBaseSpiritCost()
            )));
        }

        // 效果组件
        player.sendSystemMessage(Component.literal("§b【效果组件】"));
        for (NodeComponent component : ComponentRegistry.getInstance().getEffectComponents()) {
            player.sendSystemMessage(Component.literal(String.format(
                "  §f%s §7(%s) - §e%.1f灵力",
                component.getName(),
                component.getId(),
                component.getBaseSpiritCost()
            )));
        }

        player.sendSystemMessage(Component.literal("§a总计: " + ComponentRegistry.getInstance().getAllComponents().size() + " 个组件"));

        return 1;
    }

    /**
     * 保存测试术法到玉简
     */
    private static int saveToSlip(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        player.sendSystemMessage(Component.literal("§6===== 保存术法到玉简 ====="));

        try {
            // 创建测试术法（基础圆形伤害术）
            NodeSpell spell = new NodeSpell("test_spell", "§e基础圆形伤害术");
            spell.setDescription("测试用的简单范围伤害术法");
            spell.setTriggerType(TriggerType.SELF);

            SpellNode node1 = new SpellNode(1, 2);
            NodeComponent circle = ComponentRegistry.getInstance().getComponent("circle").copy();
            circle.setParameter("radius", 5.0);
            NodeComponent damage = ComponentRegistry.getInstance().getComponent("damage").copy();
            damage.setParameter("damage", 5.0);

            node1.addComponent(circle);
            node1.addComponent(damage);
            spell.addNode(node1);

            // 创建玉简物品
            ItemStack slip = SpellJadeSlipItem.createNodeSpellSlip(spell);

            // 给予玩家
            if (!player.addItem(slip)) {
                player.drop(slip, false);
            }

            player.sendSystemMessage(Component.literal("§a已创建节点术法玉简！"));
            player.sendSystemMessage(Component.literal("§7右键使用玉简施法"));
            player.sendSystemMessage(Component.literal("§b术法: " + spell.getName()));
            player.sendSystemMessage(Component.literal("§b灵力消耗: §f" + String.format("%.1f", spell.calculateTotalSpiritCost())));

            return 1;
        } catch (Exception e) {
            player.sendSystemMessage(Component.literal("§c错误: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }
}
