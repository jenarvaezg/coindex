package com.jenarvaezg.coindex.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The index order: `(has ratio ↓, ratio ↓, denominator ↓, name ↑)` (ADR 0021 §6, #17). */
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
     * Every level of the comparator does work: `22/22` beats `2/2`, the single slot of a 52-member
     * catalog ends the ratio stretch, and the two cards with no ratio come last by name.
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

    /** A box can never hold a gap (ADR 0020), so it has no ratio (ADR 0021 §2, §6). */
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

        // «Álbum» delante de «Zeta», como en el alfabeto español: en UTF-16 crudo toda letra
        // acentuada va detrás de la Z.
        assertEquals(
            listOf("Southern Cross", "Álbum vacío", "Zeta de dos monedas"),
            cards.map { it.name },
        )
        assertNull(cards[1].coverage)
        assertEquals(0, (cards[1] as IndexCard.Box).box.quantity)
    }

    /** A series with years still unstruck is complete at `3/3` and outranks a half-owned one. */
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

    /** The same evidence `resolvePlate` demands, with no toll (ADR 0021 §7). */
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

    /** A curated card's eyebrow comes from its file, not from its pieces (ADR 0021 §9). */
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
     * Sin la vigencia de «Federación de Rusia (1991-presente)» (#180, ADR 0021 §4); la distinción
     * real la sigue haciendo `ancienne_urss` → «Unión Soviética».
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

    /** `allemagne` y `allemagne-pre1945` se curan los dos a «Alemania»: no son un desacuerdo. */
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
     * Sin fichero, las piezas son la única autoridad, igual que en una tarjeta sin fichero
     * (ADR 0021 §11, #173).
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

/** Built as `Curation.assemble` builds it, so a card and its plate share one instance (#537). */
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

/** One card per key: the index only draws what `deriveCollection` produced (ADR 0007). */
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
