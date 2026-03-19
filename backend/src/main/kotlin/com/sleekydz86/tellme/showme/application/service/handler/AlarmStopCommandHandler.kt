package com.sleekydz86.tellme.showme.application.service.handler

import com.sleekydz86.tellme.showme.application.service.TimeAlarmService
import com.sleekydz86.tellme.showme.domain.MessageContext
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class AlarmStopCommandHandler(
    private val timeAlarmService: TimeAlarmService
) : CommandHandler {

    override fun handle(ctx: MessageContext?): Mono<String> {
        if (ctx == null || !ctx.supportsAlarmSetup()) {
            return Mono.just("프론트 채팅에서는 텔레그램 알람을 중지할 수 없어요. 텔레그램에서 /alarmstop 을 입력해 주세요.")
        }

        val chatId = ctx.chatId ?: return Mono.just("채팅 정보를 찾지 못했어요.")
        val stopped = timeAlarmService.stopActiveAlarms(chatId)

        return if (stopped > 0) {
            Mono.just("활성 알람 ${stopped}개를 중지했어요.")
        } else {
            Mono.just("현재 중지할 활성 알람이 없어요.")
        }
    }
}
