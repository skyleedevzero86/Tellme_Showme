package com.sleekydz86.tellme.aiserver.aplication.event

import com.sleekydz86.tellme.aiserver.aplication.port.AnalyticsPort
import com.sleekydz86.tellme.aiserver.aplication.port.LoggingPort
import com.sleekydz86.tellme.aiserver.aplication.port.NotificationPort
import com.sleekydz86.tellme.aiserver.domain.event.DomainEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class DomainEventHandlers(
    private val notificationPort: NotificationPort,
    private val loggingPort: LoggingPort,
    private val analyticsPort: AnalyticsPort
) {

    @EventListener
    @Async
    fun handle(event: DomainEvent) {
        loggingPort.log(event)
        analyticsPort.record(event)
        notificationPort.notify(event)
    }
}
