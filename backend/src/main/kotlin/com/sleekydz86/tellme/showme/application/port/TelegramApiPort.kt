package com.sleekydz86.tellme.showme.application.port

import com.sleekydz86.tellme.showme.domain.dto.TelegramFileResponse
import com.sleekydz86.tellme.showme.domain.dto.TelegramSendResponse
import com.sleekydz86.tellme.showme.domain.dto.TelegramUpdate
import reactor.core.publisher.Mono
import java.io.InputStream
import java.nio.file.Path

interface TelegramApiPort {
    val isTokenMissing: Boolean

    fun setWebhook(enabled: Boolean): Mono<TelegramSendResponse?>?

    fun getUpdates(offset: Long?): Mono<TelegramUpdate?>?

    fun getFile(fileId: String?): Mono<TelegramFileResponse?>?

    fun getFileDownloadUri(filePath: String?): String?

    fun downloadFileToLocal(fileId: String?, suggestedFileName: String?): Mono<Path?>?

    fun sendMessage(chatId: Long?, text: String?): Mono<TelegramSendResponse?>?

    fun sendMessageToChannel(channelIdOrUsername: String?, text: String?): Mono<TelegramSendResponse?>?

    fun sendDocument(
        chatId: Long?,
        caption: String?,
        fileName: String?,
        fileStream: InputStream?,
        fileLength: Long
    ): Mono<TelegramSendResponse?>?

    fun sendPhoto(
        chatId: Long?,
        caption: String?,
        fileName: String?,
        photoStream: InputStream?,
        size: Long
    ): Mono<TelegramSendResponse?>?

    fun sendPhotoToChannel(
        channelIdOrUsername: String?, caption: String?,
        fileName: String?, photoStream: InputStream?, size: Long
    ): Mono<TelegramSendResponse?>?
}