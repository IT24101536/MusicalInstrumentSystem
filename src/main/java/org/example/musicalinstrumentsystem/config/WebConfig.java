// WebConfig.java
package org.example.musicalinstrumentsystem.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve uploaded files from /uploads/products/ URL path
        Path uploadDir = Paths.get("uploads/products");
        String uploadPath = uploadDir.toFile().getAbsolutePath();
        
        registry.addResourceHandler("/uploads/products/**")
                .addResourceLocations("file:" + uploadPath + "/");
        
        System.out.println("✅ Static resource mapping configured: /uploads/products/ -> " + uploadPath);
    }
}
