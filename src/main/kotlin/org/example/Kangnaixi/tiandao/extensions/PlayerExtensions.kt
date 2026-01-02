package org.example.Kangnaixi.tiandao.extensions

import net.minecraft.world.entity.player.Player
import org.example.Kangnaixi.tiandao.Tiandao
import org.example.Kangnaixi.tiandao.cultivation.CultivationRealm

/**
 * Player 扩展函数集合
 *
 * 这个文件展示了 Kotlin 的扩展函数特性，让你可以像调用 Player 自己的方法一样
 * 调用这些便捷函数，而无需创建工具类。
 *
 * 使用示例：
 * ```
 * if (player.canCastSpell(50.0)) {
 *     player.consumeMana(50.0)
 *     // 施放术法
 * }
 * ```
 */

// ==================== 扩展属性 ====================

/**
 * 获取玩家的修仙数据
 * 使用：val data = player.cultivation
 */
val Player.cultivation
    get() = this.getCapability(Tiandao.CULTIVATION_CAPABILITY).resolve().orElse(null)

/**
 * 获取玩家的修仙境界
 * 使用：val realm = player.cultivationRealm
 */
val Player.cultivationRealm: CultivationRealm?
    get() = cultivation?.realm

/**
 * 获取玩家当前灵力值
 * 使用：val energy = player.spiritualEnergy
 */
val Player.spiritualEnergy: Double
    get() = cultivation?.spiritPower ?: 0.0

/**
 * 获取玩家最大灵力值
 * 使用：val maxEnergy = player.maxSpiritualEnergy
 */
val Player.maxSpiritualEnergy: Double
    get() = cultivation?.maxSpiritPower ?: 0.0

// ==================== 扩展函数 ====================

/**
 * 增加玩家灵力
 * @param amount 增加的灵力值
 * @return 是否成功（玩家有修仙数据则成功）
 */
fun Player.addSpiritualEnergy(amount: Double): Boolean {
    return cultivation?.let {
        it.addSpiritPower(amount)
        true
    } ?: false
}

/**
 * 消耗玩家灵力
 * @param amount 消耗的灵力值
 * @return 是否成功（灵力足够则成功）
 */
fun Player.consumeMana(amount: Double): Boolean {
    return cultivation?.consumeSpiritPower(amount) ?: false
}

/**
 * 检查玩家是否有足够灵力施放术法
 * @param manaCost 术法消耗
 * @return 是否有足够灵力
 */
fun Player.canCastSpell(manaCost: Double): Boolean {
    return spiritualEnergy >= manaCost
}

/**
 * 检查玩家是否达到指定境界
 * @param requiredRealm 需要的境界
 * @return 是否达到
 */
fun Player.hasReachedRealm(requiredRealm: CultivationRealm): Boolean {
    val currentRealm = cultivationRealm ?: return false
    return currentRealm.ordinal >= requiredRealm.ordinal
}

/**
 * 获取玩家灵力百分比
 * @return 0.0 到 1.0 之间的值
 */
fun Player.getEnergyPercentage(): Double {
    val max = maxSpiritualEnergy
    return if (max > 0) spiritualEnergy / max else 0.0
}

// ==================== 使用示例（注释） ====================

/*
// 在任何 Java 或 Kotlin 代码中使用：

// 示例 1：检查并消耗灵力
if (player.canCastSpell(50.0)) {
    player.consumeMana(50.0)
    // 施放火球术
}

// 示例 2：获取境界信息
val realm = player.cultivationRealm
if (realm != null) {
    player.sendSystemMessage(Component.literal("当前境界：${realm.name}"))
}

// 示例 3：增加灵力
player.addSpiritualEnergy(100.0)

// 示例 4：检查境界要求
if (player.hasReachedRealm(CultivationRealm.FOUNDATION)) {
    // 解锁筑基期术法
}

// 示例 5：显示灵力百分比
val percentage = player.getEnergyPercentage()
println("灵力：${(percentage * 100).toInt()}%")
*/

