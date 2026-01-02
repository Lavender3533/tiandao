package org.example.Kangnaixi.tiandao.data

import org.example.Kangnaixi.tiandao.cultivation.CultivationRealm
import org.example.Kangnaixi.tiandao.cultivation.SpiritualRoot
import java.util.UUID

/**
 * 修仙数据快照
 *
 * 这是一个 Kotlin 数据类示例，展示了如何用 5 行代码替代 Java 的 50+ 行。
 *
 * 数据类自动生成：
 * - getter/setter
 * - equals() 和 hashCode()
 * - toString()
 * - copy() 方法
 * - componentN() 解构函数
 *
 * 对比 Java 版本，这个类节省了大约 80% 的代码！
 */
data class CultivationSnapshot(
    val playerId: UUID,
    val playerName: String,
    val realm: CultivationRealm,
    val subRealmLevel: Int,
    val spiritualEnergy: Double,
    val maxSpiritualEnergy: Double,
    val spiritualRoot: SpiritualRoot?,
    val foundationQuality: Int,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * 获取灵力百分比
     */
    val energyPercentage: Double
        get() = if (maxSpiritualEnergy > 0) spiritualEnergy / maxSpiritualEnergy else 0.0

    /**
     * 检查是否灵力充足
     */
    fun hasEnoughEnergy(required: Double): Boolean = spiritualEnergy >= required

    /**
     * 创建一个修改了灵力的副本
     * 展示 Kotlin 的 copy() 方法
     */
    fun withEnergy(newEnergy: Double): CultivationSnapshot {
        return copy(spiritualEnergy = newEnergy.coerceIn(0.0, maxSpiritualEnergy))
    }

    /**
     * 创建一个突破到新境界的副本
     */
    fun breakthrough(newRealm: CultivationRealm, newMaxEnergy: Double): CultivationSnapshot {
        return copy(
            realm = newRealm,
            subRealmLevel = 1,
            maxSpiritualEnergy = newMaxEnergy,
            spiritualEnergy = newMaxEnergy,
            timestamp = System.currentTimeMillis()
        )
    }

    companion object {
        /**
         * 从玩家创建快照
         */
        @JvmStatic
        fun fromPlayer(player: net.minecraft.world.entity.player.Player): CultivationSnapshot? {
            val cultivation = player.getCapability(org.example.Kangnaixi.tiandao.Tiandao.CULTIVATION_CAPABILITY)
                .resolve().orElse(null) ?: return null

            return CultivationSnapshot(
                playerId = player.uuid,
                playerName = player.name.string,
                realm = cultivation.realm,
                subRealmLevel = cultivation.subRealm.ordinal,
                spiritualEnergy = cultivation.spiritPower,
                maxSpiritualEnergy = cultivation.maxSpiritPower,
                spiritualRoot = if (cultivation is org.example.Kangnaixi.tiandao.capability.CultivationCapability) {
                    cultivation.spiritualRootObject
                } else null,
                foundationQuality = cultivation.foundation
            )
        }
    }
}

// ==================== 使用示例（注释） ====================

/*
// 创建快照
val snapshot = CultivationSnapshot.fromPlayer(player)

// 使用自动生成的 toString()
println(snapshot)  // 输出所有字段

// 使用 copy() 创建修改版本
val newSnapshot = snapshot.copy(spiritualEnergy = 1000.0)

// 解构（Destructuring）
val (id, name, realm, level) = snapshot
println("玩家 $name 当前境界：$realm $level 层")

// 比较（自动生成的 equals）
if (snapshot1 == snapshot2) {
    println("数据相同")
}

// 使用自定义方法
if (snapshot.hasEnoughEnergy(50.0)) {
    val afterCast = snapshot.withEnergy(snapshot.spiritualEnergy - 50.0)
}

// 突破
val afterBreakthrough = snapshot.breakthrough(CultivationRealm.GOLDEN_CORE, 5000.0)
*/
