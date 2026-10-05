package com.jenarvaezg.coindex.ui

import androidx.compose.material3.SnackbarDuration
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A notice with an action still goes away on its own (#435): material3 reads `actionLabel` without
 * `duration` as [SnackbarDuration.Indefinite]. Pinned at the decision and at the call sites.
 */
class NoticeDurationTest {
    @Test
    fun `an action gets the long notice and a bare one the short`() {
        assertEquals(SnackbarDuration.Long, noticeDuration(hasAction = true))
        assertEquals(SnackbarDuration.Short, noticeDuration(hasAction = false))
    }

    @Test
    fun `no snackbar under ui is shown with an action and no duration`() {
        val sources = File("src/main/kotlin/com/jenarvaezg/coindex/ui")
            .walkTopDown()
            .filter { it.extension == "kt" }
            .sortedBy { it.path }
            .toList()

        // A loose floor that catches the scan reading nothing.
        assertTrue(sources.size > 40, "the scan found no sources to read")

        val offenders = sources.flatMap { file ->
            indefiniteCalls(file.readText()).map { "${file.name}: showSnackbar($it)" }
        }

        assertEquals(emptyList(), offenders, "an actionLabel without a duration is Indefinite")
    }

    @Test
    fun `the scan reads a call the way material3 does`() {
        assertEquals(
            listOf("message = text, actionLabel = \"Abrir\""),
            indefiniteCalls("""showSnackbar(message = text, actionLabel = "Abrir")"""),
        )
        assertEquals(
            emptyList(),
            indefiniteCalls("showSnackbar(\n  actionLabel = a,\n  duration = SnackbarDuration.Long,\n)"),
        )
        // No action, no problem: material3 defaults those to Short on its own.
        assertEquals(emptyList(), indefiniteCalls("showSnackbar(NO_VIEWER)"))
        // A nested call must not end the argument list early.
        assertEquals(
            listOf("actionLabel = openFile?.let { label(it) }"),
            indefiniteCalls("showSnackbar(actionLabel = openFile?.let { label(it) })"),
        )
        // A bracket inside a literal is not a bracket.
        assertEquals(
            listOf("actionLabel = \")\""),
            indefiniteCalls("showSnackbar(actionLabel = \")\")"),
        )
    }

    /**
     * Arguments of every `showSnackbar` with an action and no duration. Brackets are counted and
     * literals skipped, since `actionLabel = openFile?.let { … }` nests a call.
     */
    private fun indefiniteCalls(source: String): List<String> {
        val calls = mutableListOf<String>()
        var index = source.indexOf(CALL)
        while (index >= 0) {
            val open = index + CALL.length - 1
            val close = closingBracket(source, open)
            if (close > open) {
                val arguments = source.substring(open + 1, close).trim().trimEnd(',')
                if ("actionLabel" in arguments && "duration" !in arguments) calls += arguments
            }
            index = source.indexOf(CALL, index + CALL.length)
        }
        return calls
    }

    private fun closingBracket(source: String, open: Int): Int {
        var depth = 0
        var i = open
        while (i < source.length) {
            when (val c = source[i]) {
                '(' -> depth++
                ')' -> if (--depth == 0) return i
                '"', '\'' -> i = endOfLiteral(source, i, c)
            }
            i++
        }
        return -1
    }

    private fun endOfLiteral(source: String, start: Int, quote: Char): Int {
        var i = start + 1
        while (i < source.length) {
            when (source[i]) {
                '\\' -> i++
                quote -> return i
            }
            i++
        }
        return source.length
    }

    private companion object {
        const val CALL = "showSnackbar("
    }
}
