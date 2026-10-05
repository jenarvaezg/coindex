package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.update.InstallOutcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** What the collector reads when installing a new APK ends one way or another (#220, ADR 0011). */
class InstallMessagesTest {
    /** There is no store to fall back on (ADR 0011), so each refusal names an action. */
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
        // The system's confirmation dialog is on screen; a snackbar would talk over it.
        assertNull(installOutcomeMessage(InstallOutcome.Handed))
    }

    @Test
    fun `the banner names the version it is offering`() {
        assertEquals("NUEVA VERSIÓN 1.2.0", updateAvailableLabel("1.2.0"))
    }

    @Test
    fun `the button says Instalar, which is the word the refusals name`() {
        assertEquals("Instalar", updateInstallLabel(downloading = false))
        assertEquals("Descargando…", updateInstallLabel(downloading = true))
        assertTrue(
            updateInstallLabel(downloading = false) in
                installOutcomeMessage(InstallOutcome.PermissionAsked).orEmpty(),
        )
    }
}
