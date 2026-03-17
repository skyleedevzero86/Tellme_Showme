package com.sleekydz86.tellme.aiserver.domain.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "conversation_messages", indexes = [Index(name = "idx_conv_user_created", columnList = "userId,createdAt")])
class ConversationMessageEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(nullable = false)
    val userId: String,
    @Column(nullable = false)
    val role: String,
    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,
    @Column(nullable = false)
    val mode: String,
    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)