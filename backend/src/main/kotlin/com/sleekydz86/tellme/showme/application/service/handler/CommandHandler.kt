package com.sleekydz86.tellme.showme.application.service.handler

import com.sleekydz86.tellme.showme.domain.MessageContext
import reactor.core.publisher.Mono

interface CommandHandler {
    fun handle(ctx: MessageContext?): Mono<String>?
}
