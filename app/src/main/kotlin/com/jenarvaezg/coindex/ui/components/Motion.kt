package com.jenarvaezg.coindex.ui.components

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Whether the app is allowed to move, which is the system's answer and not the app's (#514).
 *
 * Compose already scales every duration it owns by `ANIMATOR_DURATION_SCALE`, so at zero a tween
 * finishes on the frame it starts and the flip of #337 does not move at all. What it cannot
 * collapse is a **shared element**: its place comes from a lookahead pass, so the photograph is
 * drawn once where it took off before it is drawn where it lands — one frame that puts a coin over
 * «Ver en Numista», which is the capture the audit of 14 August 2026 took a second after the touch.
 * A frame is not a duration and no scale factor divides it away; the only way to not see the flight
 * is to not make it.
 *
 * So this is not a second animation clock. It is the one question the ceremonies of ADR 0026 §3 ask
 * before they begin — «is anybody asking for quiet?» — and **wherever the app owns a switch that
 * costs nothing to throw**, it throws it: a modifier not applied, a transition object not built, a
 * `Stamping` not provided, a sensor not registered. Quiet is then a guarantee, and not an accident
 * of how the frames happened to fall.
 *
 * Two movements have no such switch short of inventing one — the flip's `animateFloatAsState` and
 * the fade of the return — and they are left to Compose, which is safe for a reason of its own:
 * what their leaked frame draws is the face that was already up and the plate still covering the
 * index, both of which belong. That is the test the shared element failed, and it is why the flight
 * is the one that had to be switched off rather than trusted.
 *
 * True by default, because a tree with nothing provided is a preview, a test or an exported sheet,
 * and none of those has a system to ask.
 */
val LocalMotion = staticCompositionLocalOf { true }

/**
 * What the app reads out of `Settings.Global.ANIMATOR_DURATION_SCALE`, and the whole of it.
 *
 * The setting is a factor and the app only wants the boundary: 10× is what a session measuring an
 * animation puts there and it is still movement, while zero is «quita las animaciones» in
 * accessibility — or whoever gets dizzy — and it is the only value that means stop. Reading the
 * animator scale and not the window or transition ones is reading the same setting Compose reads,
 * so the app and its animations cannot disagree about what the system asked for.
 */
fun movesAt(animatorDurationScale: Float): Boolean = animatorDurationScale > 0f
