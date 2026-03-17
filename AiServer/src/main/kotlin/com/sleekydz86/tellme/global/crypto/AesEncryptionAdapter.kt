package com.sleekydz86.tellme.global.crypto

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Component
class AesEncryptionAdapter(
    @Value("\${app.encryption.secret:ko-sse3-default-32byte-secret-key!!}") private val secret: String
) : EncryptionPort {

    private val algorithm = "AES"
    private val gcmAlgorithm = "AES/GCM/NoPadding"
    private val gcmTagLength = 128
    private val ivLength = 12
    private val keyBytes = secret.padEnd(32).take(32).toByteArray(Charsets.UTF_8)
    private val keySpec = SecretKeySpec(keyBytes, algorithm)

    override fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(algorithm)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec)
        val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(encrypted)
    }

    override fun decrypt(cipher: String): String {
        val decoded = Base64.getDecoder().decode(cipher)
        val c = Cipher.getInstance(algorithm)
        c.init(Cipher.DECRYPT_MODE, keySpec)
        return String(c.doFinal(decoded), Charsets.UTF_8)
    }

    override fun encryptBytes(plain: ByteArray): Pair<ByteArray, String> {
        val iv = ByteArray(ivLength).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(gcmAlgorithm)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, GCMParameterSpec(gcmTagLength, iv))
        val encrypted = cipher.doFinal(plain)
        return encrypted to Base64.getEncoder().encodeToString(iv)
    }

    override fun decryptBytes(cipher: ByteArray, ivBase64: String): ByteArray {
        val iv = Base64.getDecoder().decode(ivBase64)
        val cipherObj = Cipher.getInstance(gcmAlgorithm)
        cipherObj.init(Cipher.DECRYPT_MODE, keySpec, GCMParameterSpec(gcmTagLength, iv))
        return cipherObj.doFinal(cipher)
    }
}
