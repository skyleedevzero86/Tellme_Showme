package com.sleekydz86.tellme.aiserver.aplication.service

import com.sleekydz86.tellme.aiserver.aplication.service.impl.ChatServiceOllama
import com.sleekydz86.tellme.aiserver.aplication.service.impl.ChatServiceRag
import com.sleekydz86.tellme.aiserver.domain.model.ChatEntity
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

@Component
@Primary
class ChatServiceFacade(
    private val chatServiceOllama: ChatServiceOllama,
    private val chatServiceRag: ChatServiceRag
) : ChatService {

    override fun streamOllama(chatEntity: ChatEntity) = chatServiceOllama.streamOllama(chatEntity)

    override fun streamRag(chatEntity: ChatEntity) = chatServiceRag.streamRag(chatEntity)
}
