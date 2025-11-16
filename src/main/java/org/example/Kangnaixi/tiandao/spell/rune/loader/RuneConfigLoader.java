package org.example.Kangnaixi.tiandao.spell.rune.loader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.spell.rune.Rune;
import org.example.Kangnaixi.tiandao.spell.rune.RuneContext;
import org.example.Kangnaixi.tiandao.spell.rune.RuneRegistry;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 从JSON加载符文配置
 */
public class RuneConfigLoader {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * 从JSON文件加载所有符文
     */
    public void loadFromJson(String resourcePath) {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                Tiandao.LOGGER.error("找不到符文配置文件: {}", resourcePath);
                return;
            }

            JsonObject root = GSON.fromJson(
                new InputStreamReader(stream, StandardCharsets.UTF_8),
                JsonObject.class
            );

            JsonArray runesArray = root.getAsJsonArray("runes");
            if (runesArray == null) {
                Tiandao.LOGGER.error("JSON中没有找到runes数组");
                return;
            }

            int loadedCount = 0;
            for (int i = 0; i < runesArray.size(); i++) {
                JsonObject runeObj = runesArray.get(i).getAsJsonObject();
                try {
                    Rune rune = parseRune(runeObj);
                    RuneRegistry.getInstance().registerRune(rune);
                    loadedCount++;
                } catch (Exception e) {
                    Tiandao.LOGGER.error("解析符文失败: {}", runeObj, e);
                }
            }

            Tiandao.LOGGER.info("成功加载 {} 个符文", loadedCount);

        } catch (Exception e) {
            Tiandao.LOGGER.error("加载符文配置失败: {}", resourcePath, e);
        }
    }

    /**
     * 解析单个符文
     */
    private Rune parseRune(JsonObject json) {
        String id = json.get("id").getAsString();
        String name = json.get("name").getAsString();
        Rune.RuneTier tier = Rune.RuneTier.valueOf(json.get("tier").getAsString());
        Rune.RuneCategory category = Rune.RuneCategory.valueOf(json.get("category").getAsString());
        String description = json.get("description").getAsString();
        double spiritCost = json.get("spiritCost").getAsDouble();
        double cooldown = json.get("cooldown").getAsDouble();
        String unlockRealm = json.get("unlockRealm").getAsString();
        int unlockLevel = json.get("unlockLevel").getAsInt();
        String colorStr = json.get("color").getAsString();
        int color = parseColor(colorStr);
        int inputs = json.get("inputs").getAsInt();
        int outputs = json.get("outputs").getAsInt();

        // 解析参数
        Map<String, Object> parameters = new HashMap<>();
        if (json.has("parameters")) {
            JsonObject paramsObj = json.getAsJsonObject("parameters");
            for (String key : paramsObj.keySet()) {
                if (paramsObj.get(key).isJsonPrimitive()) {
                    if (paramsObj.get(key).getAsJsonPrimitive().isNumber()) {
                        parameters.put(key, paramsObj.get(key).getAsDouble());
                    } else {
                        parameters.put(key, paramsObj.get(key).getAsString());
                    }
                }
            }
        }

        // 创建符文实例
        return new ConfigurableRune(
            id, name, tier, category, description,
            spiritCost, cooldown, unlockRealm, unlockLevel,
            color, inputs, outputs, parameters
        );
    }

    /**
     * 解析颜色字符串 (#RRGGBB)
     */
    private int parseColor(String colorStr) {
        if (colorStr.startsWith("#")) {
            colorStr = colorStr.substring(1);
        }
        try {
            return (int) Long.parseLong(colorStr, 16) | 0xFF000000;
        } catch (NumberFormatException e) {
            Tiandao.LOGGER.warn("无法解析颜色: {}, 使用默认颜色", colorStr);
            return 0xFFFFFFFF;
        }
    }

    /**
     * 可配置符文 - 从JSON创建的符文
     */
    private static class ConfigurableRune extends Rune {
        private final Map<String, Object> parameters;

        public ConfigurableRune(String id, String name, RuneTier tier, RuneCategory category,
                                String description, double spiritCost, double cooldown,
                                String unlockRealm, int unlockLevel, int color,
                                int inputs, int outputs, Map<String, Object> parameters) {
            super(new Builder(id, name)
                .tier(tier)
                .category(category)
                .description(description)
                .spiritCost(spiritCost)
                .cooldown(cooldown)
                .unlockRealm(unlockRealm)
                .unlockLevel(unlockLevel)
                .color(color)
                .inputs(inputs)
                .outputs(outputs)
            );
            this.parameters = parameters;
        }

        @Override
        public void execute(RuneContext context) {
            // 基础执行逻辑 - 将参数存储到上下文
            parameters.forEach((key, value) -> {
                context.setVariable(getId() + "_" + key, value);
            });

            // 根据分类执行不同逻辑
            switch (getCategory()) {
                case TRIGGER -> executeTrigger(context);
                case SHAPE -> executeShape(context);
                case EFFECT -> executeEffect(context);
            }
        }

        private void executeTrigger(RuneContext context) {
            // 触发符文的基础逻辑
            switch (getId()) {
                case "self" -> {
                    context.setPosition(context.getCaster().position());
                    context.setDirection(context.getCaster().getLookAngle());
                }
                case "touch" -> {
                    // 触摸逻辑 - 前方3格范围检测
                    net.minecraft.world.phys.Vec3 start = context.getCaster().getEyePosition();
                    net.minecraft.world.phys.Vec3 end = start.add(context.getCaster().getLookAngle().scale(3.0));
                    net.minecraft.world.level.ClipContext clipContext = new net.minecraft.world.level.ClipContext(
                        start, end,
                        net.minecraft.world.level.ClipContext.Block.COLLIDER,
                        net.minecraft.world.level.ClipContext.Fluid.NONE,
                        context.getCaster()
                    );
                    net.minecraft.world.phys.BlockHitResult hit = context.getLevel().clip(clipContext);
                    // 简化版：直接设置位置和方向
                    context.setPosition(context.getCaster().position());
                    context.setDirection(context.getCaster().getLookAngle());
                }
                case "projectile" -> {
                    // 弹道逻辑 - 发射实体
                    org.example.Kangnaixi.tiandao.spell.entity.SpellProjectileEntity projectile =
                        new org.example.Kangnaixi.tiandao.spell.entity.SpellProjectileEntity(
                            org.example.Kangnaixi.tiandao.spell.entity.ModEntityTypes.SPELL_PROJECTILE.get(),
                            context.getLevel()
                        );

                    // 设置位置和方向
                    projectile.setPos(context.getCaster().getEyePosition());
                    projectile.setOwner(context.getCaster());

                    // 获取速度参数
                    double speed = 1.5;
                    if (parameters.containsKey("speed")) {
                        Object speedObj = parameters.get("speed");
                        if (speedObj instanceof Number) {
                            speed = ((Number) speedObj).doubleValue();
                        }
                    }

                    // 设置速度
                    net.minecraft.world.phys.Vec3 velocity = context.getCaster().getLookAngle().scale(speed);
                    projectile.setDeltaMovement(velocity);

                    // 将符文链传递给弹道实体（需要从context获取完整符文链）
                    // 注意：这里需要外部设置，因为ConfigurableRune不知道完整的符文链
                    context.setVariable("projectile_entity", projectile);

                    // 生成实体
                    context.getLevel().addFreshEntity(projectile);

                    // 标记为弹道模式，后续符文不再执行
                    context.setVariable("is_projectile", true);
                }
            }
        }

        private void executeShape(RuneContext context) {
            // 形状符文的基础逻辑
            // 将在后续实现具体的范围检测
        }

        private void executeEffect(RuneContext context) {
            // 效果符文调用对应的效果执行器
            String effectId = getId();
            org.example.Kangnaixi.tiandao.spell.effect.EffectExecutor effect =
                org.example.Kangnaixi.tiandao.spell.effect.EffectRegistry.getInstance().getEffect(effectId);

            if (effect != null) {
                // 获取威力参数
                double power = 1.0;
                Object powerParam = parameters.get("damage");
                if (powerParam == null) powerParam = parameters.get("healing");
                if (powerParam == null) powerParam = parameters.get("force");

                if (powerParam instanceof Number) {
                    power = ((Number) powerParam).doubleValue();
                }

                // 执行效果
                effect.execute(context, power);
            } else {
                Tiandao.LOGGER.warn("找不到效果执行器: {}", effectId);
            }
        }

        public Map<String, Object> getParameters() {
            return parameters;
        }
    }
}
