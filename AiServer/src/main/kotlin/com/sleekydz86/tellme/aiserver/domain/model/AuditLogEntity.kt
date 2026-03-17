package com.sleekydz86.tellme.aiserver.domain.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "audit_logs", indexes = [Index(name = "idx_audit_action_created", columnList = "action,createdAt")])
class AuditLogEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(nullable = false)
    val action: String,
    @Column(nullable = false)
    val userId: String,
    @Column(length = 4000)
    val details: String? = null,
    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)
