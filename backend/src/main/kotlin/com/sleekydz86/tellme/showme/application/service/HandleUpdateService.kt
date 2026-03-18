package com.sleekydz86.tellme.showme.application.service

import com.sleekydz86.tellme.showme.application.port.AiServerModeChatPort
import com.sleekydz86.tellme.showme.application.port.AiServerTelegramPort
import com.sleekydz86.tellme.showme.application.port.AiServerUploadPort
import com.sleekydz86.tellme.showme.application.port.TelegramApiPort
import com.sleekydz86.tellme.showme.application.service.handler.CommandHandler
import com.sleekydz86.tellme.showme.application.service.handler.EndCommandHandler
import com.sleekydz86.tellme.showme.application.service.handler.EchoCommandHandler
import com.sleekydz86.tellme.showme.application.service.handler.EngCommandHandler
import com.sleekydz86.tellme.showme.application.service.handler.GodCommandHandler
import com.sleekydz86.tellme.showme.application.service.handler.HelpCommandHandler
import com.sleekydz86.tellme.showme.application.service.handler.LottoCommandHandler
import com.sleekydz86.tellme.showme.application.service.handler.SearchCommandHandler
import com.sleekydz86.tellme.showme.application.service.handler.StartCommandHandler
import com.sleekydz86.tellme.showme.application.service.handler.TimeCommandHandler
import com.sleekydz86.tellme.showme.domain.BotCommand
import com.sleekydz86.tellme.showme.domain.MessageContext
import com.sleekydz86.tellme.showme.domain.dto.TelegramUpdate
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.nio.file.Files
import java.time.Instant

@Service
class HandleUpdateService(
    private val telegramApi: TelegramApiPort,
    private val aiServerUpload: AiServerUploadPort,
    private val aiServerTelegram: AiServerTelegramPort,
    private val aiServerModeChat: AiServerModeChatPort,
    private val conversationModeStore: TelegramConversationModeStore,
    private val timeAlarmService: TimeAlarmService,
    private val historySseService: HistorySseService,
    startHandler: StartCommandHandler?,
    timeHandler: TimeCommandHandler?,
    helpHandler: HelpCommandHandler?,
    lottoHandler: LottoCommandHandler?,
    godHandler: GodCommandHandler?,
    engHandler: EngCommandHandler?,
    endHandler: EndCommandHandler?,
    searchHandler: SearchCommandHandler?,
    echoHandler: EchoCommandHandler?
) {
    private val log = LoggerFactory.getLogger(HandleUpdateService::class.java)
    private val echoHandler: CommandHandler?

    private val commandHandlers: Map<BotCommand, CommandHandler?> = mapOf(
        BotCommand.START to startHandler,
        BotCommand.TIME to timeHandler,
        BotCommand.HELP to helpHandler,
        BotCommand.LOTTO to lottoHandler,
        BotCommand.GOD to godHandler,
        BotCommand.ENG to engHandler,
        BotCommand.END to endHandler,
        BotCommand.SEARCH to searchHandler
    )

    init {
        this.echoHandler = echoHandler
    }

    fun handle(message: TelegramUpdate.Message): Mono<Void> {
        val chatId = message.chat?.id ?: return Mono.empty()

        if (!message.text.isNullOrBlank()) {
            return handleText(chatId, message)
        }
        if (message.document != null) {
            return handleDocument(chatId, message)
        }
        if (!message.photo.isNullOrEmpty()) {
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

    private fun handleText(chatId: Long, message: TelegramUpdate.Message): Mono<Void> {
        val ctx = MessageContext.from(message) ?: return Mono.empty()
        val messageId = message.messageId ?: 0L
        val fallbackFromId = message.from?.id ?: 0L
        val fallbackFromName = message.from?.firstName?.takeIf { it.isNotBlank() }
            ?: message.from?.username?.takeIf { it.isNotBlank() }
            ?: "사용자"
        val fromUserId = message.senderId() ?: fallbackFromId
        val fromUserName = message.senderDisplayName() ?: fallbackFromName
        val receivedAt = message.date?.let { Instant.ofEpochSecond(it) } ?: Instant.now()

        val saveMono = aiServerTelegram.saveMessage(
            telegramMessageId = messageId,
            chatId = chatId,
            fromUserId = fromUserId,
            fromUserName = fromUserName,
            text = ctx.text,
            receivedAt = receivedAt
        ).doOnNext { saved ->
            if (!saved) {
                log.warn("Failed to save telegram message history: chatId={}, messageId={}", chatId, messageId)
            } else {
                historySseService.publishMessageSaved()
            }
        }

        val replyMono = resolveReply(ctx)

        return saveMono
            .then(replyMono.flatMap { reply -> telegramApi.sendMessage(chatId, reply)!! })
            .then()
    }

    private fun resolveReply(ctx: MessageContext): Mono<String> {
        val command = BotCommand.from(ctx.text)
        if (command != null) {
            val handler = commandHandlers[command] ?: return Mono.empty()
            return handler.handle(ctx) ?: Mono.empty()
        }

        val chatId = ctx.chatId ?: return echoHandler?.handle(ctx) ?: Mono.empty()
        if (timeAlarmService.hasPendingSetup(chatId)) {
            return Mono.just(timeAlarmService.continueSetup(chatId, ctx.text.orEmpty()))
        }

        val activeMode = conversationModeStore.get(chatId)
        if (activeMode == null) {
            return echoHandler?.handle(ctx) ?: Mono.empty()
        }

        val text = ctx.text.orEmpty()
        if (isConversationExitText(text)) {
            conversationModeStore.clear(chatId)
            return Mono.just("${activeMode.label}를 종료했어요. 이제 원래 대화로 돌아왔습니다.")
        }

        return aiServerModeChat.chat(chatId.toString(), text, activeMode)
    }

    private fun isConversationExitText(text: String?): Boolean =
        text?.trim()?.equals(EXIT_KEYWORD, ignoreCase = true) == true

    private fun handleDocument(chatId: Long, message: TelegramUpdate.Message): Mono<Void> {
        val document = message.document ?: return Mono.empty()
        val fileId = document.fileId
        val fileName = document.fileName ?: "document"
        log.info("Document received: fileId={}, fileName={}, size={}", fileId, fileName, document.fileSize)

        return telegramApi.downloadFileToLocal(fileId, fileName)!!
            .flatMap { localPath ->
                val bytes = try {
                    Files.readAllBytes(localPath)
                } catch (e: Exception) {
                    log.warn("document read error", e)
                    return@flatMap telegramApi.sendMessage(chatId, "파일 읽기 오류: ${e.message}")!!.then()
                }

                val telegramMessageId = message.messageId ?: 0L
                val fromUserName = message.senderDisplayName()
                    ?: message.from?.firstName?.takeIf { it.isNotBlank() }
                    ?: message.from?.username?.takeIf { it.isNotBlank() }
                    ?: "사용자"

                aiServerUpload.upload(
                    bytes = bytes,
                    fileName = fileName,
                    contentType = document.mimeType,
                    userId = chatId.toString(),
                    uploadSource = "TELEGRAM",
                    telegramMessageId = telegramMessageId,
                    fromUserName = fromUserName
                ).doOnNext { saved ->
                    if (!saved) {
                        log.warn(
                            "Failed to save telegram document history: chatId={}, messageId={}, fileName={}",
                            chatId,
                            telegramMessageId,
                            fileName
                        )
                    } else {
                        historySseService.publishFileSaved()
                    }
                }.flatMap { saved ->
                    val reply = if (saved) {
                        "파일을 받았습니다.\n이름: $fileName\n크기: ${document.fileSize ?: 0} bytes"
                    } else {
                        "파일 저장 중 오류가 발생했습니다."
                    }
                    telegramApi.sendMessage(chatId, reply)!!
                }
            }
            .onErrorResume { e -> telegramApi.sendMessage(chatId, "파일 저장 중 오류: ${e.message}")!! }
            .then()
    }

    private fun handlePhoto(chatId: Long, message: TelegramUpdate.Message): Mono<Void> {
        val photoList = message.photo ?: return Mono.empty()
        val largest = photoList.lastOrNull() ?: return Mono.empty()
        val fileId = largest.fileId
        val fileName = "photo_${largest.fileUniqueId}.jpg"
        log.info("Photo received: fileId={}", fileId)

        return telegramApi.downloadFileToLocal(fileId, fileName)!!
            .flatMap { localPath ->
                val bytes = try {
                    Files.readAllBytes(localPath)
                } catch (e: Exception) {
                    log.warn("photo read error", e)
                    return@flatMap telegramApi.sendMessage(chatId, "사진 읽기 오류: ${e.message}")!!.then()
                }

                val telegramMessageId = message.messageId ?: 0L
                val fromUserName = message.senderDisplayName()
                    ?: message.from?.firstName?.takeIf { it.isNotBlank() }
                    ?: message.from?.username?.takeIf { it.isNotBlank() }
                    ?: "사용자"

                aiServerUpload.upload(
                    bytes = bytes,
                    fileName = fileName,
                    contentType = "image/jpeg",
                    userId = chatId.toString(),
                    uploadSource = "TELEGRAM",
                    telegramMessageId = telegramMessageId,
                    fromUserName = fromUserName
                ).doOnNext { saved ->
                    if (!saved) {
                        log.warn(
                            "Failed to save telegram photo history: chatId={}, messageId={}, fileName={}",
                            chatId,
                            telegramMessageId,
                            fileName
                        )
                    } else {
                        historySseService.publishFileSaved()
                    }
                }.flatMap { saved ->
                    val width = largest.width ?: 0
                    val height = largest.height ?: 0
                    val reply = if (saved) {
                        "이미지 ${width}x${height} 사진을 받았습니다."
                    } else {
                        "사진 저장 중 오류가 발생했습니다."
                    }
                    telegramApi.sendMessage(chatId, reply)!!
                }
            }
            .onErrorResume { e -> telegramApi.sendMessage(chatId, "사진 저장 중 오류: ${e.message}")!! }
            .then()
    }

    private fun handleVoice(chatId: Long, message: TelegramUpdate.Message): Mono<Void> {
        val fileId = message.voice?.fileId ?: return Mono.empty()
        log.info("Voice received: fileId={}", fileId)

        return telegramApi.downloadFileToLocal(fileId, "voice_${message.messageId}.ogg")!!
            .flatMap { localPath -> telegramApi.sendMessage(chatId, "음성 메시지를 받았습니다. 저장 위치: $localPath")!! }
            .onErrorResume { e -> telegramApi.sendMessage(chatId, "음성 저장 중 오류: ${e.message}")!! }
            .then()
    }

    private fun handleVideo(chatId: Long, message: TelegramUpdate.Message): Mono<Void> {
        val fileId = message.video?.fileId ?: return Mono.empty()
        val fileName = "video_${message.messageId}.mp4"
        log.info("Video received: fileId={}", fileId)

        return telegramApi.downloadFileToLocal(fileId, fileName)!!
            .flatMap { localPath -> telegramApi.sendMessage(chatId, "동영상을 받았습니다. 저장 위치: $localPath")!! }
            .onErrorResume { e -> telegramApi.sendMessage(chatId, "동영상 저장 중 오류: ${e.message}")!! }
            .then()
    }

    companion object {
        private const val EXIT_KEYWORD = "bye"
        private const val FALLBACK_MESSAGE = "텍스트나 파일(문서/사진/음성/동영상)을 보내주세요."
    }
}
