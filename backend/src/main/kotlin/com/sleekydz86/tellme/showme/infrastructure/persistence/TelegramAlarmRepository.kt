package com.sleekydz86.tellme.showme.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface TelegramAlarmRepository : JpaRepository<TelegramAlarmEntity, Long> {
    fun findByActiveTrueAndNextTriggerAtLessThanEqualOrderByNextTriggerAtAsc(now: LocalDateTime): List<TelegramAlarmEntity>
    fun findByChatIdAndActiveTrue(chatId: Long): List<TelegramAlarmEntity>
}
