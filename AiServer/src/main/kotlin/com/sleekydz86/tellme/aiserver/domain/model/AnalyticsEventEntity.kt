package com.sleekydz86.tellme.aiserver.domain.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "analytics_events", indexes = [Index(name = "idx_analytics_type_created", columnList = "eventType,createdAt")])
class AnalyticsEventEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(nullable = false)
    val eventType: String,
    @Column(nullable = false)
    val userId: String,
    @Column(length = 4000)
    val payload: String? = null,
    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)
