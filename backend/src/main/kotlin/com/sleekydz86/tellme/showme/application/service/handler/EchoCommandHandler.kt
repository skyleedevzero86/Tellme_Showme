package com.sleekydz86.tellme.showme.application.service.handler

import com.sleekydz86.tellme.showme.domain.MessageContext
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class EchoCommandHandler : CommandHandler {
    override fun handle(ctx: MessageContext?): Mono<String> {
        return Mono.justOrEmpty(ctx?.text)
    }
}
