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
import com.jenarvaezg.coindex.domain.collectionProposalFamilyLabel
import com.jenarvaezg.coindex.ui.components.CardAction
import com.jenarvaezg.coindex.ui.components.PrimaryAction
import com.jenarvaezg.coindex.ui.screens.IndexScreen
import com.jenarvaezg.coindex.ui.screens.OnboardingScreen
import com.jenarvaezg.coindex.ui.screens.OwnGroupingScreen
import com.jenarvaezg.coindex.ui.screens.PlateScreen
import com.jenarvaezg.coindex.ui.screens.PrototypeFirstLevel
import com.jenarvaezg.coindex.ui.screens.ProposalScreen
import com.jenarvaezg.coindex.ui.screens.SettingsScreen
import com.jenarvaezg.coindex.ui.screens.UnclassifiedScreen
import com.jenarvaezg.coindex.ui.theme.Paper

/** PROTOTIPO #18: a `false` la app vuelve a ser ella misma. Se borra con la rama. */
private const val PROTOTYPE_FIRST_LEVEL = true

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

    // The plate and the proposal name themselves in the masthead, which means reading what the
    // route carries; every other destination knows its own title from the route alone.
    val route = backStackEntry?.destination?.route
    val subjectName = when {
        Routes.isPlate(route) ->
            viewModel.catalogName(backStackEntry?.arguments?.getString("catalogId"))
        Routes.isProposal(route) -> backStackEntry
            ?.arguments
            ?.getString("family")
            ?.let(::collectionProposalFamilyLabel)
        Routes.isOwnGrouping(route) -> backStackEntry
            ?.arguments
            ?.getString("groupingId")
            ?.toLongOrNull()
            ?.let { id -> state.collection.ownGroupings.firstOrNull { it.id == id }?.name }
        else -> null
    }

    // The start destination is the only one with nothing underneath it, and a «Volver» that pops
    // an empty back stack is a button that teaches you to ignore it. In that gap the masthead
    // offers settings instead. Onboarding has no masthead actions at all.
    val atIndex = state.onboarded && route == Routes.INDEX
    val onBack: (() -> Unit)? =
        if (state.onboarded && route != null && route != Routes.INDEX) {
            { navController.popBackStack() }
        } else {
            null
        }
    val onOpenSettings: (() -> Unit)? =
        if (atIndex) {
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
    ) { padding ->
        val content = Modifier.fillMaxSize().padding(padding)
        when {
            // PROTOTIPO #18 — el primer nivel entero lo sirve PrototypeFirstLevel, con su propia
            // barra de variantes. Rama desechable: esta línea se va con ella.
            PROTOTYPE_FIRST_LEVEL -> PrototypeFirstLevel(content)
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
                        catalogs = viewModel.catalogs,
                        onSync = viewModel::sync,
                        onOpenUnclassified = { navController.navigate(Routes.UNCLASSIFIED) },
                        onOpenProposal = { proposal ->
                            navController.navigate(Routes.proposal(proposal.key()))
                        },
                        onOpenOwnGrouping = { groupingId ->
                            navController.navigate(Routes.ownGrouping(groupingId))
                        },
                        onOpenPlate = { catalogId ->
                            navController.navigate(Routes.plate(catalogId))
                        },
                        onDisposition = { proposal, disposition ->
                            viewModel.setDisposition(proposal.key(), disposition)
                        },
                    )
                }
                composable(Routes.OWN_GROUPING) { entry ->
                    val groupingId = entry.arguments?.getString("groupingId")?.toLongOrNull()
                    val grouping = state.collection.ownGroupings
                        .firstOrNull { it.id == groupingId }
                    OwnGroupingScreen(
                        state = state.collection,
                        grouping = grouping,
                        onOpenSource = openUrl,
                        onRename = { name ->
                            grouping?.let { viewModel.renameOwnGrouping(it.id, name) }
                        },
                        onRemoveType = { typeId ->
                            grouping?.let { viewModel.removeFromOwnGrouping(it.id, typeId) }
                        },
                        // Undoing it leaves nothing to look at, so the screen goes with it.
                        onDelete = {
                            grouping?.let { viewModel.deleteOwnGrouping(it.id) }
                            navController.popBackStack()
                        },
                    )
                }
                composable(Routes.PROPOSAL) { entry ->
                    val key = proposalKeyFromRoute(
                        family = entry.arguments?.getString("family"),
                        weight = entry.arguments?.getString("weight"),
                        finish = entry.arguments?.getString("finish"),
                        metal = entry.arguments?.getString("metal"),
                    )
                    // A route that does not describe a canonical key is not guessed at; it is
                    // the same refusal a stored disposition gets when its parts drift.
                    if (key == null) {
                        UnknownProposal(Modifier.fillMaxSize().padding(20.dp))
                    } else {
                        ProposalScreen(
                            state = state.collection,
                            key = key,
                            catalog = viewModel.catalogFor(key),
                            plate = viewModel.plateFor(key),
                            onOpenPlate = { catalogId ->
                                navController.navigate(Routes.plate(catalogId))
                            },
                            onOpenSource = openUrl,
                            onDisposition = viewModel::setDisposition,
                            onCreateGrouping = viewModel::createOwnGrouping,
                            onAddToGrouping = viewModel::addToOwnGrouping,
                        )
                    }
                }
                composable(Routes.UNCLASSIFIED) {
                    UnclassifiedScreen(
                        state = state.collection,
                        onOpenSource = openUrl,
                        onCreateGrouping = viewModel::createOwnGrouping,
                        onAddToGrouping = viewModel::addToOwnGrouping,
                    )
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
 * Persistent notice that a newer APK is published.
 *
 * It lives in the top bar rather than among the proposals so it is visible on every screen,
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

/** A proposal route that describes no canonical variant key: said plainly, never guessed at. */
@Composable
private fun UnknownProposal(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Propuesta desconocida", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Ese enlace no describe ninguna variante de tu colección. Vuelve al índice.",
            style = MaterialTheme.typography.bodyLarge,
            color = Paper.muted,
        )
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
