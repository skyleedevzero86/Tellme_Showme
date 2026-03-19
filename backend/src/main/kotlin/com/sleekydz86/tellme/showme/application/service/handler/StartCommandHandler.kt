package com.sleekydz86.tellme.showme.application.service.handler

import com.sleekydz86.tellme.showme.domain.MessageContext
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class StartCommandHandler : CommandHandler {
    override fun handle(ctx: MessageContext?): Mono<String> {
        val name = ctx?.firstName ?: "사용자"
        return Mono.just(String.format(TEMPLATE, name))
    }

    companion object {
        private const val TEMPLATE =
            "%s님 안녕하세요. 사용 가능한 명령어는 /time, /alarmstop, /lotto, /god, /eng, /search, /end 입니다. " +
                "명령어 없이 일반 질문을 보내도 AI가 바로 응답할 수 있어요. " +
                "/time 은 현재 시각 안내 후 알람 설정까지 이어지고, 알람 문구도 직접 정할 수 있어요. " +
                "/eng 와 /god 는 대화 모드이며 bye 또는 /end 로 종료할 수 있습니다."
    }
}
