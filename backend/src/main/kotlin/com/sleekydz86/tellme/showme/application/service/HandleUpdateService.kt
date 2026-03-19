package com.sleekydz86.tellme.showme.application.service

import com.sleekydz86.tellme.showme.application.port.AiServerModeChatPort
import com.sleekydz86.tellme.showme.application.port.AiServerReplyPort
import com.sleekydz86.tellme.showme.application.port.AiServerTelegramPort
import com.sleekydz86.tellme.showme.application.port.AiServerUploadPort
import com.sleekydz86.tellme.showme.application.port.TelegramApiPort
import com.sleekydz86.tellme.showme.application.service.handler.CommandHandler
import com.sleekydz86.tellme.showme.application.service.handler.AlarmStopCommandHandler
import com.sleekydz86.tellme.showme.application.service.handler.EchoCommandHandler
import com.sleekydz86.tellme.showme.application.service.handler.EndCommandHandler
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
import java.time.Duration
import java.time.Instant

@Service
class HandleUpdateService(
    private val telegramApi: TelegramApiPort,
    private val aiServerUpload: AiServerUploadPort,
    private val aiServerTelegram: AiServerTelegramPort,
    private val aiServerModeChat: AiServerModeChatPort,
    private val aiServerReply: AiServerReplyPort,
    private val fallbackReplyFactory: ConversationModeFallbackReplyFactory,
    private val privateChatReplyFallbackFactory: PrivateChatReplyFallbackFactory,
    private val conversationModeStore: TelegramConversationModeStore,
    private val timeAlarmService: TimeAlarmService,
    private val historySseService: HistorySseService,
    startHandler: StartCommandHandler?,
    timeHandler: TimeCommandHandler?,
    helpHandler: HelpCommandHandler?,
    lottoHandler: LottoCommandHandler?,
    godHandler: GodCommandHandler?,
    engHandler: EngCommandHandler?,
    alarmStopHandler: AlarmStopCommandHandler?,
    endHandler: EndCommandHandler?,
    searchHandler: SearchCommandHandler?,
    echoHandler: EchoCommandHandler?
) {
    private val log = LoggerFactory.getLogger(HandleUpdateService::class.java)
    private val echoHandler: CommandHandler? = echoHandler

    private val commandHandlers: Map<BotCommand, CommandHandler?> = mapOf(
        BotCommand.START to startHandler,
        BotCommand.TIME to timeHandler,
        BotCommand.HELP to helpHandler,
        BotCommand.LOTTO to lottoHandler,
        BotCommand.GOD to godHandler,
        BotCommand.ENG to engHandler,
        BotCommand.ALARM_STOP to alarmStopHandler,
        BotCommand.END to endHandler,
        BotCommand.SEARCH to searchHandler
    )

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

    fun replyForContext(ctx: MessageContext): Mono<String> {
        val defaultReply = ctx.text?.takeIf { it.isNotBlank() } ?: DEFAULT_REPLY_MESSAGE
        return resolveReply(ctx)
            .onErrorResume { e ->
                log.warn(
                    "Reply generation failed: chatId={}, source={}, text={}",
                    ctx.chatId,
                    ctx.inputSource,
                    ctx.text,
                    e
                )
                Mono.just(defaultReply)
            }
            .switchIfEmpty(Mono.just(defaultReply))
    }

    private fun handleText(chatId: Long, message: TelegramUpdate.Message): Mono<Void> {
        val ctx = MessageContext.from(message) ?: return Mono.empty()
        val messageId = message.messageId ?: 0L
        val fallbackFromId = message.from?.id ?: 0L
        val fallbackFromName = message.from?.firstName?.takeIf { it.isNotBlank() }
            ?: message.from?.username?.takeIf { it.isNotBlank() }
            ?: "User"
        val fromUserId = message.senderId() ?: fallbackFromId
        val fromUserName = message.senderDisplayName() ?: fallbackFromName
        val receivedAt = message.date?.let { Instant.ofEpochSecond(it) } ?: Instant.now()

        aiServerTelegram.saveMessage(
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
        }.onErrorResume { e ->
            log.warn("Telegram message history save error: chatId={}, messageId={}", chatId, messageId, e)
            Mono.just(false)
        }.subscribe()

        return replyForContext(ctx)
            .flatMap { reply -> telegramApi.sendMessage(chatId, reply)!! }
            .then()
    }

    private fun resolveReply(ctx: MessageContext): Mono<String> {
        val allowBareAliases = ctx.isPrivateChat() || ctx.isFrontend()
        val command = BotCommand.from(ctx.text, allowBareAliases = allowBareAliases)
        if (command != null) {
            if (command == BotCommand.TIME && !ctx.supportsAlarmSetup()) {
                return Mono.just(
                    timeAlarmService.buildCurrentTimeMessage() +
                        "\nFrontend chat does not store Telegram alarms. Use /time in Telegram to continue alarm setup."
                )
            }
            val handler = commandHandlers[command] ?: return Mono.empty()
            return handler.handle(ctx) ?: Mono.empty()
        }

        val chatId = ctx.chatId ?: return echoHandler?.handle(ctx) ?: Mono.empty()
        val activeMode = conversationModeStore.get(chatId)

        if (activeMode == null && timeAlarmService.hasPendingSetup(chatId)) {
            if (!ctx.supportsAlarmSetup()) {
                timeAlarmService.cancelSetup(chatId)
                return Mono.just("Frontend chat does not continue Telegram alarm setup. Use /time in Telegram instead.")
            }
            return Mono.just(timeAlarmService.continueSetup(chatId, ctx.text.orEmpty()))
        }

        if (activeMode == null) {
            val text = ctx.text.orEmpty()
            val replyContext = ctx.replyContextSummary()
            log.info(
                "Routing general chat text to AI reply: chatId={}, chatType={}, source={}, hasReplyContext={}, text={}",
                chatId,
                ctx.chatType,
                ctx.inputSource,
                ctx.hasReplyContext(),
                text
            )
            return aiServerReply.reply(chatId.toString(), text, replyContext)
                .timeout(PRIVATE_CHAT_REPLY_TIMEOUT, Mono.just(privateChatReplyFallbackFactory.build(text)))
                .onErrorResume { e ->
                    log.warn("General chat fallback activated: chatId={}, chatType={}, source={}", chatId, ctx.chatType, ctx.inputSource, e)
                    Mono.just(privateChatReplyFallbackFactory.build(text))
                }
        }

        val text = ctx.text.orEmpty()
        val replyContext = ctx.replyContextSummary()
        log.info("Routing text to active conversation mode: chatId={}, mode={}, hasReplyContext={}, text={}", chatId, activeMode.aiMode, ctx.hasReplyContext(), text)
        if (isConversationExitText(text)) {
            conversationModeStore.clear(chatId)
            return Mono.just("${activeMode.label} ended. Returning to normal conversation.")
        }

        return aiServerModeChat.chat(chatId.toString(), text, activeMode, replyContext)
            .timeout(MODE_REPLY_TIMEOUT, Mono.just(fallbackReplyFactory.build(activeMode, text)))
            .onErrorResume { e ->
                log.warn("Mode chat fallback activated: chatId={}, mode={}", chatId, activeMode.aiMode, e)
                Mono.just(fallbackReplyFactory.build(activeMode, text))
            }
    }

    private fun isConversationExitText(text: String?): Boolean =
        text?.trim()?.equals(EXIT_KEYWORD, ignoreCase = true) == true

    private fun handleDocument(chatId: Long, message: TelegramUpdate.Message): Mono<Void> {
        val document = message.document ?: return Mono.empty()
        val fileId = document.fileId ?: return Mono.empty()
        val fileName = document.fileName ?: "document"
        log.info("Document received: fileId={}, fileName={}, size={}", fileId, fileName, document.fileSize)

        return telegramApi.downloadFileToLocal(fileId, fileName)!!
            .flatMap { localPath ->
                val bytes = try {
                    Files.readAllBytes(localPath)
                } catch (e: Exception) {
                    log.warn("Document read error", e)
                    return@flatMap telegramApi.sendMessage(chatId, "Failed to read document: ${e.message}")!!.then()
                }

                val telegramMessageId = message.messageId ?: 0L
                val fromUserName = message.senderDisplayName()
                    ?: message.from?.firstName?.takeIf { it.isNotBlank() }
                    ?: message.from?.username?.takeIf { it.isNotBlank() }
                    ?: "User"

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
                        "Document received.\nName: $fileName\nSize: ${document.fileSize ?: 0} bytes"
                    } else {
                        "An error occurred while saving the document."
                    }
                    telegramApi.sendMessage(chatId, reply)!!
                }
            }
            .onErrorResume { e -> telegramApi.sendMessage(chatId, "Document handling failed: ${e.message}")!! }
            .then()
    }

    private fun handlePhoto(chatId: Long, message: TelegramUpdate.Message): Mono<Void> {
        val photoList = message.photo ?: return Mono.empty()
        val largest = photoList.lastOrNull() ?: return Mono.empty()
        val fileId = largest.fileId ?: return Mono.empty()
        val fileName = "photo_${largest.fileUniqueId ?: message.messageId}.jpg"
        log.info("Photo received: fileId={}", fileId)

        return telegramApi.downloadFileToLocal(fileId, fileName)!!
            .flatMap { localPath ->
                val bytes = try {
                    Files.readAllBytes(localPath)
                } catch (e: Exception) {
                    log.warn("Photo read error", e)
                    return@flatMap telegramApi.sendMessage(chatId, "Failed to read photo: ${e.message}")!!.then()
                }

                val telegramMessageId = message.messageId ?: 0L
                val fromUserName = message.senderDisplayName()
                    ?: message.from?.firstName?.takeIf { it.isNotBlank() }
                    ?: message.from?.username?.takeIf { it.isNotBlank() }
                    ?: "User"

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
                        "Photo received: ${width}x${height}"
                    } else {
                        "An error occurred while saving the photo."
                    }
                    telegramApi.sendMessage(chatId, reply)!!
                }
            }
            .onErrorResume { e -> telegramApi.sendMessage(chatId, "Photo handling failed: ${e.message}")!! }
            .then()
    }

    private fun handleVoice(chatId: Long, message: TelegramUpdate.Message): Mono<Void> {
        val fileId = message.voice?.fileId ?: return Mono.empty()
        log.info("Voice received: fileId={}", fileId)

        return telegramApi.downloadFileToLocal(fileId, "voice_${message.messageId}.ogg")!!
            .flatMap { localPath -> telegramApi.sendMessage(chatId, "Voice message received. Saved at: $localPath")!! }
            .onErrorResume { e -> telegramApi.sendMessage(chatId, "Voice handling failed: ${e.message}")!! }
            .then()
    }

    private fun handleVideo(chatId: Long, message: TelegramUpdate.Message): Mono<Void> {
        val fileId = message.video?.fileId ?: return Mono.empty()
        val fileName = "video_${message.messageId}.mp4"
        log.info("Video received: fileId={}", fileId)

        return telegramApi.downloadFileToLocal(fileId, fileName)!!
            .flatMap { localPath -> telegramApi.sendMessage(chatId, "Video received. Saved at: $localPath")!! }
            .onErrorResume { e -> telegramApi.sendMessage(chatId, "Video handling failed: ${e.message}")!! }
            .then()
    }

    companion object {
        private const val EXIT_KEYWORD = "bye"
        private val MODE_REPLY_TIMEOUT: Duration = Duration.ofSeconds(45)
        private val PRIVATE_CHAT_REPLY_TIMEOUT: Duration = Duration.ofSeconds(45)
        private const val DEFAULT_REPLY_MESSAGE = "답변을 만드는 데 문제가 있었어요. 한 번만 다시 말씀해 주세요."
        private const val FALLBACK_MESSAGE = "텍스트나 파일(문서/사진/음성/동영상)을 보내 주세요."
    }
}
