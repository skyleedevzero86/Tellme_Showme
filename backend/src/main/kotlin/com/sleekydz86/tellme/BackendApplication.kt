package com.sleekydz86.tellme

import com.sleekydz86.tellme.global.config.StubBeansConfig
import com.sleekydz86.tellme.global.config.TelegramBotProperties
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
@Import(StubBeansConfig::class)
class BackendApplication {

    @Bean
    fun logWebhookStatus(properties: TelegramBotProperties): ApplicationRunner {
        return ApplicationRunner {
            val configured = properties.api.webhookUrl.isNotBlank()
            if (configured) {
                org.slf4j.LoggerFactory.getLogger(BackendApplication::class.java)
                    .info("TELEGRAM_WEBHOOK_URL: configured (웹후크 설정 가능)")
            } else {
                org.slf4j.LoggerFactory.getLogger(BackendApplication::class.java)
                    .info("TELEGRAM_WEBHOOK_URL: not set (웹후크 설정 불가. 환경 변수 또는 run-backend-with-webhook.ps1 사용)")
            }
        }
    }
}

fun main(args: Array<String>) {
    runApplication<BackendApplication>(*args)
}
