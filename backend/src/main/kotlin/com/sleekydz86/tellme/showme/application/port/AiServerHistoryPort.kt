package com.sleekydz86.tellme.showme.application.port

import reactor.core.publisher.Mono

interface AiServerHistoryPort {
    fun getMessageHistory(page: Int, size: Int, search: String?): Mono<String>
    fun getFileHistory(page: Int, size: Int, search: String?): Mono<String>
}
