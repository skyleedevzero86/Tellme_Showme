package com.sleekydz86.tellme.aiserver.infrastructure.persistence

import com.sleekydz86.tellme.aiserver.domain.model.AnalyticsEventEntity
import org.springframework.data.jpa.repository.JpaRepository

interface AnalyticsEventRepository : JpaRepository<AnalyticsEventEntity, Long>
