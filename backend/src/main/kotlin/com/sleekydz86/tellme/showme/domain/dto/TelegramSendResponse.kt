package com.sleekydz86.tellme.showme.domain.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty


@JsonIgnoreProperties(ignoreUnknown = true)
data class TelegramSendResponse(
    @JsonProperty("ok")
    val ok: Boolean? = null,
    @JsonProperty("description")
    val description: String? = null,
    @JsonProperty("result")
    val result: Any? = null
)
