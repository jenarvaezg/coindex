package com.jenarvaezg.coindex.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jenarvaezg.coindex.data.update.UpdateStatus
import com.jenarvaezg.coindex.ui.components.CardAction
import com.jenarvaezg.coindex.ui.components.PrimaryAction
import com.jenarvaezg.coindex.ui.screens.CoinsScreen
import com.jenarvaezg.coindex.ui.screens.IndexScreen
import com.jenarvaezg.coindex.ui.screens.OnboardingScreen
import com.jenarvaezg.coindex.ui.screens.MissingSubject
import com.jenarvaezg.coindex.ui.screens.PiecesScreen
import com.jenarvaezg.coindex.ui.screens.PlateScreen
import com.jenarvaezg.coindex.ui.screens.SettingsScreen
import com.jenarvaezg.coindex.ui.shelf.ownedTypeCount
import com.jenarvaezg.coindex.ui.theme.Paper

@Composable
fun CoindexApp(viewModel: CoindexViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val snackbarHost = remember { SnackbarHostState() }
    val context = LocalContext.current
    val backStackEntry by navController.currentBackStackEntryAsState()

    // Al volver a primer plano se recomprueba, con el suelo de tiempo de shouldCheckForUpdate.
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.checkForUpdate() }

    LaunchedEffect(state.message) {
        state.message?.let { message ->
            snackbarHost.showSnackbar(message)
            viewModel.dismissMessage()
        }
    }

    val openUrl: (String) -> Unit = { url ->
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
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
        containerColor = Paper.paper,
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            Column {
                Masthead(
                    subtitle = mastheadSubtitle(
                        screenTitle(route, subjectName),
                        state.versionName,
                    ),
                    onBack = onBack,
                    onOpenSettings = onOpenSettings,
                )
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
                    collections = state.collection.index.size,
                    // Read from the same place Coins draws its rows, so the bar cannot promise a
                    // number the screen behind it then contradicts.
                    coins = ownedTypeCount(state.collection),
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
            else -> NavHost(
                navController = navController,
                startDestination = Routes.INDEX,
                modifier = content,
            ) {
                composable(Routes.INDEX) {
                    IndexScreen(
                        state = state.collection,
                        budget = state.budget,
                        loading = state.loading,
                        syncing = state.syncing,
                        lastSync = state.lastSync,
                        shelf = state.indexShelf,
                        onNarrow = viewModel::narrowIndex,
                        onSync = viewModel::sync,
                        onOpen = { destination ->
                            navController.navigate(routeOf(destination))
                        },
                        notebook = viewModel::notebookPages,
                        onMessage = viewModel::showMessage,
                    )
                }
                composable(Routes.COINS) {
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
                    )
                }
                composable(Routes.OWN_GROUPING) { entry ->
                    val boxId = entry.arguments?.getString("groupingId")?.toLongOrNull()
                    val card = boxId?.let(state.collection::piecesCardForBox)
                    PiecesScreen(
                        state = state.collection,
                        subject = card?.let { piecesSubject(state.collection, it) },
                        onOpenSource = openUrl,
                        onMessage = viewModel::showMessage,
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
                composable(Routes.DERIVED_COLLECTION) { entry ->
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
                            "Ese enlace no describe ninguna variante de tu colección. Vuelve " +
                                "al índice.",
                            Modifier.fillMaxSize().padding(20.dp),
                        )
                    } else {
                        val card = state.collection.piecesCardFor(key)
                        // No upkeep: a derived collection is not something anyone typed.
                        PiecesScreen(
                            state = state.collection,
                            subject = card?.let { piecesSubject(state.collection, it) },
                            onOpenSource = openUrl,
                            onMessage = viewModel::showMessage,
                        )
                    }
                }
                composable(Routes.SETTINGS) {
                    // Read once per visit: the form owns its own edits from then on, and it
                    // opens on a clean slate rather than on the last visit's complaint.
                    val values = remember { viewModel.currentSettings() }
                    LaunchedEffect(Unit) { viewModel.clearValidation() }
                    SettingsScreen(
                        values = values,
                        budget = state.budget,
                        versionName = state.versionName,
                        validation = state.validation,
                        onSave = { apiKey, userId, budgetCap ->
                            if (viewModel.saveSettings(apiKey, userId, budgetCap)) {
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
                    )
                }
                composable(Routes.PLATE) { entry ->
                    val catalogId = entry.arguments?.getString("catalogId").orEmpty()
                    PlateScreen(
                        result = viewModel.plate(catalogId),
                        images = state.collection.images,
                        onOpenSource = openUrl,
                        onMessage = viewModel::showMessage,
                        modifier = Modifier.fillMaxSize().background(Paper.paper),
                    )
                }
            }
        }
    }
}

/**
 * The bottom bar of two destinations: Collections and Coins (ADR 0021 §1).
 *
 * **The app opens in Collections and this crosses to Coins and back.** A home screen that asked which
 * hierarchy you wanted was prototyped and rejected — it charged a tap per launch to choose the same
 * thing every time — and Coins is not a view inside a collection, so it could not be a button on the
 * index either.
 *
 * Each cell carries how many cards are behind it, which is the number the destination itself then
 * prints: 58 collections, and one row per Numista type owned. Drawn as two halves of a rule rather
 * than as Material's `NavigationBar`, which brings its own elevation, ripple and icon slot into a
 * notebook that has none of the three.
 */
@Composable
private fun HierarchyBar(
    route: String?,
    collections: Int,
    coins: Int,
    onCross: (String) -> Unit,
) {
    Column {
        HorizontalDivider(thickness = 2.dp, color = Paper.ink)
        Row(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
            HierarchyCell(
                label = "Colecciones · $collections",
                selected = route == Routes.INDEX,
                onClick = { onCross(Routes.INDEX) },
                modifier = Modifier.weight(1f),
            )
            HierarchyCell(
                label = "Monedas · $coins",
                selected = route == Routes.COINS,
                onClick = { onCross(Routes.COINS) },
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
private fun UpdateBanner(
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
                "NUEVA VERSIÓN ${update.manifest.versionName}",
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
            text = if (updating) "Descargando…" else "Instalar",
            onClick = onInstall,
            enabled = !updating,
        )
    }
    HorizontalDivider(color = Paper.line)
}

/**
 * The notebook's masthead, kept clear of the status bar.
 *
 * With targetSdk 36 the window is edge-to-edge and there is no way back: without
 * [statusBarsPadding] the title sits under the clock and the system bar swallows the taps
 * meant for «Volver». The paper background is painted before the padding so the inset strip
 * still reads as part of the page.
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
    Column(modifier = Modifier.background(Paper.paper).statusBarsPadding()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("COINDEX", style = MaterialTheme.typography.titleLarge)
            when {
                onBack != null -> CardAction(text = "← Volver", onClick = onBack)
                onOpenSettings != null -> CardAction(text = "Ajustes", onClick = onOpenSettings)
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
        Text("No se pudo arrancar", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Los datos curados que viajan con la app no son válidos, así que Coindex se " +
                "detiene en lugar de mostrarte una lámina incorrecta.",
            style = MaterialTheme.typography.bodyLarge,
            color = Paper.muted,
        )
        Text(message, style = MaterialTheme.typography.bodyMedium, color = Paper.rust)
    }
}
