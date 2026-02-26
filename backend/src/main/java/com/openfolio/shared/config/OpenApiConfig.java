package com.openfolio.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Bean
    public OpenAPI openFolioOpenAPI() {
        final String bearerAuth = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("OpenFolio API")
                        .description("""
                                REST API for OpenFolio — a full-stack developer portfolio platform.
                                
                                **Features:** GitHub-powered portfolios, AI-enhanced resumes, PDF export, \
                                public portfolio publishing, and a resume builder.
                                
                                **Authentication:** Most endpoints require a JWT Bearer token obtained via \
                                `/api/v1/auth/login` or `/api/v1/auth/oauth/{provider}`. \
                                Public endpoints under `/api/v1/public/**` and `/api/v1/auth/**` do not require authentication.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("OpenFolio")
                                .url("https://github.com/BRMilev22/OpenFolio"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url(baseUrl).description("Current server")))
                .addSecurityItem(new SecurityRequirement().addList(bearerAuth))
                .components(new Components()
                        .addSecuritySchemes(bearerAuth, new SecurityScheme()
                                .name(bearerAuth)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT access token — obtain via login or OAuth")));
    }
}
