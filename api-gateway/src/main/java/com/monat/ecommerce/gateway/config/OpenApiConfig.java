package com.monat.ecommerce.gateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI apiGatewayOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Monat API Gateway API")
                .version("v1")
                .description("OpenAPI definition for the Monat API Gateway."));
    }
}
