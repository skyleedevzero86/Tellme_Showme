package com.sleekydz86.tellme.showme.infrastructure.adapter

import com.sleekydz86.tellme.showme.application.port.AiServerHistoryPort
import com.sleekydz86.tellme.showme.application.port.AiServerTelegramPort
import com.sleekydz86.tellme.showme.application.port.AiServerUploadPort
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.MediaType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.time.Instant

@Component
class AiServerWebClientAdapter(
    @Qualifier("aiServerWebClient") private val webClient: WebClient
) : AiServerUploadPort, AiServerTelegramPort, AiServerHistoryPort {
    private val log = LoggerFactory.getLogger(AiServerWebClientAdapter::class.java)

    override fun upload(
        bytes: ByteArray,
        fileName: String,
        contentType: String?,
        userId: String,
        uploadSource: String,
        telegramMessageId: Long?,
        fromUserName: String?
    ): Mono<Boolean> {
        val body: MultiValueMap<String, Any> = LinkedMultiValueMap()
        body.add("file", object : ByteArrayResource(bytes) {
            override fun getFilename(): String = fileName
        })
        var requestSpec = webClient.post()
            .uri("/rag/upload")
            .header("X-User-Id", userId)
            .header("X-Upload-Source", uploadSource)
        if (uploadSource == "TELEGRAM") {
            telegramMessageId?.let { requestSpec = requestSpec.header("X-Telegram-Message-Id", it.toString()) }
            fromUserName?.let { requestSpec = requestSpec.header("X-From-User-Name", it) }
        }
        return requestSpec
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .bodyValue(body)
            .retrieve()
            .toBodilessEntity()
            .map { it.statusCode.is2xxSuccessful }
            .onErrorResume { e ->
                log.warn(
                    "Failed to upload file to AiServer: userId={}, source={}, fileName={}",
                    userId,
                    uploadSource,
                    fileName,
                    e
                )
                Mono.just(false)
            }
    }

    override fun saveMessage(
        telegramMessageId: Long,
        chatId: Long,
        fromUserId: Long,
        fromUserName: String?,
        text: String?,
        receivedAt: Instant
    ): Mono<Boolean> {
        val body = mapOf(
            "telegramMessageId" to telegramMessageId,
            "chatId" to chatId,
            "fromUserId" to fromUserId,
            "fromUserName" to (fromUserName ?: ""),
            "text" to (text ?: ""),
            "receivedAt" to receivedAt.toString()
        )
        return webClient.post()
            .uri("/api/telegram/messages")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .retrieve()
            .toBodilessEntity()
            .map { it.statusCode.is2xxSuccessful }
            .onErrorResume { e ->
                log.warn(
                    "Failed to save telegram message to AiServer: chatId={}, messageId={}",
                    chatId,
                    telegramMessageId,
                    e
                )
                Mono.just(false)
            }
    }

    override fun getMessageHistory(page: Int, size: Int, search: String?): Mono<String> {
        val uri = buildHistoryUri("/api/telegram/messages", page, size, search)
        return webClient.get()
            .uri(uri)
            .retrieve()
            .bodyToMono(String::class.java)
            .defaultIfEmpty("{}")
    }

    override fun getFileHistory(page: Int, size: Int, search: String?): Mono<String> {
        val uri = buildHistoryUri("/api/telegram/files", page, size, search)
        return webClient.get()
            .uri(uri)
            .retrieve()
            .bodyToMono(String::class.java)
            .defaultIfEmpty("{}")
    }

    private fun buildHistoryUri(path: String, page: Int, size: Int, search: String?): String {
        val params = mutableListOf("page=$page", "size=$size")
        if (!search.isNullOrBlank()) params.add("search=${java.net.URLEncoder.encode(search, Charsets.UTF_8)}")
        return "$path?${params.joinToString("&")}"
    }
}
