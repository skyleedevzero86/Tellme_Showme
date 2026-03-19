package com.sleekydz86.tellme.aiserver.aplication.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class LocalOllamaCompletionService(
    @Value("\${spring.ai.ollama.base-url:http://localhost:11434}") baseUrl: String,
    @param:Value("\${spring.ai.ollama.chat.model:qwen2.5:7b-instruct}") private val model: String,
    @param:Value("\${spring.ai.ollama.chat.options.temperature:0.7}") private val temperature: Double,
    @param:Value("\${spring.ai.ollama.chat.options.num_predict:1000}") private val numPredict: Int
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val webClient = WebClient.builder()
        .baseUrl(baseUrl)
        .build()

    fun generate(prompt: String): String {
        val normalizedPrompt = prompt.trim()
        if (normalizedPrompt.isBlank()) {
            return ""
        }

        return runCatching {
            webClient.post()
                .uri("/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                    GenerateRequest(
                        model = model,
                        prompt = normalizedPrompt,
                        stream = false,
                        options = mapOf(
                            "temperature" to temperature,
                            "num_predict" to numPredict
                        )
                    )
                )
                .retrieve()
                .bodyToMono(GenerateResponse::class.java)
                .block()
                ?.response
                .orEmpty()
                .trim()
        }.onFailure { error ->
            logger.warn("Local Ollama generate call failed: model={}", model, error)
        }.getOrDefault("")
    }

    private data class GenerateRequest(
        val model: String,
        val prompt: String,
        val stream: Boolean,
        val options: Map<String, Any>
    )

    private data class GenerateResponse(
        val response: String? = null
    )
}
