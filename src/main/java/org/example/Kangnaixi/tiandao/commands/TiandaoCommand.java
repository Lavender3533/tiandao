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
import net.minecraft.server.level.ServerPlayer;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.cultivation.CultivationRealm;
import org.example.Kangnaixi.tiandao.cultivation.SpiritualRoot;
import org.example.Kangnaixi.tiandao.cultivation.SpiritualRootQuality;
import org.example.Kangnaixi.tiandao.cultivation.SpiritualRootType;

/**
 * 天道修仙系统主命令
 */
public class TiandaoCommand {

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
        context.getSource().sendSuccess(() -> Component.literal("  /tiandao help - 完整帮助"), false);
        return 1;
    }
    
    private static int showDetailedHelp(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal("=== 天道修仙系统命令帮助 ==="), false);
        context.getSource().sendSuccess(() -> Component.literal(""), false);
        context.getSource().sendSuccess(() -> Component.literal("玩家命令:"), false);
        context.getSource().sendSuccess(() -> Component.literal("  /tiandao status - 查看自己的修仙状态"), false);
        context.getSource().sendSuccess(() -> Component.literal("  /tiandao status <玩家> - 查看其他玩家状态"), false);
        context.getSource().sendSuccess(() -> Component.literal(""), false);
        context.getSource().sendSuccess(() -> Component.literal("管理员命令 (需要OP权限):"), false);
        context.getSource().sendSuccess(() -> Component.literal("  /tiandao setrealm <境界> [等级] - 设置境界"), false);
        context.getSource().sendSuccess(() -> Component.literal("  /tiandao setroot <灵根> - 设置灵根"), false);
        context.getSource().sendSuccess(() -> Component.literal("  /tiandao allocate <玩家> [类型] [品质] - 分配灵根"), false);
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
     * 发送灵根分配消息
     */
    private static void sendRootAssignmentMessage(ServerPlayer player, org.example.Kangnaixi.tiandao.capability.ICultivation cultivation) {
        SpiritualRootType rootType = cultivation.getSpiritualRoot();
        
        if (cultivation instanceof org.example.Kangnaixi.tiandao.capability.CultivationCapability) {
            org.example.Kangnaixi.tiandao.capability.CultivationCapability cap = (org.example.Kangnaixi.tiandao.capability.CultivationCapability) cultivation;
            SpiritualRoot root = cap.getSpiritualRootObject();
            
            if (rootType == SpiritualRootType.NONE) {
                // 凡人消息
                player.sendSystemMessage(Component.literal("§7=== 天命测定 ==="));
                player.sendSystemMessage(Component.literal("§8你是凡人之躯，无法感知灵气。"));
                player.sendSystemMessage(Component.literal("§8需寻求机缘，方可踏入修仙之路..."));
            } else {
                // 有灵根
                SpiritualRootQuality quality = root.getQuality();
                String qualityName = quality.getDisplayName();
                
                player.sendSystemMessage(Component.literal("§6=== 天命测定 ==="));
                player.sendSystemMessage(Component.literal("§f你拥有 " + rootType.getDisplayName() + "，品质：§" + getColorCode(quality) + qualityName));
                player.sendSystemMessage(Component.literal("§7修炼效率：§a×" + String.format("%.1f", quality.getCultivationBonus())));
                
                // 特殊消息
                if (quality == SpiritualRootQuality.PERFECT) {
                    player.sendSystemMessage(Component.literal("§e§l⚡ 天降祥瑞！你拥有传说中的天灵根！ ⚡"));
                } else if (quality == SpiritualRootQuality.EXCELLENT) {
                    player.sendSystemMessage(Component.literal("§d恭喜！极品灵根，千年难遇！"));
                }
            }
        }
    }
    
    /**
     * 获取品质对应的颜色代码
     */
    private static String getColorCode(SpiritualRootQuality quality) {
        switch (quality) {
            case POOR:
                return "7"; // 灰色
            case NORMAL:
                return "f"; // 白色
            case GOOD:
                return "a"; // 绿色
            case EXCELLENT:
                return "d"; // 紫色
            case PERFECT:
                return "6"; // 金色
            default:
                return "f";
        }
    }
    
    /**
     * 开始修炼
     */
    private static int practiceStart(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        // 使用打坐修炼方式
        boolean success = org.example.Kangnaixi.tiandao.cultivation.PracticeTickHandler.startPractice(player, "meditation");
        
        if (success) {
            context.getSource().sendSuccess(() -> Component.literal("§a开始打坐修炼"), false);
        }
        
        return success ? 1 : 0;
    }
    
    /**
     * 停止修炼
     */
    private static int practiceStop(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        player.getCapability(org.example.Kangnaixi.tiandao.Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
            if (cultivation.isPracticing()) {
                org.example.Kangnaixi.tiandao.cultivation.PracticeTickHandler.stopPractice(player, "manual");
                context.getSource().sendSuccess(() -> Component.literal("§7已停止修炼"), false);
            } else {
                player.sendSystemMessage(Component.literal("§c你当前没有在修炼"));
            }
        });
        
        return 1;
    }
    
    /**
     * 查看修炼状态
     */
    private static int practiceStatus(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        player.getCapability(org.example.Kangnaixi.tiandao.Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
            context.getSource().sendSuccess(() -> Component.literal("§6=== 修炼状态 ==="), false);
            
            // 修炼状态
            if (cultivation.isPracticing()) {
                String methodName = org.example.Kangnaixi.tiandao.practice.PracticeRegistry.getInstance()
                    .getPracticeMethod(cultivation.getCurrentPracticeMethod())
                    .getDisplayName();
                context.getSource().sendSuccess(() -> Component.literal("§a修炼中: " + methodName), false);
            } else {
                context.getSource().sendSuccess(() -> Component.literal("§7未在修炼"), false);
            }
            
            // 修炼经验
            int currentExp = cultivation.getCultivationExperience();
            int requiredExp = cultivation.getRequiredExperienceForLevel();
            double progress = (double) currentExp / requiredExp * 100;
            
            context.getSource().sendSuccess(() -> Component.literal(
                String.format("§f修炼经验: §e%d§f/§e%d §7(%.1f%%)", currentExp, requiredExp, progress)
            ), false);
            
            // 经验进度条
            int bars = (int) (progress / 5); // 20格进度条
            StringBuilder progressBar = new StringBuilder("§8[");
            for (int i = 0; i < 20; i++) {
                if (i < bars) {
                    progressBar.append("§a█");
                } else {
                    progressBar.append("§7█");
                }
            }
            progressBar.append("§8]");
            context.getSource().sendSuccess(() -> Component.literal(progressBar.toString()), false);
            
            // 是否可以突破
            if (currentExp >= requiredExp) {
                context.getSource().sendSuccess(() -> Component.literal("§e§l⚡ 经验已满，可尝试突破！"), false);
            }
        });
        
        return 1;
    }
    
    // === 功法管理命令 ===
    
    /**
     * 查看已学习的功法列表
     */
    private static int techniqueList(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
            context.getSource().sendSuccess(() -> Component.literal("§6=== 已学习的功法 ==="), false);
            
            java.util.List<org.example.Kangnaixi.tiandao.technique.TechniqueData> techniques = cultivation.getLearnedTechniques();
            
            if (techniques.isEmpty()) {
                context.getSource().sendSuccess(() -> Component.literal("§7你还没有学习任何功法"), false);
            } else {
                for (org.example.Kangnaixi.tiandao.technique.TechniqueData technique : techniques) {
                    boolean isEquipped = cultivation.getEquippedTechnique() != null && 
                                        cultivation.getEquippedTechnique().getId().equals(technique.getId());
                    String status = isEquipped ? " §a[已装备]" : "";
                    
                    context.getSource().sendSuccess(() -> Component.literal(String.format(
                        "§e%s §7(Lv.%d) §8[%d/%d经验]%s",
                        technique.getName(),
                        technique.getLevel(),
                        technique.getExperience(),
                        technique.getMaxExperience(),
                        status
                    )), false);
                    
                    context.getSource().sendSuccess(() -> Component.literal(
                        "§7  效率: §a" + String.format("%.1f%%", technique.getEfficiencyBonus() * 100)
                    ), false);
                }
            }
        });
        
        return 1;
    }
    
    /**
     * 学习功法
     */
    private static int techniqueLearn(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String techniqueId = StringArgumentType.getString(context, "techniqueId");
        
        // 从注册表获取功法模板
        org.example.Kangnaixi.tiandao.technique.TechniqueData techniqueTemplate = 
            org.example.Kangnaixi.tiandao.technique.TechniqueRegistry.getInstance().getTechniqueById(techniqueId);
        
        if (techniqueTemplate == null) {
            context.getSource().sendFailure(Component.literal("§c功法不存在: " + techniqueId));
            return 0;
        }
        
        player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
            // 检查是否已经学习
            if (cultivation.hasTechnique(techniqueId)) {
                context.getSource().sendFailure(Component.literal("§c你已经学习了这个功法！"));
                return;
            }
            
            // 学习功法
            if (cultivation.learnTechnique(techniqueTemplate)) {
                context.getSource().sendSuccess(() -> Component.literal(
                    "§a成功学习功法: §e" + techniqueTemplate.getName()
                ), true);
                
                // 同步到客户端
                org.example.Kangnaixi.tiandao.network.NetworkHandler.sendToPlayer(
                    new org.example.Kangnaixi.tiandao.network.CultivationDataSyncPacket(cultivation),
                    player
                );
                
                Tiandao.LOGGER.info("玩家 {} 学习了功法: {}", player.getName().getString(), techniqueTemplate.getName());
            } else {
                context.getSource().sendFailure(Component.literal("§c学习功法失败！"));
            }
        });
        
        return 1;
    }
    
    /**
     * 装备功法
     */
    private static int techniqueEquip(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String techniqueId = StringArgumentType.getString(context, "techniqueId");
        
        player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
            // 检查是否学习了该功法
            if (!cultivation.hasTechnique(techniqueId)) {
                context.getSource().sendFailure(Component.literal("§c你还没有学习这个功法！"));
                return;
            }
            
            org.example.Kangnaixi.tiandao.technique.TechniqueData technique = cultivation.getTechniqueById(techniqueId);
            
            // 装备功法
            if (cultivation.equipTechnique(techniqueId)) {
                context.getSource().sendSuccess(() -> Component.literal(
                    "§a成功装备功法: §e" + technique.getName()
                ), true);
                
                context.getSource().sendSuccess(() -> Component.literal(
                    "§7当前效率加成: §a" + String.format("%.1f%%", technique.getEfficiencyBonus() * 100)
                ), false);
                
                // 同步到客户端
                org.example.Kangnaixi.tiandao.network.NetworkHandler.sendToPlayer(
                    new org.example.Kangnaixi.tiandao.network.CultivationDataSyncPacket(cultivation),
                    player
                );
                
                Tiandao.LOGGER.info("玩家 {} 装备了功法: {}", player.getName().getString(), technique.getName());
            } else {
                context.getSource().sendFailure(Component.literal("§c装备功法失败！"));
            }
        });
        
        return 1;
    }
    
    /**
     * 卸下功法
     */
    private static int techniqueUnequip(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
            if (!cultivation.hasEquippedTechnique()) {
                context.getSource().sendFailure(Component.literal("§c你当前没有装备任何功法！"));
                return;
            }
            
            String techniqueName = cultivation.getEquippedTechnique().getName();
            
            // 卸下功法（暂时没有副作用）
            cultivation.unequipTechnique();
            
            context.getSource().sendSuccess(() -> Component.literal(
                "§7已卸下功法: §e" + techniqueName
            ), true);
            
            // 同步到客户端
            org.example.Kangnaixi.tiandao.network.NetworkHandler.sendToPlayer(
                new org.example.Kangnaixi.tiandao.network.CultivationDataSyncPacket(cultivation),
                player
            );
            
            Tiandao.LOGGER.info("玩家 {} 卸下了功法: {}", player.getName().getString(), techniqueName);
        });
        
        return 1;
    }
    
    // ==================== 术法命令 ====================
    
    /**
     * 列出已解锁的术法
     */
    private static int spellList(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
            java.util.List<String> unlockedSpells = cultivation.getUnlockedSpells();
            
            if (unlockedSpells.isEmpty()) {
                context.getSource().sendFailure(Component.literal("§c你还没有解锁任何术法！"));
                return;
            }
            
            context.getSource().sendSuccess(() -> Component.literal("§6§l【已解锁术法】"), false);
            
            for (String spellId : unlockedSpells) {
                org.example.Kangnaixi.tiandao.spell.SpellData spell = 
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
                        statusText = " §7[就绪]";
                    }
                    
                    final String spellName = spell.getName();
                    final String spellDesc = spell.getDescription().split("\n")[0];
                    final double spiritCost = spell.getSpiritCost();
                    final int cooldown = spell.getCooldown();
                    final String id = spellId;
                    
                    context.getSource().sendSuccess(() -> Component.literal(
                        String.format("§e%s §7- §f%s%s",
                            spellName,
                            spellDesc,
                            statusText)
                    ), false);
                    
                    context.getSource().sendSuccess(() -> Component.literal(
                        String.format("  §7消耗: §b%.0f灵力 §7| 冷却: §b%ds §7| ID: §8%s",
                            spiritCost,
                            cooldown,
                            id)
                    ), false);
                }
            }
            
            context.getSource().sendSuccess(() -> Component.literal("§7共 §a" + unlockedSpells.size() + " §7个术法"), false);
        });
        
        return 1;
    }
    
    /**
     * 解锁术法
     */
    private static int spellUnlock(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String spellId = StringArgumentType.getString(context, "spellId");
        
        // 检查术法是否存在
        org.example.Kangnaixi.tiandao.spell.SpellData spell = 
            org.example.Kangnaixi.tiandao.spell.SpellRegistry.getInstance().getSpellById(spellId);
        
        if (spell == null) {
            context.getSource().sendFailure(Component.literal("§c术法不存在: " + spellId));
            return 0;
        }
        
        player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
            // 检查是否已解锁
            if (cultivation.hasSpell(spellId)) {
                context.getSource().sendFailure(Component.literal("§c你已经解锁了这个术法！"));
                return;
            }
            
            // 解锁术法
            cultivation.unlockSpell(spellId);
            
            context.getSource().sendSuccess(() -> Component.literal(""), false);
            context.getSource().sendSuccess(() -> Component.literal("§6§l【习得新术法】"), false);
            context.getSource().sendSuccess(() -> Component.literal("§e" + spell.getName()), false);
            context.getSource().sendSuccess(() -> Component.literal(""), false);
            context.getSource().sendSuccess(() -> Component.literal("§7" + spell.getDescription()), false);
            context.getSource().sendSuccess(() -> Component.literal(""), false);
            context.getSource().sendSuccess(() -> Component.literal(
                String.format("§b消耗: §f%.0f灵力 §7| §b冷却: §f%ds",
                    spell.getSpiritCost(),
                    spell.getCooldown())
            ), false);
            
            // 播放解锁音效
            player.playSound(net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, 1.0f, 1.0f);
            
            // 同步到客户端
            org.example.Kangnaixi.tiandao.network.NetworkHandler.sendToPlayer(
                new org.example.Kangnaixi.tiandao.network.CultivationDataSyncPacket(cultivation),
                player
            );
            
            Tiandao.LOGGER.info("玩家 {} 解锁了术法: {}", player.getName().getString(), spell.getName());
        });
        
        return 1;
    }
    
    /**
     * 释放术法
     */
    private static int spellCast(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String spellId = StringArgumentType.getString(context, "spellId");
        
        player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
            // 检查是否已解锁
            if (!cultivation.hasSpell(spellId)) {
                context.getSource().sendFailure(Component.literal("§c你还没有解锁这个术法！"));
                return;
            }
            
            // 获取术法
            org.example.Kangnaixi.tiandao.spell.SpellData spell = 
                org.example.Kangnaixi.tiandao.spell.SpellRegistry.getInstance().getSpellById(spellId);
            
            if (spell == null) {
                context.getSource().sendFailure(Component.literal("§c术法不存在: " + spellId));
                return;
            }
            
            // 尝试释放术法
            if (spell.cast(player, cultivation)) {
                context.getSource().sendSuccess(() -> Component.literal(
                    "§a成功释放术法: §e" + spell.getName()
                ), true);
                
                // 同步到客户端
                org.example.Kangnaixi.tiandao.network.NetworkHandler.sendToPlayer(
                    new org.example.Kangnaixi.tiandao.network.CultivationDataSyncPacket(cultivation),
                    player
                );
            } else {
                // 释放失败，给出原因
                if (spell.isOnCooldown()) {
                    int remaining = spell.getCooldownRemaining();
                    context.getSource().sendFailure(Component.literal(
                        "§c术法冷却中！还需 " + remaining + " 秒"
                    ));
                } else if (cultivation.getSpiritPower() < spell.getSpiritCost()) {
                    context.getSource().sendFailure(Component.literal(
                        String.format("§c灵力不足！需要 %.0f，当前 %.0f",
                            spell.getSpiritCost(),
                            cultivation.getSpiritPower())
                    ));
                } else {
                    context.getSource().sendFailure(Component.literal("§c释放术法失败！"));
                }
            }
        });
        
        return 1;
    }
}

