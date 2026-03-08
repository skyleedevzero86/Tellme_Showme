package com.sleekydz86.tellme.global.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient


@Configuration
@EnableConfigurationProperties(TelegramBotProperties::class)
class TelegramBotConfig {
    @Bean
    fun telegramWebClient(props: TelegramBotProperties): WebClient {
        return WebClient.builder()
            .baseUrl(props.api.baseUrl)
            .build()
    }
}
