package com.sleekydz86.tellme.global.config

import io.minio.MinioClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MinioConfig {

    @Bean
    fun minioClient(
        @Value("\${minio.url:http://localhost:9000}") url: String,
        @Value("\${minio.access-key:minioadmin}") accessKey: String,
        @Value("\${minio.secret-key:minioadmin}") secretKey: String
    ): MinioClient = MinioClient.builder().endpoint(url).credentials(accessKey, secretKey).build()
}
