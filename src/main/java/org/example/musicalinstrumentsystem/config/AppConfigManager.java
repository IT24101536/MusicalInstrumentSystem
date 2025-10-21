package org.example.musicalinstrumentsystem.config;

import java.util.HashMap;
import java.util.Map;


public class AppConfigManager {
    
    // The single instance (eagerly initialized)
    private static final AppConfigManager INSTANCE = new AppConfigManager();
    
    // Configuration storage
    private final Map<String, Object> configurations;
    
    // Private constructor prevents instantiation from outside
    private AppConfigManager() {
        configurations = new HashMap<>();
        initializeDefaultConfigurations();
        System.out.println(" AppConfigManager Singleton initialized");
    }
    

    public static AppConfigManager getInstance() {
        return INSTANCE;
    }
    

    private void initializeDefaultConfigurations() {
        // Low stock threshold
        configurations.put("lowStockThreshold", 5);
        
        // Max cart items
        configurations.put("maxCartItems", 50);
        
        // Product image max size (MB)
        configurations.put("maxImageSizeMB", 5);
        
        // Session timeout (minutes)
        configurations.put("sessionTimeoutMinutes", 30);
        
        // Email notifications enabled
        configurations.put("emailNotificationsEnabled", true);
        
        // Feature flags
        configurations.put("featurePayPalEnabled", true);
        configurations.put("featureStripeEnabled", true);
        configurations.put("featureCreditCardEnabled", true);
    }
    

    public Object getConfig(String key) {
        return configurations.get(key);
    }
    

    public Integer getIntConfig(String key) {
        Object value = configurations.get(key);
        return value instanceof Integer ? (Integer) value : null;
    }


    public Boolean getBooleanConfig(String key) {
        Object value = configurations.get(key);
        return value instanceof Boolean ? (Boolean) value : null;
    }
    

    public String getStringConfig(String key) {
        Object value = configurations.get(key);
        return value != null ? value.toString() : null;
    }
    

    public void setConfig(String key, Object value) {
        configurations.put(key, value);
        System.out.println("📝 Configuration updated: " + key + " = " + value);
    }
    

    public boolean hasConfig(String key) {
        return configurations.containsKey(key);
    }
    

    public Map<String, Object> getAllConfigurations() {
        return new HashMap<>(configurations);
    }
    

    public int getLowStockThreshold() {
        return getIntConfig("lowStockThreshold");
    }
    

    public boolean isEmailNotificationsEnabled() {
        Boolean enabled = getBooleanConfig("emailNotificationsEnabled");
        return enabled != null && enabled;
    }

    public boolean isPaymentMethodEnabled(String method) {
        String key = "feature" + method + "Enabled";
        Boolean enabled = getBooleanConfig(key);
        return enabled != null && enabled;
    }

    public void printConfigurations() {
        System.out.println("=== APP CONFIGURATION MANAGER ===");
        configurations.forEach((key, value) -> 
            System.out.println("  " + key + " = " + value)
        );
        System.out.println("================================");
    }
}
