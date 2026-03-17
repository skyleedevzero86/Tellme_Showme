package com.sleekydz86.tellme.aiserver.presentation.controller

import com.sleekydz86.tellme.aiserver.aplication.service.ChatService
import com.sleekydz86.tellme.aiserver.domain.model.ChatEntity
import com.sleekydz86.tellme.aiserver.presentation.dto.request.ChatRequest
import com.sleekydz86.tellme.aiserver.presentation.dto.request.ChatSendRequest
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/chat")
class ChatController(
    private val chatService: ChatService
) {

    @PostMapping("/ai")
    fun chatAi(@RequestBody body: ChatRequest) {
        chatService.streamOllama(ChatEntity.forOllama(body.currentUserName, body.message))
    }

    @PostMapping("/send")
    fun chatSend(@RequestBody body: ChatSendRequest) {
        chatService.streamRag(ChatEntity.forRag(body.currentUserName, body.message, body.useKnowledgeBase))
    }
}
