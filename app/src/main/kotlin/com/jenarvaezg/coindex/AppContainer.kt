package com.jenarvaezg.coindex

import android.content.Context
import com.jenarvaezg.coindex.data.CallBudgetGate
import com.jenarvaezg.coindex.data.CoindexRepository
import com.jenarvaezg.coindex.data.CredentialStore
import com.jenarvaezg.coindex.data.ShelfStore
import com.jenarvaezg.coindex.data.SyncLog
import com.jenarvaezg.coindex.data.SyncService
import com.jenarvaezg.coindex.data.db.CoindexDatabase
import com.jenarvaezg.coindex.domain.validateShortNamesAcross
import com.jenarvaezg.coindex.data.numista.NumistaClient
import com.jenarvaezg.coindex.data.seed.CatalogAssets
import com.jenarvaezg.coindex.data.seed.GroupingAssets
import com.jenarvaezg.coindex.data.seed.ProgrammeAssets
import com.jenarvaezg.coindex.data.seed.TypeCacheSeed
import com.jenarvaezg.coindex.data.seed.TypeThumbnailBackfill
import com.jenarvaezg.coindex.data.update.UpdateChecker
import com.jenarvaezg.coindex.data.update.UpdateInstaller
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

/**
 * Manual dependency wiring. The app is small and single-user; a DI framework would add more
 * indirection than it removes.
 *
 * The curated catalogs are parsed and validated here, on the first access, and a failure is
 * allowed to propagate: shipping a broken catalog must be loud.
 */
class AppContainer(context: Context) {
    private val applicationContext = context.applicationContext

    val database: CoindexDatabase by lazy { CoindexDatabase.open(applicationContext) }

    val credentials: CredentialStore by lazy { CredentialStore(applicationContext) }

    val syncLog: SyncLog by lazy { SyncLog(applicationContext) }

    /** What the collector was looking through last time (ADR 0021 §1), on both hierarchies. */
    val shelves: ShelfStore by lazy { ShelfStore(applicationContext) }

    val repository: CoindexRepository by lazy {
        val catalogs = CatalogAssets.load(applicationContext.assets)
        val groupings = GroupingAssets.load(applicationContext.assets)
        // The index draws both species side by side and indistinguishably (#12), so a card name
        // repeated across them is only visible here, where both are loaded (#22).
        validateShortNamesAcross(catalogs, groupings)
        CoindexRepository(
            database = database,
            catalogs = catalogs,
            groupings = groupings,
            // A programme reaches no card, so it takes no part in the name check above (ADR 0022).
            programmes = ProgrammeAssets.load(applicationContext.assets),
        )
    }

    val typeCacheSeed: TypeCacheSeed by lazy {
        TypeCacheSeed.fromAssets(applicationContext.assets, database.typeMeta())
    }

    val typeThumbnailBackfill: TypeThumbnailBackfill by lazy {
        TypeThumbnailBackfill(database.typeMeta())
    }

    val syncService: SyncService by lazy {
        SyncService(database.collectedItems(), database.typeMeta(), database.apiCalls())
    }

    private val httpClient: HttpClient by lazy { HttpClient(OkHttp) }

    private val budgetGate: CallBudgetGate by lazy {
        CallBudgetGate(database.apiCalls(), monthlyBudget = { credentials.monthlyBudget })
    }

    /**
     * Self-update against the public GitHub releases. These requests go to GitHub, never to
     * Numista, so they are outside the API budget gate on purpose.
     */
    val updateChecker: UpdateChecker by lazy {
        UpdateChecker(httpClient, currentVersionCode = installedVersionCode())
    }

    val updateInstaller: UpdateInstaller by lazy { UpdateInstaller(applicationContext, httpClient) }

    /** Version name of the running APK, shown in the masthead. */
    fun installedVersionName(): String = runCatching {
        applicationContext.packageManager
            .getPackageInfo(applicationContext.packageName, 0)
            .versionName
            .orEmpty()
    }.getOrDefault("")

    private fun installedVersionCode(): Int = runCatching {
        applicationContext.packageManager
            .getPackageInfo(applicationContext.packageName, 0)
            .longVersionCode
            .toInt()
    }.getOrDefault(0)

    /** A client bound to the stored API key, or null while onboarding is pending. */
    fun numistaClient(): NumistaClient? {
        val stored = credentials.credentials() ?: return null
        return NumistaClient(httpClient, stored.apiKey, budgetGate)
    }
}
