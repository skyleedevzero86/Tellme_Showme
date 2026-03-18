package com.sleekydz86.tellme.showme.application.port

import reactor.core.publisher.Mono

interface AiServerSearchPort {
    fun search(userId: String, message: String): Mono<String>
}
