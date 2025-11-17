package org.example.Kangnaixi.tiandao.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.capability.CultivationCapability;
import org.example.Kangnaixi.tiandao.capability.ICultivation;
import org.example.Kangnaixi.tiandao.cultivation.*;
import org.example.Kangnaixi.tiandao.practice.PracticeMethod;
import org.example.Kangnaixi.tiandao.practice.PracticeRegistry;
import org.example.Kangnaixi.tiandao.technique.TechniqueData;
import org.example.Kangnaixi.tiandao.technique.TechniqueRegistry;

/**
 * 天道修仙系统主命令
 */
public class TiandaoCommand {

    private static final String DEFAULT_PRACTICE_METHOD = "meditation";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // 注册主命令 /tiandao
        dispatcher.register(Commands.literal("tiandao")
            .executes(TiandaoCommand::showHelp)
            .then(Commands.literal("help")
                .executes(TiandaoCommand::showDetailedHelp))
            .then(Commands.literal("status")
                .executes(TiandaoCommand::showStatus)
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(context -> showStatusForPlayer(context, EntityArgument.getPlayer(context, "player")))))
            .then(Commands.literal("setrealm")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("realm", StringArgumentType.string())
                    .suggests((context, builder) -> {
                        for (CultivationRealm realm : CultivationRealm.values()) {
                            builder.suggest(realm.name().toLowerCase());
                        }
                        return builder.buildFuture();
                    })
                    .executes(TiandaoCommand::setRealm)
                    .then(Commands.argument("level", IntegerArgumentType.integer(0))
                        .executes(TiandaoCommand::setRealmWithLevel))))
            .then(Commands.literal("setroot")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("root", StringArgumentType.string())
                    .suggests((context, builder) -> {
                        for (SpiritualRootType root : SpiritualRootType.values()) {
                            builder.suggest(root.name().toLowerCase());
                        }
                        return builder.buildFuture();
                    })
                    .executes(TiandaoCommand::setSpiritualRoot)))
            .then(Commands.literal("foundation")
                .executes(TiandaoCommand::showOwnFoundation)
                .then(Commands.literal("set")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                            .executes(TiandaoCommand::setFoundationValue))))
                .then(Commands.literal("add")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", IntegerArgumentType.integer(-100, 100))
                            .executes(TiandaoCommand::addFoundationValue))))
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(context -> showFoundationForPlayer(context.getSource(), EntityArgument.getPlayer(context, "player")))))
            .then(Commands.literal("addprogress")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                    .executes(TiandaoCommand::addProgress)))
            .then(Commands.literal("addspiritpower")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                    .executes(TiandaoCommand::addSpiritPower)))
            .then(Commands.literal("breakthrough")
                .requires(source -> source.hasPermission(2))
                .executes(TiandaoCommand::breakthrough))
            .then(Commands.literal("practice")
                .then(Commands.literal("start")
                    .executes(TiandaoCommand::practiceStart))
                .then(Commands.literal("stop")
                    .executes(TiandaoCommand::practiceStop))
                .then(Commands.literal("status")
                    .executes(TiandaoCommand::practiceStatus)))
            .then(Commands.literal("technique")
                .then(Commands.literal("list")
                    .executes(TiandaoCommand::techniqueList))
                .then(Commands.literal("learn")
                    .then(Commands.argument("techniqueId", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            for (String id : org.example.Kangnaixi.tiandao.technique.TechniqueRegistry.getInstance().getAllTechniqueIds()) {
                                builder.suggest(id);
                            }
                            return builder.buildFuture();
                        })
                        .executes(TiandaoCommand::techniqueLearn)))
                .then(Commands.literal("equip")
                    .then(Commands.argument("techniqueId", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            try {
                                ServerPlayer player = context.getSource().getPlayerOrException();
                                player.getCapability(org.example.Kangnaixi.tiandao.Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
                                    for (org.example.Kangnaixi.tiandao.technique.TechniqueData technique : cultivation.getLearnedTechniques()) {
                                        builder.suggest(technique.getId());
                                    }
                                });
                            } catch (Exception e) {}
                            return builder.buildFuture();
                        })
                        .executes(TiandaoCommand::techniqueEquip)))
                .then(Commands.literal("unequip")
                    .executes(TiandaoCommand::techniqueUnequip)))
            .then(Commands.literal("spell")
                .then(Commands.literal("list")
                    .executes(TiandaoCommand::spellList))
                .then(Commands.literal("unlock")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("spellId", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            for (String id : org.example.Kangnaixi.tiandao.spell.SpellRegistry.getInstance().getAllSpellIds()) {
                                builder.suggest(id);
                            }
                            return builder.buildFuture();
                        })
                        .executes(TiandaoCommand::spellUnlock)))
                .then(Commands.literal("cast")
                    .then(Commands.argument("spellId", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            try {
                                ServerPlayer player = context.getSource().getPlayerOrException();
                                player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
                                    for (String spellId : cultivation.getUnlockedSpells()) {
                                        builder.suggest(spellId);
                                    }
                                });
                            } catch (CommandSyntaxException e) {
                                // ignore
                            }
                            return builder.buildFuture();
                        })
                        .executes(TiandaoCommand::spellCast))))
            .then(Commands.literal("allocate")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(TiandaoCommand::allocateSpiritualRoot)
                    .then(Commands.argument("type", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            for (SpiritualRootType root : SpiritualRootType.values()) {
                                builder.suggest(root.name().toLowerCase());
                            }
                            return builder.buildFuture();
                        })
                        .executes(TiandaoCommand::allocateSpiritualRootWithType)
                        .then(Commands.argument("quality", StringArgumentType.string())
                            .suggests((context, builder) -> {
                                for (SpiritualRootQuality quality : SpiritualRootQuality.values()) {
                                    builder.suggest(quality.name().toLowerCase());
                                }
                                return builder.buildFuture();
                            })
                            .executes(TiandaoCommand::allocateSpiritualRootWithTypeAndQuality)))))
        );
        
        // 注册别名 /cultivation (向后兼容)
        dispatcher.register(Commands.literal("cultivation")
            .executes(TiandaoCommand::showHelp)
            .then(Commands.literal("help")
                .executes(TiandaoCommand::showDetailedHelp))
            .then(Commands.literal("status")
                .executes(TiandaoCommand::showStatus)
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(context -> showStatusForPlayer(context, EntityArgument.getPlayer(context, "player")))))
            .then(Commands.literal("setrealm")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("realm", StringArgumentType.string())
                    .suggests((context, builder) -> {
                        for (CultivationRealm realm : CultivationRealm.values()) {
                            builder.suggest(realm.name().toLowerCase());
                        }
                        return builder.buildFuture();
                    })
                    .executes(TiandaoCommand::setRealm)
                    .then(Commands.argument("level", IntegerArgumentType.integer(0))
                        .executes(TiandaoCommand::setRealmWithLevel))))
            .then(Commands.literal("setroot")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("root", StringArgumentType.string())
                    .suggests((context, builder) -> {
                        for (SpiritualRootType root : SpiritualRootType.values()) {
                            builder.suggest(root.name().toLowerCase());
                        }
                        return builder.buildFuture();
                    })
                    .executes(TiandaoCommand::setSpiritualRoot)))
            .then(Commands.literal("foundation")
                .executes(TiandaoCommand::showOwnFoundation)
                .then(Commands.literal("set")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                            .executes(TiandaoCommand::setFoundationValue))))
                .then(Commands.literal("add")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", IntegerArgumentType.integer(-100, 100))
                            .executes(TiandaoCommand::addFoundationValue))))
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(context -> showFoundationForPlayer(context.getSource(), EntityArgument.getPlayer(context, "player")))))
            .then(Commands.literal("addprogress")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                    .executes(TiandaoCommand::addProgress)))
            .then(Commands.literal("addspiritpower")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                    .executes(TiandaoCommand::addSpiritPower)))
            .then(Commands.literal("breakthrough")
                .requires(source -> source.hasPermission(2))
                .executes(TiandaoCommand::breakthrough))
            .then(Commands.literal("allocate")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(TiandaoCommand::allocateSpiritualRoot)
                    .then(Commands.argument("type", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            for (SpiritualRootType root : SpiritualRootType.values()) {
                                builder.suggest(root.name().toLowerCase());
                            }
                            return builder.buildFuture();
                        })
                        .executes(TiandaoCommand::allocateSpiritualRootWithType)
                        .then(Commands.argument("quality", StringArgumentType.string())
                            .suggests((context, builder) -> {
                                for (SpiritualRootQuality quality : SpiritualRootQuality.values()) {
                                    builder.suggest(quality.name().toLowerCase());
                                }
                                return builder.buildFuture();
                            })
                            .executes(TiandaoCommand::allocateSpiritualRootWithTypeAndQuality)))))
        );
    }

    private static int showHelp(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal("=== 天道修仙系统 v1.0 ==="), false);
        context.getSource().sendSuccess(() -> Component.literal("使用 /tiandao help 查看完整命令列表"), false);
        context.getSource().sendSuccess(() -> Component.literal("常用命令:"), false);
        context.getSource().sendSuccess(() -> Component.literal("  /tiandao status - 查看修仙状态"), false);
        context.getSource().sendSuccess(() -> Component.literal("  /tiandao foundation - 查看根基状态"), false);
        context.getSource().sendSuccess(() -> Component.literal("  /tiandao help - 完整帮助"), false);
        return 1;
    }

    private static int showDetailedHelp(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal("=== 天道修仙系统命令帮助 ==="), false);
        context.getSource().sendSuccess(() -> Component.literal(""), false);
        context.getSource().sendSuccess(() -> Component.literal("玩家命令:"), false);
        context.getSource().sendSuccess(() -> Component.literal("  /tiandao status - 查看自己的修仙状态"), false);
        context.getSource().sendSuccess(() -> Component.literal("  /tiandao status <玩家> - 查看其他玩家状态"), false);
        context.getSource().sendSuccess(() -> Component.literal("  /tiandao foundation [玩家] - 查看根基状态"), false);
        context.getSource().sendSuccess(() -> Component.literal(""), false);
        context.getSource().sendSuccess(() -> Component.literal("管理员命令 (需要OP权限):"), false);
        context.getSource().sendSuccess(() -> Component.literal("  /tiandao setrealm <境界> [等级] - 设置境界"), false);
        context.getSource().sendSuccess(() -> Component.literal("  /tiandao setroot <灵根> - 设置灵根"), false);
        context.getSource().sendSuccess(() -> Component.literal("  /tiandao allocate <玩家> [类型] [品质] - 分配灵根"), false);
        context.getSource().sendSuccess(() -> Component.literal("  /tiandao foundation set <玩家> <数值> - 直接设置根基"), false);
        context.getSource().sendSuccess(() -> Component.literal("  /tiandao foundation add <玩家> <变化量> - 调整根基"), false);
        context.getSource().sendSuccess(() -> Component.literal("  /tiandao addprogress <数量> - 增加修炼进度"), false);
        context.getSource().sendSuccess(() -> Component.literal("  /tiandao addspiritpower <数量> - 增加灵力"), false);
        context.getSource().sendSuccess(() -> Component.literal("  /tiandao breakthrough - 强制突破"), false);
        context.getSource().sendSuccess(() -> Component.literal(""), false);
        context.getSource().sendSuccess(() -> Component.literal("提示: 命令支持Tab自动补全"), false);
        context.getSource().sendSuccess(() -> Component.literal("提示: /cultivation 是 /tiandao 的别名"), false);
        return 1;
    }

    private static int showStatus(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        return showStatusForPlayer(context, player);
    }

    private static int showStatusForPlayer(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        final CommandSourceStack source = context.getSource();
        player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
            // 显示灵根信息
            SpiritualRootType rootType = cultivation.getSpiritualRoot();
            final String rootName = rootType != null ? rootType.getDisplayName() : "无";
            
            // 从cultivation获取完整的灵根对象来获取品质
            final String rootQuality;
            if (cultivation.getSpiritualRootObject() != null) {
                rootQuality = cultivation.getSpiritualRootObject().getQuality().getDisplayName();
            } else {
                rootQuality = "无";
            }
            
            // 显示境界信息
            CultivationRealm realm = cultivation.getRealm();
            final String realmName = realm != null ? realm.getDisplayName() : "凡人";
            
            // 显示修炼进度
            double progress = cultivation.getCultivationProgress();
            double required = realm != null ? realm.getRequiredProgress(cultivation.getLevel()) : 100;
            final double percentage = (progress / required) * 100;
            
            // 显示灵力信息
            double spiritPower = cultivation.getCurrentSpiritPower();
            double maxSpiritPower = cultivation.getMaxSpiritPower();
            final double spiritPercentage = (spiritPower / maxSpiritPower) * 100;
            
            // 发送状态信息
            source.sendSuccess(() -> Component.literal("===== 修仙状态 ====="), true);
            source.sendSuccess(() -> Component.literal("灵根: " + rootName + " (" + rootQuality + ")"), true);
            source.sendSuccess(() -> Component.literal("境界: " + realmName + " " + cultivation.getLevel() + "级"), true);
            source.sendSuccess(() -> Component.literal("修炼进度: " + String.format("%.1f", percentage) + "%"), true);
            source.sendSuccess(() -> Component.literal("灵力: " + String.format("%.1f", spiritPower) + "/" + String.format("%.1f", maxSpiritPower) + " (" + String.format("%.1f", spiritPercentage) + "%)"), true);
            source.sendSuccess(() -> Component.literal("修炼速度加成: " + String.format("%.1f", cultivation.getCultivationBonus() * 100) + "%"), true);
            source.sendSuccess(() -> Component.literal("灵力恢复速度: " + String.format("%.2f", cultivation.getSpiritPowerRecoveryRate()) + "/秒"), true);
        });
        
        return 1;
    }

    private static int showOwnFoundation(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        return showFoundationForPlayer(context.getSource(), player);
    }

    private static int showFoundationForPlayer(CommandSourceStack source, ServerPlayer player) {
        return player.getCapability(Tiandao.CULTIVATION_CAPABILITY).map(cultivation -> {
            int foundation = cultivation.getFoundation();
            source.sendSuccess(() -> buildFoundationComponent(player, foundation), false);
            return 1;
        }).orElseGet(() -> {
            source.sendFailure(Component.literal("未找到该玩家的修仙数据"));
            return 0;
        });
    }

    private static int setFoundationValue(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        int value = IntegerArgumentType.getInteger(context, "value");
        return target.getCapability(Tiandao.CULTIVATION_CAPABILITY).map(cultivation -> {
            cultivation.setFoundation(value);
            org.example.Kangnaixi.tiandao.network.NetworkHandler.sendToPlayer(
                new org.example.Kangnaixi.tiandao.network.CultivationDataSyncPacket(cultivation),
                target
            );
            context.getSource().sendSuccess(() ->
                Component.literal("已将 " + target.getName().getString() + " 的根基设置为 " + cultivation.getFoundation()), true);
            sendFoundationUpdateToPlayer(target, cultivation.getFoundation());
            return 1;
        }).orElseGet(() -> {
            context.getSource().sendFailure(Component.literal("未找到该玩家的修仙数据"));
            return 0;
        });
    }

    private static int addFoundationValue(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        int amount = IntegerArgumentType.getInteger(context, "amount");
        return target.getCapability(Tiandao.CULTIVATION_CAPABILITY).map(cultivation -> {
            int before = cultivation.getFoundation();
            cultivation.addFoundation(amount);
            int after = cultivation.getFoundation();
            org.example.Kangnaixi.tiandao.network.NetworkHandler.sendToPlayer(
                new org.example.Kangnaixi.tiandao.network.CultivationDataSyncPacket(cultivation),
                target
            );
            context.getSource().sendSuccess(() ->
                Component.literal(String.format("已调整 %s 的根基: %d → %d (变化 %+d)",
                    target.getName().getString(), before, after, after - before)), true);
            sendFoundationUpdateToPlayer(target, after);
            return 1;
        }).orElseGet(() -> {
            context.getSource().sendFailure(Component.literal("未找到该玩家的修仙数据"));
            return 0;
        });
    }

    private static void sendFoundationUpdateToPlayer(ServerPlayer player, int foundation) {
        FoundationSystem.FoundationDescriptor descriptor = FoundationSystem.describeFoundation(foundation);
        player.sendSystemMessage(Component.literal(
            String.format("§a当前根基: %d (%s)", foundation, descriptor.label())
        ));
    }

    private static Component buildFoundationComponent(ServerPlayer player, int foundation) {
        FoundationSystem.FoundationDescriptor descriptor = FoundationSystem.describeFoundation(foundation);
        return Component.literal("玩家 " + player.getName().getString() + " 根基: " + foundation + " (")
            .append(Component.literal(descriptor.label())
                .withStyle(style -> style.withColor(TextColor.fromRgb(descriptor.color()))))
            .append(Component.literal(")"));
    }

    private static int setRealm(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String realmName = StringArgumentType.getString(context, "realm");
        final CommandSourceStack source = context.getSource();
        
        try {
            CultivationRealm realm = CultivationRealm.valueOf(realmName.toUpperCase());
            player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
                cultivation.setRealm(realm);
                cultivation.setLevel(0);
                cultivation.setCultivationProgress(0);
                cultivation.setMaxSpiritPower(realm.getBaseMaxEnergy());
                cultivation.setSpiritPower(realm.getBaseMaxEnergy());
                
                source.sendSuccess(() -> Component.literal("已将境界设置为: " + realm.getDisplayName() + " 0级"), true);
            });
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal("无效的境界名称: " + realmName));
            return 0;
        }
        
        return 1;
    }

    private static int setRealmWithLevel(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String realmName = StringArgumentType.getString(context, "realm");
        int level = IntegerArgumentType.getInteger(context, "level");
        final CommandSourceStack source = context.getSource();
        
        try {
            CultivationRealm realm = CultivationRealm.valueOf(realmName.toUpperCase());
            player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
                cultivation.setRealm(realm);
                cultivation.setLevel(level);
                cultivation.setCultivationProgress(0);
                cultivation.setMaxSpiritPower(realm.getBaseMaxEnergy());
                cultivation.setSpiritPower(realm.getBaseMaxEnergy());
                
                source.sendSuccess(() -> Component.literal("已将境界设置为: " + realm.getDisplayName() + " " + level + "级"), true);
            });
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal("无效的境界名称: " + realmName));
            return 0;
        }
        
        return 1;
    }

    private static int setSpiritualRoot(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String rootName = StringArgumentType.getString(context, "root");
        final CommandSourceStack source = context.getSource();
        
        try {
            SpiritualRootType root = SpiritualRootType.valueOf(rootName.toUpperCase());
            player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
                cultivation.setSpiritualRoot(root);
                
                final String rootQuality;
                if (cultivation.getSpiritualRootObject() != null) {
                    rootQuality = cultivation.getSpiritualRootObject().getQuality().getDisplayName();
                } else {
                    rootQuality = "未知";
                }
                
                source.sendSuccess(() -> Component.literal("已将灵根设置为: " + root.getDisplayName() + " (" + rootQuality + ")"), true);
            });
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal("无效的灵根名称: " + rootName));
            return 0;
        }
        
        return 1;
    }

    private static int addProgress(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        int amount = IntegerArgumentType.getInteger(context, "amount");
        final CommandSourceStack source = context.getSource();
        
        player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
            cultivation.addCultivationProgress(amount);
            source.sendSuccess(() -> Component.literal("已增加修炼进度: " + amount), true);
        });
        
        return 1;
    }

    private static int addSpiritPower(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        int amount = IntegerArgumentType.getInteger(context, "amount");
        final CommandSourceStack source = context.getSource();
        
        player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
            cultivation.addSpiritPower(amount);
            source.sendSuccess(() -> Component.literal("已增加灵力: " + amount), true);
        });
        
        return 1;
    }

    private static int breakthrough(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        final CommandSourceStack source = context.getSource();
        
        player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
            boolean success = cultivation.tryBreakthrough();
            
            if (success) {
                source.sendSuccess(() -> Component.literal("突破成功！当前境界: " + cultivation.getRealm().getDisplayName() + " " + cultivation.getLevel() + "级"), true);
            } else {
                source.sendFailure(Component.literal("突破失败，可能需要更多修炼进度"));
            }
        });
        
        return 1;
    }
    
    private static int allocateSpiritualRoot(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        final CommandSourceStack source = context.getSource();
        
        player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
            SpiritualRootType oldRootType = cultivation.getSpiritualRoot();
            final String oldRootName = oldRootType != null ? oldRootType.getDisplayName() : "无";
            
            final String oldRootQuality;
            if (cultivation.getSpiritualRootObject() != null) {
                oldRootQuality = cultivation.getSpiritualRootObject().getQuality().getDisplayName();
            } else {
                oldRootQuality = "无";
            }
            
            // 使用新的分配器随机分配灵根
            org.example.Kangnaixi.tiandao.cultivation.SpiritualRootAssigner.assignRandomRoot(cultivation, player);
            cultivation.setRootAssigned(true);
            
            SpiritualRoot newRoot = cultivation.getSpiritualRootObject();
            
            source.sendSuccess(() -> Component.literal("已为玩家 " + player.getName().getString() + " 随机分配灵根"), true);
            source.sendSuccess(() -> Component.literal("原灵根: " + oldRootName + " (" + oldRootQuality + ")"), true);
            source.sendSuccess(() -> Component.literal("新灵根: " + newRoot.getType().getDisplayName() + " (" + newRoot.getQuality().getDisplayName() + ")"), true);
            
            // 向玩家发送分配消息
            sendRootAssignmentMessage(player, cultivation);
        });
        
        return 1;
    }
    
    private static int allocateSpiritualRootWithType(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        String rootName = StringArgumentType.getString(context, "type");
        final CommandSourceStack source = context.getSource();
        
        try {
            SpiritualRootType rootType = SpiritualRootType.valueOf(rootName.toUpperCase());
            player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
                SpiritualRoot newRoot = new SpiritualRoot(rootType);
                cultivation.setSpiritualRootObject(newRoot);
                cultivation.setRootAssigned(true);
                
                source.sendSuccess(() -> Component.literal("已为玩家 " + player.getName().getString() + " 分配指定灵根"), true);
                source.sendSuccess(() -> Component.literal("新灵根: " + newRoot.getType().getDisplayName() + " (" + newRoot.getQuality().getDisplayName() + ")"), true);
                
                // 向玩家发送分配消息
                sendRootAssignmentMessage(player, cultivation);
            });
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal("无效的灵根名称: " + rootName));
            return 0;
        }
        
        return 1;
    }
    
    private static int allocateSpiritualRootWithTypeAndQuality(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        String rootName = StringArgumentType.getString(context, "type");
        String qualityName = StringArgumentType.getString(context, "quality");
        final CommandSourceStack source = context.getSource();
        
        try {
            SpiritualRootType rootType = SpiritualRootType.valueOf(rootName.toUpperCase());
            SpiritualRootQuality quality = SpiritualRootQuality.valueOf(qualityName.toUpperCase());
            
            player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
                SpiritualRoot newRoot = new SpiritualRoot(rootType, quality);
                cultivation.setSpiritualRootObject(newRoot);
                cultivation.setRootAssigned(true);
                
                source.sendSuccess(() -> Component.literal("已为玩家 " + player.getName().getString() + " 分配指定灵根和品质"), true);
                source.sendSuccess(() -> Component.literal("新灵根: " + newRoot.getType().getDisplayName() + " (" + newRoot.getQuality().getDisplayName() + ")"), true);
                source.sendSuccess(() -> Component.literal("特殊能力: " + newRoot.getSpecialAbility()), true);
                source.sendSuccess(() -> Component.literal("修炼速度加成: " + String.format("%.1f", newRoot.getCultivationSpeedBonus() * 100) + "%"), true);
                
                // 向玩家发送分配消息
                sendRootAssignmentMessage(player, cultivation);
            });
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains(qualityName.toUpperCase())) {
                source.sendFailure(Component.literal("无效的品质名称: " + qualityName));
            } else {
                source.sendFailure(Component.literal("无效的灵根名称: " + rootName));
            }
            return 0;
        }
        
        return 1;
    }
    
    /**
     * 列出已解锁的术法
     */
    private static int spellList(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();

        player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
            java.util.List<String> unlockedSpells = cultivation.getUnlockedSpells();

            if (unlockedSpells.isEmpty()) {
                context.getSource().sendFailure(Component.literal("§c你还没有解锁任何术法"));
                return;
            }

            context.getSource().sendSuccess(() -> Component.literal("§6§l已解锁的术法"), false);

            for (String spellId : unlockedSpells) {
                org.example.Kangnaixi.tiandao.spell.definition.SpellDefinition spell =
                    org.example.Kangnaixi.tiandao.spell.SpellRegistry.getInstance().getSpellById(spellId);

                if (spell != null) {
                    int cooldownRemaining = cultivation.getSpellCooldownRemaining(spellId);
                    boolean isActive = cultivation.isSpellActive(spellId);

                    final String statusText;
                    if (isActive) {
                        statusText = " §a[激活中]";
                    } else if (cooldownRemaining > 0) {
                        statusText = " §c[冷却: " + cooldownRemaining + "s]";
                    } else {
                        statusText = " §7[可用]";
                    }

                    final String displayName = spell.getMetadata().displayName();
                    final String spellDesc = spell.getMetadata().description().split("\n")[0];
                    final double spiritCost = spell.getBaseStats().spiritCost();
                    final double cooldown = spell.getBaseStats().cooldownSeconds();
                    final String id = spellId;

                    context.getSource().sendSuccess(() -> Component.literal(
                        String.format("§e%s §7- §f%s%s",
                            displayName,
                            spellDesc,
                            statusText)
                    ), false);

                    context.getSource().sendSuccess(() -> Component.literal(
                        String.format("  §7灵力: §b%.0f点 §7| §b冷却: §b%.1fs §7| ID: §8%s",
                            spiritCost,
                            cooldown,
                            id)
                    ), false);
                }
            }

            context.getSource().sendSuccess(() -> Component.literal("§7共 §a" + unlockedSpells.size() + " §7条术法"), false);
        });

        return 1;
    }    /**
     * 解锁术法
     */
    /**
     * 解锁术法
     */
    private static int spellUnlock(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String spellId = StringArgumentType.getString(context, "spellId");

        org.example.Kangnaixi.tiandao.spell.definition.SpellDefinition spell =
            org.example.Kangnaixi.tiandao.spell.SpellRegistry.getInstance().getSpellById(spellId);

        if (spell == null) {
            context.getSource().sendFailure(Component.literal("§c术法不存在: " + spellId));
            return 0;
        }

        player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
            if (cultivation.hasSpell(spellId)) {
                context.getSource().sendFailure(Component.literal("§c你已经掌握该术法"));
                return;
            }

            cultivation.unlockSpell(spellId);

            context.getSource().sendSuccess(() -> Component.literal(""), false);
            context.getSource().sendSuccess(() -> Component.literal("§6§l恭喜获得新术法"), false);
            context.getSource().sendSuccess(() -> Component.literal("§e" + spell.getMetadata().displayName()), false);
            context.getSource().sendSuccess(() -> Component.literal(""), false);
            context.getSource().sendSuccess(() -> Component.literal("§7" + spell.getMetadata().description()), false);
            context.getSource().sendSuccess(() -> Component.literal(""), false);
            context.getSource().sendSuccess(() -> Component.literal(
                String.format("§b灵力: §f%.0f点 §7| §b冷却: §f%.1fs",
                    spell.getBaseStats().spiritCost(),
                    spell.getBaseStats().cooldownSeconds())
            ), false);

            player.playSound(net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, 1.0f, 1.0f);

            org.example.Kangnaixi.tiandao.network.NetworkHandler.sendToPlayer(
                new org.example.Kangnaixi.tiandao.network.CultivationDataSyncPacket(cultivation),
                player
            );

            Tiandao.LOGGER.info("玩家 {} 解锁术法: {}", player.getName().getString(), spell.getMetadata().displayName());
        });

        return 1;
    }    /**
     * 释放术法
     */
    /**
     * 通过命令施法
     */
    private static int spellCast(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String spellId = StringArgumentType.getString(context, "spellId");

        player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
            if (!cultivation.hasSpell(spellId)) {
                context.getSource().sendFailure(Component.literal("§c你还没有解锁该术法"));
                return;
            }

            org.example.Kangnaixi.tiandao.spell.definition.SpellDefinition spell =
                org.example.Kangnaixi.tiandao.spell.SpellRegistry.getInstance().getSpellById(spellId);

            if (spell == null) {
                context.getSource().sendFailure(Component.literal("§c术法不存在: " + spellId));
                return;
            }

            org.example.Kangnaixi.tiandao.spell.runtime.SpellCastingService.CastResult castResult =
                org.example.Kangnaixi.tiandao.spell.runtime.SpellCastingService.cast(player, cultivation, spell);

            if (castResult.success()) {
                double cost = castResult.runtimeResult().numbers().spiritCost();
                context.getSource().sendSuccess(() -> Component.literal(
                    "§a成功施放术法: §e" + spell.getMetadata().displayName()), true);
                ExperienceConversionSystem.onSpiritConsumed(player, cost);
            } else if (castResult.failureReason() == org.example.Kangnaixi.tiandao.spell.runtime.SpellCastingService.CastResult.FailureReason.COOLDOWN) {
                context.getSource().sendFailure(Component.literal("§c术法冷却中，剩余 " + castResult.cooldownRemaining() + " 秒"));
            } else if (castResult.failureReason() == org.example.Kangnaixi.tiandao.spell.runtime.SpellCastingService.CastResult.FailureReason.SPIRIT) {
                context.getSource().sendFailure(Component.literal(
                    String.format("§c灵力不足，需要 %.0f 当前 %.0f",
                        castResult.expectedSpirit(),
                        castResult.currentSpirit())));
            } else {
                context.getSource().sendFailure(Component.literal("§c无法施放该术法"));
            }
        });

        return 1;
    }

    private static int practiceStart(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String methodId = resolvePracticeMethodId();
        if (methodId == null) {
            context.getSource().sendFailure(Component.literal("§c尚未注册任何修炼方式"));
            return 0;
        }
        boolean started = PracticeTickHandler.startPractice(player, methodId);
        if (started) {
            context.getSource().sendSuccess(() -> Component.literal("§a开始修炼方式: §f" + methodId), false);
            return 1;
        }
        return 0;
    }

    private static int practiceStop(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
            if (cultivation.isPracticing()) {
                PracticeTickHandler.stopPractice(player, "command");
                context.getSource().sendSuccess(() -> Component.literal("§e已停止修炼"), false);
            } else {
                context.getSource().sendSuccess(() -> Component.literal("§7当前未在修炼"), false);
            }
        });
        return 1;
    }

    private static int practiceStatus(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        return player.getCapability(Tiandao.CULTIVATION_CAPABILITY).map(cultivation -> {
            if (cultivation.isPracticing()) {
                String methodId = cultivation.getCurrentPracticeMethod();
                PracticeMethod method = PracticeRegistry.getInstance().getPracticeMethod(methodId);
                String display = method != null ? method.getDisplayName() : methodId;
                context.getSource().sendSuccess(() -> Component.literal("§a正在修炼: §f" + display), false);
            } else {
                context.getSource().sendSuccess(() -> Component.literal("§7当前未在修炼"), false);
            }
            return 1;
        }).orElse(0);
    }

    private static String resolvePracticeMethodId() {
        PracticeRegistry registry = PracticeRegistry.getInstance();
        if (registry.isRegistered(DEFAULT_PRACTICE_METHOD)) {
            return DEFAULT_PRACTICE_METHOD;
        }
        return registry.getAllPracticeIds().stream().findFirst().orElse(null);
    }

    private static int techniqueList(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        return player.getCapability(Tiandao.CULTIVATION_CAPABILITY).map(cultivation -> {
            java.util.List<TechniqueData> learned = cultivation.getLearnedTechniques();
            if (learned.isEmpty()) {
                context.getSource().sendSuccess(() -> Component.literal("§7尚未学习任何功法"), false);
                return 0;
            }
            context.getSource().sendSuccess(() -> Component.literal("§6=== 已学习功法 ==="), false);
            TechniqueData equipped = cultivation.getEquippedTechnique();
            for (TechniqueData technique : learned) {
                boolean isEquipped = equipped != null && equipped.getId().equals(technique.getId());
                String prefix = isEquipped ? "§a[已装备] " : "§7";
                String line = prefix + technique.getName() + " §8(Lv." + technique.getLevel() + ")";
                context.getSource().sendSuccess(() -> Component.literal(line), false);
            }
            return 1;
        }).orElse(0);
    }

    private static int techniqueLearn(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String techniqueId = StringArgumentType.getString(context, "techniqueId");
        TechniqueData template = TechniqueRegistry.getInstance().getTechniqueById(techniqueId);
        if (template == null) {
            context.getSource().sendFailure(Component.literal("§c功法不存在: " + techniqueId));
            return 0;
        }
        return player.getCapability(Tiandao.CULTIVATION_CAPABILITY).map(cultivation -> {
            if (cultivation.hasTechnique(techniqueId)) {
                context.getSource().sendFailure(Component.literal("§e已经掌握该功法"));
                return 0;
            }
            if (template.getRequiredRoot() != SpiritualRootType.NONE
                && cultivation.getSpiritualRoot() != template.getRequiredRoot()) {
                context.getSource().sendFailure(Component.literal("§c灵根不符合要求"));
                return 0;
            }
            CultivationRealm playerRealm = cultivation.getRealm();
            if (playerRealm.ordinal() < template.getRequiredRealm().ordinal()) {
                context.getSource().sendFailure(Component.literal("§c境界不足，无法学习"));
                return 0;
            }
            if (playerRealm == template.getRequiredRealm()
                && cultivation.getLevel() < template.getRequiredLevel()) {
                context.getSource().sendFailure(Component.literal("§c境界层级不足，无法学习"));
                return 0;
            }
            boolean learned = cultivation.learnTechnique(template);
            if (learned) {
                context.getSource().sendSuccess(() -> Component.literal("§a学会功法: §f" + template.getName()), false);
                return 1;
            }
            context.getSource().sendFailure(Component.literal("§c学习功法失败"));
            return 0;
        }).orElse(0);
    }

    private static int techniqueEquip(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String techniqueId = StringArgumentType.getString(context, "techniqueId");
        return player.getCapability(Tiandao.CULTIVATION_CAPABILITY).map(cultivation -> {
            if (!cultivation.hasTechnique(techniqueId)) {
                context.getSource().sendFailure(Component.literal("§c尚未学会该功法"));
                return 0;
            }
            if (cultivation.equipTechnique(techniqueId)) {
                context.getSource().sendSuccess(() -> Component.literal("§a已装备功法: §f" + techniqueId), false);
                return 1;
            }
            context.getSource().sendFailure(Component.literal("§c装备功法失败"));
            return 0;
        }).orElse(0);
    }

    private static int techniqueUnequip(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        return player.getCapability(Tiandao.CULTIVATION_CAPABILITY).map(cultivation -> {
            if (!cultivation.hasEquippedTechnique()) {
                context.getSource().sendFailure(Component.literal("§7当前没有装备功法"));
                return 0;
            }
            cultivation.unequipTechnique();
            context.getSource().sendSuccess(() -> Component.literal("§e已卸下当前功法"), false);
            return 1;
        }).orElse(0);
    }

    private static void sendRootAssignmentMessage(ServerPlayer player, ICultivation cultivation) {
        SpiritualRootType rootType = cultivation.getSpiritualRoot();
        SpiritualRoot root = null;
        if (cultivation instanceof CultivationCapability cap) {
            root = cap.getSpiritualRootObject();
        }
        if (rootType == SpiritualRootType.NONE || root == null) {
            player.sendSystemMessage(Component.literal("§7=== 天命测定 ==="));
            player.sendSystemMessage(Component.literal("§8你是凡人之躯，无法感知灵气。"));
            player.sendSystemMessage(Component.literal("§8需寻求机缘，方可踏入修仙之路。"));
            return;
        }
        SpiritualRootQuality quality = root.getQuality();
        String qualityName = quality != null ? quality.getDisplayName() : "未知";
        String colorCode = quality != null ? toColorCode(quality.getColor()) : "§f";
        player.sendSystemMessage(Component.literal("§6=== 天命测定 ==="));
        player.sendSystemMessage(Component.literal("§f你拥有 " + rootType.getDisplayName() + "，品质：" + colorCode + qualityName));
        if (quality != null) {
            player.sendSystemMessage(Component.literal("§7修炼效率：§a×" + String.format("%.1f", quality.getCultivationBonus())));
            if (quality == SpiritualRootQuality.PERFECT) {
                player.sendSystemMessage(Component.literal("§e§l天降祥瑞！你拥有传说中的天灵根！"));
            } else if (quality == SpiritualRootQuality.EXCELLENT) {
                player.sendSystemMessage(Component.literal("§d恭喜！极品灵根，千年难遇！"));
            }
        }
    }

    private static String toColorCode(int rgb) {
        String hex = String.format("%06X", rgb & 0xFFFFFF);
        StringBuilder builder = new StringBuilder("§x");
        for (char c : hex.toCharArray()) {
            builder.append('§').append(Character.toLowerCase(c));
        }
        return builder.toString();
    }
}
