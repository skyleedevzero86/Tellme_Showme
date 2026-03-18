package com.sleekydz86.tellme.aiserver.infrastructure.persistence

import com.sleekydz86.tellme.aiserver.domain.model.TelegramMessageEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface TelegramMessageRepository : JpaRepository<TelegramMessageEntity, Long> {
    fun findByTextContainingIgnoreCase(search: String?, pageable: Pageable): Page<TelegramMessageEntity>
}
