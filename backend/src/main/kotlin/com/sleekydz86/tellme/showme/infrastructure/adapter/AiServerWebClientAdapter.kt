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
            .defaultIfEmpty("검색 결과가 비어 있습니다.")
            .onErrorResume { e ->
                log.warn("Failed to search in AiServer: userId={}", userId, e)
                Mono.just("문서 검색 중 오류가 발생했습니다. AiServer 상태를 확인해 주세요.")
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
            .map { if (it.isBlank()) "${mode.label} 응답이 비어 있습니다." else it }
            .onErrorResume { e ->
                log.warn("Failed to chat with AiServer mode: userId={}, mode={}", userId, mode.aiMode, e)
                Mono.just("AiServer ${mode.label} 응답 중 오류가 발생했습니다. AiServer 상태를 확인해 주세요.")
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
    }
}
