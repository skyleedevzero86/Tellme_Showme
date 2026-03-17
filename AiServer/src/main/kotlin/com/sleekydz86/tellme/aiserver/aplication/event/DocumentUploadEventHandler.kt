package com.sleekydz86.tellme.aiserver.aplication.event

import com.sleekydz86.tellme.aiserver.domain.event.DocumentUploaded
import com.sleekydz86.tellme.aiserver.domain.model.DocumentUploadEntity
import com.sleekydz86.tellme.aiserver.infrastructure.persistence.DocumentUploadRepository
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class DocumentUploadEventHandler(
    private val documentUploadRepository: DocumentUploadRepository
) {

    @EventListener
    @Async
    fun handle(event: DocumentUploaded) {
        documentUploadRepository.save(
            DocumentUploadEntity(
                userId = event.userId,
                fileName = event.fileName,
                objectKey = event.objectKey,
                contentType = event.contentType,
                uploadSource = event.uploadSource
            )
        )
    }
}
