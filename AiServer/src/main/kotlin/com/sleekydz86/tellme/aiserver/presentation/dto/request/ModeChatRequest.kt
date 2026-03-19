package com.sleekydz86.tellme.aiserver.presentation.dto.request

data class ModeChatRequest(
    val currentUserName: String,
    val message: String,
    val mode: String,
    val replyContext: String? = null
)
