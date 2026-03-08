package com.sleekydz86.tellme.global.config

import com.sleekydz86.tellme.showme.application.port.ExternalContentPort
import com.sleekydz86.tellme.showme.application.port.TelegramApiPort
import com.sleekydz86.tellme.showme.infrastructure.adapter.ExternalContentAdapterStub
import com.sleekydz86.tellme.showme.infrastructure.adapter.TelegramApiPortStub
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class StubBeansConfig {

    @Bean
    @ConditionalOnMissingBean(TelegramApiPort::class)
    fun telegramApiPort(): TelegramApiPort = TelegramApiPortStub()

    @Bean
    @ConditionalOnMissingBean(ExternalContentPort::class)
    fun externalContentPort(): ExternalContentPort = ExternalContentAdapterStub()
}