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
 * The notebook's own label, and under it the name of the hierarchy you are in (ADR 0021 §1).
 *
 * «Cuaderno de colección · Láminas de plata» used to be an eyebrow and a `displayLarge` title, which
 * worked while the index *was* the app. With two sibling hierarchies the big line has to say which
 * one you are looking at, so the two halves of the root label fold into one eyebrow above it — the
 * notebook keeps its name, and «Colecciones» and «Monedas» get the slot that tells them apart.
 *
 * One composable and not two headings, so the two roots cannot drift into looking like two apps.
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
