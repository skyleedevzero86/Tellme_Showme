package com.sleekydz86.tellme.showme.infrastructure.web

import com.sleekydz86.tellme.showme.domain.dto.SendMessageResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.multipart.MultipartException
import org.springframework.web.multipart.MaxUploadSizeExceededException

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleUploadTooLarge(e: MaxUploadSizeExceededException): ResponseEntity<SendMessageResponse> {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
            .body(SendMessageResponse(status = "fail", message = "파일 크기가 너무 큽니다."))
    }

    @ExceptionHandler(MultipartException::class)
    fun handleMultipart(e: MultipartException): ResponseEntity<SendMessageResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(SendMessageResponse(status = "fail", message = "파일 업로드 요청을 처리하지 못했습니다."))
    }

    @ExceptionHandler(Exception::class)
    fun handleAny(e: Exception): ResponseEntity<SendMessageResponse> {
        log.error("Unhandled error", e)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(SendMessageResponse(status = "fail", message = e.message ?: "서버 오류가 발생했습니다."))
    }
}

