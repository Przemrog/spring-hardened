package com.example.notes.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // udostepnienie wgranych awatarow spod /avatars/**
        registry.addResourceHandler("/avatars/**")
                .addResourceLocations("file:uploads/");
    }
}
