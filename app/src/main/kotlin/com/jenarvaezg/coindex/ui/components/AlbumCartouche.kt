package com.jenarvaezg.coindex.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jenarvaezg.coindex.ui.CoinName
import com.jenarvaezg.coindex.ui.theme.Paper

private val ALBUM_CARTOUCHE_HEIGHT = 52.dp

/**
 * A recess pressed into the album board: shadow at the top edge, fresh cardboard at the bottom.
 * Shared by the Coins cartouche (#350) and the year tag (#337) so both sink the same way. The sides
 * are deliberately not drawn.
 */
fun Modifier.recessedInBoard(
    fillAlpha: Float = AlbumToneConfig.Default.cartoucheAlpha,
    topRuleAlpha: Float = AlbumToneConfig.Default.cartoucheTopRuleAlpha,
): Modifier = drawBehind {
    drawRect(Paper.paperDeep.copy(alpha = fillAlpha))
    drawLine(
        Paper.ink.copy(alpha = topRuleAlpha),
        start = Offset.Zero,
        end = Offset(size.width, 0f),
        strokeWidth = 2.dp.toPx(),
    )
    drawLine(
        Color.White.copy(alpha = 0.55f),
        start = Offset(0f, size.height),
        end = Offset(size.width, size.height),
        strokeWidth = 1.dp.toPx(),
    )
}

/** Static recessed album label: one denomination range and a two-line theme slot. */
@Composable
fun AlbumCartouche(
    name: CoinName,
    modifier: Modifier = Modifier,
    tone: AlbumToneConfig = AlbumToneConfig.Default,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            // Fixed height so every label is equal; the worst case fits (#350).
            .height(ALBUM_CARTOUCHE_HEIGHT)
            .recessedInBoard(tone.cartoucheAlpha, tone.cartoucheTopRuleAlpha)
            .padding(horizontal = 5.dp, vertical = 4.dp),
    ) {
        Text(
            text = name.denomination,
            style = MaterialTheme.typography.labelLarge,
            autoSize = TextAutoSize.StepBased(
                minFontSize = 1.sp,
                maxFontSize = 12.sp,
                stepSize = 0.5.sp,
            ),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Visible,
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            name.theme?.let { theme ->
                Text(
                    text = theme,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
