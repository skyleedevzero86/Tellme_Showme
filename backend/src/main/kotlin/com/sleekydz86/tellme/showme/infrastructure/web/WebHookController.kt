package com.sleekydz86.tellme.showme.infrastructure.web

import com.sleekydz86.tellme.global.config.TelegramBotProperties
import com.sleekydz86.tellme.showme.application.service.HandleUpdateService
import com.sleekydz86.tellme.showme.application.service.usecase.ChannelBroadcastUseCase
import com.sleekydz86.tellme.showme.application.service.usecase.PollUpdatesUseCase
import com.sleekydz86.tellme.showme.application.service.usecase.SetWebhookUseCase
import com.sleekydz86.tellme.showme.application.port.TelegramApiPort
import com.sleekydz86.tellme.showme.domain.dto.SendMessageResponse
import com.sleekydz86.tellme.showme.domain.dto.TelegramSendResponse
import com.sleekydz86.tellme.showme.domain.dto.WebhookUpdate
import com.sleekydz86.tellme.showme.domain.dto.WebhookInfoResponse
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import reactor.core.publisher.Mono

@RestController
class WebHookController(
    private val setWebhookUseCase: SetWebhookUseCase,
    private val telegramApi: TelegramApiPort,
    private val handleUpdateService: HandleUpdateService,
    private val pollUpdatesUseCase: PollUpdatesUseCase,
    private val channelBroadcastUseCase: ChannelBroadcastUseCase,
    private val properties: TelegramBotProperties
) {
    private val log = LoggerFactory.getLogger(WebHookController::class.java)

    @GetMapping(value = ["/webhook_status.do"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun webhookStatus(): ResponseEntity<Map<String, Any>> {
        val url = properties.api.webhookUrl
        val configured = url.isNotBlank()
        return ResponseEntity.ok(mapOf(
            "webhookUrlConfigured" to configured,
            "webhookUrl" to (url.ifBlank { "" })
        ))
    }

    @GetMapping(value = ["/webhook_info.do"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun webhookInfo(): Mono<ResponseEntity<WebhookInfoResponse>> {
        return (telegramApi.getWebhookInfo() ?: Mono.empty())
            .map { ResponseEntity.ok(it) }
            .defaultIfEmpty(ResponseEntity.ok(WebhookInfoResponse(ok = false, result = null)))
    }

    @GetMapping(value = ["/webhook_delete.do"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun deleteWebhook(): Mono<ResponseEntity<TelegramSendResponse>> {
        return (telegramApi.deleteWebhook() ?: Mono.empty())
            .map { ResponseEntity.ok(it) }
            .defaultIfEmpty(ResponseEntity.ok(TelegramSendResponse(ok = false, description = "Not available", result = null)))
    }

    @GetMapping(value = ["/callback.do"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun callbackGet(): ResponseEntity<String> {
        return ResponseEntity.ok("OK")
    }

    @GetMapping(value = ["/webHook.do"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun setWebHook(
        @RequestParam("enabled") enabled: Boolean,
        @RequestParam("url", required = false) url: String?
    ): Mono<ResponseEntity<TelegramSendResponse>> {
        return setWebhookUseCase.setWebhook(enabled, url)
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
    fun fileUpload(
        @RequestParam("filename") file: MultipartFile?,
        @RequestParam(value = "caption", required = false) caption: String?
    ): Mono<ResponseEntity<SendMessageResponse>> {
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
        return channelBroadcastUseCase.sendPhoto(channel, bytes, fileName, caption)
            .map { ResponseEntity.ok(it) }
    }

    @PostMapping(
        value = ["/document_upload.do"],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun documentUpload(
        @RequestParam("filename") file: MultipartFile?,
        @RequestParam(value = "caption", required = false) caption: String?
    ): Mono<ResponseEntity<SendMessageResponse>> {
        val channel: String? = properties.channelUsername
        if (file == null || file.isEmpty) {
            return Mono.just(ResponseEntity.ok().body(SendMessageResponse("fail", null)))
        }
        val fileName = file.originalFilename ?: "document"
        val bytes: ByteArray
        try {
            bytes = file.bytes
        } catch (e: Exception) {
            log.warn("document_upload read error", e)
            return Mono.just(ResponseEntity.ok().body(SendMessageResponse("fail", null)))
        }
        return channelBroadcastUseCase.sendDocument(channel, bytes, fileName, caption)
            .map { ResponseEntity.ok(it) }
    }

    @PostMapping(value = ["/callback.do"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun callback(@RequestBody(required = false) update: WebhookUpdate?): Mono<ResponseEntity<String>> {
        log.info("Webhook callback received: update_id={}, message.text={}", update?.updateId, update?.message?.text)
        val ok = ResponseEntity.ok("ok")
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
