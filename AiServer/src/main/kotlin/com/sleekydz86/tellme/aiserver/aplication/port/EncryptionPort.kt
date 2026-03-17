package com.sleekydz86.tellme.aiserver.aplication.port

interface EncryptionPort {
    fun encrypt(plain: String): String
    fun decrypt(cipher: String): String
    fun encryptBytes(plain: ByteArray): Pair<ByteArray, String>
    fun decryptBytes(cipher: ByteArray, ivBase64: String): ByteArray
}
