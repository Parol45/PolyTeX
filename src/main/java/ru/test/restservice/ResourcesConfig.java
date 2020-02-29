package ru.test.restservice;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.TimeUnit;

// Добавление папок с ресурсами и отключение кэширования (потом мб поменять)
@Configuration
public class ResourcesConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/test/**").addResourceLocations("file:test/").setCacheControl(CacheControl.maxAge(0, TimeUnit.HOURS));
        registry.addResourceHandler("src/main/resources/static/**").addResourceLocations("file:src/main/resources/static//").setCacheControl(CacheControl.maxAge(0, TimeUnit.HOURS));
    }
}