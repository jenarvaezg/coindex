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
import com.jenarvaezg.coindex.ui.API_KEY_FIELD_LABEL
import com.jenarvaezg.coindex.ui.CREDENTIALS_EXPLANATION
import com.jenarvaezg.coindex.ui.NOTICES_LABEL
import com.jenarvaezg.coindex.ui.PHOTO_CACHE_HEADING
import com.jenarvaezg.coindex.ui.SETTINGS_CREDENTIALS_HEADING
import com.jenarvaezg.coindex.ui.SETTINGS_SAVE_ACTION
import com.jenarvaezg.coindex.ui.SIGN_OUT_ACTION
import com.jenarvaezg.coindex.ui.SIGN_OUT_EXPLANATION
import com.jenarvaezg.coindex.ui.SettingsValues
import com.jenarvaezg.coindex.ui.USER_ID_FIELD_LABEL
import com.jenarvaezg.coindex.ui.VALUATION_HEADING
import com.jenarvaezg.coindex.ui.apiKeyRevealLabel
import com.jenarvaezg.coindex.ui.photoCacheLabel
import com.jenarvaezg.coindex.ui.valuationLabel
import com.jenarvaezg.coindex.ui.syncActionLabel
import com.jenarvaezg.coindex.ui.components.CardAction
import com.jenarvaezg.coindex.ui.components.FieldCard
import com.jenarvaezg.coindex.ui.components.PrimaryAction
import com.jenarvaezg.coindex.ui.theme.Paper

/**
 * The way back into the credentials once onboarding is done.
 *
 * Without it a mistyped or expired API key was a dead end: every sync failed with a 401 and the
 * only cure was clearing the app's data, taking the synced collection with it. Signing out is
 * offered separately, and keeps the collection.
 *
 * Reading order (#422): credentials first, «Guardar ajustes» beside the fields it keeps, then
 * «Sincronizar» as the one filled action on the screen — never before the credentials it needs,
 * and never as a bordered secondary lost between the prose and the form.
 */
@Composable
fun SettingsScreen(
    values: SettingsValues,
    photoCache: PhotoCacheStatus,
    /** How far the valuation pass has got, and why it is held if it is (ADR 0028 §6). */
    valuation: ValuationStatus,
    /**
     * What the marked casillas add to the month, or null while nothing is marked (ADR 0029 §5).
     *
     * It rides on the valuation's card because it is the same subject: that card is where the app says
     * what the pass costs, and a mark is the first thing that makes the figure the collector's own
     * decision. Absent and not zero — a phone with nothing marked has the fixed pass it always had.
     */
    wishSpend: String?,
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
        // No eyebrow: the masthead of this screen already says «Ajustes», and printing it twice
        // one line apart is the furniture §5 prices.
        Text(SETTINGS_CREDENTIALS_HEADING, style = MaterialTheme.typography.headlineMedium)
        Text(
            CREDENTIALS_EXPLANATION,
            style = MaterialTheme.typography.bodyMedium,
            color = Paper.muted,
        )

        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text(API_KEY_FIELD_LABEL) },
            singleLine = true,
            visualTransformation = if (revealKey) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            modifier = Modifier.fillMaxWidth(),
        )
        CardAction(
            text = apiKeyRevealLabel(revealKey),
            onClick = { revealKey = !revealKey },
        )
        OutlinedTextField(
            value = userId,
            onValueChange = { userId = it.filter(Char::isDigit) },
            label = { Text(USER_ID_FIELD_LABEL) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        validation?.let { text ->
            Text(text, style = MaterialTheme.typography.bodyMedium, color = Paper.rust)
        }
        // Beside the fields it keeps; CardAction so «Sincronizar» stays the one PrimaryAction
        // on the screen (docs/ux/p1-jul-2026.md §1, #422).
        CardAction(
            text = SETTINGS_SAVE_ACTION,
            onClick = { onSave(apiKey, userId) },
        )
        // After the credentials it needs, and filled: FieldGuide's level 1, not a CardAction
        // stranded between the explanation and the fields (#422).
        PrimaryAction(
            text = syncActionLabel(syncing),
            onClick = onSync,
            enabled = !syncing,
        )

        // The photographs are the one thing here that is not a setting: nothing on this card can
        // be pressed. It is the only place the background prefetch is allowed to speak (#191), and
        // it is here because «faltan 320 y están cayendo» and «faltan 320 porque estás con datos»
        // look identical from the outside and are not.
        FieldCard(modifier = Modifier.fillMaxWidth()) {
            Text(PHOTO_CACHE_HEADING, style = MaterialTheme.typography.titleMedium)
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
            Text(VALUATION_HEADING, style = MaterialTheme.typography.titleMedium)
            Text(
                valuationLabel(valuation),
                style = MaterialTheme.typography.bodyMedium,
                color = Paper.muted,
                modifier = Modifier.padding(top = 4.dp),
            )
            // The elastic half of the same budget (ADR 0029 §5): the line above says what the pass is
            // doing, and this one says what the collector's own marks add to it every month.
            wishSpend?.let { spend ->
                Text(
                    spend,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Paper.rust,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        FieldCard(dashed = true, modifier = Modifier.fillMaxWidth()) {
            // No title: the word is the button, and a card whose heading repeats its only
            // control says it twice (§5).
            Text(
                SIGN_OUT_EXPLANATION,
                style = MaterialTheme.typography.bodyMedium,
                color = Paper.muted,
                modifier = Modifier.padding(bottom = 10.dp),
            )
            CardAction(text = SIGN_OUT_ACTION, onClick = onSignOut)
        }

        CardAction(text = NOTICES_LABEL, onClick = onOpenNotices)
    }
}
