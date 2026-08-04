package com.jenarvaezg.coindex.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A curated statement that some Numista types were struck for the same commemoration, and
 * nothing more (ADR 0022).
 *
 * It exists because a collection catalog answers one question — «how far along is this variant»
 * — and the collector asked a second one at the same time: the 2,50 escudos of 1977 belongs to
 * «los 2,50 escudos de cuproníquel», and it also completes, or not, the three denominations the
 * mint struck for the centenary of Alexandre Herculano. Both readings are true and they cut
 * across each other.
 *
 * It is deliberately **not** a collection: it declares no family, no weight, no finish and no
 * metal, it never reaches [deriveCollection], and it produces no card in the index. That is the
 * whole point of it being a separate file kind — a set catalog (ADR 0012) would have *won* the
 * family precedence and moved the coin off its denomination card instead of adding a second
 * reading to it.
 *
 * Its members are types, not slots, and they are **not** bounded by what the catalogs hold: the
 * 25 escudos of both Portuguese programmes sit in no catalog and never will, because neither
 * collection owns one. A progress computed by joining catalogs would have printed «1 de 2» over
 * a programme of three.
 */
@Serializable
data class CommemorativeProgramme(
    @SerialName("schema_version") val schemaVersion: Int,
    val id: String,
    val name: String,
    /** The card-sized name, under the same rules as a catalog's: required, prefix of [name]. */
    @SerialName("short_name") val shortName: String,
    @SerialName("issuer_code") val issuerCode: String,
    /** The year of the commemoration, which is one year by construction. */
    val year: Int,
    /**
     * What sustains the boundary. Any HTTPS host, unlike a catalog's `source`: a programme is
     * never a Numista fact — Numista groups these types under a technical monetary system and
     * nothing else — so requiring a Numista URL would have forced a citation that proves nothing.
     */
    val source: String,
    /** What [source] proves, in prose so the claim outlives the link. Required, never optional. */
    @SerialName("source_note") val sourceNote: String,
    @SerialName("updated_at") val updatedAt: String,
    val members: List<CommemorativeProgrammeMember>,
) {
    /** How many of [members] the collector owns, over how many the programme has. */
    fun progress(items: List<CollectedItem>): ProgrammeProgress {
        val owned = items.filter { it.quantity > 0 }.map { it.typeId }.toSet()
        return ProgrammeProgress(
            owned = members.count { it.numistaTypeId in owned },
            total = members.size,
        )
    }

    fun claims(typeId: Int): Boolean = members.any { it.numistaTypeId == typeId }

    fun validate(): CommemorativeProgrammeValidationError? {
        if (schemaVersion != 1) {
            return CommemorativeProgrammeValidationError.UnsupportedSchemaVersion(schemaVersion)
        }
        if (!isSlug(id)) {
            return CommemorativeProgrammeValidationError.InvalidId(id)
        }
        for ((field, value) in listOf(
            "programme.name" to name,
            "programme.short_name" to shortName,
            "programme.issuer_code" to issuerCode,
            "programme.source_note" to sourceNote,
            "programme.updated_at" to updatedAt,
        )) {
            if (value.isBlank()) return CommemorativeProgrammeValidationError.BlankField(field)
        }
        if (!name.startsWith(shortName)) {
            return CommemorativeProgrammeValidationError.ShortNameNotPrefix(shortName)
        }
        if (!isProgrammeSource(source)) {
            return CommemorativeProgrammeValidationError.InvalidSource
        }
        // A programme of one member is inert: it says nothing the coin's own ficha does not, and
        // it would print «1 de 1» beside a coin the collector already has.
        if (members.size < 2) {
            return CommemorativeProgrammeValidationError.NotEnoughMembers(members.size)
        }
        val seen = mutableSetOf<Int>()
        for (member in members) {
            if (member.label.isBlank()) {
                return CommemorativeProgrammeValidationError.BlankField("programme.member.label")
            }
            if (member.numistaTypeId <= 0) {
                return CommemorativeProgrammeValidationError.InvalidNumistaTypeId
            }
            if (!seen.add(member.numistaTypeId)) {
                return CommemorativeProgrammeValidationError.DuplicateNumistaTypeId(
                    member.numistaTypeId,
                )
            }
        }
        return null
    }
}

@Serializable
data class CommemorativeProgrammeMember(
    /** What the slot reads as, which for these programmes is its denomination. */
    val label: String,
    @SerialName("numista_type_id") val numistaTypeId: Int,
)

/**
 * Owned over total for one programme. Every member counts in the denominator: unlike a plate's
 * (ADR 0020), nothing here is unmeasurable — a programme names published types only.
 */
data class ProgrammeProgress(val owned: Int, val total: Int)

/** One programme a catalog touches, with how far along the collector is in it. */
data class ProgrammeStanding(
    val programme: CommemorativeProgramme,
    val progress: ProgrammeProgress,
)

/**
 * The programmes that name any type of [catalog], in file order, each with its own progress.
 *
 * The progress is over the **programme**, not over the catalog: that is the whole reading the
 * collector asked for, and it is why the 25 escudos of both Portuguese programmes count in the
 * denominator even though no catalog claims them.
 */
fun programmeStandings(
    catalog: CollectionCatalog,
    programmes: List<CommemorativeProgramme>,
    items: List<CollectedItem>,
): List<ProgrammeStanding> {
    val types = catalog.members.mapNotNull { it.numistaTypeId }.toSet()
    return programmes
        .filter { programme -> types.any(programme::claims) }
        .map { programme -> ProgrammeStanding(programme, programme.progress(items)) }
}

private fun isProgrammeSource(value: String): Boolean =
    value.startsWith("https://") && value.length > "https://".length && value.none { it == ' ' }

sealed class CommemorativeProgrammeValidationError(val message: String) {
    data class UnsupportedSchemaVersion(val version: Int) : CommemorativeProgrammeValidationError(
        "commemorative programme schema version `$version` is not supported",
    )

    data class InvalidId(val value: String) : CommemorativeProgrammeValidationError(
        "programme id `$value` must be a nonempty lowercase slug",
    )

    data class BlankField(val field: String) : CommemorativeProgrammeValidationError(
        "`$field` must not be blank",
    )

    data class ShortNameNotPrefix(val shortName: String) : CommemorativeProgrammeValidationError(
        "programme `short_name` `$shortName` must be a prefix of `name`",
    )

    data object InvalidSource : CommemorativeProgrammeValidationError(
        "programme `source` must be an https URL",
    )

    data class NotEnoughMembers(val size: Int) : CommemorativeProgrammeValidationError(
        "a commemorative programme needs at least two members, and this one has $size",
    )

    data object InvalidNumistaTypeId : CommemorativeProgrammeValidationError(
        "programme member `numista_type_id` must be a positive integer",
    )

    data class DuplicateNumistaTypeId(val typeId: Int) : CommemorativeProgrammeValidationError(
        "Numista type `$typeId` appears twice in the same commemorative programme",
    )
}
