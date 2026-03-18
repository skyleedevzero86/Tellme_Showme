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
        @RequestParam("userId", required = false) userIdParam: String?,
        @RequestParam("uploadSource", required = false) uploadSourceParam: String?,
        @RequestParam("telegramMessageId", required = false) telegramMessageIdParam: Long?,
        @RequestParam("fromUserName", required = false) fromUserNameParam: String?,
        @RequestHeader("X-User-Id", required = false) userIdHeader: String?,
        @RequestHeader("X-Upload-Source", required = false) uploadSourceHeader: String?,
        @RequestHeader("X-Telegram-Message-Id", required = false) telegramMessageIdHeader: Long?,
        @RequestHeader("X-From-User-Name", required = false) fromUserNameHeader: String?
    ): LeeResult<Nothing> {
        val userId = userIdParam ?: userIdHeader ?: "anonymous"
        val uploadSource = when ((uploadSourceParam ?: uploadSourceHeader)?.uppercase()) {
            "TELEGRAM" -> "TELEGRAM"
            else -> "FRONTEND"
        }
        val telegramMessageId = telegramMessageIdParam ?: telegramMessageIdHeader
        val fromUserName = fromUserNameParam ?: fromUserNameHeader

        if (file.isEmpty) {
            return LeeResult.error("유효하지 않은 파일입니다.")
        }

        val fileName = file.originalFilename ?: "unknown-file"

        return try {
            val objectKey = storagePort.save(file, userId)
            documentService.loadText(storagePort.get(objectKey), fileName, objectKey).fold(
                onSuccess = {
                    domainEventPublisher.publish(
                        DocumentUploaded(
                            fileName = fileName,
                            userId = userId,
                            objectKey = objectKey,
                            contentType = file.contentType ?: "application/octet-stream",
                            uploadSource = uploadSource,
                            telegramMessageId = telegramMessageId,
                            fromUserName = fromUserName
                        )
                    )
                    LeeResult.ok(msg = "파일이 성공적으로 업로드되었습니다.")
                },
                onFailure = { LeeResult.error("업로드 실패: ${it.message}") }
            )
        } catch (e: Exception) {
            LeeResult.error("업로드 실패: ${e.message ?: "알 수 없는 처리 오류"}", 500)
        }
    }
}
