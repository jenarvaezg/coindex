package com.jenarvaezg.coindex.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.ui.components.paperSurface
import androidx.compose.ui.unit.sp
import com.jenarvaezg.coindex.R

/**
 * Ornithological field-guide palette, carried over from the frozen web prototype so both
 * renderings of Coindex look like the same notebook.
 */
object Paper {
    val ink = Color(0xFF2D3029)
    val muted = Color(0xFF686A5D)
    val paper = Color(0xFFEEE8D7)
    val paperDeep = Color(0xFFDDD3BB)
    val line = Color(0xFF7D806C)
    val moss = Color(0xFF495C49)
    val rust = Color(0xFF8B553C)
    val card = Color(0x94FFFCF2)
    val hairline = Color(0xFF878577)
}

/** Dimensions shared by the plate and the index, so cards line up across screens. */
object PlateMetrics {
    val gutter = 16.dp

    /**
     * Gap between stacked cards that carry their own action row.
     *
     * Wider than [gutter] on purpose: with the actions at the bottom edge of one card and the
     * next card's title right under them, a thumb aiming for «Ignorar» could land on the card
     * below — which happened during the UX review itself.
     */
    val cardStack = 26.dp
    val cardPadding = 14.dp
    val minPlateCell = 168.dp
}

private val fieldColors = lightColorScheme(
    primary = Paper.ink,
    onPrimary = Paper.paper,
    secondary = Paper.moss,
    tertiary = Paper.rust,
    background = Paper.paper,
    onBackground = Paper.ink,
    surface = Paper.paper,
    onSurface = Paper.ink,
    surfaceVariant = Paper.paperDeep,
    onSurfaceVariant = Paper.muted,
    outline = Paper.line,
)

@OptIn(ExperimentalTextApi::class)
val BitterFamily = FontFamily(
    Font(
        R.font.bitter_variable,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400)),
    ),
)

val BarlowCondensedFamily = FontFamily(
    Font(R.font.barlow_condensed_regular, weight = FontWeight.Normal),
    Font(R.font.barlow_condensed_semibold, weight = FontWeight.SemiBold),
)

/**
 * A guide, not a dashboard: serif for prose, a condensed sans in small caps for data. The
 * palette is paper-toned in both system themes on purpose — the plate is a printed page.
 */
private val fieldTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = BitterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 40.sp,
        lineHeight = 42.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = BitterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 26.sp,
        lineHeight = 30.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = BitterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 21.sp,
        lineHeight = 25.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = BitterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 21.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = BitterFamily,
        fontSize = 16.sp,
        lineHeight = 23.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = BitterFamily,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = BarlowCondensedFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        fontFeatureSettings = "'smcp', 'tnum'",
    ),
    labelMedium = TextStyle(
        fontFamily = BarlowCondensedFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        fontFeatureSettings = "'smcp', 'tnum'",
    ),
    labelSmall = TextStyle(
        fontFamily = BarlowCondensedFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        fontFeatureSettings = "'smcp', 'tnum'",
    ),
)

/** The palette does not follow the system dark theme on purpose: a plate is paper. */
@Composable
fun CoindexTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = fieldColors,
        typography = fieldTypography,
    ) {
        // The sheet is the window, and not a background each screen paints for itself: the grain
        // used to live on two screens and stop at the edge of the third (#351).
        Box(modifier = Modifier.fillMaxSize().paperSurface(), content = { content() })
    }
}
