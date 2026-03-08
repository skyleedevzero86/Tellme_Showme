package com.sleekydz86.tellme.showme.infrastructure.adapter

import com.sleekydz86.tellme.global.config.TelegramBotProperties
import com.sleekydz86.tellme.showme.application.port.TelegramApiPort
import com.sleekydz86.tellme.showme.domain.dto.TelegramFileResponse
import com.sleekydz86.tellme.showme.domain.dto.TelegramSendResponse
import com.sleekydz86.tellme.showme.domain.dto.TelegramUpdate
import com.sleekydz86.tellme.showme.domain.dto.WebhookInfoResponse
import org.slf4j.LoggerFactory
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.InputStreamResource
import org.springframework.http.MediaType
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.io.IOException
import java.io.InputStream
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import org.springframework.context.annotation.Primary
import lombok.RequiredArgsConstructor

@Component
@Primary
@RequiredArgsConstructor
class TelegramWebClientAdapter(
    @Qualifier("telegramWebClient") private val telegramWebClient: WebClient,
    private val properties: TelegramBotProperties
) : TelegramApiPort {

    private val log = LoggerFactory.getLogger(TelegramWebClientAdapter::class.java)

    override val isTokenMissing: Boolean
        get() {
            val t = properties.bot.token
            return t.isBlank()
        }

    override fun setWebhook(enabled: Boolean, urlOverride: String?): Mono<TelegramSendResponse>? {
        var path = "/bot" + botToken() + "/setWebhook"
        if (enabled) {
            var webhookUrl = (urlOverride?.trim() ?: properties.api.webhookUrl.trim()).ifBlank { null }
                ?: return Mono.error(IllegalStateException("웹후크 URL을 입력하세요. ngrok 실행 후 나온 주소 (예: https://xxxx.ngrok-free.app)"))
            if (!webhookUrl.startsWith("https://") || webhookUrl.contains("your-public-https-url")) {
                return Mono.error(IllegalStateException("웹후크 URL은 HTTPS 주소여야 합니다. (예: https://xxxx.ngrok-free.app)"))
            }
            if (!webhookUrl.endsWith("/callback.do")) {
                webhookUrl = webhookUrl.trimEnd('/') + "/callback.do"
            }
            log.info("setWebhook 요청 URL: {}", webhookUrl)
            path += "?url=" + URLEncoder.encode(webhookUrl, StandardCharsets.UTF_8)
        } else {
            path += "?url="
        }
        return telegramWebClient.get()
            .uri(path)
            .retrieve()
            .bodyToMono(TelegramSendResponse::class.java)
            .onErrorMap(WebClientResponseException::class.java) { ex ->
                if (ex.statusCode.value() == 400) {
                    val body = ex.responseBodyAsString
                    val hint = when {
                        body.contains("Failed to resolve host", ignoreCase = true) || body.contains("Name or service not known", ignoreCase = true) ->
                            "Telegram이 호스트를 찾지 못했습니다. Spring 로그에서 'setWebhook 요청 URL:' 로 전송한 주소를 확인하세요. 주소가 정확한지, 앞뒤 공백/줄바꿈이 없는지 확인하고, 브라우저에서 그 주소가 열리는지 확인하세요."
                        body.isNotBlank() -> body
                        else -> "HTTPS이고 외부에서 접속 가능한 URL인지 확인하세요."
                    }
                    IllegalStateException("Telegram 웹후크 거부(400): $hint")
                } else {
                    ex
                }
            }
    }

    override fun getWebhookInfo(): Mono<WebhookInfoResponse>? {
        val path = "/bot" + botToken() + "/getWebhookInfo"
        return telegramWebClient.get()
            .uri(path)
            .retrieve()
            .bodyToMono(WebhookInfoResponse::class.java)
    }

    override fun deleteWebhook(): Mono<TelegramSendResponse>? {
        val path = "/bot" + botToken() + "/deleteWebhook"
        return telegramWebClient.get()
            .uri(path)
            .retrieve()
            .bodyToMono(TelegramSendResponse::class.java)
    }

    override fun getUpdates(offset: Long?): Mono<TelegramUpdate>? {
        var path = "/bot" + botToken() + "/getUpdates"
        if (offset != null && offset > 0) {
            path += "?offset=" + offset + "&timeout=30"
        } else {
            path += "?timeout=30"
        }
        return telegramWebClient.get()
            .uri(path)
            .retrieve()
            .bodyToMono(TelegramUpdate::class.java)
            .doOnNext { u ->
                if (u?.ok == false) {
                    log.warn("getUpdates not ok: {}", u)
                }
            }
    }

    override fun getFile(fileId: String?): Mono<TelegramFileResponse>? {
        val path = "/bot" + botToken() + "/getFile?file_id=" + fileId
        return telegramWebClient.get()
            .uri(path)
            .retrieve()
            .bodyToMono(TelegramFileResponse::class.java)
    }

    override fun getFileDownloadUri(filePath: String?): String? {
        return "/file/bot" + botToken() + "/" + filePath
    }

    override fun downloadFileToLocal(fileId: String?, suggestedFileName: String?): Mono<Path>? {
        return getFile(fileId)!!
            .filter { r -> r.ok == true && r.result != null }
            .flatMap { r ->
                val filePath = r.result!!.filePath ?: return@flatMap Mono.error<Path>(IllegalStateException("file_path is null"))
                val name = if (!suggestedFileName.isNullOrBlank())
                    suggestedFileName
                else
                    filePath.substring(filePath.lastIndexOf('/') + 1)
                downloadFileByPathToLocal(filePath, name)
            }
    }

    private fun downloadFileByPathToLocal(filePath: String, suggestedFileName: String?): Mono<Path> {
        val uri = getFileDownloadUri(filePath) ?: return Mono.error(IllegalStateException("uri is null"))
        return telegramWebClient.get()
            .uri(uri)
            .retrieve()
            .bodyToMono(ByteArray::class.java)
            .flatMap { bytes ->
                try {
                    val dir = Path.of(properties.files.downloadDir)
                    Files.createDirectories(dir)
                    val name = if (!suggestedFileName.isNullOrBlank())
                        suggestedFileName
                    else
                        filePath.substring(filePath.lastIndexOf('/') + 1)
                    val target = dir.resolve(name)
                    Files.write(target, bytes)
                    log.debug("Downloaded file to {}", target)
                    Mono.just(target)
                } catch (e: IOException) {
                    Mono.error<Path>(e)
                }
            }
    }

    override fun sendMessage(chatId: Long?, text: String?): Mono<TelegramSendResponse>? {
        if (chatId == null) return Mono.empty()
        return telegramWebClient.post()
            .uri("/bot" + botToken() + "/sendMessage")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(mapOf("chat_id" to chatId, "text" to (text ?: "")))
            .retrieve()
            .bodyToMono(TelegramSendResponse::class.java)
    }

    override fun sendMessageToChannel(channelIdOrUsername: String?, text: String?): Mono<TelegramSendResponse>? {
        return telegramWebClient.post()
            .uri("/bot" + botToken() + "/sendMessage")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(mapOf("chat_id" to (channelIdOrUsername ?: ""), "text" to (text ?: "")))
            .retrieve()
            .bodyToMono(TelegramSendResponse::class.java)
    }

    override fun sendDocument(
        chatId: Long?,
        caption: String?,
        fileName: String?,
        fileStream: InputStream?,
        fileLength: Long
    ): Mono<TelegramSendResponse>? {
        if (fileStream == null) return Mono.empty()
        val body: MultiValueMap<String, Any> = LinkedMultiValueMap()
        body.add("chat_id", chatId)
        if (!caption.isNullOrBlank()) body.add("caption", caption)
        body.add("document", object : InputStreamResource(fileStream) {
            override fun getFilename(): String = fileName ?: "file"
        })
        return telegramWebClient.post()
            .uri("/bot" + botToken() + "/sendDocument")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(TelegramSendResponse::class.java)
    }

    override fun sendPhoto(
        chatId: Long?,
        caption: String?,
        fileName: String?,
        photoStream: InputStream?,
        size: Long
    ): Mono<TelegramSendResponse>? {
        if (photoStream == null) return Mono.empty()
        val body: MultiValueMap<String, Any> = LinkedMultiValueMap()
        body.add("chat_id", chatId)
        if (!caption.isNullOrBlank()) body.add("caption", caption)
        body.add("photo", object : InputStreamResource(photoStream) {
            override fun getFilename(): String = fileName ?: "photo.jpg"
        })
        return telegramWebClient.post()
            .uri("/bot" + botToken() + "/sendPhoto")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(TelegramSendResponse::class.java)
    }

    override fun sendPhotoToChannel(
        channelIdOrUsername: String?,
        caption: String?,
        fileName: String?,
        photoBytes: ByteArray?
    ): Mono<TelegramSendResponse>? {
        if (photoBytes == null || photoBytes.isEmpty()) return Mono.empty()
        val name = fileName?.takeIf { it.isNotBlank() } ?: "photo.jpg"
        val body: MultiValueMap<String, Any> = LinkedMultiValueMap()
        body.add("chat_id", channelIdOrUsername ?: "")
        if (!caption.isNullOrBlank()) body.add("caption", caption)
        body.add("photo", object : ByteArrayResource(photoBytes) {
            override fun getFilename(): String = name
        })
        return telegramWebClient.post()
            .uri("/bot" + botToken() + "/sendPhoto")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(TelegramSendResponse::class.java)
    }

    override fun sendDocumentToChannel(
        channelIdOrUsername: String?,
        caption: String?,
        fileName: String?,
        documentBytes: ByteArray?
    ): Mono<TelegramSendResponse>? {
        if (documentBytes == null || documentBytes.isEmpty()) return Mono.empty()
        val name = fileName?.takeIf { it.isNotBlank() } ?: "document"
        val body: MultiValueMap<String, Any> = LinkedMultiValueMap()
        body.add("chat_id", channelIdOrUsername ?: "")
        if (!caption.isNullOrBlank()) body.add("caption", caption)
        body.add("document", object : ByteArrayResource(documentBytes) {
            override fun getFilename(): String = name
        })
        return telegramWebClient.post()
            .uri("/bot" + botToken() + "/sendDocument")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(TelegramSendResponse::class.java)
    }

    private fun botToken(): String = properties.bot.token
}
