package com.jenarvaezg.coindex.debug.calibration

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.jenarvaezg.coindex.data.photos.CoinPhoto
import com.jenarvaezg.coindex.ui.CoinName
import com.jenarvaezg.coindex.ui.components.AlbumCartouche
import com.jenarvaezg.coindex.ui.components.AlbumHole
import com.jenarvaezg.coindex.ui.components.CoinTilt
import com.jenarvaezg.coindex.ui.components.LocalCoinGloss
import com.jenarvaezg.coindex.ui.components.LocalCoinTilt
import com.jenarvaezg.coindex.ui.components.LocalStamping
import com.jenarvaezg.coindex.ui.components.StampedRatio
import com.jenarvaezg.coindex.ui.components.Stamping
import com.jenarvaezg.coindex.ui.components.rememberInkFall
import com.jenarvaezg.coindex.ui.components.coinGloss
import com.jenarvaezg.coindex.ui.components.paperSurface
import com.jenarvaezg.coindex.ui.theme.BarlowCondensedFamily
import com.jenarvaezg.coindex.ui.theme.Paper
import java.util.Locale
import kotlinx.coroutines.delay

private val OBVERSE_URLS = listOf(
    "https://en.numista.com/catalogue/photos/venezuela/503-180.jpg",
    "https://en.numista.com/catalogue/photos/venezuela/503-original.jpg",
)
private val REVERSE_URLS = listOf(
    "https://en.numista.com/catalogue/photos/venezuela/502-180.jpg",
    "https://en.numista.com/catalogue/photos/venezuela/502-original.jpg",
)
private val TONE_CALIBRATION_PHOTO = CoinPhoto(
    thumbnail = OBVERSE_URLS[0],
    picture = OBVERSE_URLS[1],
)
private val TONE_CALIBRATION_NAME = CoinName("1 Bolívar", "Simón Bolívar")
private val GHOST_FILTER = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

@Composable
fun CalibrationBenchScreen(glossPositionFraction: Float = 0f) {
    var state by remember { mutableStateOf(CalibrationState()) }
    // The bench holds the tilt the same way the app does — read in the draw phase — so moving the
    // virtual sensor repaints the coins instead of recomposing the whole HUD under the sliders.
    val reading = remember { mutableFloatStateOf(0f) }
    SideEffect { reading.floatValue = glossPositionFraction }
    val tilt = remember { object : CoinTilt { override val lateral get() = reading.floatValue } }

    Surface(color = Paper.paper, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // The bench calibrates the grain on the production surface itself (#351): the same
                // Modifier the app paints its sheet with, so the bench cannot drift from what ships.
                .paperSurface(state.grainOpacity)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp),
        ) {
            Text(
                text = "BANCO DE CALIBRACIÓN",
                fontFamily = BarlowCondensedFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                letterSpacing = 1.2.sp,
                color = Paper.moss,
            )
            CalibrationTabs(
                selected = state.selectedTab,
                onSelected = { tab -> state = state.withTab(tab) },
            )
            Spacer(Modifier.height(14.dp))
            when (state.selectedTab) {
                CalibrationTab.EFFECTS -> {
                    Text(
                        text = "1 Bolívar · 1960",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = "N#5316 · una sola ranura real, todos los efectos en vivo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Paper.muted,
                    )
                    Spacer(Modifier.height(14.dp))
                    CalibrationSlot(state, tilt)
                    Spacer(Modifier.height(14.dp))
                    ProductionStrip(state, tilt)
                    Spacer(Modifier.height(18.dp))
                    CalibrationControls(
                        state = state,
                        onChange = { control, value -> state = state.withControl(control, value) },
                        onGhostShownChange = { shown -> state = state.withGhostShown(shown) },
                    )
                }
                CalibrationTab.TONE -> {
                    Text(
                        text = "1 Bolívar · 1960",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = "Separación papel↔objeto · siete valores independientes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Paper.muted,
                    )
                    Spacer(Modifier.height(14.dp))
                    ToneCalibrationPreview(state)
                    Spacer(Modifier.height(18.dp))
                    ToneCalibrationControls(
                        state = state,
                        onChange = { control, value -> state = state.withControl(control, value) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CalibrationTabs(
    selected: CalibrationTab,
    onSelected: (CalibrationTab) -> Unit,
) {
    val tabs = CalibrationTab.entries
    PrimaryTabRow(
        selectedTabIndex = tabs.indexOf(selected),
        containerColor = Color.Transparent,
        contentColor = Paper.ink,
        divider = {},
    ) {
        tabs.forEach { tab ->
            Tab(
                selected = selected == tab,
                onClick = { onSelected(tab) },
                text = {
                    Text(
                        text = when (tab) {
                            CalibrationTab.EFFECTS -> "EFECTOS"
                            CalibrationTab.TONE -> "TONO"
                        },
                        fontFamily = BarlowCondensedFamily,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.7.sp,
                    )
                },
            )
        }
    }
}

@Composable
private fun ToneCalibrationPreview(state: CalibrationState) {
    val tone = state.albumToneConfig()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, tone.hairlineColor, RoundedCornerShape(3.dp))
            .background(Paper.paper)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AlbumCartouche(
            name = TONE_CALIBRATION_NAME,
            modifier = Modifier.height(58.dp),
            tone = tone,
        )
        Spacer(Modifier.height(18.dp))
        AlbumHole(
            photo = TONE_CALIBRATION_PHOTO,
            modifier = Modifier.size(166.dp),
            tone = tone,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "1960",
            fontFamily = BarlowCondensedFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun CalibrationSlot(state: CalibrationState, tilt: CoinTilt) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.94f)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .clip(RoundedCornerShape(3.dp))
            .background(Paper.paperDeep)
            .border(1.dp, Paper.hairline.copy(alpha = 0.55f), RoundedCornerShape(3.dp)),
    ) {
        CoinRecess(
            state = state,
            tilt = tilt,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 30.dp),
        )

        RecessedYearTag(
            depth = state.recessDepthDp.dp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 18.dp, bottom = 18.dp),
        )
        CompletionRatio(
            durationMillis = state.stampingDurationMillis,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 6.dp, end = 7.dp),
        )
    }
}

@Composable
private fun CoinRecess(
    state: CalibrationState,
    tilt: CoinTilt,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(133.dp),
    ) {
        Spacer(
            Modifier
                .size(127.dp)
                .clip(CircleShape)
                .background(Paper.paperDeep),
        )
        if (state.showGhost) {
            GhostCoin(opacity = state.ghostOpacity, diameterDp = state.ghostDiameterDp)
        } else {
            FlippingCoin(state, tilt)
        }
        // Nothing is drawn over the coin here either: production retired the pair of half arcs in
        // #357 and the acetate's fixed reflection in #338 — two layers for the result of one, which
        // is the variant D #303 discarded. A slot that kept them would be calibrating a drawing the
        // app no longer paints.
    }
}

/**
 * The empty casilla, at the opacity and the **diameter** it is being read at (#556).
 *
 * The slot the bench paints is 121 dp of coin in a 133 dp recess, which is the plate's own casilla and
 * therefore the one size at which the penumbra was never in doubt. A ghost that could only be seen there
 * could not answer the question #556 asks — whether the sunk design is still a sentence at the 34 dp of
 * the country axis — so the drawing shrinks inside the recess while the cardboard around it stays put:
 * what is being judged is the design, not the hole.
 */
@Composable
private fun GhostCoin(opacity: Float, diameterDp: Float) {
    Box(
        modifier = Modifier
            .size(diameterDp.dp)
            .clip(CircleShape)
            .background(Paper.paperDeep),
    ) {
        CatalogFace(
            candidates = REVERSE_URLS,
            contentDescription = "Fantasma del Bolívar de 1960",
            colorFilter = GHOST_FILTER,
            modifier = Modifier
                .fillMaxSize()
                .alpha(opacity),
        )
        Canvas(Modifier.fillMaxSize().padding(5.dp)) {
            drawCircle(
                color = Paper.ink.copy(alpha = 0.46f),
                style = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 4.dp.toPx())),
                ),
            )
        }
    }
}

@Composable
private fun FlippingCoin(state: CalibrationState, tilt: CoinTilt) {
    val transition = rememberInfiniteTransition(label = "coin calibration")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 180f,
        animationSpec = infiniteRepeatable(
            animation = tween(state.flipDurationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "${state.flipDurationMillis} ms flip",
    )
    val showsObverse = rotation < 90f
    val faceRotation = if (showsObverse) rotation else rotation - 180f

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(121.dp)
            .graphicsLayer {
                rotationY = faceRotation
                cameraDistance = 12f * density
            }
            .clip(CircleShape)
            .background(Paper.paper),
    ) {
        CatalogFace(
            candidates = if (showsObverse) OBVERSE_URLS else REVERSE_URLS,
            contentDescription = if (showsObverse) "Anverso del Bolívar de 1960" else "Reverso del Bolívar de 1960",
            // The production effect itself and not a copy of it: what the sliders move here is the
            // same drawing the plate paints, which is the whole point of calibrating on a bench.
            modifier = Modifier.fillMaxSize().coinGloss(state.glossConfig(), tilt),
        )
    }
}

/**
 * The same coin at the size it actually ships at, next to the slot that is three times bigger.
 *
 * #303 judged the gloss on a 121 dp hole and already called it *subtle* there; the three surfaces
 * that paint one use **104 dp** (`IndexScreen`, `CoinsScreen`, `PlateScreen`), so a bench that only
 * shows the big slot would calibrate an effect nobody sees. Three of them and not one because what
 * is at stake is the grid: a casilla in a row of casillas, which is where the father looks.
 */
@Composable
private fun ProductionStrip(state: CalibrationState, tilt: CoinTilt) {
    Column {
        Text(
            text = "A 104 DP · EL TAMAÑO DE LAS TRES PANTALLAS",
            fontFamily = BarlowCondensedFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            letterSpacing = 0.7.sp,
            color = Paper.moss,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        CompositionLocalProvider(
            LocalCoinGloss provides state.glossConfig(),
            LocalCoinTilt provides tilt,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(3) {
                    AlbumHole(
                        photo = TONE_CALIBRATION_PHOTO,
                        modifier = Modifier.size(104.dp),
                        tone = state.albumToneConfig(),
                    )
                }
            }
        }
    }
}

@Composable
private fun CatalogFace(
    candidates: List<String>,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    colorFilter: ColorFilter? = null,
) {
    var attempt by remember(candidates) { mutableIntStateOf(0) }
    AsyncImage(
        model = candidates[attempt],
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        colorFilter = colorFilter,
        onState = { state ->
            if (state is AsyncImagePainter.State.Error && attempt < candidates.lastIndex) {
                attempt += 1
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun RecessedYearTag(depth: Dp, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(
                width = CalibrationState.YEAR_TAG_WIDTH_DP.dp,
                height = CalibrationState.YEAR_TAG_HEIGHT_DP.dp,
            )
            .drawWithCache {
                val depthPx = depth.toPx()
                val dark = Paper.ink.copy(alpha = 0.12f + depth.value / 30f)
                val light = Color.White.copy(alpha = 0.55f)
                onDrawBehind {
                    drawRect(Paper.paperDeep)
                    drawLine(dark, start = androidx.compose.ui.geometry.Offset.Zero, end = androidx.compose.ui.geometry.Offset(size.width, 0f), strokeWidth = depthPx)
                    drawLine(dark, start = androidx.compose.ui.geometry.Offset.Zero, end = androidx.compose.ui.geometry.Offset(0f, size.height), strokeWidth = depthPx)
                    drawLine(light, start = androidx.compose.ui.geometry.Offset(0f, size.height), end = androidx.compose.ui.geometry.Offset(size.width, size.height), strokeWidth = depthPx)
                    drawLine(light, start = androidx.compose.ui.geometry.Offset(size.width, 0f), end = androidx.compose.ui.geometry.Offset(size.width, size.height), strokeWidth = depthPx)
                }
            },
    ) {
        Text(
            text = "1960",
            fontFamily = BarlowCondensedFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = Paper.ink,
        )
    }
}

/**
 * The stamp of a complete sheet, falling on a loop so the millisecond can be judged.
 *
 * **The production drawing itself and not a copy of it**, the way the gloss already is: the ink the
 * sliders time here is the ink the plate presses, which is the whole point of calibrating on a
 * bench. What the bench adds is the repetition — a plate stamps once, on opening, and a number you
 * can only see once is a number you cannot calibrate.
 */
@Composable
private fun CompletionRatio(durationMillis: Int, modifier: Modifier = Modifier) {
    var press by remember { mutableIntStateOf(0) }
    LaunchedEffect(durationMillis) {
        while (true) {
            delay(durationMillis + 1_200L)
            press += 1
        }
    }
    // Keyed on the press: the stamp falls when it enters composition, so a fresh one is what a
    // repeat is. Toggling `complete` instead would animate the ink back *out* first, which is a
    // withdrawal production never draws.
    key(press) {
        CompositionLocalProvider(LocalStamping provides Stamping(durationMillis)) {
            StampedRatio(
                ratio = "22/22",
                complete = true,
                fall = rememberInkFall(complete = true),
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun CalibrationControls(
    state: CalibrationState,
    onChange: (CalibrationControl, Float) -> Unit,
    onGhostShownChange: (Boolean) -> Unit,
) {
    CalibrationSection(title = "PAPEL · MOSAICO 96 DP · SOFT-LIGHT") {
        ControlSlider(
            label = "Opacidad",
            value = state.grainOpacity,
            control = CalibrationControl.GRAIN_OPACITY,
            display = percent(state.grainOpacity),
            onChange = onChange,
        )
    }
    CalibrationSection(title = "BRILLO · 105°") {
        ControlSlider(
            label = "Intensidad",
            value = state.glossIntensity,
            control = CalibrationControl.GLOSS_INTENSITY,
            display = percent(state.glossIntensity),
            onChange = onChange,
        )
        ControlSlider(
            label = "Recorrido",
            value = state.glossTravel,
            control = CalibrationControl.GLOSS_TRAVEL,
            display = "±${(state.glossTravel * 100).toInt()} %",
            onChange = onChange,
        )
    }
    CalibrationSection(title = "GIRO") {
        ControlSlider(
            label = "Duración",
            value = state.flipDurationMillis.toFloat(),
            control = CalibrationControl.FLIP_DURATION_MILLIS,
            display = "${state.flipDurationMillis} ms",
            onChange = onChange,
        )
    }
    CalibrationSection(title = "ESTAMPADO") {
        ControlSlider(
            label = "Duración",
            value = state.stampingDurationMillis.toFloat(),
            control = CalibrationControl.STAMPING_DURATION_MILLIS,
            display = "${state.stampingDurationMillis} ms",
            onChange = onChange,
        )
    }
    CalibrationSection(title = "RÓTULO HUNDIDO · 48,3 × 28 DP") {
        ControlSlider(
            label = "Profundidad",
            value = state.recessDepthDp,
            control = CalibrationControl.RECESS_DEPTH_DP,
            display = decimal(state.recessDepthDp, " dp"),
            onChange = onChange,
        )
    }
    CalibrationSection(title = "FANTASMA · DISEÑO Y REGLA PUNTEADA") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Casilla vacía", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = state.showGhost, onCheckedChange = onGhostShownChange)
        }
        ControlSlider(
            label = "Opacidad",
            value = state.ghostOpacity,
            control = CalibrationControl.GHOST_OPACITY,
            display = percent(state.ghostOpacity),
            onChange = onChange,
        )
        ControlSlider(
            label = "Diámetro",
            value = state.ghostDiameterDp,
            control = CalibrationControl.GHOST_DIAMETER_DP,
            display = decimal(state.ghostDiameterDp, " dp"),
            onChange = onChange,
        )
    }
}

@Composable
private fun ToneCalibrationControls(
    state: CalibrationState,
    onChange: (CalibrationControl, Float) -> Unit,
) {
    CalibrationSection(title = "CARTELA") {
        ControlSlider(
            label = "Fondo",
            value = state.cartoucheAlpha,
            control = CalibrationControl.CARTOUCHE_ALPHA,
            display = percent(state.cartoucheAlpha),
            onChange = onChange,
        )
        ControlSlider(
            label = "Regla",
            value = state.cartoucheRuleAlpha,
            control = CalibrationControl.CARTOUCHE_RULE_ALPHA,
            display = percent(state.cartoucheRuleAlpha),
            onChange = onChange,
        )
    }
    CalibrationSection(title = "TROQUEL · FILETE DE 1 DP") {
        ControlSlider(
            label = "Cartón",
            value = state.cardAlpha,
            control = CalibrationControl.CARD_ALPHA,
            display = percent(state.cardAlpha),
            onChange = onChange,
        )
        ControlSlider(
            label = "Tono filete",
            value = state.hairlineTone.toFloat(),
            control = CalibrationControl.HAIRLINE_TONE,
            display = rgbHex(state.hairlineColorRgb),
            onChange = onChange,
        )
    }
    CalibrationSection(title = "PARED DEL TROQUEL · BARRIDO SIN COSTURA") {
        ControlSlider(
            label = "Ancho",
            value = state.dieWall.widthDp,
            control = CalibrationControl.DIE_WALL_WIDTH_DP,
            display = decimal(state.dieWall.widthDp, " dp"),
            onChange = onChange,
        )
        ControlSlider(
            label = "Sombra",
            value = state.dieWall.shadowAlpha,
            control = CalibrationControl.DIE_WALL_SHADOW_ALPHA,
            display = percent(state.dieWall.shadowAlpha),
            onChange = onChange,
        )
        ControlSlider(
            label = "Canto",
            value = state.dieWall.sheenAlpha,
            control = CalibrationControl.DIE_WALL_SHEEN_ALPHA,
            display = percent(state.dieWall.sheenAlpha),
            onChange = onChange,
        )
    }
}

@Composable
private fun CalibrationSection(title: String, content: @Composable () -> Unit) {
    HorizontalDivider(color = Paper.hairline.copy(alpha = 0.55f))
    Text(
        text = title,
        modifier = Modifier.padding(top = 14.dp, bottom = 4.dp),
        fontFamily = BarlowCondensedFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 0.7.sp,
        color = Paper.moss,
    )
    content()
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun ControlSlider(
    label: String,
    value: Float,
    control: CalibrationControl,
    display: String,
    onChange: (CalibrationControl, Float) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = label,
            modifier = Modifier.width(78.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
        Slider(
            value = value,
            onValueChange = { onChange(control, it) },
            valueRange = control.range,
            steps = control.steps,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = display,
            modifier = Modifier.width(58.dp),
            fontFamily = BarlowCondensedFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            textAlign = TextAlign.End,
            color = Paper.ink,
        )
    }
}

private fun percent(value: Float): String = "${(value * 100).toInt()} %"

private fun decimal(value: Float, suffix: String): String =
    String.format(Locale.ROOT, "%.1f%s", value, suffix)

private fun rgbHex(rgb: Int): String = String.format(Locale.ROOT, "#%06X", rgb)
