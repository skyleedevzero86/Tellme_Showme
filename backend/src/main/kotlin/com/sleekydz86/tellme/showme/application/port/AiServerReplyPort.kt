package com.sleekydz86.tellme.showme.application.port

import reactor.core.publisher.Mono

interface AiServerReplyPort {
    fun reply(userId: String, message: String, replyContext: String? = null): Mono<String>
}
