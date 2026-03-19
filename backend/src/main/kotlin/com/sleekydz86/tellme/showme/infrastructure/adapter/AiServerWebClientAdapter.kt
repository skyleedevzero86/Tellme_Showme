package com.sleekydz86.tellme.showme.infrastructure.adapter

import com.sleekydz86.tellme.showme.application.port.AiServerHistoryPort
import com.sleekydz86.tellme.showme.application.port.AiServerModeChatPort
import com.sleekydz86.tellme.showme.application.port.AiServerSearchPort
import com.sleekydz86.tellme.showme.application.port.AiServerTelegramPort
import com.sleekydz86.tellme.showme.application.port.AiServerUploadPort
import com.sleekydz86.tellme.showme.application.port.FilePreviewPayload
import com.sleekydz86.tellme.showme.domain.ConversationMode
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.net.URLEncoder
import java.time.Duration
import java.time.Instant

@Component
class AiServerWebClientAdapter(
    @Qualifier("aiServerWebClient") private val webClient: WebClient
) : AiServerUploadPort, AiServerTelegramPort, AiServerHistoryPort, AiServerSearchPort, AiServerModeChatPort {
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

    override fun search(userId: String, message: String): Mono<String> {
        val body = mapOf(
            "currentUserName" to userId,
            "message" to message,
            "useKnowledgeBase" to true
        )
        return webClient.post()
            .uri("/chat/search")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(String::class.java)
            .timeout(AI_SERVER_RESPONSE_TIMEOUT)
            .map { it.trim() }
            .defaultIfEmpty(EMPTY_SEARCH_RESULT_MESSAGE)
            .onErrorResume { e ->
                log.warn("Failed to search in AiServer: userId={}", userId, e)
                Mono.just(SEARCH_ERROR_MESSAGE)
            }
    }

    override fun chat(userId: String, message: String, mode: ConversationMode): Mono<String> {
        val body = mapOf(
            "currentUserName" to userId,
            "message" to message,
            "mode" to mode.aiMode
        )
        return webClient.post()
            .uri("/chat/mode")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(String::class.java)
            .timeout(AI_SERVER_RESPONSE_TIMEOUT)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .switchIfEmpty(Mono.error(IllegalStateException("AiServer mode reply is blank: mode=${mode.aiMode}")))
            .doOnError { e ->
                log.warn("Failed to chat with AiServer mode: userId={}, mode={}", userId, mode.aiMode, e)
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

    override fun getFilePreview(objectKey: String): Mono<FilePreviewPayload> {
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
                        FilePreviewPayload(
                            bytes = bytes,
                            contentType = contentType,
                            contentDisposition = contentDisposition
                        )
                    }
            }
    }

    private fun buildHistoryUri(path: String, page: Int, size: Int, search: String?): String {
        val params = mutableListOf("page=$page", "size=$size")
        if (!search.isNullOrBlank()) {
            params.add("search=${URLEncoder.encode(search, Charsets.UTF_8)}")
        }
        return "$path?${params.joinToString("&")}"
    }

    companion object {
        private val AI_SERVER_RESPONSE_TIMEOUT: Duration = Duration.ofSeconds(10)
        private const val EMPTY_SEARCH_RESULT_MESSAGE =
            "\uac80\uc0c9 \uacb0\uacfc\uac00 \ube44\uc5b4 \uc788\uc2b5\ub2c8\ub2e4."
        private const val SEARCH_ERROR_MESSAGE =
            "\ubb38\uc11c \uac80\uc0c9 \uc911 \uc624\ub958\uac00 \ubc1c\uc0dd\ud588\uc2b5\ub2c8\ub2e4. AiServer \uc0c1\ud0dc\ub97c \ud655\uc778\ud574 \uc8fc\uc138\uc694."
    }
}
