package com.jenarvaezg.coindex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.ui.components.CardAction
import com.jenarvaezg.coindex.ui.components.Eyebrow
import com.jenarvaezg.coindex.ui.components.FieldCard
import com.jenarvaezg.coindex.ui.components.LinkText
import com.jenarvaezg.coindex.ui.components.PrimaryAction
import com.jenarvaezg.coindex.ui.theme.Paper

/*
 * PROTOTIPO DESECHABLE — ticket #18 del mapa #16.
 *
 * Tres formas rivales del primer nivel, conmutables desde la barra flotante de abajo:
 *
 *   A · Índice de colecciones — lo de hoy, tal cual sale en el móvil del padre.
 *   B · Dos jerarquías hermanas — el primer nivel se parte en Colecciones y Monedas.
 *   C · Un cuaderno, dos vistas — el cuaderno sigue siendo el objeto; Láminas/Monedas es
 *       una lectura del mismo material, no otra jerarquía.
 *
 * Sin tests, sin datos reales, sin navegación de verdad: el estado vive en memoria. Los datos
 * son los medidos en #17 sobre el móvil del padre (58 tarjetas, 33 con catálogo, 4 completas,
 * 15 a 1/N, 50 sin clasificar) con nombres y tamaños sacados de `data/`. Se borra al resolver.
 */

// ─── Datos ────────────────────────────────────────────────────────────────────────────────

data class ProtoPlate(
    val family: String,
    val variant: String,
    val types: Int,
    val pieces: Int,
    val filled: Int?,
    val total: Int?,
    val issuer: String,
)

private val ISSUERS = mapOf(
    "afrique_du_sud" to "Sudáfrica", "allemagne_rfa" to "Alemania, República Federal de",
    "armenie" to "Armenia", "australie" to "Australia", "autriche" to "Austria",
    "canada" to "Canadá", "chine" to "China, República Popular", "egypte" to "Egipto",
    "espagne" to "España", "etats-unis" to "Estados Unidos", "france" to "Francia",
    "gibraltar" to "Gibraltar", "haiti" to "Haití (1804-presente)",
    "inde-britannique" to "India Británica", "italie" to "Italia", "jersey" to "Jersey",
    "malaisie" to "Malasia", "maroc" to "Marruecos", "mexique" to "México", "niue" to "Niue",
    "pays-bas" to "Países Bajos", "portugal" to "Portugal",
    "rome" to "Romano, Imperio (27 a. C. - 395 d. C.)", "royaume-uni" to "Reino Unido",
    "rwanda" to "Ruanda", "suede" to "Suecia", "venezuela" to "Venezuela",
)

private fun p(
    family: String,
    variant: String,
    types: Int,
    pieces: Int,
    filled: Int?,
    total: Int?,
    issuer: String,
) = ProtoPlate(family, variant, types, pieces, filled, total, ISSUERS[issuer] ?: issuer)

val PROTO_PLATES: List<ProtoPlate> = listOf(
    p("Fuertes de Venezuela", "0,804 oz", 22, 41, 22, 22, "venezuela"),
    p("Medios de Venezuela", "0,040 oz", 18, 24, 18, 18, "venezuela"),
    p("Reales de Venezuela", "0,080 oz", 22, 31, 22, 22, "venezuela"),
    p("1 Bolívar de Venezuela", "0,161 oz", 22, 38, 22, 22, "venezuela"),
    p("2 Bolívares de Venezuela", "0,322 oz", 19, 26, 19, 25, "venezuela"),
    p("100 Pesetas de Franco", "0,611 oz", 4, 12, 4, 5, "espagne"),
    p("Capitales de provincia y ciudades autónomas", "0,434 oz", 1, 1, 1, 52, "espagne"),
    p("Onza Libertad bullion anual", "1 oz", 1, 3, 1, 44, "mexique"),
    p("Australian Kookaburra", "1 oz", 1, 1, 1, 37, "australie"),
    p("Vienna Philharmonic bullion anual", "1 oz", 1, 1, 1, 19, "autriche"),
    p("Noah's Ark 1 oz bullion anual", "1 oz", 1, 1, 1, 16, "armenie"),
    p("Noah's Ark ½ oz bullion anual", "0,500 oz", 1, 1, 1, 16, "armenie"),
    p("Noah's Ark ¼ oz bullion anual", "0,250 oz", 1, 1, 1, 16, "armenie"),
    p("Silver Britannia .999 bullion anual", "1 oz", 1, 1, 1, 15, "royaume-uni"),
    p("Silver Britannia .958 1 oz", "1,043 oz", 1, 1, 1, 15, "royaume-uni"),
    p("Panda de plata 30 g bullion anual", "0,965 oz", 1, 1, 1, 11, "chine"),
    p("Silver Krugerrand bullion anual desde 2018", "1 oz", 1, 3, 1, 9, "afrique_du_sud"),
    p("Lunar ounce", "1 oz", 1, 1, 1, 10, "rwanda"),
    p("Nautical Ounce", "1 oz", 1, 1, 1, 10, "rwanda"),
    p("Southern Cross bullion anual", "1 oz", 1, 1, 1, 2, "niue"),
    p("St George and the Dragon", "1 oz", 1, 1, 1, 3, "royaume-uni"),
    p("American Silver Eagle bullion anual", "1 oz", 2, 3, 2, 42, "etats-unis"),
    p("Silver Maple Leaf bullion anual", "1 oz", 3, 4, 3, 39, "canada"),
    p("Australian Koala", "1 oz", 5, 6, 5, 20, "australie"),
    p("Lunar Series II", "1 oz", 4, 5, 4, 12, "australie"),
    p("The Royal Tudor Beasts", "2 oz", 3, 3, 3, 10, "royaume-uni"),
    p("The Queen's Beasts", "2 oz", 6, 7, 6, 11, "royaume-uni"),
    p("Panda de plata 1 oz bullion anual", "1 oz", 8, 9, 8, 27, "chine"),
    p("Serie de monedas de plata obtenidas a valor facial", "0,579 oz", 12, 14, 12, 37, "espagne"),
    p("500 escudos conmemorativos de plata .500", "0,450 oz", 7, 7, 7, 7, "portugal"),
    p("1000 escudos conmemorativos de plata .500", "0,900 oz", 9, 10, 9, 19, "portugal"),
    p("Dólar conmemorativo de plata .500 de Canadá", "0,750 oz", 5, 5, 5, 21, "canada"),
    p("10 gulden conmemorativos de Beatrix", "0,482 oz", 3, 3, 3, 5, "pays-bas"),
    p("French regions", "0,289 oz", 4, 4, null, null, "france"),
    p("Royal Diadem series", "1 oz", 2, 2, null, null, "royaume-uni"),
    p("System 1969-1980", "0,401 oz", 3, 3, null, null, "portugal"),
    p("System 1927-1968", "0,321 oz", 2, 2, null, null, "portugal"),
    p("Disney 100 Years of Wonder", "1 oz", 1, 1, null, null, "niue"),
    p("Tribute to the Spanish Army", "0,868 oz", 1, 1, null, null, "espagne"),
    p("Europa Star", "0,450 oz", 1, 1, null, null, "portugal"),
    p("Austria and its People", "0,643 oz", 1, 1, null, null, "autriche"),
    p("Millennium", "0,868 oz", 1, 2, null, null, "espagne"),
    p("Charlemagme - Mounted Knight", "1 oz", 1, 1, null, null, "gibraltar"),
    p("5 marcos conmemorativos", "0,353 oz", 4, 5, null, null, "allemagne_rfa"),
    p("Dólar de plata Morgan", "0,773 oz", 3, 3, null, null, "etats-unis"),
    p("500 pesetas de plata", "0,579 oz", 2, 2, null, null, "espagne"),
    p("Denario imperial", "Conjunto", 2, 2, null, null, "rome"),
    p("Gourde de plata", "0,289 oz", 2, 3, null, null, "haiti"),
    p("500 lire Caravelle", "0,289 oz", 2, 2, null, null, "italie"),
    p("Florín de Australia", "0,321 oz", 2, 2, null, null, "australie"),
    p("Rupia de la India Británica", "0,343 oz", 1, 1, null, null, "inde-britannique"),
    p("Corona sueca de plata", "0,225 oz", 1, 1, null, null, "suede"),
    p("5 francos Hércules", "0,804 oz", 1, 1, null, null, "france"),
    p("Dirham de Marruecos", "0,193 oz", 1, 1, null, null, "maroc"),
    p("Crown de Jersey", "0,911 oz", 1, 1, null, null, "jersey"),
    p("Ringgit de Malasia", "0,225 oz", 1, 1, null, null, "malaisie"),
    p("Piastra de Egipto", "0,180 oz", 1, 1, null, null, "egypte"),
    p("Escudo de Portugal", "0,321 oz", 1, 1, null, null, "portugal"),
).sortedBy { it.family.lowercase() }

data class ProtoPiece(
    val title: String,
    val issuer: String,
    val year: Int?,
    val medal: Boolean,
    val typeId: Int,
    val collections: List<String>,
) {
    val classified: Boolean get() = collections.isNotEmpty()
}

private fun c(
    title: String,
    issuer: String,
    year: Int?,
    medal: Boolean,
    id: Int,
    collections: List<String>,
) = ProtoPiece(title, issuer, year, medal, id, collections)

val PROTO_PIECES: List<ProtoPiece> = listOf(
    c("Medal - 1200 Jahre Münzgeschichte (Euro)", "Alemania, República Fe…", 2002, true, 132242, listOf()),
    c("1 Onza", "México", 1949, true, 13333, listOf("Onza Troy de México")),
    c("1 Onza", "México", 1978, true, 13398, listOf("Onza Troy de México")),
    c("1 ECU (Don Quixote of La Mancha)", "España", 1994, true, 18156, listOf()),
    c("1 Euro (Ground forces)", "España", 1998, true, 18161, listOf()),
    c("5 ECU (Charles V)", "España", 1989, true, 19125, listOf()),
    c("2 Dollars - Charles III (Equilibrium)", "Niue", 2025, true, 477907, listOf("Equilibrium")),
    c("2 Dollars - Charles III (Scrooge McDuck)", "Niue", 2025, true, 484131, listOf()),
    c("2 Dollars - Charles III (Southern Cross)", "Niue", 2025, true, 485082, listOf("Southern Cross bullion anual")),
    c("Medal - 400th Anniversary of Caracas", "Venezuela", null, true, 578835, listOf()),
    c("Medalla Antonio José de Sucre 1795-1995 – Ba…", "Venezuela", null, true, 581856, listOf()),
    c("10 Diners - Joan Martí i Alanis (Council of …", "Andorra", 1995, true, 59404, listOf()),
    c("3 Euro (Discovery of the mainland)", "España", 1998, true, 67681, listOf()),
    c("35 ECUs / 25 Pounds - Elizabeth II (Knight)", "Gibraltar", 1992, false, 104170, listOf()),
    c("½ Rupee - George VI", "India Británica", 1942, false, 10613, listOf()),
    c("5 Pesos", "México", 1947, false, 10919, listOf()),
    c("20 Escudos (Financial Renewal)", "Portugal", 1953, false, 11158, listOf()),
    c("5 Euros (Pope John XXI)", "Portugal", 2005, false, 11440, listOf()),
    c("100 Pesos", "México", 1977, false, 11552, listOf()),
    c("50 Schilling (Garden Exhibition)", "Austria", 1974, false, 12454, listOf()),
    c("5 Kronor - Gustaf VI Adolf", "Suecia", 1954, false, 12994, listOf()),
    c("50 Escudos (Os Lusiadas)", "Portugal", 1972, false, 13027, listOf()),
    c("50 Escudos (Marechal Carmona)", "Portugal", 1969, false, 13173, listOf()),
    c("Nummus - Licinius I (IOVI CONSERVATORI; Cyzi…", "Romano, Imperio (27 a.…", 316, false, 131809, listOf()),
    c("10 Euros (Nebra Sky Disc)", "Alemania, República Fe…", 2008, false, 13203, listOf()),
    c("8 Euros (Passarola)", "Portugal", 2007, false, 13289, listOf()),
    c("5 Euros (People in Europe)", "Italia", 2003, false, 13695, listOf()),
    c("10 Euros (People in Europe)", "Italia", 2003, false, 13706, listOf()),
    c("½ Dirham - Abd al-Aziz (Paris)", "Marruecos", 1897, false, 14018, listOf()),
    c("10 Bolívares (Bolivar coins)", "Venezuela", 1973, false, 14538, listOf()),
    c("2½ Pounds - Elizabeth II (Silver Wedding)", "Jersey", 1972, false, 15357, listOf()),
    c("10 Euros (Guadeloupe, 1st type)", "Francia", 2010, false, 15486, listOf()),
    c("10 Euros (French Guiana, 1st type)", "Francia", 2010, false, 15487, listOf()),
    c("1 Dollar - Elizabeth II (4th Portrait - Koal…", "Australia", 2017, false, 100525, listOf("Australian Koala")),
    c("2 Roubles (P.P. Semyonov-Tyan-Shansky)", "Federación de Rusia (1…", 2017, false, 101685, listOf("Outstanding Personalities of Russia")),
    c("500 Escudos (Return of Macau to China)", "Portugal", 1999, false, 10207, listOf("500 escudos conmemorativos de plata .500 de Portugal")),
    c("1 Bolívar", "Venezuela", 1879, false, 10338, listOf("1 Bolívar de Venezuela")),
    c("2 Bolívares", "Venezuela", 1879, false, 10339, listOf("2 Bolívares de Venezuela")),
    c("5 Bolívares", "Venezuela", 1879, false, 10340, listOf("Fuertes de Venezuela")),
    c("1 Bolívar", "Venezuela", 1947, false, 10398, listOf("1 Bolívar de Venezuela")),
    c("2 Bolívares", "Venezuela", 1947, false, 10399, listOf("2 Bolívares de Venezuela")),
    c("1000 Escudos (Dom Manuel I)", "Portugal", 1998, false, 10658, listOf("1000 escudos conmemorativos de plata .500 de Portugal")),
    c("3 Roubles (Surb-Khach Monastery)", "Federación de Rusia (1…", 2017, false, 107292, listOf("Architectural Monuments of Russia")),
    c("5 Pounds - Elizabeth II (Red Dragon of Wales…", "Reino Unido", 2017, false, 107370, listOf("The Queen's Beasts")),
    c("50 Francs (Year of the Rooster)", "Ruanda", 2017, false, 107917, listOf("Lunar ounce")),
    c("12 Euros - Juan Carlos I (Spanish Presidency…", "España", 2010, false, 10814, listOf("Serie de monedas de plata obtenidas a valor facial")),
    c("1 Dollar - Elizabeth II (3rd Portrait - Kook…", "Australia", 1995, false, 10841, listOf("Australian Kookaburra")),
    c("1 Dollar - Elizabeth II (Silver Jubilee)", "Canadá", 1977, false, 10973, listOf("Dólar conmemorativo de plata .500 de Canadá")),
    c("2 Roubles (K.D. Balmont)", "Federación de Rusia (1…", 2017, false, 110873, listOf("Outstanding Personalities of Russia")),
    c("1000 Escudos (Pauliteiros Dancers)", "Portugal", 1997, false, 11120, listOf("1000 escudos conmemorativos de plata .500 de Portugal")),
    c("30 Euros - Felipe VI (Treaty on the European…", "España", 2017, false, 111783, listOf("Serie de monedas de plata obtenidas a valor facial")),
    c("1 Dollar - Elizabeth II (Calgary)", "Canadá", 1975, false, 11564, listOf("Dólar conmemorativo de plata .500 de Canadá")),
    c("500 Escudos (Banco de Portugal)", "Portugal", 1996, false, 11696, listOf("500 escudos conmemorativos de plata .500 de Portugal")),
    c("1000 Escudos (D. João II)", "Portugal", 1995, false, 11697, listOf("1000 escudos conmemorativos de plata .500 de Portugal")),
    c("1000 Escudos (Our Lady of Conception)", "Portugal", 1996, false, 11698, listOf("1000 escudos conmemorativos de plata .500 de Portugal")),
    c("1000 Escudos (Oceanographic Expeditions)", "Portugal", 1997, false, 11699, listOf("1000 escudos conmemorativos de plata .500 de Portugal")),
    c("1000 Escudos (International Year of Oceans)", "Portugal", 1998, false, 11700, listOf("1000 escudos conmemorativos de plata .500 de Portugal")),
    // Añadida a mano: hoy NINGÚN tipo curado vive en dos colecciones (0 de 723), así que sin
    // esta fila la lista de pertenencias nunca se vería con más de un elemento.
    c("1 Dollar 'Morgan Dollar'", "Estados Unidos", 1878, false, 1492, listOf(
        "Dólar de plata clásico de EE. UU.",
        "Plata a valor facial de EE. UU.",
    )),
)

/** Los números del móvil del padre, medidos en #17. */
private const val PIECES = 572
private const val TYPES = 191
private const val UNCLASSIFIED = 50
private val MEDALS = PROTO_PIECES.count { it.medal }

// ─── El conmutador ────────────────────────────────────────────────────────────────────────

private val VARIANTS = listOf(
    "A — Índice de colecciones (hoy)",
    "B — Dos jerarquías hermanas",
    "C — Un cuaderno, dos vistas",
)

@Composable
fun PrototypeFirstLevel(modifier: Modifier = Modifier) {
    var variant by remember { mutableStateOf(0) }
    Box(modifier.fillMaxSize()) {
        when (variant) {
            0 -> VariantA()
            1 -> VariantB()
            else -> VariantC()
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .background(Paper.ink)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "◀",
                color = Paper.paper,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .clickable { variant = (variant + VARIANTS.size - 1) % VARIANTS.size }
                    .padding(horizontal = 8.dp),
            )
            Text(
                VARIANTS[variant],
                color = Paper.paper,
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                "▶",
                color = Paper.paper,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .clickable { variant = (variant + 1) % VARIANTS.size }
                    .padding(horizontal = 8.dp),
            )
        }
    }
}

private val PAGE = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 96.dp)

// ─── A · Índice de colecciones (hoy) ──────────────────────────────────────────────────────

@Composable
private fun VariantA() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PAGE,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Eyebrow("Cuaderno de colección")
                Text("Láminas de plata", style = MaterialTheme.typography.displayLarge)
                Text(
                    "Propuestas a partir de las piezas que tienes ahora mismo.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Paper.muted,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 14.dp),
                ) {
                    PrimaryAction(text = "Sincronizar", onClick = {})
                    CardAction(text = "Sin clasificar · $UNCLASSIFIED", onClick = {})
                }
                Text(
                    "Sincronizado hace 2 horas · Presupuesto de la API: 41 / 1000 llamadas",
                    style = MaterialTheme.typography.labelLarge,
                    color = Paper.muted,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
        item { BlockHeading("Seguidas") }
        items(PROTO_PLATES) { plate -> PlateCard(plate, actions = true) }
    }
}

// ─── B · Dos jerarquías hermanas ──────────────────────────────────────────────────────────

private enum class BranchB { Fork, Collections, Coins }

@Composable
private fun VariantB() {
    var where by remember { mutableStateOf(BranchB.Fork) }
    when (where) {
        BranchB.Fork -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PAGE,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Eyebrow("Cuaderno de colección")
                    Text("Coindex", style = MaterialTheme.typography.displayLarge)
                    Text(
                        "Dos maneras de entrar en lo mismo.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Paper.muted,
                    )
                }
            }
            item {
                DoorCard(
                    eyebrow = "Por serie",
                    title = "Colecciones",
                    count = "${PROTO_PLATES.size}",
                    lines = listOf(
                        "33 con lámina curada · 25 propuestas sueltas",
                        "4 completas · 15 con una sola casilla",
                    ),
                    onClick = { where = BranchB.Collections },
                )
            }
            item {
                DoorCard(
                    eyebrow = "Por pieza",
                    title = "Monedas",
                    count = "$PIECES",
                    lines = listOf(
                        "$TYPES tipos distintos · $PIECES piezas",
                        "$UNCLASSIFIED sin colección · $MEDALS medallas y fichas",
                    ),
                    onClick = { where = BranchB.Coins },
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PrimaryAction(text = "Sincronizar", onClick = {})
                }
            }
        }

        BranchB.Collections -> PlatesList(
            actions = true,
            header = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LinkText(
                        text = "← Coindex",
                        style = MaterialTheme.typography.labelLarge,
                        onClick = { where = BranchB.Fork },
                    )
                    Text("Colecciones", style = MaterialTheme.typography.displayLarge)
                    Text(
                        "${PROTO_PLATES.size} series derivadas de tus piezas.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Paper.muted,
                    )
                }
            },
        )

        BranchB.Coins -> CoinsList(
            onOpenCollection = { where = BranchB.Collections },
            header = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LinkText(
                        text = "← Coindex",
                        style = MaterialTheme.typography.labelLarge,
                        onClick = { where = BranchB.Fork },
                    )
                    Text("Monedas", style = MaterialTheme.typography.displayLarge)
                    Text(
                        "$PIECES piezas · $TYPES tipos distintos.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Paper.muted,
                    )
                }
            },
        )
    }
}

@Composable
private fun DoorCard(
    eyebrow: String,
    title: String,
    count: String,
    lines: List<String>,
    onClick: () -> Unit,
) {
    FieldCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), emphasized = true) {
        Eyebrow(eyebrow)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(title, style = MaterialTheme.typography.displayLarge)
            Text(count, style = MaterialTheme.typography.displayLarge, color = Paper.rust)
        }
        lines.forEach {
            Text(
                it,
                style = MaterialTheme.typography.labelLarge,
                color = Paper.muted,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

// ─── C · Un cuaderno, dos vistas ──────────────────────────────────────────────────────────

@Composable
private fun VariantC() {
    var plates by remember { mutableStateOf(true) }
    val header = @Composable {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Eyebrow("Cuaderno de colección")
            Text("Láminas de plata", style = MaterialTheme.typography.displayLarge)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                Tab("Láminas · ${PROTO_PLATES.size}", plates) { plates = true }
                Tab("Monedas · $PIECES", !plates) { plates = false }
            }
        }
    }
    if (plates) {
        PlatesList(actions = false, header = header)
    } else {
        CoinsList(onOpenCollection = { plates = true }, header = header)
    }
}

@Composable
private fun Tab(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) Paper.paper else Paper.ink,
        modifier = Modifier
            .background(if (selected) Paper.ink else Paper.card)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

// ─── Las dos listas, compartidas por B y C ────────────────────────────────────────────────

/** Las colecciones, con los filtros que el índice de hoy no tiene. */
@Composable
private fun PlatesList(actions: Boolean, header: @Composable () -> Unit) {
    var filter by remember { mutableStateOf(0) }
    val curated = PROTO_PLATES.filter { it.total != null }
    val shown = when (filter) {
        1 -> curated
        2 -> curated.filter { it.filled == it.total }
        3 -> curated.filter { it.filled != it.total }
        4 -> PROTO_PLATES.filter { it.total == null }
        else -> PROTO_PLATES
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PAGE,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { header() }
        item {
            FilterRow(
                listOf(
                    "Todas · ${PROTO_PLATES.size}",
                    "Con lámina · ${curated.size}",
                    "Completas · ${curated.count { it.filled == it.total }}",
                    "A medias · ${curated.count { it.filled != it.total }}",
                    "Sin lámina · ${PROTO_PLATES.count { it.total == null }}",
                ),
                filter,
            ) { filter = it }
        }
        items(shown) { plate -> PlateCard(plate, actions = actions) }
    }
}

/**
 * Las monedas. Cada ficha lleva las colecciones en las que vive, como enlaces de vuelta: la
 * moneda es la puerta a su colección, y la lista admite varias aunque hoy nunca haya más de una.
 */
@Composable
private fun CoinsList(onOpenCollection: (String) -> Unit, header: @Composable () -> Unit) {
    var filter by remember { mutableStateOf(0) }
    val shown = when (filter) {
        1 -> PROTO_PIECES.filter { !it.classified }
        2 -> PROTO_PIECES.filter { it.medal }
        3 -> PROTO_PIECES.filter { it.classified }
        else -> PROTO_PIECES
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PAGE,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { header() }
        item {
            FilterRow(
                listOf(
                    "Todas · $PIECES",
                    "Sin colección · $UNCLASSIFIED",
                    "Medallas · $MEDALS",
                    "En alguna colección",
                ),
                filter,
            ) { filter = it }
        }
        items(shown) { piece ->
            FieldCard(modifier = Modifier.fillMaxWidth()) {
                Eyebrow(piece.issuer)
                Text(piece.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    buildString {
                        append(piece.year?.toString() ?: "Sin año")
                        append(" · N# ").append(piece.typeId)
                        if (piece.medal) append(" · Medalla")
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = Paper.muted,
                    modifier = Modifier.padding(top = 2.dp),
                )
                if (piece.collections.isEmpty()) {
                    Text(
                        "En ninguna colección",
                        style = MaterialTheme.typography.labelLarge,
                        color = Paper.rust,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                } else {
                    Text(
                        if (piece.collections.size == 1) "En esta colección" else "En estas colecciones",
                        style = MaterialTheme.typography.labelMedium,
                        color = Paper.muted,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    piece.collections.forEach { name ->
                        LinkText(
                            text = name,
                            style = MaterialTheme.typography.labelLarge,
                            onClick = { onOpenCollection(name) },
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterRow(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        labels.chunked(2).forEachIndexed { row, chunk ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                chunk.forEachIndexed { column, label ->
                    val index = row * 2 + column
                    Chip(label, index == selected) { onSelect(index) }
                }
            }
        }
    }
}

@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) Paper.paper else Paper.ink,
        modifier = Modifier
            .background(if (selected) Paper.moss else Paper.card)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

// ─── Piezas comunes ───────────────────────────────────────────────────────────────────────

@Composable
private fun BlockHeading(title: String) {
    Column {
        HorizontalDivider(color = Paper.line)
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

@Composable
private fun PlateCard(plate: ProtoPlate, actions: Boolean) {
    FieldCard(modifier = Modifier.fillMaxWidth()) {
        Eyebrow(plate.issuer)
        LinkText(
            text = plate.family,
            style = MaterialTheme.typography.titleLarge,
            onClick = {},
        )
        Text(plate.variant, style = MaterialTheme.typography.bodyLarge)
        Text(
            buildString {
                append(plate.types).append(if (plate.types == 1) " tipo distinto" else " tipos distintos")
                append(" · ").append(plate.pieces).append(if (plate.pieces == 1) " pieza" else " piezas")
                if (plate.filled != null && plate.total != null) {
                    append(" · ").append(plate.filled).append("/").append(plate.total)
                }
            },
            style = MaterialTheme.typography.labelLarge,
            color = Paper.muted,
            modifier = Modifier.padding(top = 4.dp),
        )
        if (actions) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 10.dp),
            ) {
                if (plate.total != null) CardAction(text = "Ver lámina", onClick = {})
                CardAction(text = "Dejar de seguir", onClick = {})
            }
        }
    }
}
