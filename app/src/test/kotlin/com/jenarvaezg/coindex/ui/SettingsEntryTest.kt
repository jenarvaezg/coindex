package com.jenarvaezg.coindex.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * What the two credential forms make of what was typed (#220).
 *
 * It used to be three branches of validation braided into a keystore write, a preferences write and
 * a snackbar, inside the one file with no tests — so the sentence a collector reads when they paste
 * their profile URL instead of the number in it could only be checked by running the app.
 */
class SettingsEntryTest {
    @Test
    fun `the number in the profile URL is what a user id is`() {
        val entry = assertIs<SettingsEntry.Accepted>(settingsEntry(" key ", " 2104 "))

        assertEquals(2104L, entry.credentials.userId)
        // Trimmed, because a key pasted from a browser brings a space with it.
        assertEquals("key", entry.credentials.apiKey)
    }

    @Test
    fun `each field of the settings form is named in its own complaint`() {
        assertEquals(
            "La API key no puede estar vacía.",
            assertIs<SettingsEntry.Refused>(settingsEntry("  ", "2104")).problem,
        )
        assertEquals(
            "El identificador de usuario es el número de la URL de tu perfil de Numista.",
            assertIs<SettingsEntry.Refused>(
                settingsEntry("key", "en.numista.com/user/2104"),
            ).problem,
        )
    }

    @Test
    fun `a user id of zero or below is not a profile`() {
        assertIs<SettingsEntry.Refused>(settingsEntry("key", "0"))
        assertIs<SettingsEntry.Refused>(settingsEntry("key", "-2104"))
    }

    @Test
    fun `onboarding names neither field, because neither has been filled in before`() {
        val problem = "Introduce una API key y un identificador de usuario válidos."

        assertEquals(problem, assertIs<SettingsEntry.Refused>(onboardingEntry("", "2104")).problem)
        assertEquals(problem, assertIs<SettingsEntry.Refused>(onboardingEntry("key", "")).problem)
    }

    @Test
    fun `onboarding accepts the two credentials`() {
        val entry = assertIs<SettingsEntry.Accepted>(onboardingEntry("key", "2104"))

        assertEquals(2104L, entry.credentials.userId)
    }
}
