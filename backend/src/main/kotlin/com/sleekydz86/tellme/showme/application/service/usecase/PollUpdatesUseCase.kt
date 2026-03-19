package com.sleekydz86.tellme.showme.application.service.usecase

import com.sleekydz86.tellme.showme.application.port.TelegramApiPort
import com.sleekydz86.tellme.showme.application.service.HandleUpdateService
import com.sleekydz86.tellme.showme.application.service.TelegramPollingGuard
import com.sleekydz86.tellme.showme.domain.dto.TelegramUpdate
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import java.util.concurrent.atomic.AtomicLong

@Service
class PollUpdatesUseCase(
    private val telegramApi: TelegramApiPort,
    private val handleUpdateService: HandleUpdateService,
    private val pollingGuard: TelegramPollingGuard
) {
    private val log = LoggerFactory.getLogger(PollUpdatesUseCase::class.java)
    private val lastUpdateId = AtomicLong(0)
    private val objectMapper = ObjectMapper()

    fun pollAndProcess(): String? {
        if (telegramApi.isTokenMissing) {
            return jsonResult("텔레그램 토큰이 설정되지 않았습니다.")
        }

        val webhookUrl = telegramApi.getWebhookInfo()?.block()?.result?.url?.takeIf { it.isNotBlank() }
        if (webhookUrl != null) {
            return jsonResultWebhookActive()
        }

        if (!pollingGuard.tryAcquire(MANUAL_OWNER)) {
            return jsonResultPollingBusy()
        }

        return try {
            var result = jsonResult("신규 메시지가 없습니다.")
            for (attempt in 0 until MAX_ATTEMPTS) {
                val offset = if (lastUpdateId.get() > 0) lastUpdateId.get() + 1 else null
                val update = try {
                    telegramApi.getUpdates(offset)?.block()
                } catch (e: Exception) {
                    log.warn("getUpdates error", e)
                    return jsonResult("오류가 발생했습니다.")
                }

                if (update == null || update.result.isNullOrEmpty()) {
                    if (sleepInterruptible()) {
                        return jsonResult("오류가 발생했습니다.")
                    }
                    continue
                }

                val processedTexts = mutableListOf<String>()
                for (updateResult in update.result!!) {
                    val incomingMessage = updateResult?.incomingMessage() ?: continue
                    val updateId = updateResult.updateId ?: continue
                    if (updateId <= lastUpdateId.get()) {
                        continue
                    }

                    handleUpdateService.handle(incomingMessage).block()
                    incomingMessage.text?.let { processedTexts.add(it) }
                    lastUpdateId.set(updateId)
                }

                if (processedTexts.isNotEmpty()) {
                    result = jsonResult("수신한 메시지(${processedTexts.joinToString(", ")})를 처리했습니다.")
                    break
                }

                if (sleepInterruptible()) {
                    return jsonResult("오류가 발생했습니다.")
                }
            }
            result
        } finally {
            pollingGuard.release(MANUAL_OWNER)
        }
    }

    private fun sleepInterruptible(): Boolean {
        return try {
            Thread.sleep(SLEEP_MS.toLong())
            false
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            true
        }
    }

    private fun jsonResult(message: String?): String? {
        return try {
            objectMapper.writeValueAsString(mapOf("result" to (message ?: "")))
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

    private fun jsonResultPollingBusy(): String {
        val message = when (pollingGuard.currentOwner()) {
            SCHEDULER_OWNER -> POLLING_BUSY_BY_SCHEDULER_MESSAGE
            MANUAL_OWNER -> POLLING_BUSY_BY_MANUAL_MESSAGE
            else -> POLLING_BUSY_GENERIC_MESSAGE
        }
        return jsonResult(message) ?: ERROR_JSON
    }

    companion object {
        private const val MAX_ATTEMPTS = 10
        private const val SLEEP_MS = 5000
        private const val ERROR_JSON = "{\"result\":\"오류가 발생했습니다.\"}"
        private const val WEBHOOK_ACTIVE_MESSAGE =
            "웹훅 사용 중에는 getUpdates(폴링)를 사용할 수 없습니다. 웹훅 페이지에서 삭제 후 이용하세요."
        private const val POLLING_BUSY_BY_SCHEDULER_MESSAGE =
            "자동 텔레그램 polling이 이미 실행 중입니다. /get-updates 화면과 자동 polling 중 하나만 사용해 주세요."
        private const val POLLING_BUSY_BY_MANUAL_MESSAGE =
            "다른 getUpdates 요청이 이미 진행 중입니다. 잠시 후 다시 시도해 주세요."
        private const val POLLING_BUSY_GENERIC_MESSAGE =
            "다른 getUpdates 요청과 충돌했습니다. 잠시 후 다시 시도해 주세요."
        private const val MANUAL_OWNER = "manual"
        private const val SCHEDULER_OWNER = "scheduler"
    }
}
