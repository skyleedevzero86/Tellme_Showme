package com.sleekydz86.tellme.showme.infrastructure.adapter

import com.sleekydz86.tellme.showme.application.port.AiServerUploadPort
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Component
class AiServerWebClientAdapter(
    @Qualifier("aiServerWebClient") private val webClient: WebClient
) : AiServerUploadPort {

    override fun upload(
        bytes: ByteArray,
        fileName: String,
        contentType: String?,
        userId: String,
        uploadSource: String
    ): Mono<Boolean> {
        val body: MultiValueMap<String, Any> = LinkedMultiValueMap()
        body.add("file", object : ByteArrayResource(bytes) {
            override fun getFilename(): String = fileName
        })
        return webClient.post()
            .uri("/rag/upload")
            .header("X-User-Id", userId)
            .header("X-Upload-Source", uploadSource)
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .bodyValue(body)
            .retrieve()
            .toBodilessEntity()
            .map { it.statusCode.is2xxSuccessful }
            .onErrorReturn(false)
    }
}
