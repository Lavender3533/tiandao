package org.example.Kangnaixi.tiandao.spell;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import org.example.Kangnaixi.tiandao.spell.runtime.FormType;

import javax.annotation.Nullable;

/**
 * 引导施法状态数据结构
 * 用于管理 CHANNEL（引导）和 DURATION（持续通道）类型术法的施法进度
 */
public class ChannelingSpellState {
    private final String spellId;              // 术法 ID
    private final FormType formType;           // CHANNEL 或 DURATION
    private final int totalTicks;              // 总引导时间（ticks）
    private int ticksElapsed;                  // 已经过的 ticks
    private final double spiritCostPerTick;    // 每 tick 灵力消耗
    private final long startTime;              // 开始时间戳（毫秒）

    // 用于 CHANNEL 类型：记录玩家初始位置，用于移动打断检测
    @Nullable
    private final Vec3 startPosition;

    public ChannelingSpellState(String spellId, FormType formType, int totalTicks,
                                double spiritCostPerTick, @Nullable Vec3 startPosition) {
        this(spellId, formType, totalTicks, 0, spiritCostPerTick,
            System.currentTimeMillis(), startPosition);
    }

    private ChannelingSpellState(String spellId, FormType formType, int totalTicks,
                                 int ticksElapsed, double spiritCostPerTick,
                                 long startTime, @Nullable Vec3 startPosition) {
        this.spellId = spellId;
        this.formType = formType;
        this.totalTicks = totalTicks;
        this.ticksElapsed = ticksElapsed;
        this.spiritCostPerTick = spiritCostPerTick;
        this.startTime = startTime;
        this.startPosition = startPosition;
    }

    /**
     * 推进一个 Tick
     */
    public void tick() {
        ticksElapsed++;
    }

    /**
     * 引导是否仍在进行中
     */
    public boolean isActive() {
        return ticksElapsed < totalTicks;
    }

    /**
     * 是否应在玩家移动时打断（仅 CHANNEL 类型）
     */
    public boolean shouldInterruptOnMovement() {
        return formType == FormType.CHANNEL;
    }

    /**
     * 获取引导进度（0.0 - 1.0）
     */
    public double getProgress() {
        return Math.min(1.0, (double) ticksElapsed / totalTicks);
    }

    /**
     * 获取剩余 Tick 数
     */
    public int getRemainingTicks() {
        return Math.max(0, totalTicks - ticksElapsed);
    }

    /**
     * 序列化为 NBT（用于保存玩家数据）
     */
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("spellId", spellId);
        tag.putString("formType", formType.name());
        tag.putInt("totalTicks", totalTicks);
        tag.putInt("ticksElapsed", ticksElapsed);
        tag.putDouble("spiritCostPerTick", spiritCostPerTick);
        tag.putLong("startTime", startTime);

        if (startPosition != null) {
            tag.putDouble("startX", startPosition.x);
            tag.putDouble("startY", startPosition.y);
            tag.putDouble("startZ", startPosition.z);
        }

        return tag;
    }

    /**
     * 从 NBT 反序列化
     */
    public static ChannelingSpellState fromNBT(CompoundTag tag) {
        Vec3 startPos = null;
        if (tag.contains("startX")) {
            startPos = new Vec3(
                tag.getDouble("startX"),
                tag.getDouble("startY"),
                tag.getDouble("startZ")
            );
        }

        return new ChannelingSpellState(
            tag.getString("spellId"),
            FormType.valueOf(tag.getString("formType")),
            tag.getInt("totalTicks"),
            tag.getInt("ticksElapsed"),
            tag.getDouble("spiritCostPerTick"),
            tag.getLong("startTime"),
            startPos
        );
    }

    // Getters
    public String getSpellId() {
        return spellId;
    }

    public FormType getFormType() {
        return formType;
    }

    public int getTotalTicks() {
        return totalTicks;
    }

    public int getTicksElapsed() {
        return ticksElapsed;
    }

    public double getSpiritCostPerTick() {
        return spiritCostPerTick;
    }

    public long getStartTime() {
        return startTime;
    }

    @Nullable
    public Vec3 getStartPosition() {
        return startPosition;
    }

    @Override
    public String toString() {
        return "ChannelingSpellState{" +
            "spellId='" + spellId + '\'' +
            ", formType=" + formType +
            ", progress=" + getProgress() * 100 + "%" +
            ", ticks=" + ticksElapsed + "/" + totalTicks +
            ", spiritCostPerTick=" + spiritCostPerTick +
            '}';
    }
}
