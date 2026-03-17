package com.sleekydz86.tellme.aiserver.infrastructure.persistence

import com.sleekydz86.tellme.aiserver.domain.model.AuditLogEntity
import org.springframework.data.jpa.repository.JpaRepository

interface AuditLogRepository : JpaRepository<AuditLogEntity, Long>
