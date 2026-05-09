package com.ainotebook.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path dir = Path.of(System.getProperty("user.dir"), "uploads");
        String location = dir.toUri().toString();
        registry.addResourceHandler("/auth/uploads/**").addResourceLocations(location);
    }
}

