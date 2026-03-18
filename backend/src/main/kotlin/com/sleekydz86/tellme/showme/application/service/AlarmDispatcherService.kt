package com.sleekydz86.tellme.showme.application.service

import com.sleekydz86.tellme.showme.application.port.TelegramApiPort
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class AlarmDispatcherService(
    private val telegramApi: TelegramApiPort,
    private val timeAlarmService: TimeAlarmService
) {
    private val log = LoggerFactory.getLogger(AlarmDispatcherService::class.java)

    @Scheduled(fixedDelayString = "\${telegram.alarm.dispatcher-fixed-delay-ms:30000}")
    fun dispatchDueAlarms() {
        if (telegramApi.isTokenMissing) return

        val dueAlarms = timeAlarmService.findDueAlarms()
        for (alarm in dueAlarms) {
            try {
                telegramApi.sendMessage(alarm.chatId, timeAlarmService.buildAlarmNotificationMessage(alarm))?.block()
                timeAlarmService.markTriggered(alarm)
            } catch (error: Exception) {
                log.warn("Failed to dispatch alarm: id={}, chatId={}", alarm.id, alarm.chatId, error)
            }
        }
    }
}
