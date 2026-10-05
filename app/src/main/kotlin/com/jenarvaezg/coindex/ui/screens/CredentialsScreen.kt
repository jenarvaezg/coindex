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
 * Edit or clear the Numista credentials after onboarding, so a wrong or expired key doesn't force
 * clearing the app's data. Signing out keeps the collection.
 *
 * One screen below «Este teléfono» (#521, shaped like ADR 0026 §14): it is rarely needed, and every
 * state that blames the key links here (ADR 0028 §6.1 and the sync refusals that name it).
 * «Cerrar sesión» lives here because it deletes these two values.
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
    // Masked by default (onboarding promises it is stored encrypted), but revealable to spot a
    // typo.
    var revealKey by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // No heading: the top bar already names the screen (ADR 0026 §5).
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
        // Not a `PrimaryAction`: the one filled action of this flow is «Sincronizar», one screen up
        // (docs/ux/p1-jul-2026.md §1, #422).
        CardAction(
            text = CREDENTIALS_SAVE_ACTION,
            onClick = { onSave(apiKey, userId) },
        )

        FieldCard(dashed = true, modifier = Modifier.fillMaxWidth()) {
            // No title: it would repeat the card's only button (ADR 0026 §5).
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
