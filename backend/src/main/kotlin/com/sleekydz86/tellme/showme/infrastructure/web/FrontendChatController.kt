package com.sleekydz86.tellme.showme.infrastructure.web

import com.sleekydz86.tellme.showme.application.service.FrontendChatSessionIdCodec
import com.sleekydz86.tellme.showme.application.service.HandleUpdateService
import com.sleekydz86.tellme.showme.domain.InputSource
import com.sleekydz86.tellme.showme.domain.MessageContext
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class FrontendChatController(
    private val handleUpdateService: HandleUpdateService,
    private val sessionIdCodec: FrontendChatSessionIdCodec
) {

    @PostMapping(value = ["/chat_input.do"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun chatInput(@RequestBody request: FrontendChatRequest): Mono<ResponseEntity<FrontendChatResponse>> {
        val sessionId = request.sessionId.trim().ifBlank { "frontend-anonymous" }
        val text = request.message.trim()
        if (text.isBlank()) {
            return Mono.just(ResponseEntity.ok(FrontendChatResponse(sessionId, "Please enter a message.")))
        }

        val ctx = MessageContext(
            chatId = sessionIdCodec.toChatId(sessionId),
            text = text,
            firstName = request.displayName?.trim().takeUnless { it.isNullOrBlank() } ?: "Frontend user",
            chatType = "private",
            inputSource = InputSource.FRONTEND
        )

        return handleUpdateService.replyForContext(ctx)
            .map { reply -> ResponseEntity.ok(FrontendChatResponse(sessionId, reply)) }
    }
}

data class FrontendChatRequest(
    val sessionId: String = "",
    val message: String = "",
    val displayName: String? = null
)

data class FrontendChatResponse(
    val sessionId: String,
    val reply: String
)
