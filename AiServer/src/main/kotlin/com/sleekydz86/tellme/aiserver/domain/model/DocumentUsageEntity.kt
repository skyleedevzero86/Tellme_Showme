package com.sleekydz86.tellme.aiserver.domain.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "document_usages", indexes = [Index(name = "idx_usage_upload_used", columnList = "documentUploadId,usedAt")])
class DocumentUsageEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(nullable = false)
    val documentUploadId: Long,
    @Column(nullable = false)
    val userId: String,
    @Column(columnDefinition = "TEXT")
    val usedInQuery: String? = null,
    @Column(nullable = false)
    val usedAt: Instant = Instant.now()
)
