package com.jenarvaezg.coindex.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Which face of its coins a catalog puts on paper when the page prints one (#227).
 *
 * The notebook prints **one face per coin** and that is settled (#169): at 1:1 the paper gives one
 * face per coin, and a second picture could only be paid for by halving the diameter, which is the
 * one thing a page measured with a ruler cannot do. What was never decided is *which* of the two.
 * The cells read Numista's `reverse` because the field is called that, and «Numista's reverse» is
 * not «the face of the coin»: on the 50 gourdes of Haiti the reverse is the coat of arms and the
 * mermaid — the coin — is on the obverse.
 *
 * The criterion is **the face that IS the coin**: the one the collector recognises as this piece.
 * Britannia, the Amur tiger, the mermaid. On a date run that prints fifteen identical Britannias,
 * and that is exactly what an album is — what tells one slot from another is the caption, not the
 * picture. It is a **declaration of the curator and never an inference**: the cache carries both
 * descriptions and an oracle over them was written to find the candidates, and it already fails on
 * two of five (it flagged the Bazhov portrait, which is the good face, as a coat of arms).
 *
 * Absent means [Reverse], which is what the seventy-three shipped plates print today, so the field
 * costs no curation to exist and the notebook is unchanged until one is written (#229 is the sweep).
 * Declaring both faces (#230) is the case where this stops mattering: the cell prints the obverse
 * and then the reverse whatever the plate declares.
 */
@Serializable
enum class PrintedSide {
    @SerialName("obverse")
    Obverse,

    @SerialName("reverse")
    Reverse,
}
