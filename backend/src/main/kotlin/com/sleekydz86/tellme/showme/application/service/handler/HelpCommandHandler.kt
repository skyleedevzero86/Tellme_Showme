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
                "- /start : 시작 메시지\n" +
                "- /time : 현재 시간 안내 후 알람 설정\n" +
                "- /lotto : 로또 번호\n" +
                "- /eng : AiServer 영어 대화 모드 시작\n" +
                "- /god : AiServer 명언 대화 모드 시작\n" +
                "- /search 질문 : /channel 에 업로드한 문서 기반 검색\n" +
                "- /end : /eng, /god, 알람 설정 흐름 종료\n" +
                "- bye : /eng 또는 /god 대화 모드 종료\n" +
                "- 문서/사진/음성/동영상 전송 : 파일 다운로드"
            )
    }
}
