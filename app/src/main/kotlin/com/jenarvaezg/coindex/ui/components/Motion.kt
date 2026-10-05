package com.jenarvaezg.coindex.ui.components

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Whether the system allows animation (#514). Compose scales tweens by `ANIMATOR_DURATION_SCALE`,
 * but a shared element still draws one frame at its origin (from the lookahead pass), so with
 * animations off the flight must not be made at all. The ceremonies of ADR 0026 §3 check this and
 * skip what they can cheaply skip: the shared-element modifier, the transition, `Stamping`, the
 * sensor. The flip and the return fade are left to Compose, since their one leaked frame shows
 * something that belongs there anyway.
 *
 * True by default: previews, tests and exported sheets have no system to ask.
 */
val LocalMotion = staticCompositionLocalOf { true }

/**
 * Whether `Settings.Global.ANIMATOR_DURATION_SCALE` allows movement: only zero (the accessibility
 * setting that removes animations) means stop. It is the scale Compose reads, so both agree.
 */
fun movesAt(animatorDurationScale: Float): Boolean = animatorDurationScale > 0f
