package org.example.musicalinstrumentsystem.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Singleton Pattern Implementation - Application Configuration Manager
 * 
 * This class manages application-wide configuration settings that should exist
 * as a single instance throughout the application lifecycle.
 * 
 * Use Cases:
 * - Runtime configuration cache
 * - Feature flags
 * - System-wide settings
 * - Performance metrics tracking
 */
public class AppConfigManager {
    
    // The single instance (eagerly initialized)
    private static final AppConfigManager INSTANCE = new AppConfigManager();
    
    // Configuration storage
    private final Map<String, Object> configurations;
    
    // Private constructor prevents instantiation from outside
    private AppConfigManager() {
        configurations = new HashMap<>();
        initializeDefaultConfigurations();
        System.out.println("✅ AppConfigManager Singleton initialized");
    }
    
    /**
     * Get the singleton instance
     */
    public static AppConfigManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * Initialize default configurations
     */
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
    
    /**
     * Get configuration value
     */
    public Object getConfig(String key) {
        return configurations.get(key);
    }
    
    /**
     * Get configuration as Integer
     */
    public Integer getIntConfig(String key) {
        Object value = configurations.get(key);
        return value instanceof Integer ? (Integer) value : null;
    }
    
    /**
     * Get configuration as Boolean
     */
    public Boolean getBooleanConfig(String key) {
        Object value = configurations.get(key);
        return value instanceof Boolean ? (Boolean) value : null;
    }
    
    /**
     * Get configuration as String
     */
    public String getStringConfig(String key) {
        Object value = configurations.get(key);
        return value != null ? value.toString() : null;
    }
    
    /**
     * Set configuration value (runtime)
     */
    public void setConfig(String key, Object value) {
        configurations.put(key, value);
        System.out.println("📝 Configuration updated: " + key + " = " + value);
    }
    
    /**
     * Check if configuration exists
     */
    public boolean hasConfig(String key) {
        return configurations.containsKey(key);
    }
    
    /**
     * Get all configurations (read-only)
     */
    public Map<String, Object> getAllConfigurations() {
        return new HashMap<>(configurations);
    }
    
    /**
     * Get low stock threshold
     */
    public int getLowStockThreshold() {
        return getIntConfig("lowStockThreshold");
    }
    
    /**
     * Check if email notifications are enabled
     */
    public boolean isEmailNotificationsEnabled() {
        Boolean enabled = getBooleanConfig("emailNotificationsEnabled");
        return enabled != null && enabled;
    }
    
    /**
     * Check if payment method is enabled
     */
    public boolean isPaymentMethodEnabled(String method) {
        String key = "feature" + method + "Enabled";
        Boolean enabled = getBooleanConfig(key);
        return enabled != null && enabled;
    }
    
    /**
     * Print all configurations (for debugging)
     */
    public void printConfigurations() {
        System.out.println("=== APP CONFIGURATION MANAGER ===");
        configurations.forEach((key, value) -> 
            System.out.println("  " + key + " = " + value)
        );
        System.out.println("================================");
    }
}
