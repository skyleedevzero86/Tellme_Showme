package com.sleekydz86.tellme.aiserver.presentation.controller

import com.sleekydz86.tellme.aiserver.aplication.event.DomainEventPublisher
import com.sleekydz86.tellme.aiserver.aplication.port.StoragePort
import com.sleekydz86.tellme.aiserver.aplication.service.DocumentService
import com.sleekydz86.tellme.aiserver.domain.event.DocumentUploaded
import com.sleekydz86.tellme.aiserver.presentation.dto.LeeResult
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/rag")
class RagController(
    private val storagePort: StoragePort,
    private val documentService: DocumentService,
    private val domainEventPublisher: DomainEventPublisher
) {

    @PostMapping("/upload")
    fun upload(
        @RequestParam("file") file: MultipartFile,
        @RequestHeader("X-User-Id", required = false) userId: String?
    ): LeeResult<Nothing> {
        val uid = userId ?: "익명"
        if (file.isEmpty) return LeeResult.error("유효하지 않은 파일입니다.")
        val fileName = file.originalFilename ?: "알 수 없음"

        return try {
            val objectKey = storagePort.save(file, uid)
            documentService.loadText(storagePort.get(objectKey), fileName, objectKey).fold(
                onSuccess = {
                    domainEventPublisher.publish(
                        DocumentUploaded(
                            fileName = fileName,
                            userId = uid,
                            objectKey = objectKey,
                            contentType = file.contentType ?: "application/octet-stream"
                        )
                    )
                    LeeResult.ok(msg = "파일이 성공적으로 업로드되었습니다.")
                },
                onFailure = { LeeResult.error("업로드 실패: ${it.message}") }
            )
        } catch (e: Exception) {
            LeeResult.error("업로드 실패: ${e.message ?: "저장 또는 처리 오류."}", 500)
        }
    }
}
