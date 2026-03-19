package com.sleekydz86.tellme.showme.application.service

import com.sleekydz86.tellme.showme.application.port.TelegramApiPort
import com.sleekydz86.tellme.showme.domain.dto.TelegramUpdate
import lombok.RequiredArgsConstructor
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = ["telegram.polling.enabled"], havingValue = "true", matchIfMissing = true)
class PollingSchedulerService(
    private val telegramApi: TelegramApiPort,
    private val handleUpdateService: HandleUpdateService,
    private val pollingGuard: TelegramPollingGuard
) {
    private val log = LoggerFactory.getLogger(PollingSchedulerService::class.java)
    private var nextOffset: Long = 0
    private val suspendedUntilEpochMs = AtomicLong(0)

    @Scheduled(fixedDelayString = "\${telegram.polling.fixed-delay-ms:500}")
    fun poll() {
        if (telegramApi.isTokenMissing) return
        if (System.currentTimeMillis() < suspendedUntilEpochMs.get()) return
        if (!pollingGuard.tryAcquire(SCHEDULER_OWNER)) return

        try {
            val update = telegramApi.getUpdates(nextOffset)?.block() ?: return
            nextOffset = processResult(update)
            suspendedUntilEpochMs.set(0)
        } catch (err: WebClientResponseException) {
            if (err.statusCode.value() == 409) {
                suspendPollingOnConflict(err)
            } else {
                log.error("Poll error", err)
            }
        } catch (err: Exception) {
            log.error("Poll error", err)
        } finally {
            pollingGuard.release(SCHEDULER_OWNER)
        }
    }

    private fun processResult(update: TelegramUpdate): Long {
        val resultList = update.result
        if (resultList.isNullOrEmpty()) {
            return nextOffset
        }

        var maxId = nextOffset
        for (updateResult in resultList) {
            val updateId = updateResult?.updateId
            if (updateId != null) {
                maxId = max(maxId, updateId + 1)
            }

            val message = updateResult?.incomingMessage() ?: continue
            try {
                handleUpdateService.handle(message).block()
            } catch (e: Exception) {
                log.warn("Handle message error: updateId={}", updateId, e)
            }
        }
        return maxId
    }

    private fun suspendPollingOnConflict(err: WebClientResponseException) {
        val description = extractConflictDescription(err.responseBodyAsString)
        val normalized = description.lowercase()
        val backoffMs = when {
            normalized.contains("webhook") -> WEBHOOK_CONFLICT_BACKOFF_MS
            normalized.contains("terminated by other getupdates request") ||
                normalized.contains("only one bot instance") -> MULTIPLE_POLLER_CONFLICT_BACKOFF_MS
            else -> DEFAULT_CONFLICT_BACKOFF_MS
        }

        suspendedUntilEpochMs.set(System.currentTimeMillis() + backoffMs)

        val summary = when {
            normalized.contains("webhook") ->
                "Telegram webhook is active, so scheduled polling will pause briefly."
            normalized.contains("terminated by other getupdates request") ||
                normalized.contains("only one bot instance") ->
                "Another getUpdates consumer is already running, so scheduled polling will pause briefly."
            else ->
                "Telegram getUpdates returned 409 Conflict, so scheduled polling will pause briefly."
        }

        log.warn("{} Retry in {} ms. {}", summary, backoffMs, description)
    }

    private fun extractConflictDescription(responseBody: String?): String {
        val body = responseBody.orEmpty()
        val description = Regex("\"description\"\\s*:\\s*\"([^\"]+)\"")
            .find(body)
            ?.groupValues
            ?.getOrNull(1)

        return description?.takeIf { it.isNotBlank() }
            ?: body.ifBlank { "409 Conflict from Telegram getUpdates" }
    }

    companion object {
        private const val DEFAULT_CONFLICT_BACKOFF_MS = 15_000L
        private const val MULTIPLE_POLLER_CONFLICT_BACKOFF_MS = 10_000L
        private const val WEBHOOK_CONFLICT_BACKOFF_MS = 60_000L
        private const val SCHEDULER_OWNER = "scheduler"
    }
}
