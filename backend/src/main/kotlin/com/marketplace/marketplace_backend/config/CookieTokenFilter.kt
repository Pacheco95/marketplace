package com.marketplace.marketplace_backend.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class CookieTokenFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = request.cookies?.firstOrNull { it.name == "marketplace_access_token" }?.value

        if (token != null) {
            val wrapped =
                object : HttpServletRequestWrapper(request) {
                    override fun getHeader(name: String): String? =
                        if (name.equals("Authorization", ignoreCase = true)) {
                            "Bearer $token"
                        } else {
                            super.getHeader(name)
                        }
                }
            filterChain.doFilter(wrapped, response)
        } else {
            filterChain.doFilter(request, response)
        }
    }
}
