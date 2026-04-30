package com.marketplace.marketplace_backend.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class OpenApiConfig {
    @Bean
    open fun openAPI(): OpenAPI {
        val cookieScheme =
            SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .`in`(SecurityScheme.In.COOKIE)
                .name("marketplace_access_token")

        return OpenAPI()
            .info(
                Info()
                    .title("Marketplace API")
                    .version("1.0.0")
                    .description("Marketplace platform REST API"),
            ).components(
                Components().addSecuritySchemes("cookieAuth", cookieScheme),
            ).addSecurityItem(SecurityRequirement().addList("cookieAuth"))
    }
}
