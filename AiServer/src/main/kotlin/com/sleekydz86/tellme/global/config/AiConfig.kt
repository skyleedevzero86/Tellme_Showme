package com.sleekydz86.tellme.global.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.nio.file.Paths

@Configuration
class AiConfig {

    @Value("\${huggingface.chat.cache-dir:./models}")
    private lateinit var cacheDir: String

    @Bean
    fun initCacheDir(): String {
        Paths.get(cacheDir).toFile().mkdirs()
        return cacheDir
    }
}
