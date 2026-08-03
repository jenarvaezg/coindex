package com.jenarvaezg.coindex.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CuratedOrphansTest {
    @Test
    fun `an empty register is valid`() {
        assertNull(
            CuratedOrphans(schemaVersion = 1, updatedAt = "2026-08-03", orphans = emptyList())
                .validate(),
        )
    }

    @Test
    fun `duplicate type ids fail validation`() {
        val error = CuratedOrphans(
            schemaVersion = 1,
            updatedAt = "2026-08-03",
            orphans = listOf(
                OrphanEntry(1_885, "sola"),
                OrphanEntry(1_885, "otra vez"),
            ),
        ).validate()
        check(error is CuratedOrphansValidationError.DuplicateNumistaTypeId)
        assertEquals(1_885, error.typeId)
    }

    @Test
    fun `blank reason fails validation`() {
        val error = CuratedOrphans(
            schemaVersion = 1,
            updatedAt = "2026-08-03",
            orphans = listOf(OrphanEntry(1_885, "   ")),
        ).validate()
        assertTrue(error is CuratedOrphansValidationError.BlankReason)
    }

    @Test
    fun `orphan catalog collisions list issued catalog types only`() {
        val catalogs = listOf(
            CollectionCatalog(
                schemaVersion = 1,
                id = "prueba",
                name = "Prueba",
                issuerCode = "espagne",
                family = "Prueba",
                weightMillioz = 1_000,
                metal = Metal.Silver,
                seriesStatus = SeriesStatus.Closed,
                closedNote = "cerrada para el test",
                source = "https://en.numista.com/catalogue/pieces1885.html",
                updatedAt = "2026-08-03",
                members = listOf(
                    CollectionCatalogMember(
                        id = "issued",
                        label = "Emitida",
                        year = 2_000,
                        numistaTypeId = 1_885,
                    ),
                    CollectionCatalogMember(
                        id = "announced",
                        label = "Anunciada",
                        status = MemberStatus.Announced,
                        source = "https://example.com/announced",
                        sourceNote = "solo anunciada",
                    ),
                ),
            ),
        )
        val orphans = CuratedOrphans(
            schemaVersion = 1,
            updatedAt = "2026-08-03",
            orphans = listOf(
                OrphanEntry(1_885, "choca con catálogo"),
                OrphanEntry(9_999, "libre"),
            ),
        )
        assertEquals(listOf(1_885), orphanCatalogCollisions(orphans, catalogs))
    }
}
