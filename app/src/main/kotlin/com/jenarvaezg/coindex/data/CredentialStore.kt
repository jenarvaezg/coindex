package com.jenarvaezg.coindex.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val PREFS = "coindex-credentials"
private const val KEY_ALIAS = "coindex-api-key"
private const val KEY_API_KEY = "numista_api_key"
private const val KEY_USER_ID = "numista_user_id"
private const val GCM_TAG_BITS = 128
private const val IV_BYTES = 12

/** Internal monthly cap, kept below the observed ~2.000 limit to leave margin. */
const val DEFAULT_MONTHLY_BUDGET: Int = 1500

data class Credentials(val apiKey: String, val userId: Long)

/**
 * The collector's own Numista credentials.
 *
 * An interface because the real one is an Android Keystore away: onboarding, signing out and the
 * settings form are decisions about what was typed, and none of them should need a device to be
 * read.
 */
interface CredentialStore {
    fun credentials(): Credentials?

    fun save(apiKey: String, userId: Long)

    fun clear()
}

/**
 * Stores the collector's own Numista credentials on the device.
 *
 * The API key is encrypted with an AES/GCM key that lives in the Android Keystore and never
 * leaves it; only the ciphertext reaches shared preferences. The user id is stored as-is.
 */
class KeystoreCredentialStore(context: Context) : CredentialStore {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override fun credentials(): Credentials? {
        val userId = prefs.getLong(KEY_USER_ID, -1L).takeIf { it >= 0 } ?: return null
        val apiKey = decryptedApiKey()?.takeIf(String::isNotBlank) ?: return null
        return Credentials(apiKey, userId)
    }

    override fun save(apiKey: String, userId: Long) {
        prefs.edit()
            .putString(KEY_API_KEY, encrypt(apiKey))
            .putLong(KEY_USER_ID, userId)
            .apply()
    }

    override fun clear() {
        prefs.edit().remove(KEY_API_KEY).remove(KEY_USER_ID).apply()
    }

    private fun decryptedApiKey(): String? {
        val stored = prefs.getString(KEY_API_KEY, null) ?: return null
        val decoded = runCatching { Base64.decode(stored, Base64.NO_WRAP) }.getOrNull() ?: return null
        if (decoded.size <= IV_BYTES) return null
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_BITS, decoded, 0, IV_BYTES)
        return runCatching {
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), spec)
            String(cipher.doFinal(decoded, IV_BYTES, decoded.size - IV_BYTES), Charsets.UTF_8)
        }.getOrNull()
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val ciphertext = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv + ciphertext, Base64.NO_WRAP)
    }

    private fun secretKey(): SecretKey {
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
}
