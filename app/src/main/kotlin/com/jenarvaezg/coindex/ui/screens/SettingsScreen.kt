package com.jenarvaezg.coindex.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.data.photos.PhotoCacheStatus
import com.jenarvaezg.coindex.data.prices.ValuationStatus
import com.jenarvaezg.coindex.ui.SETTINGS_CREDENTIALS_HEADING
import com.jenarvaezg.coindex.ui.SettingsValues
import com.jenarvaezg.coindex.ui.photoCacheLabel
import com.jenarvaezg.coindex.ui.valuationLabel
import com.jenarvaezg.coindex.ui.syncActionLabel
import com.jenarvaezg.coindex.ui.components.CardAction
import com.jenarvaezg.coindex.ui.components.Eyebrow
import com.jenarvaezg.coindex.ui.components.FieldCard
import com.jenarvaezg.coindex.ui.components.PrimaryAction
import com.jenarvaezg.coindex.ui.theme.Paper

/**
 * The way back into the credentials once onboarding is done.
 *
 * Without it a mistyped or expired API key was a dead end: every sync failed with a 401 and the
 * only cure was clearing the app's data, taking the synced collection with it. Signing out is
 * offered separately, and keeps the collection.
 */
@Composable
fun SettingsScreen(
    values: SettingsValues,
    photoCache: PhotoCacheStatus,
    /** How far the valuation pass has got, and why it is held if it is (ADR 0028 §6). */
    valuation: ValuationStatus,
    syncing: Boolean,
    validation: String?,
    onSave: (apiKey: String, userId: String) -> Unit,
    onSignOut: () -> Unit,
    onSync: () -> Unit,
    onOpenNotices: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var apiKey by remember(values) { mutableStateOf(values.apiKey) }
    var userId by remember(values) { mutableStateOf(values.userId) }
    // The onboarding copy promises the key is stored encrypted, so it is masked here by
    // default; it is also the one field a collector needs to read back to spot a typo.
    var revealKey by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Eyebrow("Ajustes")
        Text(SETTINGS_CREDENTIALS_HEADING, style = MaterialTheme.typography.headlineMedium)
        Text(
            "Se guardan cifradas en este teléfono y nunca salen de él. Si Numista rechaza tus " +
                "sincronizaciones, la API key es lo primero que hay que revisar aquí.",
            style = MaterialTheme.typography.bodyMedium,
            color = Paper.muted,
        )
        CardAction(
            text = syncActionLabel(syncing),
            onClick = onSync,
            enabled = !syncing,
        )

        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("API key de Numista") },
            singleLine = true,
            visualTransformation = if (revealKey) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            modifier = Modifier.fillMaxWidth(),
        )
        CardAction(
            text = if (revealKey) "Ocultar la API key" else "Mostrar la API key",
            onClick = { revealKey = !revealKey },
        )
        OutlinedTextField(
            value = userId,
            onValueChange = { userId = it.filter(Char::isDigit) },
            label = { Text("Identificador de usuario") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        validation?.let { text ->
            Text(text, style = MaterialTheme.typography.bodyMedium, color = Paper.rust)
        }
        PrimaryAction(
            text = "Guardar ajustes",
            onClick = { onSave(apiKey, userId) },
        )

        // The photographs are the one thing here that is not a setting: nothing on this card can
        // be pressed. It is the only place the background prefetch is allowed to speak (#191), and
        // it is here because «faltan 320 y están cayendo» and «faltan 320 porque estás con datos»
        // look identical from the outside and are not.
        FieldCard(modifier = Modifier.fillMaxWidth()) {
            Text("Fotos del catálogo", style = MaterialTheme.typography.titleMedium)
            Text(
                photoCacheLabel(photoCache),
                style = MaterialTheme.typography.bodyMedium,
                color = Paper.muted,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        // The pass is silent everywhere else, and this is the one line it is allowed (ADR 0028 §6):
        // «Las cifras» with no money section looks the same whether the prices are on their way or
        // the month's allowance is gone, and only one of the two is worth waiting for.
        FieldCard(modifier = Modifier.fillMaxWidth()) {
            Text("Precios de catálogo", style = MaterialTheme.typography.titleMedium)
            Text(
                valuationLabel(valuation),
                style = MaterialTheme.typography.bodyMedium,
                color = Paper.muted,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        FieldCard(dashed = true, modifier = Modifier.fillMaxWidth()) {
            Text("Cerrar sesión", style = MaterialTheme.typography.titleMedium)
            Text(
                "Borra la API key y el identificador de este teléfono y vuelve al alta. Las " +
                    "piezas ya sincronizadas se quedan donde están.",
                style = MaterialTheme.typography.bodyMedium,
                color = Paper.muted,
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
            )
            CardAction(text = "Cerrar sesión", onClick = onSignOut)
        }

        CardAction(text = "Avisos y licencias", onClick = onOpenNotices)
    }
}
