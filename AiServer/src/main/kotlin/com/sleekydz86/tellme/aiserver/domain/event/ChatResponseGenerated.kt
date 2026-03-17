package com.sleekydz86.tellme.aiserver.domain.event

import java.time.Instant

data class ChatResponseGenerated(
    val userId: String,
    val responseLength: Int,
    val mode: String,
    val timestamp: Instant = Instant.now()
) : DomainEvent
