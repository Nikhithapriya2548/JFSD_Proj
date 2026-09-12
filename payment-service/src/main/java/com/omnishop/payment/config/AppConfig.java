package com.omnishop.payment.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.*;
import org.springframework.context.annotation.*;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class AppConfig implements WebMvcConfigurer {

    @Bean
    public OpenAPI paymentServiceApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("OmniShop Payment Service API")
                        .description("SIMULATED payment gateway for demo purposes only — no real money moves. Part of the OmniShop e-commerce platform (ACSD30).")
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
