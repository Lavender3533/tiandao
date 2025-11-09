package org.example.Kangnaixi.tiandao.technique;

import com.google.gson.*;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.cultivation.CultivationRealm;
import org.example.Kangnaixi.tiandao.cultivation.SpiritualRootType;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 功法注册表
 * 从JSON文件加载和管理所有可用的功法模板
 */
public class TechniqueRegistry {
    
    private static TechniqueRegistry instance;
    private final Map<String, TechniqueData> techniques = new LinkedHashMap<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_DIR = "config/tiandao";
    private static final String TECHNIQUE_FILE = "techniques.json";
    
    private TechniqueRegistry() {
        loadFromJson();
    }
    
    public static TechniqueRegistry getInstance() {
        if (instance == null) {
            instance = new TechniqueRegistry();
        }
        return instance;
    }
    
    /**
     * 从JSON文件加载功法
     */
    private void loadFromJson() {
        try {
            Path configPath = Paths.get(CONFIG_DIR, TECHNIQUE_FILE);
            File configFile = configPath.toFile();
            
            // 如果配置文件不存在，创建默认配置
            if (!configFile.exists()) {
                Tiandao.LOGGER.info("功法配置文件不存在，创建默认配置: {}", configFile.getAbsolutePath());
                createDefaultJsonConfig(configFile);
            }
            
            // 读取JSON文件
            try (Reader reader = new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                
                if (!root.has("techniques")) {
                    Tiandao.LOGGER.warn("JSON配置文件格式错误，使用默认配置");
                    createDefaultJsonConfig(configFile);
                    return;
                }
                
                JsonArray techniqueArray = root.getAsJsonArray("techniques");
                
                for (JsonElement element : techniqueArray) {
                    try {
                        JsonObject techniqueObj = element.getAsJsonObject();
                        
                        String id = techniqueObj.get("id").getAsString();
                        String name = techniqueObj.get("name").getAsString();
                        String description = techniqueObj.get("description").getAsString();
                        String requiredRootStr = techniqueObj.get("requiredRoot").getAsString();
                        String requiredRealmStr = techniqueObj.get("requiredRealm").getAsString();
                        int requiredLevel = techniqueObj.get("requiredLevel").getAsInt();
                        
                        SpiritualRootType requiredRoot = SpiritualRootType.valueOf(requiredRootStr);
                        CultivationRealm requiredRealm = CultivationRealm.valueOf(requiredRealmStr);
                        
                        TechniqueData technique = new TechniqueData(
                            id, name, description,
                            requiredRoot, requiredRealm, requiredLevel
                        );
                        
                        registerTechnique(technique);
                        
                    } catch (Exception e) {
                        Tiandao.LOGGER.error("解析功法数据时出错: {}", element, e);
                    }
                }
                
                Tiandao.LOGGER.info("功法注册表加载完成，共加载 {} 门功法", techniques.size());
                
            }
        } catch (Exception e) {
            Tiandao.LOGGER.error("加载功法配置文件时出错", e);
        }
    }
    
    /**
     * 创建默认的JSON配置文件
     */
    private void createDefaultJsonConfig(File configFile) {
        try {
            // 确保目录存在
            configFile.getParentFile().mkdirs();
            
            // 构建默认配置
            JsonObject root = new JsonObject();
            root.addProperty("_comment1", "天道修仙 - 功法配置文件");
            root.addProperty("_comment2", "功法等级系统: 可从Lv.1升到Lv.10, Lv.1时效率100%, Lv.10时效率200%");
            root.addProperty("_comment3", "打坐修炼时每秒获得2点功法经验，被动恢复时每秒获得1点功法经验");
            
            JsonArray techniques = new JsonArray();
            
            // 测试功法 - 无灵根要求
            techniques.add(createTechniqueJson(
                "basic_qi_refinement",
                "基础炼气诀",
                "最基础的修炼功法，任何灵根都可以修炼。适合初学者入门。",
                "NONE",
                "MORTAL",
                1
            ));
            
            // 金系功法
            techniques.add(createTechniqueJson(
                "golden_core_sutra",
                "金丹真经",
                "金系基础功法，擅长攻击与防御。修炼此功法可提升金属性灵力的掌控能力。",
                "GOLD",
                "QI_CONDENSATION",
                1
            ));
            
            // 木系功法
            techniques.add(createTechniqueJson(
                "evergreen_scripture",
                "长青经",
                "木系基础功法，擅长治疗与生命力。修炼此功法可提升木属性灵力的掌控能力。",
                "WOOD",
                "QI_CONDENSATION",
                1
            ));
            
            // 水系功法
            techniques.add(createTechniqueJson(
                "flowing_water_art",
                "流水诀",
                "水系基础功法，擅长灵活与变化。修炼此功法可提升水属性灵力的掌控能力。",
                "WATER",
                "QI_CONDENSATION",
                1
            ));
            
            // 火系功法
            techniques.add(createTechniqueJson(
                "blazing_sun_manual",
                "烈阳真诀",
                "火系基础功法，擅长爆发与攻击。修炼此功法可提升火属性灵力的掌控能力。",
                "FIRE",
                "QI_CONDENSATION",
                1
            ));
            
            // 土系功法
            techniques.add(createTechniqueJson(
                "earth_foundation_technique",
                "厚土功",
                "土系基础功法，擅长防御与稳固。修炼此功法可提升土属性灵力的掌控能力。",
                "EARTH",
                "QI_CONDENSATION",
                1
            ));
            
            root.add("techniques", techniques);
            
            // 写入文件
            String jsonContent = gson.toJson(root);
            Files.write(configFile.toPath(), jsonContent.getBytes(StandardCharsets.UTF_8));
            
            Tiandao.LOGGER.info("已创建默认功法配置文件: {}", configFile.getAbsolutePath());
            
        } catch (Exception e) {
            Tiandao.LOGGER.error("创建默认功法配置文件时出错", e);
        }
    }
    
    /**
     * 创建功法JSON对象
     */
    private JsonObject createTechniqueJson(String id, String name, String description,
                                           String requiredRoot, String requiredRealm, int requiredLevel) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", id);
        obj.addProperty("name", name);
        obj.addProperty("description", description);
        obj.addProperty("requiredRoot", requiredRoot);
        obj.addProperty("requiredRealm", requiredRealm);
        obj.addProperty("requiredLevel", requiredLevel);
        return obj;
    }
    
    /**
     * 注册功法模板
     */
    public void registerTechnique(TechniqueData technique) {
        if (techniques.containsKey(technique.getId())) {
            Tiandao.LOGGER.warn("功法 {} 已存在，将被覆盖", technique.getId());
        }
        techniques.put(technique.getId(), technique);
        Tiandao.LOGGER.debug("注册功法: {} ({})", technique.getName(), technique.getId());
    }
    
    /**
     * 根据ID获取功法模板（创建新副本）
     */
    public TechniqueData getTechniqueById(String id) {
        TechniqueData template = techniques.get(id);
        if (template == null) {
            return null;
        }
        // 返回副本，避免修改模板
        return new TechniqueData(template);
    }
    
    /**
     * 获取所有功法ID
     */
    public Set<String> getAllTechniqueIds() {
        return new LinkedHashSet<>(techniques.keySet());
    }
    
    /**
     * 获取所有功法模板（只读）
     */
    public Collection<TechniqueData> getAllTechniques() {
        return Collections.unmodifiableCollection(techniques.values());
    }
    
    /**
     * 检查功法是否存在
     */
    public boolean hasTechnique(String id) {
        return techniques.containsKey(id);
    }
    
    /**
     * 根据灵根类型获取适配的功法
     */
    public List<TechniqueData> getTechniquesByRoot(SpiritualRootType rootType) {
        List<TechniqueData> result = new ArrayList<>();
        for (TechniqueData technique : techniques.values()) {
            if (technique.getRequiredRoot() == rootType) {
                result.add(new TechniqueData(technique));
            }
        }
        return result;
    }
    
    /**
     * 获取功法数量
     */
    public int getTechniqueCount() {
        return techniques.size();
    }
    
    /**
     * 清空注册表（用于重载）
     */
    public void clear() {
        techniques.clear();
        Tiandao.LOGGER.info("功法注册表已清空");
    }
    
    /**
     * 重新加载功法
     */
    public void reload() {
        clear();
        loadFromJson();
        Tiandao.LOGGER.info("功法注册表已重新加载");
    }
}
