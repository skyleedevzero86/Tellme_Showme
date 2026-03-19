package com.sleekydz86.tellme.showme.application.service

import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicReference

@Component
class TelegramPollingGuard {
    private val activeOwner = AtomicReference<String?>(null)

    fun tryAcquire(owner: String): Boolean = activeOwner.compareAndSet(null, owner)

    fun release(owner: String) {
        activeOwner.compareAndSet(owner, null)
    }

    fun currentOwner(): String? = activeOwner.get()
}
