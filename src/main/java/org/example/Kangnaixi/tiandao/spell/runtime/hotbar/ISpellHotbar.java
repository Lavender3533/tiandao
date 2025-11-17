package org.example.Kangnaixi.tiandao.spell.runtime.hotbar;

/**
 * 术法快捷栏能力接口
 */
public interface ISpellHotbar {

    /**
     * 获取所有槽位的副本
     */
    String[] getSlots();

    /**
     * 获取指定槽位的术法ID
     * @param index 槽位索引（0~8）
     * @return 术法ID，如果为空返回null
     */
    String getSlot(int index);

    /**
     * 设置指定槽位的术法ID
     * @param index 槽位索引（0~8）
     * @param spellId 术法ID
     */
    void setSlot(int index, String spellId);

    /**
     * 获取当前激活的槽位索引
     * @return 0~8
     */
    int getActiveIndex();

    /**
     * 设置当前激活的槽位索引
     * @param index 0~8
     */
    void setActiveIndex(int index);

    /**
     * 获取当前激活槽位的术法ID
     * @return 术法ID，如果为空返回null
     */
    String getActiveSpellId();

    /**
     * 清空指定槽位
     * @param index 槽位索引（0~8）
     */
    void clearSlot(int index);

    /**
     * 清空所有槽位
     */
    void clearAll();

    /**
     * 从另一个能力复制数据
     * @param other 源能力
     */
    void copyFrom(ISpellHotbar other);
}
