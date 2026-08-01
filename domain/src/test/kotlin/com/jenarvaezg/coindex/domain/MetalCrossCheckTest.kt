package com.jenarvaezg.coindex.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * El cruce vive en el suite y **nunca** en el validador de arranque (#40).
 *
 * Lo que un catálogo declara es la variante de la colección, no una afirmación sobre cada miembro:
 * siete monedas de plata y una de cuproníquel pueden ser curaduría legítima, y una comprobación
 * fatal convertiría el `composition.text` de Numista en un veto sobre el criterio del curador. Lo
 * que sí caza es el intruso accidental — el vigésimo de onza de **oro** que estuvo en el catálogo
 * del Kookaburra haciéndose pasar por la onza de plata de 2009 (#63).
 */
class MetalCrossCheckTest {
    private fun catalog(vararg members: CollectionCatalogMember) = CollectionCatalog(
        schemaVersion = 1,
        id = "australian-kookaburra-perth-1oz",
        name = "Kookaburra",
        issuerCode = "australie",
        family = "Australian Kookaburra",
        weightMillioz = 1_000,
        finish = null,
        metal = Metal.Silver,
        seriesStatus = SeriesStatus.Open,
        source = "https://en.numista.com/catalogue/series.php?id=6118",
        updatedAt = "2026-08-01",
        members = members.toList(),
    )

    private val silverOunce = CollectionCatalogMember("2009", "2009", 2_009, 17_382)
    private val goldTwentieth = CollectionCatalogMember("2009-bis", "2009", 2_009, 43_242)

    private val compositions = mapOf(
        17_382 to "Plata 999",
        43_242 to "Oro 999,9",
    )

    @Test
    fun `an intruder of another metal is reported with its type and both metals`() {
        val deviations = metalDeviations(listOf(catalog(silverOunce, goldTwentieth)), compositions)

        val deviation = deviations.single()
        assertEquals(43_242, deviation.numistaTypeId)
        assertEquals(Metal.Silver, deviation.declared)
        assertEquals(Metal.Gold, deviation.observed)
        // El mensaje tiene que bastar para ir a la ficha sin abrir el test.
        assertTrue(deviation.toString().contains("Numista 43242"), deviation.toString())
        assertTrue(deviation.toString().contains("gold"), deviation.toString())
    }

    /**
     * La salida del curador: una nota en prosa en la casilla, como la `closed_note` al cerrar una
     * serie. Silencia esa casilla y ninguna otra.
     */
    @Test
    fun `a member that declares the deviation in prose is exempt`() {
        val declared = goldTwentieth.copy(
            variantNote = "Es la única de oro de la lámina y entra a propósito: la colección " +
                "del padre la persigue con las de plata.",
        )

        assertEquals(
            emptyList(),
            metalDeviations(listOf(catalog(silverOunce, declared)), compositions),
        )
        // Y no tapa a la de al lado: otra casilla desviada sigue saliendo.
        val second = CollectionCatalogMember("2010", "2010", 2_010, 43_243)
        assertEquals(
            1,
            metalDeviations(
                listOf(catalog(silverOunce, declared, second)),
                compositions + (43_243 to "Oro 999,9"),
            ).size,
        )
    }

    @Test
    fun `nothing is claimed about a type nobody cached or a text nobody understands`() {
        // Que falte la ficha lo denuncia el test de la caché sembrada, no éste.
        assertEquals(emptyList(), metalDeviations(listOf(catalog(goldTwentieth)), emptyMap()))
        assertEquals(
            emptyList(),
            metalDeviations(listOf(catalog(goldTwentieth)), mapOf(43_242 to "Aleación rara")),
        )
    }

    /** Un conjunto no declara metal, así que no hay nada contra lo que cruzar. */
    @Test
    fun `a set is not cross-checked`() {
        val set = setCatalogStub()

        assertEquals(emptyList(), metalDeviations(listOf(set), mapOf(22_178 to "Oro 999,9")))
    }
}
