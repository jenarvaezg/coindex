package com.jenarvaezg.coindex.ui.ficha

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.em
import com.jenarvaezg.coindex.ui.components.ExternalLinkGlyph
import com.jenarvaezg.coindex.ui.theme.Paper

/**
 * Numista's comments as prose: paragraphs and emphasis kept, and every link drawn as one that
 * leaves the app, with the arrow of ADR 0026 §3 after it.
 */
@Composable
internal fun FichaProse(html: String, modifier: Modifier = Modifier) {
    val prose = remember(html) {
        withLinkGlyphs(AnnotatedString.fromHtml(absoluteLinks(html), linkStyles = LINK_STYLES))
    }
    Text(
        text = prose,
        style = MaterialTheme.typography.bodyMedium,
        inlineContent = LINK_GLYPH_CONTENT,
        modifier = modifier,
    )
}

/** Numista writes links to its own pages from the site's root. */
internal fun absoluteLinks(html: String): String = html.replace("href=\"/", "href=\"$NUMISTA/")

private const val NUMISTA = "https://es.numista.com"

/** Each link followed by the drawn arrow; the paragraph breaks at the end trimmed. */
private fun withLinkGlyphs(text: AnnotatedString): AnnotatedString {
    val trimmed = text.subSequence(0, text.text.trimEnd().length)
    val links = trimmed.getLinkAnnotations(0, trimmed.length).sortedBy { it.end }
    return buildAnnotatedString {
        var cursor = 0
        links.forEach { link ->
            append(trimmed.subSequence(cursor, link.end))
            append('\u00A0')
            appendInlineContent(LINK_GLYPH)
            cursor = link.end
        }
        append(trimmed.subSequence(cursor, trimmed.length))
    }
}

private val LINK_STYLES = TextLinkStyles(
    style = SpanStyle(color = Paper.moss, textDecoration = TextDecoration.Underline),
)

private const val LINK_GLYPH = "external-link-glyph"

private val LINK_GLYPH_CONTENT = mapOf(
    LINK_GLYPH to InlineTextContent(
        Placeholder(
            width = 0.85.em,
            height = 0.85.em,
            placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
        ),
    ) {
        ExternalLinkGlyph(color = Paper.moss, modifier = Modifier.fillMaxSize())
    },
)
