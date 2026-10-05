package com.jenarvaezg.coindex.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jenarvaezg.coindex.data.db.DATABASE_MIME_TYPE
import com.jenarvaezg.coindex.data.update.UpdateStatus
import com.jenarvaezg.coindex.ui.APP_NAME
import com.jenarvaezg.coindex.ui.components.BackGlyph
import com.jenarvaezg.coindex.ui.components.CardAction
import com.jenarvaezg.coindex.ui.components.FichaRefresh
import com.jenarvaezg.coindex.ui.components.LocalNavAnimation
import com.jenarvaezg.coindex.ui.components.LocalSharedTransition
import com.jenarvaezg.coindex.ui.components.PrimaryAction
import com.jenarvaezg.coindex.ui.components.paperSurface
import com.jenarvaezg.coindex.ui.print.NotebookSubject
import com.jenarvaezg.coindex.ui.screens.CoinSheetSurface
import com.jenarvaezg.coindex.ui.screens.CoinsScreen
import com.jenarvaezg.coindex.ui.screens.ExploreScreen
import com.jenarvaezg.coindex.ui.screens.WishesScreen
import com.jenarvaezg.coindex.ui.screens.FiguresScreen
import com.jenarvaezg.coindex.ui.screens.IndexScreen
import com.jenarvaezg.coindex.ui.screens.OnboardingScreen
import com.jenarvaezg.coindex.ui.screens.MissingSubject
import com.jenarvaezg.coindex.ui.screens.NoticesScreen
import com.jenarvaezg.coindex.ui.screens.PiecesScreen
import com.jenarvaezg.coindex.ui.screens.PlateMarking
import com.jenarvaezg.coindex.ui.screens.PlateScreen
import com.jenarvaezg.coindex.ui.screens.CredentialsScreen
import com.jenarvaezg.coindex.ui.screens.PhoneScreen
import com.jenarvaezg.coindex.ui.shelf.CoinsShelf
import com.jenarvaezg.coindex.ui.shelf.NotebookAxis
import com.jenarvaezg.coindex.ui.shelf.YearFilter
import com.jenarvaezg.coindex.ui.theme.Paper
import kotlinx.coroutines.launch

@Composable
fun CoindexApp(viewModel: CoindexViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val snackbarHost = remember { SnackbarHostState() }
    val context = LocalContext.current
    // Composition-scoped, not ViewModel-scoped: what runs on it ends in a chooser, which needs the
    // screen to still be there.
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()

    // Back in the foreground: check for an update (throttled by shouldCheckForUpdate) and retry
    // the missing photos, in case there is wifi now (#191).
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        viewModel.checkForUpdate()
        viewModel.retryPhotoPrefetch()
    }

    LaunchedEffect(state.message) {
        state.message?.let { notice ->
            // Abrir is offered only when something can open the file (#436). openDownloadedFile
            // still catches the viewer vanishing between this check and the tap.
            val openFile = notice.openFile?.takeIf { file ->
                canViewDownloadedFile(context, Uri.parse(file.uri), file.mimeType)
            }
            val result = snackbarHost.showSnackbar(
                message = notice.text,
                actionLabel = openFile?.let { DOWNLOAD_OPEN_ACTION },
                duration = noticeDuration(hasAction = openFile != null),
            )
            if (result == SnackbarResult.ActionPerformed) {
                openFile?.let { file ->
                    if (!openDownloadedFile(context, Uri.parse(file.uri), file.mimeType)) {
                        snackbarHost.showSnackbar(DOWNLOAD_NO_VIEWER_MESSAGE)
                    }
                }
            }
            viewModel.dismissMessage()
        }
    }

    // Everything derived from the state, computed in one place (#542). The same object comes back
    // while none of its inputs has changed, which is what the `remember`s below key on.
    val reading = viewModel.reading(state)

    val openUrl: (String) -> Unit = { url ->
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    val openTypeOnNumista: (Int) -> Unit = { typeId -> openUrl(reading.numistaUrl(typeId)) }
    // Every card opens through here, and so does a claim on a coin's sheet (ADR 0021 §9).
    val openCard: (CardDestination) -> Unit = { destination ->
        navController.navigate(routeOf(destination))
    }

    // A plate's money and what tasar it would spend (#541); the régimes are `PlateFinance`'s, and
    // only who handles a press is decided here. Keyed on the reading alone: `state.valuingPlate`
    // flips twice per press and nothing in here reads it.
    val plateFinance = remember(reading) {
        reading.plateFinance(onValue = viewModel::valuePlate, onMessage = viewModel::showMessage)
    }

    // Shared by the two surfaces that show a piece of a type (#185), so they agree on a ficha's age
    // and on whether it is being refreshed.
    val ficha: (Int) -> FichaRefresh = { typeId ->
        FichaRefresh(
            fetchedAt = reading.fichaFetchedAt(typeId),
            refreshing = typeId in state.refreshingFichas,
            onRefresh = { viewModel.refreshFicha(typeId) },
        )
    }

    // The coin sheet of the three surfaces that draw casillas (#508). Remembered so its lambdas are
    // stable: a fresh one per recomposition would rebuild the row on every frame of the entrance.
    // `refreshingFichas` is the only key besides the reading because it is in flight, not derived.
    val coinSheet = remember(reading, state.refreshingFichas) {
        CoinSheetSurface(
            coin = reading::coin,
            ficha = ficha,
            value = reading::coinValue,
            onOpenNumista = openTypeOnNumista,
            onOpenClaim = openCard,
        )
    }

    // The plate and the collections take their masthead title from the route's arguments; every
    // other destination's title comes from the route alone.
    val route = backStackEntry?.destination?.route
    val subjectName = when {
        Routes.isPlate(route) ->
            reading.catalogName(backStackEntry?.arguments?.getString("catalogId"))
        // The whole key, not just the family: three Britannias share one (#22). The name is read
        // off the card so it keeps the disambiguation against its neighbours (#565).
        Routes.isDerivedCollection(route) -> variantKeyFromRoute(
            family = backStackEntry?.arguments?.getString("family"),
            weight = backStackEntry?.arguments?.getString("weight"),
            finish = backStackEntry?.arguments?.getString("finish"),
            metal = backStackEntry?.arguments?.getString("metal"),
        )?.let(reading::derivedName)
        Routes.isOwnGrouping(route) -> backStackEntry
            ?.arguments
            ?.getString("groupingId")
            ?.toLongOrNull()
            ?.let(reading::boxName)
        else -> null
    }

    // A root has nothing to pop, so it offers «Este teléfono» instead of «Volver» (ADR 0021 §1).
    // Onboarding has no masthead actions.
    val atRoot = state.onboarded && Routes.isRoot(route)

    val onBack: (() -> Unit)? =
        if (state.onboarded && route != null && !Routes.isRoot(route)) {
            { navController.popBackStack() }
        } else {
            null
        }
    val onOpenPhone: (() -> Unit)? =
        if (atRoot) {
            { navController.navigate(Routes.PHONE) }
        } else {
            null
        }

    Scaffold(
        // Transparent so the paper [CoindexTheme] paints also shows behind the status bar (#351).
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopChrome {
                // The roots draw the sewn edge as their masthead (ADR 0026 §1, §13).
                if (!Routes.ownsChrome(route)) {
                    Masthead(
                        // The installed version is in Avisos y licencias instead (#410).
                        subtitle = screenTitle(route, subjectName),
                        onBack = onBack,
                        onOpenPhone = onOpenPhone,
                    )
                }
                (state.update as? UpdateStatus.Available)?.let { available ->
                    UpdateBanner(available, state.updating, viewModel::installUpdate)
                }
            }
        },
        bottomBar = {
            // Only on the roots: deeper screens leave with «Volver», and a bar there would be a
            // second way back that goes somewhere else.
            if (atRoot) {
                HierarchyBar(
                    route = route,
                    collections = reading.sewnEdge?.collections,
                    // Distinct types, the same count the sewn edge prints (#426, #516).
                    types = reading.sewnEdge?.types,
                    // Grams, never money (#316). Null until the sewn edge is ready, so loading
                    // doesn't read as an empty collection (#418).
                    grams = reading.sewnEdge?.let { reading.figures.figures.weight.value },
                    onCross = { destination ->
                        navController.navigate(destination) {
                            // The roots are siblings, not a stack: crossing piles up no entries
                            // and each root keeps its scroll position.
                            popUpTo(Routes.INDEX) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { padding ->
        val content = Modifier.fillMaxSize().padding(padding)
        when {
            state.fatalError != null -> FatalError(state.fatalError!!, content)
            !state.onboarded -> OnboardingScreen(
                validation = state.validation,
                onSave = viewModel::completeOnboarding,
                modifier = content,
            )
            // The coin of ADR 0026 §3 flies between both ends of a navigation, and the NavHost is
            // the only thing on both sides of one, so the shared-transition layout goes around it.
            // What flies is decided by `Modifier.travellingCoin` on the die-cut holes.
            //
            // Both destinations stay composed during the flight, each on opaque paper ([page]), so
            // the one on top just covers the other (#377, #381). The NavHost stacks by depth: going
            // in, the plate lands on top and needs no transition. Coming back it is still on top
            // while it leaves, so without the pop-exit fade it would sit opaque through the flight
            // and vanish in one frame (#370). With animations off (#514) the fade needs no special
            // case: at scale zero it ends on its first frame.
            else -> TravelLayout(modifier = content) {
                NavHost(
                    navController = navController,
                    startDestination = Routes.INDEX,
                    modifier = Modifier.fillMaxSize(),
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None },
                    popEnterTransition = { EnterTransition.None },
                    popExitTransition = { fadeOut(tween(LIFT_MS)) },
                ) {
                    page(Routes.INDEX) {
                        Travelling(this) {
                            IndexScreen(
                                state = state.collection,
                                loading = state.loading,
                                lastSync = state.lastSync,
                                shelf = state.indexShelf,
                                catalogs = reading.catalogs,
                                onNarrow = viewModel::narrowIndex,
                                onOpen = openCard,
                                onOpenCoins = { coinsShelf ->
                                    crossToCoins(navController, viewModel, coinsShelf)
                                },
                                sewnEdge = reading.sewnEdge,
                                // Rows, not a count: the head of the sheet draws the first few
                                // casillas (#520). No costs here; the list one tap in has them.
                                wishes = reading.wishedRows,
                                showcase = reading.showcase.size,
                                onOpenWishes = { navController.navigate(Routes.WISHES) },
                                onOpenShowcase = { navController.navigate(Routes.EXPLORE) },
                                onOpenPhone = { navController.navigate(Routes.PHONE) },
                                notebookOptions = state.notebookOptions,
                                onNotebookPrinted = viewModel::notebookPrinted,
                                notebook = { cards, unclaimed, options ->
                                    viewModel.notebookPages(
                                        NotebookSubject.Index(cards, unclaimed),
                                        options,
                                    )
                                },
                                onMessage = viewModel::showMessage,
                                onExporting = viewModel::notebookExporting,
                            )
                        }
                    }
                    page(Routes.COINS) {
                        CoinsScreen(
                            state = state.collection,
                            shelf = state.coinsShelf,
                            curatedNames = reading.curatedNames,
                            onNarrow = viewModel::narrowCoins,
                            onCreateBox = viewModel::createOwnGrouping,
                            onAddToBox = viewModel::addToOwnGrouping,
                            sewnEdge = reading.sewnEdge,
                            onOpenPhone = { navController.navigate(Routes.PHONE) },
                            sheet = coinSheet,
                        )
                    }
                    page(Routes.FIGURES) {
                        FiguresScreen(
                            subject = reading.figures,
                            sewnEdge = reading.sewnEdge,
                            // The spot is dated by when the prices arrived, not by a ticking clock,
                            // so «plata de hoy» doesn't change while you look at it (#316).
                            nowMillis = reading.pricesArrivedAt,
                            onOpenCountry = { country ->
                                crossToCoins(
                                    navController,
                                    viewModel,
                                    CoinsShelf(issuer = country, axis = NotebookAxis.ByCountry),
                                )
                            },
                            onOpenYear = { year ->
                                crossToCoins(
                                    navController,
                                    viewModel,
                                    CoinsShelf(
                                        year = YearFilter.Of(year),
                                        axis = NotebookAxis.ByYear,
                                    ),
                                )
                            },
                            onOpenPhone = { navController.navigate(Routes.PHONE) },
                        )
                    }
                    page(Routes.OWN_GROUPING) { entry ->
                        val boxId = entry.arguments?.getString("groupingId")?.toLongOrNull()
                        val card = boxId?.let(reading::boxCard)
                        PiecesScreen(
                            state = state.collection,
                            subject = card?.let(reading::pieces),
                            onOpenNumista = openTypeOnNumista,
                            onMessage = viewModel::showMessage,
                            ficha = ficha,
                            notebookOptions = state.notebookOptions,
                            onNotebookPrinted = viewModel::notebookPrinted,
                            notebookPages = { options ->
                                card?.let {
                                    viewModel.notebookPages(NotebookSubject.Sheet(it), options)
                                } ?: emptyList()
                            },
                            onExporting = viewModel::notebookExporting,
                            upkeep = card?.let { box ->
                                BoxUpkeep(
                                    onRename = { name ->
                                        viewModel.renameOwnGrouping(box.box.id, name)
                                    },
                                    onRemoveType = { typeId ->
                                        viewModel.removeFromOwnGrouping(box.box.id, typeId)
                                    },
                                    // A deleted box leaves nothing to show, so the screen goes too.
                                    onDelete = {
                                        viewModel.deleteOwnGrouping(box.box.id)
                                        navController.popBackStack()
                                    },
                                )
                            },
                        )
                    }
                    page(Routes.DERIVED_COLLECTION) { entry ->
                        val key = variantKeyFromRoute(
                            family = entry.arguments?.getString("family"),
                            weight = entry.arguments?.getString("weight"),
                            finish = entry.arguments?.getString("finish"),
                            metal = entry.arguments?.getString("metal"),
                        )
                        // A route that isn't a whole canonical key is not guessed at: the key is
                        // the identity of a derived card (ADR 0021 §5).
                        if (key == null) {
                            MissingSubject(
                                UNKNOWN_VARIANT_LINK,
                                Modifier.fillMaxSize().padding(20.dp),
                            )
                        } else {
                            val card = reading.derivedCard(key)
                            // No upkeep: nobody typed a derived collection. When it is gone,
                            // `PiecesScreen` uses its default wording (ADR 0026 §5).
                            PiecesScreen(
                                state = state.collection,
                                subject = card?.let(reading::pieces),
                                onOpenNumista = openTypeOnNumista,
                                onMessage = viewModel::showMessage,
                                ficha = ficha,
                                notebookOptions = state.notebookOptions,
                                onNotebookPrinted = viewModel::notebookPrinted,
                                notebookPages = { options ->
                                    card?.let {
                                        viewModel.notebookPages(NotebookSubject.Sheet(it), options)
                                    } ?: emptyList()
                                },
                                onExporting = viewModel::notebookExporting,
                            )
                        }
                    }
                    // The annex (ADR 0026 §8), reached only from the last row of the Colecciones
                    // list. The shelf window is here, the wish list one door further in
                    // (ADR 0030 §8).
                    page(Routes.EXPLORE) {
                        ExploreScreen(
                            tiles = reading.tiles,
                            wishes = reading.livingWishes.size,
                            images = state.collection.images,
                            onOpenPlate = { catalogId ->
                                navController.navigate(Routes.plate(catalogId))
                            },
                            onOpenWishes = { navController.navigate(Routes.WISHES) },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    page(Routes.WISHES) {
                        WishesScreen(
                            subject = reading.wishAnnex,
                            images = state.collection.images,
                            notebookOptions = state.notebookOptions,
                            onNotebookPrinted = viewModel::notebookPrinted,
                            notebookPages = { options ->
                                viewModel.notebookPages(NotebookSubject.Wishes, options)
                            },
                            onExporting = viewModel::notebookExporting,
                            sheet = coinSheet,
                            onRemove = viewModel::removeWish,
                            onMessage = viewModel::showMessage,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    page(Routes.PHONE) {
                        PhoneScreen(
                            photoCache = state.photoCache,
                            valuation = state.valuation,
                            // Labelled, since nothing else on this card is about the marks
                            // (ADR 0029 §5). Null rather than zero when nothing is marked.
                            wishSpend = wishBudgetLabel(reading.wishCalls),
                            syncing = state.syncing,
                            exporting = state.exportingData,
                            onSync = viewModel::sync,
                            // Written by the ViewModel, sent from here: the chooser is an Intent
                            // and belongs to the screen, like every other export (#548).
                            onExportData = {
                                scope.launch {
                                    viewModel.exportData()?.let { dump ->
                                        runCatching {
                                            handToShareSheet(
                                                context = context,
                                                file = dump,
                                                mimeType = DATABASE_MIME_TYPE,
                                                title = DATA_EXPORT_CHOOSER_TITLE,
                                            )
                                        }.onFailure { failure ->
                                            viewModel.showMessage(dataExportFailure(failure.message))
                                        }
                                    }
                                }
                            },
                            onOpenCredentials = { navController.navigate(Routes.CREDENTIALS) },
                            onOpenNotices = { navController.navigate(Routes.NOTICES) },
                        )
                    }
                    page(Routes.CREDENTIALS) {
                        // Read once per visit: the form owns its edits from then on, and it opens
                        // without the last visit's validation error.
                        val values = remember { viewModel.currentCredentials() }
                        LaunchedEffect(Unit) { viewModel.clearValidation() }
                        CredentialsScreen(
                            values = values,
                            validation = state.validation,
                            // Back to «Este teléfono», where the sync that sent the collector here
                            // lives.
                            onSave = { apiKey, userId ->
                                if (viewModel.saveCredentials(apiKey, userId)) {
                                    navController.popBackStack()
                                }
                            },
                            // Popped before signing out: the controller outlives the NavHost, and
                            // a surviving entry would title the onboarding form «Credenciales» and
                            // reopen after the next sign-in.
                            onSignOut = {
                                navController.popBackStack(Routes.INDEX, inclusive = false)
                                viewModel.signOut()
                            },
                        )
                    }
                    page(Routes.NOTICES) {
                        NoticesScreen(versionName = state.versionName)
                    }
                    page(Routes.PLATE) { entry ->
                        val catalogId = entry.arguments?.getString("catalogId").orEmpty()
                        Travelling(this) {
                            PlateScreen(
                                // Once per reading, not per recomposition (#218): building the
                                // album walks the inventory; scrolls and exports don't change it.
                                result = remember(reading, catalogId) { reading.plate(catalogId) },
                                images = state.collection.images,
                                finance = plateFinance,
                                marking = remember(reading) {
                                    PlateMarking(reading.wishedKeys, viewModel::toggleWish)
                                },
                                valuing = state.valuingPlate == catalogId,
                                notebookOptions = state.notebookOptions,
                                onNotebookPrinted = viewModel::notebookPrinted,
                                notebookPages = { options ->
                                    viewModel.notebookPages(
                                        NotebookSubject.Plate(catalogId),
                                        options,
                                    )
                                },
                                onExporting = viewModel::notebookExporting,
                                onOpenSource = openUrl,
                                sheet = coinSheet,
                                onMessage = viewModel::showMessage,
                                nowMillis = reading.pricesArrivedAt,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Crosses to Coins with the shelf already narrowed, from the index's country and year axes (#386)
 * and from the figures of «Las cifras». Navigates like the bottom bar: no stacking, scroll kept.
 */
private fun crossToCoins(
    navController: androidx.navigation.NavHostController,
    viewModel: CoindexViewModel,
    shelf: CoinsShelf,
) {
    viewModel.narrowCoins(shelf)
    navController.navigate(Routes.COINS) {
        popUpTo(Routes.INDEX) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * The app's single shared-element layout, around the NavHost because a shared element spans two
 * screens: the hole of a card and the hole of a casilla are the same object (#300).
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun TravelLayout(modifier: Modifier, content: @Composable () -> Unit) {
    SharedTransitionLayout(modifier = modifier) {
        CompositionLocalProvider(LocalSharedTransition provides this, content = content)
    }
}

/** Exposes a destination's own enter/exit scope to the shared elements inside it. */
@Composable
private fun Travelling(scope: AnimatedVisibilityScope, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalNavAnimation provides scope, content = content)
}

/** The fade of the page on top as it leaves on the way back. */
private const val LIFT_MS = 180

/**
 * A destination on opaque paper, so it can cover the one it replaces during a transition (#381).
 * Every route goes through here rather than `composable`.
 *
 * [paperSurface] anchors its grain to the window, so this sheet lands in register with the one
 * [com.jenarvaezg.coindex.ui.theme.CoindexTheme] paints behind everything (#351).
 */
private fun NavGraphBuilder.page(
    route: String,
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) = composable(route) { entry ->
    val arrival = this
    Box(modifier = Modifier.fillMaxSize().paperSurface()) { arrival.content(entry) }
}

/**
 * The bottom bar: Collections, Coins and «Las cifras» (ADR 0021 §1, amended by ADR 0026 §8). The
 * app opens in Collections; a launch screen asking which hierarchy to open was rejected as a tap
 * per launch to pick the same thing.
 *
 * Each cell counts what its destination is made of: cards, Numista types owned (#516) and grams.
 * Never money: an amount in a permanent bar is a ticker on show to anyone glancing at the phone
 * (#316).
 *
 * Drawn as a rule rather than Material's `NavigationBar`, whose elevation, ripple and icon slot the
 * notebook doesn't use.
 */
@Composable
private fun HierarchyBar(
    route: String?,
    collections: Int?,
    types: Int?,
    grams: Double?,
    onCross: (String) -> Unit,
) {
    Column {
        HorizontalDivider(thickness = 2.dp, color = Paper.ink)
        Row(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
            HierarchyCell(
                label = collectionsCellLabel(collections),
                selected = route == Routes.INDEX,
                onClick = { onCross(Routes.INDEX) },
                modifier = Modifier.weight(1f),
            )
            HierarchyCell(
                label = typesCellLabel(types),
                selected = route == Routes.COINS,
                onClick = { onCross(Routes.COINS) },
                modifier = Modifier.weight(1f),
            )
            HierarchyCell(
                label = figuresCellLabel(figuresCellCount(grams)),
                selected = route == Routes.FIGURES,
                onClick = { onCross(Routes.FIGURES) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HierarchyCell(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) Paper.paper else Paper.ink,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .background(if (selected) Paper.ink else Paper.paperDeep)
            // The current cell still takes the tap: a dead cell reads as a broken control.
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(vertical = 16.dp),
    )
}

/** Persistent, non-blocking notice in the top bar of every screen that a newer APK is out. */
@Composable
internal fun UpdateBanner(
    update: UpdateStatus.Available,
    updating: Boolean,
    onInstall: () -> Unit,
) {
    val notes = update.manifest.notes?.takeIf(String::isNotBlank)
    // Keyed on the note itself: a newer version's news arrives collapsed and unmeasured.
    var expanded by remember(notes) { mutableStateOf(false) }
    var truncated by remember(notes) { mutableStateOf(false) }
    val disclosure = updateNotesDisclosure(expanded, truncated)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Paper.paperDeep)
            .padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                // The whole block takes the tap, not just the hint. No hint means nothing is
                // hidden.
                .then(
                    if (disclosure.hint == null) {
                        Modifier
                    } else {
                        Modifier.clickable(role = Role.Button) { expanded = !expanded }
                    },
                )
                .padding(end = 12.dp),
        ) {
            Text(
                updateAvailableLabel(update.manifest.versionName),
                style = MaterialTheme.typography.labelMedium,
                color = Paper.rust,
            )
            notes?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Paper.muted,
                    maxLines = disclosure.maxLines,
                    overflow = TextOverflow.Ellipsis,
                    // Only the collapsed layout can report the overflow; once expanded there is
                    // none left to see, and reading it back would retract the hint.
                    onTextLayout = { layout ->
                        if (!expanded) truncated = layout.hasVisualOverflow
                    },
                )
                disclosure.hint?.let { hint ->
                    Text(
                        hint,
                        style = MaterialTheme.typography.labelMedium,
                        color = Paper.moss,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
        PrimaryAction(
            text = updateInstallLabel(updating),
            onClick = onInstall,
            enabled = !updating,
        )
    }
    HorizontalDivider(color = Paper.line)
}

/**
 * Everything the [Scaffold] stacks above the page, kept clear of the status bar.
 *
 * targetSdk 36 forces edge-to-edge, and `Scaffold` doesn't pad its `topBar` slot
 * (`contentWindowInsets` reaches the body only), so the inset is paid here once rather than by
 * whichever occupant comes first. When the roots dropped the masthead that used to pay it
 * (ADR 0026 §1), the update banner ended up under the clock (#356).
 */
@Composable
internal fun TopChrome(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.statusBarsPadding(),
        content = content,
    )
}

/**
 * The notebook's masthead. The status-bar inset is [TopChrome]'s.
 *
 * The right-hand slot holds at most one action: [onBack] away from a root, [onOpenPhone] on one.
 */
@Composable
private fun Masthead(
    subtitle: String,
    onBack: (() -> Unit)?,
    onOpenPhone: (() -> Unit)?,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(APP_NAME, style = MaterialTheme.typography.titleLarge)
            when {
                onBack != null -> CardAction(
                    text = BACK_LABEL,
                    onClick = onBack,
                    icon = { BackGlyph() },
                )
                onOpenPhone != null ->
                    CardAction(text = PHONE_LABEL, onClick = onOpenPhone)
            }
        }
        Text(
            subtitle,
            style = MaterialTheme.typography.labelMedium,
            color = Paper.muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
        )
        HorizontalDivider(thickness = 2.dp, color = Paper.ink)
    }
}

@Composable
private fun FatalError(message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(FATAL_HEADING, style = MaterialTheme.typography.headlineMedium)
        Text(
            FATAL_EXPLANATION,
            style = MaterialTheme.typography.bodyLarge,
            color = Paper.muted,
        )
        Text(message, style = MaterialTheme.typography.bodyMedium, color = Paper.rust)
    }
}
