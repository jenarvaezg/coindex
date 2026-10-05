package com.jenarvaezg.coindex.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A curated statement that some Numista types were struck for the same commemoration, and nothing
 * more (ADR 0022): a second reading across catalogs. The 2,50 escudos of 1977 belongs to the
 * cupronickel 2,50 escudos and also to the three denominations struck for the centenary of
 * Alexandre Herculano.
 *
 * Not a collection: it declares no family, weight, finish or metal, never reaches
 * [deriveCollection] and makes no card. A set catalog (ADR 0012) would instead win the family
 * precedence and move the coin off its denomination card.
 *
 * Its members are types, not slots, and need not appear in any catalog: the 25 escudos of both
 * Portuguese programmes are in none, yet each programme counts three.
 */
@Serializable
data class CommemorativeProgramme(
    @SerialName("schema_version") val schemaVersion: Int,
    val id: String,
    val name: String,
    /** The card-sized name, under the same rules as a catalog's: required, prefix of [name]. */
    @SerialName("short_name") val shortName: String,
    @SerialName("issuer_code") val issuerCode: String,
    /** The year of the commemoration; a programme has exactly one. */
    val year: Int,
    /**
     * What sustains the boundary: any HTTPS URL, unlike a catalog's `source`, because Numista files
     * these types only under a technical monetary system and cannot prove a programme.
     */
    val source: String,
    /** What [source] proves, in prose so the claim outlives the link. Required. */
    @SerialName("source_note") val sourceNote: String,
    @SerialName("updated_at") val updatedAt: String,
    val members: List<CommemorativeProgrammeMember>,
) {
    /**
     * The Numista types this programme names, held because the assembly asks once per curated
     * catalog (#539). Outside the constructor, so a curated file cannot declare it.
     */
    val typeIds: Set<Int> by lazy { members.mapTo(mutableSetOf()) { it.numistaTypeId } }

    /** How many of [members] the collector owns, over how many the programme has. */
    fun progress(items: List<CollectedItem>): ProgrammeProgress = progressOver(ownedTypeIds(items))

    /**
     * The same count against owned types gathered once for a whole assembly: the one arithmetic,
     * shared with [CatalogProgrammes].
     */
    fun progressOver(owned: Set<Int>): ProgrammeProgress = ProgrammeProgress(
        owned = members.count { it.numistaTypeId in owned },
        total = members.size,
    )

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
 * The types the collector owns at least one piece of, which a progress counts against. Shared by
 * one programme's progress and every catalog's standings (#539); a zero quantity is a piece no
 * longer owned.
 */
fun ownedTypeIds(items: List<CollectedItem>): Set<Int> =
    items.filter { it.quantity > 0 }.mapTo(mutableSetOf()) { it.typeId }

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
