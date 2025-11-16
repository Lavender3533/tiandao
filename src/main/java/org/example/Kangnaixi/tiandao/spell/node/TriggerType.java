package org.example.Kangnaixi.tiandao.spell.node;

/**
 * 触发方式 - 术法如何被释放
 */
public enum TriggerType {
    /**
     * 自身 - 以施法者为中心释放
     */
    SELF("自身", "以自己为中心释放", 5.0),

    /**
     * 触摸 - 触摸前方目标释放
     */
    TOUCH("触摸", "触摸前方3格释放", 8.0),

    /**
     * 弹道 - 发射弹道，命中时释放
     */
    PROJECTILE("弹道", "发射弹道，命中时释放", 10.0);

    private final String displayName;
    private final String description;
    private final double spiritCost;

    TriggerType(String displayName, String description, double spiritCost) {
        this.displayName = displayName;
        this.description = description;
        this.spiritCost = spiritCost;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public double getSpiritCost() {
        return spiritCost;
    }
}
