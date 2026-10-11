package com.jenarvaezg.coindex.ui

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val SEARCH = "search"

/**
 * The bar brings Monedas back as it was left; a cross with its own filters opens it without the
 * search left there (#645). Monedas here is only its saveable search box.
 */
@RunWith(AndroidJUnit4::class)
class RootCrossingTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var nav: NavHostController

    private fun roots() {
        compose.setContent {
            nav = rememberNavController()
            NavHost(navController = nav, startDestination = Routes.INDEX) {
                composable(Routes.INDEX) { Text("Colecciones") }
                composable(Routes.COINS) {
                    var query by rememberSaveable { mutableStateOf("") }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.testTag(SEARCH),
                    )
                }
                composable(Routes.FIGURES) { Text("Las cifras") }
            }
        }
    }

    private fun go(cross: NavHostController.() -> Unit) {
        compose.runOnIdle { nav.cross() }
        compose.waitForIdle()
    }

    private fun searchLeftInCoins(word: String) {
        go { crossToRoot(Routes.COINS) }
        compose.onNodeWithTag(SEARCH).performTextInput(word)
    }

    private fun route() = compose.runOnIdle { nav.currentDestination?.route }

    @Test
    fun theBarBringsBackTheSearchLeftInCoins() {
        roots()
        searchLeftInCoins("bazan")

        go { crossToRoot(Routes.INDEX) }
        go { crossToRoot(Routes.COINS) }

        compose.onNodeWithTag(SEARCH).assertTextEquals("bazan")
    }

    @Test
    fun aCrossFromColeccionesOpensCoinsWithoutTheSearch() {
        roots()
        searchLeftInCoins("bazan")

        go { crossToRoot(Routes.INDEX) }
        go { crossToFreshCoins() }

        compose.onNodeWithTag(SEARCH).assertTextEquals("")
    }

    @Test
    fun theBarDoesNotBringBackTheSearchACrossLeftBehind() {
        roots()
        searchLeftInCoins("bazan")
        go { crossToRoot(Routes.INDEX) }
        go { crossToFreshCoins() }
        compose.onNodeWithTag(SEARCH).performTextInput("duro")

        go { crossToRoot(Routes.INDEX) }
        go { crossToRoot(Routes.COINS) }

        compose.onNodeWithTag(SEARCH).assertTextEquals("duro")
    }

    @Test
    fun aCrossFromLasCifrasOpensCoinsWithoutTheSearch() {
        roots()
        searchLeftInCoins("bazan")

        go { crossToRoot(Routes.FIGURES) }
        go { crossToFreshCoins() }

        compose.onNodeWithTag(SEARCH).assertTextEquals("")
    }

    @Test
    fun afterACrossFromLasCifrasTheBarStillLandsOnColecciones() {
        roots()
        go { crossToRoot(Routes.FIGURES) }
        go { crossToFreshCoins() }

        go { crossToRoot(Routes.INDEX) }

        assertEquals(Routes.INDEX, route())
    }
}
