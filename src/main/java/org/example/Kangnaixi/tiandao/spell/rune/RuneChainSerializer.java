package org.example.Kangnaixi.tiandao.spell.rune;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import org.example.Kangnaixi.tiandao.Tiandao;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 符文链序列化工具
 * 用于将符文链保存到NBT和从NBT恢复
 */
public class RuneChainSerializer {

    /**
     * 将符文链序列化到NBT
     */
    public static CompoundTag serialize(List<Rune> runeChain) {
        return serialize(runeChain, new HashMap<>());
    }

    /**
     * 将符文链和参数序列化到NBT
     * @param runeChain 符文链
     * @param parameters 符文参数（例如半径、距离等）
     */
    public static CompoundTag serialize(List<Rune> runeChain, Map<String, Map<String, Double>> parameters) {
        CompoundTag tag = new CompoundTag();

        // 保存符文ID列表
        ListTag runeList = new ListTag();
        for (Rune rune : runeChain) {
            runeList.add(StringTag.valueOf(rune.getId()));
        }
        tag.put("runes", runeList);

        // 保存符文参数
        CompoundTag paramsTag = new CompoundTag();
        for (Map.Entry<String, Map<String, Double>> entry : parameters.entrySet()) {
            String runeId = entry.getKey();
            Map<String, Double> runeParams = entry.getValue();

            CompoundTag runeParamTag = new CompoundTag();
            for (Map.Entry<String, Double> param : runeParams.entrySet()) {
                runeParamTag.putDouble(param.getKey(), param.getValue());
            }
            paramsTag.put(runeId, runeParamTag);
        }
        tag.put("parameters", paramsTag);

        // 保存元数据
        tag.putLong("created_time", System.currentTimeMillis());
        tag.putInt("version", 1);

        return tag;
    }

    /**
     * 从NBT反序列化符文链
     */
    @Nullable
    public static List<Rune> deserialize(CompoundTag tag) {
        if (!tag.contains("runes")) {
            Tiandao.LOGGER.warn("NBT中没有符文数据");
            return null;
        }

        ListTag runeList = tag.getList("runes", 8); // 8 = STRING
        if (runeList.isEmpty()) {
            return new ArrayList<>();
        }

        List<Rune> runeChain = new ArrayList<>();
        RuneRegistry registry = RuneRegistry.getInstance();

        for (int i = 0; i < runeList.size(); i++) {
            String runeId = runeList.getString(i);
            Rune rune = registry.getRuneById(runeId);

            if (rune != null) {
                runeChain.add(rune);
            } else {
                Tiandao.LOGGER.warn("找不到符文: {}", runeId);
            }
        }

        return runeChain;
    }

    /**
     * 从NBT反序列化符文链和参数
     */
    public static DeserializedData deserializeWithParameters(CompoundTag tag) {
        List<Rune> runeChain = deserialize(tag);
        if (runeChain == null) {
            return new DeserializedData(new ArrayList<>(), new HashMap<>());
        }

        // 读取参数
        Map<String, Map<String, Double>> parameters = new HashMap<>();
        if (tag.contains("parameters")) {
            CompoundTag paramsTag = tag.getCompound("parameters");

            for (String runeId : paramsTag.getAllKeys()) {
                CompoundTag runeParamTag = paramsTag.getCompound(runeId);
                Map<String, Double> runeParams = new HashMap<>();

                for (String paramKey : runeParamTag.getAllKeys()) {
                    runeParams.put(paramKey, runeParamTag.getDouble(paramKey));
                }

                parameters.put(runeId, runeParams);
            }
        }

        return new DeserializedData(runeChain, parameters);
    }

    /**
     * 验证NBT数据是否有效
     */
    public static boolean isValid(CompoundTag tag) {
        if (!tag.contains("runes")) {
            return false;
        }

        if (!tag.contains("version")) {
            return false;
        }

        int version = tag.getInt("version");
        if (version != 1) {
            Tiandao.LOGGER.warn("不支持的符文链版本: {}", version);
            return false;
        }

        return true;
    }

    /**
     * 获取符文链的显示信息
     */
    public static String getDisplayInfo(CompoundTag tag) {
        if (!isValid(tag)) {
            return "无效的符文链";
        }

        List<Rune> runeChain = deserialize(tag);
        if (runeChain == null || runeChain.isEmpty()) {
            return "空符文链";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < runeChain.size(); i++) {
            if (i > 0) sb.append(" → ");
            sb.append(runeChain.get(i).getName());
        }

        return sb.toString();
    }

    /**
     * 计算符文链的总灵力消耗
     */
    public static double calculateTotalCost(List<Rune> runeChain) {
        return runeChain.stream()
            .mapToDouble(Rune::getSpiritCost)
            .sum();
    }

    /**
     * 反序列化结果
     */
    public static class DeserializedData {
        private final List<Rune> runeChain;
        private final Map<String, Map<String, Double>> parameters;

        public DeserializedData(List<Rune> runeChain, Map<String, Map<String, Double>> parameters) {
            this.runeChain = runeChain;
            this.parameters = parameters;
        }

        public List<Rune> getRuneChain() {
            return runeChain;
        }

        public Map<String, Map<String, Double>> getParameters() {
            return parameters;
        }

        /**
         * 获取指定符文的参数
         */
        @Nullable
        public Map<String, Double> getRuneParameters(String runeId) {
            return parameters.get(runeId);
        }

        /**
         * 获取指定符文的指定参数
         */
        @Nullable
        public Double getParameter(String runeId, String paramKey) {
            Map<String, Double> runeParams = parameters.get(runeId);
            if (runeParams == null) {
                return null;
            }
            return runeParams.get(paramKey);
        }
    }
}
