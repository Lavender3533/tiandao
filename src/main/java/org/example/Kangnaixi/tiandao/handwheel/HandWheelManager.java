package org.example.Kangnaixi.tiandao.handwheel;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.capability.IStarChartData;
import org.example.Kangnaixi.tiandao.spell.blueprint.SpellBlueprint;
import org.example.Kangnaixi.tiandao.spell.blueprint.SpellBlueprintBuilder;
import org.example.Kangnaixi.tiandao.spell.blueprint.SpellBlueprintLibrary;
import org.example.Kangnaixi.tiandao.starchart.StarNode;
import org.example.Kangnaixi.tiandao.starchart.StarNodeCategory;
import org.example.Kangnaixi.tiandao.starchart.StarTestNodes;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 手盘管理器 - 连接星盘解锁与术法组合
 *
 * 职责：
 * 1. 管理当前组合槽位
 * 2. 读取玩家已解锁节点
 * 3. 验证节点组合合法性
 * 4. 调用Builder生成Blueprint
 * 5. 写入玩家已学术法
 */
public class HandWheelManager {
    private static HandWheelManager clientInstance;

    private final HandWheelCombination combination;
    private boolean enabled = false;
    private String lastError = null;

    public HandWheelManager() {
        this.combination = new HandWheelCombination();
    }

    public static HandWheelManager getClientInstance() {
        if (clientInstance == null) {
            clientInstance = new HandWheelManager();
        }
        return clientInstance;
    }

    // ========== 状态管理 ==========

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean toggle() {
        this.enabled = !this.enabled;
        if (!enabled) {
            combination.clearAll();
        }
        return enabled;
    }

    public HandWheelCombination getCombination() {
        return combination;
    }

    @Nullable
    public String getLastError() {
        return lastError;
    }

    // ========== 槽位操作（核心API） ==========

    /**
     * 设置槽位节点（供星盘联动调用）
     *
     * @param category 节点类别
     * @param nodeId   节点ID
     * @param player   玩家（用于验证解锁状态）
     * @return true if slot set successfully
     */
    public boolean setSlot(StarNodeCategory category, String nodeId, Player player) {
        lastError = null;

        // 1. 验证节点存在
        StarNode node = StarTestNodes.getNodeById(nodeId);
        if (node == null) {
            lastError = "节点不存在: " + nodeId;
            Tiandao.LOGGER.warn(lastError);
            return false;
        }

        // 2. 验证类别匹配
        if (node.getCategory() != category) {
            lastError = "节点类别不匹配";
            Tiandao.LOGGER.warn(lastError);
            return false;
        }

        // 3. 验证玩家已解锁（跳过BLUEPRINT类型）
        if (category != StarNodeCategory.BLUEPRINT) {
            IStarChartData starChart = player.getCapability(Tiandao.STAR_CHART_CAP).orElse(null);
            if (starChart == null || !starChart.isNodeUnlocked(nodeId)) {
                lastError = "节点未解锁: " + node.getName();
                Tiandao.LOGGER.warn(lastError);
                return false;
            }
        }

        // 4. 设置槽位
        boolean success = combination.setSlot(category, nodeId);
        if (success) {
            Tiandao.LOGGER.debug("手盘槽位设置: {} → {}", category.getDisplayName(), node.getName());
        }
        return success;
    }

    /**
     * 清空指定槽位
     */
    public void clearSlot(StarNodeCategory category) {
        combination.clearSlot(category);
        Tiandao.LOGGER.debug("手盘槽位清空: {}", category.getDisplayName());
    }

    /**
     * 清空所有槽位
     */
    public void clearAll() {
        combination.clearAll();
        lastError = null;
        Tiandao.LOGGER.debug("手盘全部清空");
    }

    // ========== 获取可用节点 ==========

    /**
     * 获取玩家已解锁的指定类别节点
     */
    public List<StarNode> getUnlockedNodes(Player player, StarNodeCategory category) {
        List<StarNode> result = new ArrayList<>();

        IStarChartData starChart = player.getCapability(Tiandao.STAR_CHART_CAP).orElse(null);
        if (starChart == null) return result;

        Set<String> unlockedIds = starChart.getUnlockedNodes();

        for (StarNode node : StarTestNodes.getAllNodes()) {
            if (node.getCategory() == category && unlockedIds.contains(node.getId())) {
                result.add(node);
            }
        }

        return result;
    }

    /**
     * 获取所有已解锁节点
     */
    public List<StarNode> getAllUnlockedNodes(Player player) {
        List<StarNode> result = new ArrayList<>();

        IStarChartData starChart = player.getCapability(Tiandao.STAR_CHART_CAP).orElse(null);
        if (starChart == null) return result;

        Set<String> unlockedIds = starChart.getUnlockedNodes();

        for (StarNode node : StarTestNodes.getAllNodes()) {
            if (node.getCategory() != StarNodeCategory.BLUEPRINT
                && unlockedIds.contains(node.getId())) {
                result.add(node);
            }
        }

        return result;
    }

    // ========== 编译组合 → 生成Blueprint ==========

    /**
     * 编译当前组合为Blueprint
     *
     * @param player 玩家（用于设置创建者信息）
     * @return CompileResult 编译结果
     */
    public CompileResult compile(Player player) {
        lastError = null;

        // 1. 验证组合
        HandWheelCombination.ValidationResult validation = combination.validate();
        if (!validation.isValid()) {
            lastError = validation.getMessage();
            return new CompileResult(false, lastError, null);
        }

        // 2. 构建Blueprint
        SpellBlueprintBuilder builder = new SpellBlueprintBuilder();

        try {
            // 设置核心节点（2必填）
            StarNode effectNode = combination.getEffectSlot().getNode();
            StarNode formNode = combination.getFormSlot().getNode();

            builder.setEffectNode(effectNode)
                   .setFormNode(formNode);

            // 设置调制节点
            for (StarNode modifier : combination.getActiveModifiers()) {
                builder.addModifier(modifier);
            }

            // 设置创建者信息
            builder.setCreatorId(player.getUUID())
                   .setCreatorName(player.getScoreboardName());

            // 构建
            SpellBlueprint blueprint = builder.build();

            Tiandao.LOGGER.info("术法组合成功: {} ({})",
                blueprint.getName(), blueprint.getId());

            return new CompileResult(true, "组合成功", blueprint);

        } catch (SpellBlueprintBuilder.BuildException e) {
            lastError = e.getMessage();
            Tiandao.LOGGER.warn("术法组合失败: {}", lastError);
            return new CompileResult(false, lastError, null);
        }
    }

    /**
     * 编译并保存到玩家数据
     */
    public CompileResult compileAndSave(ServerPlayer player) {
        CompileResult result = compile(player);

        if (result.isSuccess() && result.getBlueprint() != null) {
            SpellBlueprint blueprint = result.getBlueprint();

            // 1. 注册到全局蓝图库
            SpellBlueprintLibrary.register(blueprint);

            // 2. 添加到玩家已学术法
            player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
                // 解锁术法ID
                cultivation.unlockSpell(blueprint.getId());
                // 保存完整蓝图数据
                cultivation.learnBlueprint(blueprint);
                Tiandao.LOGGER.info("玩家 {} 学会新术法: {}",
                    player.getScoreboardName(), blueprint.getName());
            });

            // 3. 同步到客户端（如需要）
            // NetworkHandler.sendToPlayer(new SpellLearnedPacket(blueprint), player);

            // 4. 清空手盘（可选）
            // clearAll();
        }

        return result;
    }

    // ========== 序列化 ==========

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("enabled", enabled);
        tag.put("combination", combination.serializeNBT());
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        this.enabled = tag.getBoolean("enabled");
        if (tag.contains("combination")) {
            combination.deserializeNBT(tag.getCompound("combination"));
        }
    }

    // ========== 内部类 ==========

    public static class CompileResult {
        private final boolean success;
        private final String message;
        private final SpellBlueprint blueprint;

        public CompileResult(boolean success, String message, @Nullable SpellBlueprint blueprint) {
            this.success = success;
            this.message = message;
            this.blueprint = blueprint;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        @Nullable
        public SpellBlueprint getBlueprint() {
            return blueprint;
        }
    }
}
