package org.example.Kangnaixi.tiandao.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.network.NetworkHandler;

/**
 * 术法快捷栏调试命令: /spellruntime bind/clear/list.
 */
public class SpellHotbarCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("spellruntime")
                .then(Commands.literal("bind")
                    .then(Commands.argument("slot", IntegerArgumentType.integer(1, 9))
                        .then(Commands.argument("spellId", StringArgumentType.string())
                            .executes(SpellHotbarCommand::bindSpell))))
                .then(Commands.literal("clear")
                    .then(Commands.argument("slot", IntegerArgumentType.integer(1, 9))
                        .executes(SpellHotbarCommand::clearSlot)))
                .then(Commands.literal("list")
                    .executes(SpellHotbarCommand::listHotbar))
        );
    }

    private static int bindSpell(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("此命令只能由玩家执行"));
            return 0;
        }
        int slot = IntegerArgumentType.getInteger(context, "slot");
        String spellId = StringArgumentType.getString(context, "spellId");

        player.getCapability(Tiandao.SPELL_HOTBAR_CAP).ifPresent(hotbar -> {
            hotbar.setSlot(slot - 1, spellId);
            player.sendSystemMessage(Component.literal(
                "§a已将术法 §e" + spellId + " §a绑定到槽位 §b" + slot
            ));
            NetworkHandler.sendSpellHotbarSyncToPlayer(hotbar, player);
        });
        return 1;
    }

    private static int clearSlot(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("此命令只能由玩家执行"));
            return 0;
        }
        int slot = IntegerArgumentType.getInteger(context, "slot");

        player.getCapability(Tiandao.SPELL_HOTBAR_CAP).ifPresent(hotbar -> {
            hotbar.setSlot(slot - 1, null);
            player.sendSystemMessage(Component.literal("§7已清空槽位 §b" + slot));
            NetworkHandler.sendSpellHotbarSyncToPlayer(hotbar, player);
        });
        return 1;
    }

    private static int listHotbar(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("此命令只能由玩家执行"));
            return 0;
        }

        player.getCapability(Tiandao.SPELL_HOTBAR_CAP).ifPresent(hotbar -> {
            player.sendSystemMessage(Component.literal("§6=== 术法快捷栏 ==="));
            for (int i = 0; i < 9; i++) {
                String spellId = hotbar.getSlot(i);
                boolean isActive = (i == hotbar.getActiveIndex());
                String prefix = isActive ? "§a▶ " : "§7  ";
                String display = spellId == null || spellId.isEmpty() ? "§8<空>" : "§e" + spellId;
                player.sendSystemMessage(Component.literal(prefix + "§b[" + (i + 1) + "] " + display));
            }
        });
        return 1;
    }
}
