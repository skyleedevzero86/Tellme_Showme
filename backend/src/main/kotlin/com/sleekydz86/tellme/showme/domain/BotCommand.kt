package com.sleekydz86.tellme.showme.domain

import java.util.*


enum class BotCommand(val value: String) {
    START("/start"),
    TIME("/time"),
    HELP("/help"),
    LOTTO("/lotto"),
    GOD("/god"),
    ENG("/eng");

    companion object {
        fun from(text: String?): Optional<BotCommand?> {
            if (text == null || text.isBlank()) {
                return Optional.empty<BotCommand?>()
            }
            val trimmed = text.trim { it <= ' ' }
            return Arrays.stream<BotCommand?>(entries.toTypedArray())
                .filter { c: BotCommand? -> c!!.value == trimmed }
                .findFirst()
        }
    }
}