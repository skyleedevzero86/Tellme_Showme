package com.sleekydz86.tellme.aiserver.presentation.controller

import com.sleekydz86.tellme.aiserver.aplication.port.RedisSearchPort
import com.sleekydz86.tellme.aiserver.aplication.service.ChatService
import com.sleekydz86.tellme.aiserver.aplication.service.ModeAnswerService
import com.sleekydz86.tellme.aiserver.aplication.service.RagAnswerService
import com.sleekydz86.tellme.aiserver.domain.model.ChatEntity
import com.sleekydz86.tellme.aiserver.presentation.dto.request.ChatRequest
import com.sleekydz86.tellme.aiserver.presentation.dto.request.ChatSendRequest
import com.sleekydz86.tellme.aiserver.presentation.dto.request.ModeChatRequest
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/chat")
class ChatController(
    private val chatService: ChatService,
    private val ragAnswerService: RagAnswerService,
    private val modeAnswerService: ModeAnswerService,
    private val redisSearchPort: RedisSearchPort
) {

    @PostMapping("/ai")
    fun chatAi(@RequestBody body: ChatRequest) {
        chatService.streamOllama(ChatEntity.forOllama(body.currentUserName, body.message))
    }

    @PostMapping("/send")
    fun chatSend(@RequestBody body: ChatSendRequest) {
        chatService.streamRag(ChatEntity.forRag(body.currentUserName, body.message, body.useKnowledgeBase))
    }

    @PostMapping("/search", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun chatSearch(@RequestBody body: ChatSendRequest): String {
        redisSearchPort.incrementQueryScore(body.message)
        return ragAnswerService.answer(
            currentUserName = body.currentUserName,
            question = body.message,
            useKnowledgeBase = true,
            strictKnowledgeBase = true
        )
    }

    @PostMapping("/reply", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun chatReply(@RequestBody body: ChatSendRequest): String {
        return ragAnswerService.answer(
            currentUserName = body.currentUserName,
            question = body.message,
            useKnowledgeBase = body.useKnowledgeBase,
            strictKnowledgeBase = false
        )
    }

    @PostMapping("/mode", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun chatMode(@RequestBody body: ModeChatRequest): String {
        return modeAnswerService.answer(
            currentUserName = body.currentUserName,
            message = body.message,
            mode = body.mode
        )
    }
}
