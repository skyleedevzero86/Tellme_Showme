package com.sleekydz86.tellme.aiserver.aplication.service.impl

import com.sleekydz86.tellme.aiserver.aplication.service.DocumentService
import org.slf4j.LoggerFactory
import org.springframework.ai.document.Document
import org.springframework.ai.reader.tika.TikaDocumentReader
import org.springframework.ai.transformer.splitter.TokenTextSplitter
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service

@Service
class DocumentServiceImpl(
    private val vectorStore: VectorStore
) : DocumentService {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val textSplitter = TokenTextSplitter()

    override fun loadText(resource: Resource, fileName: String, objectKey: String?): Result<Unit> = try {
        val documentReader = TikaDocumentReader(resource)
        val documents = documentReader.get()
        val objectKeyMeta = objectKey?.let { mapOf("objectKey" to it) } ?: emptyMap()
        val documentsWithMetadata = documents.map { doc ->
            Document(
                doc.content,
                doc.metadata + mapOf(
                    "fileName" to fileName,
                    "source" to resource.toString(),
                    "timestamp" to System.currentTimeMillis().toString(),
                    "contentLength" to doc.content.length.toString()
                ) + objectKeyMeta
            )
        }
        val splitDocuments = textSplitter.apply(documentsWithMetadata)
        vectorStore.add(splitDocuments)
        logger.info("문서 로드됨: fileName={}, chunks={}", fileName, splitDocuments.size)
        Result.success(Unit)
    } catch (e: Exception) {
        logger.error("문서 로드 실패: fileName={}", fileName, e)
        Result.failure(e)
    }

    override fun doSearch(query: String): List<Document> = try {
        val searchRequest = SearchRequest.query(query).withTopK(20).withSimilarityThreshold(0.01)
        vectorStore.similaritySearch(searchRequest)
    } catch (e: Exception) {
        logger.error("검색 실패: query={}", query, e)
        emptyList()
    }
}
