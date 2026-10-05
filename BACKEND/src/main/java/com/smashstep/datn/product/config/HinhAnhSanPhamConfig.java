package com.smashstep.datn.product.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class HinhAnhSanPhamConfig implements WebMvcConfigurer {
    @Value("${smashstep.product-images.directory:uploads/products}")
    private String thuMucAnh;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String viTri = ThuMucAnhSanPham.resolve(thuMucAnh).toUri().toString();
        registry.addResourceHandler("/uploads/products/**")
                .addResourceLocations(viTri.endsWith("/") ? viTri : viTri + "/");
    }
}
