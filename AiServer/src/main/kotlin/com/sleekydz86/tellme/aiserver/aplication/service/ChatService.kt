package com.sleekydz86.tellme.aiserver.aplication.service

import com.sleekydz86.tellme.aiserver.domain.model.ChatEntity

interface ChatService {
    fun streamOllama(chatEntity: ChatEntity)
    fun streamRag(chatEntity: ChatEntity)
}
