package com.initprep.auth.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI authServiceOpenAPI() {

        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
            .info(new Info()
                .title("InitPrep Auth Service API")
                .version("1.0.0")
                .description("Authentication APIs for InitPrep")
                .contact(new Contact().name("Lalit Mohan Mishra"))
                .license(new License().name("MIT")))

            .addSecurityItem(
                new SecurityRequirement().addList(securitySchemeName)
            )

            .components(
                new Components()
                    .addSecuritySchemes(
                        securitySchemeName,
                        new SecurityScheme()
                            .name(securitySchemeName)
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                    )
            );
    }
}
