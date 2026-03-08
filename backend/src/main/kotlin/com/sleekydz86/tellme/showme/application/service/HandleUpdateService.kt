package com.sleekydz86.tellme.showme.application.service

import com.sleekydz86.tellme.showme.application.port.TelegramApiPort
import com.sleekydz86.tellme.showme.application.service.handler.*
import com.sleekydz86.tellme.showme.domain.BotCommand
import com.sleekydz86.tellme.showme.domain.MessageContext
import com.sleekydz86.tellme.showme.domain.dto.TelegramSendResponse
import com.sleekydz86.tellme.showme.domain.dto.TelegramUpdate
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.nio.file.Path


@Service
class HandleUpdateService(
    private val telegramApi: TelegramApiPort,
    startHandler: StartCommandHandler?,
    timeHandler: TimeCommandHandler?,
    helpHandler: HelpCommandHandler?,
    lottoHandler: LottoCommandHandler?,
    godHandler: GodCommandHandler?,
    engHandler: EngCommandHandler?,
    echoHandler: EchoCommandHandler?
) {
    private val log = LoggerFactory.getLogger(HandleUpdateService::class.java)
    private val echoHandler: CommandHandler?

    private val commandHandlers: kotlin.collections.Map<BotCommand, CommandHandler?> = mapOf(
        BotCommand.START to startHandler,
        BotCommand.TIME to timeHandler,
        BotCommand.HELP to helpHandler,
        BotCommand.LOTTO to lottoHandler,
        BotCommand.GOD to godHandler,
        BotCommand.ENG to engHandler
    )

    init {
        this.echoHandler = echoHandler
    }

    fun handle(message: TelegramUpdate.Message): Mono<Void> {
        val chatId = message.chat?.id ?: return Mono.empty()

        if (message.text != null && !message.text.isBlank()) {
            return handleText(chatId, message)
        }
        if (message.document != null) {
            return handleDocument(chatId, message)
        }
        if (message.photo != null && !message.photo.isEmpty()) {
            return handlePhoto(chatId, message)
        }
        if (message.voice != null) {
            return handleVoice(chatId, message)
        }
        if (message.video != null) {
            return handleVideo(chatId, message)
        }

        return telegramApi.sendMessage(chatId, FALLBACK_MESSAGE)!!.then()
    }

    private fun handleText(chatId: Long?, message: TelegramUpdate.Message?): Mono<Void> {
        val ctx = MessageContext.from(message) ?: return Mono.empty()

        val cmd = BotCommand.from(ctx.text)
        val handler = (cmd?.let { commandHandlers[it] } ?: echoHandler) ?: return Mono.empty()
        val replyMono = handler.handle(ctx) ?: return Mono.empty()

        return replyMono
            .flatMap { reply -> telegramApi.sendMessage(chatId, reply)!! }
            .then()
    }

    private fun handleDocument(chatId: Long?, message: TelegramUpdate.Message): Mono<Void> {
        val doc = message.document!!
        val fileId = doc.fileId
        val fileName = doc.fileName ?: "document"
        log.info(
            "Document received: fileId={}, fileName={}, size={}",
            fileId,
            fileName,
            doc.fileSize
        )

        return telegramApi.downloadFileToLocal(fileId, fileName)!!
            .flatMap { localPath -> telegramApi.sendMessage(chatId, "파일을 받았습니다.\n이름: $fileName\n크기: ${doc.fileSize} bytes\n로컬 저장: $localPath")!! }
            .onErrorResume { e -> telegramApi.sendMessage(chatId, "파일 저장 중 오류: ${e.message}")!! }
            .then()
    }

    private fun handlePhoto(chatId: Long?, message: TelegramUpdate.Message): Mono<Void> {
        val photoList = message.photo!!
        val largest = photoList[photoList.size - 1]!!
        val fileId = largest.fileId
        log.info("Photo received: fileId={}", fileId)

        return telegramApi.downloadFileToLocal(fileId, "photo_${largest.fileUniqueId}.jpg")!!
            .flatMap { localPath -> telegramApi.sendMessage(chatId, "사진을 받았습니다. (해상도 ${largest.width}x${largest.height})\n저장: $localPath")!! }
            .onErrorResume { e -> telegramApi.sendMessage(chatId, "사진 저장 중 오류: ${e.message}")!! }
            .then()
    }

    private fun handleVoice(chatId: Long?, message: TelegramUpdate.Message): Mono<Void> {
        val fileId = message.voice!!.fileId
        log.info("Voice received: fileId={}", fileId)

        return telegramApi.downloadFileToLocal(fileId, "voice_${message.messageId}.ogg")!!
            .flatMap { localPath -> telegramApi.sendMessage(chatId, "음성 메시지를 받았습니다. 저장: $localPath")!! }
            .onErrorResume { e -> telegramApi.sendMessage(chatId, "음성 저장 중 오류: ${e.message}")!! }
            .then()
    }

    private fun handleVideo(chatId: Long?, message: TelegramUpdate.Message): Mono<Void> {
        val fileId = message.video!!.fileId
        val fileName = "video_${message.messageId}.mp4"
        log.info("Video received: fileId={}", fileId)

        return telegramApi.downloadFileToLocal(fileId, fileName)!!
            .flatMap { localPath -> telegramApi.sendMessage(chatId, "동영상을 받았습니다. 저장: $localPath")!! }
            .onErrorResume { e -> telegramApi.sendMessage(chatId, "동영상 저장 중 오류: ${e.message}")!! }
            .then()
    }

    companion object {
        private const val FALLBACK_MESSAGE = "텍스트나 파일(문서/사진/음성/동영상)을 보내주세요."
    }
}
