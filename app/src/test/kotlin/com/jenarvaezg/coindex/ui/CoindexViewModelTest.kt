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
import com.jenarvaezg.coindex.data.FakeShelfStore
import com.jenarvaezg.coindex.data.FakeSyncLog
import com.jenarvaezg.coindex.data.FakeTypeMetaDao
import com.jenarvaezg.coindex.data.SyncRecord
import com.jenarvaezg.coindex.data.SyncService
import com.jenarvaezg.coindex.data.TypeRefresh
import com.jenarvaezg.coindex.data.db.ApiCallEntity
import com.jenarvaezg.coindex.data.db.TypeMetaEntity
import com.jenarvaezg.coindex.data.numista.CallBudget
import com.jenarvaezg.coindex.data.numista.NumistaClient
import com.jenarvaezg.coindex.data.photos.PhotoCacheStatus
import com.jenarvaezg.coindex.data.photos.PhotoPrefetchLoop
import com.jenarvaezg.coindex.data.photos.PrefetchConditions
import com.jenarvaezg.coindex.data.update.FakeUpdateInstaller
import com.jenarvaezg.coindex.data.update.UpdateChecker
import com.jenarvaezg.coindex.data.update.UpdateFlow
import com.jenarvaezg.coindex.data.update.UpdateStatus
import com.jenarvaezg.coindex.domain.Curation
import com.jenarvaezg.coindex.ui.print.NotebookOptions
import com.jenarvaezg.coindex.ui.shelf.IndexShelf
import com.jenarvaezg.coindex.ui.shelf.IndexSort
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
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

    /** Held outside the ViewModel, exactly as `AppContainer` holds it. */
    private val photos = PhotoPrefetchLoop(
        prefetch,
        { syncing -> PrefetchConditions(unmeteredNetwork = true, syncing = syncing) },
    )
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
    ): CoindexViewModel {
        val repository = CoindexRepository(
            collectedItemDao = items,
            typeMetaDao = types,
            ownGroupingDao = ownGroupings,
            curation = Curation(catalogs = emptyList()),
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
            client = client,
            warmUpFichaCache = warmUp,
            installedVersionName = "0.15.0",
        )
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
        given: () -> Unit = {},
        body: suspend TestScope.(CoindexViewModel) -> Unit,
    ) = runTest(dispatcher) {
        given()
        val viewModel = viewModel(client, warmUp)
        try {
            body(viewModel)
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

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
            viewModel.state.value.message,
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
            assertEquals("1 pieza · 1 ficha nueva · 3 llamadas", state.message)
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
        assertTrue(state.message!!.startsWith("Ficha de Numista 99022"), state.message!!)
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
        assertEquals("Ajustes guardados.", viewModel.state.value.message)
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
                viewModel.state.value.message,
            )
            assertTrue(ownGroupings.groupings.value.isEmpty())
        }

    @Test
    fun `a box with a name and coins is created and announced`() = onViewModel { viewModel ->
        runCurrent()

        viewModel.createOwnGrouping("  Las francesas  ", listOf(LOOSE_TYPE))
        runCurrent()

        assertEquals("Las francesas", ownGroupings.groupings.value.single().name)
        assertEquals("Colección «Las francesas» creada.", viewModel.state.value.message)
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
                state.message,
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
}
