package com.sleekydz86.tellme.aiserver.presentation.controller

import com.sleekydz86.tellme.aiserver.aplication.port.StoragePort
import com.sleekydz86.tellme.aiserver.domain.model.DocumentUploadEntity
import com.sleekydz86.tellme.aiserver.domain.model.TelegramMessageEntity
import com.sleekydz86.tellme.aiserver.infrastructure.persistence.DocumentUploadRepository
import com.sleekydz86.tellme.aiserver.infrastructure.persistence.TelegramMessageRepository
import com.sleekydz86.tellme.aiserver.presentation.dto.LeeResult
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.core.io.Resource
import java.time.Instant
import java.nio.charset.StandardCharsets

@RestController
@RequestMapping("/api/telegram")
class TelegramHistoryController(
    private val telegramMessageRepository: TelegramMessageRepository,
    private val documentUploadRepository: DocumentUploadRepository,
    private val jdbcTemplate: JdbcTemplate,
    private val storagePort: StoragePort
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
        val safePage = page.coerceAtLeast(0)
        val safeSize = size.coerceIn(1, 100)
        val offset = safePage * safeSize
        val searchTerm = search?.trim()?.takeIf { it.isNotEmpty() }

        val items = if (searchTerm == null) {
            jdbcTemplate.query(
                """
                    SELECT id, telegram_message_id, chat_id, from_user_id, from_user_name, text, received_at, created_at
                    FROM telegram_messages
                    ORDER BY received_at DESC
                    LIMIT ? OFFSET ?
                """.trimIndent(),
                { rs, _ ->
                    MessageHistoryItem(
                        id = rs.getLong("id"),
                        telegramMessageId = rs.getLong("telegram_message_id"),
                        chatId = rs.getString("chat_id"),
                        fromUserId = rs.getString("from_user_id"),
                        fromUserName = rs.getString("from_user_name"),
                        text = rs.getString("text"),
                        receivedAt = rs.getObject("received_at").toString(),
                        createdAt = rs.getObject("created_at").toString()
                    )
                },
                safeSize,
                offset
            )
        } else {
            jdbcTemplate.query(
                """
                    SELECT id, telegram_message_id, chat_id, from_user_id, from_user_name, text, received_at, created_at
                    FROM telegram_messages
                    WHERE COALESCE(text, '') ILIKE ?
                    ORDER BY received_at DESC
                    LIMIT ? OFFSET ?
                """.trimIndent(),
                { rs, _ ->
                    MessageHistoryItem(
                        id = rs.getLong("id"),
                        telegramMessageId = rs.getLong("telegram_message_id"),
                        chatId = rs.getString("chat_id"),
                        fromUserId = rs.getString("from_user_id"),
                        fromUserName = rs.getString("from_user_name"),
                        text = rs.getString("text"),
                        receivedAt = rs.getObject("received_at").toString(),
                        createdAt = rs.getObject("created_at").toString()
                    )
                },
                "%$searchTerm%",
                safeSize,
                offset
            )
        }

        val totalElements = if (searchTerm == null) {
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM telegram_messages", Long::class.java) ?: 0L
        } else {
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM telegram_messages WHERE COALESCE(text, '') ILIKE ?",
                Long::class.java,
                "%$searchTerm%"
            ) ?: 0L
        }
        val totalPages = if (totalElements == 0L) 0 else ((totalElements + safeSize - 1) / safeSize).toInt()

        val response = MessageHistoryResponse(
            content = items,
            totalElements = totalElements,
            totalPages = totalPages,
            number = safePage,
            size = safeSize
        )
        return ResponseEntity.ok(LeeResult.ok(data = response))
    }

    @GetMapping("/files")
    fun listFiles(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) search: String?
    ): ResponseEntity<LeeResult<FileHistoryResponse>> {
        val safePage = page.coerceAtLeast(0)
        val safeSize = size.coerceIn(1, 100)
        val offset = safePage * safeSize
        val searchTerm = search?.trim()?.takeIf { it.isNotEmpty() }

        val items = if (searchTerm == null) {
            jdbcTemplate.query(
                """
                    SELECT id, file_name, object_key, content_type, user_id, from_user_name, telegram_message_id, upload_source, created_at
                    FROM document_uploads
                    WHERE upload_source = 'TELEGRAM'
                    ORDER BY created_at DESC
                    LIMIT ? OFFSET ?
                """.trimIndent(),
                { rs, _ ->
                    FileHistoryItem(
                        id = rs.getLong("id"),
                        fileName = rs.getString("file_name"),
                        objectKey = rs.getString("object_key"),
                        contentType = rs.getString("content_type"),
                        userId = rs.getString("user_id"),
                        fromUserName = rs.getString("from_user_name"),
                        telegramMessageId = rs.getObject("telegram_message_id") as? Long,
                        uploadSource = rs.getString("upload_source"),
                        createdAt = rs.getObject("created_at").toString()
                    )
                },
                safeSize,
                offset
            )
        } else {
            jdbcTemplate.query(
                """
                    SELECT id, file_name, object_key, content_type, user_id, from_user_name, telegram_message_id, upload_source, created_at
                    FROM document_uploads
                    WHERE upload_source = 'TELEGRAM'
                      AND COALESCE(file_name, '') ILIKE ?
                    ORDER BY created_at DESC
                    LIMIT ? OFFSET ?
                """.trimIndent(),
                { rs, _ ->
                    FileHistoryItem(
                        id = rs.getLong("id"),
                        fileName = rs.getString("file_name"),
                        objectKey = rs.getString("object_key"),
                        contentType = rs.getString("content_type"),
                        userId = rs.getString("user_id"),
                        fromUserName = rs.getString("from_user_name"),
                        telegramMessageId = rs.getObject("telegram_message_id") as? Long,
                        uploadSource = rs.getString("upload_source"),
                        createdAt = rs.getObject("created_at").toString()
                    )
                },
                "%$searchTerm%",
                safeSize,
                offset
            )
        }

        val totalElements = if (searchTerm == null) {
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM document_uploads WHERE upload_source = 'TELEGRAM'",
                Long::class.java
            ) ?: 0L
        } else {
            jdbcTemplate.queryForObject(
                """
                    SELECT COUNT(*)
                    FROM document_uploads
                    WHERE upload_source = 'TELEGRAM'
                      AND COALESCE(file_name, '') ILIKE ?
                """.trimIndent(),
                Long::class.java,
                "%$searchTerm%"
            ) ?: 0L
        }
        val totalPages = if (totalElements == 0L) 0 else ((totalElements + safeSize - 1) / safeSize).toInt()

        val response = FileHistoryResponse(
            content = items,
            totalElements = totalElements,
            totalPages = totalPages,
            number = safePage,
            size = safeSize
        )
        return ResponseEntity.ok(LeeResult.ok(data = response))
    }

    @GetMapping("/files/preview")
    fun previewFile(@RequestParam("objectKey") objectKey: String): ResponseEntity<Resource> {
        val metadata = jdbcTemplate.query(
            """
                SELECT file_name, content_type
                FROM document_uploads
                WHERE upload_source = 'TELEGRAM'
                  AND object_key = ?
                LIMIT 1
            """.trimIndent(),
            { rs, _ ->
                FilePreviewMetadata(
                    fileName = rs.getString("file_name"),
                    contentType = rs.getString("content_type")
                )
            },
            objectKey
        ).firstOrNull() ?: return ResponseEntity.notFound().build()

        val resource = storagePort.get(objectKey)
        val contentType = metadata.contentType
            ?.takeIf { it.isNotBlank() }
            ?.let { raw -> runCatching { MediaType.parseMediaType(raw) }.getOrNull() }
            ?: MediaType.APPLICATION_OCTET_STREAM

        return ResponseEntity.ok()
            .contentType(contentType)
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.inline()
                    .filename(metadata.fileName, StandardCharsets.UTF_8)
                    .build()
                    .toString()
            )
            .body(resource)
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

    data class FilePreviewMetadata(
        val fileName: String,
        val contentType: String?
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
