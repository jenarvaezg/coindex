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
import com.jenarvaezg.coindex.data.PlateResult
import com.jenarvaezg.coindex.data.update.UpdateStatus
import com.jenarvaezg.coindex.ui.APP_NAME
import com.jenarvaezg.coindex.ui.components.BackGlyph
import com.jenarvaezg.coindex.ui.components.CardAction
import com.jenarvaezg.coindex.ui.components.FichaRefresh
import com.jenarvaezg.coindex.ui.components.LocalNavAnimation
import com.jenarvaezg.coindex.ui.components.LocalSharedTransition
import com.jenarvaezg.coindex.ui.components.PrimaryAction
import com.jenarvaezg.coindex.ui.components.paperSurface
import com.jenarvaezg.coindex.ui.screens.CoinsScreen
import com.jenarvaezg.coindex.ui.screens.FiguresScreen
import com.jenarvaezg.coindex.ui.screens.IndexScreen
import com.jenarvaezg.coindex.ui.screens.OnboardingScreen
import com.jenarvaezg.coindex.ui.screens.MissingSubject
import com.jenarvaezg.coindex.ui.screens.NoticesScreen
import com.jenarvaezg.coindex.ui.screens.PiecesScreen
import com.jenarvaezg.coindex.ui.screens.PlateScreen
import com.jenarvaezg.coindex.ui.screens.SettingsScreen
import com.jenarvaezg.coindex.ui.shelf.CoinsShelf
import com.jenarvaezg.coindex.ui.shelf.NotebookAxis
import com.jenarvaezg.coindex.ui.shelf.YearFilter
import com.jenarvaezg.coindex.ui.theme.Paper

@Composable
fun CoindexApp(viewModel: CoindexViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val snackbarHost = remember { SnackbarHostState() }
    val context = LocalContext.current
    val backStackEntry by navController.currentBackStackEntryAsState()

    // Al volver a primer plano se recomprueba, con el suelo de tiempo de shouldCheckForUpdate.
    // Y es también cuando se reintentan las fotos que faltan: puede que ahora haya wifi (#191).
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        viewModel.checkForUpdate()
        viewModel.retryPhotoPrefetch()
    }

    LaunchedEffect(state.message) {
        state.message?.let { notice ->
            // Offer Abrir only when something can open the file — a button that crashes is worse
            // than no button (#436). The try/catch in openDownloadedFile still covers the race
            // where the viewer vanishes between the check and the tap.
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

    val openUrl: (String) -> Unit = { url ->
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    // Built here, once, for the three surfaces that print money — the ficha of a coin, the header of
    // its plate and «Las cifras» — so none of them can disagree with another about one coin. The
    // market has to have landed: while it has not, `settled` is false and there is no amount to give
    // anybody (ADR 0028 §7).
    val coinValue: (Int) -> CoinValue? = { typeId ->
        if (!state.valuation.settled) {
            null
        } else {
            coinValue(typeId, state.collection, state.prices.spot, state.prices::of)
        }
    }
    // The plate asks for three readings at once and gets them or gets none of them (#493): the value
    // of what is in it, the cost of closing it and the price inside each hole are one walk of the same
    // album, and while the market is still arriving the empty value is what withdraws all three.
    val plateMoney: (PlateResult.Available) -> PlateMoney = { resolved ->
        if (!state.valuation.settled) {
            PlateMoney()
        } else {
            plateMoney(
                resolved.album,
                state.collection,
                state.prices.listings,
                state.prices.spot,
                state.prices::of,
            )
        }
    }

    // Built here, once, for the two surfaces that show a piece of a type (#185): both read the same
    // cache date and the same in-flight set, so the two cards can never disagree about how old a
    // ficha is or whether it is already being refreshed.
    val ficha: (Int) -> FichaRefresh = { typeId ->
        FichaRefresh(
            fetchedAt = state.collection.fichaFetchedAt[typeId],
            refreshing = typeId in state.refreshingFichas,
            onRefresh = { viewModel.refreshFicha(typeId) },
        )
    }

    // The plate and the collection name themselves in the masthead, which means reading what the
    // route carries; every other destination knows its own title from the route alone.
    val route = backStackEntry?.destination?.route
    val subjectName = when {
        Routes.isPlate(route) ->
            viewModel.catalogName(backStackEntry?.arguments?.getString("catalogId"))
        // The whole key, not just the family: three Britannias share one and only the key
        // tells them apart (#22).
        Routes.isDerivedCollection(route) -> variantKeyFromRoute(
            family = backStackEntry?.arguments?.getString("family"),
            weight = backStackEntry?.arguments?.getString("weight"),
            finish = backStackEntry?.arguments?.getString("finish"),
            metal = backStackEntry?.arguments?.getString("metal"),
        )?.let(viewModel.titles::of)
        Routes.isOwnGrouping(route) -> backStackEntry
            ?.arguments
            ?.getString("groupingId")
            ?.toLongOrNull()
            ?.let { id -> state.collection.ownGroupings.firstOrNull { it.id == id }?.name }
        else -> null
    }

    // A root destination has nothing underneath it, and a «Volver» that pops an empty back stack is
    // a button that teaches you to ignore it. In that gap the masthead offers settings instead —
    // from **both** roots now (ADR 0021 §1), because neither is more the home than the other.
    // Onboarding has no masthead actions at all.
    val atRoot = state.onboarded && Routes.isRoot(route)

    // Assembled once per collection and per price book, and never per recomposition: it walks the whole
    // inventory, and the bottom bar reads its weight on every screen the bar is drawn on.
    val figures = remember(state.collection, state.prices, state.valuation.settled) {
        figuresSubject(
            state = state.collection,
            spot = state.prices.spot,
            prices = state.prices::of,
            settled = state.valuation.settled,
        )
    }
    // One census for the sewn edge of every root (#400): collections from the index, pieces and
    // types from the same figures walk «La materia» already uses — so the three tabs cannot invent
    // three totals, and the HierarchyBar's type count is the same number. Absent while still
    // reading (#418): zeros here would claim the collection is empty before the snapshot lands.
    val sewnEdge = if (state.loading) {
        null
    } else {
        SewnEdgeCounts(
            collections = state.collection.index.size,
            pieces = figures.figures.pieces,
            types = figures.figures.types,
        )
    }
    val onBack: (() -> Unit)? =
        if (state.onboarded && route != null && !Routes.isRoot(route)) {
            { navController.popBackStack() }
        } else {
            null
        }
    val onOpenSettings: (() -> Unit)? =
        if (atRoot) {
            { navController.navigate(Routes.SETTINGS) }
        } else {
            null
        }

    Scaffold(
        // Transparent so the sheet [CoindexTheme] paints reaches the strip behind the status bar:
        // painting paper here left that strip plain wherever the top bar was empty, and a grain
        // that stops at the clock is two papers again (#351).
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopChrome {
                // Both album roots own their chrome: the sewn edge is their shared masthead.
                // Keeping the generic one above either would print COINDEX and Settings twice and
                // spend the space their die-cut grids just recovered (ADR 0026 §1, §13).
                if (!Routes.ownsChrome(route)) {
                    Masthead(
                        // The installed version lives in Avisos y licencias (#410): printing it on
                        // every interior masthead was permanent furniture the collector does not need.
                        subtitle = screenTitle(route, subjectName),
                        onBack = onBack,
                        onOpenSettings = onOpenSettings,
                    )
                }
                (state.update as? UpdateStatus.Available)?.let { available ->
                    UpdateBanner(available, state.updating, viewModel::installUpdate)
                }
            }
        },
        bottomBar = {
            // Only on the two roots. Everything else is reached *through* one of them, and a bar
            // offering to jump hierarchies from three screens deep would be a second «Volver» that
            // does something else.
            if (atRoot) {
                HierarchyBar(
                    route = route,
                    collections = sewnEdge?.collections,
                    // Same type count the sewn edge prints, and the same distinct set [coinRows]
                    // draws — including a hostile zero coerced to one piece (#426).
                    coins = sewnEdge?.types,
                    // Grams, and never money (#316): an amount in a permanent bar is a pocket ticker.
                    // Null with the sewn edge (#418): «0,00 kg» while reading is a false empty collection.
                    grams = sewnEdge?.let { figures.figures.weight.value },
                    onCross = { destination ->
                        navController.navigate(destination) {
                            // The two roots are siblings, not a stack: crossing over and back must
                            // not pile up entries, and each side keeps its own scroll position.
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
                onSave = viewModel::saveCredentials,
                modifier = content,
            )
            // The journey of ADR 0026 §3 needs one layout over both ends of it, and the NavHost is
            // the only thing in the app that is on both sides of a navigation. What actually flies
            // is decided far from here — `Modifier.travellingCoin` on the two die-cut holes — so
            // the host provides the scope and knows nothing about coins.
            //
            // **Only the leaf on top is animated, and which one that is changes direction with
            // the journey.** Both ends of a navigation are composed together for as long as the
            // coin is in the air, so what is seen in that half second is decided here and nowhere
            // else. Two attempts got it wrong for the same reason: a destination painted no paper
            // of its own, so a stack of two of them was a stack of two transparencies. Compose's
            // crossfade let the paper show through both and the casillas washed out mid-flight,
            // then snapped opaque on landing; `None` on all four (#377) faded nothing and drew the
            // plate whole over the whole index, a double exposure for half of every journey (#381).
            // `page` is what fixes both: an opaque destination can simply cover the one it
            // replaces, and then the only question left is who covers whom.
            //
            // The NavHost stacks by depth, so going in, the plate arrives on top — it needs no
            // transition, it just covers the index, and the index below it needs none either
            // because nothing of it is left to see. Coming back the same stacking works against
            // us: the plate is *still* on top while it leaves, so with nothing of its own it sits
            // there opaque for the whole flight home and then vanishes in one frame. That is the
            // snap of #370 arriving from the other side, and it is why the return — and only the
            // return — is given a fade out. Short: the index is uncovered early and the coin lands
            // on a sheet that has been settled for most of its flight.
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
                                catalogs = viewModel.catalogs,
                                onNarrow = viewModel::narrowIndex,
                                onOpen = { destination ->
                                    navController.navigate(routeOf(destination))
                                },
                                onOpenCoins = { coinsShelf ->
                                    crossToCoins(navController, viewModel, coinsShelf)
                                },
                                sewnEdge = sewnEdge,
                                onSettings = { navController.navigate(Routes.SETTINGS) },
                                notebookOptions = state.notebookOptions,
                                onNotebookPrinted = viewModel::notebookPrinted,
                                notebook = viewModel::notebookPages,
                                onMessage = viewModel::showMessage,
                                onExporting = viewModel::notebookExporting,
                            )
                        }
                    }
                    page(Routes.COINS) {
                        CoinsScreen(
                            state = state.collection,
                            shelf = state.coinsShelf,
                            curatedNames = viewModel.curatedNames,
                            onNarrow = viewModel::narrowCoins,
                            onOpen = { destination ->
                                navController.navigate(routeOf(destination))
                            },
                            onCreateBox = viewModel::createOwnGrouping,
                            onAddToBox = viewModel::addToOwnGrouping,
                            onOpenSource = openUrl,
                            sewnEdge = sewnEdge,
                            onSettings = { navController.navigate(Routes.SETTINGS) },
                            ficha = ficha,
                            value = coinValue,
                        )
                    }
                    page(Routes.FIGURES) {
                        FiguresScreen(
                            subject = figures,
                            sewnEdge = sewnEdge,
                            // Read once per composition of the page: what it dates is the spot, and a
                            // clock that ticked would make «plata de hoy» a thing that changes while
                            // you look at it, which is the pocket ticker #316 refused.
                            nowMillis = remember(state.prices.spot) { System.currentTimeMillis() },
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
                            onSettings = { navController.navigate(Routes.SETTINGS) },
                        )
                    }
                    page(Routes.OWN_GROUPING) { entry ->
                        val boxId = entry.arguments?.getString("groupingId")?.toLongOrNull()
                        val card = boxId?.let(state.collection::piecesCardForBox)
                        PiecesScreen(
                            state = state.collection,
                            subject = card?.let { piecesSubject(state.collection, it) },
                            onOpenSource = openUrl,
                            onMessage = viewModel::showMessage,
                            ficha = ficha,
                            notebookOptions = state.notebookOptions,
                            onNotebookPrinted = viewModel::notebookPrinted,
                            notebookPages = { options ->
                                card?.let { viewModel.notebookPagesForCard(it, options) }
                                    ?: emptyList()
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
                                    // Undoing it leaves nothing to look at, so the screen goes too.
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
                        // A route that does not describe a canonical key is not guessed at: the key
                        // is the identity of the cards no curated file names (ADR 0021 §5), and half
                        // a key names none of them.
                        if (key == null) {
                            MissingSubject(
                                UNKNOWN_VARIANT_LINK,
                                Modifier.fillMaxSize().padding(20.dp),
                            )
                        } else {
                            val card = state.collection.piecesCardFor(key)
                            // No upkeep: a derived collection is not something anyone typed. What
                            // it says when it is gone is `PiecesScreen`'s own default, which is the
                            // one wording of that event (ADR 0026 §5).
                            PiecesScreen(
                                state = state.collection,
                                subject = card?.let { piecesSubject(state.collection, it) },
                                onOpenSource = openUrl,
                                onMessage = viewModel::showMessage,
                                ficha = ficha,
                                notebookOptions = state.notebookOptions,
                                onNotebookPrinted = viewModel::notebookPrinted,
                                notebookPages = { options ->
                                    card?.let { viewModel.notebookPagesForCard(it, options) }
                                        ?: emptyList()
                                },
                                onExporting = viewModel::notebookExporting,
                            )
                        }
                    }
                    page(Routes.SETTINGS) {
                        // Read once per visit: the form owns its own edits from then on, and it
                        // opens on a clean slate rather than on the last visit's complaint.
                        val values = remember { viewModel.currentSettings() }
                        LaunchedEffect(Unit) { viewModel.clearValidation() }
                        SettingsScreen(
                            values = values,
                            photoCache = state.photoCache,
                            valuation = state.valuation,
                            syncing = state.syncing,
                            validation = state.validation,
                            onSave = { apiKey, userId ->
                                if (viewModel.saveSettings(apiKey, userId)) {
                                    navController.popBackStack()
                                }
                            },
                            // Popped before the state flips: the NavHost leaves composition on
                            // sign-out, but the controller outlives it, and a surviving «settings»
                            // entry would make the masthead say «Ajustes» over the onboarding form
                            // and drop the collector back into settings once they sign in again.
                            onSignOut = {
                                navController.popBackStack(Routes.INDEX, inclusive = false)
                                viewModel.signOut()
                            },
                            onSync = viewModel::sync,
                            onOpenNotices = { navController.navigate(Routes.NOTICES) },
                        )
                    }
                    page(Routes.NOTICES) {
                        NoticesScreen(versionName = state.versionName)
                    }
                    page(Routes.PLATE) { entry ->
                        val catalogId = entry.arguments?.getString("catalogId").orEmpty()
                        Travelling(this) {
                            PlateScreen(
                                // Resolved once per collection and not once per recomposition (#218):
                                // building the album walks the whole inventory, and the screen recomposes
                                // for reasons — a scroll, an export in flight — that leave it unchanged.
                                result = remember(state.collection, catalogId) {
                                    viewModel.plate(catalogId)
                                },
                                images = state.collection.images,
                                money = plateMoney,
                                notebookOptions = state.notebookOptions,
                                onNotebookPrinted = viewModel::notebookPrinted,
                                notebookPages = { options ->
                                    viewModel.notebookPagesForPlate(catalogId, options)
                                },
                                onExporting = viewModel::notebookExporting,
                                onOpenSource = openUrl,
                                onMessage = viewModel::showMessage,
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
 * Crosses to Coins with the shelf already narrowed.
 *
 * The one gesture three surfaces share: the country and year axes of the index (#386) and now every
 * touchable figure of «Las cifras». Sibling roots are not a stack, so crossing must not pile up entries
 * and each side keeps its own scroll position.
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
 * The one shared-element layout of the app, over both ends of every navigation.
 *
 * It is here and not around a screen because a shared element is a promise about two screens: the
 * hole of a card and the hole of a casilla are the same object seen twice (#300), and the layout is
 * what lets Compose believe it.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun TravelLayout(modifier: Modifier, content: @Composable () -> Unit) {
    SharedTransitionLayout(modifier = modifier) {
        CompositionLocalProvider(LocalSharedTransition provides this, content = content)
    }
}

/** Hands one destination its own arrival, which is the half of a journey a screen can see. */
@Composable
private fun Travelling(scope: AnimatedVisibilityScope, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalNavAnimation provides scope, content = content)
}

/** How long the leaf on top takes to lift off the one underneath, on the way back. */
private const val LIFT_MS = 180

/**
 * A destination that is a leaf of paper: opaque, so it can be laid over the one it replaces.
 *
 * Every route goes through here rather than through `composable` directly, because being opaque is
 * not a property of one screen — it is what makes a transition possible at all. Two destinations are
 * composed together for as long as a navigation lasts, and while they were transparent there was no
 * honest way to cross between them: the paper showed through both of them at once (#381).
 *
 * This does not take the sheet back to the days of #351, when the grain lived on two screens and
 * stopped at the edge of the third. [paperSurface] anchors its mosaic to the window and not to the
 * surface, so this leaf falls exactly on top of the one [com.jenarvaezg.coindex.ui.theme.CoindexTheme]
 * paints behind everything — same tone, same fibre, in register. It is still one sheet; there are
 * simply no gaps in it now, which is the case that anchoring was for.
 */
private fun NavGraphBuilder.page(
    route: String,
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) = composable(route) { entry ->
    val arrival = this
    Box(modifier = Modifier.fillMaxSize().paperSurface()) { arrival.content(entry) }
}

/**
 * The bottom bar of three destinations: Collections, Coins and «Las cifras» (ADR 0021 §1, amended by
 * ADR 0026 §8).
 *
 * **The app still opens in Collections and the third is last.** A home screen that asked which hierarchy
 * you wanted was prototyped and rejected — it charged a tap per launch to choose the same thing every
 * time — and with three cells that argument is stronger, not weaker.
 *
 * **Each cell names its grain with its count**, and the count is what the destination is *made of*
 * rather than how many things are inside it: cards, Numista types owned, and grams. «Las cifras» counts
 * weight and **never money** — an amount in a permanent bar is a pocket ticker that changes on its own
 * and puts the collector's estate in front of anyone glancing at the phone (#316).
 *
 * Drawn as three parts of a rule rather than as Material's `NavigationBar`, which brings its own
 * elevation, ripple and icon slot into a notebook that has none of the three.
 */
@Composable
private fun HierarchyBar(
    route: String?,
    collections: Int?,
    coins: Int?,
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
                label = coinsCellLabel(coins),
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
            // The cell you are already on still takes the tap: a dead half of the bar reads as a
            // control that has stopped working.
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(vertical = 16.dp),
    )
}

/**
 * Persistent notice that a newer APK is published.
 *
 * It lives in the top bar rather than among the cards so it is visible on every screen,
 * and it does not block: a pending update is not a reason to stop looking at the collection.
 */
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
                // The whole block takes the tap, not just the hint: a two-line strip is a small
                // enough target already. No hint means nothing is hidden, so nothing to open.
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
 * With targetSdk 36 the window is edge-to-edge and there is no way back: without
 * [statusBarsPadding] whatever comes first sits under the clock and the system bar swallows its
 * taps. `Scaffold` does not pad its `topBar` slot — `contentWindowInsets` reaches the body only —
 * so the strip is paid here, once, rather than by whoever happens to be first: the masthead used
 * to pay it for both, and when the album roots dropped it (ADR 0026 §1) the update banner was
 * left drawing under the clock with «Instalar» sharing the strip with the system icons (#356).
 *
 * The paper background is painted before the padding so the inset strip still reads as part of
 * the page, and not as a loose band of whatever colour the first occupant happens to use.
 */
@Composable
internal fun TopChrome(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.statusBarsPadding(),
        content = content,
    )
}

/**
 * The notebook's masthead.
 *
 * The status-bar inset is [TopChrome]'s, not this composable's: it is one of several occupants
 * of the top bar and only one of them may pay the strip.
 *
 * The right-hand slot holds at most one action, and only when it does something: [onBack] away
 * from the start destination, [onOpenSettings] on it.
 */
@Composable
private fun Masthead(
    subtitle: String,
    onBack: (() -> Unit)?,
    onOpenSettings: (() -> Unit)?,
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
                onOpenSettings != null ->
                    CardAction(text = SETTINGS_LABEL, onClick = onOpenSettings)
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
