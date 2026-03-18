package com.sleekydz86.tellme.showme.application.service.usecase

import com.sleekydz86.tellme.showme.application.port.TelegramApiPort
import com.sleekydz86.tellme.showme.application.service.HandleUpdateService
import com.sleekydz86.tellme.showme.domain.dto.TelegramUpdate
import org.slf4j.LoggerFactory
import org.springframework.web.reactive.function.client.WebClientResponseException
import tools.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicLong


@Service
class PollUpdatesUseCase(
    private val telegramApi: TelegramApiPort,
    private val handleUpdateService: HandleUpdateService
) {
    private val log = LoggerFactory.getLogger(PollUpdatesUseCase::class.java)
    private val lastUpdateId = AtomicLong(0)
    private val objectMapper = ObjectMapper()

    fun pollAndProcess(): String? {
        if (telegramApi.isTokenMissing) {
            return jsonResult("봇 토큰이 설정되지 않았습니다.")
        }
        val webhookUrl = telegramApi.getWebhookInfo()?.block()?.result?.url?.takeIf { it.isNotBlank() }
        if (webhookUrl != null) {
            return jsonResultWebhookActive()
        }
        var result = jsonResult("신규 메시지 없음")
        for (i in 0..<MAX_ATTEMPTS) {
            val offset = if (lastUpdateId.get() > 0) lastUpdateId.get() + 1 else null
            val update: TelegramUpdate?
            try {
                update = telegramApi.getUpdates(offset)?.block()
            } catch (e: Exception) {
                if (e is WebClientResponseException && e.statusCode.value() == 409) {
                    return jsonResultWebhookActive()
                }
                log.warn("getUpdates error", e)
                return jsonResult("오류")
            }
            if (update == null || update.result == null || update.result.isEmpty()) {
                if (sleepInterruptible()) {
                    return jsonResult("오류")
                }
                continue
            }

            val processedTexts = mutableListOf<String>()
            for (ur in update.result!!) {
                val u = ur ?: continue
                val incomingMessage = u.incomingMessage()
                if (u.updateId != null && u.updateId > lastUpdateId.get() && incomingMessage != null) {
                    handleUpdateService.handle(incomingMessage).block()
                    incomingMessage.text?.let { processedTexts.add(it) }
                    lastUpdateId.set(u.updateId)
                }
            }
            if (processedTexts.isNotEmpty()) {
                result = jsonResult("수신된 메시지(" + processedTexts.joinToString(", ") + ")를 처리함.")
                break
            }
            if (sleepInterruptible()) {
                return jsonResult("오류")
            }
        }
        return result
    }

    private fun sleepInterruptible(): Boolean {
        try {
            Thread.sleep(SLEEP_MS.toLong())
            return false
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            return true
        }
    }

    private fun jsonResult(message: String?): String? {
        return try {
            objectMapper.writeValueAsString(mapOf<String, String>("result" to (message ?: "")))
        } catch (e: tools.jackson.core.JacksonException) {
            ERROR_JSON
        }
    }

    private fun jsonResultWebhookActive(): String {
        return try {
            objectMapper.writeValueAsString(
                mapOf(
                    "result" to WEBHOOK_ACTIVE_MESSAGE,
                    "webhookActive" to true
                )
            )
        } catch (e: tools.jackson.core.JacksonException) {
            ERROR_JSON
        }
    }

    companion object {
        private const val MAX_ATTEMPTS = 10
        private const val SLEEP_MS = 5000
        private const val ERROR_JSON = "{\"result\" : \"오류\"}"
        private const val WEBHOOK_ACTIVE_MESSAGE = "웹후크 사용 중에는 getUpdates(폴링)를 사용할 수 없습니다. 웹후크 페이지에서 삭제 후 이용하세요."
    }
}
