package com.sleekydz86.tellme.aiserver.presentation.controller

import com.sleekydz86.tellme.aiserver.domain.model.DocumentUploadEntity
import com.sleekydz86.tellme.aiserver.domain.model.TelegramMessageEntity
import com.sleekydz86.tellme.aiserver.infrastructure.persistence.DocumentUploadRepository
import com.sleekydz86.tellme.aiserver.infrastructure.persistence.TelegramMessageRepository
import com.sleekydz86.tellme.aiserver.presentation.dto.LeeResult
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant

@RestController
@RequestMapping("/api/telegram")
class TelegramHistoryController(
    private val telegramMessageRepository: TelegramMessageRepository,
    private val documentUploadRepository: DocumentUploadRepository
) {

    @PostMapping("/messages")
    fun saveMessage(
        @RequestBody body: SaveTelegramMessageRequest
    ): ResponseEntity<LeeResult<Nothing>> {
        val receivedAt = parseInstant(body.receivedAt) ?: Instant.now()
        val entity = TelegramMessageEntity(
            telegramMessageId = body.telegramMessageId,
            chatId = body.chatId.toString(),
            fromUserId = body.fromUserId.toString(),
            fromUserName = body.fromUserName,
            text = body.text,
            receivedAt = receivedAt
        )
        telegramMessageRepository.save(entity)
        return ResponseEntity.ok(LeeResult.ok(msg = "저장되었습니다."))
    }

    @GetMapping("/messages")
    fun listMessages(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) search: String?
    ): ResponseEntity<LeeResult<MessageHistoryResponse>> {
        val pageable = PageRequest.of(
            page.coerceAtLeast(0),
            size.coerceIn(1, 100),
            Sort.by(Sort.Direction.DESC, "receivedAt")
        )
        val result = if (search.isNullOrBlank()) {
            telegramMessageRepository.findAll(pageable)
        } else {
            telegramMessageRepository.findByTextContainingIgnoreCase(search.trim(), pageable)
        }
        val items = result.content.map { msg ->
            MessageHistoryItem(
                id = msg.id!!,
                telegramMessageId = msg.telegramMessageId,
                chatId = msg.chatId,
                fromUserId = msg.fromUserId,
                fromUserName = msg.fromUserName,
                text = msg.text,
                receivedAt = msg.receivedAt.toString(),
                createdAt = msg.createdAt.toString()
            )
        }
        val response = MessageHistoryResponse(
            content = items,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            number = result.number,
            size = result.size
        )
        return ResponseEntity.ok(LeeResult.ok(data = response))
    }

    @GetMapping("/files")
    fun listFiles(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) search: String?
    ): ResponseEntity<LeeResult<FileHistoryResponse>> {
        val pageable = PageRequest.of(
            page.coerceAtLeast(0),
            size.coerceIn(1, 100),
            Sort.by(Sort.Direction.DESC, "createdAt")
        )
        val result = if (search.isNullOrBlank()) {
            documentUploadRepository.findByUploadSource("TELEGRAM", pageable)
        } else {
            documentUploadRepository.findByUploadSourceAndFileNameContainingIgnoreCase(
                "TELEGRAM",
                search.trim(),
                pageable
            )
        }
        val items = result.content.map { f ->
            FileHistoryItem(
                id = f.id!!,
                fileName = f.fileName,
                objectKey = f.objectKey,
                contentType = f.contentType,
                userId = f.userId,
                fromUserName = f.fromUserName,
                telegramMessageId = f.telegramMessageId,
                uploadSource = f.uploadSource,
                createdAt = f.createdAt.toString()
            )
        }
        val response = FileHistoryResponse(
            content = items,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            number = result.number,
            size = result.size
        )
        return ResponseEntity.ok(LeeResult.ok(data = response))
    }

    data class SaveTelegramMessageRequest(
        val telegramMessageId: Long,
        val chatId: Long,
        val fromUserId: Long,
        val fromUserName: String?,
        val text: String?,
        val receivedAt: String?
    )

    data class MessageHistoryItem(
        val id: Long,
        val telegramMessageId: Long,
        val chatId: String,
        val fromUserId: String,
        val fromUserName: String?,
        val text: String?,
        val receivedAt: String,
        val createdAt: String
    )

    data class MessageHistoryResponse(
        val content: List<MessageHistoryItem>,
        val totalElements: Long,
        val totalPages: Int,
        val number: Int,
        val size: Int
    )

    data class FileHistoryItem(
        val id: Long,
        val fileName: String,
        val objectKey: String,
        val contentType: String,
        val userId: String,
        val fromUserName: String?,
        val telegramMessageId: Long?,
        val uploadSource: String,
        val createdAt: String
    )

    data class FileHistoryResponse(
        val content: List<FileHistoryItem>,
        val totalElements: Long,
        val totalPages: Int,
        val number: Int,
        val size: Int
    )

    private fun parseInstant(raw: String?): Instant? {
        if (raw.isNullOrBlank()) return null
        val trimmed = raw.trim()
        trimmed.toLongOrNull()?.let { n ->
            return if (trimmed.length >= 13) Instant.ofEpochMilli(n) else Instant.ofEpochSecond(n)
        }
        return try {
            Instant.parse(trimmed)
        } catch (_: Exception) {
            null
        }
    }
}
