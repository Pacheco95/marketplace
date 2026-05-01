package com.marketplace.marketplace_backend.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
open class WebMvcConfig(
    @Value("\${app.cors.allowed-origin:http://localhost:3000}")
    private val allowedOrigin: String,
) : WebMvcConfigurer {
    // allowedOrigin is read from app.cors.allowed-origin — set this to the production
    // frontend domain in production environments; default is localhost for local dev only.
    override fun addCorsMappings(registry: CorsRegistry) {
        registry
            .addMapping("/**")
            .allowedOrigins(allowedOrigin)
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
    }
}
