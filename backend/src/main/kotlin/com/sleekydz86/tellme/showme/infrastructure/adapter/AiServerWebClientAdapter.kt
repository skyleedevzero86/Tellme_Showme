package com.sleekydz86.tellme.showme.infrastructure.adapter

import com.sleekydz86.tellme.showme.application.port.AiServerHistoryPort
import com.sleekydz86.tellme.showme.application.port.AiServerTelegramPort
import com.sleekydz86.tellme.showme.application.port.AiServerUploadPort
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.time.Instant
import java.net.URLEncoder

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
        val bodyBuilder = MultipartBodyBuilder()
        val filePart = bodyBuilder.part("file", object : ByteArrayResource(bytes) {
            override fun getFilename(): String = fileName
        })
        filePart.filename(fileName)
        contentType
            ?.takeIf { it.isNotBlank() }
            ?.let { raw -> runCatching { MediaType.parseMediaType(raw) }.getOrNull() }
            ?.let { filePart.contentType(it) }

        bodyBuilder.part("userId", userId)
        bodyBuilder.part("uploadSource", uploadSource)
        telegramMessageId?.let { bodyBuilder.part("telegramMessageId", it.toString()) }
        fromUserName?.let { bodyBuilder.part("fromUserName", it) }

        val body = bodyBuilder.build()
        var requestSpec = webClient.post()
            .uri("/rag/upload")
            .header("X-User-Id", userId)
            .header("X-Upload-Source", uploadSource)
        if (uploadSource == "TELEGRAM") {
            telegramMessageId?.let { requestSpec = requestSpec.header("X-Telegram-Message-Id", it.toString()) }
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

    override fun getFilePreview(objectKey: String): Mono<com.sleekydz86.tellme.showme.application.port.FilePreviewPayload> {
        val encodedObjectKey = URLEncoder.encode(objectKey, Charsets.UTF_8)
        return webClient.get()
            .uri("/api/telegram/files/preview?objectKey=$encodedObjectKey")
            .exchangeToMono { response ->
                if (!response.statusCode().is2xxSuccessful) {
                    return@exchangeToMono response.createException().flatMap { Mono.error(it) }
                }
                val contentType = response.headers().contentType().map { it.toString() }.orElse(null)
                val contentDisposition = response.headers().asHttpHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)
                response.bodyToMono(ByteArray::class.java)
                    .map { bytes ->
                        com.sleekydz86.tellme.showme.application.port.FilePreviewPayload(
                            bytes = bytes,
                            contentType = contentType,
                            contentDisposition = contentDisposition
                        )
                    }
            }
    }

    private fun buildHistoryUri(path: String, page: Int, size: Int, search: String?): String {
        val params = mutableListOf("page=$page", "size=$size")
        if (!search.isNullOrBlank()) params.add("search=${java.net.URLEncoder.encode(search, Charsets.UTF_8)}")
        return "$path?${params.joinToString("&")}"
    }
}
