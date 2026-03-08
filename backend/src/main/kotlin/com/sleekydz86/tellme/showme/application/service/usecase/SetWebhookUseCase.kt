package com.sleekydz86.tellme.showme.application.service.usecase

import com.sleekydz86.tellme.showme.application.port.TelegramApiPort
import com.sleekydz86.tellme.showme.domain.dto.TelegramSendResponse
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono


@Service
class SetWebhookUseCase(
    private val telegramApi: TelegramApiPort
) {
    fun setWebhook(enabled: Boolean, urlOverride: String? = null): Mono<TelegramSendResponse> {
        return telegramApi.setWebhook(enabled, urlOverride)!!
            .onErrorResume { e -> Mono.just(errorResponse(e.message)) }
    }

    companion object {
        private fun errorResponse(description: String?): TelegramSendResponse =
            TelegramSendResponse(ok = false, description = description, result = null)
    }
}
