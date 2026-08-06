package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.update.InstallOutcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * What the collector reads when installing a new APK goes one way or the other (#220, ADR 0011).
 *
 * Four of the five outcomes are refusals, and they lived inside the one class with no test.
 */
class InstallMessagesTest {
    /**
     * Each refusal of installing ends in something the collector can do on this phone.
     *
     * There is no store to fall back on (ADR 0011), so «no se pudo» with no way out would leave the
     * only route to a new version behind a sentence that names no action.
     */
    @Test
    fun `every way an install can refuse names what to do next`() {
        assertEquals(
            "Concede a Coindex permiso para instalar aplicaciones y vuelve a pulsar Instalar.",
            installOutcomeMessage(InstallOutcome.PermissionAsked),
        )
        assertEquals(
            "Este dispositivo no permite conceder el permiso de instalación: descarga el APK " +
                "desde GitHub e instálalo a mano.",
            installOutcomeMessage(InstallOutcome.PermissionUnavailable),
        )
        assertEquals(
            "No hay instalador de paquetes en este dispositivo: instala el APK a mano.",
            installOutcomeMessage(InstallOutcome.NoInstaller),
        )
        assertEquals(
            "No se pudo descargar la actualización: HTTP 503 al descargar el APK",
            installOutcomeMessage(
                InstallOutcome.Failed(IllegalStateException("HTTP 503 al descargar el APK")),
            ),
        )
    }

    @Test
    fun `an install the system took over says nothing at all`() {
        // The system's own dialog is on screen asking for the confirmation; a snackbar under it
        // would be Coindex talking over the only screen that matters.
        assertNull(installOutcomeMessage(InstallOutcome.Handed))
    }
}
