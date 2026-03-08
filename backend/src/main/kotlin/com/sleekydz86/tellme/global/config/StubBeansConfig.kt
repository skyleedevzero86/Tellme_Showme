package com.sleekydz86.tellme.global.config

import com.sleekydz86.tellme.showme.application.port.ExternalContentPort
import com.sleekydz86.tellme.showme.application.port.TelegramApiPort
import com.sleekydz86.tellme.showme.infrastructure.adapter.ExternalContentAdapterStub
import com.sleekydz86.tellme.showme.infrastructure.adapter.TelegramApiPortStub
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class StubBeansConfig {

    @Bean
    @Primary
    fun telegramApiPort(): TelegramApiPort = TelegramApiPortStub()

    @Bean
    @Primary
    fun externalContentPort(): ExternalContentPort = ExternalContentAdapterStub()
}