package org.example.Kangnaixi.tiandao.cultivation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.Kangnaixi.tiandao.Tiandao;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the meditation time-acceleration logic.
 */
@Mod.EventBusSubscriber(modid = Tiandao.MODID)
public class TimeAccelerationHandler {

    private static final int UPDATE_RADIUS = 8;
    private static final int UPDATE_INTERVAL = 5;
    private static final double MULTIPLAYER_BONUS_PER_PLAYER = 0.15D;
    private static final double MAX_MULTIPLAYER_BONUS = 1.0D;
    private static final double MAX_GLOBAL_ACCELERATION = 4.0D;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (event.getServer().getTickCount() % UPDATE_INTERVAL != 0) {
            return;
        }

        MinecraftServer server = event.getServer();
        List<ServerPlayer> allPlayers = server.getPlayerList().getPlayers();
        if (allPlayers.isEmpty()) {
            return;
        }

        List<MeditatingPlayer> meditatingPlayers = new ArrayList<>();
        for (ServerPlayer player : allPlayers) {
            player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
                if (cultivation.isPracticing()) {
                    double acceleration = Math.max(1.0D, cultivation.getTimeAcceleration());
                    meditatingPlayers.add(new MeditatingPlayer(player, acceleration));
                }
            });
        }

        if (meditatingPlayers.isEmpty()) {
            return;
        }

        boolean singlePlayer = allPlayers.size() == 1;
        boolean everyoneMeditating = meditatingPlayers.size() == allPlayers.size();

        if (singlePlayer || everyoneMeditating) {
            double globalAcceleration = determineGlobalAcceleration(meditatingPlayers);
            if (globalAcceleration > 1.0D) {
                applyGlobalTimeAcceleration(server, globalAcceleration);
            }
            return;
        }

        for (MeditatingPlayer entry : meditatingPlayers) {
            if (entry.acceleration() > 1.0D) {
                applyTimeAcceleration(entry.player(), entry.acceleration());
            }
        }
    }

    private static double determineGlobalAcceleration(List<MeditatingPlayer> meditatingPlayers) {
        double baseAcceleration = meditatingPlayers.stream()
            .mapToDouble(MeditatingPlayer::acceleration)
            .max()
            .orElse(1.0D);

        int count = meditatingPlayers.size();
        double bonus = 0.0D;
        if (count > 1) {
            bonus = Math.min(MAX_MULTIPLAYER_BONUS, (count - 1) * MULTIPLAYER_BONUS_PER_PLAYER);
        }

        double combined = baseAcceleration + bonus;
        return Math.min(MAX_GLOBAL_ACCELERATION, combined);
    }

    private static void applyGlobalTimeAcceleration(MinecraftServer server, double acceleration) {
        int extraWholeTicks = (int)Math.floor(acceleration - 1.0D);
        double fractional = Math.max(0.0D, acceleration - 1.0D - extraWholeTicks);

        for (int i = 0; i < extraWholeTicks; i++) {
            tickAllLevelsOnce(server);
        }

        if (fractional > 0 && getServerRandom(server) < fractional) {
            tickAllLevelsOnce(server);
        }
    }

    private static void tickAllLevelsOnce(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            level.tick(() -> true);
        }
    }

    private static double getServerRandom(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        if (overworld != null) {
            return overworld.random.nextDouble();
        }
        return Math.random();
    }

    private static void applyTimeAcceleration(ServerPlayer player, double acceleration) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockPos playerPos = player.blockPosition();
        int extraTicks = (int) Math.floor(acceleration - 1.0D);

        for (int i = 0; i < extraTicks; i++) {
            accelerateCropGrowth(serverLevel, playerPos);
            accelerateEntityAI(serverLevel, playerPos);
        }

        double fractionalPart = acceleration - 1.0D - extraTicks;
        if (Math.random() < fractionalPart) {
            accelerateCropGrowth(serverLevel, playerPos);
            accelerateEntityAI(serverLevel, playerPos);
        }
    }

    private static void accelerateCropGrowth(ServerLevel level, BlockPos centerPos) {
        int blocksToTick = 10;

        for (int i = 0; i < blocksToTick; i++) {
            int offsetX = level.random.nextInt(UPDATE_RADIUS * 2 + 1) - UPDATE_RADIUS;
            int offsetY = level.random.nextInt(5) - 2;
            int offsetZ = level.random.nextInt(UPDATE_RADIUS * 2 + 1) - UPDATE_RADIUS;

            BlockPos pos = centerPos.offset(offsetX, offsetY, offsetZ);
            BlockState state = level.getBlockState(pos);
            Block block = state.getBlock();

            if (block instanceof BonemealableBlock bonemealable) {
                if (level.random.nextInt(10) == 0) {
                    try {
                        if (bonemealable.isValidBonemealTarget(level, pos, state, false)) {
                            if (level.random.nextInt(5) == 0) {
                                bonemealable.performBonemeal(level, level.random, pos, state);
                            }
                        }
                    } catch (Exception e) {
                        Tiandao.LOGGER.debug("Unable to accelerate block {}", block.getName().getString());
                    }
                }
            }
        }
    }

    private static void accelerateEntityAI(ServerLevel level, BlockPos centerPos) {
        net.minecraft.world.phys.AABB aabb = new net.minecraft.world.phys.AABB(centerPos).inflate(UPDATE_RADIUS);
        List<net.minecraft.world.entity.LivingEntity> livingEntities = level.getEntitiesOfClass(
            net.minecraft.world.entity.LivingEntity.class,
            aabb,
            entity -> !(entity instanceof ServerPlayer) && entity.isAlive() && !entity.isRemoved()
        );

        for (net.minecraft.world.entity.LivingEntity entity : livingEntities) {
            if (entity.isAlive() && !entity.isRemoved()) {
                // Reserved for future global effects (healing, breeding, etc.).
            }
        }
    }

    private record MeditatingPlayer(ServerPlayer player, double acceleration) {}
}
