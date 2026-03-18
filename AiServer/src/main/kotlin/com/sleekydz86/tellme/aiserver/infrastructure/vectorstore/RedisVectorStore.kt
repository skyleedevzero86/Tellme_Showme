package com.sleekydz86.tellme.aiserver.infrastructure.vectorstore

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.pgvector.PGvector
import org.slf4j.LoggerFactory
import org.springframework.ai.document.Document
import org.springframework.ai.document.DocumentMetadata
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.ai.vectorstore.filter.Filter
import org.springframework.beans.factory.InitializingBean
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.util.LinkedHashMap
import java.util.UUID

@Component
class RedisVectorStore(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val jdbcTemplate: JdbcTemplate,
    private val embeddingModel: EmbeddingModel,
    private val objectMapper: ObjectMapper
) : VectorStore, InitializingBean {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val metadataTypeRef = object : TypeReference<Map<String, Any>>() {}

    @Volatile
    private var pgVectorReady = false

    private var embeddingDimensions: Int = DEFAULT_DIMENSIONS

    override fun afterPropertiesSet() {
        try {
            embeddingDimensions = runCatching { embeddingModel.dimensions() }
                .getOrElse { embeddingModel.embed("vector dimension probe").size }

            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector")
            jdbcTemplate.execute(
                """
                CREATE TABLE IF NOT EXISTS $TABLE_NAME (
                    id TEXT PRIMARY KEY,
                    content TEXT NOT NULL,
                    metadata JSONB NOT NULL,
                    embedding VECTOR($embeddingDimensions) NOT NULL
                )
                """.trimIndent()
            )
            jdbcTemplate.execute(
                """
                CREATE INDEX IF NOT EXISTS $INDEX_NAME
                ON $TABLE_NAME
                USING hnsw (embedding vector_cosine_ops)
                """.trimIndent()
            )

            pgVectorReady = true
            logger.info("Hybrid vector store ready: table={}, dimensions={}", TABLE_NAME, embeddingDimensions)
        } catch (e: Exception) {
            pgVectorReady = false
            logger.warn("PGVector initialization failed. Falling back to Redis keyword search only.", e)
        }
    }

    override fun add(documents: List<Document>) {
        documents.forEach { document ->
            val content = document.text?.takeIf { it.isNotBlank() } ?: return@forEach
            val documentId = document.id.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
            val metadata = LinkedHashMap(document.metadata)
            metadata["id"] = documentId

            cacheDocument(documentId, content, metadata)

            if (!pgVectorReady) {
                return@forEach
            }

            runCatching {
                upsertPgVectorDocument(
                    id = documentId,
                    content = content,
                    metadata = metadata,
                    embedding = embeddingModel.embed(document)
                )
            }.onFailure { error ->
                logger.warn("Failed to upsert PGVector document: id={}", documentId, error)
            }
        }
    }

    override fun similaritySearch(searchRequest: SearchRequest): List<Document> {
        val query = searchRequest.query.trim()
        if (query.isBlank()) {
            return emptyList()
        }

        val pgResults = if (pgVectorReady) {
            runCatching { similaritySearchInPgVector(searchRequest) }
                .onFailure { error -> logger.warn("PGVector similarity search failed. Falling back to Redis.", error) }
                .getOrDefault(emptyList())
        } else {
            emptyList()
        }

        return if (pgResults.isNotEmpty()) {
            pgResults
        } else {
            similaritySearchInRedis(searchRequest)
        }
    }

    override fun delete(idList: List<String>) {
        idList.forEach { id ->
            if (pgVectorReady) {
                runCatching { jdbcTemplate.update("DELETE FROM $TABLE_NAME WHERE id = ?", id) }
                    .onFailure { error -> logger.warn("Failed to delete PGVector document: id={}", id, error) }
            }

            redisTemplate.delete(contentKey(id))
            redisTemplate.delete(metadataKey(id))
            redisTemplate.opsForSet().remove(DOCUMENT_IDS_SET_KEY, id)
        }
    }

    override fun delete(filterExpression: Filter.Expression) {
        throw UnsupportedOperationException("RedisVectorStore does not support filter-based deletion.")
    }

    private fun upsertPgVectorDocument(
        id: String,
        content: String,
        metadata: Map<String, Any>,
        embedding: FloatArray
    ) {
        val sql = """
            INSERT INTO $TABLE_NAME (id, content, metadata, embedding)
            VALUES (?, ?, ?::jsonb, ?)
            ON CONFLICT (id) DO UPDATE
            SET content = EXCLUDED.content,
                metadata = EXCLUDED.metadata,
                embedding = EXCLUDED.embedding
        """.trimIndent()

        jdbcTemplate.update(
            sql,
            id,
            content,
            objectMapper.writeValueAsString(metadata),
            PGvector(embedding)
        )
    }

    private fun similaritySearchInPgVector(searchRequest: SearchRequest): List<Document> {
        val threshold = searchRequest.similarityThreshold.coerceIn(0.0, 1.0)
        val maxDistance = if (threshold <= 0.0) Double.MAX_VALUE else 1.0 - threshold
        val queryEmbedding = PGvector(embeddingModel.embed(searchRequest.query))

        val sql = """
            SELECT id, content, metadata, (embedding <=> ?) AS distance
            FROM $TABLE_NAME
            WHERE (embedding <=> ?) <= ?
            ORDER BY distance ASC
            LIMIT ?
        """.trimIndent()

        return jdbcTemplate.query(
            sql,
            { rs, _ ->
                val id = rs.getString("id")
                val content = rs.getString("content")
                val distance = rs.getDouble("distance")
                val metadata = readMetadata(rs.getString("metadata")).toMutableMap()
                metadata["id"] = id
                metadata[DocumentMetadata.DISTANCE.value()] = distance

                Document.builder()
                    .id(id)
                    .text(content)
                    .metadata(metadata)
                    .score((1.0 - distance).coerceAtLeast(0.0))
                    .build()
            },
            queryEmbedding,
            queryEmbedding,
            maxDistance,
            searchRequest.topK
        )
    }

    private fun similaritySearchInRedis(searchRequest: SearchRequest): List<Document> {
        val ids = redisTemplate.opsForSet().members(DOCUMENT_IDS_SET_KEY)
            ?.map { it.toString() }
            .orEmpty()

        return ids.mapNotNull { id ->
            val content = redisTemplate.opsForValue().get(contentKey(id))?.toString() ?: return@mapNotNull null
            val metadataJson = redisTemplate.opsForValue().get(metadataKey(id))
            val metadata = readMetadata(metadataJson).toMutableMap()
            val score = calculateSimilarity(searchRequest.query, content)
            if (score < searchRequest.similarityThreshold) {
                return@mapNotNull null
            }

            metadata["id"] = id
            metadata[DocumentMetadata.DISTANCE.value()] = 1.0 - score

            Document.builder()
                .id(id)
                .text(content)
                .metadata(metadata)
                .score(score)
                .build()
        }.sortedByDescending { it.score ?: 0.0 }
            .take(searchRequest.topK)
    }

    private fun cacheDocument(id: String, content: String, metadata: Map<String, Any>) {
        redisTemplate.opsForValue().set(contentKey(id), content)
        redisTemplate.opsForValue().set(metadataKey(id), objectMapper.writeValueAsString(metadata))
        redisTemplate.opsForSet().add(DOCUMENT_IDS_SET_KEY, id)
    }

    private fun readMetadata(raw: Any?): Map<String, Any> {
        if (raw == null) {
            return emptyMap()
        }
        if (raw is Map<*, *>) {
            return raw.entries.associate { it.key.toString() to it.value.toString() }
        }
        return runCatching {
            objectMapper.readValue(raw.toString(), metadataTypeRef)
        }.getOrDefault(emptyMap())
    }

    private fun calculateSimilarity(query: String, content: String): Double {
        val queryWords = query.lowercase()
            .split(Regex("\\s+"))
            .map { it.trim() }
            .filter { it.length > 1 }

        if (queryWords.isEmpty()) {
            return 0.0
        }

        val contentLower = content.lowercase()
        val matches = queryWords.count { contentLower.contains(it) }
        return matches.toDouble() / queryWords.size
    }

    private fun contentKey(id: String) = "rag:document:content:$id"

    private fun metadataKey(id: String) = "rag:document:metadata:$id"

    companion object {
        private const val TABLE_NAME = "ai_document_vector"
        private const val INDEX_NAME = "ai_document_vector_embedding_idx"
        private const val DOCUMENT_IDS_SET_KEY = "rag:document:ids"
        private const val DEFAULT_DIMENSIONS = 384
    }
}
