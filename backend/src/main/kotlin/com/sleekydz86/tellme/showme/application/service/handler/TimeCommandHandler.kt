package com.sleekydz86.tellme.showme.application.service.handler

import com.sleekydz86.tellme.showme.application.service.TimeAlarmService
import com.sleekydz86.tellme.showme.domain.MessageContext
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class TimeCommandHandler(
    private val timeAlarmService: TimeAlarmService
) : CommandHandler {
    override fun handle(ctx: MessageContext?): Mono<String> {
        val chatId = ctx?.chatId ?: return Mono.just("채팅 정보를 찾지 못했습니다.")
        return Mono.just(timeAlarmService.startSetup(chatId))
    }
}
