package com.sleekydz86.tellme.aiserver.domain.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "notifications")
class NotificationEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(nullable = false)
    val type: String,
    @Column(nullable = false)
    val userId: String,
    @Column(length = 2000)
    val payload: String? = null,
    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),
    @Column(nullable = false)
    var read: Boolean = false
)
