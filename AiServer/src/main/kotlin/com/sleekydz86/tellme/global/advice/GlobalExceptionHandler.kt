package com.sleekydz86.tellme.global.advice

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.server.ResponseStatusException

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<LeeResult<Nothing>> {
        val message = ex.bindingResult.fieldErrors
            .joinToString(", ") { err: FieldError -> "${err.field}: ${err.defaultMessage ?: "유효하지 않음"}" }
        logger.warn("검증 실패: {}", message)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(LeeResult.error(message, HttpStatus.BAD_REQUEST.value()))
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleBadRequest(ex: HttpMessageNotReadableException): ResponseEntity<LeeResult<Nothing>> {
        logger.warn("잘못된 요청 본문: {}", ex.message)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(LeeResult.error("요청 본문이 올바르지 않습니다.", HttpStatus.BAD_REQUEST.value()))
    }

    @ExceptionHandler(IllegalArgumentException::class, IllegalStateException::class)
    fun handleBadArgument(ex: Exception): ResponseEntity<LeeResult<Nothing>> {
        logger.warn("잘못된 인자 또는 상태: {}", ex.message)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(LeeResult.error(ex.message ?: "잘못된 요청입니다.", HttpStatus.BAD_REQUEST.value()))
    }

    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleMaxUploadSize(ex: MaxUploadSizeExceededException): ResponseEntity<LeeResult<Nothing>> {
        logger.warn("업로드 용량 초과: {}", ex.message)
        return ResponseEntity
            .status(HttpStatus.PAYLOAD_TOO_LARGE)
            .body(LeeResult.error("파일 크기가 제한을 초과합니다.", HttpStatus.PAYLOAD_TOO_LARGE.value()))
    }

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatus(ex: ResponseStatusException): ResponseEntity<LeeResult<Nothing>> {
        val status = ex.statusCode
        val message = ex.reason ?: status.toString()
        logger.warn("응답 상태 예외: {} - {}", status, message)
        return ResponseEntity
            .status(status)
            .body(LeeResult.error(message, status.value()))
    }

    @ExceptionHandler(Exception::class)
    fun handleAny(ex: Exception): ResponseEntity<LeeResult<Nothing>> {
        logger.error("처리되지 않은 예외: {}", ex.message, ex)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(LeeResult.error("오류가 발생했습니다. 잠시 후 다시 시도해 주세요.", HttpStatus.INTERNAL_SERVER_ERROR.value()))
    }
}
