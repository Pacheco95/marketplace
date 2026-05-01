package com.marketplace.marketplace_backend.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
open class SecurityConfig(
    private val cookieTokenFilter: CookieTokenFilter,
    @Value("\${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private val issuerUri: String,
) {
    @Bean
    open fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .addFilterBefore(cookieTokenFilter, BearerTokenAuthenticationFilter::class.java)
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(
                        "/api/v1/auth/login",
                        "/api/v1/auth/callback",
                        "/api/v1/auth/one-tap",
                        "/api/v1/auth/refresh",
                        "/api/v1/auth/logout",
                        "/api/v1/api-docs/**",
                        "/api/v1/swagger-ui/**",
                        "/api/v1/swagger-ui.html",
                    ).permitAll()
                    .requestMatchers("/api/v1/**")
                    .authenticated()
                    .anyRequest()
                    .permitAll()
            }.oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.jwkSetUri("$issuerUri/protocol/openid-connect/certs")
                }
            }

        return http.build()
    }
}
