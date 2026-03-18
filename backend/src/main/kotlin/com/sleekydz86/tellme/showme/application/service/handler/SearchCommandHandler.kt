package com.sleekydz86.tellme.showme.application.service.handler

import com.sleekydz86.tellme.showme.application.port.AiServerSearchPort
import com.sleekydz86.tellme.showme.domain.MessageContext
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class SearchCommandHandler(
    private val aiServerSearch: AiServerSearchPort
) : CommandHandler {

    override fun handle(ctx: MessageContext?): Mono<String> {
        val query = ctx?.commandArgument()
        if (query.isNullOrBlank()) {
            return Mono.just(USAGE)
        }

        val userId = ctx.chatId?.toString() ?: ctx.firstName ?: "telegram-user"
        return aiServerSearch.search(userId, query)
            .map { if (it.isBlank()) EMPTY_RESULT else it }
    }

    companion object {
        private const val USAGE =
            "사용법: /search 질문\n먼저 http://localhost:3000/channel 에서 문서를 업로드한 뒤 질문해 주세요."

        private const val EMPTY_RESULT = "업로드된 문서에서 답변을 찾지 못했습니다."
    }
}
