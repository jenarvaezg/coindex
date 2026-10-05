package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.ApiCallLedger
import com.jenarvaezg.coindex.data.CoindexRepository
import com.jenarvaezg.coindex.data.CollectionSync
import com.jenarvaezg.coindex.data.credentialsOnJvm
import com.jenarvaezg.coindex.data.Credentials
import com.jenarvaezg.coindex.data.FakeApiCallDao
import com.jenarvaezg.coindex.data.FakeCollectedItemDao
import com.jenarvaezg.coindex.data.FakeNamedValues
import com.jenarvaezg.coindex.data.FakeOwnGroupingDao
import com.jenarvaezg.coindex.data.FakePhotoPrefetch
import com.jenarvaezg.coindex.data.FakePriceDao
import com.jenarvaezg.coindex.data.FakeShelfStore
import com.jenarvaezg.coindex.data.FakeTypeMetaDao
import com.jenarvaezg.coindex.data.FakeValuationPass
import com.jenarvaezg.coindex.data.FakeWishDao
import com.jenarvaezg.coindex.data.StoredNotebook
import com.jenarvaezg.coindex.data.StoredSyncLog
import com.jenarvaezg.coindex.data.SyncRecord
import com.jenarvaezg.coindex.data.InventoryRefresh
import com.jenarvaezg.coindex.data.RejectionCause
import com.jenarvaezg.coindex.data.StoredRejectionWall
import com.jenarvaezg.coindex.data.SyncService
import com.jenarvaezg.coindex.data.TypeRefresh
import com.jenarvaezg.coindex.data.db.ApiCallEntity
import com.jenarvaezg.coindex.data.db.CollectedItemEntity
import com.jenarvaezg.coindex.data.db.DatabaseExport
import com.jenarvaezg.coindex.data.db.IssuePriceEntity
import com.jenarvaezg.coindex.data.db.TypeMetaEntity
import com.jenarvaezg.coindex.data.numista.CallBudget
import com.jenarvaezg.coindex.data.numista.NumistaClient
import com.jenarvaezg.coindex.data.photos.PhotoCacheStatus
import com.jenarvaezg.coindex.data.photos.PhotoPrefetchLoop
import com.jenarvaezg.coindex.data.photos.PrefetchConditions
import com.jenarvaezg.coindex.data.prices.OwnedIssue
import com.jenarvaezg.coindex.data.prices.PlateHole
import com.jenarvaezg.coindex.data.prices.ValuationLoop
import com.jenarvaezg.coindex.data.update.FakeUpdateInstaller
import com.jenarvaezg.coindex.data.update.UpdateChecker
import com.jenarvaezg.coindex.data.update.UpdateFlow
import com.jenarvaezg.coindex.data.update.UpdateStatus
import com.jenarvaezg.coindex.domain.CollectionCatalog
import com.jenarvaezg.coindex.domain.CollectionCatalogMember
import com.jenarvaezg.coindex.domain.Curation
import com.jenarvaezg.coindex.domain.Metal
import com.jenarvaezg.coindex.domain.SeriesStatus
import com.jenarvaezg.coindex.domain.wishKey
import com.jenarvaezg.coindex.ui.print.NotebookOptions
import com.jenarvaezg.coindex.ui.print.NotebookSubject
import com.jenarvaezg.coindex.ui.print.forSheetExport
import com.jenarvaezg.coindex.ui.shelf.IndexShelf
import com.jenarvaezg.coindex.ui.shelf.IndexSort
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/** A type nothing in `data/` names, so the memoized mappers cannot answer for it. */
private const val LOOSE_TYPE = 990_220

private const val THUMBNAIL = "https://en.numista.com/catalogue/photos/anverso-180.jpg"

/** Long enough after the first of the month that a stamped call counts against it. */
private const val NOW = 1_786_170_660_000L

/** A stored last sync, for the tests that check it is read and kept. */
private val RECORD = SyncRecord(
    atMillis = NOW,
    collectionItems = 58,
    typesFetched = 0,
    callsSpent = 2,
)

/**
 * A two-year date run of the type the collection carries, so a casilla can be marked (ADR 0029).
 * Two members because a mark is keyed on type and year, and a filled mark must leave its sibling's
 * hole alone.
 */
private val WISHED_CATALOG = CollectionCatalog(
    schemaVersion = 2,
    id = "venezuela-fuertes-test",
    name = "Fuertes de Venezuela",
    shortName = "Fuertes",
    family = "Fuertes de Venezuela",
    issuerCode = "venezuela",
    weightMillioz = 804,
    metal = Metal.Silver,
    seriesStatus = SeriesStatus.Closed,
    source = "https://en.numista.com/catalogue/pieces10340.html",
    updatedAt = "2026-08-14",
    members = listOf(1_929, 1_930).map { year ->
        CollectionCatalogMember(
            id = "fuertes-$year",
            label = year.toString(),
            year = year,
            numistaTypeId = LOOSE_TYPE,
        )
    },
)

private const val ONE_ITEM = """
{
  "item_count": 1,
  "items": [
    {"id": 1, "quantity": 1, "type": {"id": $LOOSE_TYPE, "title": "5 Bolívares"},
     "issue": {"year": 1929, "gregorian_year": 1929}}
  ]
}
"""

/**
 * The state the screens read and what each gesture does to it (#220). Every collaborator is a
 * stand-in and every clock is held still.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CoindexViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    private val items = FakeCollectedItemDao()
    private val types = FakeTypeMetaDao()
    private val ownGroupings = FakeOwnGroupingDao()
    private val apiCalls = FakeApiCallDao()
    /** The real store over a fake file, with a key the JVM can make (#546). */
    private val credentials = credentialsOnJvm().apply { save(apiKey = "key", userId = 2104) }
    private val shelves = FakeShelfStore()
    private val notebook = StoredNotebook(FakeNamedValues())
    private val syncLog = StoredSyncLog(FakeNamedValues())
    private val prefetch = FakePhotoPrefetch(PhotoCacheStatus(wanted = 2, missing = 1))
    private val prices = FakePriceDao()
    private val wishes = FakeWishDao()
    private val valuationPass = FakeValuationPass()

    /** Held outside the ViewModel, as `AppContainer` holds it. */
    private val photos = PhotoPrefetchLoop(
        prefetch,
        { syncing -> PrefetchConditions(unmeteredNetwork = true, syncing = syncing) },
    )

    /** Also held outside, and told whether a sync is in flight the same way. */
    private val valuation = ValuationLoop(valuationPass, { syncing })
    private var syncing = false
    private val installer = FakeUpdateInstaller()
    private var warmedUp = 0

    /** What the mock engine was asked for, so a gesture guarded twice can be counted. */
    private val requested = mutableListOf<String>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun numistaClient(): NumistaClient {
        val engine = MockEngine { request ->
            requested += request.url.encodedPath
            val path = request.url.encodedPath
            val body = when {
                path.contains("oauth_token") -> """{"access_token":"t","expires_in":600}"""
                path.contains("collected_items") -> ONE_ITEM
                else -> """{"id": $LOOSE_TYPE, "title": "5 Bolívares", "weight": 25.0}"""
            }
            respond(body, HttpStatusCode.OK, headersOf("Content-Type", "application/json"))
        }
        val budget = object : CallBudget {
            override suspend fun reserve(endpoint: String) {
                apiCalls.record(ApiCallEntity(endpoint = endpoint, calledAt = NOW))
            }
        }
        return NumistaClient(HttpClient(engine), "key", budget, "https://api.example/v3") { NOW }
    }

    /**
     * An automatic inventory refresh that is never due (#605). With an empty sync log the ViewModel
     * would refresh on construction and skew the call counts; a raised wall stands it down.
     */
    private fun neverDue(sync: CollectionSync): InventoryRefresh {
        val wall = StoredRejectionWall(FakeNamedValues()) { NOW }
        wall.raise(RejectionCause.Credentials)
        return InventoryRefresh(sync, wall) { NOW }
    }

    /** GitHub, with a release newer than the installed one. */
    private fun updateChecker(): UpdateChecker = UpdateChecker(
        HttpClient(
            MockEngine { request ->
                val body = if (request.url.encodedPath.endsWith("releases/latest")) {
                    """
                    {"tag_name": "v0.16.0", "assets": [
                      {"name": "update.json",
                       "browser_download_url": "https://api.example/download/update.json",
                       "size": 1},
                      {"name": "coindex-24.apk",
                       "browser_download_url": "https://api.example/download/coindex-24.apk",
                       "size": 29}
                    ]}
                    """
                } else {
                    """
                    {"versionCode": 24, "versionName": "0.16.0", "apkAsset": "coindex-24.apk"}
                    """
                }
                respond(body, HttpStatusCode.OK, headersOf("Content-Type", "application/json"))
            },
        ),
        currentVersionCode = 23,
        repo = "jenarvaezg/coindex",
        apiBaseUrl = "https://api.example",
    )

    /** The clock the ViewModel reads itself, movable so a stamp can be seen to move (#220). */
    private var clock = NOW

    private fun viewModel(
        client: () -> NumistaClient? = { numistaClient() },
        warmUp: suspend () -> Unit = { warmedUp += 1 },
        catalogs: List<CollectionCatalog> = emptyList(),
        dataExport: DatabaseExport = dataExport(),
        automaticRefresh: Boolean = false,
    ): CoindexViewModel {
        val repository = CoindexRepository(
            collectedItemDao = items,
            typeMetaDao = types,
            ownGroupingDao = ownGroupings,
            priceDao = prices,
            wishDao = wishes,
            curation = Curation(catalogs = catalogs),
        )
        val ledger = ApiCallLedger(apiCalls) { NOW }
        val collectionSync = CollectionSync(
            syncService = SyncService(items, types, ledger) { NOW },
            syncLog = syncLog,
            wall = StoredRejectionWall(FakeNamedValues()) { NOW },
        ) { NOW }
        return CoindexViewModel(
            repository = { repository },
            credentials = credentials,
            shelves = shelves,
            notebook = notebook,
            collectionSync = collectionSync,
            inventoryRefresh = if (automaticRefresh) {
                InventoryRefresh(collectionSync, StoredRejectionWall(FakeNamedValues()) { NOW }, 0L) { NOW }
            } else {
                neverDue(collectionSync)
            },
            typeRefresh = TypeRefresh(types) { NOW },
            updates = UpdateFlow(updateChecker(), installer) { NOW },
            photos = photos,
            valuation = valuation,
            client = client,
            warmUpFichaCache = warmUp,
            dataExport = dataExport,
            installedVersionName = "0.15.0",
            now = { clock },
        )
    }

    /** A dump over real files in a temporary directory (#548). */
    private fun dataExport(
        base: File = File(exportRoot, "coindex.db").apply { writeText("la colección") },
    ): DatabaseExport = DatabaseExport(
        source = base,
        directory = File(exportRoot, "salida"),
        versionName = { "0.15.0" },
        checkpoint = {},
    )

    private val exportRoot: File by lazy {
        Files.createTempDirectory("coindex-viewmodel-export").toFile()
    }

    /**
     * Runs [body] on a fresh ViewModel and cancels its scope at the end: the update poll is an
     * endless `while (true)`, so an uncancelled test would never finish.
     *
     * @param given what the stores hold before the ViewModel first reads them.
     */
    private fun onViewModel(
        client: () -> NumistaClient? = { numistaClient() },
        warmUp: suspend () -> Unit = { warmedUp += 1 },
        // Empty unless a test marks a casilla: wishes resolve against a catalog (ADR 0029 §2).
        catalogs: List<CollectionCatalog> = emptyList(),
        dataExport: DatabaseExport = dataExport(),
        given: () -> Unit = {},
        /** True only in the tests about the automatic refresh. */
        automaticRefresh: Boolean = false,
        body: suspend TestScope.(CoindexViewModel) -> Unit,
    ) = runTest(dispatcher) {
        given()
        val viewModel = viewModel(client, warmUp, catalogs, dataExport, automaticRefresh)
        try {
            body(viewModel)
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }


    /**
     * One piece of [ficha]'s type. Its issue id lives in `raw`, not in a column, as a real sync
     * stores it (ADR 0028).
     */
    private fun collected(typeId: Int = LOOSE_TYPE, issueId: Int = 8_508) = CollectedItemEntity(
        id = 1,
        typeId = typeId,
        quantity = 1,
        title = "5 Bolívares",
        issuerCode = "venezuela",
        issueYear = 1929,
        gregorianYear = 1929,
        grade = "unc",
        price = null,
        forSwap = false,
        collectionName = null,
        raw = """{"issue":{"id":$issueId}}""",
        syncedAt = NOW,
    )

    private fun ficha(typeId: Int = LOOSE_TYPE) = TypeMetaEntity(
        typeId = typeId,
        title = "5 Bolívares",
        family = "Bolívar de plata",
        issuerCode = "venezuela",
        minYear = 1929,
        maxYear = 1929,
        weightGrams = 25.0,
        obverseUrl = null,
        reverseUrl = null,
        raw = "{}",
        fetchedAt = NOW,
        obverseThumbnailUrl = THUMBNAIL,
    )

    @Test
    fun `the first state is what the stores were already holding`() = onViewModel(
        given = {
            syncLog.last = RECORD
            shelves.index = IndexShelf(sort = IndexSort.Alphabetical)
            notebook.options = NotebookOptions(photographs = false)
        },
    ) { viewModel ->
        val state = viewModel.state.value

        assertEquals("0.15.0", state.versionName)
        assertEquals(RECORD, state.lastSync)
        assertEquals(IndexSort.Alphabetical, state.indexShelf.sort)
        assertFalse(state.notebookOptions.photographs)
    }

    @Test
    fun `a launch tops the fichas up before the collection is read`() =
        onViewModel(
            given = { apiCalls.calls += ApiCallEntity(endpoint = "/types/1", calledAt = NOW) },
        ) { viewModel ->
            runCurrent()

            assertEquals(1, warmedUp)
            assertFalse(viewModel.state.value.loading)
            assertTrue(viewModel.state.value.onboarded)
        }

    @Test
    fun `no stored credentials is the onboarding screen`() = onViewModel(
        client = { null },
        given = { credentials.clear() },
    ) { viewModel ->
        runCurrent()

        assertFalse(viewModel.state.value.onboarded)
    }

    @Test
    fun `a collection that cannot be read stops the spinner and says why`() = onViewModel(
        warmUp = { error("la caché de fichas está corrupta") },
    ) { viewModel ->
        runCurrent()

        assertFalse(viewModel.state.value.loading)
        assertEquals("la caché de fichas está corrupta", viewModel.state.value.fatalError)
    }

    @Test
    fun `without an API key a sync says so and spends nothing`() = onViewModel(
        client = { null },
    ) { viewModel ->
        runCurrent()

        viewModel.sync()
        runCurrent()

        assertFalse(viewModel.state.value.syncing)
        assertEquals(
            "Falta la API key de Numista. Añádela en Credenciales.",
            viewModel.state.value.message?.text,
        )
        assertTrue(requested.isEmpty())
    }

    @Test
    fun `a sync reports what it did and remembers it`() =
        onViewModel { viewModel ->
            runCurrent()

            viewModel.sync()
            val state = viewModel.state.first { it.lastSync != null }

            assertFalse(state.syncing)
            // Token, collection and the one missing ficha: three calls.
            assertEquals(3, state.lastSync?.callsSpent)
            assertEquals(NOW, state.lastSync?.atMillis)
            assertEquals("1 pieza · 1 ficha nueva · 3 consultas", state.message?.text)
            // Stored, not only announced.
            assertEquals(state.lastSync, syncLog.last)
        }

    /**
     * Silent because the collector did not ask for it (#605); the line under the sync button is
     * where it shows.
     */
    @Test
    fun `a launch brings the inventory by itself and says nothing about it`() =
        onViewModel(automaticRefresh = true) { viewModel ->
            val state = viewModel.state.first { it.lastSync != null }

            assertEquals(1, requested.count { it.contains("collected_items") })
            assertEquals(NOW, state.lastSync?.atMillis)
            assertNull(state.message, "un refresco que nadie pidió no interrumpe con un snackbar")
            assertFalse(state.syncing)
        }

    @Test
    fun `a launch whose refresh fails leaves no message behind`() =
        onViewModel(client = { null }, automaticRefresh = true) { viewModel ->
            runCurrent()

            assertNull(viewModel.state.value.message)
            assertNull(viewModel.state.value.lastSync)
            assertFalse(viewModel.state.value.syncing)
        }

    @Test
    fun `a second tap while a sync is in flight is not a second sync`() = onViewModel { viewModel ->
        runCurrent()

        viewModel.sync()
        viewModel.sync()
        viewModel.state.first { it.lastSync != null }

        assertEquals(1, requested.count { it.contains("collected_items") })
    }

    @Test
    fun `two fichas can be asked for at once, and each row reports itself`() = onViewModel(
        given = { types.rows.value = listOf(ficha(), ficha(LOOSE_TYPE + 1)) },
    ) { viewModel ->
        runCurrent()

        viewModel.refreshFicha(LOOSE_TYPE)
        viewModel.refreshFicha(LOOSE_TYPE + 1)

        // A set, so two rows can spin at once instead of greying out the screen (#185).
        assertEquals(setOf(LOOSE_TYPE, LOOSE_TYPE + 1), viewModel.state.value.refreshingFichas)

        val state = viewModel.state.first { it.refreshingFichas.isEmpty() && it.message != null }

        // The snackbar names whichever of the two answered last, hence the shared prefix.
        assertEquals(2, requested.count { it.contains("/types/") })
        assertTrue(state.message!!.text.startsWith("Ficha de Numista 99022"), state.message!!.text)
    }

    @Test
    fun `the same ficha asked for twice costs one call`() = onViewModel(
        given = { types.rows.value = listOf(ficha()) },
    ) { viewModel ->
        runCurrent()

        viewModel.refreshFicha(LOOSE_TYPE)
        viewModel.refreshFicha(LOOSE_TYPE)
        viewModel.state.first { it.refreshingFichas.isEmpty() && it.message != null }

        assertEquals(1, requested.count { it.contains("/types/") })
    }

    @Test
    fun `saving the credentials stores them and says so`() = onViewModel { viewModel ->
        runCurrent()

        val saved = viewModel.saveCredentials(apiKey = " otra ", userId = "3105")
        runCurrent()

        assertTrue(saved)
        assertEquals(Credentials("otra", 3105), credentials.credentials())
        assertEquals("Credenciales guardadas.", viewModel.state.value.message?.text)
        assertNull(viewModel.state.value.validation)
    }

    @Test
    fun `a credentials form that is refused stores nothing at all`() = onViewModel { viewModel ->
        runCurrent()

        val saved = viewModel.saveCredentials(apiKey = "otra", userId = "perfil")
        runCurrent()

        assertFalse(saved)
        assertEquals(Credentials("key", 2104), credentials.credentials())
        assertEquals(
            "El identificador de usuario es el número de la URL de tu perfil de Numista.",
            viewModel.state.value.validation,
        )
    }

    @Test
    fun `signing out forgets the credentials and keeps everything else`() = onViewModel(
        given = { syncLog.last = RECORD },
    ) { viewModel ->
        runCurrent()

        viewModel.signOut()

        assertNull(credentials.credentials())
        assertFalse(viewModel.state.value.onboarded)
        // The collection and the last-sync record stay on the device.
        assertEquals(58, viewModel.state.value.lastSync?.collectionItems)
    }

    @Test
    fun `a chip is written through the moment it is tapped`() = onViewModel { viewModel ->
        runCurrent()

        viewModel.narrowIndex(IndexShelf(sort = IndexSort.Alphabetical))

        // There is no «way out» of a root destination to save it on (ADR 0021 §1).
        assertEquals(IndexSort.Alphabetical, shelves.index.sort)
        assertEquals(IndexSort.Alphabetical, viewModel.state.value.indexShelf.sort)
    }

    /** Both doors end in the card `destinationOf` sends to its plate (ADR 0021 §9, #539). */
    @Test
    fun `a plate and its card print the same pages`() = onViewModel(
        catalogs = listOf(WISHED_CATALOG),
        given = {
            types.rows.value = listOf(ficha())
            items.rows.value = listOf(collected())
        },
    ) { viewModel ->
        runCurrent()
        val card = viewModel.state.value.collection.index.single()
        val options = NotebookOptions(photographs = false)

        val sheet = viewModel.notebookPages(NotebookSubject.Sheet(card), options)
        assertTrue(sheet.isNotEmpty(), "la lámina no ha impreso nada que comparar")
        assertEquals(sheet, viewModel.notebookPages(NotebookSubject.Plate(WISHED_CATALOG.id), options))
    }

    /**
     * Packing a folio and the unclaimed coins only mean something in a notebook (#401): the same
     * card as a sheet and as the index differs by [forSheetExport] and nothing else.
     */
    @Test
    fun `one lamina is printed with the notebook switches cleared`() = onViewModel(
        catalogs = listOf(WISHED_CATALOG),
        given = {
            types.rows.value = listOf(ficha())
            items.rows.value = listOf(collected())
        },
    ) { viewModel ->
        runCurrent()
        val card = viewModel.state.value.collection.index.single()
        val options = NotebookOptions(sharePage = true, unclaimed = true, photographs = false)

        val asIndex = viewModel.notebookPages(
            NotebookSubject.Index(listOf(card), emptyList()),
            options.forSheetExport(),
        )
        assertTrue(asIndex.isNotEmpty(), "la tarjeta no ha impreso nada que comparar")
        assertEquals(asIndex, viewModel.notebookPages(NotebookSubject.Sheet(card), options))
    }

    /**
     * Its coins are in no card of the index, yet it prints with the notebook's geometry and
     * switches (ADR 0029 §7).
     */
    @Test
    fun `the wish list is one more subject of the same printer`() = onViewModel(
        catalogs = listOf(WISHED_CATALOG),
    ) { viewModel ->
        runCurrent()
        assertTrue(
            viewModel.notebookPages(NotebookSubject.Wishes, NotebookOptions()).isEmpty(),
            "sin ninguna casilla marcada no hay folio que gastar",
        )

        viewModel.toggleWish(requireNotNull(WISHED_CATALOG.members.first().wishKey()))
        runCurrent()

        val pages = viewModel.notebookPages(
            NotebookSubject.Wishes,
            NotebookOptions(photographs = false),
        )
        assertEquals(1, pages.size)
        assertEquals(1, pages.single().cells.size)
    }

    @Test
    fun `the notebook is remembered when it is printed`() = onViewModel { viewModel ->
        runCurrent()

        viewModel.notebookPrinted(NotebookOptions(photographs = false))

        assertFalse(notebook.options.photographs)
        assertFalse(viewModel.state.value.notebookOptions.photographs)
    }

    @Test
    fun `a box with no coins is refused out loud, and nothing is stored`() =
        onViewModel { viewModel ->
            runCurrent()

            viewModel.createOwnGrouping("Las francesas", emptyList())
            runCurrent()

            assertEquals(
                "Ponle un nombre a la colección y elige al menos una moneda.",
                viewModel.state.value.message?.text,
            )
            assertTrue(ownGroupings.groupings.value.isEmpty())
        }

    @Test
    fun `a box with a name and coins is created and announced`() = onViewModel { viewModel ->
        runCurrent()

        viewModel.createOwnGrouping("  Las francesas  ", listOf(LOOSE_TYPE))
        runCurrent()

        assertEquals("Las francesas", ownGroupings.groupings.value.single().name)
        assertEquals("Colección «Las francesas» creada.", viewModel.state.value.message?.text)
    }

    @Test
    fun `the banner opens on whatever GitHub published`() = onViewModel { viewModel ->
        val state = viewModel.state.first { it.update is UpdateStatus.Available }

        assertEquals(24, (state.update as UpdateStatus.Available).manifest.versionCode)
    }

    @Test
    fun `installing without the permission asks for it and leaves the button alone`() =
        onViewModel(given = { installer.permitted = false }) { viewModel ->
            viewModel.state.first { it.update is UpdateStatus.Available }

            viewModel.installUpdate()
            val state = viewModel.state.first { it.message != null }

            assertEquals(
                "Concede a Coindex permiso para instalar aplicaciones y vuelve a pulsar Instalar.",
                state.message?.text,
            )
            // The button is never disabled for a branch that fetches nothing.
            assertFalse(state.updating)
            assertTrue(installer.downloads.isEmpty())
        }

    @Test
    fun `the photographs are asked for once the collection has been read`() = onViewModel(
        given = { types.rows.value = listOf(ficha()) },
    ) { viewModel ->
        runCurrent()

        // The first three seconds of a cold start are left to the first screen.
        assertTrue(prefetch.passes.isEmpty())

        advanceTimeBy(4_000)

        assertEquals(
            listOf(THUMBNAIL),
            prefetch.passes.single().images.map { it.obverse.thumbnail },
        )
        assertEquals(1, viewModel.state.value.photoCache.missing)
    }


    /**
     * Same trigger as the photographs, for the issues the collection carries (ADR 0028 §3). Running
     * every launch is affordable because a fully cached pass costs no calls.
     */
    @Test
    fun `the prices are asked for once the collection has been read`() = onViewModel(
        given = {
            types.rows.value = listOf(ficha())
            items.rows.value = listOf(collected())
        },
    ) { viewModel ->
        runCurrent()
        assertTrue(valuationPass.passes.isEmpty())

        advanceTimeBy(4_000)

        assertEquals(
            listOf(OwnedIssue(typeId = LOOSE_TYPE, issueId = 8_508)),
            valuationPass.passes.single().plan.owned,
        )
        assertEquals(0, viewModel.state.value.valuation.missing)
    }

    /** One gesture toggles the mark both ways (ADR 0029 §5). */
    @Test
    fun `marking a casilla twice is a mark and then no mark`() = onViewModel(
        catalogs = listOf(WISHED_CATALOG),
    ) { viewModel ->
        runCurrent()
        val key = requireNotNull(WISHED_CATALOG.members.first().wishKey())

        viewModel.toggleWish(key)
        runCurrent()
        assertEquals(listOf(key), viewModel.state.value.wishes.map { it.key })

        viewModel.toggleWish(key)
        runCurrent()
        assertTrue(viewModel.state.value.wishes.isEmpty())
    }

    /**
     * The collection is empty, yet the marked slot is priced: ADR 0029 §4 lifts #282's filter for
     * it. The pass starts now because «+2 consultas al mes» promises the current month.
     */
    @Test
    fun `a marked casilla reaches the plan of its own pass`() = onViewModel(
        catalogs = listOf(WISHED_CATALOG),
    ) { viewModel ->
        runCurrent()
        advanceTimeBy(4_000)
        val before = valuationPass.passes.size

        viewModel.toggleWish(requireNotNull(WISHED_CATALOG.members.first().wishKey()))
        runCurrent()
        advanceTimeBy(4_000)

        val plan = valuationPass.passes.last().plan
        assertTrue(valuationPass.passes.size > before, "marcar no ha lanzado ningún pase")
        assertEquals(
            listOf(PlateHole(catalogId = WISHED_CATALOG.id, typeId = LOOSE_TYPE, year = 1_929)),
            plan.holes,
        )
    }

    /** A mark on a filled casilla is dead (ADR 0029 §2). */
    @Test
    fun `a mark whose casilla is full is not priced`() = onViewModel(
        catalogs = listOf(WISHED_CATALOG),
        given = {
            types.rows.value = listOf(ficha())
            items.rows.value = listOf(collected())
        },
    ) { viewModel ->
        runCurrent()
        advanceTimeBy(4_000)

        viewModel.toggleWish(requireNotNull(WISHED_CATALOG.members.first().wishKey()))
        runCurrent()
        advanceTimeBy(4_000)

        // The plate's other hole stays: it is priced as the cost of closing, not as a mark.
        assertEquals(listOf(1_930), valuationPass.passes.last().plan.holes.map { it.year })
    }

    /**
     * The pass and the sync share the monthly allowance, so the sync cancels the pass and waits for
     * it to unwind instead of failing with `BudgetExhausted` (ADR 0028 §6).
     */
    @Test
    fun `a sync during a pass takes the budget back and waits for it`() = onViewModel(
        given = {
            types.rows.value = listOf(ficha())
            items.rows.value = listOf(collected())
            valuationPass.gate = CompletableDeferred()
        },
    ) { viewModel ->
        runCurrent()
        advanceTimeBy(4_000)
        assertEquals(1, valuationPass.passes.size)

        viewModel.sync()
        // Not `advanceUntilIdle`: the endless update poll never lets the scheduler idle.
        val state = viewModel.state.first { it.lastSync != null }

        assertEquals(1, valuationPass.cancelled)
        // The record the sync wrote proves it got its calls.
        assertFalse(state.syncing)
        assertEquals(NOW, state.lastSync?.atMillis)
        assertTrue(
            requested.none { it.contains("prices") },
            "el pase cancelado no ha llegado a pedir precios",
        )
    }

    /**
     * A new ViewModel over the same process starts no pass, since the fichas have not changed, and
     * still reports the cache status, which does not travel with the pass (ADR 0024).
     */
    @Test
    fun `a second launch in the same process still knows what the phone holds`() = onViewModel(
        given = { types.rows.value = listOf(ficha()) },
    ) { first ->
        runCurrent()
        advanceTimeBy(4_000)
        assertEquals(1, prefetch.passes.size)
        first.viewModelScope.cancel()

        val second = viewModel()
        runCurrent()
        advanceTimeBy(4_000)

        assertEquals(1, prefetch.passes.size)
        assertEquals(1, second.state.value.photoCache.missing)
        assertEquals(2, second.state.value.photoCache.wanted)
        second.viewModelScope.cancel()
    }

    /** The dump is returned: the share chooser is an `Intent` and belongs to the screen (#548). */
    @Test
    fun `exporting the data returns the written file`() = onViewModel { viewModel ->
        val dump = viewModel.exportData()

        // The date in the name is `DatabaseExportTest`'s to pin.
        assertTrue(dump?.name.orEmpty().startsWith("coindex-0.15.0-"))
        assertEquals("la colección", dump?.readText())
        assertNull(viewModel.state.value.message)
    }

    @Test
    fun `an export that fails says so and hands back nothing`() = onViewModel(
        dataExport = dataExport(base = File(exportRoot, "no-existe.db")),
    ) { viewModel ->
        val dump = viewModel.exportData()

        assertNull(dump)
        assertTrue("No se pudieron exportar los datos" in viewModel.state.value.message?.text.orEmpty())
    }

    /**
     * Screens key their `remember` on the reading, and [ScreenReading]'s fields are `by lazy`, so a
     * new instance per emission would walk the inventory again (#542).
     */
    @Test
    fun `the reading is the same object until the collection under it moves`() = onViewModel(
        given = {
            types.rows.value = listOf(ficha())
            items.rows.value = listOf(collected())
        },
    ) { viewModel ->
        runCurrent()
        val reading = viewModel.reading()

        assertSame(reading, viewModel.reading())

        // A ficha in flight derives nothing.
        viewModel.refreshFicha(LOOSE_TYPE)
        assertSame(reading, viewModel.reading())

        // A second piece is another collection.
        items.rows.value = listOf(collected(), collected().copy(id = 2))
        runCurrent()
        assertTrue(reading !== viewModel.reading())
    }

    /**
     * Stamped on arrival rather than per emission, which would move every age on screen; the
     * figures it dates never expire (ADR 0030 §4).
     */
    @Test
    fun `a price book that lands is stamped, and the empty one it replaces was stamped at launch`() =
        onViewModel { viewModel ->
            runCurrent()
            // Never 1970: an age read before the first book needs a reference.
            assertEquals(NOW, viewModel.state.value.pricesArrivedAt)

            clock = NOW + 60_000
            prices.prices.value = listOf(
                IssuePriceEntity(typeId = LOOSE_TYPE, issueId = 8_508, grade = "unc", eur = 40.0),
            )
            runCurrent()

            assertEquals(NOW + 60_000, viewModel.state.value.pricesArrivedAt)
        }

    /**
     * A pass writes its rows one by one, so a single memo would rebuild the shelf window and the
     * open plate once per row (#218).
     */
    @Test
    fun `a price that lands leaves the collection's own reading where it was`() = onViewModel(
        given = {
            types.rows.value = listOf(ficha())
            items.rows.value = listOf(collected())
        },
    ) { viewModel ->
        runCurrent()
        val before = viewModel.reading()

        prices.prices.value = listOf(
            IssuePriceEntity(typeId = LOOSE_TYPE, issueId = 8_508, grade = "unc", eur = 40.0),
        )
        runCurrent()
        val after = viewModel.reading()

        assertTrue(before !== after)
        assertSame(before.of, after.of)
    }

    @Test
    fun `the button is free again once the copy is written`() = onViewModel { viewModel ->
        assertFalse(viewModel.state.value.exportingData)

        viewModel.exportData()

        assertFalse(viewModel.state.value.exportingData)
    }
}
