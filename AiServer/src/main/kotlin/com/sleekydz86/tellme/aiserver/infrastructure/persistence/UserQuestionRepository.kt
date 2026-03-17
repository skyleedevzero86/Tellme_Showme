package com.sleekydz86.tellme.aiserver.infrastructure.persistence

import com.sleekydz86.tellme.aiserver.domain.model.UserQuestionEntity
import org.springframework.data.jpa.repository.JpaRepository

interface UserQuestionRepository : JpaRepository<UserQuestionEntity, Long>
