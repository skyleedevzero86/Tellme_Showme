package com.sleekydz86.tellme.showme.application.service.usecase

import com.sleekydz86.tellme.showme.application.port.TelegramApiPort
import com.sleekydz86.tellme.showme.domain.dto.SendMessageResponse
import com.sleekydz86.tellme.showme.domain.dto.TelegramSendResponse
import lombok.RequiredArgsConstructor
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
@RequiredArgsConstructor
class ChannelBroadcastUseCase(
    private val telegramApi: TelegramApiPort
) {
    fun sendMessage(channelUsername: String?, message: String?): Mono<SendMessageResponse> {
        if (channelUsername.isNullOrBlank()) {
            return Mono.just(SendMessageResponse(STATUS_FAIL, null))
        }
        val text = message?.trim() ?: ""
        if (text.isEmpty()) {
            return Mono.just(SendMessageResponse(STATUS_FAIL, null))
        }
        return telegramApi.sendMessageToChannel(channelUsername, text)!!
            .map { res: TelegramSendResponse ->
                if (res.ok == true) SendMessageResponse(STATUS_OK, text)
                else SendMessageResponse(STATUS_FAIL, null)
            }
            .onErrorResume { Mono.just(SendMessageResponse(STATUS_FAIL, null)) }
    }

    fun sendPhoto(channelUsername: String?, bytes: ByteArray?, fileName: String?, caption: String?): Mono<SendMessageResponse> {
        if (channelUsername.isNullOrBlank() || bytes == null || bytes.isEmpty()) {
            return Mono.just(SendMessageResponse(STATUS_FAIL, null))
        }
        val name = fileName?.takeIf { it.isNotBlank() } ?: "photo.jpg"
        val cap = caption?.take(1024)?.takeIf { it.isNotBlank() }
        return telegramApi.sendPhotoToChannel(
            channelUsername,
            cap,
            name,
            bytes
        )!!
            .map { res: TelegramSendResponse ->
                if (res.ok == true) SendMessageResponse(STATUS_OK, null)
                else SendMessageResponse(STATUS_FAIL, null)
            }
            .onErrorResume { Mono.just(SendMessageResponse(STATUS_FAIL, null)) }
    }

    fun sendDocument(channelUsername: String?, bytes: ByteArray?, fileName: String?, caption: String?): Mono<SendMessageResponse> {
        if (channelUsername.isNullOrBlank() || bytes == null || bytes.isEmpty()) {
            return Mono.just(SendMessageResponse(STATUS_FAIL, null))
        }
        val name = fileName?.takeIf { it.isNotBlank() } ?: "document"
        val cap = caption?.take(1024)?.takeIf { it.isNotBlank() }
        return telegramApi.sendDocumentToChannel(channelUsername, cap, name, bytes)!!
            .map { res: TelegramSendResponse ->
                if (res.ok == true) SendMessageResponse(STATUS_OK, null)
                else SendMessageResponse(STATUS_FAIL, null)
            }
            .onErrorResume { Mono.just(SendMessageResponse(STATUS_FAIL, null)) }
    }

    companion object {
        private const val STATUS_OK = "ok"
        private const val STATUS_FAIL = "fail"
    }
}