package com.sleekydz86.tellme.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "telegram")
class TelegramBotProperties {
    var bot: Bot = Bot()
    var api: Api = Api()
    var files: Files = Files()
    var bibleApiUrl: String = ""
    var englishApiUrl: String = ""
    var channelUsername: String = ""

    class Bot {
        var token: String = ""
    }

    class Api {
        var baseUrl: String = "https://api.telegram.org"
        var webhookUrl: String = ""
    }

    class Files {
        var downloadDir: String = System.getProperty("java.io.tmpdir") + "/tellme-showme/files"
    }
}
