package com.sleekydz86.tellme.aiserver.domain.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "document_uploads", indexes = [Index(name = "idx_upload_user_created", columnList = "userId,createdAt")])
class DocumentUploadEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(nullable = false)
    val userId: String,
    @Column(nullable = false)
    val fileName: String,
    @Column(nullable = false)
    val objectKey: String,
    @Column(nullable = false)
    val contentType: String,
    @Column(length = 500)
    val encryptionIv: String? = null,
    @Column(nullable = false, length = 32)
    val uploadSource: String = "FRONTEND",
    @Column(nullable = false)
    var usageCount: Long = 0L,
    @Column
    var lastUsedAt: Instant? = null,
    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)
