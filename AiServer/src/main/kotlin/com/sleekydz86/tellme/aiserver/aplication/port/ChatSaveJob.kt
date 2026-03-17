package com.sleekydz86.tellme.aiserver.aplication.port

data class ChatSaveJob(
    val userId: String,
    val userMessage: String,
    val assistantMessage: String,
    val mode: String,
    val createdAt: Long = System.currentTimeMillis()
)
