package com.jenarvaezg.coindex.domain

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * The matching rules `scripts/` keeps a copy of, written down where the copy can be checked.
 *
 * Two curation reports live in Python and mirror this module by hand: `weight-deviations.py`
 * copies the snapping tolerance, the common bullion weights and the family functions, and
 * `type-claims.py` copies the cross-catalog claim rule that [CatalogSeeds.parseAll] makes fatal.
 * They are outside the app on purpose (ADR 0021 §12) and they are ports, not shared code — so
 * until this test existed, the Python suite tested the copy against itself and nothing went red
 * when a constant moved here alone.
 *
 * This narrows the crossing to one file: `fixtures/matching-digest.json`, generated here and
 * versioned, holding the constants plus the vectors that pin them — grams to normalized
 * milli-ounces, families to technical or not, schema version to catalog species, and a claim
 * layout to whether the app refuses to start on it. `scripts/test_matching_digest.py` asserts the
 * Python copies against it, so changing a rule here alone turns CI red on the Python side.
 *
 * **The port is one-directional.** The app is the runtime gate and the single source: the digest
 * says what this module does, and never what the reports would like it to do. Nothing reads the
 * digest back into production code.
 *
 * When this test fails, the generated candidate is already written to
 * `domain/build/matching-digest.json`; the fix is to install it and to follow it in Python.
 */
class MatchingDigestTest {
    @Test
    fun `the versioned digest is the one this suite generates`() {
        val generated = generateDigest()
        File(CANDIDATE_DIGEST).apply {
            parentFile?.mkdirs()
            writeText(generated)
        }
        val committed = File(VERSIONED_DIGEST)
        assertEquals(
            if (committed.isFile) committed.readText() else "",
            generated,
            "el digest versionado ya no es el que emite la suite: instala el candidato con " +
                "`cp domain/$CANDIDATE_DIGEST $VERSIONED_DIGEST` y haz que las copias de " +
                "`scripts/` lo sigan, que es lo que su suite va a exigir",
        )
    }
}

private const val VERSIONED_DIGEST = "../fixtures/matching-digest.json"

private const val CANDIDATE_DIGEST = "build/matching-digest.json"

/**
 * Bumped when the **shape** of the digest changes, so that a Python suite reading a layout it does
 * not know fails saying so instead of asserting nothing. Adding a vector to an existing list is not
 * a shape change; adding, renaming or removing a field is.
 */
private const val DIGEST_VERSION = 1

private fun jsonOrNull(value: Int?): JsonElement =
    if (value == null) JsonNull else JsonPrimitive(value)

private fun jsonOrNull(value: String?): JsonElement =
    if (value == null) JsonNull else JsonPrimitive(value)

private val digestJson = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
}

private fun generateDigest(): String {
    val digest = buildJsonObject {
        put("digest_version", DIGEST_VERSION)
        put(
            "about",
            "Las reglas de emparejamiento que `scripts/` copia a mano, emitidas por " +
                "`MatchingDigestTest` de `:domain` y afirmadas por " +
                "`scripts/test_matching_digest.py`. No se edita a mano y nada de producción lo " +
                "lee: la app es la compuerta de runtime y la fuente única.",
        )
        putJsonObject("snapping") {
            put("grams_per_troy_ounce", GRAMS_PER_TROY_OUNCE)
            putJsonArray("common_weights_millioz") {
                COMMON_WEIGHTS_MILLIOZ.forEach { weight -> add(weight) }
            }
            put("snap_tolerance_millioz", SNAP_TOLERANCE_MILLIOZ)
        }
        putJsonArray("normalized_weights") {
            NORMALIZED_WEIGHT_INPUTS.forEach { grams ->
                addJsonObject {
                    put("grams", grams)
                    put("millioz", jsonOrNull(normalizeWeightMillioz(gramsToOunces(grams))))
                }
            }
        }
        putJsonArray("families") {
            FAMILY_INPUTS.forEach { family ->
                addJsonObject {
                    put("family", family)
                    put("normalized", jsonOrNull(normalizeFamily(family)))
                    put("technical", isTechnicalFamily(family))
                }
            }
        }
        putJsonArray("catalog_species") {
            PROBED_SCHEMA_VERSIONS.forEach { schemaVersion ->
                addJsonObject {
                    put("schema_version", schemaVersion)
                    put("species", speciesOf(schemaVersion))
                }
            }
        }
        putJsonArray("cross_claims") {
            CROSS_CLAIM_CASES.forEach { case ->
                addJsonObject {
                    put("case", case.id)
                    put("reads", case.reads)
                    put("rejected", runtimeRejects(case))
                    putJsonArray("catalogs") {
                        case.catalogs.forEach { catalog -> add(catalog.claimView()) }
                    }
                }
            }
        }
    }
    return digestJson.encodeToString(JsonObject.serializer(), digest) + "\n"
}

/**
 * The grams a ficha can carry, chosen at the edges: the six common weights exactly, the 31.1 g
 * Numista writes for an ounce, both sides of the tolerance above and below it, the near-ounce
 * that must never read as one, the Morgan dollar the magnet used to get wrong (#288), and what is
 * not a weight at all.
 */
private val NORMALIZED_WEIGHT_INPUTS = listOf(
    0.0,
    -1.0,
    1.5552,
    7.7758692,
    7.9,
    15.5517384,
    26.73,
    30.0,
    30.75,
    30.78,
    31.1,
    31.1034768,
    31.4,
    31.45,
    62.2069536,
    155.517384,
    311.034768,
)

/**
 * `technical` is read off the family **as written**, which is why the trailing-space and the
 * full-width-digit rows are here: `System 1999 ` and `System １９９９` are not monetary systems, and
 * a copy that trims or that accepts any Unicode digit would say they are.
 */
private val FAMILY_INPUTS = listOf(
    "",
    "   ",
    " Lunar   ounce ",
    "\tBritannia\n",
    "Lunar ounce",
    "System",
    "System ",
    "System 1999",
    "System 1999 ",
    "System 1971-2001",
    "System 19-2001",
    "System of a Down",
    "System １９９９",
    "system 1999",
)

/** One below the supported range, the four supported versions, the retired 4, and one above. */
private val PROBED_SCHEMA_VERSIONS = listOf(0, 1, 2, 3, 4, 5, 6)

/**
 * What species of catalog a `schema_version` makes: a simple one, a date run, a set, an issue run,
 * or no catalog at all.
 *
 * Asked of a probe that is not a valid catalog in every version — it declares a physical variant
 * that a set may not, and carries no issue that an issue run needs — which it does not have to be:
 * the schema check is the first of [validate], so any other error it reports already means the
 * version is supported.
 */
private fun speciesOf(schemaVersion: Int): String {
    val probe = schemaProbe(schemaVersion)
    return when {
        probe.validate() is CollectionCatalogValidationError.UnsupportedSchemaVersion -> "unsupported"
        probe.isDateRun -> "date_run"
        probe.isSet -> "set"
        probe.isIssueRun -> "issue_run"
        else -> "simple"
    }
}

/** The catalog [speciesOf] reads the species off. */
private fun schemaProbe(schemaVersion: Int): CollectionCatalog = CollectionCatalog(
    schemaVersion = schemaVersion,
    id = "schema-probe",
    name = "Schema probe",
    shortName = "Schema probe",
    issuerCode = "niue",
    family = "Schema probe",
    weightMillioz = 1_000,
    finish = Finish.Bullion,
    metal = Metal.Silver,
    seriesStatus = SeriesStatus.Open,
    source = NUMISTA_TYPE_SOURCE,
    updatedAt = DIGEST_DATE,
    members = listOf(
        CollectionCatalogMember(
            id = "2025-schema-probe",
            label = "Schema probe",
            year = 2025,
            numistaTypeId = CROSS_CLAIM_TYPE_ID,
        ),
    ),
)

/** The type every cross-claim case argues over. */
private const val CROSS_CLAIM_TYPE_ID = 485_082

private const val NUMISTA_TYPE_SOURCE =
    "https://en.numista.com/catalogue/pieces$CROSS_CLAIM_TYPE_ID.html"

/** Plainly synthetic: what sustains an announced member is a real mint page, and this is not one. */
private const val ANNOUNCED_SOURCE = "https://coindex.invalid/anunciada"

private const val DIGEST_DATE = "2026-09-07"

/** What a catalog says about the type, reduced to what a claim is made of. */
private class CatalogClaim(
    val id: String,
    val schemaVersion: Int,
    val members: List<MemberClaim>,
)

/** A member with no type is announced, which the expansion below has to say out loud. */
private class MemberClaim(
    val typeId: Int?,
    val issueIds: List<Int> = emptyList(),
    val year: Int = 2025,
)

private class CrossClaimCase(
    val id: String,
    val reads: String,
    val catalogs: List<CatalogClaim>,
)

/**
 * The claim layouts the reports have to judge the same way the app does.
 *
 * A set beside a catalog is deliberately **not** rejected: [CatalogSeeds.parseAll] leaves sets out
 * of the crossing (ADR 0012). The Python judge stops on it all the same, and says why where it
 * declares the difference — `STRICTER_THAN_THE_RUNTIME` in `test_matching_digest.py`.
 */
private val CROSS_CLAIM_CASES = listOf(
    CrossClaimCase(
        id = "one_catalog",
        reads = "one catalog names the type, with nothing to break a tie",
        catalogs = listOf(
            CatalogClaim("solo", 1, listOf(MemberClaim(CROSS_CLAIM_TYPE_ID))),
        ),
    ),
    CrossClaimCase(
        id = "same_catalog_twice",
        reads = "one date run repeats the type across two years",
        catalogs = listOf(
            CatalogClaim(
                "run",
                2,
                listOf(
                    MemberClaim(CROSS_CLAIM_TYPE_ID, year = 2025),
                    MemberClaim(CROSS_CLAIM_TYPE_ID, year = 2026),
                ),
            ),
        ),
    ),
    CrossClaimCase(
        id = "two_bare_catalogs",
        reads = "two catalogs name the type and neither is issue-qualified",
        catalogs = listOf(
            CatalogClaim("bullion", 1, listOf(MemberClaim(CROSS_CLAIM_TYPE_ID))),
            CatalogClaim("proof", 1, listOf(MemberClaim(CROSS_CLAIM_TYPE_ID))),
        ),
    ),
    CrossClaimCase(
        id = "one_bare_one_qualified",
        reads = "two catalogs name the type and only one breaks the tie by issue",
        catalogs = listOf(
            CatalogClaim("bullion", 1, listOf(MemberClaim(CROSS_CLAIM_TYPE_ID))),
            CatalogClaim("proof", 1, listOf(MemberClaim(CROSS_CLAIM_TYPE_ID, listOf(582_778)))),
        ),
    ),
    CrossClaimCase(
        id = "qualified_and_disjoint",
        reads = "two catalogs break the tie by issue and their issues are disjoint",
        catalogs = listOf(
            CatalogClaim("bullion", 1, listOf(MemberClaim(CROSS_CLAIM_TYPE_ID, listOf(582_780)))),
            CatalogClaim(
                "proof",
                1,
                listOf(MemberClaim(CROSS_CLAIM_TYPE_ID, listOf(582_778, 585_569))),
            ),
        ),
    ),
    CrossClaimCase(
        id = "qualified_and_overlapping",
        reads = "two catalogs break the tie by issue and both claim one of them",
        catalogs = listOf(
            CatalogClaim("bullion", 1, listOf(MemberClaim(CROSS_CLAIM_TYPE_ID, listOf(582_780)))),
            CatalogClaim(
                "proof",
                1,
                listOf(MemberClaim(CROSS_CLAIM_TYPE_ID, listOf(582_780, 585_569))),
            ),
        ),
    ),
    CrossClaimCase(
        id = "set_beside_catalog",
        reads = "a set and a catalog of the denomination name the type",
        catalogs = listOf(
            CatalogClaim("estuche", 3, listOf(MemberClaim(CROSS_CLAIM_TYPE_ID))),
            CatalogClaim("denominacion", 1, listOf(MemberClaim(CROSS_CLAIM_TYPE_ID))),
        ),
    ),
    CrossClaimCase(
        id = "announced_beside_catalog",
        reads = "an announced member holds no type, so it claims nothing beside a catalog",
        catalogs = listOf(
            CatalogClaim("anunciado", 1, listOf(MemberClaim(typeId = null))),
            CatalogClaim("bullion", 1, listOf(MemberClaim(CROSS_CLAIM_TYPE_ID))),
        ),
    ),
)

/** The claim-relevant part of a catalog file, which is also a payload `load_claims` can read. */
private fun CatalogClaim.claimView(): JsonObject = buildJsonObject {
    put("id", id)
    put("schema_version", schemaVersion)
    putJsonArray("members") {
        members.forEach { member ->
            addJsonObject {
                put("numista_type_id", jsonOrNull(member.typeId))
                putJsonArray("numista_issue_ids") {
                    member.issueIds.forEach { issueId -> add(issueId) }
                }
            }
        }
    }
}

/**
 * Whether the app refuses to start on this layout, asked of the gate itself.
 *
 * The claim view is expanded into whole catalog files, because the runtime verdict is what
 * [CatalogSeeds.parseAll] says and not what a rule extracted from it would say. Which means the
 * crossing has to be told apart from every other reason a seed is refused, and the wording of its
 * two messages is what tells it: a rejection that does not read as one fails here, loudly, rather
 * than being written into the digest as a claim verdict it is not.
 */
private fun runtimeRejects(case: CrossClaimCase): Boolean {
    val files = case.catalogs.map { catalog -> "${catalog.id}.json" to catalog.seedFile() }
    return try {
        CatalogSeeds.parseAll(files)
        false
    } catch (error: CatalogSeedException) {
        val message = error.message.orEmpty()
        require("is claimed by" in message) {
            "el caso `${case.id}` no llegó al cruce de reclamaciones: $message"
        }
        true
    }
}

/** The claim view grown into a file the seed loader accepts, and nothing more. */
private fun CatalogClaim.seedFile(): String {
    val catalog = buildJsonObject {
        put("schema_version", schemaVersion)
        put("id", id)
        put("name", id)
        put("short_name", id)
        put("issuer_code", "niue")
        put("family", id)
        if (schemaVersion != SET_SCHEMA_VERSION) {
            put("weight_millioz", 1_000)
            put("finish", "Bullion")
            put("metal", "silver")
        }
        put("series_status", "open")
        put("source", NUMISTA_TYPE_SOURCE)
        put("updated_at", DIGEST_DATE)
        putJsonArray("members") {
            members.forEachIndexed { index, member ->
                addJsonObject {
                    put("id", "$id-$index")
                    put("label", id)
                    put("year", member.year)
                    if (member.typeId == null) {
                        put("status", "announced")
                        put("source", ANNOUNCED_SOURCE)
                        put("source_note", "anunciada por la ceca y todavía sin acuñar")
                    } else {
                        put("numista_type_id", member.typeId)
                        if (member.issueIds.isNotEmpty()) {
                            putJsonArray("numista_issue_ids") {
                                member.issueIds.forEach { issueId -> add(issueId) }
                            }
                        }
                    }
                }
            }
        }
    }
    return Json.encodeToString(JsonObject.serializer(), catalog)
}

/** A set is `schema_version` 3 (ADR 0012), which is the one species the reports also read. */
private val SET_SCHEMA_VERSION = PROBED_SCHEMA_VERSIONS.singleOrNull { speciesOf(it) == "set" }
    ?: error("ninguna versión de $PROBED_SCHEMA_VERSIONS es un conjunto: amplía el sondeo")
