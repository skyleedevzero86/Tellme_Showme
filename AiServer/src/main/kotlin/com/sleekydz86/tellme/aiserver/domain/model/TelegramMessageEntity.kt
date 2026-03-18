package com.sleekydz86.tellme.aiserver.domain.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(
    name = "telegram_messages",
    indexes = [
        Index(name = "idx_telegram_msg_chat_created", columnList = "chatId,receivedAt"),
        Index(name = "idx_telegram_msg_text", columnList = "text")
    ]
)
class TelegramMessageEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(nullable = false)
    val telegramMessageId: Long,
    @Column(nullable = false)
    val chatId: String,
    @Column(nullable = false)
    val fromUserId: String,
    @Column(length = 255)
    val fromUserName: String? = null,
    @Column(columnDefinition = "TEXT")
    val text: String? = null,
    @Column(nullable = false)
    val receivedAt: Instant,
    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)
