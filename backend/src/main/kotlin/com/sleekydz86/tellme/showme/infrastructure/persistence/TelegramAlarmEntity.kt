package com.sleekydz86.tellme.showme.infrastructure.persistence

import com.sleekydz86.tellme.showme.domain.AlarmType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.time.LocalTime

@Entity
@Table(name = "telegram_alarm")
class TelegramAlarmEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var chatId: Long = 0,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var alarmType: AlarmType = AlarmType.INTERVAL,

    @Column
    var intervalMinutes: Int? = null,

    @Column
    var timeOfDay: LocalTime? = null,

    @Column(nullable = false)
    var nextTriggerAt: LocalDateTime = LocalDateTime.now(),

    @Column
    var lastTriggeredAt: LocalDateTime? = null,

    @Column(nullable = false)
    var active: Boolean = true,

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
