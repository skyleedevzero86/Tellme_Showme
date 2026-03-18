package com.sleekydz86.tellme.global.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
@EnableConfigurationProperties(AiServerProperties::class)
class AiServerConfig {
    @Bean
    fun aiServerWebClient(props: AiServerProperties): WebClient {
        return WebClient.builder()
            .baseUrl(props.url)
            .build()
    }
}
