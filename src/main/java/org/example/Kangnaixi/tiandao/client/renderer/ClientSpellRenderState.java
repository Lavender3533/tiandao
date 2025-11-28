package org.example.Kangnaixi.tiandao.client.renderer;

/**
 * 客户端术法可视化状态控制。
 * 仅在本地存储手盘、星盘的可见状态，服务端同步逻辑后续接入。
 */
public final class ClientSpellRenderState {
    private static boolean wheelVisible;
    private static boolean starVisible;

    private ClientSpellRenderState() {
    }

    public static boolean isWheelVisible() {
        return wheelVisible;
    }

    public static boolean isStarVisible() {
        return starVisible;
    }

    public static void setWheelVisible(boolean visible) {
        wheelVisible = visible;
    }

    public static void setStarVisible(boolean visible) {
        starVisible = visible;
    }

    /**
     * 一键切换（同时控制手盘与星盘）。
     *
     * @return 切换后的状态（true = 开启）
     */
    public static boolean toggleAll() {
        boolean next = !wheelVisible || !starVisible;
        wheelVisible = next;
        starVisible = next;
        return next;
    }
}
