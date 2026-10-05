package com.jenarvaezg.coindex

import android.content.Context
import com.jenarvaezg.coindex.data.ApiCallLedger
import com.jenarvaezg.coindex.data.CallBudgetGate
import com.jenarvaezg.coindex.data.CoindexRepository
import com.jenarvaezg.coindex.data.CollectionSync
import com.jenarvaezg.coindex.data.CREDENTIAL_PREFERENCES
import com.jenarvaezg.coindex.data.DEFAULT_MONTHLY_BUDGET
import com.jenarvaezg.coindex.data.InventoryRefresh
import com.jenarvaezg.coindex.data.keystoreSecret
import com.jenarvaezg.coindex.data.NamedValues
import com.jenarvaezg.coindex.data.NOTEBOOK_PREFERENCES
import com.jenarvaezg.coindex.data.REJECTION_WALL_PREFERENCES
import com.jenarvaezg.coindex.data.RejectionWall
import com.jenarvaezg.coindex.data.SharedPreferenceValues
import com.jenarvaezg.coindex.data.StoredCredentials
import com.jenarvaezg.coindex.data.StoredNotebook
import com.jenarvaezg.coindex.data.StoredRejectionWall
import com.jenarvaezg.coindex.data.StoredSyncLog
import com.jenarvaezg.coindex.data.SYNC_LOG_PREFERENCES
import com.jenarvaezg.coindex.data.SyncService
import com.jenarvaezg.coindex.data.TypeRefresh
import com.jenarvaezg.coindex.data.db.CoindexDatabase
import com.jenarvaezg.coindex.data.db.DATABASE_EXPORT_DIR
import com.jenarvaezg.coindex.data.db.DatabaseExport
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
import com.jenarvaezg.coindex.data.numista.NumistaClient
import com.jenarvaezg.coindex.data.seed.AssetCuratedFiles
import com.jenarvaezg.coindex.data.seed.TYPE_SEED_PREFERENCES
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
import java.io.File

/**
 * Manual dependency wiring. The app is small and single-user; a DI framework would add more
 * indirection than it removes.
 *
 * The curated files are parsed and validated by [Curation.load] on first access, and a failure
 * propagates: shipping a broken catalog must be loud.
 *
 * Most of what is built here is private, the database above all: a screen gets the thing that
 * answers its question, such as [calls] for the month's budget, never the table behind it (#220).
 */
class AppContainer(context: Context) {
    private val applicationContext = context.applicationContext

    private val database: CoindexDatabase by lazy { CoindexDatabase.open(applicationContext) }

    /**
     * One preferences file per subject (#546). The stores are plain classes over [NamedValues], so
     * which file each lives in is decided here, with every other name on the device.
     */
    private fun valuesIn(name: String): NamedValues =
        SharedPreferenceValues(applicationContext, name)

    /**
     * The wall the valuation pass stopped against, remembered across launches (#579). Held here
     * because the pass raises it and the credential store takes it down when the collector saves a
     * key, the only way out of the credentials wall, which has no clock.
     */
    private val rejectionWall: RejectionWall by lazy {
        StoredRejectionWall(valuesIn(REJECTION_WALL_PREFERENCES))
    }

    /** The collector's own Numista credentials, with the keystore key they are sealed with. */
    val credentials: StoredCredentials by lazy {
        StoredCredentials(valuesIn(CREDENTIAL_PREFERENCES), rejectionWall, ::keystoreSecret)
    }

    private val syncLog: StoredSyncLog by lazy { StoredSyncLog(valuesIn(SYNC_LOG_PREFERENCES)) }

    /** What the collector was looking through last time (ADR 0021 §1), on both hierarchies. */
    val shelves: ShelfStore by lazy { StoredShelves(valuesIn(SHELF_PREFERENCES)) }

    /** How the collector printed their notebook last time: the switches of #228. */
    val notebook: StoredNotebook by lazy { StoredNotebook(valuesIn(NOTEBOOK_PREFERENCES)) }

    /**
     * A checkpointed copy of the database for the share sheet (#548), built here because this is
     * the only place the database is reachable (#220).
     */
    val dataExport: DatabaseExport by lazy {
        DatabaseExport(
            source = applicationContext.getDatabasePath(CoindexDatabase.DATABASE_NAME),
            directory = File(applicationContext.cacheDir, DATABASE_EXPORT_DIR),
            versionName = ::installedVersionName,
            checkpoint = database::checkpoint,
        )
    }

    /** What has been spent of this month's API allowance, and the only reader of `api_call_log`. */
    val calls: ApiCallLedger by lazy { ApiCallLedger(database.apiCalls()) }

    val repository: CoindexRepository by lazy {
        CoindexRepository(
            collectedItemDao = database.collectedItems(),
            typeMetaDao = database.typeMeta(),
            ownGroupingDao = database.ownGroupings(),
            priceDao = database.prices(),
            wishDao = database.wishes(),
            // One loader for the three kinds of curated file, validated as a whole (#545).
            curation = Curation.load(AssetCuratedFiles(applicationContext.assets)),
        )
    }

    private val typeCacheSeed: TypeCacheSeed by lazy {
        TypeCacheSeed.fromAssets(
            assets = applicationContext.assets,
            typeMeta = database.typeMeta(),
            values = valuesIn(TYPE_SEED_PREFERENCES),
            installedVersionCode = installedVersionCode(),
        )
    }

    private val typeThumbnailBackfill: TypeThumbnailBackfill by lazy {
        TypeThumbnailBackfill(database.typeMeta())
    }

    private val fichaBackfill: FichaBackfill by lazy { FichaBackfill(database.typeMeta()) }

    /**
     * Brings the ficha cache up to what the APK ships, before the collection is read: the seed
     * adds what is missing and, once per version, overwrites (#67, #606); then the backfills fill
     * columns added after a row was cached (v3 thumbnails, v6 fields, #221), since a cached type is
     * never fetched again. They run after the seed, so its fresh rows need nothing.
     */
    suspend fun warmUpFichaCache() {
        typeCacheSeed.topUp(repository.curation.curatedTypeIds())
        typeThumbnailBackfill.run()
        fichaBackfill.run()
    }

    private val syncService: SyncService by lazy {
        SyncService(database.collectedItems(), database.typeMeta(), calls)
    }

    /** One sync, stamped and written down (#220). */
    val collectionSync: CollectionSync by lazy { CollectionSync(syncService, syncLog, rejectionWall) }

    /** The inventory brought up to date because a day passed, not because anybody pressed (#605). */
    val inventoryRefresh: InventoryRefresh by lazy { InventoryRefresh(collectionSync, rejectionWall) }

    /** One type's ficha, asked again on purpose (#185, ADR 0025). */
    val typeRefresh: TypeRefresh by lazy { TypeRefresh(database.typeMeta()) }

    /**
     * The photographs Numista answers `404` for, remembered across launches (#191). Held here
     * because the loader's interceptor writes it, from any OkHttp thread, and the prefetch reads it.
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
     * When a pass of the prefetch is worth starting (#191). Held here rather than in the ViewModel so
     * what it remembers, status included, survives a new ViewModel in the same process instead of
     * re-checking every cached photo.
     */
    val photos: PhotoPrefetchLoop by lazy {
        PhotoPrefetchLoop(photoPrefetch, prefetchConditions::current)
    }

    private val httpClient: HttpClient by lazy { HttpClient(OkHttp) }

    private val budgetGate: CallBudgetGate by lazy {
        CallBudgetGate(calls, monthlyBudget = { DEFAULT_MONTHLY_BUDGET })
    }

    /**
     * The silver spot, from two keyless calls outside the budget of ADR 0003: neither host is
     * `api.numista.com`, as with the CDN photographs of ADR 0024.
     */
    private val spot: SpotStore by lazy {
        SpotStore(database.prices(), HttpSpotReader(httpClient))
    }

    private val valuationPass: ValuationPass by lazy {
        NumistaValuationPass(database.prices(), ::numistaClient, spot, rejectionWall)
    }

    /**
     * When the catalog prices are asked for (ADR 0028). Held here like [photos], so a new ViewModel
     * (a rotation) doesn't query the database again.
     */
    val valuation: ValuationLoop by lazy { ValuationLoop(valuationPass, { isSyncing() }) }

    /**
     * Whether a sync is in flight. Read from the syncs themselves so the pass can ask right before its
     * first call: both spend the same monthly allowance. The automatic refresh counts from the moment
     * it is claimed, ahead of the pass (#605), so the pass holds instead of being cancelled mid-call.
     */
    private fun isSyncing(): Boolean = collectionSync.inFlight || inventoryRefresh.inFlight

    /**
     * Self-update against the public GitHub releases, outside the Numista budget gate.
     */
    private val updateChecker: UpdateChecker by lazy {
        UpdateChecker(httpClient, currentVersionCode = installedVersionCode())
    }

    private val updateInstaller: UpdateInstaller by lazy {
        SystemUpdateInstaller(applicationContext, httpClient)
    }

    /**
     * Looking for a newer APK and installing it (ADR 0011). Held here like [photos]: it remembers
     * when it last asked, and a rotation is no reason to ask GitHub again.
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
