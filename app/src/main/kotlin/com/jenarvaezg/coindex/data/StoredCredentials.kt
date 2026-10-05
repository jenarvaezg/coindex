package com.jenarvaezg.coindex.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** The preferences file the credentials live in. */
const val CREDENTIAL_PREFERENCES: String = "coindex-credentials"

private const val KEY_ALIAS = "coindex-api-key"
private const val KEY_API_KEY = "numista_api_key"
private const val KEY_USER_ID = "numista_user_id"
private const val GCM_TAG_BITS = 128
private const val IV_BYTES = 12

/** Internal monthly cap, kept below the observed ~2.000 limit to leave margin. */
const val DEFAULT_MONTHLY_BUDGET: Int = 1500

data class Credentials(val apiKey: String, val userId: Long)

/**
 * Stores the collector's own Numista credentials on the device. The API key is encrypted with an
 * AES/GCM key that never leaves the Android Keystore; only the ciphertext reaches the named values.
 * The user id is stored as-is. The secret is passed in ([keystoreSecret] in the app) so a JVM test
 * can run the encryption round trip (#546).
 *
 * Saving also takes down the [RejectionWall]: the credentials wall has no clock (#579), and this is
 * the only place a key is written.
 */
class StoredCredentials(
    private val values: NamedValues,
    private val wall: RejectionWall,
    private val secret: () -> SecretKey,
) {
    fun credentials(): Credentials? {
        val userId = values.int64(KEY_USER_ID)?.takeIf { it >= 0 } ?: return null
        val apiKey = decryptedApiKey()?.takeIf(String::isNotBlank) ?: return null
        return Credentials(apiKey, userId)
    }

    /**
     * Writes the credentials down and gives Numista another chance. The wall falls whatever its
     * cause: saving the form means «prueba otra vez», a different key escapes a quota shared with
     * another phone (#562) as a corrected one escapes a refusal, and being wrong costs one call.
     */
    fun save(apiKey: String, userId: Long) {
        values.write(
            mapOf(
                KEY_API_KEY to Stored.Text(encrypt(apiKey)),
                KEY_USER_ID to Stored.Int64(userId),
            ),
        )
        wall.clear()
    }

    fun clear() = values.write(mapOf(KEY_API_KEY to null, KEY_USER_ID to null))

    private fun decryptedApiKey(): String? {
        val stored = values.text(KEY_API_KEY) ?: return null
        val decoded = runCatching { Base64.getDecoder().decode(stored) }.getOrNull() ?: return null
        if (decoded.size <= IV_BYTES) return null
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_BITS, decoded, 0, IV_BYTES)
        return runCatching {
            cipher.init(Cipher.DECRYPT_MODE, secret(), spec)
            String(cipher.doFinal(decoded, IV_BYTES, decoded.size - IV_BYTES), Charsets.UTF_8)
        }.getOrNull()
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secret())
        val ciphertext = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        // `java.util.Base64` matches the `android.util.Base64` `NO_WRAP` output older versions
        // stored (RFC 4648, padded, no line breaks) and also runs off a device.
        return Base64.getEncoder().encodeToString(cipher.iv + ciphertext)
    }
}

/**
 * The AES/GCM key of this install, from the Android Keystore, generated the first time it is asked
 * for. It never leaves the keystore.
 */
fun keystoreSecret(): SecretKey {
    val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
    val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
    generator.init(
        KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .build(),
    )
    return generator.generateKey()
}
