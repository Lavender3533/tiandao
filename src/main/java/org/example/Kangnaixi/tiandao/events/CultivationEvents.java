package org.example.Kangnaixi.tiandao.events;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.capability.CultivationCapability;
import org.example.Kangnaixi.tiandao.capability.CultivationProvider;
import org.example.Kangnaixi.tiandao.capability.ICultivation;
import org.example.Kangnaixi.tiandao.cultivation.SpiritualRootType;
import org.example.Kangnaixi.tiandao.cultivation.SpiritualRoot;
import org.example.Kangnaixi.tiandao.cultivation.FoundationSystem;
import org.example.Kangnaixi.tiandao.network.CultivationDataSyncPacket;
import org.example.Kangnaixi.tiandao.network.NetworkHandler;
import org.example.Kangnaixi.tiandao.spell.runtime.hotbar.ISpellHotbar;
import org.example.Kangnaixi.tiandao.spell.runtime.IPlayerSpells;
import org.example.Kangnaixi.tiandao.spell.runtime.PlayerSpellsProvider;
import org.example.Kangnaixi.tiandao.spell.runtime.hotbar.ISpellHotbar;
import org.example.Kangnaixi.tiandao.spell.runtime.hotbar.SpellHotbarProvider;

/**
 * 修仙系统事件处理器
 *
 */
@Mod.EventBusSubscriber(modid = Tiandao.MODID)
public class CultivationEvents {
    
    /**
     * 为玩家附加修仙能力
     */
    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            CultivationProvider provider = new CultivationProvider();
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Tiandao.MODID, "cultivation");
            event.addCapability(id, provider);
            Tiandao.LOGGER.debug("为玩家附加修仙 Capability");

            PlayerSpellsProvider spellsProvider = new PlayerSpellsProvider();
            ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(Tiandao.MODID, "player_spells");
            event.addCapability(spellId, spellsProvider);

            SpellHotbarProvider hotbarProvider = new SpellHotbarProvider();
            ResourceLocation hotbarId = ResourceLocation.fromNamespaceAndPath(Tiandao.MODID, "spell_hotbar");
            event.addCapability(hotbarId, hotbarProvider);
            Tiandao.LOGGER.debug("为玩家附加术法快捷栏 Capability");

            org.example.Kangnaixi.tiandao.capability.StarChartProvider starChartProvider = new org.example.Kangnaixi.tiandao.capability.StarChartProvider();
            ResourceLocation starChartId = ResourceLocation.fromNamespaceAndPath(Tiandao.MODID, "star_chart");
            event.addCapability(starChartId, starChartProvider);
            Tiandao.LOGGER.debug("为玩家附加星盘 Capability");
        }
    }

    /**
     * 玩家克隆时复制修仙数据
     */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        // 无论是否死亡，都需要复制修仙数据
        Player original = event.getOriginal();
        Player player = event.getEntity();
        
        // 确保原始玩家的数据被持久化
        original.reviveCaps();
        
        LazyOptional<ICultivation> originalCap = original.getCapability(Tiandao.CULTIVATION_CAPABILITY);
        LazyOptional<ICultivation> newCap = player.getCapability(Tiandao.CULTIVATION_CAPABILITY);

        originalCap.ifPresent(originalCultivation -> {
            newCap.ifPresent(newCultivation -> {
                newCultivation.copyFrom(originalCultivation);

                if (originalCultivation instanceof CultivationCapability origCap) {
                    SpiritualRoot root = origCap.getSpiritualRootObject();
                    Tiandao.LOGGER.debug("玩家克隆 - 原始灵根: {} ({})",
                        root != null ? root.getType().getDisplayName() : "null",
                        root != null && root.getQuality() != null ? root.getQuality().getDisplayName() : "null");
                }

                if (newCultivation instanceof CultivationCapability newCapImpl) {
                    SpiritualRoot root = newCapImpl.getSpiritualRootObject();
                    Tiandao.LOGGER.debug("玩家克隆 - 新灵根: {} ({})",
                        root != null ? root.getType().getDisplayName() : "null",
                        root != null && root.getQuality() != null ? root.getQuality().getDisplayName() : "null");
                }

                if (player instanceof ServerPlayer serverPlayer) {
                    NetworkHandler.sendToPlayer(new CultivationDataSyncPacket(newCultivation), serverPlayer);
                    Tiandao.LOGGER.debug("玩家克隆时同步修仙数据到客户端");
                }
            });
        });

        LazyOptional<IPlayerSpells> originalSpells = original.getCapability(Tiandao.PLAYER_SPELLS_CAP);
        LazyOptional<IPlayerSpells> newSpells = player.getCapability(Tiandao.PLAYER_SPELLS_CAP);
        originalSpells.ifPresent(oldCap -> newSpells.ifPresent(copy -> copy.copyFrom(oldCap)));

        LazyOptional<ISpellHotbar> originalHotbar = original.getCapability(Tiandao.SPELL_HOTBAR_CAP);
        LazyOptional<ISpellHotbar> newHotbar = player.getCapability(Tiandao.SPELL_HOTBAR_CAP);
        originalHotbar.ifPresent(oldCap -> newHotbar.ifPresent(copy -> copy.copyFrom(oldCap)));

        LazyOptional<org.example.Kangnaixi.tiandao.capability.IStarChartData> originalStarChart = original.getCapability(Tiandao.STAR_CHART_CAP);
        LazyOptional<org.example.Kangnaixi.tiandao.capability.IStarChartData> newStarChart = player.getCapability(Tiandao.STAR_CHART_CAP);
        originalStarChart.ifPresent(oldData -> {
            newStarChart.ifPresent(newData -> {
                if (oldData instanceof org.example.Kangnaixi.tiandao.capability.StarChartCapability oldStarCap &&
                    newData instanceof org.example.Kangnaixi.tiandao.capability.StarChartCapability newStarCap) {
                    newStarCap.copyFrom(oldStarCap);
                }
            });
        });

        // 清理原始玩家的capability
        original.invalidateCaps();
    }
    
    /**
     * 玩家登录时初始化修仙能力
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        
        player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
            // NBT数据是唯一真实来源，只在真正的新玩家时才初始化
            SpiritualRootType rootType = cultivation.getSpiritualRoot();
            double currentSpirit = cultivation.getSpiritPower();
            double maxSpirit = cultivation.getMaxSpiritPower();
            
            Tiandao.LOGGER.info("登录事件检查 - 玩家: {}, 灵根: {}, 灵力: {}/{}, 已分配: {}, 实例={}", 
                player.getName().getString(), rootType, currentSpirit, maxSpirit, 
                cultivation.hasRootAssigned(), System.identityHashCode(cultivation));
            
            // 检查是否已分配灵根
            if (!cultivation.hasRootAssigned()) {
                // 首次加入，随机分配灵根
                org.example.Kangnaixi.tiandao.cultivation.SpiritualRootAssigner.assignRandomRoot(cultivation, player);
                cultivation.setRootAssigned(true);
                
                // 首次获得灵根时初始化根基值为100
                if (cultivation.getFoundation() == 0) {
                    cultivation.setFoundation(100);
                    Tiandao.LOGGER.info("为新玩家 {} 初始化根基值: 100", player.getName().getString());
                }
                
                // 显示分配结果
                if (player instanceof ServerPlayer) {
                    sendRootAssignmentMessage((ServerPlayer) player, cultivation);
                }
                
                Tiandao.LOGGER.info("为新玩家 {} 分配灵根: {} ({})", 
                    player.getName().getString(), 
                    cultivation.getSpiritualRoot().getDisplayName(),
                    ((CultivationCapability) cultivation).getSpiritualRootObject().getQuality().getDisplayName());
            } else {
                // 老玩家，所有数据（包括灵力）都已从NBT加载
                Tiandao.LOGGER.info("玩家 {} 已有灵根，从NBT加载数据完成，灵力: {}/{}, 实例={}", 
                        player.getName().getString(), 
                        cultivation.getSpiritPower(), 
                        cultivation.getMaxSpiritPower(),
                        System.identityHashCode(cultivation));
            }
            
            // 立即同步到客户端
            if (player instanceof ServerPlayer) {
                ServerPlayer serverPlayer = (ServerPlayer) player;
                NetworkHandler.sendToPlayer(new CultivationDataSyncPacket(cultivation), serverPlayer);
                Tiandao.LOGGER.debug("玩家登录时立即同步修仙数据到客户端");

                // 延迟1秒后再次同步，确保客户端完全准备好
                scheduleDelayedSync(serverPlayer, cultivation, 20); // 20 ticks = 1秒

                player.getCapability(Tiandao.SPELL_HOTBAR_CAP).ifPresent(hotbar ->
                    NetworkHandler.sendSpellHotbarSyncToPlayer(hotbar, serverPlayer));

                // 同步星盘数据
                player.getCapability(Tiandao.STAR_CHART_CAP).ifPresent(starChart -> {
                    if (starChart instanceof org.example.Kangnaixi.tiandao.capability.StarChartCapability data) {
                        NetworkHandler.sendToPlayer(new org.example.Kangnaixi.tiandao.network.packet.S2CSyncStarChartPacket(data), serverPlayer);
                        Tiandao.LOGGER.debug("玩家登录时同步星盘数据到客户端");
                    }
                });
            }
        });
    }
    
    /**
     * 延迟同步数据到客户端
     */
    private static void scheduleDelayedSync(ServerPlayer player, ICultivation cultivation, int delayTicks) {
        player.getServer().tell(new net.minecraft.server.TickTask(
            player.getServer().getTickCount() + delayTicks,
            () -> {
                // 再次同步数据
                if (player.isAlive() && !player.isRemoved()) {
                    NetworkHandler.sendToPlayer(new CultivationDataSyncPacket(cultivation), player);
                    Tiandao.LOGGER.info("延迟同步修仙数据到客户端 [{}]", player.getName().getString());
                }
            }
        ));
    }
    
    /**
     * 玩家重生时同步修仙数据
     * 这个事件在玩家死亡重生或从末地返回时触发
     */
    @SubscribeEvent
    public static void onPlayerRespawn(net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        
        // 只在服务器端处理
        if (!player.level().isClientSide && player instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer) player;
            
            // 延迟几tick再同步，确保客户端准备好
            serverPlayer.getServer().execute(() -> {
                serverPlayer.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
                    // 打印debug信息
                    if (cultivation instanceof CultivationCapability) {
                        CultivationCapability cap = (CultivationCapability) cultivation;
                        SpiritualRoot root = cap.getSpiritualRootObject();
                        Tiandao.LOGGER.debug("玩家重生 - 当前灵根: {} ({}), 灵力: {}/{}",
                            root != null ? root.getType().getDisplayName() : "null",
                            root != null && root.getQuality() != null ? root.getQuality().getDisplayName() : "null",
                            cultivation.getSpiritPower(),
                            cultivation.getMaxSpiritPower());
                    }
                    
                    // 同步到客户端
                    NetworkHandler.sendToPlayer(new CultivationDataSyncPacket(cultivation), serverPlayer);
                    Tiandao.LOGGER.debug("玩家重生时同步修仙数据到客户端");
                });
            });
        }
    }
    
    /**
     * 玩家每tick更新修仙状态
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide) {
            Player player = event.player;
            
            player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
                // 计算时间加速对恢复速度的影响
                double timeAcceleration = 1.0;
                if (cultivation.isPracticing()) {
                    timeAcceleration = cultivation.getTimeAcceleration();
                }
                
                // 计算打坐恢复加成
                double meditationBonus = 1.0;
                if (cultivation.isPracticing()) {
                    // 获取打坐方式的恢复加成
                    String methodId = cultivation.getCurrentPracticeMethod();
                    if (methodId != null && !methodId.isEmpty()) {
                        org.example.Kangnaixi.tiandao.practice.PracticeMethod method = 
                            org.example.Kangnaixi.tiandao.practice.PracticeRegistry.getInstance().getPracticeMethod(methodId);
                        if (method != null && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                            meditationBonus = method.getSpiritRecoveryBonus(serverPlayer, cultivation);
                        }
                    }
                }
                
                // 灵力自然恢复（受时间加速和打坐加成影响）
                if (cultivation.getSpiritPower() < cultivation.getMaxSpiritPower()) {
                    // 使用基于灵力强度的恢复机制
                    if (cultivation instanceof CultivationCapability) {
                        CultivationCapability cap = (CultivationCapability) cultivation;
                        double intensityBonus = cap.getIntensityBasedRecoveryBonus(player);
                        
                        if (intensityBonus > 0) {
                            // 基础恢复速率 * 强度加成 * 灵根和境界加成 * 打坐加成 * 时间加速
                            double recoveryRate = 0.1 * intensityBonus * cultivation.getSpiritPowerRecoveryRate() * meditationBonus * timeAcceleration;
                            cultivation.addSpiritPower(recoveryRate);
                        }
                    } else {
                        // 兼容性处理
                        double recoveryRate = cultivation.getSpiritPowerRecoveryRate() * 0.1 * meditationBonus * timeAcceleration; // 每0.1秒恢复一次
                        cultivation.addSpiritPower(recoveryRate);
                    }
                }
                
                // 根基恢复（每游戏日检查一次，受时间加速影响）
                if (timeAcceleration > 1.0) {
                    // 时间加速时，根基恢复速度也加快
                    updateFoundationRecoveryAccelerated(player, cultivation, timeAcceleration);
                } else {
                    updateFoundationRecovery(player, cultivation);
                }
            });
        }
    }
    
    /**
     * 更新根基恢复
     */
    private static void updateFoundationRecovery(Player player, ICultivation cultivation) {
        // 每游戏日（24000 ticks）检查一次根基恢复
        if (player.tickCount % 24000 == 0) {
            int currentFoundation = cultivation.getFoundation();
            if (currentFoundation < 100) {
                // 基础恢复：每游戏日1点
                int recoveryAmount = 1;
                
                // 如果正在打坐，额外恢复0.5点
                if (cultivation.isPracticing()) {
                    recoveryAmount += 1; // 简化处理，直接加1点（相当于0.5点但整数化）
                }
                
                // 根基值达到80以上时恢复速度减半
                if (currentFoundation >= 80) {
                    recoveryAmount = recoveryAmount / 2;
                    if (recoveryAmount == 0) {
                        recoveryAmount = 1; // 至少恢复1点
                    }
                }
                
                // 仅在非战斗状态下恢复
                if (!cultivation.isInCombat()) {
                    cultivation.addFoundation(recoveryAmount);
                    
                    if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                        int newFoundation = cultivation.getFoundation();
                        if (newFoundation > currentFoundation) {
                            serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                String.format("§a根基恢复！根基值: %d → %d (+%d)", 
                                    currentFoundation, newFoundation, recoveryAmount)
                            ), false);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 更新根基恢复（时间加速版本）
     */
    private static void updateFoundationRecoveryAccelerated(Player player, ICultivation cultivation, double timeAcceleration) {
        // 时间加速时，检查间隔缩短（24000 / timeAcceleration ticks）
        int checkInterval = (int) (24000 / timeAcceleration);
        if (checkInterval < 20) {
            checkInterval = 20; // 最小间隔20tick（1秒）
        }
        
        if (player.tickCount % checkInterval == 0) {
            int currentFoundation = cultivation.getFoundation();
            if (currentFoundation < 100) {
                // 基础恢复：按时间加速比例恢复
                // 时间加速3倍时，恢复速度也快3倍
                double recoveryAmount = 1.0 / (24000.0 / checkInterval);
                
                // 如果正在打坐，额外恢复
                if (cultivation.isPracticing()) {
                    recoveryAmount *= 1.5; // 打坐时额外50%恢复速度
                }
                
                // 根基值达到80以上时恢复速度减半
                if (currentFoundation >= 80) {
                    recoveryAmount *= 0.5;
                }
                
                // 仅在非战斗状态下恢复
                if (!cultivation.isInCombat() && recoveryAmount >= 1.0) {
                    int recovery = (int) recoveryAmount;
                    cultivation.addFoundation(recovery);
                    
                    if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer && recovery > 0) {
                        int newFoundation = cultivation.getFoundation();
                        if (newFoundation > currentFoundation) {
                            serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                String.format("§a根基恢复（时间加速）！根基值: %d → %d (+%d)", 
                                    currentFoundation, newFoundation, recovery)
                            ), false);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 玩家退出时保存数据
     */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer) {
            player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
                Tiandao.LOGGER.info("玩家 {} 退出，灵力: {}/{}", 
                    player.getName().getString(),
                    cultivation.getSpiritPower(),
                    cultivation.getMaxSpiritPower());
            });
        }
    }
    
    private static final double DEATH_EXPERIENCE_LOSS_RATIO = 0.25D;

    /**
     * 玩家死亡事件 - 降低根基值
     */
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            FoundationSystem.onPlayerDeath(player);

            player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
                boolean changed = false;

                double previousSpiritPower = cultivation.getSpiritPower();
                if (previousSpiritPower > 0) {
                    cultivation.setSpiritPower(0);
                    player.sendSystemMessage(Component.literal("§c你的灵力在死亡中尽数散去。"), false);
                    changed = true;
                }

                int currentExp = cultivation.getCultivationExperience();
                if (currentExp > 0) {
                    int expLoss = Math.max(1, (int)Math.floor(currentExp * DEATH_EXPERIENCE_LOSS_RATIO));
                    int newExp = Math.max(0, currentExp - expLoss);
                    cultivation.setCultivationExperience(newExp);
                    player.sendSystemMessage(Component.literal(
                        String.format("§c修炼受阻！修炼经验: %d → %d (-%d)", currentExp, newExp, expLoss)
                    ), false);
                    changed = true;
                }

                if (changed) {
                    NetworkHandler.sendToPlayer(new CultivationDataSyncPacket(cultivation), player);
                }
            });
        }
    }
    
    /**
     * 玩家受伤事件 - 打断修炼并记录战斗时间，检测重伤
     */
    @SubscribeEvent
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
                // 记录受伤时间
                cultivation.setLastCombatTime(System.currentTimeMillis());
                
                // 检测重伤（生命值低于10%）
                float healthAfterDamage = player.getHealth() - event.getAmount();
                float maxHealth = player.getMaxHealth();
                if (healthAfterDamage < maxHealth * 0.1f) {
                    FoundationSystem.onPlayerCriticalHealth(player);
                }
                
                // 如果正在修炼，打断修炼
                if (cultivation.isPracticing()) {
                    org.example.Kangnaixi.tiandao.cultivation.PracticeTickHandler.stopPractice(player, "hurt");
                }
            });
        }
    }
    
    /**
     * 发送灵根分配消息
     */
    private static void sendRootAssignmentMessage(ServerPlayer player, ICultivation cultivation) {
        SpiritualRootType rootType = cultivation.getSpiritualRoot();
        
        if (cultivation instanceof CultivationCapability) {
            CultivationCapability cap = (CultivationCapability) cultivation;
            org.example.Kangnaixi.tiandao.cultivation.SpiritualRoot root = cap.getSpiritualRootObject();
            
            if (rootType == SpiritualRootType.NONE) {
                // 凡人消息
                player.sendSystemMessage(Component.literal("§7=== 天命测定 ==="));
                player.sendSystemMessage(Component.literal("§8你是凡人之躯，无法感知灵气。"));
                player.sendSystemMessage(Component.literal("§8需寻求机缘，方可踏入修仙之路..."));
            } else {
                // 有灵根
                org.example.Kangnaixi.tiandao.cultivation.SpiritualRootQuality quality = root.getQuality();
                String qualityName = quality.getDisplayName();
                int qualityColor = quality.getColor();
                String colorCode = String.format("§x§%x§%x§%x§%x§%x§%x",
                    (qualityColor >> 20) & 0xF,
                    (qualityColor >> 16) & 0xF,
                    (qualityColor >> 12) & 0xF,
                    (qualityColor >> 8) & 0xF,
                    (qualityColor >> 4) & 0xF,
                    qualityColor & 0xF
                );
                
                player.sendSystemMessage(Component.literal("§6=== 天命测定 ==="));
                player.sendSystemMessage(Component.literal("§f你拥有 " + rootType.getDisplayName() + "，品质：" + colorCode + qualityName));
                player.sendSystemMessage(Component.literal("§7修炼效率：§a×" + String.format("%.1f", quality.getCultivationBonus())));
                
                // 特殊消息
                if (quality == org.example.Kangnaixi.tiandao.cultivation.SpiritualRootQuality.PERFECT) {
                    player.sendSystemMessage(Component.literal("§e§l⚡ 天降祥瑞！你拥有传说中的天灵根！ ⚡"));
                } else if (quality == org.example.Kangnaixi.tiandao.cultivation.SpiritualRootQuality.EXCELLENT) {
                    player.sendSystemMessage(Component.literal("§d恭喜！极品灵根，千年难遇！"));
                }
            }
        }
    }
}
