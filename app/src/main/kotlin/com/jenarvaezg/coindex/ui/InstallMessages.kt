package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.update.InstallOutcome

/** Said the moment the APK starts coming down, and replaced by whatever the install did. */
const val UPDATE_DOWNLOADING_MESSAGE: String = "Descargando la actualización…"

fun updateAvailableLabel(versionName: String): String = "NUEVA VERSIÓN $versionName"

/**
 * The banner's button. «Instalar», not «Actualizar», to match «vuelve a pulsar Instalar» in
 * [installOutcomeMessage] (ADR 0011).
 */
fun updateInstallLabel(downloading: Boolean): String =
    if (downloading) "Descargando…" else "Instalar"

/**
 * What an install attempt leaves the collector to do, or null when the system installer took
 * over. Where there is something to do on this phone it is said: grant the permission and press
 * again, or install the APK by hand (ADR 0011).
 */
fun installOutcomeMessage(outcome: InstallOutcome): String? = when (outcome) {
    // The system installer is on screen, asking for confirmation.
    InstallOutcome.Handed -> null
    InstallOutcome.PermissionAsked ->
        "Concede a Coindex permiso para instalar aplicaciones y vuelve a pulsar Instalar."
    InstallOutcome.PermissionUnavailable ->
        "Este dispositivo no permite conceder el permiso de instalación: descarga el APK desde " +
            "GitHub e instálalo a mano."
    InstallOutcome.NoInstaller ->
        "No hay instalador de paquetes en este dispositivo: instala el APK a mano."
    is InstallOutcome.Failed ->
        "No se pudo descargar la actualización: ${outcome.error.message}"
}
