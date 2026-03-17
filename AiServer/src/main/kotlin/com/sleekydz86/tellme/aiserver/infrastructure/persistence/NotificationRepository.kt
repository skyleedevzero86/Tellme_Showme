package com.sleekydz86.tellme.aiserver.infrastructure.persistence

import com.sleekydz86.tellme.aiserver.domain.model.NotificationEntity
import org.springframework.data.jpa.repository.JpaRepository

interface NotificationRepository : JpaRepository<NotificationEntity, Long>
