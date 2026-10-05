package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.Credentials

/**
 * What a credential form decided about what was typed into it. Pure rules about text, kept apart
 * from the keystore write so they can be tested.
 */
sealed interface CredentialsEntry {
    /** Everything the form asked for, parsed. */
    data class Accepted(val credentials: Credentials) : CredentialsEntry

    /** In the collector's words, shown next to the field. */
    data class Refused(val problem: String) : CredentialsEntry
}

/**
 * The onboarding form: an API key and the number in the collector's Numista profile URL. One
 * message for both fields: neither has been filled in before, so naming one would be a guess.
 */
fun onboardingEntry(apiKey: String, userId: String): CredentialsEntry {
    val credentials = typedCredentials(apiKey, userId)
        ?: return CredentialsEntry.Refused(
            "Introduce una API key y un identificador de usuario válidos.",
        )
    return CredentialsEntry.Accepted(credentials)
}

/**
 * The «Credenciales» form. Unlike onboarding, the message names the field, since this screen is
 * visited to change one of the two.
 */
fun credentialsEntry(apiKey: String, userId: String): CredentialsEntry {
    if (apiKey.isBlank()) return CredentialsEntry.Refused("La API key no puede estar vacía.")
    val credentials = typedCredentials(apiKey, userId)
        ?: return CredentialsEntry.Refused(
            "El identificador de usuario es el número de la URL de tu perfil de Numista.",
        )
    return CredentialsEntry.Accepted(credentials)
}

/**
 * Both fields parsed, or null when they don't make a pair. The user id is the positive number in
 * the profile URL; a pasted URL or a zero is refused.
 */
private fun typedCredentials(apiKey: String, userId: String): Credentials? {
    val parsedUserId = userId.trim().toLongOrNull() ?: return null
    if (apiKey.isBlank() || parsedUserId <= 0) return null
    return Credentials(apiKey.trim(), parsedUserId)
}
