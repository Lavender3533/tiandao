package org.example.Kangnaixi.tiandao.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.example.Kangnaixi.tiandao.spell.entity.ModEntityTypes;
import org.example.Kangnaixi.tiandao.spell.node.ComponentRegistry;
import org.example.Kangnaixi.tiandao.spell.node.NodeComponent;
import org.example.Kangnaixi.tiandao.spell.node.NodeSpell;
import org.example.Kangnaixi.tiandao.spell.node.SpellNode;
import org.example.Kangnaixi.tiandao.spell.node.TriggerType;
import org.example.Kangnaixi.tiandao.spell.node.entity.NodeSpellProjectileEntity;

/**
 * Simple command to spawn a test fireball projectile.
 */
public class ProjectileTestCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("projectiletest")
                .then(Commands.literal("fireball").executes(ProjectileTestCommand::testFireball))
        );
    }

    private static int testFireball(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        player.sendSystemMessage(Component.literal("Launching demo fireball..."));

        try {
            NodeSpell spell = new NodeSpell("test_fireball", "Test Fireball");
            spell.setTriggerType(TriggerType.SELF);

            SpellNode impactNode = new SpellNode(1, 2);
            NodeComponent circle = ComponentRegistry.getInstance().getComponent("circle").copy();
            circle.setParameter("radius", 3.0);
            NodeComponent explosion = ComponentRegistry.getInstance().getComponent("explosion").copy();
            explosion.setParameter("power", 3.0);
            impactNode.addComponent(circle);
            impactNode.addComponent(explosion);
            spell.addNode(impactNode);

            NodeSpellProjectileEntity projectile = new NodeSpellProjectileEntity(
                ModEntityTypes.NODE_SPELL_PROJECTILE.get(),
                player.level()
            );
            projectile.setPos(player.getEyePosition());
            projectile.setOwner(player);
            var dir = player.getLookAngle();
            projectile.shoot(dir.x, dir.y, dir.z, 1.6F, 0.2F);
            projectile.setSpellData(spell, 0);
            projectile.setTemplateId("tiandao:projectile/basic");
            player.level().addFreshEntity(projectile);

            player.sendSystemMessage(Component.literal("Fireball launched."));
            return 1;
        } catch (Exception e) {
            player.sendSystemMessage(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }
}
