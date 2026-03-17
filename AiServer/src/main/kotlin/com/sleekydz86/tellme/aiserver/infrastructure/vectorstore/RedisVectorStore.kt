package com.sleekydz86.tellme.aiserver.infrastructure.vectorstore

import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.ai.vectorstore.filter.Filter
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class RedisVectorStore(
    private val redisTemplate: RedisTemplate<String, Any>
) : VectorStore {

    private val documents = ConcurrentHashMap<String, Document>()
    private var nextId = 1

    override fun add(documents: List<Document>) {
        documents.forEach { doc ->
            val id = "doc_${nextId++}"
            val text = doc.text.orEmpty()
            val storedDocument = Document(text, doc.metadata + mapOf("id" to id))
            this.documents[id] = storedDocument
            val key = "vec:document:$id"
            val metadataKey = "vec:metadata:$id"
            try {
                redisTemplate.opsForValue().set(key, text)
                redisTemplate.opsForHash<String, String>()
                    .putAll(metadataKey, doc.metadata.mapValues { (_, value) -> value.toString() })
            } catch (_: Exception) {
            }
        }
    }

    override fun similaritySearch(searchRequest: SearchRequest): List<Document> {
        val query = searchRequest.query
        val topK = searchRequest.topK
        val threshold = searchRequest.similarityThreshold

        return documents.values
            .mapNotNull { doc ->
                val score = calculateSimilarity(query, doc.text.orEmpty())
                if (score >= threshold) doc to score else null
            }
            .sortedByDescending { it.second }
            .take(topK)
            .map { it.first }
    }

    override fun similaritySearch(query: String): List<Document> {
        return similaritySearch(SearchRequest.builder().query(query).build())
    }

    override fun delete(idList: List<String>) {
        idList.forEach { id ->
            documents.remove(id)
            redisTemplate.delete("vec:document:$id")
            redisTemplate.delete("vec:metadata:$id")
        }
    }

    override fun delete(filterExpression: Filter.Expression) {
        throw UnsupportedOperationException("RedisVectorStore에서는 필터 기반 삭제를 지원하지 않습니다.")
    }

    private fun calculateSimilarity(query: String, content: String): Double {
        val queryWords = query.lowercase().split(" ").filter { it.length > 2 }
        if (queryWords.isEmpty()) return 0.0

        val contentLower = content.lowercase()
        val matches = queryWords.count { contentLower.contains(it) }
        return matches.toDouble() / queryWords.size
    }
}
