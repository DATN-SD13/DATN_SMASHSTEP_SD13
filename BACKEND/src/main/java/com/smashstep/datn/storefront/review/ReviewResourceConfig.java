package com.smashstep.datn.storefront.review;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class ReviewResourceConfig implements WebMvcConfigurer {
    private final ReviewUploadService service;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = service.directory().toUri().toString();
        registry.addResourceHandler("/uploads/reviews/**")
                .addResourceLocations(location.endsWith("/") ? location : location + "/");
    }
}
