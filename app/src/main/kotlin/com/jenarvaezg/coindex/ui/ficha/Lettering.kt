package com.jenarvaezg.coindex.ui.ficha

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/** A stretch of a legend, and whether it is a digit to draw upside down. */
data class LetteringRun(val text: String, val turned: Boolean = false)

/**
 * Splits a legend around Unicode's turned digits (`↊`, `↋`), which neither Bitter nor Barlow has:
 * each becomes its digit, drawn turned over (#643).
 */
fun letteringRuns(text: String): List<LetteringRun> {
    val runs = mutableListOf<LetteringRun>()
    val plain = StringBuilder()
    for (char in text) {
        val digit = TURNED_DIGITS[char]
        if (digit == null) {
            plain.append(char)
        } else {
            if (plain.isNotEmpty()) runs += LetteringRun(plain.toString())
            plain.clear()
            runs += LetteringRun(digit, turned = true)
        }
    }
    if (plain.isNotEmpty()) runs += LetteringRun(plain.toString())
    return runs
}

private val TURNED_DIGITS = mapOf('↊' to "2", '↋' to "3")

/** A legend as struck, its turned digits drawn as their digit turned over. */
@Composable
internal fun LetteringText(text: String, style: TextStyle, modifier: Modifier = Modifier) {
    val runs = remember(text) { letteringRuns(text) }
    val annotated = remember(runs) {
        buildAnnotatedString {
            runs.forEach { run ->
                if (run.turned) {
                    appendInlineContent(turnedId(run.text), run.text)
                } else {
                    append(run.text)
                }
            }
        }
    }
    val inline = remember(runs, style) {
        runs.filter { it.turned }.map { it.text }.distinct().associate { digit ->
            turnedId(digit) to InlineTextContent(
                Placeholder(
                    width = 0.62.em,
                    height = 1.em,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
                ),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        digit,
                        style = style.copy(letterSpacing = 0.sp),
                        modifier = Modifier.rotate(180f),
                    )
                }
            }
        }
    }
    Text(text = annotated, style = style, inlineContent = inline, modifier = modifier)
}

private fun turnedId(digit: String): String = "turned-$digit"
