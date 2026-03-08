package com.sleekydz86.tellme.showme.domain.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import lombok.Data


@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class WebhookUpdate {
    @JsonProperty("update_id")
    private val updateId: Long? = null

    @JsonProperty("message")
    private val message: TelegramUpdate.Message? = null
}
