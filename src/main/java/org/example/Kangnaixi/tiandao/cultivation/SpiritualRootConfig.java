package org.example.Kangnaixi.tiandao.cultivation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.level.ServerPlayer;
import org.example.Kangnaixi.tiandao.Tiandao;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * 灵根配置管理类
 * 负责从JSON文件加载灵根配置，并管理玩家的灵根分配
 */
public class SpiritualRootConfig {
    private static SpiritualRootConfig instance;
    private static final String CONFIG_FILE = "spiritual_roots.json";
    private static final String PLAYER_DATA_FILE = "player_spiritual_roots.json";
    
    private Map<String, SpiritualRootConfigJson> rootConfigs;
    private Map<UUID, String> playerRootAssignments;
    private Gson gson;
    
    private SpiritualRootConfig() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.rootConfigs = new HashMap<>();
        this.playerRootAssignments = new HashMap<>();
        loadConfig();
        loadPlayerData();
    }
    
    /**
     * 获取单例实例
     */
    public static SpiritualRootConfig getInstance() {
        if (instance == null) {
            instance = new SpiritualRootConfig();
        }
        return instance;
    }
    
    /**
     * 加载灵根配置
     */
    private void loadConfig() {
        File configDir = new File("config/tiandao");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        
        File configFile = new File(configDir, CONFIG_FILE);
        if (!configFile.exists()) {
            Tiandao.LOGGER.warn("灵根配置文件不存在，使用默认配置: " + configFile.getAbsolutePath());
            createDefaultConfig(configFile);
        }
        
        try (FileReader reader = new FileReader(configFile)) {
            Type type = new TypeToken<Map<String, SpiritualRootConfigJson>>() {}.getType();
            this.rootConfigs = gson.fromJson(reader, type);
            Tiandao.LOGGER.info("成功加载灵根配置文件");
        } catch (IOException e) {
            Tiandao.LOGGER.error("加载灵根配置文件失败: " + e.getMessage());
            createDefaultConfig(configFile);
            loadConfig();
        }
    }
    
    /**
     * 创建默认配置文件
     */
    private void createDefaultConfig(File configFile) {
        Map<String, SpiritualRootConfigJson> defaultConfigs = new HashMap<>();
        
        // 添加基础五行灵根配置
        defaultConfigs.put("none", new SpiritualRootConfigJson("无灵根", "无灵根", "#808080", 0.0f, "没有灵根的凡人，修炼困难"));
        defaultConfigs.put("gold", new SpiritualRootConfigJson("金灵根", "金", "#FFD700", 1.1f, "金属性灵根，擅长锋利与防御"));
        defaultConfigs.put("wood", new SpiritualRootConfigJson("木灵根", "木", "#228B22", 1.05f, "木属性灵根，擅长治疗与生长"));
        defaultConfigs.put("water", new SpiritualRootConfigJson("水灵根", "水", "#1E90FF", 1.0f, "水属性灵根，擅长流动与变化"));
        defaultConfigs.put("fire", new SpiritualRootConfigJson("火灵根", "火", "#FF4500", 1.15f, "火属性灵根，擅长爆发与燃烧"));
        defaultConfigs.put("earth", new SpiritualRootConfigJson("土灵根", "土", "#8B4513", 0.95f, "土属性灵根，擅长稳定与承载"));
        
        this.rootConfigs = defaultConfigs;
        
        try (FileWriter writer = new FileWriter(configFile)) {
            gson.toJson(defaultConfigs, writer);
            Tiandao.LOGGER.info("创建默认灵根配置文件: " + configFile.getAbsolutePath());
        } catch (IOException e) {
            Tiandao.LOGGER.error("创建默认灵根配置文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 加载玩家灵根分配数据
     */
    private void loadPlayerData() {
        File configDir = new File("config/tiandao");
        File playerDataFile = new File(configDir, PLAYER_DATA_FILE);
        
        if (!playerDataFile.exists()) {
            return; // 没有玩家数据文件是正常的
        }
        
        try (FileReader reader = new FileReader(playerDataFile)) {
            Type type = new TypeToken<Map<UUID, String>>() {}.getType();
            this.playerRootAssignments = gson.fromJson(reader, type);
            Tiandao.LOGGER.info("成功加载玩家灵根分配数据");
        } catch (IOException e) {
            Tiandao.LOGGER.error("加载玩家灵根分配数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 保存玩家灵根分配数据
     */
    private void savePlayerData() {
        File configDir = new File("config/tiandao");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        
        File playerDataFile = new File(configDir, PLAYER_DATA_FILE);
        
        try (FileWriter writer = new FileWriter(playerDataFile)) {
            gson.toJson(playerRootAssignments, writer);
        } catch (IOException e) {
            Tiandao.LOGGER.error("保存玩家灵根分配数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 为玩家分配灵根
     * 如果玩家已有灵根，则返回已有灵根
     * 否则随机分配一个灵根
     */
    public SpiritualRootType assignSpiritualRoot(ServerPlayer player) {
        UUID playerUUID = player.getUUID();
        
        // 检查玩家是否已有灵根
        if (playerRootAssignments.containsKey(playerUUID)) {
            String rootId = playerRootAssignments.get(playerUUID);
            SpiritualRootType rootType = SpiritualRootType.fromId(rootId);
            if (rootType != SpiritualRootType.NONE || rootId.equals("none")) {
                return rootType;
            }
        }
        
        // 随机分配灵根
        SpiritualRootType newRoot = SpiritualRootType.getRandomRoot();
        playerRootAssignments.put(playerUUID, newRoot.getId());
        savePlayerData();
        
        Tiandao.LOGGER.info("为玩家 " + player.getName().getString() + " 分配了灵根: " + newRoot.getDisplayName());
        return newRoot;
    }
    
    /**
     * 强制为玩家重新分配灵根
     */
    public SpiritualRootType reassignSpiritualRoot(ServerPlayer player) {
        UUID playerUUID = player.getUUID();
        
        // 随机分配新灵根
        SpiritualRootType newRoot = SpiritualRootType.getRandomRoot();
        playerRootAssignments.put(playerUUID, newRoot.getId());
        savePlayerData();
        
        Tiandao.LOGGER.info("为玩家 " + player.getName().getString() + " 重新分配了灵根: " + newRoot.getDisplayName());
        return newRoot;
    }
    
    /**
     * 获取灵根配置
     */
    public SpiritualRootConfigJson getRootConfig(String rootId) {
        return rootConfigs.get(rootId);
    }
    
    /**
     * 获取灵根恢复加成
     */
    public float getRecoveryBonus(SpiritualRootType rootType) {
        SpiritualRootConfigJson config = getRootConfig(rootType.getId());
        return config != null ? config.getRecoveryBonus() : 0.0f;
    }
    
    /**
     * 获取所有可用的灵根类型
     */
    public SpiritualRootType[] getAllRootTypes() {
        return new SpiritualRootType[] {
            SpiritualRootType.NONE,
            SpiritualRootType.GOLD,
            SpiritualRootType.WOOD,
            SpiritualRootType.WATER,
            SpiritualRootType.FIRE,
            SpiritualRootType.EARTH
        };
    }
    
    /**
     * 灵根配置JSON类
     */
    public static class SpiritualRootConfigJson {
        private String name;
        private String shortName;
        private String color;
        private float recoveryBonus;
        private String description;
        
        public SpiritualRootConfigJson() {
        }
        
        public SpiritualRootConfigJson(String name, String shortName, String color, float recoveryBonus, String description) {
            this.name = name;
            this.shortName = shortName;
            this.color = color;
            this.recoveryBonus = recoveryBonus;
            this.description = description;
        }
        
        public String getName() {
            return name;
        }
        
        public String getShortName() {
            return shortName;
        }
        
        public String getColor() {
            return color;
        }
        
        public float getRecoveryBonus() {
            return recoveryBonus;
        }
        
        public String getDescription() {
            return description;
        }
    }
}