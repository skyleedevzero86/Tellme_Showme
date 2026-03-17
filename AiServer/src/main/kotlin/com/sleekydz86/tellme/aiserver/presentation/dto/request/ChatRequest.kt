package com.sleekydz86.tellme.aiserver.presentation.dto.request

data class ChatRequest(
    val currentUserName: String,
    val message: String
)
