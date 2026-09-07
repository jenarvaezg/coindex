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
 * Stores the collector's own Numista credentials on the device.
 *
 * The API key is encrypted with an AES/GCM key that lives in the Android Keystore and never
 * leaves it; only the ciphertext reaches the named values. The user id is stored as-is.
 *
 * A plain class and no longer an interface with a fake of its own (#546), and the seam moved to the
 * thing that actually needs a device: **the key**, which arrives as a function and is
 * [keystoreSecret] in the app. What was untestable here was never the file — onboarding, signing out
 * and the settings form were readable enough behind a fake store — it was the keystore, and a fake
 * store answered by never encrypting anything. With the key handed in, a JVM test runs the round
 * trip whole, which is the half of this class that had no test at all.
 */
class StoredCredentials(
    private val values: NamedValues,
    private val secret: () -> SecretKey,
) {
    fun credentials(): Credentials? {
        val userId = values.int64(KEY_USER_ID)?.takeIf { it >= 0 } ?: return null
        val apiKey = decryptedApiKey()?.takeIf(String::isNotBlank) ?: return null
        return Credentials(apiKey, userId)
    }

    fun save(apiKey: String, userId: Long) = values.write(
        mapOf(
            KEY_API_KEY to Stored.Text(encrypt(apiKey)),
            KEY_USER_ID to Stored.Int64(userId),
        ),
    )

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
        // The platform's own encoder and no longer `android.util.Base64` with `NO_WRAP`: both are
        // RFC 4648 padded with no line breaks, so what is already on a phone still decodes, and this
        // one exists off a device too.
        return Base64.getEncoder().encodeToString(cipher.iv + ciphertext)
    }
}

/**
 * The AES/GCM key of this install, from the Android Keystore, generated the first time it is asked
 * for.
 *
 * It never leaves the keystore: what travels is the cipher, initialised with it.
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
