package com.sleekydz86.tellme.aiserver.aplication.service

import org.springframework.ai.document.Document
import org.springframework.core.io.Resource

interface DocumentService {
    fun loadText(resource: Resource, fileName: String, objectKey: String? = null): Result<Unit>
    fun doSearch(query: String): List<Document>
}
