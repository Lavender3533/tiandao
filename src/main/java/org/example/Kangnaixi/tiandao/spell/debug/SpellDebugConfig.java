package org.example.Kangnaixi.tiandao.spell.debug;

public final class SpellDebugConfig {

    private static boolean showTargets = false;

    private SpellDebugConfig() {
    }

    public static boolean isShowTargets() {
        return showTargets;
    }

    public static void setShowTargets(boolean value) {
        showTargets = value;
    }

    public static void toggleTargets() {
        showTargets = !showTargets;
    }
}

