package com.jenarvaezg.coindex.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Curated register of [Orphan] verdicts (#121, #133).
 *
 * Editorial only: it does not feed proposals, plates, or the unclassified residue screen.
 * Absolute solitude is enough but not required; calendar solitude (Gothic Horror) stays out.
 * An empty list is valid — the schema can ship before any verdict is versioned.
 */
@Serializable
data class CuratedOrphans(
    @SerialName("schema_version") val schemaVersion: Int,
    @SerialName("updated_at") val updatedAt: String,
    val orphans: List<OrphanEntry> = emptyList(),
) {
    fun validate(): CuratedOrphansValidationError? {
        if (schemaVersion != 1) {
            return CuratedOrphansValidationError.UnsupportedSchemaVersion(schemaVersion)
        }
        if (updatedAt.isBlank()) {
            return CuratedOrphansValidationError.BlankField("orphans.updated_at")
        }
        val seen = mutableSetOf<Int>()
        for (entry in orphans) {
            if (entry.numistaTypeId <= 0) {
                return CuratedOrphansValidationError.InvalidNumistaTypeId(entry.numistaTypeId)
            }
            if (!seen.add(entry.numistaTypeId)) {
                return CuratedOrphansValidationError.DuplicateNumistaTypeId(entry.numistaTypeId)
            }
            if (entry.reason.isBlank()) {
                return CuratedOrphansValidationError.BlankReason(entry.numistaTypeId)
            }
        }
        return null
    }
}

/** One curator verdict: this Numista type will not get a collection-catalog plate. */
@Serializable
data class OrphanEntry(
    @SerialName("numista_type_id") val numistaTypeId: Int,
    val reason: String,
)

sealed class CuratedOrphansValidationError(val message: String) {
    data class UnsupportedSchemaVersion(val version: Int) : CuratedOrphansValidationError(
        "curated orphans schema version `$version` is not supported",
    )

    data class BlankField(val field: String) : CuratedOrphansValidationError(
        "required field `$field` cannot be blank",
    )

    data class InvalidNumistaTypeId(val typeId: Int) : CuratedOrphansValidationError(
        "Numista type id `$typeId` must be greater than zero",
    )

    data class DuplicateNumistaTypeId(val typeId: Int) : CuratedOrphansValidationError(
        "Numista type id `$typeId` is named more than once in the orphans register",
    )

    data class BlankReason(val typeId: Int) : CuratedOrphansValidationError(
        "orphan reason for Numista type `$typeId` cannot be blank",
    )
}

/**
 * Types that appear both as an orphan verdict and as an issued catalog member.
 *
 * Lives in the suite, not at startup: a collision is a curation mistake to fix by editing
 * one of the two files, and making it fatal on boot would be the unsilenceable red #133
 * forbids (same bargain as the metal and object-class nets).
 */
fun orphanCatalogCollisions(
    orphans: CuratedOrphans,
    catalogs: List<CollectionCatalog>,
): List<Int> {
    val catalogTypes = catalogs
        .asSequence()
        .flatMap { catalog -> catalog.members.asSequence() }
        .filter { it.isIssued }
        .mapNotNull { it.numistaTypeId }
        .toSet()
    return orphans.orphans.map { it.numistaTypeId }.filter { it in catalogTypes }.sorted()
}
