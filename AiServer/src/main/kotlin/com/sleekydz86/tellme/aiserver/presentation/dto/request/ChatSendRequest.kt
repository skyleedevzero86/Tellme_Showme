package com.sleekydz86.tellme.aiserver.presentation.dto.request

data class ChatSendRequest(
    val currentUserName: String,
    val message: String,
    val useKnowledgeBase: Boolean = false
)
