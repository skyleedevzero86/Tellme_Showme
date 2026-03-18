package com.sleekydz86.tellme.showme.application.port

import reactor.core.publisher.Mono

interface AiServerUploadPort {
    fun upload(
        bytes: ByteArray,
        fileName: String,
        contentType: String?,
        userId: String,
        uploadSource: String,
        telegramMessageId: Long? = null,
        fromUserName: String? = null
    ): Mono<Boolean>
}
