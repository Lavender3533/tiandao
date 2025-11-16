package org.example.Kangnaixi.tiandao.spell.rune;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 符文抽象类 - 术法系统的基础构建块
 */
public abstract class Rune {

    private final String id;
    private final String name;
    private final RuneTier tier;
    private final RuneCategory category;
    private final String description;
    private final double spiritCost;
    private final double cooldown;
    private final String unlockRealm;
    private final int unlockLevel;
    private final int color;

    // 输入输出
    private final int inputs;  // 可接受几个前置符文
    private final int outputs; // 可以连接几个后续符文

    public Rune(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.tier = builder.tier;
        this.category = builder.category;
        this.description = builder.description;
        this.spiritCost = builder.spiritCost;
        this.cooldown = builder.cooldown;
        this.unlockRealm = builder.unlockRealm;
        this.unlockLevel = builder.unlockLevel;
        this.color = builder.color;
        this.inputs = builder.inputs;
        this.outputs = builder.outputs;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public RuneTier getTier() { return tier; }
    public RuneCategory getCategory() { return category; }
    public String getDescription() { return description; }
    public double getSpiritCost() { return spiritCost; }
    public double getCooldown() { return cooldown; }
    public String getUnlockRealm() { return unlockRealm; }
    public int getUnlockLevel() { return unlockLevel; }
    public int getColor() { return color; }
    public int getInputs() { return inputs; }
    public int getOutputs() { return outputs; }

    /**
     * 符文执行逻辑
     */
    public abstract void execute(RuneContext context);

    /**
     * 是否可以连接到指定符文
     */
    public boolean canConnectTo(Rune target) {
        return this.outputs > 0 && target.inputs > 0;
    }

    /**
     * 符文等阶
     */
    public enum RuneTier {
        TIAN("天阶", 0xFF4FC3F7),  // 蓝色
        DI("地阶", 0xFF66BB6A),    // 绿色
        XUAN("玄阶", 0xFFE1BEE7),  // 紫色
        HUANG("黄阶", 0xFFFFD700); // 金色

        private final String displayName;
        private final int color;

        RuneTier(String displayName, int color) {
            this.displayName = displayName;
            this.color = color;
        }

        public String getDisplayName() { return displayName; }
        public int getColor() { return color; }
    }

    /**
     * 符文分类
     */
    public enum RuneCategory {
        TRIGGER("触发", 0xFF4FC3F7),      // 蓝色
        SHAPE("形态", 0xFF66BB6A),        // 绿色
        EFFECT("效果", 0xFFE1BEE7),       // 紫色
        CONDITION("条件", 0xFFFF9800),    // 橙色
        LOGIC("逻辑", 0xFFF44336),        // 红色
        MODIFIER("强化", 0xFFFFD700);     // 金色

        private final String displayName;
        private final int color;

        RuneCategory(String displayName, int color) {
            this.displayName = displayName;
            this.color = color;
        }

        public String getDisplayName() { return displayName; }
        public int getColor() { return color; }
    }

    /**
     * Builder模式构建符文
     */
    public static class Builder {
        private String id;
        private String name;
        private RuneTier tier = RuneTier.TIAN;
        private RuneCategory category = RuneCategory.EFFECT;
        private String description = "";
        private double spiritCost = 5.0;
        private double cooldown = 1.0;
        private String unlockRealm = "QI_CONDENSATION";
        private int unlockLevel = 1;
        private int color = 0xFF4FC3F7;
        private int inputs = 1;
        private int outputs = 1;

        public Builder(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public Builder tier(RuneTier tier) {
            this.tier = tier;
            this.color = tier.getColor();
            return this;
        }

        public Builder category(RuneCategory category) {
            this.category = category;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder spiritCost(double spiritCost) {
            this.spiritCost = spiritCost;
            return this;
        }

        public Builder cooldown(double cooldown) {
            this.cooldown = cooldown;
            return this;
        }

        public Builder unlockRealm(String realm) {
            this.unlockRealm = realm;
            return this;
        }

        public Builder unlockLevel(int level) {
            this.unlockLevel = level;
            return this;
        }

        public Builder color(int color) {
            this.color = color;
            return this;
        }

        public Builder inputs(int inputs) {
            this.inputs = inputs;
            return this;
        }

        public Builder outputs(int outputs) {
            this.outputs = outputs;
            return this;
        }
    }
}
