package com.sleekydz86.tellme.showme.application.service

import com.sleekydz86.tellme.showme.application.port.TelegramApiPort
import com.sleekydz86.tellme.showme.domain.dto.TelegramUpdate
import lombok.RequiredArgsConstructor
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import kotlin.math.max
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong


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
    private val pollInProgress = AtomicBoolean(false)
    private val suspendedUntilEpochMs = AtomicLong(0)

    @Scheduled(fixedDelayString = "\${telegram.polling.fixed-delay-ms:500}")
    fun poll() {
        if (telegramApi.isTokenMissing) return
        if (System.currentTimeMillis() < suspendedUntilEpochMs.get()) return
        if (!pollInProgress.compareAndSet(false, true)) return
        if (!pollingGuard.tryAcquire(SCHEDULER_OWNER)) {
            pollInProgress.set(false)
            return
        }
        val mono = telegramApi.getUpdates(nextOffset)
        if (mono == null) {
            pollInProgress.set(false)
            pollingGuard.release(SCHEDULER_OWNER)
            return
        }
        mono.flatMap { update: TelegramUpdate -> processResult(update) }
            .doFinally {
                pollInProgress.set(false)
                pollingGuard.release(SCHEDULER_OWNER)
            }
            .subscribe(
                { offset: Long ->
                    nextOffset = offset
                    suspendedUntilEpochMs.set(0)
                },
                { err -> handlePollError(err) }
            )
    }

    private fun processResult(update: TelegramUpdate): Mono<Long> {
        val resultList = update.result
        if (resultList.isNullOrEmpty()) {
            val offset: Long = nextOffset
            return Mono.just(offset)
        }
        var maxId: Long = nextOffset
        for (ur in resultList) {
            if (ur?.updateId != null) {
                maxId = max(maxId, ur.updateId + 1)
            }
            ur?.incomingMessage()?.let { msg ->
                handleUpdateService.handle(msg).subscribe(
                    { },
                    { e -> log.warn("Handle message error", e) }
                )
            }
        }
        return Mono.just(maxId)
    }

    private fun handlePollError(err: Throwable) {
        if (err is WebClientResponseException && err.statusCode.value() == 409) {
            suspendPollingOnConflict(err)
            return
        }
        log.error("Poll error", err)
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
