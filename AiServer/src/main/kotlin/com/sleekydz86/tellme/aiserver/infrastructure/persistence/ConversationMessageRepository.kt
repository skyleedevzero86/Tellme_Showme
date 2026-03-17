package com.sleekydz86.tellme.aiserver.infrastructure.persistence

import com.sleekydz86.tellme.aiserver.domain.model.ConversationMessageEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ConversationMessageRepository : JpaRepository<ConversationMessageEntity, Long>
