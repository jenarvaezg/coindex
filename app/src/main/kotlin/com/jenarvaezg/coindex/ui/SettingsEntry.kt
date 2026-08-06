package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.Credentials

/**
 * What the credential form decided about what was typed into it.
 *
 * A decision and not a write: parsing «2104» into a user id, refusing a blank key and refusing a
 * cap of zero are three rules about text, and text is the one thing a test can hold. What used to
 * be here — the same three rules interleaved with a keystore write, a preferences write and a
 * snackbar — could only be read by running the app.
 */
sealed interface SettingsEntry {
    /**
     * Everything the form asked for, parsed.
     *
     * @param budgetCap null on the onboarding form, which does not show the cap: the collector who
     *   has not synced once yet has no idea what a month of theirs costs, and the default is what
     *   the first month is for.
     */
    data class Accepted(val credentials: Credentials, val budgetCap: Int?) : SettingsEntry

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
    return SettingsEntry.Accepted(credentials, budgetCap = null)
}

/**
 * The settings form, which is the same two fields plus the monthly cap.
 *
 * Read as one unit and refused as one unit: the three writes behind it are not a transaction, so a
 * form that stored the key and then complained about the cap would leave the collector's settings
 * half saved with no way to tell which half.
 *
 * Here each field is named, because this screen is visited to change **one** of the three and a
 * complaint that does not say which one is a complaint about the wrong field.
 */
fun settingsEntry(apiKey: String, userId: String, budgetCap: String): SettingsEntry {
    if (apiKey.isBlank()) return SettingsEntry.Refused("La API key no puede estar vacía.")
    val credentials = typedCredentials(apiKey, userId)
        ?: return SettingsEntry.Refused(
            "El identificador de usuario es el número de la URL de tu perfil de Numista.",
        )
    val cap = budgetCap.trim().toIntOrNull()?.takeIf { it > 0 }
        ?: return SettingsEntry.Refused(
            "El techo de presupuesto tiene que ser un número de llamadas mayor que cero.",
        )
    return SettingsEntry.Accepted(credentials, cap)
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

/** Said once the three of them are stored. */
const val SETTINGS_SAVED_MESSAGE: String = "Ajustes guardados."
