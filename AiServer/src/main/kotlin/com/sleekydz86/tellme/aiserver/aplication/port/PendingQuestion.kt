package com.sleekydz86.tellme.aiserver.aplication.port

data class PendingQuestion(
    val userId: String,
    val question: String,
    val mode: String,
    val createdAt: Long
)