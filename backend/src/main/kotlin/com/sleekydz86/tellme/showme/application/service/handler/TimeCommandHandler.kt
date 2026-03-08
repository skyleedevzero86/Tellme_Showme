package com.sleekydz86.tellme.showme.application.service.handler

import com.sleekydz86.tellme.showme.domain.MessageContext
import reactor.core.publisher.Mono
import java.util.*

class TimeCommandHandler : CommandHandler {
    override fun handle(ctx: MessageContext?): Mono<String> {
        val cal = GregorianCalendar()
        val amPm = if (cal.get(Calendar.AM_PM) == Calendar.AM) "오전" else "오후"
        val msg = String.format(
            TEMPLATE,
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DATE),
            DAY_NAMES[cal.get(Calendar.DAY_OF_WEEK)],
            amPm,
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            cal.get(Calendar.SECOND)
        )
        return Mono.just(msg)
    }

    companion object {
        private val DAY_NAMES = arrayOf("", "일요일", "월요일", "화요일", "수요일", "목요일", "금요일", "토요일")
        private const val TEMPLATE = "현재 시간은 %d년 %d월 %d일 %s %s %d시 %d분 %d초 입니다."
    }
}