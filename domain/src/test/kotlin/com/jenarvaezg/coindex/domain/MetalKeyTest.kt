package com.jenarvaezg.coindex.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

/**
 * El defecto que #40 mandó cerrar, en el caso que lo produce: una serie bullion emitida en plata
 * y en oro con **el mismo peso y el mismo acabado**.
 *
 * Equilibrium es el caso vivo — N#307244 en plata y N#309842 en oro, los dos a 31,1 g— y hasta
 * esta versión los dos catálogos compartían clave. Nada fallaba a la vista: la tarjeta se empareja
 * con su catálogo por clave exacta y se queda con el primero que encuentre, así que el segundo
 * catálogo que alguien curara nacería sin lámina que abrir.
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

    /**
     * Lo que la pantalla hace con la tarjeta: `IndexScreen` se queda con el primer catálogo cuya
     * clave coincida. Con dos claves distintas cada propuesta encuentra el suyo.
     */
    @Test
    fun `each catalog is reachable from the proposal its own pieces produce`() {
        val catalogs = listOf(silver, gold)
        val derivation = deriveCollection(
            items = listOf(piece(1, 307_244), piece(2, 309_842)),
            typeMeta = mapOf(
                307_244 to meta(307_244, Metal.Silver),
                309_842 to meta(309_842, Metal.Gold),
            ),
            catalogs = catalogs,
        )

        assertEquals(2, derivation.proposals.size)
        val found = derivation.proposals.map { proposal ->
            catalogs.firstOrNull { catalog -> catalog.key() == proposal.key() }?.id
        }
        assertEquals(listOf("equilibrium-silver", "equilibrium-gold"), found)
    }

    /**
     * El metal lo declara el catálogo, no la ficha de cada miembro (ADR 0016): una pieza de oro
     * dentro del catálogo de plata sigue contando en la lámina de plata. Quien la delata es el
     * cruce del suite, no la clave.
     */
    @Test
    fun `a member takes its catalog's metal even when its own ficha says another`() {
        val intruder = piece(3, 309_842)
        val derivation = deriveCollection(
            items = listOf(intruder),
            typeMeta = mapOf(309_842 to meta(309_842, Metal.Gold)),
            catalogs = listOf(silver.copy(members = silver.members + gold.members)),
        )

        assertEquals(Metal.Silver, derivation.proposals.single().metal)
    }

    /** Una pieza sin catálogo sí toma el metal de su ficha, y dos metales son dos tarjetas. */
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

        assertEquals(listOf(Metal.Silver, Metal.Gold), derivation.proposals.map { it.metal })
    }
}
