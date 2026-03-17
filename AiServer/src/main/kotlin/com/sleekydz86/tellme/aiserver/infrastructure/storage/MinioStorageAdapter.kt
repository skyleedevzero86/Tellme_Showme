package com.sleekydz86.tellme.aiserver.infrastructure.storage

import com.sleekydz86.tellme.aiserver.aplication.port.EncryptionPort
import com.sleekydz86.tellme.aiserver.aplication.port.StoragePort
import io.minio.BucketExistsArgs
import io.minio.GetObjectArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import org.slf4j.LoggerFactory
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

@Component
class MinioStorageAdapter(
    private val minioClient: MinioClient,
    private val encryptionPort: EncryptionPort,
    private val redisTemplate: RedisTemplate<String, Any>,
    @Value("\${minio.bucket:ko-sse3-uploads}") private val bucket: String
) : StoragePort {

    private val logger = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    fun ensureBucket() {
        try {
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build())
                logger.info("MinIO 버킷 생성됨: {}", bucket)
            }
        } catch (e: Exception) {
            logger.warn("MinIO 버킷 확인/생성 실패: {}", e.message)
        }
    }

    companion object {
        private const val ENC_PREFIX = "enc:"
        private const val IV_TTL_DAYS = 365L
    }

    override fun save(file: MultipartFile, userId: String): String {
        val objectKey = "uploads/$userId/${UUID.randomUUID()}_${file.originalFilename ?: "파일"}"
        val bytes = file.inputStream.readAllBytes()
        val (encrypted, ivBase64) = encryptionPort.encryptBytes(bytes)
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(bucket)
                .`object`(objectKey)
                .stream(ByteArrayInputStream(encrypted), encrypted.size.toLong(), -1)
                .contentType(file.contentType ?: "application/octet-stream")
                .build()
        )
        redisTemplate.opsForValue().set(ENC_PREFIX + objectKey, ivBase64, IV_TTL_DAYS, TimeUnit.DAYS)
        logger.info("MinIO 저장(암호화): bucket={}, objectKey={}", bucket, objectKey)
        return objectKey
    }

    override fun get(objectKey: String): Resource {
        val encrypted = minioClient.getObject(
            GetObjectArgs.builder().bucket(bucket).`object`(objectKey).build()
        ).readAllBytes()
        val ivBase64 = (redisTemplate.opsForValue().get(ENC_PREFIX + objectKey) as? String) ?: throw IllegalStateException("IV를 찾을 수 없음: $objectKey")
        val decrypted = encryptionPort.decryptBytes(encrypted, ivBase64)
        return ByteArrayResource(decrypted)
    }
}
