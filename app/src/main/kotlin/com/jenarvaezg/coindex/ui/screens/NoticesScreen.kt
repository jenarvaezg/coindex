package com.jenarvaezg.coindex.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.ui.components.Eyebrow
import com.jenarvaezg.coindex.ui.theme.Paper

private data class PackagedLicense(val heading: String, val asset: String)

private val packagedLicenses = listOf(
    PackagedLicense("Apache License 2.0", "licenses/apache-2.0.txt"),
    PackagedLicense("MIT · slf4j-api", "licenses/slf4j-mit.txt"),
    PackagedLicense("SIL Open Font License 1.1", "licenses/ofl-1.1.txt"),
)

@Composable
fun NoticesScreen(modifier: Modifier = Modifier) {
    val assets = LocalContext.current.assets
    val licenseTexts = remember(assets) {
        packagedLicenses.map { license ->
            license to assets.open(license.asset).bufferedReader().use { it.readText() }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Eyebrow("Avisos y licencias")
        Text(
            "Fichas y fotografías: datos proporcionados por Numista (numista.com). " +
                "Cada pieza lleva su N#.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            "Software de terceros: Compose, AndroidX, Room, Ktor, OkHttp, Okio, Coil, ZXing " +
                "y kotlinx — Apache 2.0; slf4j-api — MIT.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            "Tipografías: Bitter y Barlow Condensed — SIL Open Font License 1.1.",
            style = MaterialTheme.typography.bodyMedium,
        )
        licenseTexts.forEach { (license, text) ->
            Text(
                license.heading,
                style = MaterialTheme.typography.titleMedium,
                color = Paper.rust,
            )
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
