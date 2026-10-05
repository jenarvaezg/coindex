package com.jenarvaezg.coindex.domain

/**
 * The things the collection is compared against, each drawn by hand. An enum, so a figure and its
 * drawing cannot drift apart; the silhouettes are part of the identity (`docs/ux/cifras-326.md`).
 */
enum class Referent {
    Brick,
    Cat,
    BowlingBall,
    Tyre,
    Labrador,
    Bicycle,
    Car,
    Bus,
    Lorry,
    Whale,
    Stool,
    Shepherd,
    Countertop,
    Doorknob,
    Person,
}

/** One rung: what it is and how much of the magnitude it is worth. */
data class Rung(val referent: Referent, val amount: Double)

/**
 * Which magnitude a ladder measures, and so the unit its rungs are written in. The unit belongs to
 * the ladder, not the rung, so one ladder cannot mix grams and kilos.
 */
enum class LadderUnit(val suffix: String) {
    Kilograms("kg"),
    Metres("m"),
    Centimetres("cm"),
}

/**
 * Which of the three ladders this is. The sentence each is read with («una al lado de otra llegan
 * a») is copy and lives with the copy (ADR 0026 §6); the domain only names the ladder.
 */
enum class LadderKind {
    Weight,
    Row,
    Stack,
}

/** One ladder of five referents, and what magnitude it measures. */
data class Ladder(val kind: LadderKind, val unit: LadderUnit, val rungs: List<Rung>)

/**
 * Where the collection stands on a ladder, on an ordinal scale: the rungs are equally spaced and
 * the collection is interpolated between its two neighbours. A logarithmic scale piled labels on
 * top of each other; for the same reason the ladder has no zoom.
 *
 * @param fraction 0 at the first rung, 1 at the last.
 * @param justPassed the rung the collection has already gone by, or null while it is under the
 *   first.
 * @param nextUp the next rung to reach, or null once past the last, when the ladder needs to grow.
 */
data class LadderPlacement(
    val fraction: Double,
    val justPassed: Rung?,
    val nextUp: Rung?,
)

/**
 * All three ladders: what the collection weighs, how far it reaches in a row and how high it
 * stacks. Literal amounts (what a brick actually weighs), fixed so that as coins arrive the next
 * rung comes within reach (#304).
 */
object Ladders {
    val weight: Ladder = Ladder(
        kind = LadderKind.Weight,
        unit = LadderUnit.Kilograms,
        rungs = listOf(
            Rung(Referent.Brick, 2.0),
            Rung(Referent.Cat, 4.5),
            Rung(Referent.BowlingBall, 7.26),
            Rung(Referent.Tyre, 9.5),
            Rung(Referent.Labrador, 30.0),
        ),
    )

    val row: Ladder = Ladder(
        kind = LadderKind.Row,
        unit = LadderUnit.Metres,
        rungs = listOf(
            Rung(Referent.Bicycle, 1.8),
            Rung(Referent.Car, 4.4),
            Rung(Referent.Bus, 12.0),
            Rung(Referent.Lorry, 16.5),
            Rung(Referent.Whale, 25.0),
        ),
    )

    val stack: Ladder = Ladder(
        kind = LadderKind.Stack,
        unit = LadderUnit.Centimetres,
        rungs = listOf(
            Rung(Referent.Stool, 45.0),
            Rung(Referent.Shepherd, 60.0),
            Rung(Referent.Countertop, 90.0),
            Rung(Referent.Doorknob, 100.0),
            Rung(Referent.Person, 170.0),
        ),
    )

    val all: List<Ladder> = listOf(weight, row, stack)
}

/**
 * Places a value on a ladder. Below the first rung it sits at the bottom with nothing passed; past
 * the last it sits at the top with nothing left to reach, the sign that the ladder must grow.
 */
fun Ladder.place(value: Double): LadderPlacement {
    val last = rungs.size - 1
    if (last < 1) return LadderPlacement(0.0, null, rungs.firstOrNull())
    if (value <= rungs.first().amount) return LadderPlacement(0.0, null, rungs.first())
    if (value >= rungs.last().amount) return LadderPlacement(1.0, rungs.last(), null)
    val lower = rungs.indexOfLast { it.amount <= value }
    val below = rungs[lower]
    val above = rungs[lower + 1]
    val span = above.amount - below.amount
    val within = if (span <= 0.0) 0.0 else (value - below.amount) / span
    return LadderPlacement((lower + within) / last, below, above)
}
