package com.jenarvaezg.coindex.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jenarvaezg.coindex.ui.screens.IndexScreen
import com.jenarvaezg.coindex.ui.screens.OnboardingScreen
import com.jenarvaezg.coindex.ui.screens.PlateScreen
import com.jenarvaezg.coindex.ui.screens.UnclassifiedScreen
import com.jenarvaezg.coindex.ui.theme.Paper

private object Routes {
    const val INDEX = "index"
    const val UNCLASSIFIED = "unclassified"
    const val PLATE = "plate/{catalogId}"

    fun plate(catalogId: String) = "plate/$catalogId"
}

@Composable
fun CoindexApp(viewModel: CoindexViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val snackbarHost = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(state.message) {
        state.message?.let { message ->
            snackbarHost.showSnackbar(message)
            viewModel.dismissMessage()
        }
    }

    val openUrl: (String) -> Unit = { url ->
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    Scaffold(
        containerColor = Paper.paper,
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = { Masthead(navController) },
    ) { padding ->
        val content = Modifier.fillMaxSize().padding(padding)
        when {
            state.fatalError != null -> FatalError(state.fatalError!!, content)
            !state.onboarded -> OnboardingScreen(
                message = state.message,
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
                        syncing = state.syncing,
                        catalogs = viewModel.catalogs,
                        onSync = viewModel::sync,
                        onOpenUnclassified = { navController.navigate(Routes.UNCLASSIFIED) },
                        onOpenPlate = { catalogId ->
                            navController.navigate(Routes.plate(catalogId))
                        },
                        onOpenSource = openUrl,
                        onDisposition = { proposal, disposition ->
                            viewModel.setDisposition(proposal.key(), disposition)
                        },
                    )
                }
                composable(Routes.UNCLASSIFIED) {
                    UnclassifiedScreen(state = state.collection, onOpenSource = openUrl)
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

@Composable
private fun Masthead(navController: NavHostController) {
    Column(modifier = Modifier.background(Paper.paper)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 12.dp, top = 14.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("COINDEX", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = { navController.popBackStack() }) {
                Text("Volver", style = MaterialTheme.typography.labelLarge, color = Paper.moss)
            }
        }
        Text(
            "Inventario de campo · plata bullion",
            style = MaterialTheme.typography.labelMedium,
            color = Paper.muted,
            modifier = Modifier.padding(start = 20.dp, bottom = 8.dp),
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
