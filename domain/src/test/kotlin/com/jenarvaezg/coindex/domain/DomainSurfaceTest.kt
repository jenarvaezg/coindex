package com.jenarvaezg.coindex.domain

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Every public symbol of `:domain` has a caller in the app, or is marked [SuiteOnly] (#222). A
 * public function with no caller keeps a green test while the app is broken, as
 * `CollectionCatalog.emissionLabelFor` did after #183 deleted its caller.
 *
 * A net, not a proof: usage is matched by name over source text, so an overload hides behind a
 * called sibling and a name shared with an unrelated symbol reads as used.
 *
 * `domain/build.gradle.kts` declares the trees it reads as task inputs; without that the task
 * stays UP-TO-DATE when only `:app` changed.
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

    /** A report the app calls is production code wearing an exemption from the test above. */
    @Test
    fun `a suite-only symbol has no caller in production`() {
        val called = domainSurface.filter { it.suiteOnly }.filter { productionUses(it.name) > 0 }
        assertEquals(
            emptyList(),
            called.map { it.where },
            "`@SuiteOnly` dice que a esto lo llama la suite y no la app; si la app lo llama, quita la marca",
        )
    }

    @Test
    fun `a suite-only symbol is exercised by the suite`() {
        val unread = domainSurface.filter { it.suiteOnly }.filter { suiteUses(it.name) == 0 }
        assertEquals(
            emptyList(),
            unread.map { it.where },
            "un informe que nadie ejecuta es el mismo resto que una función sin llamador",
        )
    }

    /** The tests above all expect nothing, which a scanner that reads nothing would pass. */
    @Test
    fun `the scanner sees the surface it is checking`() {
        val names = domainSurface.map { it.name }
        assertTrue("deriveCollection" in names, "no ve una función de nivel superior")
        assertTrue("emissionLabelFor" in names, "no ve un miembro de una clase pública")
        assertTrue("CollectionCatalog" in names, "no ve un tipo")
        assertFalse("DerivedCollectionAccumulator" in names, "cuenta una clase privada")
        // The floor only has to catch a scanner that reads nothing.
        assertTrue(domainSurface.size > 200, "el escáner sólo ve ${domainSurface.size} símbolos")
    }

    /**
     * On a written sample, since the module's private names all collide with public ones. A counted
     * local would hide misses: a local `quantity` makes every public `quantity` read as used.
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
     * `@SuiteOnly` silences the first test, so the exemptions are also listed here: the
     * disagreement reports of ADR 0021 §12 with their vocabularies, and the cured tables of
     * ADR 0023 and ADR 0031.
     */
    @Test
    fun `the exemptions are the reports that live in the suite on purpose`() {
        assertEquals(
            listOf(
                "OrphanSeeds",
                "curedFamilyLabels",
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

private data class PublicSymbol(
    val name: String,
    val file: String,
    val line: Int,
    val suiteOnly: Boolean,
) {
    val where: String get() = "$file:$line $name"
}

/**
 * The scope a declaration sits in. [holdsSurface] is false for a function, whose declarations are
 * all locals; [visible] carries the whole chain, so nothing inside a private class counts.
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
 * A KDoc reference like `[metalDeviations]` is not a call. Blanked rather than removed so that line
 * numbers survive.
 */
private fun withoutComments(source: String): String = BLOCK_COMMENT
    .replace(source) { match -> match.value.replace(Regex("""[^\n]"""), " ") }
    .lines()
    .joinToString("\n") { withoutLineComment(it) }

/** Only a `//` outside a string literal opens a comment; otherwise a URL blanks its line's tail. */
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
 * The public declarations of one file, read by indentation: a declaration belongs to the innermost
 * scope still open above it, and is surface only inside a chain of public types.
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

/** Lines in production sources that name the symbol, not counting its declarations. */
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
