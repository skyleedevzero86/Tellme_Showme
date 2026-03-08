package com.sleekydz86.tellme.showme.domain.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class TelegramFileResponse(
    @JsonProperty("ok")
    val ok: Boolean? = null,
    @JsonProperty("result")
    val result: FileResult? = null
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class FileResult(
        @JsonProperty("file_id")
        val fileId: String? = null,
        @JsonProperty("file_unique_id")
        val fileUniqueId: String? = null,
        @JsonProperty("file_size")
        val fileSize: Long? = null,
        @JsonProperty("file_path")
        val filePath: String? = null
    )
}