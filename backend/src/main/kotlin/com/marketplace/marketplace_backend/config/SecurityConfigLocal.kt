package com.marketplace.marketplace_backend.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

@Profile("local")
@Configuration
open class SecurityConfigLocal {
    @Bean
    @Order(1)
    open fun localSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .securityMatcher(
                "/api/v1/api-docs/**",
                "/api/v1/swagger-ui/**",
                "/api/v1/swagger-ui.html",
            ).authorizeHttpRequests { it.anyRequest().permitAll() }
            .csrf { it.disable() }

        return http.build()
    }
}
