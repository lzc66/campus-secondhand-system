package com.campus.secondhand.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class WebMvcConfig implements WebMvcConfigurer {

    private final StorageProperties storageProperties;

    public WebMvcConfig(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String publicBaseUrl = storageProperties.getPublicBaseUrl();
        String pattern = publicBaseUrl.endsWith("/") ? publicBaseUrl + "**" : publicBaseUrl + "/**";
        String location = Path.of(storageProperties.getRootDir()).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler(pattern)
                .addResourceLocations(location);
    }
}