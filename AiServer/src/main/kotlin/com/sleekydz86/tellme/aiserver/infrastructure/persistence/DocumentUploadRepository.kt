package com.sleekydz86.tellme.aiserver.infrastructure.persistence

import com.sleekydz86.tellme.aiserver.domain.model.DocumentUploadEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface DocumentUploadRepository : JpaRepository<DocumentUploadEntity, Long> {
    fun findByObjectKey(objectKey: String): Optional<DocumentUploadEntity>
    fun findByUploadSource(uploadSource: String, pageable: Pageable): Page<DocumentUploadEntity>
    fun findByUploadSourceAndFileNameContainingIgnoreCase(
        uploadSource: String,
        fileName: String,
        pageable: Pageable
    ): Page<DocumentUploadEntity>
}
