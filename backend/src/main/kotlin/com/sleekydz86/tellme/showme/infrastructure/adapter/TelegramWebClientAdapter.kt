package com.sleekydz86.tellme.showme.infrastructure.adapter

import com.sleekydz86.tellme.global.config.TelegramBotProperties
import com.sleekydz86.tellme.showme.application.port.TelegramApiPort
import com.sleekydz86.tellme.showme.domain.dto.TelegramFileResponse
import com.sleekydz86.tellme.showme.domain.dto.TelegramSendResponse
import com.sleekydz86.tellme.showme.domain.dto.TelegramUpdate
import org.slf4j.LoggerFactory
import org.springframework.core.io.InputStreamResource
import org.springframework.http.MediaType
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.client.WebClient
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

    override fun setWebhook(enabled: Boolean): Mono<TelegramSendResponse>? {
        var path = "/bot" + botToken() + "/setWebhook"
        if (enabled) {
            val webhookUrl = properties.api.webhookUrl
            if (webhookUrl.isBlank()) {
                return Mono.error(IllegalStateException("telegram.api.webhook-url 이 설정되지 않았습니다."))
            }
            path += "?url=" + URLEncoder.encode(webhookUrl, StandardCharsets.UTF_8)
        } else {
            path += "?url="
        }
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
        photoStream: InputStream?,
        size: Long
    ): Mono<TelegramSendResponse>? {
        if (photoStream == null) return Mono.empty()
        val body: MultiValueMap<String, Any> = LinkedMultiValueMap()
        body.add("chat_id", channelIdOrUsername ?: "")
        if (!caption.isNullOrBlank()) body.add("caption", caption)
        body.add("photo", object : InputStreamResource(photoStream) {
            override fun getFilename(): String = fileName?.takeIf { it.isNotBlank() } ?: "photo.jpg"
        })
        return telegramWebClient.post()
            .uri("/bot" + botToken() + "/sendPhoto")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(TelegramSendResponse::class.java)
    }

    private fun botToken(): String = properties.bot.token
}
