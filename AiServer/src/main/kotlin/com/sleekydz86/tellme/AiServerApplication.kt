package com.sleekydz86.tellme

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class AiServerApplication

fun main(args: Array<String>) {
    runApplication<AiServerApplication>(*args)
}
