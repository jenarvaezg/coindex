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
import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.ui.components.Eyebrow
import com.jenarvaezg.coindex.ui.components.PrimaryAction
import com.jenarvaezg.coindex.ui.theme.Paper

/**
 * First launch: the collector's own Numista credentials.
 *
 * Each user spends their own API budget, which is what makes the app local-first and removes
 * the shared ledger the web version needed.
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
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Eyebrow("Cuaderno de colección")
        Text("Coindex", style = MaterialTheme.typography.displayLarge)
        Text(
            "Introduce tu API key de Numista y tu identificador de usuario. Se guardan " +
                "cifrados en este teléfono y nunca salen de él: cada colección consume su " +
                "propio presupuesto de la API.",
            style = MaterialTheme.typography.bodyLarge,
            color = Paper.muted,
        )
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("API key de Numista") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
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
        PrimaryAction(text = "Guardar y empezar", onClick = { onSave(apiKey, userId) })
        Text(
            "La API key se obtiene en numista.com › Mi perfil › API. El identificador de " +
                "usuario aparece en la URL de tu perfil.",
            style = MaterialTheme.typography.bodyMedium,
            color = Paper.muted,
        )
    }
}
