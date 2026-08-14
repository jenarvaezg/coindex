package com.jenarvaezg.coindex

import android.content.Context
import com.jenarvaezg.coindex.data.ApiCallLedger
import com.jenarvaezg.coindex.data.CallBudgetGate
import com.jenarvaezg.coindex.data.CoindexRepository
import com.jenarvaezg.coindex.data.CollectionSync
import com.jenarvaezg.coindex.data.CredentialStore
import com.jenarvaezg.coindex.data.DEFAULT_MONTHLY_BUDGET
import com.jenarvaezg.coindex.data.KeystoreCredentialStore
import com.jenarvaezg.coindex.data.NotebookStore
import com.jenarvaezg.coindex.data.SharedPreferenceValues
import com.jenarvaezg.coindex.data.StoredNotebook
import com.jenarvaezg.coindex.data.StoredSyncLog
import com.jenarvaezg.coindex.data.SyncLog
import com.jenarvaezg.coindex.data.SyncService
import com.jenarvaezg.coindex.data.TypeRefresh
import com.jenarvaezg.coindex.data.db.CoindexDatabase
import com.jenarvaezg.coindex.data.ficha.FichaBackfill
import com.jenarvaezg.coindex.data.photos.CoilPhotoPrefetch
import com.jenarvaezg.coindex.data.photos.DevicePrefetchConditions
import com.jenarvaezg.coindex.data.photos.GonePhotographs
import com.jenarvaezg.coindex.data.photos.PhotoPrefetch
import com.jenarvaezg.coindex.data.photos.PhotoPrefetchLoop
import com.jenarvaezg.coindex.data.photos.StoredGonePhotographs
import com.jenarvaezg.coindex.data.prices.HttpSpotReader
import com.jenarvaezg.coindex.data.prices.NumistaValuationPass
import com.jenarvaezg.coindex.data.prices.SpotStore
import com.jenarvaezg.coindex.data.prices.ValuationLoop
import com.jenarvaezg.coindex.data.prices.ValuationPass
import com.jenarvaezg.coindex.domain.Curation
import com.jenarvaezg.coindex.domain.validateShortNamesAcross
import com.jenarvaezg.coindex.data.numista.NumistaClient
import com.jenarvaezg.coindex.data.seed.CatalogAssets
import com.jenarvaezg.coindex.data.seed.GroupingAssets
import com.jenarvaezg.coindex.data.seed.ProgrammeAssets
import com.jenarvaezg.coindex.data.seed.TypeCacheSeed
import com.jenarvaezg.coindex.data.seed.TypeThumbnailBackfill
import com.jenarvaezg.coindex.data.update.SystemUpdateInstaller
import com.jenarvaezg.coindex.data.update.UpdateChecker
import com.jenarvaezg.coindex.data.update.UpdateFlow
import com.jenarvaezg.coindex.data.update.UpdateInstaller
import com.jenarvaezg.coindex.ui.shelf.SHELF_PREFERENCES
import com.jenarvaezg.coindex.ui.shelf.ShelfStore
import com.jenarvaezg.coindex.ui.shelf.StoredShelves
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

/**
 * Manual dependency wiring. The app is small and single-user; a DI framework would add more
 * indirection than it removes.
 *
 * The curated catalogs are parsed and validated here, on the first access, and a failure is
 * allowed to propagate: shipping a broken catalog must be loud.
 *
 * Most of what is built here is **private**: the database above all, whose DAOs used to be reachable
 * from the UI by walking `container.database.apiCalls()` (#220). What a screen is given is the thing
 * that answers its question — [calls] for the month's budget — and never the table behind it.
 */
class AppContainer(context: Context) {
    private val applicationContext = context.applicationContext

    private val database: CoindexDatabase by lazy { CoindexDatabase.open(applicationContext) }

    val credentials: CredentialStore by lazy { KeystoreCredentialStore(applicationContext) }

    private val syncLog: SyncLog by lazy { StoredSyncLog(applicationContext) }

    /** What the collector was looking through last time (ADR 0021 §1), on both hierarchies. */
    val shelves: ShelfStore by lazy {
        StoredShelves(SharedPreferenceValues(applicationContext, SHELF_PREFERENCES))
    }

    /** How the collector printed their notebook last time: the five switches of #228. */
    val notebook: NotebookStore by lazy { StoredNotebook(applicationContext) }

    /** What is left of this month's API allowance, and the only reader of `api_call_log`. */
    val calls: ApiCallLedger by lazy { ApiCallLedger(database.apiCalls()) }

    val repository: CoindexRepository by lazy {
        val catalogs = CatalogAssets.load(applicationContext.assets)
        val groupings = GroupingAssets.load(applicationContext.assets)
        // The index draws both species side by side and indistinguishably (#12), so a card name
        // repeated across them is only visible here, where both are loaded (#22).
        validateShortNamesAcross(catalogs, groupings)
        CoindexRepository(
            collectedItemDao = database.collectedItems(),
            typeMetaDao = database.typeMeta(),
            ownGroupingDao = database.ownGroupings(),
            priceDao = database.prices(),
            wishDao = database.wishes(),
            curation = Curation(
                catalogs = catalogs,
                groupings = groupings,
                // A programme reaches no card, so it takes no part in the name check above
                // (ADR 0022).
                programmes = ProgrammeAssets.load(applicationContext.assets),
            ),
        )
    }

    private val typeCacheSeed: TypeCacheSeed by lazy {
        TypeCacheSeed.fromAssets(applicationContext.assets, database.typeMeta())
    }

    private val typeThumbnailBackfill: TypeThumbnailBackfill by lazy {
        TypeThumbnailBackfill(database.typeMeta())
    }

    private val fichaBackfill: FichaBackfill by lazy { FichaBackfill(database.typeMeta()) }

    /**
     * Brings the ficha cache up to what the APK ships, before the collection is read.
     *
     * Three steps and one moment. The seed used to be a **first-install** gift: every catalog curated
     * afterwards shipped its fichas in the asset and none of them reached a phone that already had
     * the app (#67). And a cache seeded before version 3 has no thumbnails, while a cached type is
     * never fetched again — without the backfill the plate would keep asking for the heavy originals
     * for ever on exactly those phones.
     *
     * The third step is the same move over the five columns of version 6 (#221): before it runs, a
     * ficha cached by an older APK has no issuer name, no metal, no diameter and no QR, because
     * those stopped being parsed on every read. Both backfills come **after** the seed, so the rows
     * it has just written are already right and neither pass has anything to do on them.
     */
    suspend fun warmUpFichaCache() {
        typeCacheSeed.topUp(repository.curation.curatedTypeIds())
        typeThumbnailBackfill.run()
        fichaBackfill.run()
    }

    private val syncService: SyncService by lazy {
        SyncService(database.collectedItems(), database.typeMeta(), calls)
    }

    /** One explicit sync, stamped and written down (#220). */
    val collectionSync: CollectionSync by lazy { CollectionSync(syncService, syncLog) }

    /** One type's ficha, asked again on purpose (#185, ADR 0025). */
    val typeRefresh: TypeRefresh by lazy { TypeRefresh(database.typeMeta()) }

    /**
     * The photographs Numista answers `404` for, remembered across launches (#191).
     *
     * Held here and not inside the image loader because both ends need it: the loader's interceptor
     * writes to it from whatever thread OkHttp is on, and the prefetch reads it before deciding
     * what to ask for.
     */
    val gonePhotographs: GonePhotographs by lazy { StoredGonePhotographs(applicationContext) }

    /** Catalog photographs brought in before anybody asks for them (#191). */
    private val photoPrefetch: PhotoPrefetch by lazy {
        CoilPhotoPrefetch(applicationContext, gonePhotographs)
    }

    /** Whether this is a good moment to spend the collector's data on them. */
    private val prefetchConditions: DevicePrefetchConditions by lazy {
        DevicePrefetchConditions(applicationContext)
    }

    /**
     * When a pass of the prefetch is worth starting (#191).
     *
     * Held here rather than inside the ViewModel because what it remembers has to outlive the screen:
     * a collector who leaves the app and comes back gets a new ViewModel over the same process, and
     * reopening sixteen hundred cache snapshots to find out that nothing has changed is precisely the
     * cold start this exists to remove. The status travels with it, so the settings line is still
     * true on that second launch.
     */
    val photos: PhotoPrefetchLoop by lazy {
        PhotoPrefetchLoop(photoPrefetch, prefetchConditions::current)
    }

    private val httpClient: HttpClient by lazy { HttpClient(OkHttp) }

    private val budgetGate: CallBudgetGate by lazy {
        CallBudgetGate(calls, monthlyBudget = { DEFAULT_MONTHLY_BUDGET })
    }

    /**
     * The silver spot, from two keyless calls that are **not** counted against the budget of ADR 0003:
     * neither host is `api.numista.com`, the same distinction ADR 0024 draws for CDN photographs.
     */
    private val spot: SpotStore by lazy {
        SpotStore(database.prices(), HttpSpotReader(httpClient))
    }

    private val valuationPass: ValuationPass by lazy {
        NumistaValuationPass(database.prices(), ::numistaClient, spot)
    }

    /**
     * When the catalog prices are asked for (ADR 0028).
     *
     * Held here for the same reason [photos] is: what it remembers has to outlive the screen. With every
     * price already on the phone, the second launch of a month must cost **zero** calls, and a loop
     * rebuilt with each ViewModel would ask the database again on every rotation.
     */
    val valuation: ValuationLoop by lazy { ValuationLoop(valuationPass, { isSyncing() }) }

    /**
     * Whether a sync is in flight, asked of the one thing that knows.
     *
     * Read through the sync itself rather than handed in by the ViewModel, because the pass has to be
     * able to ask **at the moment it starts its first call**: the two spend the same monthly allowance,
     * and three seconds of a cold start is long enough for the collector to have pressed «Sincronizar».
     */
    private fun isSyncing(): Boolean = collectionSync.inFlight

    /**
     * Self-update against the public GitHub releases. These requests go to GitHub, never to
     * Numista, so they are outside the API budget gate on purpose.
     */
    private val updateChecker: UpdateChecker by lazy {
        UpdateChecker(httpClient, currentVersionCode = installedVersionCode())
    }

    private val updateInstaller: UpdateInstaller by lazy {
        SystemUpdateInstaller(applicationContext, httpClient)
    }

    /**
     * Looking for a newer APK and installing it (ADR 0011).
     *
     * Held here for the same reason as [photos]: what it remembers is *when it last asked*, and a
     * rotation is not a reason to ask GitHub again.
     */
    val updates: UpdateFlow by lazy { UpdateFlow(updateChecker, updateInstaller) }

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
