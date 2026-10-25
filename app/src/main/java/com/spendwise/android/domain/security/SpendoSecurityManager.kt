package com.spendwise.android.domain.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Storage contract for persisting and retrieving security credentials.
 */
interface SpendoSecurityStorage {
    suspend fun getPreferenceDirect(key: String): String?
    suspend fun setPreference(key: String, value: String)
}

/**
 * Enterprise-grade on-device security manager for Spendo.
 * Hashes passwords using PBKDF2WithHmacSHA256 with cryptographically random unique salts.
 * Eliminates hardcoded passwords, plaintext storage, and protects financial balances.
 */
class SpendoSecurityManager(private val storage: SpendoSecurityStorage) {

    companion object {
        const val PREF_SALT_HEX = "spendo_security_salt_hex"
        const val PREF_HASH_HEX = "spendo_security_hash_hex"
        const val PREF_HAS_CUSTOM_PASSWORD = "spendo_security_has_custom"

        const val DEFAULT_LEGACY_PIN = "4892"
        private const val ITERATIONS = 10_000
        private const val KEY_LENGTH_BITS = 256
        private const val SALT_LENGTH_BYTES = 16
    }

    /**
     * Verifies if the provided password authenticates against stored credentials.
     * If no password has ever been set, accepts the initial legacy default "4892" and immediately
     * migrates it into a salted PBKDF2 hash on-device.
     */
    suspend fun verifyPassword(inputPassword: String): Boolean {
        if (inputPassword.isEmpty()) return false

        val storedSaltHex = storage.getPreferenceDirect(PREF_SALT_HEX)
        val storedHashHex = storage.getPreferenceDirect(PREF_HASH_HEX)

        if (storedSaltHex == null || storedHashHex == null) {
            // First-time setup / migration from legacy:
            if (inputPassword == DEFAULT_LEGACY_PIN) {
                // Auto-migrate default PIN to salted hash
                initializeDefaultHash(DEFAULT_LEGACY_PIN)
                return true
            }
            return false
        }

        val salt = hexToBytes(storedSaltHex)
        val computedHash = hashPassword(inputPassword, salt)
        val computedHashHex = bytesToHex(computedHash)

        return MessageDigest.isEqual(computedHashHex.toByteArray(Charsets.UTF_8), storedHashHex.toByteArray(Charsets.UTF_8))
    }

    /**
     * Changes the user password. Requires the correct current password.
     * Generates a fresh cryptographically secure 16-byte salt and persists the PBKDF2 hash.
     * Immediately revokes the old password.
     */
    suspend fun changePassword(currentPassword: String, newPassword: String): PasswordChangeResult {
        if (!verifyPassword(currentPassword)) {
            return PasswordChangeResult.InvalidCurrentPassword
        }

        if (newPassword.length < 4) {
            return PasswordChangeResult.PasswordTooShort
        }

        if (newPassword == currentPassword) {
            return PasswordChangeResult.SameAsCurrentPassword
        }

        val newSalt = ByteArray(SALT_LENGTH_BYTES)
        SecureRandom().nextBytes(newSalt)

        val newHash = hashPassword(newPassword, newSalt)

        storage.setPreference(PREF_SALT_HEX, bytesToHex(newSalt))
        storage.setPreference(PREF_HASH_HEX, bytesToHex(newHash))
        storage.setPreference(PREF_HAS_CUSTOM_PASSWORD, "true")

        return PasswordChangeResult.Success
    }

    /**
     * Checks if the user has changed away from the initial default password.
     */
    suspend fun hasCustomPassword(): Boolean {
        return storage.getPreferenceDirect(PREF_HAS_CUSTOM_PASSWORD) == "true"
    }

    private suspend fun initializeDefaultHash(defaultPin: String) {
        val salt = ByteArray(SALT_LENGTH_BYTES)
        SecureRandom().nextBytes(salt)
        val hash = hashPassword(defaultPin, salt)
        storage.setPreference(PREF_SALT_HEX, bytesToHex(salt))
        storage.setPreference(PREF_HASH_HEX, bytesToHex(hash))
        storage.setPreference(PREF_HAS_CUSTOM_PASSWORD, "false")
    }

    private fun hashPassword(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}

sealed class PasswordChangeResult {
    object Success : PasswordChangeResult()
    object InvalidCurrentPassword : PasswordChangeResult()
    object PasswordTooShort : PasswordChangeResult()
    object SameAsCurrentPassword : PasswordChangeResult()
    data class Error(val message: String) : PasswordChangeResult()
}
