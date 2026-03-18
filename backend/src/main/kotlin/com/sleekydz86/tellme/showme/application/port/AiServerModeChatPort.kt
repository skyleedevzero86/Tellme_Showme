package com.sleekydz86.tellme.showme.application.port

import com.sleekydz86.tellme.showme.domain.ConversationMode
import reactor.core.publisher.Mono

interface AiServerModeChatPort {
    fun chat(userId: String, message: String, mode: ConversationMode): Mono<String>
}
