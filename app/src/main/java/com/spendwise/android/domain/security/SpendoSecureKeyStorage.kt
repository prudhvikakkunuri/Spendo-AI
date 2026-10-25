package com.spendwise.android.domain.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Enterprise-grade hardware/software Keystore encryption for securing AI provider API keys.
 * Uses AES-256 in GCM authenticated mode.
 * Protects secrets from plaintext persistence, SQLite database inspection, and export leaks.
 * Supports JVM JUnit testing environments with automatic fallback.
 */
class SpendoSecureKeyStorage(private val storage: SpendoSecurityStorage) {

    companion object {
        private const val ANDROID_KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "spendo_ai_master_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val GCM_IV_LENGTH_BYTES = 12

        private const val PREF_KEY_PREFIX = "enc_api_key_"
        private const val PREF_ACTIVE_PROVIDER = "spendo_active_ai_provider"
        private const val PREF_ACTIVE_MODEL = "spendo_active_ai_model"
    }

    private var fallbackJvmKey: SecretKey? = null

    private fun getOrCreateKey(): SecretKey {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER)
            keyStore.load(null)

            if (keyStore.containsAlias(KEY_ALIAS)) {
                val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
                if (entry != null) return entry.secretKey
            }

            // Generate new AES key inside AndroidKeyStore
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE_PROVIDER)
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()

            keyGenerator.init(spec)
            return keyGenerator.generateKey()
        } catch (_: Exception) {
            // JVM testing or unsupported Keystore fallback
            if (fallbackJvmKey == null) {
                val keyGen = KeyGenerator.getInstance("AES")
                keyGen.init(256)
                fallbackJvmKey = keyGen.generateKey()
            }
            return fallbackJvmKey!!
        }
    }

    /**
     * Encrypts plaintext string using AES-256-GCM and returns IV + ciphertext encoded in Base64.
     */
    fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        val key = getOrCreateKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        val ivBase64 = encodeBase64(iv)
        val cipherBase64 = encodeBase64(encryptedBytes)
        return "$ivBase64:$cipherBase64"
    }

    /**
     * Decrypts an IV + ciphertext Base64 bundle using AES-256-GCM.
     */
    fun decrypt(encryptedPayload: String): String? {
        if (encryptedPayload.isBlank() || !encryptedPayload.contains(":")) return null
        return try {
            val parts = encryptedPayload.split(":", limit = 2)
            val iv = decodeBase64(parts[0])
            val encryptedBytes = decodeBase64(parts[1])

            val key = getOrCreateKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)

            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun saveApiKey(providerId: String, apiKey: String) {
        val encrypted = encrypt(apiKey)
        storage.setPreference(PREF_KEY_PREFIX + providerId.lowercase(), encrypted)
    }

    suspend fun getApiKey(providerId: String): String? {
        val encrypted = storage.getPreferenceDirect(PREF_KEY_PREFIX + providerId.lowercase()) ?: return null
        return decrypt(encrypted)
    }

    suspend fun deleteApiKey(providerId: String) {
        storage.setPreference(PREF_KEY_PREFIX + providerId.lowercase(), "")
    }

    suspend fun saveActiveProvider(providerId: String, model: String) {
        storage.setPreference(PREF_ACTIVE_PROVIDER, providerId)
        storage.setPreference(PREF_ACTIVE_MODEL, model)
    }

    suspend fun getActiveProvider(): String? {
        return storage.getPreferenceDirect(PREF_ACTIVE_PROVIDER)
    }

    suspend fun getActiveModel(): String? {
        return storage.getPreferenceDirect(PREF_ACTIVE_MODEL)
    }

    private fun encodeBase64(bytes: ByteArray): String {
        return try {
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (_: Exception) {
            java.util.Base64.getEncoder().encodeToString(bytes)
        }
    }

    private fun decodeBase64(base64: String): ByteArray {
        return try {
            Base64.decode(base64, Base64.NO_WRAP)
        } catch (_: Exception) {
            java.util.Base64.getDecoder().decode(base64)
        }
    }
}
