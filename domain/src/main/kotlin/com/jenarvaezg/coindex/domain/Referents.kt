package com.jenarvaezg.coindex.domain

/**
 * The fourteen things the collection is compared against, each of which is a drawing.
 *
 * An enum and not a string, so the drawing and the figure cannot drift: the silhouettes are hand-made
 * and are part of the identity — «no son un asset que se pueda descargar» (`docs/ux/cifras-326.md`) —
 * and a referent whose name was typed twice would be a ladder with a blank rung.
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
 * Which magnitude a ladder measures, which decides the unit its rungs are written in.
 *
 * The unit is the ladder's and not the figure's: the collection's weight is accumulated in grams and
 * read in kilos, and a rung that carried its own unit would let one ladder mix the two.
 */
enum class LadderUnit(val suffix: String) {
    Kilograms("kg"),
    Metres("m"),
    Centimetres("cm"),
}

/**
 * Which of the three ladders this is.
 *
 * The sentence each one is read with — «una al lado de otra llegan a» — is **copy and lives with the
 * copy** (ADR 0026 §6), so the domain names the ladder and the screen says it. It is not furniture
 * either way: without that sentence the two lower ladders are two lines with animals on them, which is
 * why those five words pass the density bar of #305 (`docs/ux/cifras-326.md`).
 */
enum class LadderKind {
    Weight,
    Row,
    Stack,
}

/** One ladder of five referents, and what magnitude it measures. */
data class Ladder(val kind: LadderKind, val unit: LadderUnit, val rungs: List<Rung>)

/**
 * Where the collection stands on a ladder, on an **ordinal** scale.
 *
 * The five rungs are equally spaced and the collection is interpolated between its two neighbours. A
 * logarithmic scale was tried first and piled three labels on top of the fourth — the bowling ball and
 * the tyre overlapped and the lorry disappeared under the mark — and that is also why the ladder
 * **carries no zoom**: zooming an ordinal scale means nothing, and making it metric brings the
 * overlaps back.
 *
 * @param fraction 0 at the first rung, 1 at the last.
 * @param justPassed the rung the collection has already gone by, or null while it is under the first.
 * @param nextUp the rung that pulls it forward, or null once it is over the last — which is the day
 *   the ladder has to grow, and it is a datum of the app and not of the collector.
 */
data class LadderPlacement(
    val fraction: Double,
    val justPassed: Rung?,
    val nextUp: Rung?,
)

/**
 * All three ladders: what the collection weighs, what it reaches, and what it raises.
 *
 * They are literals and not a rule, and both halves of that matter. The amounts are what a brick and a
 * bowling ball actually weigh, so nothing here is derived from the collection; and the ladder is fixed,
 * which is what buys the thing no lone number gives — **the next rung pulls forward**. As coins arrive
 * you see what has just been passed and what is within reach. It is `revela, no reproches` applied to
 * matter (#304).
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
 * Places a value on a ladder.
 *
 * Below the first rung it sits at the bottom with nothing passed yet; over the last it sits at the top
 * with nothing left to reach, and that is the state that says the list of referents has to grow.
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
