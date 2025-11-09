package org.example.Kangnaixi.tiandao.practice;

import org.example.Kangnaixi.tiandao.Tiandao;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 修炼方式注册表
 * 管理所有可用的修炼方式
 */
public class PracticeRegistry {
    
    private static final PracticeRegistry INSTANCE = new PracticeRegistry();
    
    private final Map<String, PracticeMethod> practices = new HashMap<>();
    
    private PracticeRegistry() {
        // 私有构造函数，单例模式
    }
    
    /**
     * 获取注册表单例
     */
    public static PracticeRegistry getInstance() {
        return INSTANCE;
    }
    
    /**
     * 注册修炼方式
     * @param practice 修炼方式实例
     */
    public void register(PracticeMethod practice) {
        if (practices.containsKey(practice.getId())) {
            Tiandao.LOGGER.warn("修炼方式已注册，将覆盖: {}", practice.getId());
        }
        practices.put(practice.getId(), practice);
        Tiandao.LOGGER.info("注册修炼方式: {} ({})", practice.getDisplayName(), practice.getId());
    }
    
    /**
     * 根据ID获取修炼方式
     * @param id 修炼方式ID
     * @return 修炼方式实例，如果不存在返回null
     */
    public PracticeMethod getPracticeMethod(String id) {
        return practices.get(id);
    }
    
    /**
     * 获取所有已注册的修炼方式ID
     * @return 修炼方式ID集合
     */
    public Set<String> getAllPracticeIds() {
        return practices.keySet();
    }
    
    /**
     * 检查修炼方式是否已注册
     * @param id 修炼方式ID
     * @return true如果已注册，false否则
     */
    public boolean isRegistered(String id) {
        return practices.containsKey(id);
    }
    
    /**
     * 初始化并注册所有修炼方式
     * 在模组初始化时调用
     */
    public static void init() {
        Tiandao.LOGGER.info("初始化修炼方式注册表...");
        PracticeRegistry registry = getInstance();
        
        // 注册打坐修炼（基础修炼方式）
        registry.register(new MeditationPractice());
        
        // 未来可以在这里注册其他修炼方式：
        // registry.register(new AlchemyPractice());
        // registry.register(new CombatPractice());
        // registry.register(new TechniqueStudyPractice());
        
        Tiandao.LOGGER.info("修炼方式注册完成，共 {} 种", registry.practices.size());
    }
}

