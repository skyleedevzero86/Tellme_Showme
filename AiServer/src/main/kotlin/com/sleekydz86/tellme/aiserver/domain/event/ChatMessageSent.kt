package com.sleekydz86.tellme.aiserver.domain.event

import java.time.Instant

data class ChatMessageSent(
    val userId: String,
    val message: String,
    val useKnowledge: Boolean,
    val mode: String,
    val timestamp: Instant = Instant.now()
) : DomainEvent
