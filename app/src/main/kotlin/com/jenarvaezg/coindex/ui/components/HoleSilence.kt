package com.jenarvaezg.coindex.ui.components

/**
 * Why a hole is not showing a coin, when it is not showing one (#510).
 *
 * Three things can be behind an empty hole and the album used to draw two of them the same: the
 * stand-in disc stood both for «the photograph is on its way» and for «the photograph is not on
 * this phone and nothing is bringing it right now». On a phone off wifi the second is the normal
 * state of a whole plate — ADR 0024 only prefetches on an unmetered network — and it read as a
 * grid of broken images, which is what the audit of 14 August 2026 wrote down.
 *
 * The **dashed ring** of a casilla the collector is missing is not one of these: that is a fact
 * about the collection and it is drawn over whatever the photograph turned out to be.
 */
enum class HoleSilence {
    /** Asked for and not answered yet. Seconds, and it ends in a coin or in [NotOnThisPhone]. */
    Loading,

    /**
     * Asked for and answered without a picture: no network, or a URL that is not coming back.
     *
     * Always temporary and always the collector's to fix — walk into a wifi — which is why it is
     * the one of the three that says something out loud.
     */
    NotOnThisPhone,

    /** Nothing to ask for: the catalogue holds no candidate for this face. */
    NoPhotograph,
}

/**
 * Which silence this is, out of what the hole already knows.
 *
 * [settled] is the load having reported back — Coil calls it once per set of candidates, after the
 * last fallback has failed — and it is the whole of the difference between waiting and loading.
 * Nothing here consults the network, the prefetch or its refusal: a photograph that did not arrive
 * did not arrive, whatever the reason, and the reason has a screen of its own in Ajustes.
 *
 * @param candidates how many URLs this face offers, which is zero for a face with no picture.
 * @return null when the coin is on the hole and there is no silence to explain.
 */
fun holeSilence(candidates: Int, settled: Boolean, painted: Boolean): HoleSilence? = when {
    painted -> null
    candidates == 0 -> HoleSilence.NoPhotograph
    settled -> HoleSilence.NotOnThisPhone
    else -> HoleSilence.Loading
}
