package com.omnishop.order.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.*;
import org.springframework.context.annotation.*;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI orderServiceApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("OmniShop Order Service API")
                        .description("Order microservice: placement with live price/stock validation against product-service. Part of the OmniShop e-commerce platform (ACSD30).")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("OmniShop Team")
                                .email("team@omnishop.example")));
    }
}
