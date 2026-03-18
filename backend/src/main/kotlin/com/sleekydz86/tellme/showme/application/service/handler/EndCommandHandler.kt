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

        val reply = when {
            mode != null && alarmSetupCancelled ->
                "${mode.label}와 알람 설정을 종료했어요. 이제 원래 대화로 돌아왔습니다."

            mode != null ->
                "${mode.label}를 종료했어요. 이제 원래 대화로 돌아왔습니다."

            alarmSetupCancelled ->
                "알람 설정을 종료했어요."

            else ->
                NO_ACTIVE_STATE_MESSAGE
        }

        return Mono.just(reply)
    }

    companion object {
        private const val NO_ACTIVE_STATE_MESSAGE = "현재 종료할 대화나 알람 설정이 없습니다."
    }
}
