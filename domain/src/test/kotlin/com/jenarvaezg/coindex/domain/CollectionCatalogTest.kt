package com.jenarvaezg.coindex.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal fun teslaCatalogStub() = CollectionCatalog(
    schemaVersion = 1,
    id = "nikola-tesla-serbia-1oz",
    name = "Nikola Tesla · Serbia · 1 oz",
    issuerCode = "serbie",
    family = "Nikola Tesla",
    weightMillioz = 1_000,
    finish = null,
    source = "https://en.numista.com/catalogue/series.php?id=5303",
    updatedAt = "2026-07-29",
    members = listOf(
        CollectionCatalogMember("alternating-current", "Alternating current", 2018, 150_352),
        CollectionCatalogMember("x-rays", "X-Rays", 2020, 195_591),
    ),
)

internal fun dateRunCatalogStub() = CollectionCatalog(
    schemaVersion = 2,
    id = "venezuela-5-bolivares",
    name = "5 Bolívares · Venezuela",
    issuerCode = "venezuela",
    family = "5 Bolívares de Venezuela",
    weightMillioz = 804,
    finish = null,
    source = "https://en.numista.com/catalogue/pieces10340.html",
    updatedAt = "2026-07-29",
    members = listOf(
        CollectionCatalogMember("1904", "1904", 1904, 10_340),
        CollectionCatalogMember("1905", "1905", 1905, 10_340),
    ),
)

/** The 1983 Portuguese trio: three denominations issued together as one mint set (ADR 0012). */
internal fun setCatalogStub() = CollectionCatalog(
    schemaVersion = 3,
    id = "portugal-1983-exposicion-europea-de-arte",
    name = "XVII Exposición Europea de Arte · Portugal 1983",
    issuerCode = "portugal",
    family = "XVII Exposición Europea de Arte de 1983",
    source = "https://en.numista.com/catalogue/series.php?id=6598",
    updatedAt = "2026-07-30",
    members = listOf(
        CollectionCatalogMember("500-escudos", "500 escudos · 7 g", 1983, 22_178),
        CollectionCatalogMember("750-escudos", "750 escudos · 12,5 g", 1983, 22_179),
        CollectionCatalogMember("1000-escudos", "1000 escudos · 21 g", 1983, 22_180),
    ),
)

internal fun portugueseAnnualCatalogStub() = CollectionCatalog(
    schemaVersion = 1,
    id = "portugal-500-escudos-plata-500",
    name = "Conjunto anual · 500 escudos de plata .500 · Portugal 1995-2001",
    issuerCode = "portugal",
    family = "500 escudos conmemorativos de plata .500 de Portugal",
    weightMillioz = 450,
    finish = null,
    source = "https://en.numista.com/catalogue/series.php?id=6598",
    updatedAt = "2026-07-30",
    members = listOf(
        CollectionCatalogMember("1995-santo-antonio", "Santo António", 1995, 13_042),
        CollectionCatalogMember("2001-oporto", "Oporto", 2001, 13_046),
    ),
)

private fun item(id: Long, typeId: Int, quantity: Int = 1) =
    CollectedItem(id = id, quantity = quantity, typeId = typeId)

private fun datedItem(id: Long, typeId: Int, issueYear: Int?) =
    CollectedItem(id = id, quantity = 1, typeId = typeId, issueYear = issueYear)

class CollectionCatalogValidationTest {
    @Test
    fun `validation requires versioned slugged unique sourced exact variants`() {
        val definition = teslaCatalogStub()
        assertEquals(
            CollectionProposalKey.fromCanonicalParts("Nikola Tesla", 1_000, "unknown"),
            definition.key(),
        )
        assertNull(definition.validate())

        val duplicateMemberId = definition.copy(
            members = definition.members.mapIndexed { index, member ->
                if (index == 1) member.copy(id = definition.members[0].id) else member
            },
        )
        assertTrue(duplicateMemberId.validate()!!.message.contains("duplicated"))

        val duplicateTypeId = definition.copy(
            members = definition.members.mapIndexed { index, member ->
                if (index == 1) {
                    member.copy(numistaTypeId = definition.members[0].numistaTypeId)
                } else {
                    member
                }
            },
        )
        assertTrue(duplicateTypeId.validate()!!.message.contains("150352"))

        val foreignSource = definition.copy(source = "https://example.com/catalog")
        assertTrue(foreignSource.validate()!!.message.contains("Numista series"))

        val unnormalizedFamily = definition.copy(family = " Nikola Tesla")
        assertTrue(unnormalizedFamily.validate()!!.message.contains("variant key"))

        assertEquals(
            CollectionCatalogValidationError.UnsupportedSchemaVersion(4),
            definition.copy(schemaVersion = 4).validate(),
        )
        assertEquals(
            CollectionCatalogValidationError.MissingWeight,
            definition.copy(weightMillioz = null).validate(),
        )
        assertEquals(
            CollectionCatalogValidationError.EmptyMembers,
            definition.copy(members = emptyList()).validate(),
        )
        assertEquals(
            CollectionCatalogValidationError.InvalidNumistaTypeId,
            definition.copy(
                members = listOf(definition.members[0].copy(numistaTypeId = 0)),
            ).validate(),
        )
        assertEquals(
            CollectionCatalogValidationError.InvalidId("catalog", "Nikola_Tesla"),
            definition.copy(id = "Nikola_Tesla").validate(),
        )
    }

    @Test
    fun `date runs repeat one type across years and accept type page sources`() {
        val definition = dateRunCatalogStub()
        assertNull(definition.validate())

        val duplicatedYear = definition.copy(
            members = listOf(
                definition.members[0],
                definition.members[1].copy(id = "1904-bis", year = 1904),
            ),
        )
        assertEquals(
            CollectionCatalogValidationError.DuplicateMemberYear(10_340, 1904),
            duplicatedYear.validate(),
        )

        val seriesSourced = definition.copy(
            source = "https://en.numista.com/catalogue/series.php?id=11467",
        )
        assertNull(seriesSourced.validate())

        val badSource = definition.copy(source = "https://en.numista.com/10340")
        assertEquals(CollectionCatalogValidationError.InvalidSource, badSource.validate())

        // A type-page source is only legitimate for a date run.
        val typePageOnV1 = definition.copy(
            schemaVersion = 1,
            members = definition.members.take(1),
        )
        assertEquals(CollectionCatalogValidationError.InvalidSource, typePageOnV1.validate())
    }

    @Test
    fun `a set declares no physical variant and keys on an absent weight`() {
        val definition = setCatalogStub()
        assertNull(definition.validate())
        assertTrue(definition.isSet)
        assertFalse(definition.isDateRun)

        val key = definition.key()
        assertNull(key.weightMillioz)
        assertNull(key.finish)
        assertEquals(SPANNING_VARIANTS_WEIGHT, key.storedWeightMillioz())
        assertEquals(
            key,
            CollectionProposalKey.fromCanonicalParts(
                definition.family,
                SPANNING_VARIANTS_WEIGHT,
                "unknown",
            ),
        )

        assertEquals(
            CollectionCatalogValidationError.SetDeclaresVariant,
            definition.copy(weightMillioz = 450).validate(),
        )
        assertEquals(
            CollectionCatalogValidationError.SetDeclaresVariant,
            definition.copy(finish = Finish.Proof).validate(),
        )
        // A set names each type once: it is a set, not a date run.
        assertEquals(
            CollectionCatalogValidationError.DuplicateNumistaTypeId(22_178),
            definition.copy(
                members = definition.members + definition.members[0].copy(id = "bis"),
            ).validate(),
        )
        // Zero stays an invalid weight, so a defaulted row is ignored rather than read as a set.
        assertNull(CollectionProposalKey.fromCanonicalParts(definition.family, 0, "unknown"))
        // A set spans finishes too, so a stored finish makes the key uncanonical.
        assertNull(
            CollectionProposalKey.fromCanonicalParts(
                definition.family,
                SPANNING_VARIANTS_WEIGHT,
                "proof",
            ),
        )
    }

    @Test
    fun `a set album counts owned members across denominations`() {
        val definition = setCatalogStub()
        val padre = buildCollectionCatalogAlbum(
            definition,
            listOf(item(1, 22_178, 2), item(2, 22_179, 2), item(3, 22_180, 2)),
        )
        assertEquals(3, padre.ownedMembers())
        assertTrue(definition.isEvidencedBy(listOf(item(4, 22_180))))

        val partial = buildCollectionCatalogAlbum(definition, listOf(item(5, 22_178)))
        assertEquals(1, partial.ownedMembers())
        assertEquals(CollectionCatalogMemberStatus.Missing, partial.members[1].status)
    }
}

class CollectionCatalogAlbumTest {
    @Test
    fun `album uses exact type ids sums quantity and isolates supplied holdings`() {
        val definition = teslaCatalogStub()
        assertTrue(definition.isEvidencedBy(listOf(item(9, 195_591))))
        assertFalse(definition.isEvidencedBy(listOf(item(10, 999_999))))

        val jose = buildCollectionCatalogAlbum(
            definition,
            listOf(item(1, 195_591), item(2, 195_591, quantity = 2), item(3, 999_999, quantity = 7)),
        )
        val padre = buildCollectionCatalogAlbum(definition, emptyList())

        assertEquals(2, jose.members.size)
        assertEquals(CollectionCatalogMemberStatus.Missing, jose.members[0].status)
        val owned = jose.members[1].status as? CollectionCatalogMemberStatus.Owned
        assertNotNull(owned)
        assertEquals(3, owned.quantity)
        assertEquals(2, owned.items.size)
        assertEquals(1, jose.ownedMembers())
        assertEquals(0, padre.ownedMembers())
        assertTrue(
            padre.members.all { it.status == CollectionCatalogMemberStatus.Missing },
        )
    }

    @Test
    fun `date run album matches by issue year and undated pieces never fill a year`() {
        val definition = dateRunCatalogStub()
        val album = buildCollectionCatalogAlbum(
            definition,
            listOf(
                datedItem(1, 10_340, 1904),
                datedItem(2, 10_340, null),
                datedItem(3, 10_340, 1910),
            ),
        )

        assertEquals(1, album.ownedMembers())
        val owned = album.members[0].status as? CollectionCatalogMemberStatus.Owned
        assertNotNull(owned)
        assertEquals(1, owned.quantity)
        assertEquals(CollectionCatalogMemberStatus.Missing, album.members[1].status)

        // A gregorian year also fills a slot when the issue year is absent.
        val gregorian = buildCollectionCatalogAlbum(
            definition,
            listOf(CollectedItem(id = 5, quantity = 1, typeId = 10_340, gregorianYear = 1905)),
        )
        assertEquals(1, gregorian.ownedMembers())

        // Plate reachability stays type-based even while every year is missing.
        assertTrue(definition.isEvidencedBy(listOf(datedItem(4, 10_340, null))))
    }
}
