package com.sleekydz86.tellme.showme.domain.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import lombok.Data


@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class TelegramUpdate {
    @JsonProperty("ok")
    val ok: Boolean? = null

    @JsonProperty("result")
    val result: MutableList<UpdateResult?>? = null

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    class UpdateResult {
        @JsonProperty("update_id")
        val updateId: Long? = null

        @JsonProperty("message")
        val message: Message? = null
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    class Message {
        @JsonProperty("message_id")
        val messageId: Long? = null

        @JsonProperty("from")
        val from: User? = null

        @JsonProperty("chat")
        val chat: Chat? = null

        @JsonProperty("date")
        val date: Long? = null

        @JsonProperty("text")
        val text: String? = null

        @JsonProperty("document")
        val document: Document? = null

        @JsonProperty("photo")
        val photo: MutableList<PhotoSize?>? = null

        @JsonProperty("voice")
        val voice: Voice? = null

        @JsonProperty("video")
        val video: Video? = null
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    class User {
        @JsonProperty("id")
        val id: Long? = null

        @JsonProperty("is_bot")
        val isBot: Boolean? = null

        @JsonProperty("first_name")
        val firstName: String? = null

        @JsonProperty("username")
        val username: String? = null
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    class Chat {
        @JsonProperty("id")
        val id: Long? = null

        @JsonProperty("type")
        val type: String? = null

        @JsonProperty("title")
        val title: String? = null

        @JsonProperty("username")
        val username: String? = null
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    class Document {
        @JsonProperty("file_id")
        val fileId: String? = null

        @JsonProperty("file_unique_id")
        val fileUniqueId: String? = null

        @JsonProperty("file_name")
        val fileName: String? = null

        @JsonProperty("mime_type")
        val mimeType: String? = null

        @JsonProperty("file_size")
        val fileSize: Long? = null
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    class PhotoSize {
        @JsonProperty("file_id")
        val fileId: String? = null

        @JsonProperty("file_unique_id")
        val fileUniqueId: String? = null

        @JsonProperty("width")
        val width: Int? = null

        @JsonProperty("height")
        val height: Int? = null

        @JsonProperty("file_size")
        val fileSize: Long? = null
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    class Voice {
        @JsonProperty("file_id")
        val fileId: String? = null

        @JsonProperty("file_unique_id")
        val fileUniqueId: String? = null

        @JsonProperty("duration")
        val duration: Int? = null

        @JsonProperty("mime_type")
        val mimeType: String? = null

        @JsonProperty("file_size")
        val fileSize: Long? = null
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    class Video {
        @JsonProperty("file_id")
        val fileId: String? = null

        @JsonProperty("file_unique_id")
        val fileUniqueId: String? = null

        @JsonProperty("width")
        val width: Int? = null

        @JsonProperty("height")
        val height: Int? = null

        @JsonProperty("duration")
        val duration: Int? = null

        @JsonProperty("mime_type")
        val mimeType: String? = null

        @JsonProperty("file_size")
        val fileSize: Long? = null
    }
}
