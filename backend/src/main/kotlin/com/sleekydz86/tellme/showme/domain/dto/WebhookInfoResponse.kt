package com.sleekydz86.tellme.showme.domain.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class WebhookInfoResponse(
    @JsonProperty("ok") val ok: Boolean? = null,
    @JsonProperty("result") val result: WebhookInfoResult? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class WebhookInfoResult(
    @JsonProperty("url") val url: String? = null,
    @JsonProperty("pending_update_count") val pendingUpdateCount: Int? = null,
    @JsonProperty("last_error_message") val lastErrorMessage: String? = null,
    @JsonProperty("last_error_date") val lastErrorDate: Long? = null
)
