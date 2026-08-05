package com.jenarvaezg.coindex.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal fun teslaCatalogStub() = CollectionCatalog(
    schemaVersion = 1,
    id = "nikola-tesla-serbia-1oz",
    name = "Nikola Tesla · Serbia · 1 oz",
    shortName = "Nikola Tesla",
    issuerCode = "serbie",
    family = "Nikola Tesla",
    weightMillioz = 1_000,
    finish = null,
    metal = Metal.Silver,
    seriesStatus = SeriesStatus.Open,
    source = "https://en.numista.com/catalogue/series.php?id=5303",
    updatedAt = "2026-08-01",
    members = listOf(
        CollectionCatalogMember("alternating-current", "Alternating current", 2018, 150_352),
        CollectionCatalogMember("x-rays", "X-Rays", 2020, 195_591),
    ),
)

internal fun dateRunCatalogStub() = CollectionCatalog(
    schemaVersion = 2,
    id = "venezuela-5-bolivares",
    name = "5 Bolívares · Venezuela",
    shortName = "5 Bolívares",
    issuerCode = "venezuela",
    family = "5 Bolívares de Venezuela",
    weightMillioz = 804,
    finish = null,
    metal = Metal.Silver,
    seriesStatus = SeriesStatus.Closed,
    closedNote = "La plata venezolana se acabó en 1965.",
    source = "https://en.numista.com/catalogue/pieces10340.html",
    updatedAt = "2026-08-01",
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
    shortName = "XVII Exposición Europea de Arte",
    issuerCode = "portugal",
    family = "XVII Exposición Europea de Arte de 1983",
    seriesStatus = SeriesStatus.Closed,
    closedNote = "Un estuche de tres monedas para la exposición de 1983, sin más emisiones.",
    source = "https://en.numista.com/catalogue/series.php?id=6598",
    updatedAt = "2026-08-01",
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
    shortName = "Conjunto anual",
    issuerCode = "portugal",
    family = "500 escudos conmemorativos de plata .500 de Portugal",
    weightMillioz = 450,
    finish = null,
    metal = Metal.Silver,
    seriesStatus = SeriesStatus.Closed,
    closedNote = "El escudo desapareció con el euro el 1 de enero de 2002.",
    source = "https://en.numista.com/catalogue/series.php?id=6598",
    updatedAt = "2026-08-01",
    members = listOf(
        CollectionCatalogMember("1995-santo-antonio", "Santo António", 1995, 13_042),
        CollectionCatalogMember("2001-oporto", "Oporto", 2001, 13_046),
    ),
)

/**
 * The tenth Royal Tudor Beast: named by the mint in 2022, struck in proof, and still not issued
 * in bullion. Its design points at the 2 oz proof — the very piece the father owns and the one
 * that must never fill this slot.
 */
internal fun announcedMemberStub() = CollectionCatalogMember(
    id = "seymour-panther",
    label = "Seymour Panther",
    status = MemberStatus.Announced,
    source = "https://britannia-uk.com/royal-tudor-beasts/",
    sourceNote = "Salió en proof en 2022 y sigue sin salir en bullion.",
    designTypeId = 307_800,
)

/** A struck and sold coin whose Numista type is not publicly verifiable yet (#48). */
internal fun unlistedMemberStub() = CollectionCatalogMember(
    id = "2023-rabbit",
    label = "Year of the Rabbit",
    year = 2023,
    status = MemberStatus.Unlisted,
    source = "https://www.perthmint.com/year-of-the-rabbit/",
    sourceNote = "Acuñada y vendida por Perth Mint; Numista todavía no tiene una ficha publicada.",
)

private fun item(id: Long, typeId: Int, quantity: Int = 1) =
    CollectedItem(id = id, quantity = quantity, typeId = typeId)

private fun datedItem(id: Long, typeId: Int, issueYear: Int?) =
    CollectedItem(id = id, quantity = 1, typeId = typeId, issueYear = issueYear)

/**
 * The stars of the 100 pesetas of Franco (ADR 0014): one type, one year on every issue, and the
 * star is a variety of it. The issue ids here are stand-ins; the shipped seed carries the real
 * ones. The 1969 holds two — the curved and the straight nine — because the collector counts one
 * star.
 */
internal fun issueRunCatalogStub() = CollectionCatalog(
    schemaVersion = 5,
    id = "espana-paquillos",
    name = "Paquillos · 100 pesetas de Franco",
    shortName = "Paquillos",
    issuerCode = "espagne",
    family = "100 Pesetas de Franco",
    weightMillioz = 611,
    finish = null,
    metal = Metal.Silver,
    seriesStatus = SeriesStatus.Closed,
    closedNote = "El tipo solo se acuñó con las estrellas 66 a 70.",
    source = "https://en.numista.com/catalogue/pieces1885.html",
    updatedAt = "2026-08-01",
    members = listOf(
        CollectionCatalogMember("estrella-66", "Estrella 66", 1966, 1_885, listOf(9_001)),
        CollectionCatalogMember("estrella-69", "Estrella 69", 1966, 1_885, listOf(9_004, 9_005)),
        CollectionCatalogMember("estrella-70", "Estrella 70", 1966, 1_885, listOf(9_006)),
    ),
)

/** A piece attached to one Numista issue, all of them dated the same year. */
private fun issuedItem(id: Long, typeId: Int, issueId: Int?, quantity: Int = 1) = CollectedItem(
    id = id,
    quantity = quantity,
    typeId = typeId,
    issueYear = 1966,
    issueId = issueId,
)

class CollectionCatalogValidationTest {
    @Test
    fun `catalog deserialization uses the generalized source pair`() {
        val contents = """
            {
              "schema_version": 1,
              "id": "lunar-rabbit",
              "name": "Lunar rabbit",
              "short_name": "Lunar rabbit",
              "issuer_code": "australia",
              "family": "Lunar Series III",
              "weight_millioz": 1000,
              "metal": "silver",
              "series_status": "open",
              "source": "https://en.numista.com/catalogue/series.php?id=4750",
              "updated_at": "2026-08-01",
              "members": [{
                "id": "2023-rabbit",
                "label": "Year of the Rabbit",
                "year": 2023,
                "status": "unlisted",
                "source": "https://www.perthmint.com/year-of-the-rabbit/",
                "source_note": "Acuñada y vendida; Numista no tiene una ficha publicada."
              }]
            }
        """.trimIndent()

        val catalog = CatalogSeeds.parse("rabbit.json", contents)

        assertEquals(MemberStatus.Unlisted, catalog.members.single().status)
        assertEquals(
            "https://www.perthmint.com/year-of-the-rabbit/",
            catalog.members.single().source,
        )
        assertFailsWith<CatalogSeedException> {
            CatalogSeeds.parse(
                "legacy-rabbit.json",
                contents.replace("\"source_note\"", "\"announced_note\""),
            )
        }
    }

    @Test
    fun `validation requires versioned slugged unique sourced exact variants`() {
        val definition = teslaCatalogStub()
        assertEquals(
            VariantKey.fromCanonicalParts("Nikola Tesla", 1_000, "unknown", "silver"),
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
        // Callarse el metal es chocar con el siguiente catálogo de la misma familia (#40).
        assertEquals(
            CollectionCatalogValidationError.MissingMetal,
            definition.copy(metal = null).validate(),
        )
        // La nota que exime del cruce tiene que decir algo, como la `closed_note`.
        assertEquals(
            CollectionCatalogValidationError.BlankVariantNote(definition.members[0].id),
            definition.copy(
                members = definition.members.mapIndexed { index, member ->
                    if (index == 0) member.copy(variantNote = "  ") else member
                },
            ).validate(),
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

    /**
     * El emisor de la cabecera es el de un miembro que no dice otro, y no una afirmación sobre la
     * lista (#170): Pressburg acuña Equilibrium alternando Tokelau y Niue, así que un solo código
     * sólo podía rotularse encima de las dos monedas que dicen Niue mintiendo.
     *
     * Las dos reglas son la misma en los dos sentidos. Un `issuer_code` en blanco en el miembro se
     * rechaza como cualquier campo declarado y vacío, y una cabecera que no es el emisor de nadie
     * también: un valor por defecto que no le sirve de defecto a ningún miembro es el mismo rótulo
     * falso una indirección más adentro.
     */
    @Test
    fun `a member may name its own issuer and the header defaults for the rest`() {
        val single = teslaCatalogStub()
        assertNull(single.validate())
        assertEquals(setOf("serbie"), single.issuerCodes())
        assertEquals("serbie", single.issuerCodeOf(single.members[0]))

        val spanning = single.copy(
            members = single.members.mapIndexed { index, member ->
                if (index == 1) member.copy(issuerCode = "niue") else member
            },
        )
        assertNull(spanning.validate())
        assertEquals(setOf("serbie", "niue"), spanning.issuerCodes())
        assertEquals("niue", spanning.issuerCodeOf(spanning.members[1]))

        assertEquals(
            CollectionCatalogValidationError.BlankMemberIssuerCode(single.members[0].id),
            single.copy(
                members = single.members.mapIndexed { index, member ->
                    if (index == 0) member.copy(issuerCode = " ") else member
                },
            ).validate(),
        )
        assertEquals(
            CollectionCatalogValidationError.UnusedIssuerCode("serbie"),
            single.copy(
                members = single.members.map { member -> member.copy(issuerCode = "niue") },
            ).validate(),
        )
    }

    /**
     * Closing costs proof and opening costs none (#28): the note is obligatory one way and
     * forbidden the other, the same symmetry the issue ids already use outside an issue run.
     */
    @Test
    fun `a closed catalog must say why and an open one cannot`() {
        val open = teslaCatalogStub()
        assertNull(open.validate())
        assertEquals(SeriesStatus.Open, open.seriesStatus)
        assertNull(open.closedNote)

        assertEquals(
            CollectionCatalogValidationError.ClosedWithoutNote,
            open.copy(seriesStatus = SeriesStatus.Closed).validate(),
        )
        assertEquals(
            CollectionCatalogValidationError.ClosedWithoutNote,
            open.copy(seriesStatus = SeriesStatus.Closed, closedNote = "   ").validate(),
        )
        assertEquals(
            CollectionCatalogValidationError.OpenWithClosedNote,
            open.copy(closedNote = "la serie murió en 2019").validate(),
        )

        val closed = dateRunCatalogStub()
        assertNull(closed.validate())
        assertEquals(SeriesStatus.Closed, closed.seriesStatus)
        assertEquals(
            CollectionCatalogValidationError.OpenWithClosedNote,
            closed.copy(seriesStatus = SeriesStatus.Open).validate(),
        )
    }

    /**
     * The boundary an open catalog cannot write in `closed_note` (#53, ADR 0020): a `source_note`
     * is optional, allowed in both statuses, and refused only when blank.
     */
    @Test
    fun `a catalog may cite the boundary that Numista does not draw`() {
        val open = teslaCatalogStub()
        assertNull(open.sourceNote)
        assertNull(open.copy(sourceNote = "la ceca la presenta como gama").validate())
        assertEquals(
            CollectionCatalogValidationError.BlankSourceNote,
            open.copy(sourceNote = "   ").validate(),
        )

        val closed = dateRunCatalogStub()
        assertNull(closed.copy(sourceNote = "el Handboek los cierra en cinco").validate())
    }

    /**
     * Interior years the mint skipped are not curation debt (#130, #131). Declaring them takes
     * the same bargain as `closed_note`: the years are structured for the stale-catalogs report,
     * and the note is required prose with the proof. They never become members, so the plate
     * denominator stays untouched.
     */
    @Test
    fun `a catalog may declare years the mint did not issue`() {
        val open = teslaCatalogStub()
        assertTrue(open.noIssueYears.isEmpty())
        assertNull(open.noIssueNote)

        val silenced = open.copy(
            noIssueYears = listOf(2019),
            noIssueNote = "La ceca saltó 2019; la serie pasa de 2018 a 2020.",
        )
        assertNull(silenced.validate())

        assertEquals(
            CollectionCatalogValidationError.NoIssueYearsWithoutNote,
            open.copy(noIssueYears = listOf(2019)).validate(),
        )
        assertEquals(
            CollectionCatalogValidationError.BlankNoIssueNote,
            open.copy(noIssueYears = listOf(2019), noIssueNote = "   ").validate(),
        )
        assertEquals(
            CollectionCatalogValidationError.NoIssueNoteWithoutYears,
            open.copy(noIssueNote = "prosa huérfana").validate(),
        )
        assertEquals(
            CollectionCatalogValidationError.DuplicateNoIssueYear(2019),
            open.copy(
                noIssueYears = listOf(2019, 2019),
                noIssueNote = "duplicado",
            ).validate(),
        )
        assertEquals(
            CollectionCatalogValidationError.NoIssueYearConflictsWithMember(2018),
            open.copy(
                noIssueYears = listOf(2018),
                noIssueNote = "choca con Alternating current",
            ).validate(),
        )
        assertEquals(
            CollectionCatalogValidationError.NoIssueYearOutsideSpan(2017),
            open.copy(
                noIssueYears = listOf(2017),
                noIssueNote = "antes del primer miembro",
            ).validate(),
        )
        assertEquals(
            CollectionCatalogValidationError.NoIssueYearOutsideSpan(2021),
            open.copy(
                noIssueYears = listOf(2021),
                noIssueNote = "después del último miembro",
            ).validate(),
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

        // A type page is legitimate in every version: the types Numista files under no series at
        // all can still form a catalog, and then there is no series URL to cite (#51).
        val typePageOnV1 = definition.copy(
            schemaVersion = 1,
            members = definition.members.take(1),
        )
        assertNull(typePageOnV1.validate())
    }

    @Test
    fun `issue runs repeat one type across issues and every member names one`() {
        val definition = issueRunCatalogStub()
        assertNull(definition.validate())

        // The type repeats, as it does in a date run, and the shared year is not a duplicate.
        assertTrue(definition.members.all { it.numistaTypeId == 1_885 && it.year == 1966 })

        val withoutIssue = definition.copy(
            members = definition.members.map { it.copy(numistaIssueIds = emptyList()) },
        )
        assertEquals(
            CollectionCatalogValidationError.MemberWithoutIssue("estrella-66"),
            withoutIssue.validate(),
        )

        // One issue in two slots would let a single piece fill both.
        val sharedIssue = definition.copy(
            members = listOf(
                definition.members[0],
                definition.members[1].copy(numistaIssueIds = listOf(9_001)),
            ),
        )
        assertEquals(
            CollectionCatalogValidationError.DuplicateNumistaIssueId(9_001),
            sharedIssue.validate(),
        )

        val zeroIssue = definition.copy(
            members = listOf(definition.members[0].copy(numistaIssueIds = listOf(0))),
        )
        assertEquals(
            CollectionCatalogValidationError.InvalidNumistaIssueId,
            zeroIssue.validate(),
        )

        // A date run may refine a year with issues when the type page mixes finishes (#91).
        val issuesOnDateRun = dateRunCatalogStub().let { run ->
            run.copy(members = listOf(run.members[0].copy(numistaIssueIds = listOf(9_001))))
        }
        assertNull(issuesOnDateRun.validate())

        val ordinaryWithBadIssues = teslaCatalogStub().copy(
            members = listOf(
                teslaCatalogStub().members[0].copy(numistaIssueIds = listOf(9_001, 9_001)),
            ),
        )
        assertEquals(
            CollectionCatalogValidationError.DuplicateNumistaIssueId(9_001),
            ordinaryWithBadIssues.validate(),
        )
        assertEquals(
            CollectionCatalogValidationError.InvalidNumistaIssueId,
            ordinaryWithBadIssues.copy(
                members = listOf(ordinaryWithBadIssues.members[0].copy(numistaIssueIds = listOf(0))),
            ).validate(),
        )
    }

    /**
     * The symmetry of [MemberStatus], both ways round (#31). Nothing is implicit: an absent
     * `numista_type_id` never *means* announced, and an announced member never carries one —
     * Numista catalogues struck coins.
     */
    @Test
    fun `an announced member costs its source and forbids a numista type`() {
        val definition = teslaCatalogStub().let { tesla ->
            tesla.copy(members = tesla.members + announcedMemberStub())
        }
        assertNull(definition.validate())

        fun withPanther(member: CollectionCatalogMember) =
            definition.copy(members = definition.members.dropLast(1) + member)

        val announced = announcedMemberStub()
        assertEquals(
            CollectionCatalogValidationError.AnnouncedWithNumistaType("seymour-panther"),
            withPanther(announced.copy(numistaTypeId = 307_800)).validate(),
        )
        assertEquals(
            CollectionCatalogValidationError.MemberWithoutSource("seymour-panther"),
            withPanther(announced.copy(source = null)).validate(),
        )
        // A note pasted into the URL field is not a citation.
        assertEquals(
            CollectionCatalogValidationError.MemberWithoutSource("seymour-panther"),
            withPanther(announced.copy(source = "lo dice la Royal Mint")).validate(),
        )
        // The URL of a third party rots, so the claim has to survive it in prose.
        assertEquals(
            CollectionCatalogValidationError.MemberWithoutSourceNote("seymour-panther"),
            withPanther(announced.copy(sourceNote = " ")).validate(),
        )
        assertEquals(
            CollectionCatalogValidationError.InvalidDesignTypeId,
            withPanther(announced.copy(designTypeId = 0)).validate(),
        )
        // The design is optional: a calendar slot with no design yet names nothing at all.
        assertNull(withPanther(announced.copy(designTypeId = null, year = 2027)).validate())

        // And the other direction: an issued member owes its type and its year, and may not
        // borrow the vocabulary of an announcement.
        val issued = definition.members[0]
        assertEquals(
            CollectionCatalogValidationError.InvalidNumistaTypeId,
            definition.copy(members = listOf(issued.copy(numistaTypeId = null))).validate(),
        )
        assertEquals(
            CollectionCatalogValidationError.IssuedWithoutYear("alternating-current"),
            definition.copy(members = listOf(issued.copy(year = null))).validate(),
        )
        assertEquals(
            CollectionCatalogValidationError.IssuedWithSource("alternating-current"),
            definition.copy(
                members = listOf(issued.copy(source = "https://example.org/x")),
            ).validate(),
        )
        assertEquals(
            CollectionCatalogValidationError.IssuedWithDesignType("alternating-current"),
            definition.copy(members = listOf(issued.copy(designTypeId = 307_800))).validate(),
        )
    }

    @Test
    fun `an unlisted member costs proof but forbids numista identifiers`() {
        val definition = teslaCatalogStub().let { catalog ->
            catalog.copy(members = catalog.members + unlistedMemberStub())
        }
        assertNull(definition.validate())

        fun withRabbit(member: CollectionCatalogMember) =
            definition.copy(members = definition.members.dropLast(1) + member)

        val unlisted = unlistedMemberStub()
        assertEquals(
            CollectionCatalogValidationError.UnlistedWithNumistaType("2023-rabbit"),
            withRabbit(unlisted.copy(numistaTypeId = 123)).validate(),
        )
        assertEquals(
            CollectionCatalogValidationError.UnlistedWithoutYear("2023-rabbit"),
            withRabbit(unlisted.copy(year = null)).validate(),
        )
        assertEquals(
            CollectionCatalogValidationError.MemberWithoutSource("2023-rabbit"),
            withRabbit(unlisted.copy(source = null)).validate(),
        )
        assertEquals(
            CollectionCatalogValidationError.MemberWithoutSourceNote("2023-rabbit"),
            withRabbit(unlisted.copy(sourceNote = " ")).validate(),
        )
        assertEquals(
            CollectionCatalogValidationError.IssuesOutsideIssueRun("2023-rabbit"),
            withRabbit(unlisted.copy(numistaIssueIds = listOf(12))).validate(),
        )
        assertEquals(
            CollectionCatalogValidationError.InvalidDesignTypeId,
            withRabbit(unlisted.copy(designTypeId = 0)).validate(),
        )
        assertNull(withRabbit(unlisted.copy(designTypeId = 307_800)).validate())
    }

    @Test
    fun `an issue run accepts an unlisted member without numista issues`() {
        val definition = issueRunCatalogStub().let { catalog ->
            catalog.copy(members = catalog.members + unlistedMemberStub())
        }

        assertNull(definition.validate())
    }

    /**
     * «Announced» composes with every way of identifying a member, which is exactly why it is a
     * field and not a `schema_version` of its own. What it cannot do is name issues: an issue
     * run keys on struck emissions, and an unstruck member has none.
     */
    @Test
    fun `an announced member composes with a date run and never fills a slot`() {
        val definition = dateRunCatalogStub().let { run ->
            run.copy(
                seriesStatus = SeriesStatus.Open,
                closedNote = null,
                members = run.members + announcedMemberStub(),
            )
        }
        assertNull(definition.validate())

        val album = buildCollectionCatalogAlbum(definition, listOf(datedItem(1, 10_340, 1904)))
        assertEquals(1, album.ownedMembers())
        assertEquals(2, album.issuedMembers())
        assertEquals(1, album.announcedMembers())
        assertEquals(CollectionCatalogMemberStatus.NotYetIssued, album.members[2].status)

        // The design is never matched on, so the piece it points at fills nothing.
        assertFalse(definition.memberMatches(definition.members[2], item(9, 307_800)))
        assertFalse(definition.isEvidencedBy(listOf(item(9, 307_800))))

        val withIssues = issueRunCatalogStub().let { run ->
            run.copy(
                members = run.members + announcedMemberStub().copy(numistaIssueIds = listOf(9_009)),
            )
        }
        assertEquals(
            CollectionCatalogValidationError.IssuesOutsideIssueRun("seymour-panther"),
            withIssues.validate(),
        )
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
            VariantKey.fromCanonicalParts(
                definition.family,
                SPANNING_VARIANTS_WEIGHT,
                "unknown",
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
        // Un conjunto abarca metales igual que pesos y acabados, así que tampoco declara uno.
        assertEquals(
            CollectionCatalogValidationError.SetDeclaresVariant,
            definition.copy(metal = Metal.Silver).validate(),
        )
        // A set names each type once: it is a set, not a date run.
        assertEquals(
            CollectionCatalogValidationError.DuplicateNumistaTypeId(22_178),
            definition.copy(
                members = definition.members + definition.members[0].copy(id = "bis"),
            ).validate(),
        )
        // Zero stays an invalid weight, so a defaulted row is ignored rather than read as a set.
        assertNull(VariantKey.fromCanonicalParts(definition.family, 0, "unknown", "silver"))
        // A set spans finishes too, so a stored finish makes the key uncanonical.
        assertNull(
            VariantKey.fromCanonicalParts(
                definition.family,
                SPANNING_VARIANTS_WEIGHT,
                "proof",
                "unknown",
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
    fun `an unlisted member is neither missing nor measurable`() {
        val definition = teslaCatalogStub().let { catalog ->
            catalog.copy(members = catalog.members + unlistedMemberStub().copy(designTypeId = 307_800))
        }

        val album = buildCollectionCatalogAlbum(
            definition,
            listOf(item(id = 1, typeId = 307_800)),
        )

        assertEquals(CollectionCatalogMemberStatus.Unlisted, album.members.last().status)
        assertEquals(0, album.ownedMembers())
        assertEquals(2, album.issuedMembers())
    }

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

    @Test
    fun `ordinary issue qualifiers constrain membership and evidence`() {
        val definition = teslaCatalogStub().copy(
            members = listOf(
                teslaCatalogStub().members[0].copy(numistaIssueIds = listOf(7_001, 7_002)),
            ),
        )
        val member = definition.members.single()
        val matching = CollectedItem(1, 1, 150_352, issueId = 7_002)
        val wrongIssue = CollectedItem(2, 1, 150_352, issueId = 7_003)
        val missingIssue = CollectedItem(3, 1, 150_352)

        assertNull(definition.validate())
        assertTrue(definition.memberMatches(member, matching))
        assertFalse(definition.memberMatches(member, wrongIssue))
        assertFalse(definition.memberMatches(member, missingIssue))
        assertTrue(definition.isEvidencedBy(listOf(matching)))
        assertFalse(definition.isEvidencedBy(listOf(wrongIssue, missingIssue)))

    }

    /**
     * The case the date run cannot express: six issues, one year. Keying on the year would fill
     * one slot and call the other five missing while they sit in the album.
     */
    @Test
    fun `an issue run matches by issue and ignores the year entirely`() {
        val definition = issueRunCatalogStub()

        val album = buildCollectionCatalogAlbum(
            definition,
            listOf(
                issuedItem(1, 1_885, 9_001),
                // The straight nine fills the same star as the curved one.
                issuedItem(2, 1_885, 9_005),
                // Same type, same 1966, an issue the catalog does not list.
                issuedItem(3, 1_885, 9_999),
                // A piece recorded without an issue fills nothing.
                issuedItem(4, 1_885, null),
            ),
        )

        assertEquals(2, album.ownedMembers())
        assertEquals("Estrella 66", album.members[0].member.label)
        assertTrue(album.members[0].status is CollectionCatalogMemberStatus.Owned)
        assertTrue(album.members[1].status is CollectionCatalogMemberStatus.Owned)
        assertEquals(CollectionCatalogMemberStatus.Missing, album.members[2].status)

        // Owning both varieties of one star counts one slot, with both pieces behind it.
        val bothNines = buildCollectionCatalogAlbum(
            definition,
            listOf(issuedItem(5, 1_885, 9_004), issuedItem(6, 1_885, 9_005, quantity = 2)),
        )
        val nine = bothNines.members[1].status as? CollectionCatalogMemberStatus.Owned
        assertNotNull(nine)
        assertEquals(1, bothNines.ownedMembers())
        assertEquals(3, nine.quantity)
        assertEquals(2, nine.items.size)

        // The catalog is also what names a piece the row cannot tell apart.
        assertEquals("Estrella 69", definition.emissionLabelFor(issuedItem(7, 1_885, 9_004)))
        assertNull(definition.emissionLabelFor(issuedItem(8, 1_885, 9_999)))
        assertNull(dateRunCatalogStub().emissionLabelFor(datedItem(9, 10_340, 1904)))

        // Issue-qualified evidence needs an issue, just as ownership does.
        assertTrue(definition.isEvidencedBy(listOf(issuedItem(10, 1_885, 9_001))))
        assertFalse(definition.isEvidencedBy(listOf(issuedItem(11, 1_885, null))))
    }
}
