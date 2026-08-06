package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.update.InstallOutcome

/** Said the moment the APK starts coming down, and replaced by whatever the install did. */
const val UPDATE_DOWNLOADING_MESSAGE: String = "Descargando la actualización…"

/**
 * What an install attempt leaves the collector to do, or nothing when the system took over.
 *
 * Every one of the four refusals ends in an action the collector can take on this phone, because
 * an app distributed outside a store has no other way out: grant the permission and press again,
 * or fetch the APK from GitHub and install it by hand (ADR 0011).
 */
fun installOutcomeMessage(outcome: InstallOutcome): String? = when (outcome) {
    // Nothing to say: the system's own installer is on screen, asking for the confirmation that
    // is the whole point of a sideloaded update.
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
