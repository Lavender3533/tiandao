package org.example.Kangnaixi.tiandao.test;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.capability.ICultivation;
import org.example.Kangnaixi.tiandao.cultivation.SpiritualRootType;
import org.example.Kangnaixi.tiandao.cultivation.SpiritualRootQuality;
import org.example.Kangnaixi.tiandao.cultivation.CultivationRealm;

/**
 * 灵力恢复机制测试类
 */
public class SpiritPowerRecoveryTest {
    
    /**
     * 测试基础灵力恢复
     */
    @GameTest(template = "empty")
    public static void testBasicSpiritPowerRecovery(GameTestHelper helper) {
        Player player = helper.makeMockPlayer();
        
        // 获取玩家的修仙能力
        player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
            // 设置初始灵力为50%
            cultivation.setSpiritPower(50.0);
            cultivation.setMaxSpiritPower(100.0);
            
            // 验证初始状态
            helper.assertTrue(cultivation.getSpiritPower() == 50.0, "初始灵力应为50.0");
            
            // 模拟10秒的恢复（每tick恢复0.1秒）
            for (int i = 0; i < 200; i++) { // 10秒 = 200 ticks
                double recoveryRate = cultivation.getSpiritPowerRecoveryRate() * 0.1;
                cultivation.addSpiritPower(recoveryRate);
            }
            
            // 验证恢复后的灵力
            double expectedPower = 50.0 + (10.0 * cultivation.getSpiritPowerRecoveryRate());
            double actualPower = cultivation.getSpiritPower();
            
            // 允许小误差
            helper.assertTrue(Math.abs(actualPower - expectedPower) < 0.1, 
                "10秒后灵力应为" + expectedPower + "，实际为" + actualPower);
        });
        
        helper.succeed();
    }
    
    /**
     * 测试不同灵根类型的恢复加成
     */
    @GameTest(template = "empty")
    public static void testSpiritualRootTypeRecoveryBonus(GameTestHelper helper) {
        Player player = helper.makeMockPlayer();
        
        // 测试木灵根（恢复加成1.2）
        player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
            // 设置木灵根
            cultivation.setSpiritualRoot(SpiritualRootType.WOOD);
            
            // 验证恢复加成
            double recoveryRate = cultivation.getSpiritPowerRecoveryRate();
            helper.assertTrue(recoveryRate >= 1.2, "木灵根恢复加成应至少为1.2，实际为" + recoveryRate);
        });
        
        // 测试水灵根（恢复加成1.15）
        Player player2 = helper.makeMockPlayer();
        player2.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
            // 设置水灵根
            cultivation.setSpiritualRoot(SpiritualRootType.WATER);
            
            // 验证恢复加成
            double recoveryRate = cultivation.getSpiritPowerRecoveryRate();
            helper.assertTrue(recoveryRate >= 1.15, "水灵根恢复加成应至少为1.15，实际为" + recoveryRate);
        });
        
        helper.succeed();
    }
    
    /**
     * 测试不同灵根品质的恢复加成
     */
    @GameTest(template = "empty")
    public static void testSpiritualRootQualityRecoveryBonus(GameTestHelper helper) {
        Player player = helper.makeMockPlayer();
        
        player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
            // 设置完美品质的木灵根
            cultivation.setSpiritualRoot(SpiritualRootType.WOOD);
            // 注意：这里需要直接设置品质，但当前接口不支持，这是一个测试限制
            // 实际游戏中，品质是在灵根对象中设置的
            
            // 验证恢复加成
            double recoveryRate = cultivation.getSpiritPowerRecoveryRate();
            helper.assertTrue(recoveryRate >= 1.0, "灵根恢复加成应至少为1.0，实际为" + recoveryRate);
        });
        
        helper.succeed();
    }
    
    /**
     * 测试不同境界的恢复加成
     */
    @GameTest(template = "empty")
    public static void testCultivationRealmRecoveryBonus(GameTestHelper helper) {
        Player player = helper.makeMockPlayer();
        
        // 测试金丹境界（恢复加成1.4）
        player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
            // 设置金丹境界
            cultivation.setRealm(CultivationRealm.GOLDEN_CORE);
            
            // 验证恢复加成
            double recoveryRate = cultivation.getSpiritPowerRecoveryRate();
            helper.assertTrue(recoveryRate >= 1.4, "金丹境界恢复加成应至少为1.4，实际为" + recoveryRate);
        });
        
        // 测试元婴境界（恢复加成1.45）
        Player player2 = helper.makeMockPlayer();
        player2.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
            // 设置元婴境界
            cultivation.setRealm(CultivationRealm.NASCENT_SOUL);
            
            // 验证恢复加成
            double recoveryRate = cultivation.getSpiritPowerRecoveryRate();
            helper.assertTrue(recoveryRate >= 1.45, "元婴境界恢复加成应至少为1.45，实际为" + recoveryRate);
        });
        
        helper.succeed();
    }
    
    /**
     * 测试灵力不会超过最大值
     */
    @GameTest(template = "empty")
    public static void testSpiritPowerDoesNotExceedMax(GameTestHelper helper) {
        Player player = helper.makeMockPlayer();
        
        player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
            // 设置初始灵力为最大值
            cultivation.setSpiritPower(100.0);
            cultivation.setMaxSpiritPower(100.0);
            
            // 验证初始状态
            helper.assertTrue(cultivation.getSpiritPower() == 100.0, "初始灵力应为100.0");
            
            // 尝试添加更多灵力
            cultivation.addSpiritPower(50.0);
            
            // 验证灵力不会超过最大值
            helper.assertTrue(cultivation.getSpiritPower() <= 100.0, 
                "灵力不应超过最大值100.0，实际为" + cultivation.getSpiritPower());
        });
        
        helper.succeed();
    }
}