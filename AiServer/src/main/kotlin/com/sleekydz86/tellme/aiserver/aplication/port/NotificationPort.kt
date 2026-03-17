package com.sleekydz86.tellme.aiserver.aplication.port

import com.sleekydz86.tellme.aiserver.domain.event.DomainEvent

interface NotificationPort {
    fun notify(event: DomainEvent)
}
