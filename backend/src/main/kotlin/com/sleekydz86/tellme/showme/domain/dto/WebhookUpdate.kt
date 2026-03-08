package com.sleekydz86.tellme.showme.domain.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class WebhookUpdate(
    @JsonProperty("update_id")
    val updateId: Long? = null,
    @JsonProperty("message")
    val message: TelegramUpdate.Message? = null
)
