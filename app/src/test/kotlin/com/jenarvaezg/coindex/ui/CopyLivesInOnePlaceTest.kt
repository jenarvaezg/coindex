package com.jenarvaezg.coindex.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Copy lives in one place (ADR 0026 §6): no literal containing prose reaches a visible slot under
 * `ui/`, on screen or on paper (#543). Copy files hold strings, not slots, so they need no entry.
 *
 * Deliberately no exemptions and no list of files: a whitelist with a reason is the back door. The
 * density bar of §5 is not checked here; a wall of prose inside `Labels.kt` passes.
 */
class CopyLivesInOnePlaceTest {
    private val ui = File("src/main/kotlin/com/jenarvaezg/coindex/ui")

    /**
     * The slots §6 names, plus `Eyebrow(` and `Facet(`, which wrap `Text` and take their string by
     * position. The list only grows (§6).
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
     * The paper's slots (#543): a notebook section is a value, so its words go through the fields
     * of [com.jenarvaezg.coindex.ui.print.PrintSection] and
     * [com.jenarvaezg.coindex.ui.print.PrintCell] rather than a `Text(`. `title =` is already on
     * the screen list.
     *
     * A cell's name is watched at the value, `CoinName(` and `coinName(` (as `Eyebrow(` was in
     * #342), because `name =` also matches `val name = …` in copy files. `PrintSection(` and
     * `PrintCell(` catch wording passed by position. `state =` is wide, but a wished casilla's mark
     * travels only in that field: if it flags something that isn't copy, narrow the slot rather
     * than add an exemption.
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

        // A loose floor that catches the module moving and the walk finding nothing.
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
            // Char literals are skipped, so a quoted comma or bracket doesn't end the argument.
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
            // The paper's slots.
            """PrintSection(eyebrow = "COINDEX · COLECCIÓN", facts = listOf("Piezas" to count))""" to
                listOf("\"COINDEX · COLECCIÓN\"", "\"Piezas\""),
            """PrintCell(curatedLabel = "1 onza", footnote = "1977")""" to listOf("\"1 onza\""),
            // The whole argument is read, so the theme is seen as well as the denomination.
            """PrintCell(name = CoinName("5 Pounds", "Red Dragon"))""" to
                listOf("\"5 Pounds\"", "\"Red Dragon\""),
            // Arguments passed by position.
            """PrintSection("COINDEX · COLECCIÓN", subject.title, null)""" to
                listOf("\"COINDEX · COLECCIÓN\""),
            """PrintCell("1 onza", state = null)""" to listOf("\"1 onza\""),
            // Why `name =` is not a slot: it would flag this declaration in a copy file.
            """fun f() { val name = referent(r); return if (x) "una ${'$'}name" else "un ${'$'}name" }""" to
                emptyList(),
            """source = INVENTORY_SECTION_SOURCE""" to emptyList(),
            """state = WishLabels.MARK_WORD.takeIf { cell.wished }""" to emptyList(),
            // Flags that shape a masthead, not words.
            """Masthead(subtitle = true, facts = true)""" to emptyList(),
            // A ratio has no letters, so it is not prose.
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
     * Start offsets of the prose literals reachable from a slot. The whole argument is read because
     * `text = if (revealKey) "Ocultar" else "Mostrar"` hides two strings behind a conditional.
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

    /** Rejects `setContentText(` as `Text(` and the comparison `text ==` as `text =`. */
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
     * Where a char literal ends, so `','` is one token. `'"'` matters most: read as a string opener
     * it would hide the prose after it. Unterminated, it is one character wide, so a line this
     * cannot parse doesn't swallow the rest of the file.
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
     * Where a literal ends, counting `${…}` as part of it:
     * `"Agrupar ${plural(count, "pieza", "piezas")}"` is one literal, not three.
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
     * Whether a literal has a letter outside its interpolations: `"Colecciones · ${'$'}count"` is
     * prose, `"${'$'}label · ${'$'}it"` is a format.
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
                // Before the comment marks, so `'/'` and `'"'` are read as chars.
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
