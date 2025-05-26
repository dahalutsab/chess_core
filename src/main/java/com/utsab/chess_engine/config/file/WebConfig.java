package com.utsab.chess_engine.config.file;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final FileConfig fileConfig;

    public WebConfig(FileConfig fileConfig) {
        this.fileConfig = fileConfig;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String filePath = "file:" + fileConfig.getFilePath();
        registry.addResourceHandler("/files/**")
                .addResourceLocations(filePath)
                .setCachePeriod(0); // No caching for development
    }
}