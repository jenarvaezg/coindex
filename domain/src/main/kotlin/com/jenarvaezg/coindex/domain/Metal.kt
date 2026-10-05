package com.jenarvaezg.coindex.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The dominant metal of a physical variant, the last component of its key after family, weight and
 * finish (#40). Without it a one-ounce silver coin and a gold one share a key, and a second catalog
 * curated over the same family would be unreachable, since a plate is matched to its card by exact
 * key.
 *
 * Wide from the start, so curating a new alloy does not grow the enum. [Other] is not «unknown»: it
 * is a composition with no dominant metal (bimetallic, clad). An unrecorded or unrecognised
 * composition is `null`. Curated files spell entries as the lowercase codes below.
 */
@Serializable
enum class Metal {
    @SerialName("gold")
    Gold,

    @SerialName("silver")
    Silver,

    @SerialName("platinum")
    Platinum,

    @SerialName("palladium")
    Palladium,

    @SerialName("copper")
    Copper,

    @SerialName("bronze")
    Bronze,

    @SerialName("brass")
    Brass,

    @SerialName("cupronickel")
    Cupronickel,

    @SerialName("nickel")
    Nickel,

    @SerialName("steel")
    Steel,

    @SerialName("zinc")
    Zinc,

    @SerialName("aluminium")
    Aluminium,

    /** A composition with no dominant metal: bimetallic pieces and clad cores. */
    @SerialName("other")
    Other,
}

/** Canonical code used to persist a metal, including the unknown case. */
fun metalCode(metal: Metal?): String = when (metal) {
    null -> "unknown"
    Metal.Gold -> "gold"
    Metal.Silver -> "silver"
    Metal.Platinum -> "platinum"
    Metal.Palladium -> "palladium"
    Metal.Copper -> "copper"
    Metal.Bronze -> "bronze"
    Metal.Brass -> "brass"
    Metal.Cupronickel -> "cupronickel"
    Metal.Nickel -> "nickel"
    Metal.Steel -> "steel"
    Metal.Zinc -> "zinc"
    Metal.Aluminium -> "aluminium"
    Metal.Other -> "other"
}

/**
 * Parses a canonical metal code.
 *
 * Nested optional for the same reason [finishFromCode] needs one: "unknown" is a valid code for
 * the absent metal, so `null` means the code itself was not recognised.
 */
fun metalFromCode(code: String): MetalParse? = when (code) {
    "unknown" -> MetalParse(null)
    "gold" -> MetalParse(Metal.Gold)
    "silver" -> MetalParse(Metal.Silver)
    "platinum" -> MetalParse(Metal.Platinum)
    "palladium" -> MetalParse(Metal.Palladium)
    "copper" -> MetalParse(Metal.Copper)
    "bronze" -> MetalParse(Metal.Bronze)
    "brass" -> MetalParse(Metal.Brass)
    "cupronickel" -> MetalParse(Metal.Cupronickel)
    "nickel" -> MetalParse(Metal.Nickel)
    "steel" -> MetalParse(Metal.Steel)
    "zinc" -> MetalParse(Metal.Zinc)
    "aluminium" -> MetalParse(Metal.Aluminium)
    "other" -> MetalParse(Metal.Other)
    else -> null
}

/** Result of [metalFromCode]; distinguishes "unknown metal" from "unparseable code". */
@JvmInline
value class MetalParse(val metal: Metal?)

/** Stable ordering key, so two cards of the same weight and finish sort predictably. */
internal fun metalOrder(metal: Metal?): Int = when (metal) {
    null -> 0
    Metal.Silver -> 1
    Metal.Gold -> 2
    Metal.Platinum -> 3
    Metal.Palladium -> 4
    Metal.Copper -> 5
    Metal.Bronze -> 6
    Metal.Brass -> 7
    Metal.Cupronickel -> 8
    Metal.Nickel -> 9
    Metal.Steel -> 10
    Metal.Zinc -> 11
    Metal.Aluminium -> 12
    Metal.Other -> 13
}
