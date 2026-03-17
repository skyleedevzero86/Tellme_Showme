package com.sleekydz86.tellme.aiserver.domain.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "user_questions", indexes = [Index(name = "idx_user_question_created", columnList = "userId,createdAt")])
class UserQuestionEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(nullable = false)
    val userId: String,
    @Column(nullable = false, columnDefinition = "TEXT")
    val questionText: String,
    @Column(nullable = false)
    val mode: String,
    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)
