package com.jenarvaezg.coindex.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun ounces(grams: Double): Double = grams / GRAMS_PER_TROY_OUNCE

private fun item(id: Long, typeId: Int, quantity: Int) =
    CollectedItem(id = id, quantity = quantity, typeId = typeId)

private fun metadata(id: Int, family: String, grams: Double, finish: Finish?) = TypeMeta(
    id = id,
    family = family,
    weightOz = ounces(grams),
    finish = finish,
)

class WeightNormalizationTest {
    @Test
    fun `common ounce weights snap without turning thirty grams into one ounce`() {
        assertEquals(250, normalizeWeightMillioz(ounces(7.777)))
        assertEquals(1_000, normalizeWeightMillioz(ounces(31.1)))
        assertEquals(1_000, normalizeWeightMillioz(ounces(31.21)))
        assertEquals(2_000, normalizeWeightMillioz(ounces(62.42)))
        assertEquals(965, normalizeWeightMillioz(ounces(30.0)))
    }

    @Test
    fun `non positive and non finite weights are rejected`() {
        assertNull(normalizeWeightMillioz(0.0))
        assertNull(normalizeWeightMillioz(-1.0))
        assertNull(normalizeWeightMillioz(Double.NaN))
        assertNull(normalizeWeightMillioz(Double.POSITIVE_INFINITY))
        assertNull(normalizeWeightMillioz(0.0001))
    }

    @Test
    fun `a curated weight pulls its near misses in without disturbing bullion`() {
        val curated = setOf(450, 579)

        // The 13,96 g Porto 500 escudos joins its 14 g siblings instead of splitting at 449.
        assertEquals(450, normalizeWeightMillioz(ounces(13.96), curated))
        assertEquals(450, normalizeWeightMillioz(ounces(14.0), curated))
        // Without the catalog it stays exactly where Numista puts it.
        assertEquals(449, normalizeWeightMillioz(ounces(13.96)))
        // Out of tolerance is left alone: 13,5 g is 434, eleven off the target.
        assertEquals(434, normalizeWeightMillioz(ounces(13.5), curated))
        // Bullion snapping is untouched, and 30 g still refuses to become an ounce.
        assertEquals(1_000, normalizeWeightMillioz(ounces(31.1), curated))
        assertEquals(965, normalizeWeightMillioz(ounces(30.0), curated))
        // The nearest target wins when two are in range.
        assertEquals(1_000, normalizeWeightMillioz(ounces(31.1), setOf(995)))
        assertEquals(995, normalizeWeightMillioz(ounces(30.95), setOf(995)))
    }
}

class CollectionProposalsTest {
    @Test
    fun `proposals group exact normalized family weight and finish variants`() {
        val items = listOf(item(1, 10, 2), item(2, 11, 3), item(3, 12, 1))
        val typeMeta = mapOf(
            10 to metadata(10, " Lunar   ounce ", 31.1, null),
            11 to metadata(11, "Lunar ounce", 31.21, null),
            12 to metadata(12, "Lunar ounce", 31.1, Finish.Bullion),
        )

        val proposals = buildCollectionProposals(items, typeMeta, emptyList())

        assertEquals(2, proposals.size)
        val unconfirmed = proposals.first { it.finish == null }
        assertEquals("Lunar ounce", unconfirmed.family)
        assertEquals(1_000, unconfirmed.weightMillioz)
        assertEquals(2, unconfirmed.distinctTypes)
        assertEquals(5, unconfirmed.quantity)
        val bullion = proposals.first { it.finish == Finish.Bullion }
        assertEquals(1, bullion.distinctTypes)
        assertEquals(1, bullion.quantity)
    }

    @Test
    fun `proposals are isolated to the supplied inventory without fuzzy families`() {
        val typeMeta = mapOf(
            10 to metadata(10, "Lunar Series III", 7.777, Finish.Coloured),
            20 to metadata(20, "Lunar ounce", 31.1, Finish.Bullion),
        )

        val firstUser = buildCollectionProposals(listOf(item(1, 10, 1)), typeMeta, emptyList())
        val secondUser = buildCollectionProposals(listOf(item(2, 20, 2)), typeMeta, emptyList())

        assertEquals(1, firstUser.size)
        assertEquals("Lunar Series III", firstUser[0].family)
        assertEquals(250, firstUser[0].weightMillioz)
        assertEquals(Finish.Coloured, firstUser[0].finish)
        assertEquals(1, secondUser.size)
        assertEquals("Lunar ounce", secondUser[0].family)
        assertFalse(secondUser.any { it.family == "Lunar Series III" })
    }

    @Test
    fun `curated variants also surface as proposals and unknown finish stays separate`() {
        val typeMeta = mapOf(
            10 to metadata(10, "Lunar Series III", 31.1, Finish.Bullion),
            11 to metadata(11, "Lunar Series III", 7.777, Finish.Coloured),
            12 to metadata(12, "Lunar Series III", 31.1, null),
        )

        val proposals = buildCollectionProposals(
            listOf(item(1, 10, 1), item(2, 11, 1), item(3, 12, 1)),
            typeMeta,
            emptyList(),
        )

        assertEquals(3, proposals.size)
        assertTrue(proposals.any { it.weightMillioz == 250 && it.finish == Finish.Coloured })
        assertTrue(proposals.any { it.weightMillioz == 1_000 && it.finish == null })
        assertTrue(proposals.any { it.weightMillioz == 1_000 && it.finish == Finish.Bullion })
    }

    @Test
    fun `editorial aliases preserve raw keys and technical systems group as the weakest family`() {
        val aliases = listOf(
            "SML" to "Silver Maple Leaf",
            "Red Data Book" to "Libro Rojo de Rusia",
            "Serie de monedas de plata obtenidas a valor facial" to
                "Monedas españolas de plata a valor facial",
            "Lunar ounce" to "Rwanda Lunar Ounce",
            "Nautical Ounce" to "Rwanda Nautical Ounce",
            "System 1981-2001" to "Sistema monetario 1981-2001",
            "System 2025" to "Sistema monetario 2025",
        )
        for ((raw, display) in aliases) {
            assertEquals(display, collectionProposalFamilyLabel(raw))
        }
        assertEquals("sml", collectionProposalFamilyLabel("sml"))
        assertEquals("System of a Down", collectionProposalFamilyLabel("System of a Down"))
        assertEquals("System 19-2001", collectionProposalFamilyLabel("System 19-2001"))

        val typeMeta = mapOf(
            10 to metadata(10, "System 2025", 31.1, null),
            11 to metadata(11, "System 1927-1968", 31.1, null),
            12 to metadata(12, "System 1969-1980-2001", 31.1, null),
            20 to metadata(20, "System of a Down", 31.1, null),
            21 to metadata(21, "System 19-2001", 31.1, null),
        )

        val proposals = buildCollectionProposals(
            listOf(item(1, 10, 1), item(2, 11, 1), item(3, 12, 1), item(4, 20, 1), item(5, 21, 1)),
            typeMeta,
            emptyList(),
        )

        // A technical family no longer costs the piece its proposal (ADR 0012); the raw value
        // stays in the key, and only the label reads as a monetary system.
        assertEquals(
            listOf(
                "System 19-2001",
                "System 1927-1968",
                "System 1969-1980-2001",
                "System 2025",
                "System of a Down",
            ),
            proposals.map { it.family },
        )
    }

    @Test
    fun `a curated catalog outranks a technical family and a set outranks a real one`() {
        val setCatalog = setCatalogStub()
        val annual = portugueseAnnualCatalogStub()
        fun portuguese(id: Int, grams: Double, family: String? = "System 1981-2001") = TypeMeta(
            id = id,
            title = "Escudos",
            family = family,
            issuerCode = "portugal",
            weightOz = ounces(grams),
        )
        val typeMeta = mapOf(
            13_042 to portuguese(13_042, 14.0),
            13_046 to portuguese(13_046, 13.96),
            22_178 to portuguese(22_178, 7.0),
            22_179 to portuguese(22_179, 12.5),
            // A set catalog claims its types even against a real Numista family.
            22_180 to portuguese(22_180, 21.0, family = "Familia real de Numista"),
            9_830 to portuguese(9_830, 7.0),
        )

        val derivation = deriveCollection(
            listOf(
                item(1, 13_042, 1),
                item(2, 13_046, 1),
                item(3, 22_178, 2),
                item(4, 22_179, 2),
                item(5, 22_180, 2),
                item(6, 9_830, 1),
            ),
            typeMeta,
            listOf(setCatalog, annual),
        )

        assertTrue(derivation.unclassified.isEmpty())
        val set = derivation.proposals.first { it.family == setCatalog.family }
        assertNull(set.weightMillioz)
        assertNull(set.finish)
        assertEquals(3, set.distinctTypes)
        assertEquals(6, set.quantity)

        // Both 500 escudos land on the catalog's weight, 13,96 g included.
        val annualProposal = derivation.proposals.first { it.family == annual.family }
        assertEquals(450, annualProposal.weightMillioz)
        assertEquals(2, annualProposal.distinctTypes)

        // The piece no catalog claims keeps its technical family and its own weight.
        val leftover = derivation.proposals.first { it.family == "System 1981-2001" }
        assertEquals(225, leftover.weightMillioz)
        assertEquals(1, leftover.distinctTypes)

        assertEquals(setCatalog.key(), set.key())
        assertEquals(annual.key(), annualProposal.key())
    }

    @Test
    fun `canonical keys round trip finish codes and stale preferences stay dormant`() {
        fun proposal(family: String, finish: Finish?) = CollectionProposal(
            family = family,
            weightMillioz = 1_000,
            finish = finish,
            distinctTypes = 1,
            quantity = 1,
        )
        val followed = proposal("SML", Finish.ProofColoured)
        val available = proposal("Lunar ounce", null)
        val ignored = proposal("Nautical Ounce", Finish.Bullion)
        val stale = CollectionProposalKey.fromCanonicalParts("Red Data Book", 2_000, "proof")
        assertNotNull(stale)
        val preferences = listOf(
            CollectionProposalPreference(followed.key(), ProposalDisposition.Followed),
            CollectionProposalPreference(ignored.key(), ProposalDisposition.Ignored),
            CollectionProposalPreference(stale, ProposalDisposition.Followed),
        )

        assertEquals("proof_coloured", followed.key().finishCode())
        assertNull(
            CollectionProposalKey.fromCanonicalParts("Lunar ounce", 1_000, "unknown")!!.finish,
        )
        assertNull(CollectionProposalKey.fromCanonicalParts(" Lunar ounce", 1_000, "unknown"))
        assertNull(CollectionProposalKey.fromCanonicalParts("Lunar ounce", 0, "unknown"))
        assertNull(CollectionProposalKey.fromCanonicalParts("Lunar ounce", 1_000, "Proof"))

        val classified = classifyCollectionProposals(
            listOf(followed, available, ignored),
            preferences,
        )

        assertEquals(1, classified.followed.size)
        assertEquals(1, classified.available.size)
        assertEquals(1, classified.ignored.size)
        assertEquals("SML", classified.followed[0].family)
        assertEquals("Lunar ounce", classified.available[0].family)
        assertEquals("Nautical Ounce", classified.ignored[0].family)
    }

    @Test
    fun `catalog family fallback rescues types numista leaves family-less`() {
        val catalog = dateRunCatalogStub()
        fun familyLess(id: Int) = TypeMeta(
            id = id,
            title = "5 Bolívares",
            family = null,
            issuerCode = "venezuela",
            minYear = 1879,
            maxYear = 1936,
            weightOz = ounces(25.0),
        )
        val typeMeta = mapOf(10_340 to familyLess(10_340), 99 to familyLess(99))

        val derivation = deriveCollection(
            listOf(item(1, 10_340, 2), item(2, 99, 1)),
            typeMeta,
            listOf(catalog),
        )

        // Only the catalog-referenced type is rescued; the rest stays unclassified.
        assertEquals(1, derivation.proposals.size)
        assertEquals("5 Bolívares de Venezuela", derivation.proposals[0].family)
        assertEquals(804, derivation.proposals[0].weightMillioz)
        assertEquals(2, derivation.proposals[0].quantity)
        assertEquals(1, derivation.unclassified.size)
        assertEquals(99, derivation.unclassified[0].item.typeId)
        assertEquals(UnclassifiedReason.NoFamilyOrCatalog, derivation.unclassified[0].reason)

        // The real Numista family always beats the catalog fallback.
        val withFamily = mapOf(10_340 to familyLess(10_340).copy(family = "Familia oficial"))
        val proposals = buildCollectionProposals(
            listOf(item(1, 10_340, 1)),
            withFamily,
            listOf(catalog),
        )
        assertEquals("Familia oficial", proposals[0].family)
    }

    @Test
    fun `every ungrouped piece is preserved with an auditable reason`() {
        val typeMeta = mapOf(
            12 to TypeMeta(id = 12, family = "Sin peso", weightOz = null),
            13 to TypeMeta(id = 13, family = null, weightOz = ounces(31.1)),
        )

        val derivation = deriveCollection(
            listOf(
                item(1, 10, 1),
                item(3, 12, 1),
                item(4, 13, 1),
                item(5, 12, 0),
            ),
            typeMeta,
            emptyList(),
        )

        assertTrue(derivation.proposals.isEmpty())
        assertEquals(
            listOf(
                UnclassifiedReason.MissingTypeMetadata,
                UnclassifiedReason.UnknownWeight("Sin peso"),
                UnclassifiedReason.NoFamilyOrCatalog,
            ),
            derivation.unclassified.map { it.reason },
        )
        // A piece the collector no longer owns is not a proposal and not an orphan either.
        assertFalse(derivation.unclassified.any { it.item.id == 5L })
    }
}
