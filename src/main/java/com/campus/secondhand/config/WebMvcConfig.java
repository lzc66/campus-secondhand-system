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
        // 只公开商品图/头像/演示资源目录;student-cards(学生证影像,含真实姓名学号等 PII)等敏感目录
        // 不挂载到静态资源,只能通过 /api/v1/admin/files/{fileId} 鉴权下载。
        String baseUrl = storageProperties.getPublicBaseUrl().endsWith("/")
                ? storageProperties.getPublicBaseUrl().substring(0, storageProperties.getPublicBaseUrl().length() - 1)
                : storageProperties.getPublicBaseUrl();
        for (String publicFolder : new String[]{"item-images", "avatars", "demo"}) {
            registry.addResourceHandler(baseUrl + "/" + publicFolder + "/**")
                    .addResourceLocations(locationFor(publicFolder));
        }
    }

    private String locationFor(String folder) {
        return Path.of(storageProperties.getRootDir()).resolve(folder).toAbsolutePath().normalize().toUri() + "/";
    }
}