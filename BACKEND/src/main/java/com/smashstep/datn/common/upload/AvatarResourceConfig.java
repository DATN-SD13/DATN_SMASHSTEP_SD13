package com.smashstep.datn.common.upload;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class AvatarResourceConfig implements WebMvcConfigurer {
    private final AvatarUploadService service;
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = service.directory().toUri().toString();
        registry.addResourceHandler("/uploads/avatars/**")
                .addResourceLocations(location.endsWith("/") ? location : location + "/");
    }
}
