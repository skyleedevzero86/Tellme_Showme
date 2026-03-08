package com.sleekydz86.tellme.showme.application.service.handler

import com.sleekydz86.tellme.showme.domain.MessageContext
import reactor.core.publisher.Mono


class StartCommandHandler : CommandHandler {
    override fun handle(ctx: MessageContext?): Mono<String> {
        val name = ctx?.firstName ?: "사용자"
        return Mono.just(String.format(TEMPLATE, name))
    }

    companion object {
        private const val TEMPLATE = "%s님 안녕하세요. 사용 가능한 명령은 /time, /lotto, /god, /eng 입니다."
    }
}