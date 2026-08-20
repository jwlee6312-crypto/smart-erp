package com.crmbank.erp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${STORAGE_PATH:D:/erp.crmbank.co.kr/storage}")
    private String storagePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String fixedPath = storagePath.replace("\\", "/");
        if (!fixedPath.endsWith("/")) fixedPath += "/";
        
        // 🚀 OS에 상관없이 동작하도록 프로토콜 처리
        String location;
        if (fixedPath.startsWith("/")) {
            // Linux/Ubuntu 환경
            location = "file:" + fixedPath; 
        } else {
            // Windows 환경 (D:/... 등)
            location = "file:///" + fixedPath;
        }
        
        registry.addResourceHandler("/Upload_Images/**")
                .addResourceLocations(location)
                .setCachePeriod(3600);
    }
}
