package com.jenarvaezg.coindex.ui.ficha

import kotlin.test.Test
import kotlin.test.assertEquals

/** Numista's comments link to its own pages from the site's root; the browser needs the host. */
class FichaProseTest {
    @Test
    fun `a link from numista's root gets numista's host`() {
        assertEquals(
            """<a href="https://es.numista.com/catalogue/pieces1885.html">Franco</a>""",
            absoluteLinks("""<a href="/catalogue/pieces1885.html">Franco</a>"""),
        )
    }

    @Test
    fun `a link that already names its host is left alone`() {
        val html = """<a href="https://en.wikipedia.org/wiki/Caracas">Caracas</a>"""

        assertEquals(html, absoluteLinks(html))
    }
}
