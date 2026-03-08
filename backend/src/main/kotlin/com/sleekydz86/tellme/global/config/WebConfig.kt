package com.sleekydz86.tellme.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter


@Configuration
class WebConfig {
    @Bean
    fun corsFilter(): CorsFilter {
        val config = CorsConfiguration()
        config.setAllowCredentials(true)
        config.setAllowedOrigins(mutableListOf("http://localhost:3000", "http://127.0.0.1:3000"))
        config.setAllowedMethods(mutableListOf("GET", "POST", "OPTIONS"))
        config.setAllowedHeaders(mutableListOf("*"))
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", config)
        return CorsFilter(source)
    }
}
