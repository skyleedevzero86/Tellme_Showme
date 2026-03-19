package com.sleekydz86.tellme.showme.domain

enum class ConversationMode(val aiMode: String, val label: String) {
    ENG("eng", "English mode"),
    GOD("god", "Quote mode");

    companion object {
        fun fromAiMode(value: String?): ConversationMode? =
            entries.find { it.aiMode.equals(value?.trim(), ignoreCase = true) }
    }
}
