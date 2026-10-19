package com.mdzx.student.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 允许前端开发服务器（Vite 5178 / 预览 4173）跨域调用 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /** 配置允许访问后端接口的前端跨域来源。 */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(
                        "http://127.0.0.1:5178", "http://localhost:5178",
                        "http://127.0.0.1:4173", "http://localhost:4173")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
