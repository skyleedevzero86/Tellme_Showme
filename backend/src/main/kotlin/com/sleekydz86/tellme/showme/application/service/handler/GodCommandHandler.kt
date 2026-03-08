package com.sleekydz86.tellme.showme.application.service.handler

import com.sleekydz86.tellme.showme.application.port.ExternalContentPort
import com.sleekydz86.tellme.showme.domain.MessageContext
import reactor.core.publisher.Mono


class GodCommandHandler(private val externalContent: ExternalContentPort) : CommandHandler {
    override fun handle(ctx: MessageContext?): Mono<String> {
        return Mono.justOrEmpty(externalContent.bible)
    }
}
