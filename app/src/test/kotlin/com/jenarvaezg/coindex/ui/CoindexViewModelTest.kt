package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.ApiCallLedger
import com.jenarvaezg.coindex.data.CoindexRepository
import com.jenarvaezg.coindex.data.CollectionSync
import com.jenarvaezg.coindex.data.Credentials
import com.jenarvaezg.coindex.data.FakeApiCallDao
import com.jenarvaezg.coindex.data.FakeCollectedItemDao
import com.jenarvaezg.coindex.data.FakeCredentialStore
import com.jenarvaezg.coindex.data.FakeNotebookStore
import com.jenarvaezg.coindex.data.FakeOwnGroupingDao
import com.jenarvaezg.coindex.data.FakePhotoPrefetch
import com.jenarvaezg.coindex.data.FakePriceDao
import com.jenarvaezg.coindex.data.FakeShelfStore
import com.jenarvaezg.coindex.data.FakeSyncLog
import com.jenarvaezg.coindex.data.FakeTypeMetaDao
import com.jenarvaezg.coindex.data.FakeValuationPass
import com.jenarvaezg.coindex.data.FakeWishDao
import com.jenarvaezg.coindex.data.SyncRecord
import com.jenarvaezg.coindex.data.SyncService
import com.jenarvaezg.coindex.data.TypeRefresh
import com.jenarvaezg.coindex.data.db.ApiCallEntity
import com.jenarvaezg.coindex.data.db.CollectedItemEntity
import com.jenarvaezg.coindex.data.db.DatabaseExport
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

/** The last sync there was, for the tests that check it is neither invented nor thrown away. */
private val RECORD = SyncRecord(
    atMillis = NOW,
    collectionItems = 58,
    typesFetched = 0,
    callsSpent = 2,
)

/**
 * A two-year date run of the type the collection carries, so a casilla can be marked (ADR 0029).
 *
 * Two members and not one, because the sharp questions are about telling them apart: a mark is keyed on
 * the year as well as the type, and a mark whose coin arrives has to die without taking its sibling's
 * hole with it.
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
 * The state the screens read, and what each gesture does to it (#220).
 *
 * The largest file in the app had no test at all, for two reasons the constructor below is the
 * answer to: it took an `AppContainer`, which is not something a test can substitute, and it read
 * three clocks in place. Every collaborator here is a stand-in, every clock is held still, and what
 * is left to check is exactly what a ViewModel is for — that a tap moves the right field, that a
 * refusal is spoken, and that nothing is written twice.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CoindexViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    private val items = FakeCollectedItemDao()
    private val types = FakeTypeMetaDao()
    private val ownGroupings = FakeOwnGroupingDao()
    private val apiCalls = FakeApiCallDao()
    private val credentials = FakeCredentialStore(Credentials("key", 2104))
    private val shelves = FakeShelfStore()
    private val notebook = FakeNotebookStore()
    private val syncLog = FakeSyncLog()
    private val prefetch = FakePhotoPrefetch(PhotoCacheStatus(wanted = 2, missing = 1))
    private val prices = FakePriceDao()
    private val wishes = FakeWishDao()
    private val valuationPass = FakeValuationPass()

    /** Held outside the ViewModel, exactly as `AppContainer` holds it. */
    private val photos = PhotoPrefetchLoop(
        prefetch,
        { syncing -> PrefetchConditions(unmeteredNetwork = true, syncing = syncing) },
    )

    /** Held outside it for the same reason, and told whether a sync is in flight the same way. */
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

    private fun viewModel(
        client: () -> NumistaClient? = { numistaClient() },
        warmUp: suspend () -> Unit = { warmedUp += 1 },
        catalogs: List<CollectionCatalog> = emptyList(),
        dataExport: DatabaseExport = dataExport(),
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
        return CoindexViewModel(
            repository = { repository },
            credentials = credentials,
            shelves = shelves,
            notebook = notebook,
            collectionSync = CollectionSync(
                syncService = SyncService(items, types, ledger) { NOW },
                syncLog = syncLog,
            ) { NOW },
            typeRefresh = TypeRefresh(types) { NOW },
            updates = UpdateFlow(updateChecker(), installer) { NOW },
            photos = photos,
            valuation = valuation,
            client = client,
            warmUpFichaCache = warmUp,
            dataExport = dataExport,
            installedVersionName = "0.15.0",
        )
    }

    /**
     * A dump over real files in a temporary directory (#548).
     *
     * Nothing is mocked because there is nothing to mock: [DatabaseExport] is four values and a
     * lambda, and what this class needs to know about it is what the ViewModel does with the file
     * it returns and with the failure it throws.
     */
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
     * One test, one ViewModel, and the ViewModel cancelled at the end.
     *
     * The update poll is an endless `while (true)` on purpose — a session left open all afternoon has
     * to notice a release — so a test that did not cancel the scope would keep the scheduler busy for
     * ever and never finish. Cancelling is what a screen going away does anyway.
     *
     * @param given whatever the stores have to be holding **before** the ViewModel reads them, which
     *   is most of what a launch is.
     */
    private fun onViewModel(
        client: () -> NumistaClient? = { numistaClient() },
        warmUp: suspend () -> Unit = { warmedUp += 1 },
        // The curated shelf, empty unless a test needs a casilla to mark: what a wish resolves against
        // is the file, so there is no marked slot at all without a catalog that names it (ADR 0029 §2).
        catalogs: List<CollectionCatalog> = emptyList(),
        dataExport: DatabaseExport = dataExport(),
        given: () -> Unit = {},
        body: suspend TestScope.(CoindexViewModel) -> Unit,
    ) = runTest(dispatcher) {
        given()
        val viewModel = viewModel(client, warmUp, catalogs, dataExport)
        try {
            body(viewModel)
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }


    /**
     * One piece of the ficha above, with the issue the valuation is addressed by.
     *
     * The issue id lives in the stored body and not in a column (ADR 0028), so a row that means to carry
     * one has to have it in `raw` — which is also the only place a real sync ever puts it.
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
            "Falta la API key de Numista. Añádela en Ajustes.",
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
            // Token, collection and the one ficha nobody had: three calls, and the report says so.
            assertEquals(3, state.lastSync?.callsSpent)
            assertEquals(NOW, state.lastSync?.atMillis)
            assertEquals("1 pieza · 1 ficha nueva · 3 consultas", state.message?.text)
            // Written down before it was announced: the snackbar is the copy.
            assertEquals(state.lastSync, syncLog.last)
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

        // Two rows working and not one screen greyed out: that is why it is a set (#185).
        assertEquals(setOf(LOOSE_TYPE, LOOSE_TYPE + 1), viewModel.state.value.refreshingFichas)

        val state = viewModel.state.first { it.refreshingFichas.isEmpty() && it.message != null }

        // Each of the two was asked for, and the snackbar names the ficha it is talking about —
        // whichever of the two answered last, because the two calls are independent.
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
    fun `saving the settings stores the credentials and says so`() = onViewModel { viewModel ->
        runCurrent()

        val saved = viewModel.saveSettings(apiKey = " otra ", userId = "3105")
        runCurrent()

        assertTrue(saved)
        assertEquals(Credentials("otra", 3105), credentials.credentials())
        assertEquals("Ajustes guardados.", viewModel.state.value.message?.text)
        assertNull(viewModel.state.value.validation)
    }

    @Test
    fun `a settings form that is refused stores nothing at all`() = onViewModel { viewModel ->
        runCurrent()

        val saved = viewModel.saveSettings(apiKey = "otra", userId = "perfil")
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
        // The collection stays on the device, and so does the record of when it was last synced.
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

        // Three seconds of cold start belong to the first screen and to nothing else.
        assertTrue(prefetch.passes.isEmpty())

        advanceTimeBy(4_000)

        assertEquals(
            listOf(THUMBNAIL),
            prefetch.passes.single().images.map { it.obverse.thumbnail },
        )
        assertEquals(1, viewModel.state.value.photoCache.missing)
    }


    /**
     * The valuation runs on the same trigger as the photographs, and asks about the issues the
     * collection carries (ADR 0028 §3).
     *
     * Every launch, because asking for what is missing is idempotent: with everything cached the second
     * launch of a month costs zero calls, which is what makes «every launch» affordable at all.
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

    /**
     * One gesture in both directions, because that is what a casilla is (ADR 0029 §5).
     *
     * A press on a hole marks it and a second press takes the mark off, and the state it is toggling is
     * the one on screen. Marking twice is not an event either: the row keeps the date of the first
     * mark, which is what stops the list of the annex reshuffling itself under the collector's thumb.
     */
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
     * A new mark starts a pass, and the marked casilla is in its plan (ADR 0029 §4).
     *
     * The plate has **no evidence at all** here — the collection is empty — so this is the filter #282
     * closed and ADR 0029 reopens for the marked slot alone: what the collector marks gets priced,
     * wherever it comes from. And it is asked for now rather than on the next launch, because the
     * gesture's «+2 consultas al mes» is a promise about the month it was made in.
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

    /** And a mark whose coin is already in the collection is not in the plan: it is dead (ADR 0029 §2). */
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

        // The mark is dead and gone from the plan; the plate's **other** hole is still there, because
        // that one is the cost of closing it and has nothing to do with the mark.
        assertEquals(listOf(1_930), valuationPass.passes.last().plan.holes.map { it.year })
    }

    /**
     * **A sync launched during a pass wins, and it does not fail with `BudgetExhausted`.**
     *
     * The gravest of the yields, and the one this loop is stricter about than its photographic sibling:
     * the two spend the *same* monthly allowance, so a pass still unwinding can be inside `reserve()`
     * taking a call the sync is about to need. Waited for, and not merely cancelled (ADR 0028 §6).
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
        // Waited for by its own outcome and never with `advanceUntilIdle`: the update poll is an endless
        // `while (true)`, so the scheduler is never idle and a test that waited for it would hang.
        val state = viewModel.state.first { it.lastSync != null }

        assertEquals(1, valuationPass.cancelled)
        // And the sync went through: it is the one that must not fail with `BudgetExhausted`, and the
        // record it wrote is the proof it got its calls.
        assertFalse(state.syncing)
        assertEquals(NOW, state.lastSync?.atMillis)
        assertTrue(
            requested.none { it.contains("prices") },
            "el pase cancelado no ha llegado a pedir precios",
        )
    }

    /**
     * The collector leaves the app and comes back: a new ViewModel over the same process.
     *
     * No new pass, because the fichas have not changed — reopening sixteen hundred cache snapshots
     * to find that out is the cold start the guard exists to avoid — and the settings line still has
     * to be true. It said «no hay fotos que traer» over a phone holding all of them until the status
     * stopped travelling with the pass (ADR 0024).
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

    /**
     * The dump is handed back rather than sent: the chooser is an `Intent` and belongs to the screen
     * (#548).
     */
    @Test
    fun `exporting the data returns the written file`() = onViewModel { viewModel ->
        val dump = viewModel.exportData()

        // The day itself is `DatabaseExportTest`'s to pin, with a clock it can hold still; what this
        // one is about is that the file the ViewModel hands over is the one that was written.
        assertTrue(dump?.name.orEmpty().startsWith("coindex-0.15.0-"))
        assertEquals("la colección", dump?.readText())
        assertNull(viewModel.state.value.message)
    }

    /**
     * A base that cannot be copied is a message, not a crash: there is nothing on the other side of
     * this gesture worth taking the app down for.
     */
    @Test
    fun `an export that fails says so and hands back nothing`() = onViewModel(
        dataExport = dataExport(base = File(exportRoot, "no-existe.db")),
    ) { viewModel ->
        val dump = viewModel.exportData()

        assertNull(dump)
        assertTrue("No se pudieron exportar los datos" in viewModel.state.value.message?.text.orEmpty())
    }

    /** And the flag it raises comes back down, whichever of the two ways it ended. */
    @Test
    fun `the button is free again once the copy is written`() = onViewModel { viewModel ->
        assertFalse(viewModel.state.value.exportingData)

        viewModel.exportData()

        assertFalse(viewModel.state.value.exportingData)
    }
}
