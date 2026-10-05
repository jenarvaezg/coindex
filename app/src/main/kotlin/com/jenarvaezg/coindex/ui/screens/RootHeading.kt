package com.jenarvaezg.coindex.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.ui.components.Eyebrow
import com.jenarvaezg.coindex.ui.theme.Paper

/**
 * The notebook's label as an eyebrow, and under it the name of the current hierarchy (ADR 0021 §1).
 * Shared by the top-level roots so they look like one app.
 */
@Composable
fun RootHeading(destination: String, sentence: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Eyebrow(ROOT_LABEL)
        Text(destination, style = MaterialTheme.typography.displayLarge)
        Text(sentence, style = MaterialTheme.typography.bodyLarge, color = Paper.muted)
    }
}

private const val ROOT_LABEL = "Cuaderno de colección · Láminas de plata"
