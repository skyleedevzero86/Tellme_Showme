package com.sleekydz86.tellme.aiserver.aplication.port

import org.springframework.core.io.Resource
import org.springframework.web.multipart.MultipartFile

interface StoragePort {
    fun save(file: MultipartFile, userId: String): String
    fun get(objectKey: String): Resource
}
