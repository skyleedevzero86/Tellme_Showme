package com.sleekydz86.tellme.showme.infrastructure.web

import com.sleekydz86.tellme.global.config.TelegramBotProperties
import com.sleekydz86.tellme.showme.application.service.HandleUpdateService
import com.sleekydz86.tellme.showme.application.service.usecase.ChannelBroadcastUseCase
import com.sleekydz86.tellme.showme.application.service.usecase.PollUpdatesUseCase
import com.sleekydz86.tellme.showme.application.service.usecase.SetWebhookUseCase
import com.sleekydz86.tellme.showme.domain.dto.SendMessageResponse
import com.sleekydz86.tellme.showme.domain.dto.TelegramSendResponse
import com.sleekydz86.tellme.showme.domain.dto.WebhookUpdate
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import reactor.core.publisher.Mono

@RestController
class WebHookController(
    private val setWebhookUseCase: SetWebhookUseCase,
    private val handleUpdateService: HandleUpdateService,
    private val pollUpdatesUseCase: PollUpdatesUseCase,
    private val channelBroadcastUseCase: ChannelBroadcastUseCase,
    private val properties: TelegramBotProperties
) {
    private val log = LoggerFactory.getLogger(WebHookController::class.java)

    @GetMapping(value = ["/webHook.do"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun setWebHook(@RequestParam("enabled") enabled: Boolean): Mono<ResponseEntity<TelegramSendResponse>> {
        return setWebhookUseCase.setWebhook(enabled)
            .map { ResponseEntity.ok(it) }
    }

    @get:GetMapping(value = ["/get_updates.do"], produces = [MediaType.APPLICATION_JSON_VALUE])
    val updates: ResponseEntity<String>
        get() {
            val json: String = (pollUpdatesUseCase.pollAndProcess() ?: "{}") as String
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json)
        }

    @PostMapping(value = ["/send_message.do"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun sendMessage(@RequestParam("message") message: String?): Mono<ResponseEntity<SendMessageResponse>> {
        val channel: String? = properties.channelUsername
        return channelBroadcastUseCase.sendMessage(channel, message)
            .map { ResponseEntity.ok(it) }
    }

    @PostMapping(
        value = ["/file_upload.do"],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun fileUpload(@RequestParam("filename") file: MultipartFile?): Mono<ResponseEntity<SendMessageResponse>> {
        val channel: String? = properties.channelUsername
        if (file == null || file.isEmpty) {
            return Mono.just(ResponseEntity.ok().body(SendMessageResponse("fail", null)))
        }
        val fileName = file.originalFilename ?: "photo.jpg"
        val bytes: ByteArray
        try {
            bytes = file.bytes
        } catch (e: Exception) {
            log.warn("file_upload read error", e)
            return Mono.just(ResponseEntity.ok().body(SendMessageResponse("fail", null)))
        }
        return channelBroadcastUseCase.sendPhoto(channel, bytes, fileName)
            .map { ResponseEntity.ok(it) }
    }

    @PostMapping(value = ["/callback.do"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun callback(@RequestBody update: WebhookUpdate?): Mono<ResponseEntity<Void>> {
        val ok = ResponseEntity.ok().build<Void>()
        if (update == null || update.message == null) {
            return Mono.just(ok)
        }
        return handleUpdateService.handle(update.message)
            .then(Mono.just(ok))
            .onErrorResume { e ->
                log.error("Callback handle error", e)
                Mono.just(ok)
            }
    }
}
