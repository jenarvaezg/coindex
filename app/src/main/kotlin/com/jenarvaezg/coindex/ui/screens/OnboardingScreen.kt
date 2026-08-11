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
 * First launch: the collector's own Numista credentials.
 *
 * Each user supplies their own API credentials, which is what makes the app local-first and
 * removes the shared account the web version needed.
 *
 * [validation] is the form's own channel, not the snackbar's: the two used to share one field,
 * so dismissing the snackbar also erased the text explaining what was wrong with the form.
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
            // The keyboard covers the last third of this form: without it the button and the
            // note explaining where the two values come from sit behind the keys typing them.
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
        // Pressing it with an empty form could only produce the complaint the form can already
        // see coming, so it waits until there is something to save.
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
