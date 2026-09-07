package com.jenarvaezg.coindex.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Copy lives in one place (ADR 0026 §6).
 *
 * The 110 hand-written strings of the pruning entered **without a test**, and the eleven copy
 * tests did not fail because they were not looking at them: there were two places to write copy
 * and only one of them had tests. This one closes that, and it closes it the only way that
 * cannot be argued with — a literal containing prose may not reach a visible slot anywhere under
 * `ui/`.
 *
 * **Visible is not the same as on screen** (#543). Since the notebook, the app has a second surface
 * that reads words out of `ui/` — the paper, whose slots are the fields of a `PrintSection` and not
 * the arguments of a `Text(` — and until this test learned them the exporter was the unwatched half:
 * two of its four sections wrote their furniture out by hand. What outlives the app is the folio, so
 * this is the surface where a wording nobody reviewed cannot be corrected later.
 *
 * **There are no exemptions, and deliberately no list of files.** Not symbols, not Ajustes, not
 * onboarding, not interpolations. A whitelist with a reason *is* the back door: nobody rejects a
 * pull request that adds one line with its comment, and in six months the list is the map of
 * everything unwatched. The twelve copy files need no entry here because copy files hold no
 * visible slots — they hold strings, and screens ask them for one.
 *
 * What it does **not** defend is the bar of §5: a new wall of prose written correctly inside
 * `Labels.kt` passes green. Density is three clauses in the document, measured on the AVD by a
 * review with the dump in front of it. This test defends only that prose is visible in one place.
 */
class CopyLivesInOnePlaceTest {
    private val ui = File("src/main/kotlin/com/jenarvaezg/coindex/ui")

    /**
     * The slots §6 names, and the two of this album's own that fill them positionally.
     *
     * `Text(` needs its word boundary or `setContentText(` matches it, and a notification is not a
     * Compose slot. `Eyebrow(` and `Facet(` are here because their parameters are **already** on
     * §6's list — `fun Eyebrow(text: String)`, `fun Facet(title: String)` — and an album that
     * wraps `Text` in a composable of its own moved the slot, not the copy. Naming them is not an
     * exemption: the list only ever grows, and what §6 forbids is taking something off it.
     */
    private val screenSlots = listOf(
        "Text(",
        "text =",
        "label =",
        "placeholder =",
        "title =",
        "supportingText =",
        "contentDescription =",
        "Eyebrow(",
        "Facet(",
    )

    /**
     * The paper's own slots, which are a second surface and not a second rule (#543).
     *
     * A section of the notebook is a value and not a composable, so **not one of its words goes
     * through a `Text(`**: the eyebrow, the heading, the rows of the specification, the strip at the
     * foot and every line of a cell are fields of [com.jenarvaezg.coindex.ui.print.PrintSection] and
     * [com.jenarvaezg.coindex.ui.print.PrintCell], and a scan that only knew about Compose watched
     * half the app. It showed: two of the four sections asked the printed labels for their furniture
     * and the other two wrote it out, so «tu colección en Numista» was in the copy file *and* twice
     * in the exporter, and the paper drifted against itself.
     *
     * `title =` is on the list above already, and it is the same slot on both surfaces. A cell's label
     * is watched from both of its sides — `curatedLabel` and `name` are mutually exclusive by
     * construction — and the second side is watched **at the value and not at the field**:
     * `CoinName(` and `coinName(` earn their place the way `Eyebrow(` did in the amendment of #342,
     * because their parameters are strings that get printed. `name =` was tried and is not here: it
     * matches `val name = …` as well, and one local of a copy file dragged that file's own prose into
     * a slot nothing had ever printed from — a scanner that cries wolf is how a whitelist gets
     * written.
     *
     * `PrintSection(` and `PrintCell(` close the positional way in, which on paper has no `Text(`
     * underneath to catch it: a section written by position hands its eyebrow — or a cell its label —
     * to an argument with no name on it. Both are what #342 called moving the slot rather than the
     * copy, and both read their first argument, which is the one that carries the wording.
     *
     * `state =` is the widest slot on either list, because a screen calls plenty of things `state`.
     * It is here because the mark of a wished casilla travels in that field and nowhere else, and if
     * it ever goes red over something that is not visible copy, the answer is a narrower slot — never
     * an exemption, which is the one thing §6 forbids.
     *
     * A page outlives the app, which makes this the half where a wording that got away cannot be
     * taken back by an update.
     */
    private val paperSlots = listOf(
        "PrintSection(",
        "PrintCell(",
        "eyebrow =",
        "subtitle =",
        "facts =",
        "source =",
        "curatedLabel =",
        "CoinName(",
        "coinName(",
        "footnote =",
        "state =",
    )

    private val slots = screenSlots + paperSlots

    @Test
    fun `no visible slot under ui is handed a literal containing prose`() {
        val screens = ui.walkTopDown().filter { it.extension == "kt" }.sortedBy { it.path }.toList()

        // A test that scans nothing passes for ever. `ui/` holds some seventy files, so this floor
        // is nowhere near the real count and still catches the one way this goes quietly green: the
        // module moving and `walkTopDown` returning an empty sequence.
        assertTrue(screens.size > 40, "the scan found no screens to read: ${ui.absolutePath}")

        val offenders = screens
            .flatMap { file -> prosaicSlots(file).map { "${file.name}:${it.line} ${it.literal}" } }

        assertEquals(
            emptyList(),
            offenders,
            "copy belongs in a copy file, not in a screen and not on a folio",
        )
    }

    @Test
    fun `the scan sees prose through the shapes a screen actually writes`() {
        val cases = mapOf(
            """Text("hola")""" to listOf("\"hola\""),
            "Text(\n    \"a través de la línea\",\n)" to listOf("\"a través de la línea\""),
            """CardAction(text = if (x) "Ocultar" else "Mostrar")""" to
                listOf("\"Ocultar\"", "\"Mostrar\""),
            // Two slots reach the same string, and it is one offence, not two.
            """OutlinedTextField(label = { Text("anidado") })""" to listOf("\"anidado\""),
            """Text("Colecciones · ${'$'}count")""" to listOf("\"Colecciones · ${'$'}count\""),
            """Text(count.toString(), style = x)""" to emptyList(),
            // A char literal is not a string, and the bracket inside one is not a bracket: both
            // walkers step over `','` or the argument ends in the middle of itself.
            """Text(if (c == ',') a else "hola")""" to listOf("\"hola\""),
            """Text(if (c == ')') a else "hola")""" to listOf("\"hola\""),
            """Text(if (c == '"') a else "hola")""" to listOf("\"hola\""),
            // `text ==` is a comparison and not the slot `text =`.
            """if (text == "hola") x else y""" to emptyList(),
            // One literal with two strings inside its interpolation, not three literals.
            """Text("Agrupar ${'$'}{plural(count, "pieza", "piezas")}")""" to
                listOf("""Agrupar ${'$'}{plural(count, "pieza", "piezas")}""".let { "\"$it\"" }),
            """Text("${'$'}label · ${'$'}it")""" to emptyList(),
            """Text("·")""" to emptyList(),
            """contentDescription = null""" to emptyList(),
            """// Text("un comentario")""" to emptyList(),
            """/** Text("un KDoc") */""" to emptyList(),
            """Modifier.testTag("no es una ranura")""" to emptyList(),
            // And the paper's slots, which carry the same prose without a composable in sight.
            """PrintSection(eyebrow = "COINDEX · COLECCIÓN", facts = listOf("Piezas" to count))""" to
                listOf("\"COINDEX · COLECCIÓN\"", "\"Piezas\""),
            """PrintCell(curatedLabel = "1 onza", footnote = "1977")""" to listOf("\"1 onza\""),
            // The other half of a cell's label, which is two strings inside a value: the whole
            // argument is read, so the theme is seen as well as the denomination.
            """PrintCell(name = CoinName("5 Pounds", "Red Dragon"))""" to
                listOf("\"5 Pounds\"", "\"Red Dragon\""),
            // A section written by position says the same thing with no name on the argument.
            """PrintSection("COINDEX · COLECCIÓN", subject.title, null)""" to
                listOf("\"COINDEX · COLECCIÓN\""),
            """PrintCell("1 onza", state = null)""" to listOf("\"1 onza\""),
            // And what the field would have cost: a declaration is not a slot, and the prose after
            // one belongs to the copy file it was written in.
            """fun f() { val name = referent(r); return if (x) "una ${'$'}name" else "un ${'$'}name" }""" to
                emptyList(),
            """source = INVENTORY_SECTION_SOURCE""" to emptyList(),
            """state = WishLabels.MARK_WORD.takeIf { cell.wished }""" to emptyList(),
            // The geometry says whether a masthead has these, which is a shape and not a word.
            """Masthead(subtitle = true, facts = true)""" to emptyList(),
            // A ratio is figures and a slash: no wording to diverge, exactly as on screen.
            """footnote = coverage?.let { "${'$'}{it.owned}/${'$'}{it.issued}" }""" to emptyList(),
        )

        cases.forEach { (source, expected) ->
            assertEquals(expected, prosaicLiterals(source), source)
        }
    }

    private data class Offence(val line: Int, val literal: String)

    private fun prosaicSlots(file: File): List<Offence> {
        val source = withoutComments(file.readText())
        return prosaicOffsets(source).map { offset ->
            Offence(source.take(offset).count { it == '\n' } + 1, literalAt(source, offset))
        }
    }

    private fun prosaicLiterals(source: String): List<String> =
        withoutComments(source).let { clean ->
            prosaicOffsets(clean).map { literalAt(clean, it) }
        }

    /**
     * Every literal reachable from a slot, by the offset where it starts.
     *
     * The argument is read whole rather than peeked at, because `text = if (revealKey) "Ocultar"
     * else "Mostrar"` hides two strings behind a conditional and neither of them is the next
     * token after the `=`.
     */
    private fun prosaicOffsets(source: String): List<Int> {
        val found = sortedSetOf<Int>()
        slots.forEach { slot ->
            var at = source.indexOf(slot)
            while (at >= 0) {
                if (isSlot(source, slot, at)) {
                    val argument = argumentAfter(source, at + slot.length)
                    found += literalOffsets(source, argument).filter { isProse(literalAt(source, it)) }
                }
                at = source.indexOf(slot, at + 1)
            }
        }
        return found.toList()
    }

    /**
     * Whether this occurrence is the slot, or a word that merely ends in it.
     *
     * `setContentText(` is not `Text(`, and `text ==` is a comparison and not an assignment. Both
     * would have made this test lie: the first by scanning a notification, the second by going red
     * at `if (text == "hola")` — and a test that cries wolf gets a whitelist written for it.
     */
    private fun isSlot(source: String, slot: String, at: Int): Boolean {
        val before = source.getOrNull(at - 1)
        if (before != null && (before.isLetterOrDigit() || before == '_')) return false
        return !(slot.endsWith("=") && source.getOrNull(at + slot.length) == '=')
    }

    /** The argument's own text: up to a comma or a closing bracket at the depth it started at. */
    private fun argumentAfter(source: String, from: Int): IntRange {
        var depth = 0
        var at = from
        while (at < source.length) {
            when (source[at]) {
                '"' -> at = endOfLiteral(source, at) - 1
                '\'' -> at = endOfCharLiteral(source, at) - 1
                '(', '[', '{' -> depth++
                ')', ']', '}' -> {
                    if (depth == 0) return from until at
                    depth--
                }
                ',' -> if (depth == 0) return from until at
            }
            at++
        }
        return from until source.length
    }

    private fun literalOffsets(source: String, within: IntRange): List<Int> {
        val offsets = mutableListOf<Int>()
        var at = within.first
        while (at <= within.last && at < source.length) {
            when (source[at]) {
                '"' -> {
                    offsets += at
                    at = endOfLiteral(source, at)
                }
                '\'' -> at = endOfCharLiteral(source, at)
                else -> at++
            }
        }
        return offsets
    }

    /**
     * Where a char literal ends, so that `','` is one token and not a comma.
     *
     * `'"'` is the one that matters: read as a string opener it swallows the rest of the line and the
     * prose after it goes unseen — and a scanner that under-reports is worse than none, because it is
     * the same colour as one that works. Unterminated, it is one character wide: a line this cannot
     * parse must not take the rest of the file with it.
     */
    private fun endOfCharLiteral(source: String, start: Int): Int {
        var at = start + 1
        while (at < source.length && source[at] != '\n') {
            when (source[at]) {
                '\\' -> at++
                '\'' -> return at + 1
            }
            at++
        }
        return start + 1
    }

    /**
     * Where a literal ends, counting `${…}` as part of it.
     *
     * The braces matter because an interpolation may hold a string of its own —
     * `"Agrupar ${plural(count, "pieza", "piezas")}"` is one literal and not three — and reading
     * it as three would leave the rest of the file sliced along the wrong quotes.
     */
    private fun endOfLiteral(source: String, start: Int): Int {
        if (source.startsWith("\"\"\"", start)) {
            val close = source.indexOf("\"\"\"", start + 3)
            return if (close < 0) source.length else close + 3
        }
        var at = start + 1
        while (at < source.length) {
            when (source[at]) {
                '\\' -> at++
                '"' -> return at + 1
                '\n' -> return at
                '$' -> if (source.getOrNull(at + 1) == '{') at = endOfInterpolation(source, at + 1) - 1
            }
            at++
        }
        return source.length
    }

    private fun endOfInterpolation(source: String, openBrace: Int): Int {
        var depth = 0
        var at = openBrace
        while (at < source.length) {
            when (source[at]) {
                '"' -> at = endOfLiteral(source, at) - 1
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return at + 1
                }
            }
            at++
        }
        return source.length
    }

    private fun literalAt(source: String, start: Int): String =
        source.substring(start, endOfLiteral(source, start))

    /**
     * Whether a literal carries prose, which is a letter that is not part of an interpolation.
     *
     * Interpolations are **not exempt**: `"Colecciones · ${'$'}count"` is prose and goes. What is
     * left once `${'$'}count` is taken out of `"${'$'}label · ${'$'}it"` is a middle dot, and a
     * middle dot is not a word — that string is a format, and formats have no wording to diverge.
     */
    private fun isProse(literal: String): Boolean =
        literal
            .replace(Regex("""\$\{[^}]*}"""), "")
            .replace(Regex("""\$[A-Za-z_][A-Za-z0-9_]*"""), "")
            .any { it.isLetter() }

    private fun withoutComments(source: String): String {
        val kept = StringBuilder()
        var at = 0
        while (at < source.length) {
            when {
                source[at] == '"' -> {
                    val end = endOfLiteral(source, at)
                    kept.append(source, at, end)
                    at = end
                }
                // Kept whole, and **before** the comment marks are looked for: `'/'` is a char and
                // not the start of anything, and `'"'` is a char and not a string.
                source[at] == '\'' -> {
                    val end = endOfCharLiteral(source, at)
                    kept.append(source, at, end)
                    at = end
                }
                source.startsWith("//", at) -> {
                    val end = source.indexOf('\n', at).takeIf { it >= 0 } ?: source.length
                    kept.append(" ".repeat(end - at))
                    at = end
                }
                source.startsWith("/*", at) -> {
                    val end = source.indexOf("*/", at).takeIf { it >= 0 }?.plus(2) ?: source.length
                    source.substring(at, end).forEach { kept.append(if (it == '\n') '\n' else ' ') }
                    at = end
                }
                else -> {
                    kept.append(source[at])
                    at++
                }
            }
        }
        return kept.toString()
    }
}
