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
import com.jenarvaezg.coindex.ui.API_KEY_FIELD_LABEL
import com.jenarvaezg.coindex.ui.CREDENTIALS_EXPLANATION
import com.jenarvaezg.coindex.ui.CREDENTIALS_SAVE_ACTION
import com.jenarvaezg.coindex.ui.SIGN_OUT_ACTION
import com.jenarvaezg.coindex.ui.SIGN_OUT_EXPLANATION
import com.jenarvaezg.coindex.ui.CredentialsValues
import com.jenarvaezg.coindex.ui.USER_ID_FIELD_LABEL
import com.jenarvaezg.coindex.ui.apiKeyRevealLabel
import com.jenarvaezg.coindex.ui.components.CardAction
import com.jenarvaezg.coindex.ui.components.FieldCard
import com.jenarvaezg.coindex.ui.theme.Paper

/**
 * The way back into the credentials once onboarding is done.
 *
 * Without it a mistyped or expired API key was a dead end: every sync failed with a 401 and the
 * only cure was clearing the app's data, taking the synced collection with it. Signing out is
 * offered here too, and keeps the collection.
 *
 * One screen down from «Este teléfono» since #521, in the shape ADR 0026 §14 wrote for the licence
 * notices: three words at the foot, one screen with everything inside. What justifies the nesting is
 * frequency — onboarding fills these two fields, and the only reason to come back is a key Numista
 * has started refusing — and what pays for it is that everything which blames the key opens this
 * screen: the two valuation states of ADR 0028 §6.1 and the three sync refusals that name it.
 *
 * `Cerrar sesión` comes down with the fields, because that is what it does: it deletes these two
 * values. At the foot of a maintenance page it was a destructive button on the screen the collector
 * opens to sync.
 */
@Composable
fun CredentialsScreen(
    values: CredentialsValues,
    validation: String?,
    onSave: (apiKey: String, userId: String) -> Unit,
    onSignOut: () -> Unit,
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
        // No heading: the masthead of this screen is the word this screen is about (§5).
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
        // Beside the fields it keeps. A `CardAction` and not a `PrimaryAction`: the one filled action
        // of this journey is «Sincronizar», one screen up (docs/ux/p1-jul-2026.md §1, #422).
        CardAction(
            text = CREDENTIALS_SAVE_ACTION,
            onClick = { onSave(apiKey, userId) },
        )

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
    }
}
