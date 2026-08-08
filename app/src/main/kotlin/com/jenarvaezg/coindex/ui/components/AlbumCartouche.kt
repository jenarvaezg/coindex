package com.jenarvaezg.coindex.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

/** Static recessed album label: one denomination range and at most two theme lines. */
@Composable
fun AlbumCartouche(name: CoinName, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .drawBehind {
                drawRect(Paper.paperDeep.copy(alpha = 0.72f))
                drawLine(
                    Paper.ink.copy(alpha = 0.24f),
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
            .padding(horizontal = 5.dp, vertical = 4.dp),
    ) {
        Text(
            text = name.denomination,
            style = MaterialTheme.typography.labelLarge,
            autoSize = TextAutoSize.StepBased(
                minFontSize = 8.sp,
                maxFontSize = 12.sp,
                stepSize = 0.5.sp,
            ),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = Modifier.fillMaxWidth(),
        )
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
