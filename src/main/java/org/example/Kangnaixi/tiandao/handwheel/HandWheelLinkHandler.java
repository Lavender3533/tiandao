package org.example.Kangnaixi.tiandao.handwheel;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.client.starchart.StarChartClientManager;
import org.example.Kangnaixi.tiandao.client.starchart.StarNodeInstance;
import org.example.Kangnaixi.tiandao.starchart.StarNode;
import org.example.Kangnaixi.tiandao.starchart.StarNodeCategory;

/**
 * 星盘→手盘联动处理器
 *
 * 触发条件：
 * - 玩家在星盘聚焦状态下注视某个子节点
 * - 按下确认键（默认为鼠标右键或R键）
 *
 * 流程：
 * 1. 获取当前注视的节点
 * 2. 验证节点已解锁
 * 3. 填充到手盘对应槽位
 * 4. 播放反馈效果
 */
public class HandWheelLinkHandler {
    private static HandWheelLinkHandler instance;

    private boolean linkModeEnabled = false;
    private long lastLinkTime = 0;
    private static final long LINK_COOLDOWN_MS = 200; // 防抖

    private HandWheelLinkHandler() {}

    public static HandWheelLinkHandler getInstance() {
        if (instance == null) {
            instance = new HandWheelLinkHandler();
        }
        return instance;
    }

    /**
     * 切换联动模式
     */
    public boolean toggleLinkMode() {
        linkModeEnabled = !linkModeEnabled;
        Tiandao.LOGGER.debug("手盘联动模式: {}", linkModeEnabled ? "开启" : "关闭");
        return linkModeEnabled;
    }

    public boolean isLinkModeEnabled() {
        return linkModeEnabled;
    }

    public void setLinkModeEnabled(boolean enabled) {
        this.linkModeEnabled = enabled;
    }

    /**
     * 尝试将当前注视的星盘节点填充到手盘
     *
     * @return true if node was successfully linked
     */
    public boolean tryLinkGazedNode() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;

        // 防抖
        long now = System.currentTimeMillis();
        if (now - lastLinkTime < LINK_COOLDOWN_MS) {
            return false;
        }
        lastLinkTime = now;

        // 检查星盘状态
        StarChartClientManager starChart = StarChartClientManager.getInstance();
        if (!starChart.isEnabled()) {
            return false;
        }

        // 获取注视节点
        StarNodeInstance gazedInstance = starChart.getGazedNode();
        if (gazedInstance == null) {
            Tiandao.LOGGER.info("[HandWheel] 联动失败: 没有注视任何节点 (当前状态: {})", starChart.getCurrentState());
            return false;
        }

        StarNode node = gazedInstance.getNode();
        if (node == null) {
            return false;
        }

        // === DEBUG LOG: 打印注视节点信息 ===
        Tiandao.LOGGER.info("[HandWheel-Link] 注视节点: id={}, name={}, category={}, bindingKey={}",
            node.getId(), node.getName(), node.getCategory(), node.getBindingKey());

        // 跳过主节点
        if (gazedInstance.isMasterNode()) {
            Tiandao.LOGGER.info("[HandWheel] 联动跳过: 主节点不能直接填充");
            return false;
        }

        // BLUEPRINT 类型暂时只做展示，不参与组合
        if (node.getCategory() == StarNodeCategory.BLUEPRINT) {
            Tiandao.LOGGER.info("[HandWheel] 联动跳过: BLUEPRINT节点仅用于展示");
            return false;
        }

        // 执行填充
        return linkNodeToHandWheel(node, mc.player);
    }

    /**
     * 将指定节点填充到手盘槽位
     *
     * 映射规则（3槽位）：
     * - EFFECT 节点 → effectSlot
     * - FORM 节点 → formSlot
     * - MODIFIER 节点 → modifierSlots（追加）
     *
     * 注：源（Source）由手盘中心独立选择，不在联动范围内
     */
    public boolean linkNodeToHandWheel(StarNode node, Player player) {
        if (node == null || player == null) return false;

        HandWheelManager handWheel = HandWheelManager.getClientInstance();

        // 确保手盘已启用
        if (!handWheel.isEnabled()) {
            handWheel.setEnabled(true);
        }

        StarNodeCategory category = node.getCategory();
        String bindingKey = node.getBindingKey();

        // === DEBUG LOG: 打印槽位映射信息 ===
        Tiandao.LOGGER.info("[HandWheel-Link] 填充槽位: category={}, bindingKey={}, targetSlot={}",
            category, bindingKey, category.getDisplayName());

        // 根据类别填充到对应槽位（使用 bindingKey）
        boolean success = handWheel.setSlot(category, bindingKey, player);

        if (success) {
            Tiandao.LOGGER.info("[HandWheel-Link] 成功: {} ({}) → {} 槽位",
                node.getName(), bindingKey, category.getDisplayName());

            // 打印当前组合状态
            logCurrentCombination(handWheel);

            playLinkFeedback(player);
        } else {
            Tiandao.LOGGER.warn("[HandWheel-Link] 失败: {} ({}) - 原因: {}",
                node.getName(), bindingKey, handWheel.getLastError());
        }

        return success;
    }

    /**
     * 打印当前手盘组合状态（调试用）
     */
    private void logCurrentCombination(HandWheelManager handWheel) {
        HandWheelCombination combo = handWheel.getCombination();

        StringBuilder sb = new StringBuilder("[HandWheel-Combo] 当前组合:\n");

        // Effect
        if (!combo.getEffectSlot().isEmpty()) {
            StarNode n = combo.getEffectSlot().getNode();
            sb.append("  EFFECT: ").append(n != null ? n.getName() + " (" + n.getBindingKey() + ")" : "null").append("\n");
        } else {
            sb.append("  EFFECT: 空\n");
        }

        // Form
        if (!combo.getFormSlot().isEmpty()) {
            StarNode n = combo.getFormSlot().getNode();
            sb.append("  FORM: ").append(n != null ? n.getName() + " (" + n.getBindingKey() + ")" : "null").append("\n");
        } else {
            sb.append("  FORM: 空\n");
        }

        // Modifiers
        var mods = combo.getActiveModifiers();
        if (!mods.isEmpty()) {
            sb.append("  MODIFIERS: ");
            for (StarNode mod : mods) {
                sb.append(mod.getName()).append(" (").append(mod.getBindingKey()).append("), ");
            }
            sb.append("\n");
        } else {
            sb.append("  MODIFIERS: 空\n");
        }

        Tiandao.LOGGER.info(sb.toString());
    }

    /**
     * 快速填充：按类别自动选择第一个解锁节点
     */
    public boolean autoFillSlot(StarNodeCategory category, Player player) {
        HandWheelManager handWheel = HandWheelManager.getClientInstance();

        var unlockedNodes = handWheel.getUnlockedNodes(player, category);
        if (unlockedNodes.isEmpty()) {
            Tiandao.LOGGER.warn("自动填充失败: {} 类别没有已解锁节点", category.getDisplayName());
            return false;
        }

        StarNode firstNode = unlockedNodes.get(0);
        return linkNodeToHandWheel(firstNode, player);
    }

    /**
     * 播放联动反馈效果
     */
    private void playLinkFeedback(Player player) {
        // 播放音效
        // player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f);

        // 可以在这里添加粒子效果
    }

    /**
     * 检查是否可以执行联动
     */
    public boolean canLink() {
        if (!linkModeEnabled) return false;

        StarChartClientManager starChart = StarChartClientManager.getInstance();
        if (!starChart.isEnabled()) return false;

        // 只在聚焦状态下允许联动
        if (starChart.getCurrentState() != StarChartClientManager.State.FOCUSED) {
            return false;
        }

        return true;
    }

    /**
     * 获取当前可联动的节点（用于UI提示）
     */
    public StarNode getAvailableLinkNode() {
        StarChartClientManager starChart = StarChartClientManager.getInstance();
        if (!starChart.isEnabled()) return null;

        StarNodeInstance gazed = starChart.getGazedNode();
        if (gazed == null || gazed.isMasterNode()) return null;

        return gazed.getNode();
    }
}
