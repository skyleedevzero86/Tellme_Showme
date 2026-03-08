package com.sleekydz86.tellme.showme.infrastructure.adapter

import com.sleekydz86.tellme.showme.application.port.TelegramApiPort
import com.sleekydz86.tellme.showme.domain.dto.TelegramFileResponse
import com.sleekydz86.tellme.showme.domain.dto.TelegramSendResponse
import com.sleekydz86.tellme.showme.domain.dto.TelegramUpdate
import reactor.core.publisher.Mono
import java.io.InputStream
import java.nio.file.Path

class TelegramApiPortStub : TelegramApiPort {

    override val isTokenMissing: Boolean = true

    override fun setWebhook(enabled: Boolean): Mono<TelegramSendResponse> =
        Mono.just(TelegramSendResponse(ok = true, description = "stub", result = null))

    override fun getUpdates(offset: Long?): Mono<TelegramUpdate> =
        Mono.just(TelegramUpdate())

    override fun getFile(fileId: String?): Mono<TelegramFileResponse> =
        Mono.just(TelegramFileResponse())

    override fun getFileDownloadUri(filePath: String?): String? = null

    override fun downloadFileToLocal(fileId: String?, suggestedFileName: String?): Mono<Path> =
        Mono.empty()

    override fun sendMessage(chatId: Long?, text: String?): Mono<TelegramSendResponse> =
        Mono.just(TelegramSendResponse(ok = true, description = null, result = null))

    override fun sendMessageToChannel(channelIdOrUsername: String?, text: String?): Mono<TelegramSendResponse> =
        Mono.just(TelegramSendResponse(ok = true, description = null, result = null))

    override fun sendDocument(
        chatId: Long?,
        caption: String?,
        fileName: String?,
        fileStream: InputStream?,
        fileLength: Long
    ): Mono<TelegramSendResponse> =
        Mono.just(TelegramSendResponse(ok = true, description = null, result = null))

    override fun sendPhoto(
        chatId: Long?,
        caption: String?,
        fileName: String?,
        photoStream: InputStream?,
        size: Long
    ): Mono<TelegramSendResponse> =
        Mono.just(TelegramSendResponse(ok = true, description = null, result = null))

    override fun sendPhotoToChannel(
        channelIdOrUsername: String?,
        caption: String?,
        fileName: String?,
        photoStream: InputStream?,
        size: Long
    ): Mono<TelegramSendResponse> =
        Mono.just(TelegramSendResponse(ok = true, description = null, result = null))
}
