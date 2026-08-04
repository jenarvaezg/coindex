package com.jenarvaezg.coindex.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A curated statement that some Numista types belong together, and nothing more (ADR 0013).
 *
 * It exists for the types Numista files under no `series` at all: the 100 pesetas of Franco, the
 * Venezuelan circulating silver. Those pieces have no family to group by, so they pile up in the
 * unclassified list forever, however many of them the collector owns.
 *
 * It is deliberately the weakest claim in the project. It supplies a [family] and stops there:
 * it declares no members, no weight and no finish, so it can never produce a Missing state. A
 * collection catalog naming the same type outranks it, and the physical variant still comes from
 * each type's own metadata — a grouping that happened to span two weights would honestly split
 * into two derived collections rather than pretend they are one piece.
 */
@Serializable
data class CuratedGrouping(
    @SerialName("schema_version") val schemaVersion: Int,
    val id: String,
    val name: String,
    /** The card-sized name (#22): required, unique across every curated file, prefix of [name]. */
    @SerialName("short_name") val shortName: String,
    val family: String,
    @SerialName("issuer_code") val issuerCode: String,
    /** The Numista page of a representative type: every grouping stays traceable to the catalog. */
    val source: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("type_ids") val typeIds: List<Int>,
) {
    fun validate(): CuratedGroupingValidationError? {
        if (schemaVersion != 1) {
            return CuratedGroupingValidationError.UnsupportedSchemaVersion(schemaVersion)
        }
        if (!isSlug(id)) {
            return CuratedGroupingValidationError.InvalidId(id)
        }
        for ((field, value) in listOf(
            "grouping.name" to name,
            "grouping.short_name" to shortName,
            "grouping.issuer_code" to issuerCode,
            "grouping.updated_at" to updatedAt,
        )) {
            if (value.isBlank()) return CuratedGroupingValidationError.BlankField(field)
        }
        if (!name.startsWith(shortName)) {
            return CuratedGroupingValidationError.ShortNameNotPrefix(shortName)
        }
        // The family goes straight into a variant key, so it must already be canonical.
        if (normalizeFamily(family) != family || family.length > MAX_FAMILY_LENGTH) {
            return CuratedGroupingValidationError.InvalidFamily(family)
        }
        if (!isNumistaTypeSource(source)) {
            return CuratedGroupingValidationError.InvalidSource
        }
        if (typeIds.isEmpty()) {
            return CuratedGroupingValidationError.EmptyTypeIds
        }
        val seen = mutableSetOf<Int>()
        for (typeId in typeIds) {
            if (typeId <= 0) return CuratedGroupingValidationError.InvalidNumistaTypeId
            if (!seen.add(typeId)) {
                return CuratedGroupingValidationError.DuplicateNumistaTypeId(typeId)
            }
        }
        return null
    }
}

private const val MAX_FAMILY_LENGTH = 256

sealed class CuratedGroupingValidationError(val message: String) {
    data class UnsupportedSchemaVersion(val version: Int) : CuratedGroupingValidationError(
        "curated grouping schema version `$version` is not supported",
    )

    data class InvalidId(val value: String) : CuratedGroupingValidationError(
        "grouping id `$value` must be a nonempty lowercase slug",
    )

    data class BlankField(val field: String) : CuratedGroupingValidationError(
        "required field `$field` cannot be blank",
    )

    data class ShortNameNotPrefix(val value: String) : CuratedGroupingValidationError(
        "`short_name` `$value` must be a prefix of `name`",
    )

    data class InvalidFamily(val value: String) : CuratedGroupingValidationError(
        "grouping family `$value` is not a canonical family name",
    )

    data object InvalidSource : CuratedGroupingValidationError(
        "curated grouping source must be an HTTPS Numista type URL",
    )

    data object EmptyTypeIds : CuratedGroupingValidationError(
        "curated grouping must name at least one Numista type",
    )

    data object InvalidNumistaTypeId : CuratedGroupingValidationError(
        "Numista type id must be greater than zero",
    )

    data class DuplicateNumistaTypeId(val typeId: Int) : CuratedGroupingValidationError(
        "Numista type id `$typeId` is named more than once in the curated grouping",
    )
}
