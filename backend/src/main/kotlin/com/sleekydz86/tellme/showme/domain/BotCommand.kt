package com.sleekydz86.tellme.showme.domain

enum class BotCommand(val value: String) {
    START("/start"),
    TIME("/time"),
    HELP("/help"),
    LOTTO("/lotto"),
    GOD("/god"),
    ENG("/eng");

    companion object {
        fun from(text: String?): BotCommand? {
            if (text == null || text.isBlank()) return null
            val trimmed = text.trim()
            return entries.find { it.value == trimmed }
        }
    }
}