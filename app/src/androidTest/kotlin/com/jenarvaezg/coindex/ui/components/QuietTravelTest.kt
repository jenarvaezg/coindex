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

/** The father's Snake, which is a cell of Monedas and the cover of a card at the same time. */
private const val A_TYPE = 404_064

private const val A_CATALOG = "lunar-series-iii-1oz"

/**
 * The coin does not take off where the system asked for quiet (#514).
 *
 * This is the defect itself and not a duration around it: at `animator_duration_scale 0` the sheet
 * already arrived settled, and what the audit of 14 August 2026 caught was a **shared element**
 * drawing its photograph where it took off — over «Ver en Numista», with the sheet's hole empty. Its
 * place comes from a lookahead pass that lands a frame later, and no scale factor divides a frame
 * away, so the only fix is not to make the journey. What is defended here is exactly that: with
 * quiet asked for, the two modifiers of ADR 0026 §3 hand back the modifier they were given.
 *
 * Both journeys are composed inside a real layout **and** a real destination, so each has
 * everything it needs to fly. Without that the modifiers return `this` for want of a scope and both
 * tests would pass against the bug they exist for — which is what the two controls check.
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
