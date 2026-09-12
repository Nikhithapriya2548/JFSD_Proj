package com.omnishop.product.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.*;
import org.springframework.context.annotation.*;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI productServiceApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("OmniShop Product Service API")
                        .description("Catalog microservice: product CRUD, stock queries. Part of the OmniShop e-commerce platform (ACSD30).")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("OmniShop Team")
                                .email("team@omnishop.example")));
    }
}
