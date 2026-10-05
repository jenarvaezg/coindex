package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.domain.Finish
import com.jenarvaezg.coindex.domain.Metal
import com.jenarvaezg.coindex.domain.SPANNING_VARIANTS_WEIGHT
import com.jenarvaezg.coindex.domain.VariantKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Reading a derived-collection route back into the four canonical parts of its [VariantKey]:
 * anything not already canonical is refused rather than guessed. `Routes.derivedCollection` needs
 * `android.net.Uri`, so only the reading is tested on the JVM.
 */
class DerivedCollectionRouteTest {
    @Test
    fun `a canonical route rebuilds its key, accents and spaces included`() {
        assertEquals(
            VariantKey("100 Pesetas de Franco", 611, null, Metal.Silver),
            variantKeyFromRoute("100 Pesetas de Franco", "611", "unknown", "silver"),
        )
        assertEquals(
            VariantKey("Tudor Beasts", 2_000, Finish.Bullion, Metal.Silver),
            variantKeyFromRoute("Tudor Beasts", "2000", "bullion", "silver"),
        )
    }

    /** A set spans physical variants, so its weight is the sentinel rather than a number. */
    @Test
    fun `a set route rebuilds an absent weight`() {
        val key = variantKeyFromRoute(
            "Trío de 1983",
            SPANNING_VARIANTS_WEIGHT.toString(),
            "unknown",
            "unknown",
        )
        assertEquals(VariantKey("Trío de 1983", null, null, null), key)
        assertNull(key?.weightMillioz)
    }

    @Test
    fun `anything that is not canonical is refused`() {
        // An unnormalized family, an impossible weight, an unknown finish or metal code.
        assertNull(variantKeyFromRoute("Dos  espacios", "611", "unknown", "silver"))
        assertNull(variantKeyFromRoute("Familia", "0", "unknown", "silver"))
        assertNull(variantKeyFromRoute("Familia", "611", "chapado en resina", "silver"))
        assertNull(variantKeyFromRoute("Familia", "611", "unknown", "plata"))
        // A truncated or absent argument is not a key either.
        assertNull(variantKeyFromRoute(null, "611", "unknown", "silver"))
        assertNull(variantKeyFromRoute("Familia", null, "unknown", "silver"))
        assertNull(variantKeyFromRoute("Familia", "seiscientos", "unknown", "silver"))
        assertNull(variantKeyFromRoute("Familia", "611", null, "silver"))
        assertNull(variantKeyFromRoute("Familia", "611", "unknown", null))
    }
}
