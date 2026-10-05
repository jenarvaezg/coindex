package com.jenarvaezg.coindex.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.ui.API_KEY_FIELD_LABEL
import com.jenarvaezg.coindex.ui.CREDENTIALS_EXPLANATION
import com.jenarvaezg.coindex.ui.ONBOARDING_CREDENTIALS_SOURCE
import com.jenarvaezg.coindex.ui.ONBOARDING_EYEBROW
import com.jenarvaezg.coindex.ui.ONBOARDING_SAVE_ACTION
import com.jenarvaezg.coindex.ui.ONBOARDING_TITLE
import com.jenarvaezg.coindex.ui.USER_ID_FIELD_LABEL
import com.jenarvaezg.coindex.ui.components.Eyebrow
import com.jenarvaezg.coindex.ui.components.PrimaryAction
import com.jenarvaezg.coindex.ui.theme.Paper

/**
 * First launch: the collector enters their own Numista credentials (the app is local-first, with no
 * shared account).
 *
 * [validation] is the form's own message, separate from the snackbar, so dismissing one doesn't
 * erase the other.
 */
@Composable
fun OnboardingScreen(
    validation: String?,
    onSave: (apiKey: String, userId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var apiKey by remember { mutableStateOf("") }
    var userId by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            // Otherwise the keyboard hides the button and the note below the fields.
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Eyebrow(ONBOARDING_EYEBROW)
        Text(ONBOARDING_TITLE, style = MaterialTheme.typography.displayLarge)
        Text(
            CREDENTIALS_EXPLANATION,
            style = MaterialTheme.typography.bodyLarge,
            color = Paper.muted,
        )
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text(API_KEY_FIELD_LABEL) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
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
        // Disabled until both fields have something.
        PrimaryAction(
            text = ONBOARDING_SAVE_ACTION,
            onClick = { onSave(apiKey, userId) },
            enabled = apiKey.isNotBlank() && userId.isNotBlank(),
        )
        Text(
            ONBOARDING_CREDENTIALS_SOURCE,
            style = MaterialTheme.typography.bodyMedium,
            color = Paper.muted,
        )
    }
}
