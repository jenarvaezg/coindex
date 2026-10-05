package com.jenarvaezg.coindex.data.update

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * How far an install attempt got, in terms of what is left for the collector to do. Each refusal
 * has its own message ([com.jenarvaezg.coindex.ui.installOutcomeMessage]); [Handed] says nothing,
 * since the system's dialog comes next.
 */
sealed interface InstallOutcome {
    /** The permission screen was opened; the collector has to come back and press again. */
    data object PermissionAsked : InstallOutcome

    /** Nothing on this device handles that screen, so the APK has to be installed by hand. */
    data object PermissionUnavailable : InstallOutcome

    /** The APK reached the system installer, which is now asking the collector to confirm. */
    data object Handed : InstallOutcome

    data object NoInstaller : InstallOutcome

    data class Failed(val error: Throwable) : InstallOutcome
}

/**
 * Looking for a newer APK, and installing it (ADR 0011). The check runs on launch, on every return
 * to the foreground and on a timer, so the interval decides when GitHub may be asked, here where a
 * test can reach the clock. [UpdateChecker] turns failures into [UpdateStatus.Unavailable]: an
 * unreachable GitHub never interrupts the collection.
 */
class UpdateFlow(
    private val checker: UpdateChecker,
    private val installer: UpdateInstaller,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private var lastCheckMillis: Long? = null

    /**
     * One check at a time, like [com.jenarvaezg.coindex.data.CallBudgetGate]: two concurrent checks
     * would both read the old stamp; serialized, the second sees the first's and skips.
     */
    private val asking = Mutex()

    /**
     * What GitHub says, or null when it is not time to ask yet. Null rather than
     * [UpdateStatus.UpToDate], so a skipped check leaves the banner as it was.
     */
    suspend fun check(force: Boolean = false): UpdateStatus? = asking.withLock {
        val now = nowMillis()
        if (!force && !shouldCheckForUpdate(lastCheckMillis, now)) return null
        // Stamped before the request, so a second check during it doesn't ask again.
        lastCheckMillis = now
        checker.check()
    }

    /**
     * Downloads the published APK and hands it to the system installer, asking for the special
     * install permission first if it has not been granted yet.
     *
     * @param onDownloadStart called only once there is a download to wait for, so the button is
     *   never disabled for the two branches that end before anything is fetched.
     */
    suspend fun install(
        available: UpdateStatus.Available,
        onDownloadStart: () -> Unit = {},
    ): InstallOutcome {
        if (!installer.canInstall()) {
            return if (installer.requestInstallPermission()) {
                InstallOutcome.PermissionAsked
            } else {
                InstallOutcome.PermissionUnavailable
            }
        }
        onDownloadStart()
        return runCatching {
            installer.download(available.apkUrl, available.manifest.versionCode)
        }.fold(
            onSuccess = { apk ->
                if (installer.install(apk)) InstallOutcome.Handed else InstallOutcome.NoInstaller
            },
            onFailure = { error -> InstallOutcome.Failed(error) },
        )
    }
}
