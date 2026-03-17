package com.sleekydz86.tellme.aiserver.infrastructure.persistence

import com.sleekydz86.tellme.aiserver.domain.model.DocumentUsageEntity
import org.springframework.data.jpa.repository.JpaRepository

interface DocumentUsageRepository : JpaRepository<DocumentUsageEntity, Long>
