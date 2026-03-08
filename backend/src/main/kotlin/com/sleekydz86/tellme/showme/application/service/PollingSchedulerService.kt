package com.sleekydz86.tellme.showme.application.service

import com.sleekydz86.tellme.showme.application.port.TelegramApiPort
import com.sleekydz86.tellme.showme.domain.dto.TelegramUpdate
import lombok.RequiredArgsConstructor
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import kotlin.math.max


@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = ["telegram.polling.enabled"], havingValue = "true", matchIfMissing = true)
class PollingSchedulerService(
    private val telegramApi: TelegramApiPort,
    private val handleUpdateService: HandleUpdateService
) {
    private val log = LoggerFactory.getLogger(PollingSchedulerService::class.java)
    private var nextOffset: Long = 0

    @Scheduled(fixedDelayString = "\${telegram.polling.fixed-delay-ms:500}")
    fun poll() {
        if (telegramApi.isTokenMissing) return
        val mono = telegramApi.getUpdates(nextOffset) ?: return
        mono.flatMap { update: TelegramUpdate -> processResult(update) }
            .subscribe(
                { offset: Long -> nextOffset = offset },
                { err -> log.error("Poll error", err) }
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
            ur?.message?.let { msg ->
                handleUpdateService.handle(msg).subscribe(
                    { },
                    { e -> log.warn("Handle message error", e) }
                )
            }
        }
        return Mono.just(maxId)
    }
}
