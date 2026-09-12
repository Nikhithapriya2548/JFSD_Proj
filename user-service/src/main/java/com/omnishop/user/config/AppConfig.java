package com.omnishop.user.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.*;
import org.springframework.context.annotation.*;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class AppConfig implements WebMvcConfigurer {

    @Bean
    public OpenAPI userServiceApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("OmniShop User Service API")
                        .description("Registration, BCrypt-hashed credentials, JWT login. Part of the OmniShop e-commerce platform (ACSD30).")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("OmniShop Team")
                                .email("team@omnishop.example")));
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
