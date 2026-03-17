package com.sleekydz86.tellme.aiserver.aplication.event

import com.sleekydz86.tellme.aiserver.domain.event.DomainEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class DomainEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher
) {
    fun publish(event: DomainEvent) {
        applicationEventPublisher.publishEvent(event)
    }
}
