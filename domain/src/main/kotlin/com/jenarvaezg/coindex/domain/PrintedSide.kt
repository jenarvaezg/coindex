package com.jenarvaezg.coindex.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Which face of its coins a catalog prints when the page prints one (#227); the notebook prints one
 * face per coin, at 1:1 (#169).
 *
 * Numista's `reverse` is not always the face of the coin: on the 50 gourdes of Haiti the reverse is
 * the coat of arms and the mermaid is on the obverse. The criterion is the face the collector
 * recognises as the piece (Britannia, the Amur tiger, the mermaid), even if a date run then prints
 * the same picture in every cell. It is the curator's declaration, never inferred: a heuristic over
 * the cached descriptions already failed on two of five.
 *
 * Absent means [Reverse], so the notebook is unchanged until a curator declares otherwise (#229).
 * When both faces are printed (#230) the cell shows obverse then reverse regardless.
 */
@Serializable
enum class PrintedSide {
    @SerialName("obverse")
    Obverse,

    @SerialName("reverse")
    Reverse,
    ;

    /**
     * The face on the back of the coin, which a casilla turns over to on a tap (#337). Defined here
     * so «the other side» means one thing.
     */
    val other: PrintedSide
        get() = when (this) {
            Obverse -> Reverse
            Reverse -> Obverse
        }
}
