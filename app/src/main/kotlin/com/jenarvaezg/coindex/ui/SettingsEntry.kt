package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.Credentials

/**
 * What the credential form decided about what was typed into it.
 *
 * A decision and not a write: parsing «2104» into a user id and refusing a blank key are rules
 * about text, and text is the one thing a test can hold. What used to be here — the same rules
 * interleaved with a keystore write and a snackbar — could only be read by running the app.
 */
sealed interface SettingsEntry {
    /** Everything the form asked for, parsed. */
    data class Accepted(val credentials: Credentials) : SettingsEntry

    /** What is wrong, in the collector's words, next to the field that caused it. */
    data class Refused(val problem: String) : SettingsEntry
}

/**
 * The onboarding form: an API key and the number in the collector's Numista profile URL.
 *
 * One sentence for both fields, unlike [settingsEntry] below, and that is deliberate: on the first
 * screen of the app neither field has been filled in before, so naming one of the two would be
 * guessing which of them the collector got wrong.
 */
fun onboardingEntry(apiKey: String, userId: String): SettingsEntry {
    val credentials = typedCredentials(apiKey, userId)
        ?: return SettingsEntry.Refused(
            "Introduce una API key y un identificador de usuario válidos.",
        )
    return SettingsEntry.Accepted(credentials)
}

/**
 * The settings form, which has the same two fields as onboarding.
 *
 * Here each field is named, because this screen is visited to change **one** of the two and a
 * complaint that does not say which one is a complaint about the wrong field.
 */
fun settingsEntry(apiKey: String, userId: String): SettingsEntry {
    if (apiKey.isBlank()) return SettingsEntry.Refused("La API key no puede estar vacía.")
    val credentials = typedCredentials(apiKey, userId)
        ?: return SettingsEntry.Refused(
            "El identificador de usuario es el número de la URL de tu perfil de Numista.",
        )
    return SettingsEntry.Accepted(credentials)
}

/**
 * The two fields both forms share, parsed, or null when they do not make a pair.
 *
 * The user id is **the number in the profile URL** and nothing else: a collector who pastes the
 * whole URL has typed something that is not an id, and so has one who types a zero.
 */
private fun typedCredentials(apiKey: String, userId: String): Credentials? {
    val parsedUserId = userId.trim().toLongOrNull() ?: return null
    if (apiKey.isBlank() || parsedUserId <= 0) return null
    return Credentials(apiKey.trim(), parsedUserId)
}
