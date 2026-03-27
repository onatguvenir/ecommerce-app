package com.monat.ecommerce.common.config;

import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Global OpenAPI configuration to add common headers like Accept-Language.
 * This will be automatically applied to all services scanning com.monat.ecommerce.common.
 */
@Configuration
public class GlobalOpenApiConfig {

    @Bean
    public OpenApiCustomizer localizationHeaderCustomizer() {
        return openApi -> {
            if (openApi.getPaths() != null) {
                openApi.getPaths().values().forEach(pathItem -> 
                    pathItem.readOperations().forEach(operation -> 
                        operation.addParametersItem(new HeaderParameter()
                            .name("Accept-Language")
                            .description("Language preference for the response messages (e.g. 'en', 'tr')")
                            .schema(new StringSchema()
                                ._default("en")
                                .addEnumItem("en")
                                .addEnumItem("tr"))
                            .required(false))
                    )
                );
            }
        };
    }
}
