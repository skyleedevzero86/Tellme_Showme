package com.sleekydz86.tellme.aiserver.aplication.service

import com.sleekydz86.tellme.aiserver.domain.model.DocumentUsageEntity
import com.sleekydz86.tellme.aiserver.infrastructure.persistence.DocumentUploadRepository
import com.sleekydz86.tellme.aiserver.infrastructure.persistence.DocumentUsageRepository
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class DocumentUsageService(
    private val documentUploadRepository: DocumentUploadRepository,
    private val documentUsageRepository: DocumentUsageRepository
) {

    fun recordUsage(userId: String, usedInQuery: String, objectKeys: List<String>) {
        objectKeys.forEach { key ->
            documentUploadRepository.findByObjectKey(key).ifPresent { upload ->
                documentUsageRepository.save(
                    DocumentUsageEntity(documentUploadId = upload.id!!, userId = userId, usedInQuery = usedInQuery)
                )
                upload.usageCount++
                upload.lastUsedAt = Instant.now()
                documentUploadRepository.save(upload)
            }
        }
    }
}
