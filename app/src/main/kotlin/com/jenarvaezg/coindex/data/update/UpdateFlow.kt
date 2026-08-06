package com.jenarvaezg.coindex.data.update

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * How far an install attempt got, in terms of what is left for the collector to do.
 *
 * Four of the five are refusals, and each of them is a different sentence
 * ([com.jenarvaezg.coindex.ui.installOutcomeMessage]): a permission that has to be granted, a
 * device that cannot grant it, a phone with no package installer, and a download that died. The
 * one success says nothing, because what happens next is the system's own dialog.
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
 * Looking for a newer APK, and installing it (ADR 0011).
 *
 * The two gestures live together because they share the one thing that had to come out of the
 * ViewModel: **when it is allowed to ask**. The check runs on a launch, on every return to the
 * front and on a timer, so the interval is what keeps it from asking GitHub every time the
 * collector glances at another app — and it was arithmetic on `System.currentTimeMillis()` read in
 * place, which is a rule no test could reach.
 *
 * Failures are never errors here: [UpdateChecker] swallows its own into
 * [UpdateStatus.Unavailable], because an update check that cannot reach GitHub must never interrupt
 * looking at the collection.
 */
class UpdateFlow(
    private val checker: UpdateChecker,
    private val installer: UpdateInstaller,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private var lastCheckMillis: Long? = null

    /**
     * One question at a time, like [com.jenarvaezg.coindex.data.CallBudgetGate]'s.
     *
     * The check fires on a launch, on every return to the front and on a timer, so two of them can
     * be in the air at once — and both would read a stamp neither had written yet. Serialized, the
     * second one finds the first one's stamp and answers «no toca».
     */
    private val asking = Mutex()

    /**
     * What GitHub says, or null when it is not time to ask yet.
     *
     * Null and not [UpdateStatus.UpToDate]: «no lo he preguntado» has to leave the banner exactly
     * as it was, and an answer would replace an available update with a claim nobody checked.
     */
    suspend fun check(force: Boolean = false): UpdateStatus? = asking.withLock {
        val now = nowMillis()
        if (!force && !shouldCheckForUpdate(lastCheckMillis, now)) return null
        // Stamped before the request rather than after it: two returns to the front a second apart
        // must not both reach GitHub because the first one had not answered yet.
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
