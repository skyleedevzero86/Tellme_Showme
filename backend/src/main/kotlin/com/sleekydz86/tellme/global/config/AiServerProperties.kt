package com.sleekydz86.tellme.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "ai-server")
class AiServerProperties {
    var url: String = "http://localhost:6060"
}
