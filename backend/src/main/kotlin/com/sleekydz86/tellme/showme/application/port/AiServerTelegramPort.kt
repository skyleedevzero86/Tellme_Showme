package com.sleekydz86.tellme.showme.application.port

import reactor.core.publisher.Mono
import java.time.Instant

interface AiServerTelegramPort {
    fun saveMessage(
        telegramMessageId: Long,
        chatId: Long,
        fromUserId: Long,
        fromUserName: String?,
        text: String?,
        receivedAt: Instant
    ): Mono<Boolean>
}
