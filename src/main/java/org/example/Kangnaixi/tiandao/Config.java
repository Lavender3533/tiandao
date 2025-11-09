package org.example.Kangnaixi.tiandao;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Forge's config APIs
@Mod.EventBusSubscriber(modid = Tiandao.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER
            .comment("Whether to log the dirt block on common setup")
            .define("logDirtBlock", true);

    private static final ForgeConfigSpec.IntValue MAGIC_NUMBER = BUILDER
            .comment("A magic number")
            .defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);

    public static final ForgeConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER
            .comment("What you want the introduction message to be for the magic number")
            .define("magicNumberIntroduction", "The magic number is... ");

    // a list of strings that are treated as resource locations for items
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER
            .comment("A list of items to log on common setup.")
            .defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), Config::validateItemName);

    // === 环境灵力密度配置 ===
    
    // 时间影响
    private static final ForgeConfigSpec.DoubleValue NIGHT_DENSITY_MULTIPLIER = BUILDER
            .comment("Night time spiritual density multiplier (夜晚灵气密度系数)")
            .defineInRange("spiritualDensity.nightMultiplier", 1.2, 0.1, 10.0);
    
    private static final ForgeConfigSpec.DoubleValue FULL_MOON_MULTIPLIER = BUILDER
            .comment("Full moon spiritual density multiplier (满月灵气密度系数)")
            .defineInRange("spiritualDensity.fullMoonMultiplier", 1.5, 0.1, 10.0);
    
    // 高度影响
    private static final ForgeConfigSpec.DoubleValue HIGH_ALTITUDE_MULTIPLIER = BUILDER
            .comment("High altitude (Y > 150) spiritual density multiplier (高山灵气密度系数)")
            .defineInRange("spiritualDensity.highAltitudeMultiplier", 1.3, 0.1, 10.0);
    
    private static final ForgeConfigSpec.IntValue HIGH_ALTITUDE_THRESHOLD = BUILDER
            .comment("Y coordinate threshold for high altitude bonus (高山加成的高度阈值)")
            .defineInRange("spiritualDensity.highAltitudeThreshold", 150, 64, 320);
    
    private static final ForgeConfigSpec.DoubleValue LOW_ALTITUDE_MULTIPLIER = BUILDER
            .comment("Low altitude (Y < 0) spiritual density multiplier (地下灵气密度系数)")
            .defineInRange("spiritualDensity.lowAltitudeMultiplier", 0.7, 0.1, 10.0);
    
    private static final ForgeConfigSpec.IntValue LOW_ALTITUDE_THRESHOLD = BUILDER
            .comment("Y coordinate threshold for low altitude penalty (地下减成的高度阈值)")
            .defineInRange("spiritualDensity.lowAltitudeThreshold", 0, -64, 64);
    
    // 方块加成
    private static final ForgeConfigSpec.DoubleValue SPIRIT_GATHERING_BONUS = BUILDER
            .comment("Spirit gathering block density bonus (灵气聚集方块加成)")
            .defineInRange("spiritualDensity.spiritGatheringBonus", 1.0, 0.0, 10.0);
    
    private static final ForgeConfigSpec.IntValue SPIRIT_GATHERING_RANGE = BUILDER
            .comment("Spirit gathering block effect range in blocks (灵气聚集方块作用范围)")
            .defineInRange("spiritualDensity.spiritGatheringRange", 16, 1, 64);
    
    private static final ForgeConfigSpec.DoubleValue CULTIVATION_ALTAR_BONUS = BUILDER
            .comment("Cultivation altar density bonus (修炼台加成)")
            .defineInRange("spiritualDensity.cultivationAltarBonus", 0.5, 0.0, 10.0);
    
    private static final ForgeConfigSpec.IntValue CULTIVATION_ALTAR_RANGE = BUILDER
            .comment("Cultivation altar effect range in blocks (修炼台作用范围)")
            .defineInRange("spiritualDensity.cultivationAltarRange", 8, 1, 64);
    
    private static final ForgeConfigSpec.DoubleValue MAX_BLOCK_BONUS = BUILDER
            .comment("Maximum total block bonus (最大方块加成总和)")
            .defineInRange("spiritualDensity.maxBlockBonus", 1.5, 0.0, 10.0);
    
    // 凡人HUD配置
    private static final ForgeConfigSpec.BooleanValue MORTAL_SHOW_HUD = BUILDER
            .comment("Whether mortals (players without spiritual roots) can see the HUD (凡人是否显示HUD，调试用)")
            .define("spiritualDensity.mortalShowHud", false);
    
    // === 灵根分配概率配置 ===
    
    // 灵根数量概率
    private static final ForgeConfigSpec.DoubleValue ROOT_COUNT_NONE_CHANCE = BUILDER
            .comment("Chance of getting no spiritual root (mortal) - 无灵根（凡人）概率 (%)")
            .defineInRange("spiritualRoot.rootCountNoneChance", 40.0, 0.0, 100.0);
    
    private static final ForgeConfigSpec.DoubleValue ROOT_COUNT_SINGLE_CHANCE = BUILDER
            .comment("Chance of getting single spiritual root - 单灵根概率 (%)")
            .defineInRange("spiritualRoot.rootCountSingleChance", 25.0, 0.0, 100.0);
    
    private static final ForgeConfigSpec.DoubleValue ROOT_COUNT_DOUBLE_CHANCE = BUILDER
            .comment("Chance of getting double spiritual roots - 双灵根概率 (%)")
            .defineInRange("spiritualRoot.rootCountDoubleChance", 20.0, 0.0, 100.0);
    
    private static final ForgeConfigSpec.DoubleValue ROOT_COUNT_TRIPLE_CHANCE = BUILDER
            .comment("Chance of getting triple spiritual roots - 三灵根概率 (%)")
            .defineInRange("spiritualRoot.rootCountTripleChance", 10.0, 0.0, 100.0);
    
    private static final ForgeConfigSpec.DoubleValue ROOT_COUNT_QUAD_CHANCE = BUILDER
            .comment("Chance of getting quad spiritual roots - 四灵根概率 (%)")
            .defineInRange("spiritualRoot.rootCountQuadChance", 4.0, 0.0, 100.0);
    
    private static final ForgeConfigSpec.DoubleValue ROOT_COUNT_PENTA_CHANCE = BUILDER
            .comment("Chance of getting all five spiritual roots - 五行全灵根概率 (%)")
            .defineInRange("spiritualRoot.rootCountPentaChance", 1.0, 0.0, 100.0);
    
    // 灵根品质概率
    private static final ForgeConfigSpec.DoubleValue ROOT_QUALITY_POOR_CHANCE = BUILDER
            .comment("Chance of getting poor quality spiritual root - 劣质灵根概率 (%)")
            .defineInRange("spiritualRoot.qualityPoorChance", 30.0, 0.0, 100.0);
    
    private static final ForgeConfigSpec.DoubleValue ROOT_QUALITY_NORMAL_CHANCE = BUILDER
            .comment("Chance of getting normal quality spiritual root - 普通灵根概率 (%)")
            .defineInRange("spiritualRoot.qualityNormalChance", 50.0, 0.0, 100.0);
    
    private static final ForgeConfigSpec.DoubleValue ROOT_QUALITY_GOOD_CHANCE = BUILDER
            .comment("Chance of getting good quality spiritual root - 优质灵根概率 (%)")
            .defineInRange("spiritualRoot.qualityGoodChance", 15.0, 0.0, 100.0);
    
    private static final ForgeConfigSpec.DoubleValue ROOT_QUALITY_EXCELLENT_CHANCE = BUILDER
            .comment("Chance of getting excellent quality spiritual root - 极品灵根概率 (%)")
            .defineInRange("spiritualRoot.qualityExcellentChance", 4.0, 0.0, 100.0);
    
    private static final ForgeConfigSpec.DoubleValue ROOT_QUALITY_PERFECT_CHANCE = BUILDER
            .comment("Chance of getting perfect quality spiritual root (Heavenly Root) - 天灵根概率 (%)")
            .defineInRange("spiritualRoot.qualityPerfectChance", 1.0, 0.0, 100.0);
    
    // 灵根分配特殊选项
    private static final ForgeConfigSpec.BooleanValue HARMONIOUS_PAIR_ENABLED = BUILDER
            .comment("Enable harmonious pair bonus for double roots (五行相生组合) - 启用相生组合优先")
            .define("spiritualRoot.harmoniousPairEnabled", true);
    
    private static final ForgeConfigSpec.DoubleValue HARMONIOUS_PAIR_CHANCE = BUILDER
            .comment("Chance of getting harmonious pair when rolling double roots - 双灵根时触发相生组合的概率 (%)")
            .defineInRange("spiritualRoot.harmoniousPairChance", 20.0, 0.0, 100.0);
    
    private static final ForgeConfigSpec.BooleanValue GUARANTEE_ROOT_FOR_NEW_PLAYERS = BUILDER
            .comment("Guarantee all new players get at least one spiritual root (新手友好模式：保证所有新玩家都有灵根)")
            .define("spiritualRoot.guaranteeRootForNewPlayers", false);
    
    // === 修炼系统配置 ===
    
    // 打坐修炼配置
    private static final ForgeConfigSpec.BooleanValue CROUCH_TO_MEDITATE_ENABLED = BUILDER
            .comment("Enable crouch for 3 seconds to start meditation (启用蹲伏3秒开始打坐)")
            .define("practice.crouchToMeditateEnabled", true);
    
    private static final ForgeConfigSpec.IntValue CROUCH_TO_MEDITATE_DURATION = BUILDER
            .comment("Duration in ticks required to hold crouch to start meditation (蹲伏多久开始打坐，单位tick，20tick=1秒)")
            .defineInRange("practice.crouchToMeditateDuration", 60, 20, 200);
    
    private static final ForgeConfigSpec.DoubleValue MEDITATION_SPIRIT_RECOVERY_BONUS = BUILDER
            .comment("Spirit recovery bonus multiplier during meditation (打坐灵力恢复加成倍率)")
            .defineInRange("practice.meditationSpiritRecoveryBonus", 1.0, 0.0, 10.0);
    
    private static final ForgeConfigSpec.DoubleValue MEDITATION_BASE_EXPERIENCE_RATE = BUILDER
            .comment("Base experience gain rate per second during meditation (打坐基础经验获取速率，每秒)")
            .defineInRange("practice.meditationBaseExperienceRate", 1.0, 0.0, 100.0);
    
    private static final ForgeConfigSpec.BooleanValue MEDITATION_AUTO_STOP_ON_MOVE = BUILDER
            .comment("Auto stop meditation when player moves (移动时自动停止打坐)")
            .define("practice.meditationAutoStopOnMove", true);
    
    private static final ForgeConfigSpec.BooleanValue MEDITATION_AUTO_STOP_ON_COMBAT = BUILDER
            .comment("Auto stop meditation when player takes damage (受伤时自动停止打坐)")
            .define("practice.meditationAutoStopOnCombat", true);
    
    private static final ForgeConfigSpec.IntValue COMBAT_STATE_DURATION = BUILDER
            .comment("Duration in ticks for combat state after taking damage (战斗状态持续时间，单位tick)")
            .defineInRange("practice.combatStateDuration", 100, 20, 1200);
    
    private static final ForgeConfigSpec.DoubleValue MEDITATION_PARTICLE_DENSITY = BUILDER
            .comment("Particle density for meditation visual effect (打坐粒子特效密度，0=关闭)")
            .defineInRange("practice.meditationParticleDensity", 1.0, 0.0, 5.0);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean logDirtBlock;
    public static int magicNumber;
    public static String magicNumberIntroduction;
    public static Set<Item> items;
    
    // 环境灵力密度配置公共字段
    public static double nightDensityMultiplier;
    public static double fullMoonMultiplier;
    public static double highAltitudeMultiplier;
    public static int highAltitudeThreshold;
    public static double lowAltitudeMultiplier;
    public static int lowAltitudeThreshold;
    public static double spiritGatheringBonus;
    public static int spiritGatheringRange;
    public static double cultivationAltarBonus;
    public static int cultivationAltarRange;
    public static double maxBlockBonus;
    public static boolean mortalShowHud;
    
    // 灵根分配概率配置公共字段
    public static double rootCountNoneChance;
    public static double rootCountSingleChance;
    public static double rootCountDoubleChance;
    public static double rootCountTripleChance;
    public static double rootCountQuadChance;
    public static double rootCountPentaChance;
    public static double rootQualityPoorChance;
    public static double rootQualityNormalChance;
    public static double rootQualityGoodChance;
    public static double rootQualityExcellentChance;
    public static double rootQualityPerfectChance;
    public static boolean harmoniousPairEnabled;
    public static double harmoniousPairChance;
    public static boolean guaranteeRootForNewPlayers;
    
    // 修炼系统配置公共字段
    public static boolean crouchToMeditateEnabled;
    public static int crouchToMeditateDuration;
    public static double meditationSpiritRecoveryBonus;
    public static double meditationBaseExperienceRate;
    public static boolean meditationAutoStopOnMove;
    public static boolean meditationAutoStopOnCombat;
    public static int combatStateDuration;
    public static double meditationParticleDensity;

    private static boolean validateItemName(final Object obj)
    {
        return obj instanceof final String itemName && ForgeRegistries.ITEMS.containsKey(ResourceLocation.tryParse(itemName));
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent.Loading event)
    {
        // 配置加载完成后的处理
        // 注意：在Loading事件中，配置已经加载完成，可以安全访问
        loadConfigValues();
    }

    @SubscribeEvent
    static void onReload(final ModConfigEvent.Reloading event)
    {
        // 配置重新加载后的处理
        loadConfigValues();
    }

    private static void loadConfigValues()
    {
        try {
            logDirtBlock = LOG_DIRT_BLOCK.get();
            magicNumber = MAGIC_NUMBER.get();
            magicNumberIntroduction = MAGIC_NUMBER_INTRODUCTION.get();

            // convert the list of strings into a set of items
            items = ITEM_STRINGS.get().stream()
                    .map(itemName -> ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(itemName)))
                    .collect(Collectors.toSet());
            
            // 加载环境灵力密度配置
            nightDensityMultiplier = NIGHT_DENSITY_MULTIPLIER.get();
            fullMoonMultiplier = FULL_MOON_MULTIPLIER.get();
            highAltitudeMultiplier = HIGH_ALTITUDE_MULTIPLIER.get();
            highAltitudeThreshold = HIGH_ALTITUDE_THRESHOLD.get();
            lowAltitudeMultiplier = LOW_ALTITUDE_MULTIPLIER.get();
            lowAltitudeThreshold = LOW_ALTITUDE_THRESHOLD.get();
            spiritGatheringBonus = SPIRIT_GATHERING_BONUS.get();
            spiritGatheringRange = SPIRIT_GATHERING_RANGE.get();
            cultivationAltarBonus = CULTIVATION_ALTAR_BONUS.get();
            cultivationAltarRange = CULTIVATION_ALTAR_RANGE.get();
            maxBlockBonus = MAX_BLOCK_BONUS.get();
            mortalShowHud = MORTAL_SHOW_HUD.get();
            
            // 加载灵根分配概率配置
            rootCountNoneChance = ROOT_COUNT_NONE_CHANCE.get();
            rootCountSingleChance = ROOT_COUNT_SINGLE_CHANCE.get();
            rootCountDoubleChance = ROOT_COUNT_DOUBLE_CHANCE.get();
            rootCountTripleChance = ROOT_COUNT_TRIPLE_CHANCE.get();
            rootCountQuadChance = ROOT_COUNT_QUAD_CHANCE.get();
            rootCountPentaChance = ROOT_COUNT_PENTA_CHANCE.get();
            rootQualityPoorChance = ROOT_QUALITY_POOR_CHANCE.get();
            rootQualityNormalChance = ROOT_QUALITY_NORMAL_CHANCE.get();
            rootQualityGoodChance = ROOT_QUALITY_GOOD_CHANCE.get();
            rootQualityExcellentChance = ROOT_QUALITY_EXCELLENT_CHANCE.get();
            rootQualityPerfectChance = ROOT_QUALITY_PERFECT_CHANCE.get();
            harmoniousPairEnabled = HARMONIOUS_PAIR_ENABLED.get();
            harmoniousPairChance = HARMONIOUS_PAIR_CHANCE.get();
            guaranteeRootForNewPlayers = GUARANTEE_ROOT_FOR_NEW_PLAYERS.get();
            
            // 加载修炼系统配置
            crouchToMeditateEnabled = CROUCH_TO_MEDITATE_ENABLED.get();
            crouchToMeditateDuration = CROUCH_TO_MEDITATE_DURATION.get();
            meditationSpiritRecoveryBonus = MEDITATION_SPIRIT_RECOVERY_BONUS.get();
            meditationBaseExperienceRate = MEDITATION_BASE_EXPERIENCE_RATE.get();
            meditationAutoStopOnMove = MEDITATION_AUTO_STOP_ON_MOVE.get();
            meditationAutoStopOnCombat = MEDITATION_AUTO_STOP_ON_COMBAT.get();
            combatStateDuration = COMBAT_STATE_DURATION.get();
            meditationParticleDensity = MEDITATION_PARTICLE_DENSITY.get();
        } catch (Exception e) {
            // 如果配置尚未加载，使用默认值
            logDirtBlock = true;
            magicNumber = 42;
            magicNumberIntroduction = "The magic number is... ";
            items = Collections.emptySet();
            
            // 环境灵力密度默认值
            nightDensityMultiplier = 1.2;
            fullMoonMultiplier = 1.5;
            highAltitudeMultiplier = 1.3;
            highAltitudeThreshold = 150;
            lowAltitudeMultiplier = 0.7;
            lowAltitudeThreshold = 0;
            spiritGatheringBonus = 1.0;
            spiritGatheringRange = 16;
            cultivationAltarBonus = 0.5;
            cultivationAltarRange = 8;
            maxBlockBonus = 1.5;
            mortalShowHud = false;
            
            // 灵根分配概率默认值
            rootCountNoneChance = 40.0;
            rootCountSingleChance = 25.0;
            rootCountDoubleChance = 20.0;
            rootCountTripleChance = 10.0;
            rootCountQuadChance = 4.0;
            rootCountPentaChance = 1.0;
            rootQualityPoorChance = 30.0;
            rootQualityNormalChance = 50.0;
            rootQualityGoodChance = 15.0;
            rootQualityExcellentChance = 4.0;
            rootQualityPerfectChance = 1.0;
            harmoniousPairEnabled = true;
            harmoniousPairChance = 20.0;
            guaranteeRootForNewPlayers = false;
            
            // 修炼系统默认值
            crouchToMeditateEnabled = true;
            crouchToMeditateDuration = 60;
            meditationSpiritRecoveryBonus = 1.0;
            meditationBaseExperienceRate = 1.0;
            meditationAutoStopOnMove = true;
            meditationAutoStopOnCombat = true;
            combatStateDuration = 100;
            meditationParticleDensity = 1.0;
        }
    }
}
