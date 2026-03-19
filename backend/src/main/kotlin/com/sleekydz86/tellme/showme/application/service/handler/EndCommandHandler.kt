package com.sleekydz86.tellme.showme.application.service.handler

import com.sleekydz86.tellme.showme.application.service.TelegramConversationModeStore
import com.sleekydz86.tellme.showme.application.service.TimeAlarmService
import com.sleekydz86.tellme.showme.domain.MessageContext
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class EndCommandHandler(
    private val conversationModeStore: TelegramConversationModeStore,
    private val timeAlarmService: TimeAlarmService
) : CommandHandler {

    override fun handle(ctx: MessageContext?): Mono<String> {
        val chatId = ctx?.chatId ?: return Mono.just(NO_ACTIVE_STATE_MESSAGE)
        val mode = conversationModeStore.clear(chatId)
        val alarmSetupCancelled = timeAlarmService.cancelSetup(chatId)
        val stoppedAlarmCount = if (ctx.supportsAlarmSetup()) timeAlarmService.stopActiveAlarms(chatId) else 0

        val reply = buildString {
            if (mode != null) {
                append("${mode.label}를 종료했어요.")
            }
            if (alarmSetupCancelled) {
                if (isNotEmpty()) append(" ")
                append("알람 설정을 취소했어요.")
            }
            if (stoppedAlarmCount > 0) {
                if (isNotEmpty()) append(" ")
                append("활성 알람 ${stoppedAlarmCount}개도 함께 중지했어요.")
            }
        }.ifBlank { NO_ACTIVE_STATE_MESSAGE }

        return Mono.just(reply)
    }

    companion object {
        private const val NO_ACTIVE_STATE_MESSAGE = "현재 종료할 대화나 알람 설정이 없어요."
    }
}
