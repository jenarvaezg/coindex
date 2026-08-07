package com.jenarvaezg.coindex.domain

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The public surface of `:domain` is what the app calls, plus what the suite is told it may call.
 *
 * A public symbol with no caller is an interface nobody exercises, and its green test proves
 * nothing about the app. `CollectionCatalog.emissionLabelFor` is the demonstration (#222): the
 * function was right, its test was green, and the printed output had been broken for two weeks
 * because the caller was deleted in #183 and nothing said so.
 *
 * Six symbols are public with no caller **on purpose** — the disagreement reports of ADR 0021 §12
 * live in the suite and never at startup — so intent is declared at the symbol with [SuiteOnly],
 * and the list of them is pinned below. Anything else is a leftover.
 *
 * **It is a net, not a proof.** Usage is decided by name over source text, so an overload is
 * invisible when a sibling of the same name is called — the second deletion of #222,
 * `CollectionTitles.of(DerivedCollection)`, is precisely that shape and had to be found by
 * reading — and a name shared with an unrelated symbol reads as used. What it does catch is the
 * symbol that goes quiet, which is the failure #222 is about.
 *
 * `domain/build.gradle.kts` declares the trees it reads as task inputs. Without that the task is
 * UP-TO-DATE whenever only `:app` changed, which is the one change this test exists to see.
 */
class DomainSurfaceTest {
    @Test
    fun `every public symbol of the domain has a caller in production`() {
        val stranded = domainSurface.filterNot { it.suiteOnly }.filter { productionUses(it.name) == 0 }
        assertEquals(
            emptyList(),
            stranded.map { it.where },
            "un símbolo público de `:domain` sin llamador fuera de la suite no prueba nada sobre la " +
                "app: bórralo, hazlo `private`, o márcalo `@SuiteOnly` si es un informe de la suite",
        )
    }

    /**
     * The label has to keep meaning what it says. A report the app started calling is no longer a
     * report: it is production code wearing an exemption from the test above.
     */
    @Test
    fun `a suite-only symbol has no caller in production`() {
        val called = domainSurface.filter { it.suiteOnly }.filter { productionUses(it.name) > 0 }
        assertEquals(
            emptyList(),
            called.map { it.where },
            "`@SuiteOnly` dice que a esto lo llama la suite y no la app; si la app lo llama, quita la marca",
        )
    }

    /** A report nobody runs is the same leftover as an uncalled function, only labelled. */
    @Test
    fun `a suite-only symbol is exercised by the suite`() {
        val unread = domainSurface.filter { it.suiteOnly }.filter { suiteUses(it.name) == 0 }
        assertEquals(
            emptyList(),
            unread.map { it.where },
            "un informe que nadie ejecuta es el mismo resto que una función sin llamador",
        )
    }

    /**
     * The three tests above are all «find nothing», which a scanner that reads nothing passes green
     * and forever. This is what says it read the module: a top-level function, a member of a public
     * class, and a type.
     */
    @Test
    fun `the scanner sees the surface it is checking`() {
        val names = domainSurface.map { it.name }
        assertTrue("deriveCollection" in names, "no ve una función de nivel superior")
        assertTrue("emissionLabelFor" in names, "no ve un miembro de una clase pública")
        assertTrue("CollectionCatalog" in names, "no ve un tipo")
        assertFalse("DerivedCollectionAccumulator" in names, "cuenta una clase privada")
        // 346 the day this was written. The floor only has to catch a scanner reading nothing;
        // that it reads nothing *but* surface is the test below, on source written to prove it.
        assertTrue(domainSurface.size > 200, "el escáner sólo ve ${domainSurface.size} símbolos")
    }

    /**
     * What it must not mistake for surface, pinned on source written for the purpose rather than on
     * the module, whose every private member happens to share its name with a public one somewhere.
     *
     * Counting a local is not a harmless extra: `quantity` declared inside a function makes every
     * public `quantity` read as used, and the exemption is invisible because nothing declares it.
     */
    @Test
    fun `the scanner tells surface from what merely looks like it`() {
        val surface = surfaceOf("Sample.kt", withoutComments(SAMPLE))

        assertEquals(
            listOf("visible", "Public", "member", "method", "marked"),
            surface.map { it.name },
        )
        assertEquals(listOf("marked"), surface.filter { it.suiteOnly }.map { it.name })
    }

    /**
     * The exemptions, written out. Marking a symbol `@SuiteOnly` is what quiets the first test, so
     * it cannot also be the only record that it was quieted: a seventh has to be read here too.
     * These are the disagreement reports of ADR 0021 §12 and the vocabularies they are pinned by.
     */
    @Test
    fun `the exemptions are the reports that live in the suite on purpose`() {
        assertEquals(
            listOf(
                "OrphanSeeds",
                "curedIssuerCodes",
                "metalDeviations",
                "objectClassDeviations",
                "orphanCatalogCollisions",
                "thingsThatAreNotMoney",
            ),
            domainSurface.filter { it.suiteOnly }.map { it.name }.sorted(),
        )
    }
}

/** One of every shape the scanner has to tell apart, and a comment that names a hidden one. */
private val SAMPLE = """
    package sample

    /** Doc that mentions [hidden] without calling it. */
    fun visible(): Int {
        val local = 1
        return local
    }

    private fun hidden(): Int = 0

    class Public {
        val member: Int = 0

        private val secret: Int = 0

        fun method() {
            val inner = 0
        }
    }

    private class Private {
        val insideAPrivateClass: Int = 0
    }

    internal object Internal {
        val alsoInternal: Int = 0
    }

    @SuiteOnly
    fun marked(): Int = 0
""".trimIndent()

/** One public declaration of `:domain`, and whether it declares itself suite-only. */
private data class PublicSymbol(
    val name: String,
    val file: String,
    val line: Int,
    val suiteOnly: Boolean,
) {
    val where: String get() = "$file:$line $name"
}

/**
 * What a declaration is standing inside, so a member can be told from a local.
 *
 * [holdsSurface] is false for a function — everything declared inside one is local, however public
 * the function is — and [visible] carries the whole chain, so nothing inside a private class counts.
 */
private data class Enclosing(val indent: Int, val holdsSurface: Boolean, val visible: Boolean)

/** `override` is not a visibility, but what it declares is the supertype's surface and not ours. */
private val NOT_MODULE_SURFACE = listOf("private", "internal", "protected", "override")
    .map { Regex("""\b$it\b""") }

private val DECLARATION = Regex(
    """^( *)((?:@\w+(?:\([^)]*\))?\s+)*(?:\w+\s+)*?)""" +
        """(fun|object|class|interface|typealias|val|var)\s+(?:<[^>]*>\s+)?([A-Za-z_]\w*)""",
)

private val TYPE_KINDS = setOf("object", "class", "interface")

private val BLOCK_COMMENT = Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL)

/**
 * Comments blanked out rather than removed: a KDoc reference like `[metalDeviations]` is not a
 * call, and line numbers have to survive so a finding can say where it is.
 */
private fun withoutComments(source: String): String = BLOCK_COMMENT
    .replace(source) { match -> match.value.replace(Regex("""[^\n]"""), " ") }
    .lines()
    .joinToString("\n") { withoutLineComment(it) }

/**
 * The `//` that opens a comment is the one outside a string literal. Blanking the other kind eats
 * the tail of every line carrying a URL, and a use that hides there reads as no use at all.
 */
private fun withoutLineComment(line: String): String {
    var insideText = false
    var index = 0
    while (index < line.length) {
        when {
            line[index] == '\\' && insideText -> index++
            line[index] == '"' -> insideText = !insideText
            line[index] == '/' && !insideText && line.getOrNull(index + 1) == '/' ->
                return line.take(index) + " ".repeat(line.length - index)
        }
        index++
    }
    return line
}

private fun kotlinSources(vararg directories: String): List<File> = directories.map { directory ->
    File(directory).also {
        require(it.isDirectory) { "no existe el directorio de fuentes ${it.absolutePath}" }
    }
}.flatMap { root -> root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList() }

private const val DOMAIN_SOURCES = "src/main/kotlin"

private val productionSource: Map<File, String> =
    kotlinSources(DOMAIN_SOURCES, "../app/src/main/kotlin").associateWith { withoutComments(it.readText()) }

private val suiteSource: Map<File, String> =
    kotlinSources("src/test/kotlin", "../app/src/test/kotlin").associateWith { withoutComments(it.readText()) }

private val domainSurface: List<PublicSymbol> = productionSource
    .filterKeys { it.path.startsWith(DOMAIN_SOURCES) }
    .flatMap { (file, source) -> surfaceOf(file.name, source) }

/**
 * The public declarations of one file, read by indentation.
 *
 * A declaration belongs to the innermost thing still open above it, and it is surface only when
 * that thing is a type — a public one, all the way out. Anything else is a local of some function.
 */
private fun surfaceOf(fileName: String, source: String): List<PublicSymbol> {
    val lines = source.lines()
    val open = ArrayDeque<Enclosing>()
    val surface = mutableListOf<PublicSymbol>()
    lines.forEachIndexed { index, line ->
        val declaration = DECLARATION.find(line) ?: return@forEachIndexed
        val (indent, modifiers, kind, name) = declaration.destructured
        while (open.isNotEmpty() && open.last().indent >= indent.length) open.removeLast()
        val enclosing = open.lastOrNull()
        val visible = NOT_MODULE_SURFACE.none { it.containsMatchIn(modifiers) } &&
            (enclosing == null || (enclosing.holdsSurface && enclosing.visible))
        if (visible) {
            val annotated = "@SuiteOnly" in modifiers ||
                lines.take(index).lastOrNull { it.isNotBlank() }?.trim() == "@SuiteOnly"
            surface += PublicSymbol(name, fileName, index + 1, annotated)
        }
        open.addLast(Enclosing(indent.length, kind in TYPE_KINDS, visible))
    }
    return surface
}

/**
 * How many lines outside the suite name this symbol, not counting the ones that declare it.
 *
 * A name only ever mentioned where it is declared counts as zero: something `:domain` exposes and
 * nobody reads is exactly what this test is looking for.
 */
private fun productionUses(name: String): Int = usesIn(productionSource, name)

private fun suiteUses(name: String): Int = usesIn(suiteSource, name)

private fun usesIn(sources: Map<File, String>, name: String): Int {
    val mention = Regex("""\b${Regex.escape(name)}\b""")
    return sources.values.sumOf { source ->
        source.lines().count { line ->
            mention.containsMatchIn(line) && DECLARATION.find(line)?.groupValues?.get(4) != name
        }
    }
}
