package org.example.Kangnaixi.tiandao.spell;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.example.Kangnaixi.tiandao.capability.ICultivation;
import org.example.Kangnaixi.tiandao.cultivation.CultivationRealm;
import java.util.List;

/**
 * 修仙术法编辑器 - 核心术法类
 * 完全替换现有SpellData，支持三段式骨架 + 属性 + 效果的组件化系统
 */
public class ModularSpell {

    private final String id;
    private final String name;
    private final String description;

    // 骨架结构
    private final SpellCore core;

    // 属性系统
    private final SpellAttributes attributes;

    // 效果系统
    private final SpellEffects effects;

    // 基础属性
    private final SpellStats stats;

    // 剑修强化配置
    private final SwordQiEnhancement swordQiEnhancement;

    // 运行时状态
    private long cooldownEndTime = 0;

    public ModularSpell(String id, String name, String description,
                       SpellCore core, SpellAttributes attributes,
                       SpellEffects effects, SpellStats stats,
                       SwordQiEnhancement swordQiEnhancement) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.core = core;
        this.attributes = attributes;
        this.effects = effects;
        this.stats = stats;
        this.swordQiEnhancement = swordQiEnhancement;
    }

    /**
     * 检查玩家是否满足释放条件
     */
    public boolean canCast(ServerPlayer player, ICultivation cultivation) {
        // 检查境界要求
        if (cultivation.getRealm().ordinal() < stats.getRequiredRealm().ordinal()) {
            return false;
        }

        // 检查灵力
        if (cultivation.getSpiritPower() < stats.getSpiritCost()) {
            return false;
        }

        // 检查冷却时间
        if (isOnCooldown()) {
            return false;
        }

        return true;
    }

    /**
     * 释放术法
     * @return 是否成功释放
     */
    public boolean cast(ServerPlayer player, ICultivation cultivation) {
        if (!canCast(player, cultivation)) {
            return false;
        }

        // 计算实际效果（包含剑修强化）
        SpellExecutionContext context = createExecutionContext(player, cultivation);

        // 扣除灵力
        cultivation.consumeSpiritPower(stats.getSpiritCost());

        // 开始冷却
        startCooldown();

        // 执行术法效果
        executeSpell(context);

        return true;
    }

    /**
     * 创建施法执行上下文
     */
    private SpellExecutionContext createExecutionContext(ServerPlayer player, ICultivation cultivation) {
        // 检查剑修强化条件
        boolean isSwordQiCarrier = core.getCarrier().equals("sword_qi");
        boolean hasSwordIntent = attributes.hasAttribute("sword");
        boolean isHoldingSword = isHoldingSwordItem(player);

        boolean swordEnhanced = isSwordQiCarrier && hasSwordIntent && isHoldingSword;

        // 计算强化后的属性
        double damageMultiplier = 1.0;
        double speedMultiplier = 1.0;
        double rangeMultiplier = 1.0;

        if (swordEnhanced && swordQiEnhancement != null) {
            damageMultiplier = swordQiEnhancement.getDamageMultiplier();
            speedMultiplier = swordQiEnhancement.getSpeedMultiplier();
            rangeMultiplier = swordQiEnhancement.getRangeMultiplier();
        }

        return new SpellExecutionContext(
            player, cultivation, this,
            stats.getDamage() * damageMultiplier,
            stats.getSpeed() * speedMultiplier,
            stats.getRange() * rangeMultiplier,
            swordEnhanced
        );
    }

    /**
     * 检查玩家是否持有剑类武器
     */
    private boolean isHoldingSwordItem(ServerPlayer player) {
        ItemStack mainHandItem = player.getMainHandItem();
        // 检查是否为剑类武器，支持其他mod的剑
        return mainHandItem.is(net.minecraft.tags.ItemTags.SWORDS);
    }

    /**
     * 执行术法效果
     */
    private void executeSpell(SpellExecutionContext context) {
        // 根据载体类型执行不同的效果
        switch (core.getCarrier()) {
            case "sword_qi":
                executeSwordQi(context);
                break;
            case "projectile":
                executeProjectile(context);
                break;
            case "cone":
                executeCone(context);
                break;
            case "field":
                executeField(context);
                break;
            case "glyph":
                executeGlyph(context);
                break;
            case "buff":
                executeBuff(context);
                break;
            default:
                // 默认执行
                executeDefault(context);
        }
    }

    /**
     * 执行剑气效果
     */
    private void executeSwordQi(SpellExecutionContext context) {
        // 创建剑气实体 - 临时使用默认实现
        // TODO: 实现SwordQiProjectileEntity类
        executeDefaultProjectile(context);
    }

    /**
     * 执行默认弹道效果 - 临时实现
     */
    private void executeDefaultProjectile(SpellExecutionContext context) {
        // 临时实现 - 直接造成伤害
        // TODO: 实现真正的弹道实体
        context.getPlayer().sendSystemMessage(
            net.minecraft.network.chat.Component.literal("施放了" + context.getSpell().getName() + "！伤害：" + context.getDamage())
        );
    }

    /**
     * 执行灵光弹效果
     */
    private void executeProjectile(SpellExecutionContext context) {
        // 临时使用默认实现
        executeDefaultProjectile(context);
    }

    /**
     * 执行锥形冲击效果
     */
    private void executeCone(SpellExecutionContext context) {
        // 临时实现
        context.getPlayer().sendSystemMessage(
            net.minecraft.network.chat.Component.literal("施放了锥形冲击！范围：" + context.getRange())
        );
    }

    /**
     * 执行领域效果
     */
    private void executeField(SpellExecutionContext context) {
        // 临时实现
        context.getPlayer().sendSystemMessage(
            net.minecraft.network.chat.Component.literal("展开了领域！")
        );
    }

    /**
     * 执行地面术阵效果
     */
    private void executeGlyph(SpellExecutionContext context) {
        // 临时实现
        context.getPlayer().sendSystemMessage(
            net.minecraft.network.chat.Component.literal("在地面刻下了术阵！")
        );
    }

    /**
     * 执行附体加持效果
     */
    private void executeBuff(SpellExecutionContext context) {
        // 临时实现
        context.getPlayer().sendSystemMessage(
            net.minecraft.network.chat.Component.literal("获得了加持效果！")
        );
    }

    /**
     * 默认执行
     */
    private void executeDefault(SpellExecutionContext context) {
        context.getPlayer().sendSystemMessage(
            net.minecraft.network.chat.Component.literal("施放了术法：" + context.getSpell().getName())
        );
    }

    // Getter方法

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public SpellCore getCore() {
        return core;
    }

    public SpellAttributes getAttributes() {
        return attributes;
    }

    public SpellEffects getEffects() {
        return effects;
    }

    public SpellStats getStats() {
        return stats;
    }

    public SwordQiEnhancement getSwordQiEnhancement() {
        return swordQiEnhancement;
    }

    // 冷却相关方法
    public boolean isOnCooldown() {
        return System.currentTimeMillis() < cooldownEndTime;
    }

    public int getCooldownRemaining() {
        if (!isOnCooldown()) {
            return 0;
        }
        return (int) ((cooldownEndTime - System.currentTimeMillis()) / 1000);
    }

    public void startCooldown() {
        this.cooldownEndTime = System.currentTimeMillis() + (long)(stats.getCooldown() * 1000.0);
    }

    public void clearCooldown() {
        this.cooldownEndTime = 0;
    }

    // 序列化方法
    public CompoundTag toNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("id", id);
        nbt.putLong("cooldownEndTime", cooldownEndTime);
        return nbt;
    }

    public static ModularSpell fromNBT(CompoundTag nbt) {
        // 临时实现 - 创建一个默认的术法
        // TODO: 实现真正的从NBT加载逻辑
        SpellCore core = new SpellCore("finger", "sword_qi", "instant");
        SpellAttributes attrs = new SpellAttributes(List.of());
        SpellEffects effects = new SpellEffects(List.of());
        SpellStats stats = new SpellStats(10, 1.0, 10, 6, 25, 0, 0, null, 0);
        SwordQiEnhancement enhancement = SwordQiEnhancement.createNone();

        ModularSpell spell = new ModularSpell(
            "temp_spell", "临时术法", "从NBT加载的临时术法",
            core, attrs, effects, stats, enhancement
        );
        spell.cooldownEndTime = nbt.getLong("cooldownEndTime");
        return spell;
    }

    // Getter方法已在前面定义，这里不需要重复定义

    @Override
    public String toString() {
        return String.format("ModularSpell[id=%s, name=%s, core=%s]", id, name, core);
    }
}