package com.jenarvaezg.coindex.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The Snake: both a cell of Monedas and a card's cover. */
private const val A_TYPE = 404_064

private const val A_CATALOG = "lunar-series-iii-1oz"

/**
 * The coin does not take off where the system asked for quiet (#514). Even at
 * `animator_duration_scale 0` a shared element drew its photograph at its origin for a frame,
 * because its target comes from a lookahead pass a frame later; the only fix is not to travel. With
 * quiet asked for, the two modifiers of ADR 0026 §3 hand back the modifier they were given.
 *
 * Both journeys are composed inside a real layout and destination: without a scope the modifiers
 * return `this` anyway, and the two take-off tests are the controls for that.
 */
@RunWith(AndroidJUnit4::class)
class QuietTravelTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    // D8 forbids spaces in method names below DEX 040, so instrumented tests cannot use backticks.
    fun aQuietenedTypeCoinIsHandedBackUntouched() {
        assertSame(Modifier, travellingModifier(moving = false) { typeCoin() })
    }

    @Test
    fun aQuietenedCatalogCoinIsHandedBackUntouched() {
        assertSame(Modifier, travellingModifier(moving = false) { catalogCoin() })
    }

    @Test
    fun aTypeCoinNobodyQuietenedTakesOff() {
        assertNotSame(Modifier, travellingModifier(moving = true) { typeCoin() })
    }

    @Test
    fun aCatalogCoinNobodyQuietenedTakesOff() {
        assertNotSame(Modifier, travellingModifier(moving = true) { catalogCoin() })
    }

    @Composable
    private fun typeCoin(): Modifier = Modifier.travellingTypeCoin(A_TYPE, visible = true)

    @Composable
    private fun catalogCoin(): Modifier = Modifier.travellingCoin(A_CATALOG)

    /** What the modifier came back as, in a tree that has both ends of a journey in it. */
    @OptIn(ExperimentalSharedTransitionApi::class)
    private fun travellingModifier(moving: Boolean, coin: @Composable () -> Modifier): Modifier {
        var applied: Modifier? = null
        compose.setContent {
            SharedTransitionLayout {
                AnimatedVisibility(visible = true) {
                    CompositionLocalProvider(
                        LocalSharedTransition provides this@SharedTransitionLayout,
                        LocalNavAnimation provides this@AnimatedVisibility,
                        LocalMotion provides moving,
                    ) {
                        applied = coin()
                    }
                }
            }
        }
        compose.waitForIdle()
        return applied!!
    }
}
