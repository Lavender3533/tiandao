package org.example.Kangnaixi.tiandao.cultivation;

import net.minecraft.world.entity.player.Player;
import org.example.Kangnaixi.tiandao.capability.ICultivation;

import java.util.*;

/**
 * 灵根分配器
 * 负责为玩家随机分配灵根，支持多灵根组合
 */
public class SpiritualRootAssigner {
    
    /**
     * 为玩家随机分配灵根
     * 概率分布：
     * - 40% 无灵根（凡人）
     * - 25% 单灵根
     * - 20% 双灵根
     * - 10% 三灵根
     * - 4% 四灵根
     * - 1% 五行全灵根
     * 
     * @param cultivation 玩家的修仙能力对象
     * @param player 玩家对象（可选，用于基于UUID的种子）
     */
    public static void assignRandomRoot(ICultivation cultivation, Player player) {
        Random random;
        if (player != null) {
            // 基于玩家UUID创建随机数生成器，确保每个玩家的结果可重现
            random = new Random(player.getUUID().getMostSignificantBits());
        } else {
            random = new Random();
        }
        
        // 第一步：确定灵根数量
        int rootCount = determineRootCount(random);
        
        // 第二步：选择具体的灵根类型组合
        List<SpiritualRootType> rootTypes = selectRootTypes(rootCount, random);
        
        // 第三步：确定灵根品质
        SpiritualRootQuality quality = selectQuality(random);
        
        // 第四步：应用到玩家
        if (rootTypes.isEmpty() || (rootTypes.size() == 1 && rootTypes.get(0) == SpiritualRootType.NONE)) {
            // 凡人（无灵根）
            cultivation.setSpiritualRoot(SpiritualRootType.NONE);
            cultivation.setSpiritualRootObject(new SpiritualRoot(SpiritualRootType.NONE, quality));
        } else if (rootTypes.size() == 1) {
            // 单灵根
            cultivation.setSpiritualRoot(rootTypes.get(0));
            cultivation.setSpiritualRootObject(new SpiritualRoot(rootTypes.get(0), quality));
        } else {
            // 多灵根 - 暂时选择第一个作为主灵根（未来需要扩展支持多灵根）
            // TODO: 需要扩展SpiritualRoot类支持多灵根列表
            cultivation.setSpiritualRoot(rootTypes.get(0));
            cultivation.setSpiritualRootObject(new SpiritualRoot(rootTypes.get(0), quality));
        }
    }
    
    /**
     * 确定灵根数量
     * @param random 随机数生成器
     * @return 灵根数量（0-5）
     */
    private static int determineRootCount(Random random) {
        // 新手友好模式：保证所有新玩家都有灵根
        if (org.example.Kangnaixi.tiandao.Config.guaranteeRootForNewPlayers) {
            double roll = random.nextDouble() * 100;
            double cumulative = 0;
            
            // 跳过无灵根，重新分配概率
            double totalWithRoot = org.example.Kangnaixi.tiandao.Config.rootCountSingleChance +
                                  org.example.Kangnaixi.tiandao.Config.rootCountDoubleChance +
                                  org.example.Kangnaixi.tiandao.Config.rootCountTripleChance +
                                  org.example.Kangnaixi.tiandao.Config.rootCountQuadChance +
                                  org.example.Kangnaixi.tiandao.Config.rootCountPentaChance;
            
            cumulative += (org.example.Kangnaixi.tiandao.Config.rootCountSingleChance / totalWithRoot) * 100;
            if (roll < cumulative) return 1;
            
            cumulative += (org.example.Kangnaixi.tiandao.Config.rootCountDoubleChance / totalWithRoot) * 100;
            if (roll < cumulative) return 2;
            
            cumulative += (org.example.Kangnaixi.tiandao.Config.rootCountTripleChance / totalWithRoot) * 100;
            if (roll < cumulative) return 3;
            
            cumulative += (org.example.Kangnaixi.tiandao.Config.rootCountQuadChance / totalWithRoot) * 100;
            if (roll < cumulative) return 4;
            
            return 5;
        }
        
        // 正常模式：使用配置的概率
        double roll = random.nextDouble() * 100; // 0-100
        double cumulative = 0;
        
        cumulative += org.example.Kangnaixi.tiandao.Config.rootCountNoneChance;
        if (roll < cumulative) return 0;
        
        cumulative += org.example.Kangnaixi.tiandao.Config.rootCountSingleChance;
        if (roll < cumulative) return 1;
        
        cumulative += org.example.Kangnaixi.tiandao.Config.rootCountDoubleChance;
        if (roll < cumulative) return 2;
        
        cumulative += org.example.Kangnaixi.tiandao.Config.rootCountTripleChance;
        if (roll < cumulative) return 3;
        
        cumulative += org.example.Kangnaixi.tiandao.Config.rootCountQuadChance;
        if (roll < cumulative) return 4;
        
        return 5; // 剩余概率为五行全灵根
    }
    
    /**
     * 选择具体的灵根类型
     * @param count 灵根数量
     * @param random 随机数生成器
     * @return 灵根类型列表
     */
    private static List<SpiritualRootType> selectRootTypes(int count, Random random) {
        if (count == 0) {
            return Collections.singletonList(SpiritualRootType.NONE);
        }
        
        // 可选的五行灵根
        List<SpiritualRootType> allRoots = new ArrayList<>(Arrays.asList(
            SpiritualRootType.GOLD,
            SpiritualRootType.WOOD,
            SpiritualRootType.WATER,
            SpiritualRootType.FIRE,
            SpiritualRootType.EARTH
        ));
        
        if (count >= 5) {
            // 五行全灵根
            return new ArrayList<>(allRoots);
        }
        
        // 随机选择指定数量的灵根
        Collections.shuffle(allRoots, random);
        List<SpiritualRootType> selected = new ArrayList<>();
        
        // 相生组合优先逻辑（根据配置）
        if (count == 2 && 
            org.example.Kangnaixi.tiandao.Config.harmoniousPairEnabled && 
            random.nextDouble() * 100 < org.example.Kangnaixi.tiandao.Config.harmoniousPairChance) {
            selected = selectHarmoniousPair(random);
        } else {
            // 普通随机选择
            for (int i = 0; i < count && i < allRoots.size(); i++) {
                selected.add(allRoots.get(i));
            }
        }
        
        return selected;
    }
    
    /**
     * 选择相生组合的双灵根
     * 五行相生：木→火→土→金→水→木
     */
    private static List<SpiritualRootType> selectHarmoniousPair(Random random) {
        List<List<SpiritualRootType>> harmoniousPairs = Arrays.asList(
            Arrays.asList(SpiritualRootType.WOOD, SpiritualRootType.FIRE),  // 木生火
            Arrays.asList(SpiritualRootType.FIRE, SpiritualRootType.EARTH), // 火生土
            Arrays.asList(SpiritualRootType.EARTH, SpiritualRootType.GOLD), // 土生金
            Arrays.asList(SpiritualRootType.GOLD, SpiritualRootType.WATER), // 金生水
            Arrays.asList(SpiritualRootType.WATER, SpiritualRootType.WOOD)  // 水生木
        );
        
        return new ArrayList<>(harmoniousPairs.get(random.nextInt(harmoniousPairs.size())));
    }
    
    /**
     * 选择灵根品质（根据配置文件）
     * @param random 随机数生成器
     * @return 灵根品质
     */
    private static SpiritualRootQuality selectQuality(Random random) {
        double roll = random.nextDouble() * 100; // 0-100
        double cumulative = 0;
        
        cumulative += org.example.Kangnaixi.tiandao.Config.rootQualityPoorChance;
        if (roll < cumulative) return SpiritualRootQuality.POOR;
        
        cumulative += org.example.Kangnaixi.tiandao.Config.rootQualityNormalChance;
        if (roll < cumulative) return SpiritualRootQuality.NORMAL;
        
        cumulative += org.example.Kangnaixi.tiandao.Config.rootQualityGoodChance;
        if (roll < cumulative) return SpiritualRootQuality.GOOD;
        
        cumulative += org.example.Kangnaixi.tiandao.Config.rootQualityExcellentChance;
        if (roll < cumulative) return SpiritualRootQuality.EXCELLENT;
        
        return SpiritualRootQuality.PERFECT; // 剩余概率为天灵根
    }
    
    /**
     * 为玩家分配指定的灵根（管理员命令专用）
     * @param cultivation 玩家的修仙能力对象
     * @param rootType 指定的灵根类型
     * @param quality 指定的灵根品质
     */
    public static void assignSpecificRoot(ICultivation cultivation, SpiritualRootType rootType, SpiritualRootQuality quality) {
        cultivation.setSpiritualRoot(rootType);
        cultivation.setSpiritualRootObject(new SpiritualRoot(rootType, quality));
    }
    
    /**
     * 清除玩家的灵根（用于重置）
     * @param cultivation 玩家的修仙能力对象
     */
    public static void clearRoot(ICultivation cultivation) {
        cultivation.setSpiritualRoot(SpiritualRootType.NONE);
        cultivation.setSpiritualRootObject(new SpiritualRoot(SpiritualRootType.NONE, SpiritualRootQuality.NORMAL));
    }
}

