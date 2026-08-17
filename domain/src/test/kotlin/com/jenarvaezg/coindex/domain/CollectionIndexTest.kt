package com.jenarvaezg.coindex.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The one order of the first level: `(has ratio ↓, ratio ↓, denominator ↓, name ↑)` (ADR 0021 §6).
 *
 * No ADR had ever decided the order of the index, and what shipped was two glued orderings — boxes
 * first by SQL, everything else by the raw four-part key — under three headings of dispositions.
 * The field report of #17 measured what that cost: of the collector's 33 cards with a catalog, the
 * **4 complete plates** were scattered through the list with nothing to tell them from the **15
 * that plate a single slot**.
 */
class CollectionIndexTest {
    @Test
    fun `the cover is the first owned issue in album order on its printed side`() {
        val bolivar = catalog("venezuela-bolivar", "1 Bolívar", members = 4, typeBase = 10_000)
            .copy(printedSide = PrintedSide.Obverse)
        val firstOwned = item(10_003L, 10_003)
        val laterOwned = item(10_004L, 10_004)
        val items = listOf(laterOwned, firstOwned)
        val index = CollectionIndex(
            listOf(bolivar),
            emptyList(),
            CollectionTitles(listOf(bolivar), emptyList()),
        )

        val card = index.build(
            derivation = derivation(
                listOf(bolivar.key()),
                items,
                piecesByKey = mapOf(bolivar.key() to items),
            ),
            boxes = emptyList(),
            albums = albums(listOf(bolivar), items),
            snapshot = CollectionSnapshot(items = items),
        ).single()

        assertEquals(IndexCover(typeId = 10_003, printedSide = PrintedSide.Obverse), card.cover)
    }

    /**
     * The golden table of the comparator.
     *
     * Every level of it does work here: two complete collections are ordered by denominator so
     * `22/22` beats `2/2`, a half-done one follows, the single slot of a 52-member catalog falls to
     * the bottom of the ratio stretch, and the two cards with no issue list — a card no file names
     * and a box the collector typed — come last, in the order of their names.
     */
    @Test
    fun `the index is one list ordered by ratio, denominator and name`() {
        val reales = catalog("venezuela-reales", "Reales", members = 22, typeBase = 10_000)
        val southernCross = catalog("niue-southern-cross", "Southern Cross", 2, typeBase = 20_000)
        val lunar = catalog("lunar-ii-perth-1oz-bullion", "Lunar Series II", 12, typeBase = 30_000)
        val capitales = catalog("espana-capitales", "Capitales de provincia", 52, typeBase = 40_000)
        val catalogs = listOf(reales, southernCross, lunar, capitales)
        val owned = ownedTypes(reales, 22) +
            ownedTypes(southernCross, 2) +
            ownedTypes(lunar, 6) +
            ownedTypes(capitales, 1)
        // La tarjeta que ningún fichero nombra pinta la familia de Numista verbatim (§4).
        val fileless = VariantKey("Charlemagme - Mounted Knight", 1_000, null, Metal.Silver)
        val items = owned + item(99_000L, 99_000)
        val index = CollectionIndex(catalogs, emptyList(), CollectionTitles(catalogs, emptyList()))

        val cards = index.build(
            derivation = derivation(catalogs.map { it.key() } + fileless, items),
            boxes = listOf(box(7, "Bandeja del abuelo", items.take(2))),
            albums = albums(catalogs, items),
            snapshot = CollectionSnapshot(items = items),
        )

        assertEquals(
            listOf(
                "Reales" to "22/22",
                "Southern Cross" to "2/2",
                "Lunar Series II" to "6/12",
                "Capitales de provincia" to "1/52",
                "Bandeja del abuelo" to "sin ratio",
                "Charlemagme - Mounted Knight" to "sin ratio",
            ),
            cards.map { card -> card.name to ratioLabel(card) },
        )
    }

    /**
     * A box falls in the no-ratio stretch **without privilege** (ADR 0021 §2, §6).
     *
     * By ADR 0020 a piece you do not own is not in the inventory, so a box can never contain a gap
     * and has no ratio to offer — and the empty «Tus agrupaciones» heading both measured phones
     * showed at the top of the index disappears with the block that drew it.
     */
    @Test
    fun `a box has no ratio and no privilege, and an empty one keeps its place`() {
        val southernCross = catalog("niue-southern-cross", "Southern Cross", 2, typeBase = 20_000)
        val catalogs = listOf(southernCross)
        val items = ownedTypes(southernCross, 1)
        val index = CollectionIndex(catalogs, emptyList(), CollectionTitles(catalogs, emptyList()))

        val cards = index.build(
            derivation = derivation(listOf(southernCross.key()), items),
            // Una caja vacía sobrevive con su cero y su sitio, sin nivel extra en el comparador.
            boxes = listOf(box(1, "Zeta de dos monedas", items), box(2, "Álbum vacío", emptyList())),
            albums = albums(catalogs, items),
            snapshot = CollectionSnapshot(items = items),
        )

        // «Álbum» delante de «Zeta», que es lo que dice el alfabeto español: comparar las cadenas
        // en crudo lo manda al final de la lista, porque toda letra acentuada va detrás de la Z en
        // UTF-16. Y una caja vacía sobrevive con su cero, sin nivel extra en el comparador.
        assertEquals(
            listOf("Southern Cross", "Álbum vacío", "Zeta de dos monedas"),
            cards.map { it.name },
        )
        assertNull(cards[1].coverage)
        assertEquals(0, (cards[1] as IndexCard.Box).box.quantity)
    }

    /**
     * The denominator is what the app can measure, so an announced member never counts against the
     * collector: a series with two years still to be struck is complete at `3/3`, and it therefore
     * outranks a half-owned one instead of being buried under it.
     */
    @Test
    fun `announced members stay out of the denominator`() {
        val announced = catalog("tudor-beasts-uk-2oz", "The Royal Tudor Beasts 2 oz", 3, 50_000)
            .let { base ->
                base.copy(
                    members = base.members + listOf(
                        announcedMember("2027"),
                        announcedMember("2028"),
                    ),
                )
            }
        val catalogs = listOf(announced)
        val items = ownedTypes(announced, 3)
        val index = CollectionIndex(catalogs, emptyList(), CollectionTitles(catalogs, emptyList()))

        val cards = index.build(
            derivation = derivation(listOf(announced.key()), items),
            boxes = emptyList(),
            albums = albums(catalogs, items),
            snapshot = CollectionSnapshot(items = items),
        )

        assertEquals(CoverageRatio(3, 3), cards.single().coverage)
        assertTrue(cards.single().coverage!!.nothingMissing)
    }

    /**
     * The plate action is drawn from the same evidence `resolvePlate` demands, and the toll is gone
     * (ADR 0021 §7): a catalog curated over something the collector already owns lights its plate
     * on its own, where before the whole curation stayed invisible until they guessed.
     */
    @Test
    fun `the card offers its plate on evidence alone`() {
        val owned = catalog("niue-southern-cross", "Southern Cross", 2, typeBase = 20_000)
        val unowned = catalog("lunar-ii-perth-1oz-bullion", "Lunar Series II", 12, typeBase = 30_000)
        val catalogs = listOf(owned, unowned)
        // Una pieza de la variante sin ninguna emisión oficial del catálogo: hay tarjeta, no lámina.
        val items = ownedTypes(owned, 1) + item(31_999L, 31_999)
        val index = CollectionIndex(catalogs, emptyList(), CollectionTitles(catalogs, emptyList()))

        val cards = index.build(
            derivation = derivation(listOf(owned.key(), unowned.key()), items),
            boxes = emptyList(),
            albums = albums(catalogs, items),
            snapshot = CollectionSnapshot(items = items),
        )
        val byName = cards.filterIsInstance<IndexCard.Derived>().associateBy { it.name }

        assertEquals("niue-southern-cross", byName.getValue("Southern Cross").plateCatalogId)
        assertNull(byName.getValue("Lunar Series II").plateCatalogId)
    }

    /**
     * The eyebrow of a curated card is the country its **file** declares (ADR 0021 §9).
     *
     * Before this it was derived from the pieces in the phone, which is a leftover from when the
     * card *was* the derived collection: one uncached type among Venezuelans was enough to leave a
     * card bare while its own file knew the answer. The silence clause survives exactly where the
     * pieces are the only authority there is — the cards no file names.
     */
    @Test
    fun `the file names the country, and only a card without one can go bare`() {
        val reales = catalog("venezuela-reales", "Reales", members = 2, typeBase = 10_000)
        val catalogs = listOf(reales)
        val fileless = VariantKey("Charlemagme - Mounted Knight", 1_000, null, Metal.Silver)
        val mixed = VariantKey("Pièces de 10 francs", 1_000, null, Metal.Silver)
        val items = ownedTypes(reales, 2) + item(99_000L, 99_000) +
            item(98_001L, 98_001) + item(98_002L, 98_002)
        val typeMeta = mapOf(
            meta(10_001, "venezuela", "Venezuela"),
            // La pieza venezolana sin ficha en caché: el fichero habla igual.
            meta(99_000, "france", "Francia"),
            meta(98_001, "france", "Francia"),
            meta(98_002, "belgique", "Bélgica"),
        )
        val index = CollectionIndex(catalogs, emptyList(), CollectionTitles(catalogs, emptyList()))

        val cards = index.build(
            derivation = derivation(
                listOf(reales.key(), fileless, mixed),
                items,
                piecesByKey = mapOf(
                    reales.key() to ownedTypes(reales, 2),
                    fileless to listOf(item(99_000L, 99_000)),
                    mixed to listOf(item(98_001L, 98_001), item(98_002L, 98_002)),
                ),
            ),
            boxes = emptyList(),
            albums = albums(catalogs, items),
            snapshot = CollectionSnapshot(items = items, typeMeta = typeMeta),
        )
        val byName = cards.associate { it.name to it.issuer }

        // El fichero declara `venezuela`, y el nombre sale de la misma caché que leen las demás.
        assertEquals("Venezuela", byName.getValue("Reales"))
        assertEquals("Francia", byName.getValue("Charlemagme - Mounted Knight"))
        // Dos emisores bajo una tarjeta sin fichero: un eyebrow que cubre media tarjeta no se dice.
        assertNull(byName.getValue("Pièces de 10 francs"))
    }

    /**
     * El eyebrow dice el **país**, y no la entidad emisora con su vigencia (#180).
     *
     * Cuatro catálogos declaran `russie` y dos `chine`, y hasta aquí la tarjeta rotulaba la etiqueta
     * de Numista tal cual: «Federación de Rusia (1991-presente)» son 35 caracteres en versalitas
     * sobre un `short_name` de 20 de mediana, y un paréntesis de vigencia en la línea de identidad es
     * exactamente lo que el §4 del ADR 0021 le quitó a la `family` del Krugerrand. La distinción que
     * el paréntesis hace sí es real, y la sigue haciendo `ancienne_urss` → «Unión Soviética».
     */
    @Test
    fun `the eyebrow says the country and not Numista's issuing entity`() {
        val libroRojo = catalog("red-data-book-russia", "Libro Rojo de Rusia", 2, 10_000, "russie")
        val sovieticas = catalog("urss-rublos", "Rublos soviéticos", 1, 20_000, "ancienne_urss")
        val catalogs = listOf(libroRojo, sovieticas)
        val items = ownedTypes(libroRojo, 2) + ownedTypes(sovieticas, 1)
        val typeMeta = mapOf(
            meta(10_001, "russie", "Federación de Rusia (1991-presente)"),
            meta(10_002, "russie", "Federación de Rusia (1991-presente)"),
            meta(20_001, "ancienne_urss", "Unión Soviética"),
        )
        val index = CollectionIndex(catalogs, emptyList(), CollectionTitles(catalogs, emptyList()))

        val cards = index.build(
            derivation = derivation(
                catalogs.map { it.key() },
                items,
                piecesByKey = mapOf(
                    libroRojo.key() to ownedTypes(libroRojo, 2),
                    sovieticas.key() to ownedTypes(sovieticas, 1),
                ),
            ),
            boxes = emptyList(),
            albums = albums(catalogs, items),
            snapshot = CollectionSnapshot(items = items, typeMeta = typeMeta),
        )
        val byName = cards.associate { it.name to it.issuer }

        assertEquals("Rusia", byName.getValue("Libro Rojo de Rusia"))
        assertEquals("Unión Soviética", byName.getValue("Rublos soviéticos"))
    }

    /**
     * Dos códigos de un mismo país dejan de ser un desacuerdo, y la tarjeta sin fichero habla.
     *
     * La cláusula de silencio protege contra un eyebrow que cubre media tarjeta, no contra dos
     * momentos de la historia de Alemania: `allemagne` («Alemania, República Federal de») y
     * `allemagne-pre1945` («Alemania (1871-1948)») son el mismo país, y curados los dos a «Alemania»
     * la tarjeta rotula en vez de callar por contradecirse consigo misma.
     */
    @Test
    fun `two codes of one country agree, and the card labels instead of going silent`() {
        val alemanas = VariantKey("Deutsche Mark", 1_000, null, Metal.Silver)
        val items = listOf(item(97_001L, 97_001), item(97_002L, 97_002))
        val typeMeta = mapOf(
            meta(97_001, "allemagne", "Alemania, República Federal de"),
            meta(97_002, "allemagne-pre1945", "Alemania (1871-1948)"),
        )
        val titles = CollectionTitles(emptyList(), emptyList())
        val index = CollectionIndex(emptyList(), emptyList(), titles)

        val cards = index.build(
            derivation = derivation(listOf(alemanas), items),
            boxes = emptyList(),
            albums = albums(emptyList(), items),
            snapshot = CollectionSnapshot(items = items, typeMeta = typeMeta),
        )

        assertEquals("Alemania", cards.single().issuer)
    }

    /**
     * El eyebrow de una caja es el país de sus piezas, y calla si son varios (ADR 0021 §11, #173).
     *
     * Aquí la cláusula de silencio **no** es el error de categoría del §9: sin fichero que la nombre,
     * las piezas son la única autoridad que hay. Es la misma regla que las tarjetas sin fichero, y no
     * un privilegio ni un castigo por ser lo único que el coleccionista escribió.
     */
    @Test
    fun `the eyebrow of a box is the country of its pieces, and silent when they disagree`() {
        val catalogs = emptyList<CollectionCatalog>()
        val francesas = listOf(item(98_001L, 98_001), item(98_003L, 98_003))
        val revueltas = listOf(item(98_001L, 98_001), item(98_002L, 98_002))
        val typeMeta = mapOf(
            meta(98_001, "france", "Francia"),
            meta(98_002, "belgique", "Bélgica"),
            meta(98_003, "france", "Francia"),
        )
        val index = CollectionIndex(catalogs, emptyList(), CollectionTitles(catalogs, emptyList()))

        val cards = index.build(
            derivation = derivation(emptyList(), emptyList()),
            boxes = listOf(
                box(1, "Las francesas", francesas),
                box(2, "Revueltas", revueltas),
                // Una caja vaciada no tiene piezas que digan el país, así que va desnuda.
                box(3, "Vaciada", emptyList()),
            ),
            albums = albums(catalogs, francesas + revueltas),
            snapshot = CollectionSnapshot(items = francesas + revueltas, typeMeta = typeMeta),
        )
        val byName = cards.associate { it.name to it.issuer }

        assertEquals("Francia", byName.getValue("Las francesas"))
        assertNull(byName.getValue("Revueltas"))
        assertNull(byName.getValue("Vaciada"))
    }
}

private fun ratioLabel(card: IndexCard): String =
    card.coverage?.let { "${it.owned}/${it.issued}" } ?: "sin ratio"

private fun catalog(
    id: String,
    shortName: String,
    members: Int,
    typeBase: Int,
    issuerCode: String = "venezuela",
): CollectionCatalog = CollectionCatalog(
    schemaVersion = 1,
    id = id,
    name = "$shortName · alcance editorial entero",
    shortName = shortName,
    issuerCode = issuerCode,
    family = shortName,
    weightMillioz = 1_000 + typeBase / 10_000,
    finish = Finish.Bullion,
    metal = Metal.Silver,
    seriesStatus = SeriesStatus.Open,
    source = "https://en.numista.com/catalogue/pieces295025.html",
    updatedAt = "2026-08-04",
    members = (1..members).map { position ->
        CollectionCatalogMember(
            id = "m$position",
            label = "$position",
            year = 1_900 + position,
            numistaTypeId = typeBase + position,
        )
    },
)

private fun announcedMember(label: String) = CollectionCatalogMember(
    id = label,
    label = label,
    status = MemberStatus.Announced,
    source = "https://www.royalmint.com/",
    sourceNote = "Anunciada por la casa de la moneda y todavía sin acuñar.",
)

/**
 * The albums the assembly carries, which is what `Curation.assemble` hands the index (#537).
 *
 * Built with the same call production uses rather than per card, because the point of the parameter
 * is that the card and its plate divide by one instance and not by two agreeing rules.
 */
private fun albums(catalogs: List<CollectionCatalog>, items: List<CollectedItem>): CatalogAlbums =
    CatalogAlbums.over(catalogs, items)

/** One piece per member, for the first [count] members of [catalog]. */
private fun ownedTypes(catalog: CollectionCatalog, count: Int): List<CollectedItem> =
    catalog.members.take(count).mapNotNull { member ->
        member.numistaTypeId?.let { typeId -> item(typeId.toLong(), typeId) }
    }

private fun item(id: Long, typeId: Int) = CollectedItem(id = id, quantity = 1, typeId = typeId)

private fun meta(typeId: Int, issuerCode: String, issuerName: String) =
    typeId to TypeMeta(id = typeId, issuerCode = issuerCode, issuerName = issuerName)

private fun box(id: Long, name: String, items: List<CollectedItem>) = OwnGroupingView(
    OwnGrouping(id = id, name = name, typeIds = items.map { it.typeId }),
    items,
)

/**
 * A derivation with one card per key. The index never fabricates a card: what it draws is exactly
 * what `deriveCollection` produced from the pieces the collector owns right now (ADR 0007).
 */
private fun derivation(
    keys: List<VariantKey>,
    items: List<CollectedItem>,
    piecesByKey: Map<VariantKey, List<CollectedItem>> = emptyMap(),
): CollectionDerivation = CollectionDerivation(
    derivedCollections = keys.map { key ->
        val pieces = piecesByKey[key] ?: items
        DerivedCollection(
            family = key.family,
            weightMillioz = key.weightMillioz,
            finish = key.finish,
            metal = key.metal,
            distinctTypes = pieces.map { it.typeId }.distinct().size,
            quantity = pieces.sumOf { it.quantity },
        )
    },
    unclassified = emptyList(),
    itemsByKey = keys.associateWith { key -> piecesByKey[key] ?: items },
)
