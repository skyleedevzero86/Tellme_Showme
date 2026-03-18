package com.sleekydz86.tellme.showme.application.port

import reactor.core.publisher.Mono

interface AiServerHistoryPort {
    fun getMessageHistory(page: Int, size: Int, search: String?): Mono<String>
    fun getFileHistory(page: Int, size: Int, search: String?): Mono<String>
    fun getFilePreview(objectKey: String): Mono<FilePreviewPayload>
}

data class FilePreviewPayload(
    val bytes: ByteArray,
    val contentType: String?,
    val contentDisposition: String?
)
