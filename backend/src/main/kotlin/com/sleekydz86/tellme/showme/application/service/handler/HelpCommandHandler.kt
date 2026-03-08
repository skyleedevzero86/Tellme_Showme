package com.sleekydz86.tellme.showme.application.service.handler

import com.sleekydz86.tellme.showme.domain.MessageContext
import reactor.core.publisher.Mono


class HelpCommandHandler : CommandHandler {
    override fun handle(ctx: MessageContext?): Mono<String> {
        return Mono.just(BODY)
    }

    companion object {
        private const val BODY = ("지원 기능:\n"
                + "• /start — 시작 메시지\n"
                + "• /time — 현재 시간\n"
                + "• /lotto — 예측 로또 번호\n"
                + "• /god — 성경 영문과 한글 번역\n"
                + "• /eng — 미국인이 가장 많이 쓰는 영어\n"
                + "• 문서/사진/음성/동영상 전송 시 파일 다운로드")
    }
}