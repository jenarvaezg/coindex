package com.jenarvaezg.coindex.domain

import kotlinx.serialization.json.Json

/** A seeded catalog file that could not be trusted. Loading must fail loudly, never degrade. */
class CatalogSeedException(message: String, cause: Throwable? = null) :
    IllegalStateException(message, cause)

/**
 * Parses and validates the curated catalog seeds that ship with the app.
 *
 * Unknown JSON fields are rejected on purpose: a typo in a curated file is a bug to be seen
 * at startup, not a field silently ignored.
 */
private val strictJson = Json {
    ignoreUnknownKeys = false
    explicitNulls = false
}

object CatalogSeeds {
    private val json = strictJson

    fun parse(fileName: String, contents: String): CollectionCatalog {
        val catalog = try {
            json.decodeFromString<CollectionCatalog>(contents)
        } catch (error: IllegalArgumentException) {
            throw CatalogSeedException("cannot parse seed $fileName: ${error.message}", error)
        }
        catalog.validate()?.let { invalid ->
            throw CatalogSeedException("collection catalog seed $fileName is invalid: ${invalid.message}")
        }
        return catalog
    }

    /**
     * Parses every seed, rejects duplicate catalog ids, and makes cross-catalog type claims
     * unambiguous. A type may occur in several ordinary catalogs only when every occurrence is
     * issue-qualified and those catalogs' issue sets are disjoint.
     */
    fun parseAll(files: List<Pair<String, String>>): List<CollectionCatalog> {
        val ids = mutableSetOf<String>()
        val shortNames = mutableSetOf<String>()
        val catalogs = files.map { (fileName, contents) ->
            val catalog = parse(fileName, contents)
            if (!ids.add(catalog.id)) {
                throw CatalogSeedException("collection catalog id `${catalog.id}` is duplicated")
            }
            // Two cards reading the same thing is a defect the collector cannot get past: the
            // variant that tells them apart — weight, finish — is not on the card (#22).
            if (!shortNames.add(catalog.shortName)) {
                throw CatalogSeedException(
                    "collection catalog `short_name` `${catalog.shortName}` is duplicated",
                )
            }
            catalog
        }
        validateCrossCatalogClaims(catalogs)
        return catalogs
    }

    private fun validateCrossCatalogClaims(catalogs: List<CollectionCatalog>) {
        val claimsByType =
            mutableMapOf<Int, MutableMap<String, MutableList<CollectionCatalogMember>>>()
        for (catalog in catalogs.filterNot { it.isSet }) {
            for (member in catalog.members.filter { it.isIssued }) {
                val typeId = member.numistaTypeId ?: continue
                claimsByType
                    .getOrPut(typeId) { linkedMapOf() }
                    .getOrPut(catalog.id) { mutableListOf() }
                    .add(member)
            }
        }

        for ((typeId, claimsByCatalog) in claimsByType) {
            if (claimsByCatalog.size < 2) continue
            if (claimsByCatalog.values.flatten().any { it.numistaIssueIds.isEmpty() }) {
                throw CatalogSeedException(
                    "Numista type `$typeId` is claimed by more than one collection catalog without issue-qualified identities",
                )
            }
            val issueOwners = mutableMapOf<Int, String>()
            for ((catalogId, members) in claimsByCatalog) {
                for (issueId in members.flatMap { it.numistaIssueIds }) {
                    val previous = issueOwners.putIfAbsent(issueId, catalogId)
                    if (previous != null && previous != catalogId) {
                        throw CatalogSeedException(
                            "Numista issue `$issueId` for type `$typeId` is claimed by collection catalogs `$previous` and `$catalogId`",
                        )
                    }
                }
            }
        }
    }
}

/**
 * Parses and validates the curated grouping seeds (ADR 0013), under the same rules as the
 * catalogs: unknown fields are rejected and a bad file stops the app instead of degrading.
 */
object GroupingSeeds {
    fun parse(fileName: String, contents: String): CuratedGrouping {
        val grouping = try {
            strictJson.decodeFromString<CuratedGrouping>(contents)
        } catch (error: IllegalArgumentException) {
            throw CatalogSeedException("cannot parse seed $fileName: ${error.message}", error)
        }
        grouping.validate()?.let { invalid ->
            throw CatalogSeedException(
                "curated grouping seed $fileName is invalid: ${invalid.message}",
            )
        }
        return grouping
    }

    /**
     * Parses every seed and rejects duplicate grouping ids, and any type named by two
     * groupings: a piece has exactly one family, so two groupings claiming it is a curation
     * mistake rather than a precedence question to resolve at runtime.
     */
    fun parseAll(files: List<Pair<String, String>>): List<CuratedGrouping> {
        val ids = mutableSetOf<String>()
        val shortNames = mutableSetOf<String>()
        val typeIds = mutableSetOf<Int>()
        return files.map { (fileName, contents) ->
            val grouping = parse(fileName, contents)
            if (!ids.add(grouping.id)) {
                throw CatalogSeedException("curated grouping id `${grouping.id}` is duplicated")
            }
            if (!shortNames.add(grouping.shortName)) {
                throw CatalogSeedException(
                    "curated grouping `short_name` `${grouping.shortName}` is duplicated",
                )
            }
            for (typeId in grouping.typeIds) {
                if (!typeIds.add(typeId)) {
                    throw CatalogSeedException(
                        "Numista type `$typeId` is claimed by more than one curated grouping",
                    )
                }
            }
            grouping
        }
    }
}

/**
 * Parses and validates the commemorative programme seeds (ADR 0022), under the same rules as the
 * catalogs: unknown fields are rejected and a bad file stops the app instead of degrading.
 *
 * A programme's `short_name` deliberately stays **out** of [validateShortNamesAcross]: a
 * programme is not a card, so it never sits beside a catalog in the index and cannot be confused
 * with one there. Uniqueness among programmes is enough.
 */
object ProgrammeSeeds {
    fun parse(fileName: String, contents: String): CommemorativeProgramme {
        val programme = try {
            strictJson.decodeFromString<CommemorativeProgramme>(contents)
        } catch (error: IllegalArgumentException) {
            throw CatalogSeedException("cannot parse seed $fileName: ${error.message}", error)
        }
        programme.validate()?.let { invalid ->
            throw CatalogSeedException(
                "commemorative programme seed $fileName is invalid: ${invalid.message}",
            )
        }
        return programme
    }

    /**
     * Parses every seed and rejects duplicate ids, duplicate names, and any type named by two
     * programmes: a coin was struck for one commemoration, so two programmes claiming it is a
     * curation mistake and not a precedence question to resolve at runtime.
     */
    fun parseAll(files: List<Pair<String, String>>): List<CommemorativeProgramme> {
        val ids = mutableSetOf<String>()
        val shortNames = mutableSetOf<String>()
        val typeIds = mutableSetOf<Int>()
        return files.map { (fileName, contents) ->
            val programme = parse(fileName, contents)
            if (!ids.add(programme.id)) {
                throw CatalogSeedException(
                    "commemorative programme id `${programme.id}` is duplicated",
                )
            }
            if (!shortNames.add(programme.shortName)) {
                throw CatalogSeedException(
                    "commemorative programme `short_name` `${programme.shortName}` is duplicated",
                )
            }
            for (member in programme.members) {
                if (!typeIds.add(member.numistaTypeId)) {
                    throw CatalogSeedException(
                        "Numista type `${member.numistaTypeId}` is claimed by more than one " +
                            "commemorative programme",
                    )
                }
            }
            programme
        }
    }
}

/**
 * Rejects a `short_name` shared by a catalog and a curated grouping (#22).
 *
 * Each species validates its own names while parsing, but the index draws them side by side and
 * indistinguishably (#12), so uniqueness only means anything across both. Called where both are
 * loaded, which is the only place that can see it.
 */
fun validateShortNamesAcross(
    catalogs: List<CollectionCatalog>,
    groupings: List<CuratedGrouping>,
) {
    val catalogNames = catalogs.associateBy { it.shortName }
    for (grouping in groupings) {
        val catalog = catalogNames[grouping.shortName] ?: continue
        throw CatalogSeedException(
            "`short_name` `${grouping.shortName}` is claimed by both catalog `${catalog.id}` " +
                "and grouping `${grouping.id}`",
        )
    }
}

/**
 * Parses the curated orphans register (#133).
 *
 * Same strict JSON as the catalogs: a typo is a bug. Unlike catalogs and groupings, this
 * file is editorial — it is not loaded into the app container — so structural failures surface
 * in the suite rather than as a fatal startup crash. Cross-checks against catalog claims
 * stay in the suite too ([orphanCatalogCollisions]).
 */
@SuiteOnly
object OrphanSeeds {
    fun parse(fileName: String, contents: String): CuratedOrphans {
        val orphans = try {
            strictJson.decodeFromString<CuratedOrphans>(contents)
        } catch (error: IllegalArgumentException) {
            throw CatalogSeedException("cannot parse seed $fileName: ${error.message}", error)
        }
        orphans.validate()?.let { invalid ->
            throw CatalogSeedException("orphans seed $fileName is invalid: ${invalid.message}")
        }
        return orphans
    }
}
