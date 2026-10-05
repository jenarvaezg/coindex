package com.jenarvaezg.coindex.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

/**
 * El defecto de #40: una serie bullion en plata y en oro con el mismo peso y acabado, como
 * Equilibrium (N#307244 y N#309842, ambas de 31,1 g). Sin el metal en la clave, el segundo
 * catálogo curado se quedaba sin lámina que abrir.
 */
class MetalKeyTest {
    private fun ounceCatalog(id: String, metal: Metal, typeId: Int) = CollectionCatalog(
        schemaVersion = 1,
        id = id,
        name = id,
        shortName = id,
        issuerCode = "slovaquie",
        family = "Equilibrium",
        weightMillioz = 1_000,
        finish = Finish.Bullion,
        metal = metal,
        seriesStatus = SeriesStatus.Open,
        source = "https://en.numista.com/catalogue/series.php?id=6888",
        updatedAt = "2026-08-01",
        members = listOf(
            CollectionCatalogMember("2018", "2018", 2_018, typeId),
        ),
    )

    private val silver = ounceCatalog("equilibrium-silver", Metal.Silver, 307_244)
    private val gold = ounceCatalog("equilibrium-gold", Metal.Gold, 309_842)

    private fun piece(id: Long, typeId: Int) =
        CollectedItem(id = id, quantity = 1, typeId = typeId, issueYear = 2_018)

    private fun meta(typeId: Int, metal: Metal) = TypeMeta(
        id = typeId,
        family = "Equilibrium",
        minYear = 2018,
        maxYear = 2018,
        weightOz = gramsToOunces(31.1),
        finish = Finish.Bullion,
        metal = metal,
    )

    @Test
    fun `two catalogs of the same weight and finish no longer share a key`() {
        assertNull(silver.validate())
        assertNull(gold.validate())
        assertNotEquals(silver.key(), gold.key())
        // Sin el metal serían la misma tarjeta: lo demás coincide entero.
        assertEquals(silver.family, gold.family)
        assertEquals(silver.weightMillioz, gold.weightMillioz)
        assertEquals(silver.finish, gold.finish)
    }

    /** La tarjeta se queda con el primer catálogo de clave idéntica: las claves deben diferir. */
    @Test
    fun `each catalog is reachable from the derived collection its own pieces produce`() {
        val catalogs = listOf(silver, gold)
        val derivation = deriveCollection(
            items = listOf(piece(1, 307_244), piece(2, 309_842)),
            typeMeta = mapOf(
                307_244 to meta(307_244, Metal.Silver),
                309_842 to meta(309_842, Metal.Gold),
            ),
            catalogs = catalogs,
        )

        assertEquals(2, derivation.derivedCollections.size)
        val found = derivation.derivedCollections.map { collection ->
            catalogs.firstOrNull { catalog -> catalog.key() == collection.key() }?.id
        }
        assertEquals(listOf("equilibrium-silver", "equilibrium-gold"), found)
    }

    /** El metal lo declara el catálogo (ADR 0016); la pieza intrusa la delata el cruce. */
    @Test
    fun `a member takes its catalog's metal even when its own ficha says another`() {
        val intruder = piece(3, 309_842)
        val derivation = deriveCollection(
            items = listOf(intruder),
            typeMeta = mapOf(309_842 to meta(309_842, Metal.Gold)),
            catalogs = listOf(silver.copy(members = silver.members + gold.members)),
        )

        assertEquals(Metal.Silver, derivation.derivedCollections.single().metal)
    }

    @Test
    fun `without a catalog the metal comes from the ficha and splits the card`() {
        val derivation = deriveCollection(
            items = listOf(piece(1, 307_244), piece(2, 309_842)),
            typeMeta = mapOf(
                307_244 to meta(307_244, Metal.Silver),
                309_842 to meta(309_842, Metal.Gold),
            ),
            catalogs = emptyList(),
        )

        assertEquals(
            listOf(Metal.Silver, Metal.Gold),
            derivation.derivedCollections.map { it.metal },
        )
    }
}
