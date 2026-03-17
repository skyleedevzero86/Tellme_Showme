package com.sleekydz86.tellme.aiserver.domain.event

import java.time.Instant

data class DocumentUploaded(
    val fileName: String,
    val userId: String,
    val objectKey: String,
    val contentType: String,
    val timestamp: Instant = Instant.now()
) : DomainEvent
