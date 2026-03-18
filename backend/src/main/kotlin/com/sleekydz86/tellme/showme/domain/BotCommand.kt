package com.sleekydz86.tellme.showme.domain

enum class BotCommand(val value: String) {
    START("/start"),
    TIME("/time"),
    HELP("/help"),
    LOTTO("/lotto"),
    GOD("/god"),
    ENG("/eng"),
    END("/end"),
    SEARCH("/search");

    companion object {
        fun from(text: String?): BotCommand? {
            if (text == null || text.isBlank()) return null
            val commandToken = text.trim()
                .substringBefore(" ")
                .substringBefore("@")
            return entries.find { it.value == commandToken }
        }
    }
}
