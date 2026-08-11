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
import com.jenarvaezg.coindex.ui.NOTICES_ATTRIBUTIONS
import com.jenarvaezg.coindex.ui.installedVersionLabel
import com.jenarvaezg.coindex.ui.theme.Paper

private data class PackagedLicense(val heading: String, val asset: String)

private val packagedLicenses = listOf(
    PackagedLicense("Apache License 2.0", "licenses/apache-2.0.txt"),
    PackagedLicense("MIT · slf4j-api", "licenses/slf4j-mit.txt"),
    PackagedLicense("SIL Open Font License 1.1", "licenses/ofl-1.1.txt"),
)

@Composable
fun NoticesScreen(
    versionName: String,
    modifier: Modifier = Modifier,
) {
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
        // No eyebrow: the masthead already names this screen, and «Avisos y licencias» over a
        // page of licence text is the word said twice (§5). The installed version sits here
        // instead of the masthead (#410): one place an APK build needs to be identifiable.
        Text(
            installedVersionLabel(versionName),
            style = MaterialTheme.typography.bodyMedium,
            color = Paper.muted,
        )
        NOTICES_ATTRIBUTIONS.forEach { attribution ->
            Text(attribution, style = MaterialTheme.typography.bodyMedium)
        }
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
