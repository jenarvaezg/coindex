package com.jenarvaezg.coindex.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun ounces(grams: Double): Double = grams / GRAMS_PER_TROY_OUNCE

private fun item(id: Long, typeId: Int, quantity: Int) =
    CollectedItem(id = id, quantity = quantity, typeId = typeId)

// Every published Numista page has a year; without one the type reads as a submission in review
// and derives no collection (#186).
private fun metadata(id: Int, family: String, grams: Double, finish: Finish?) = TypeMeta(
    id = id,
    family = family,
    minYear = 2020,
    maxYear = 2020,
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

    /**
     * Only the common bullion weights are targets. A catalog's members take their key from the file
     * (ADR 0016), so its declared weight has nothing to snap (#288).
     */
    @Test
    fun `a weight a catalog declares is not a snapping target`() {
        // The 13,96 g Porto 500 escudos stays at 449 beside its siblings' 450; the catalog that
        // names all seven is what joins them.
        assertEquals(449, normalizeWeightMillioz(ounces(13.96)))
        assertEquals(450, normalizeWeightMillioz(ounces(14.0)))
        // 26,73 g is the Morgan dollar's legal weight, not a rounding slip.
        assertEquals(859, normalizeWeightMillioz(ounces(26.73)))
    }
}

class CollectionDerivationTest {
    @Test
    fun `derived collections group exact normalized family weight and finish variants`() {
        val items = listOf(item(1, 10, 2), item(2, 11, 3), item(3, 12, 1))
        val typeMeta = mapOf(
            10 to metadata(10, " Lunar   ounce ", 31.1, null),
            11 to metadata(11, "Lunar ounce", 31.21, null),
            12 to metadata(12, "Lunar ounce", 31.1, Finish.Bullion),
        )

        val derivedCollections = deriveCollection(items, typeMeta, emptyList()).derivedCollections

        assertEquals(2, derivedCollections.size)
        val unconfirmed = derivedCollections.first { it.finish == null }
        assertEquals("Lunar ounce", unconfirmed.family)
        assertEquals(1_000, unconfirmed.weightMillioz)
        assertEquals(2, unconfirmed.distinctTypes)
        assertEquals(5, unconfirmed.quantity)
        val bullion = derivedCollections.first { it.finish == Finish.Bullion }
        assertEquals(1, bullion.distinctTypes)
        assertEquals(1, bullion.quantity)
    }

    @Test
    fun `derived collections are isolated to the supplied inventory without fuzzy families`() {
        val typeMeta = mapOf(
            10 to metadata(10, "Lunar Series III", 7.777, Finish.Coloured),
            20 to metadata(20, "Lunar ounce", 31.1, Finish.Bullion),
        )

        val firstUser = deriveCollection(listOf(item(1, 10, 1)), typeMeta, emptyList()).derivedCollections
        val secondUser = deriveCollection(listOf(item(2, 20, 2)), typeMeta, emptyList()).derivedCollections

        assertEquals(1, firstUser.size)
        assertEquals("Lunar Series III", firstUser[0].family)
        assertEquals(250, firstUser[0].weightMillioz)
        assertEquals(Finish.Coloured, firstUser[0].finish)
        assertEquals(1, secondUser.size)
        assertEquals("Lunar ounce", secondUser[0].family)
        assertFalse(secondUser.any { it.family == "Lunar Series III" })
    }

    @Test
    fun `curated variants also surface as derived collections and unknown finish stays separate`() {
        val typeMeta = mapOf(
            10 to metadata(10, "Lunar Series III", 31.1, Finish.Bullion),
            11 to metadata(11, "Lunar Series III", 7.777, Finish.Coloured),
            12 to metadata(12, "Lunar Series III", 31.1, null),
        )

        val derivedCollections = deriveCollection(
            listOf(item(1, 10, 1), item(2, 11, 1), item(3, 12, 1)),
            typeMeta,
            emptyList(),
        ).derivedCollections

        assertEquals(3, derivedCollections.size)
        assertTrue(
            derivedCollections.any { it.weightMillioz == 250 && it.finish == Finish.Coloured },
        )
        assertTrue(derivedCollections.any { it.weightMillioz == 1_000 && it.finish == null })
        assertTrue(
            derivedCollections.any { it.weightMillioz == 1_000 && it.finish == Finish.Bullion },
        )
    }

    @Test
    fun `a raw family prints verbatim and technical systems group as the weakest family`() {
        // A family no curated file claims prints as Numista wrote it (#22), except a generated
        // monetary system (ADR 0012) and the series of the cured table (ADR 0031). Gothic Horror
        // and DC Comics stay in English on purpose.
        //
        // `Charlemagme` is a stale spelling a phone may still hold: the table is keyed on the
        // corrected string, so the typo prints until that ficha is fetched again (ADR 0025).
        val labels = listOf(
            "SML" to "SML",
            "Red Data Book" to "Red Data Book",
            "Serie de monedas de plata obtenidas a valor facial" to
                "Serie de monedas de plata obtenidas a valor facial",
            "Lunar ounce" to "Lunar ounce",
            "Nautical Ounce" to "Nautical Ounce",
            "Gothic Horror" to "Gothic Horror",
            "DC Comics" to "DC Comics",
            "Charlemagme - Mounted Knight" to "Charlemagme - Mounted Knight",
            "Charlemagne - Mounted Knight" to "Carlomagno · el caballero",
            "Hercules type" to "Tipo Hércules",
            "French regions" to "Euros de las regiones francesas",
            "1190e anniversaire du couronnement de Charlemagne (800-1990)." to "Carlomagno",
            "Millennium" to "Milenio",
            "System 1981-2001" to "Sistema monetario 1981-2001",
            "System 2025" to "Sistema monetario 2025",
        )
        for ((raw, display) in labels) {
            assertEquals(display, familyLabel(raw))
        }
        assertEquals("sml", familyLabel("sml"))
        assertEquals("System of a Down", familyLabel("System of a Down"))
        assertEquals("System 19-2001", familyLabel("System 19-2001"))

        val typeMeta = mapOf(
            10 to metadata(10, "System 2025", 31.1, null),
            11 to metadata(11, "System 1927-1968", 31.1, null),
            12 to metadata(12, "System 1969-1980-2001", 31.1, null),
            20 to metadata(20, "System of a Down", 31.1, null),
            21 to metadata(21, "System 19-2001", 31.1, null),
        )

        val derivedCollections = deriveCollection(
            listOf(item(1, 10, 1), item(2, 11, 1), item(3, 12, 1), item(4, 20, 1), item(5, 21, 1)),
            typeMeta,
            emptyList(),
        ).derivedCollections

        // A technical family still gets a card (ADR 0012): the raw value stays in the key and
        // only the label is formatted.
        assertEquals(
            listOf(
                "System 19-2001",
                "System 1927-1968",
                "System 1969-1980-2001",
                "System 2025",
                "System of a Down",
            ),
            derivedCollections.map { it.family },
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
            minYear = 1992,
            maxYear = 1992,
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
        val set = derivation.derivedCollections.first { it.family == setCatalog.family }
        assertNull(set.weightMillioz)
        assertNull(set.finish)
        assertEquals(3, set.distinctTypes)
        assertEquals(6, set.quantity)

        // Both 500 escudos land on the catalog's weight, 13,96 g included.
        val annualCollection = derivation.derivedCollections.first { it.family == annual.family }
        assertEquals(450, annualCollection.weightMillioz)
        assertEquals(2, annualCollection.distinctTypes)

        // The piece no catalog claims keeps its technical family and its own weight.
        val leftover = derivation.derivedCollections.first { it.family == "System 1981-2001" }
        assertEquals(225, leftover.weightMillioz)
        assertEquals(1, leftover.distinctTypes)

        assertEquals(setCatalog.key(), set.key())
        assertEquals(annual.key(), annualCollection.key())
    }

    /**
     * Numista records the Portuguese 1000 escudos as 27, 28 and 28.2 g: three keys for one coin.
     * Snapping can't close a 32 milli-ounce gap without reading a 30 g piece as an ounce, so the
     * file naming all three holds them together (ADR 0016, #288).
     */
    @Test
    fun `a catalog claims its members whatever weight numista records for each`() {
        val catalog = portugueseAnnualCatalogStub().copy(
            id = "portugal-1000-escudos-plata-500",
            family = "1000 escudos conmemorativos de plata .500 de Portugal",
            weightMillioz = 900,
            members = listOf(
                CollectionCatalogMember("1992-dos-mundos", "Dos Mundos", 1992, 15_463),
                CollectionCatalogMember("1995-juan-ii", "D. João II", 1995, 11_697),
                CollectionCatalogMember("1997-pauliteiros", "Pauliteiros", 1997, 11_120),
            ),
        )
        fun escudo(id: Int, grams: Double) = TypeMeta(
            id = id,
            title = "1000 Escudos",
            family = "System 1981-2001",
            issuerCode = "portugal",
            minYear = 1992,
            maxYear = 1992,
            weightOz = ounces(grams),
        )
        val typeMeta = mapOf(
            15_463 to escudo(15_463, 27.0),
            11_697 to escudo(11_697, 28.0),
            11_120 to escudo(11_120, 28.2),
        )
        // Left to their grams the three are three different keys, declared weight or not.
        assertEquals(868, normalizeWeightMillioz(ounces(27.0)))
        assertEquals(900, normalizeWeightMillioz(ounces(28.0)))
        assertEquals(907, normalizeWeightMillioz(ounces(28.2)))

        val derivation = deriveCollection(
            listOf(item(1, 15_463, 1), item(2, 11_697, 1), item(3, 11_120, 1)),
            typeMeta,
            listOf(catalog),
        )

        assertTrue(derivation.unclassified.isEmpty())
        assertEquals(1, derivation.derivedCollections.size)
        val collection = derivation.derivedCollections.single()
        assertEquals(catalog.key(), collection.key())
        assertEquals(3, collection.distinctTypes)
        assertEquals(3, collection.quantity)
    }

    /**
     * The unclaimed Morgan dollar (26.73 g, 859) used to snap to the 868 a Spanish catalog
     * declares, and was weighed 859 in its shelf row and 868 in its card's key (#288).
     */
    @Test
    fun `a declared weight does not reach a type its catalog does not claim`() {
        val catalog = portugueseAnnualCatalogStub().copy(
            id = "us-independence-250th-spain-10-euros",
            family = "250th anniversary of the United States Declaration of Independence",
            weightMillioz = 868,
            members = listOf(
                CollectionCatalogMember("2026-declaration", "Declaración", 2026, 500_001),
            ),
        )
        val typeMeta = mapOf(
            1_492 to metadata(1_492, "Dólar de plata clásico de EE. UU.", 26.73, null),
            // The catalog's own member, whose grams Numista records a shade off the declared 868.
            500_001 to metadata(500_001, "250 aniversario", 26.9, null),
        )

        val derivation = deriveCollection(
            listOf(item(1, 1_492, 1), item(2, 500_001, 1)),
            typeMeta,
            listOf(catalog),
        )

        val morgan = derivation.derivedCollections.single { it.family != catalog.family }
        assertEquals("Dólar de plata clásico de EE. UU.", morgan.family)
        assertEquals(859, morgan.weightMillioz)
        // The declared 868 still rules the member the catalog names.
        val declaration = derivation.derivedCollections.single { it.family == catalog.family }
        assertEquals(catalog.key(), declaration.key())
        assertEquals(868, declaration.weightMillioz)
    }

    /** La clave no se persiste (ADR 0021 §5), pero identifica la tarjeta sin fichero y su ruta. */
    @Test
    fun `canonical keys round trip finish codes and refuse anything uncanonical`() {
        val proofColoured = DerivedCollection(
            family = "SML",
            weightMillioz = 1_000,
            finish = Finish.ProofColoured,
            metal = Metal.Silver,
            distinctTypes = 1,
            quantity = 1,
        )

        assertEquals("proof_coloured", proofColoured.key().finishCode())
        assertNotNull(VariantKey.fromCanonicalParts("Red Data Book", 2_000, "proof", "silver"))
        assertNull(
            VariantKey.fromCanonicalParts("Lunar ounce", 1_000, "unknown", "silver")!!.finish,
        )
        assertNull(VariantKey.fromCanonicalParts(" Lunar ounce", 1_000, "unknown", "silver"))
        assertNull(VariantKey.fromCanonicalParts("Lunar ounce", 0, "unknown", "silver"))
        assertNull(VariantKey.fromCanonicalParts("Lunar ounce", 1_000, "Proof", "silver"))
        // Un código de metal que nadie reconoce descalifica la clave igual que el acabado.
        assertNull(VariantKey.fromCanonicalParts("Lunar ounce", 1_000, "unknown", "Plata"))
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
        assertEquals(1, derivation.derivedCollections.size)
        assertEquals("5 Bolívares de Venezuela", derivation.derivedCollections[0].family)
        assertEquals(804, derivation.derivedCollections[0].weightMillioz)
        assertEquals(2, derivation.derivedCollections[0].quantity)
        assertEquals(1, derivation.unclassified.size)
        assertEquals(99, derivation.unclassified[0].item.typeId)
        assertEquals(UnclassifiedReason.NoFamilyOrCatalog, derivation.unclassified[0].reason)

    }

    @Test
    fun `date run type evidence keeps the catalog family before it can fill a year`() {
        val catalog = dateRunCatalogStub()
        val metadata = mapOf(
            10_340 to TypeMeta(
                id = 10_340,
                family = "Familia Numista distinta",
                minYear = 1936,
                maxYear = 1936,
                weightOz = ounces(25.0),
            ),
        )

        // ADR 0009 lets an undated holding open the plate by type without filling any dated slot.
        val derivedCollections = deriveCollection(
            listOf(item(1, 10_340, 1)),
            metadata,
            listOf(catalog),
        ).derivedCollections

        assertEquals(listOf(catalog.key()), derivedCollections.map { it.key() })
    }

    @Test
    fun `a matching catalog outranks a real Numista family without touching unclaimed types`() {
        val catalog = teslaCatalogStub().copy(
            family = "Lunar ounce",
            finish = Finish.ProofColoured,
            members = listOf(
                CollectionCatalogMember(
                    id = "2025-snake",
                    label = "Year of the Snake",
                    year = 2025,
                    numistaTypeId = 448_800,
                ),
            ),
        )
        val typeMeta = mapOf(
            448_800 to TypeMeta(
                id = 448_800,
                family = "Lunar ounce - Year of the Snake",
                minYear = 2025,
                maxYear = 2025,
                weightOz = ounces(31.1),
                finish = Finish.Bullion,
                metal = Metal.Silver,
            ),
            // Published and unclaimed by any catalog: Numista's data stays sovereign (#38).
            470_766 to TypeMeta(
                id = 470_766,
                family = "Pillar Dollar",
                minYear = 2025,
                maxYear = 2025,
                weightOz = ounces(31.1),
                finish = Finish.Bullion,
                metal = Metal.Silver,
            ),
        )

        val derivedCollections = deriveCollection(
            listOf(item(1, 448_800, 1), item(2, 470_766, 1)),
            typeMeta,
            listOf(catalog),
        ).derivedCollections

        assertEquals(
            listOf(
                catalog.key(),
                VariantKey("Pillar Dollar", 1_000, Finish.Bullion, Metal.Silver),
            ),
            derivedCollections.map { it.key() },
        )
    }

    @Test
    fun `an unpublished submission derives no collection, whatever family it half-declares`() {
        // #38 let such a card stand until publication, but a cached type is never fetched again
        // (#185). The fixture is invented: the shield is a missing year (`looksUnpublished`,
        // Inventory.kt), and no yearless ficha in the seeded cache declares a family.
        val submission = TypeMeta(
            id = 999_001,
            title = "Una propuesta cualquiera a la espera de árbitro",
            family = "Wedge-tailed Eagle",
            weightOz = ounces(31.1),
            finish = Finish.Bullion,
            metal = Metal.Silver,
        )

        val derivation = deriveCollection(
            listOf(item(1, 999_001, 1)),
            mapOf(999_001 to submission),
            emptyList(),
        )

        assertEquals(emptyList(), derivation.derivedCollections.map { it.key() })
        assertEquals(
            listOf(UnclassifiedReason.UnpublishedType),
            derivation.unclassified.map { it.reason },
        )
    }

    @Test
    fun `a family made only of articles is read as no family at all`() {
        // N#596807 as published: dated 2026, so not unpublished, but its `series` is «The» (#404).
        val published = TypeMeta(
            id = 596_807,
            title = "2 Pounds - Charles III (American Declaration of Independence; 1 oz Fine Silver",
            family = "The",
            minYear = 2026,
            maxYear = 2026,
            weightOz = ounces(31.1),
            finish = Finish.Bullion,
            metal = Metal.Silver,
        )

        val derivation = deriveCollection(
            listOf(item(1, 596_807, 1)),
            mapOf(596_807 to published),
            emptyList(),
        )

        assertEquals(emptyList(), derivation.derivedCollections.map { it.key() })
        assertEquals(
            listOf(UnclassifiedReason.NoFamilyOrCatalog),
            derivation.unclassified.map { it.reason },
        )
    }

    @Test
    fun `an article-only family is what a curated file and a grouping are for`() {
        val published = TypeMeta(
            id = 596_807,
            title = "2 Pounds - Charles III (American Declaration of Independence; 1 oz Fine Silver",
            family = "The",
            minYear = 2026,
            maxYear = 2026,
            weightOz = ounces(31.1),
            finish = Finish.Bullion,
            metal = Metal.Silver,
        )
        val grouping = CuratedGrouping(
            schemaVersion = 1,
            id = "declaration-of-independence",
            name = "Declaration of Independence",
            shortName = "Declaration",
            family = "Declaration of Independence",
            issuerCode = "royaume-uni",
            source = "https://en.numista.com/catalogue/pieces596807.html",
            updatedAt = "2026-08-11",
            typeIds = listOf(596_807),
        )

        val derivation = deriveCollection(
            listOf(item(1, 596_807, 1)),
            mapOf(596_807 to published),
            emptyList(),
            listOf(grouping),
        )

        // Once a curator names the coin's collection, it leaves the residue.
        assertEquals(emptyList(), derivation.unclassified.map { it.reason })
        assertEquals(
            listOf("Declaration of Independence"),
            derivation.derivedCollections.map { it.family },
        )
    }

    @Test
    fun `only a family with no word of its own is a placeholder`() {
        for (placeholder in listOf("The", "the", "La", "El", "Le", "De la", "The  of ")) {
            assertTrue(isPlaceholderFamily(placeholder), placeholder)
        }
        // Anything beyond an article is a name, and so is an initialism.
        for (family in listOf("The Royal Tudor Beasts", "Noah's Ark", "SML", "UN", "Disney", "Lunar ounce")) {
            assertFalse(isPlaceholderFamily(family), family)
        }
    }

    @Test
    fun `a curated catalog still claims a type whose page is unpublished`() {
        // The curated file outranks the check: a curator verified the type by hand.
        val catalog = teslaCatalogStub().copy(
            family = "The Royal Tudor Beasts",
            finish = Finish.Bullion,
            members = listOf(
                CollectionCatalogMember(
                    id = "2026-royal-dragon",
                    label = "Royal Dragon",
                    year = 2026,
                    numistaTypeId = 577_854,
                ),
            ),
        )
        val submission = TypeMeta(
            id = 577_854,
            family = "The",
            weightOz = ounces(31.1),
            finish = Finish.Bullion,
            metal = Metal.Silver,
        )

        val derivedCollections = deriveCollection(
            listOf(item(1, 577_854, 1)),
            mapOf(577_854 to submission),
            listOf(catalog),
        ).derivedCollections

        assertEquals(listOf(catalog.key()), derivedCollections.map { it.key() })
    }

    @Test
    fun `issue-qualified catalogs route a shared type to the exact member only`() {
        fun qualifiedCatalog(id: String, family: String, issueId: Int) = teslaCatalogStub().copy(
            id = id,
            name = family,
            shortName = family,
            family = family,
            finish = Finish.ProofColoured,
            members = listOf(
                CollectionCatalogMember(
                    id = "2020-rat",
                    label = "Rat",
                    year = 2020,
                    numistaTypeId = 400_000,
                    numistaIssueIds = listOf(issueId),
                ),
            ),
        )
        val oneOunce = qualifiedCatalog("lunar-1oz", "Lunar Series III", 70_001)
        val halfOunce = qualifiedCatalog("lunar-half-oz", "Lunar Series III", 70_002)
            .copy(weightMillioz = 500)
        val metadata = mapOf(
            400_000 to TypeMeta(
                id = 400_000,
                family = "Lunar Series III",
                weightOz = ounces(31.1),
                finish = Finish.ProofColoured,
                metal = Metal.Silver,
            ),
        )
        val matched = deriveCollection(
            listOf(
                CollectedItem(1, 1, 400_000, issueId = 70_001),
                CollectedItem(2, 1, 400_000, issueId = 70_002),
            ),
            metadata,
            listOf(oneOunce, halfOunce),
        )

        assertEquals(
            setOf(oneOunce.key(), halfOunce.key()),
            matched.derivedCollections.map { it.key() }.toSet(),
        )
        assertTrue(matched.unclassified.isEmpty())

        val unmatched = deriveCollection(
            listOf(
                CollectedItem(3, 1, 400_000, issueId = 70_999),
                CollectedItem(4, 1, 400_000),
            ),
            metadata,
            listOf(oneOunce, halfOunce),
        )
        assertTrue(unmatched.derivedCollections.isEmpty())
        assertEquals(2, unmatched.unclassified.size)
        assertTrue(
            unmatched.unclassified.all {
                it.reason == UnclassifiedReason.IssueNotClaimedByCatalog
            },
        )
    }

    @Test
    fun `overlapping catalog matches fail independently of catalog order`() {
        fun catalog(id: String, weightMillioz: Int) = teslaCatalogStub().copy(
            id = id,
            family = "Lunar Series III",
            weightMillioz = weightMillioz,
            members = listOf(
                CollectionCatalogMember(
                    id = "2020-rat",
                    label = "Rat",
                    year = 2020,
                    numistaTypeId = 400_000,
                    numistaIssueIds = listOf(70_001),
                ),
            ),
        )
        val first = catalog("lunar-one-ounce", 1_000)
        val second = catalog("lunar-half-ounce", 500)
        val owned = CollectedItem(1, 1, 400_000, issueId = 70_001)
        val metadata = mapOf(
            400_000 to TypeMeta(
                id = 400_000,
                family = "Lunar Series III",
                weightOz = ounces(31.1),
                metal = Metal.Silver,
            ),
        )

        for (catalogs in listOf(listOf(first, second), listOf(second, first))) {
            val failure = assertFailsWith<IllegalStateException> {
                deriveCollection(listOf(owned), metadata, catalogs)
            }
            assertTrue(failure.message!!.contains("lunar-half-ounce"))
            assertTrue(failure.message!!.contains("lunar-one-ounce"))
        }
    }

    /** A grouping only names types, so it is the weakest claim. */
    @Test
    fun `a curated grouping rescues family-less types and loses to every real claim`() {
        val grouping = CuratedGrouping(
            schemaVersion = 1,
            id = "venezuela-medios",
            name = "Medios",
            shortName = "Medios",
            family = "Medios de Venezuela",
            issuerCode = "venezuela",
            source = "https://en.numista.com/catalogue/pieces4369.html",
            updatedAt = "2026-07-30",
            typeIds = listOf(4_369, 9_488),
        )
        fun quarter(id: Int, family: String?) = TypeMeta(
            id = id,
            title = "¼ Bolívar",
            family = family,
            issuerCode = "venezuela",
            minYear = 1879,
            maxYear = 1936,
            weightOz = ounces(1.25),
        )

        // Two types Numista leaves family-less land on one card, because both weigh 1,25 g.
        val derivation = deriveCollection(
            listOf(item(1, 4_369, 3), item(2, 9_488, 59)),
            mapOf(4_369 to quarter(4_369, null), 9_488 to quarter(9_488, null)),
            emptyList(),
            listOf(grouping),
        )
        assertEquals(1, derivation.derivedCollections.size)
        assertEquals("Medios de Venezuela", derivation.derivedCollections[0].family)
        assertEquals(40, derivation.derivedCollections[0].weightMillioz)
        assertEquals(2, derivation.derivedCollections[0].distinctTypes)
        assertEquals(62, derivation.derivedCollections[0].quantity)
        assertTrue(derivation.unclassified.isEmpty())

        // A real Numista family wins; a technical monetary system does not.
        val real = deriveCollection(
            listOf(item(1, 4_369, 1)),
            mapOf(4_369 to quarter(4_369, "Familia oficial")),
            emptyList(),
            listOf(grouping),
        ).derivedCollections
        assertEquals("Familia oficial", real[0].family)
        val technical = deriveCollection(
            listOf(item(1, 4_369, 1)),
            mapOf(4_369 to quarter(4_369, "System 1879-1936")),
            emptyList(),
            listOf(grouping),
        ).derivedCollections
        assertEquals("Medios de Venezuela", technical[0].family)

        // And a catalog outranks the grouping, because it can also say what is missing.
        val catalog = dateRunCatalogStub()
        val catalogued = deriveCollection(
            listOf(item(1, 10_340, 1)),
            mapOf(10_340 to TypeMeta(id = 10_340, family = null, weightOz = ounces(25.0))),
            listOf(catalog),
            listOf(grouping.copy(id = "otra", typeIds = listOf(10_340))),
        ).derivedCollections
        assertEquals("5 Bolívares de Venezuela", catalogued[0].family)
    }

    /** The card's screen lists its rows: «los 5 paquillos» are five rows of one Numista type. */
    @Test
    fun `each derived collection keeps the pieces it was derived from`() {
        val typeMeta = mapOf(
            1_885 to TypeMeta(
                id = 1_885,
                family = "100 Pesetas de Franco",
                minYear = 1966,
                maxYear = 1970,
                weightOz = ounces(19.0),
            ),
            10 to metadata(10, "Otra familia", 31.1, Finish.Bullion),
        )
        val stars = (1966..1970).mapIndexed { index, year ->
            CollectedItem(id = index + 1L, quantity = 1, typeId = 1_885, issueYear = year)
        }

        val derivation = deriveCollection(stars + item(99, 10, 1), typeMeta, emptyList())

        val paquillos = derivation.derivedCollections.first { it.family == "100 Pesetas de Franco" }
        assertEquals(1, paquillos.distinctTypes)
        assertEquals(5, paquillos.quantity)
        assertEquals(
            (1966..1970).toList(),
            derivation.itemsByKey.getValue(paquillos.key()).map { it.recordedYear },
        )
        // Every derived collection has an entry, and no piece is counted under two keys.
        assertEquals(derivation.derivedCollections.size, derivation.itemsByKey.size)
        assertEquals(
            derivation.derivedCollections.sumOf { it.quantity },
            derivation.itemsByKey.values.sumOf { pieces -> pieces.sumOf { it.quantity } },
        )
    }

    @Test
    fun `every ungrouped piece is preserved with an auditable reason`() {
        val typeMeta = mapOf(
            12 to TypeMeta(id = 12, family = "Sin peso", minYear = 2020, weightOz = null),
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

        assertTrue(derivation.derivedCollections.isEmpty())
        assertEquals(
            listOf(
                UnclassifiedReason.MissingTypeMetadata,
                UnclassifiedReason.UnknownWeight("Sin peso"),
                UnclassifiedReason.NoFamilyOrCatalog,
            ),
            derivation.unclassified.map { it.reason },
        )
        // A piece the collector no longer owns is not a card and not an orphan either.
        assertFalse(derivation.unclassified.any { it.item.id == 5L })
    }

    @Test
    fun `unclassified residue collapses by type summing quantity`() {
        val typeMeta = mapOf(
            14_538 to TypeMeta(id = 14_538, family = null, weightOz = ounces(30.0)),
        )
        val derivation = deriveCollection(
            listOf(
                CollectedItem(1, 2, 14_538),
                CollectedItem(2, 1, 14_538),
            ),
            typeMeta,
            emptyList(),
        )

        val entry = derivation.unclassified.single()
        assertEquals(UnclassifiedReason.NoFamilyOrCatalog, entry.reason)
        assertEquals(1L, entry.item.id)
        assertEquals(3, entry.quantity)
        assertEquals(2, entry.rowCount)
    }

    @Test
    fun `issue-unclaimed residue stays split by issue of the same type`() {
        fun qualifiedCatalog(issueId: Int) = teslaCatalogStub().copy(
            id = "lunar-$issueId",
            name = "Lunar Series III",
            shortName = "Lunar Series III",
            family = "Lunar Series III",
            finish = Finish.ProofColoured,
            members = listOf(
                CollectionCatalogMember(
                    id = "2020-rat",
                    label = "Rat",
                    year = 2020,
                    numistaTypeId = 400_000,
                    numistaIssueIds = listOf(issueId),
                ),
            ),
        )
        val metadata = mapOf(
            400_000 to TypeMeta(
                id = 400_000,
                family = "Lunar Series III",
                weightOz = ounces(31.1),
                finish = Finish.ProofColoured,
                metal = Metal.Silver,
            ),
        )
        val derivation = deriveCollection(
            listOf(
                CollectedItem(1, 1, 400_000, issueId = 70_999),
                CollectedItem(2, 2, 400_000, issueId = 70_998),
                CollectedItem(3, 1, 400_000, issueId = 70_999),
            ),
            metadata,
            listOf(qualifiedCatalog(70_001)),
        )

        assertEquals(2, derivation.unclassified.size)
        assertTrue(
            derivation.unclassified.all { it.reason == UnclassifiedReason.IssueNotClaimedByCatalog },
        )
        val byIssue = derivation.unclassified.associateBy { it.item.issueId }
        assertEquals(2, byIssue.getValue(70_999).quantity)
        assertEquals(2, byIssue.getValue(70_999).rowCount)
        assertEquals(2, byIssue.getValue(70_998).quantity)
        assertEquals(1, byIssue.getValue(70_998).rowCount)
    }

    @Test
    fun `unclassified quantity saturates instead of wrapping`() {
        val typeMeta = mapOf(
            99 to TypeMeta(id = 99, family = null, weightOz = ounces(31.1)),
        )
        val derivation = deriveCollection(
            listOf(
                CollectedItem(1, Int.MAX_VALUE, 99),
                CollectedItem(2, 1, 99),
            ),
            typeMeta,
            emptyList(),
        )

        assertEquals(Int.MAX_VALUE, derivation.unclassified.single().quantity)
        assertEquals(2, derivation.unclassified.single().rowCount)
    }
}
