package com.sleekydz86.tellme.showme.application.service.handler

import com.sleekydz86.tellme.showme.domain.MessageContext
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class HelpCommandHandler : CommandHandler {
    override fun handle(ctx: MessageContext?): Mono<String> {
        return Mono.just(BODY)
    }

    companion object {
        private const val BODY = (
            "지원 기능:\n" +
                "- 일반 질문 : 명령어 없이 보내도 AI가 응답\n" +
                "- /start : 시작 메시지\n" +
                "- /time : 현재 시간 안내 후 알람 설정\n" +
                "- /alarmstop : 실행 중인 알람 중지\n" +
                "- /lotto : 로또 번호\n" +
                "- /eng : AiServer 영어 대화 모드 시작\n" +
                "- /god : AiServer 명언 대화 모드 시작\n" +
                "- /search 질문 : /channel 문서를 기반으로 검색\n" +
                "- /end : 대화 모드, 알람 설정, 활성 알람 종료\n" +
                "- bye : /eng 또는 /god 대화 모드 종료\n" +
                "- 문서/사진/음성/동영상 전송 : 파일 다운로드"
            )
    }
}
