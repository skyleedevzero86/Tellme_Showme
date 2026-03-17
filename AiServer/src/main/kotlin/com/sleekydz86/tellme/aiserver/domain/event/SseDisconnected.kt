package com.sleekydz86.tellme.aiserver.domain.event

import java.time.Instant

data class SseDisconnected(
    val userId: String,
    val timestamp: Instant = Instant.now()
) : DomainEvent
