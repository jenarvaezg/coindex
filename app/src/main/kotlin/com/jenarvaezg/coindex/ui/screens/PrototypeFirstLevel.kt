package com.jenarvaezg.coindex.ui.screens

import android.content.Context
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    val weightMillioz: Int?,
    val status: String?,
    val span: String?,
) {
    val firstYear: Int? get() = span?.take(4)?.toIntOrNull()
}

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

@Suppress("LongParameterList")
private fun p(
    family: String,
    variant: String,
    types: Int,
    pieces: Int,
    filled: Int?,
    total: Int?,
    issuer: String,
    weightMillioz: Int?,
    status: String?,
    span: String?,
) = ProtoPlate(
    family, variant, types, pieces, filled, total,
    ISSUERS[issuer] ?: issuer, weightMillioz, status, span,
)

val PROTO_PLATES: List<ProtoPlate> = listOf(
    p("Fuertes de Venezuela", "0,804 oz", 22, 41, 22, 22, "venezuela", 804, "closed", "1876-1936"),
    p("Medios de Venezuela", "0,040 oz", 18, 24, 18, 18, "venezuela", 40, "closed", "1894-1960"),
    p("Reales de Venezuela", "0,080 oz", 22, 31, 22, 22, "venezuela", 80, "closed", "1879-1960"),
    p("1 Bolívar de Venezuela", "0,161 oz", 22, 38, 22, 22, "venezuela", 161, "closed", "1879-1965"),
    p("2 Bolívares de Venezuela", "0,322 oz", 19, 26, 19, 25, "venezuela", 322, "closed", "1879-1965"),
    p("100 Pesetas de Franco", "0,611 oz", 4, 12, 4, 5, "espagne", 611, "closed", "1966-1966"),
    p("Capitales de provincia y ciudades autónomas", "0,434 oz", 1, 1, 1, 52, "espagne", 434, "closed", "2010-2012"),
    p("Onza Libertad bullion anual", "1 oz", 1, 3, 1, 44, "mexique", 1000, "open", "1982-2025"),
    p("Australian Kookaburra", "1 oz", 1, 1, 1, 37, "australie", 1000, "open", "1990-2026"),
    p("Vienna Philharmonic bullion anual", "1 oz", 1, 1, 1, 19, "autriche", 1000, "open", "2008-2026"),
    p("Noah's Ark 1 oz bullion anual", "1 oz", 1, 1, 1, 16, "armenie", 1000, "open", "2011-2026"),
    p("Noah's Ark ½ oz bullion anual", "0,500 oz", 1, 1, 1, 16, "armenie", 500, "open", "2011-2026"),
    p("Noah's Ark ¼ oz bullion anual", "0,250 oz", 1, 1, 1, 16, "armenie", 250, "open", "2011-2026"),
    p("Silver Britannia .999 bullion anual", "1 oz", 1, 1, 1, 15, "royaume-uni", 1000, "open", "2013-2026"),
    p("Silver Britannia .958 1 oz", "1,043 oz", 1, 1, 1, 15, "royaume-uni", 1043, "closed", "1998-2012"),
    p("Panda de plata 30 g bullion anual", "0,965 oz", 1, 1, 1, 11, "chine", 965, "open", "2016-2026"),
    p("Silver Krugerrand bullion anual desde 2018", "1 oz", 1, 3, 1, 9, "afrique_du_sud", 1000, "open", "2018-2026"),
    p("Lunar ounce", "1 oz", 1, 1, 1, 10, "rwanda", 1000, "open", "2017-2026"),
    p("Nautical Ounce", "1 oz", 1, 1, 1, 10, "rwanda", 1000, "open", "2017-2026"),
    p("Southern Cross bullion anual", "1 oz", 1, 1, 1, 2, "niue", 1000, "open", "2025-2026"),
    p("St George and the Dragon", "1 oz", 1, 1, 1, 3, "royaume-uni", 1000, "open", "2024-2026"),
    p("American Silver Eagle bullion anual", "1 oz", 2, 3, 2, 42, "etats-unis", 1000, "open", "1986-2026"),
    p("Silver Maple Leaf bullion anual", "1 oz", 3, 4, 3, 39, "canada", 1000, "open", "1988-2026"),
    p("Australian Koala", "1 oz", 5, 6, 5, 20, "australie", 1000, "open", "2007-2026"),
    p("Lunar Series II", "1 oz", 4, 5, 4, 12, "australie", 1000, "closed", "2008-2019"),
    p("The Royal Tudor Beasts", "2 oz", 3, 3, 3, 10, "royaume-uni", 2000, "open", "2022-2026"),
    p("The Queen's Beasts", "2 oz", 6, 7, 6, 11, "royaume-uni", 2000, "closed", "2016-2021"),
    p("Panda de plata 1 oz bullion anual", "1 oz", 8, 9, 8, 27, "chine", 1000, "closed", "1989-2015"),
    p("Serie de monedas de plata obtenidas a valor facial", "0,579 oz", 12, 14, 12, 37, "espagne", 579, "open", "1994-2026"),
    p("500 escudos conmemorativos de plata .500", "0,450 oz", 7, 7, 7, 7, "portugal", 450, "closed", "1995-2001"),
    p("1000 escudos conmemorativos de plata .500", "0,900 oz", 9, 10, 9, 19, "portugal", 900, "closed", "1992-2001"),
    p("Dólar conmemorativo de plata .500 de Canadá", "0,750 oz", 5, 5, 5, 21, "canada", 750, "closed", "1971-1991"),
    p("10 gulden conmemorativos de Beatrix", "0,482 oz", 3, 3, 3, 5, "pays-bas", 482, "closed", "1994-1999"),
    p("French regions", "0,289 oz", 4, 4, null, null, "france", 289, null, null),
    p("Royal Diadem series", "1 oz", 2, 2, null, null, "royaume-uni", 1000, null, null),
    p("System 1969-1980", "0,401 oz", 3, 3, null, null, "portugal", 401, null, null),
    p("System 1927-1968", "0,321 oz", 2, 2, null, null, "portugal", 321, null, null),
    p("Disney 100 Years of Wonder", "1 oz", 1, 1, null, null, "niue", 1000, null, null),
    p("Tribute to the Spanish Army", "0,868 oz", 1, 1, null, null, "espagne", 868, null, null),
    p("Europa Star", "0,450 oz", 1, 1, null, null, "portugal", 450, null, null),
    p("Austria and its People", "0,643 oz", 1, 1, null, null, "autriche", 643, null, null),
    p("Millennium", "0,868 oz", 1, 2, null, null, "espagne", 868, null, null),
    p("Charlemagme - Mounted Knight", "1 oz", 1, 1, null, null, "gibraltar", 1000, null, null),
    p("5 marcos conmemorativos", "0,353 oz", 4, 5, null, null, "allemagne_rfa", 353, null, null),
    p("Dólar de plata Morgan", "0,773 oz", 3, 3, null, null, "etats-unis", 773, null, null),
    p("500 pesetas de plata", "0,579 oz", 2, 2, null, null, "espagne", 579, null, null),
    p("Denario imperial", "Conjunto", 2, 2, null, null, "rome", null, null, null),
    p("Gourde de plata", "0,289 oz", 2, 3, null, null, "haiti", 289, null, null),
    p("500 lire Caravelle", "0,289 oz", 2, 2, null, null, "italie", 289, null, null),
    p("Florín de Australia", "0,321 oz", 2, 2, null, null, "australie", 321, null, null),
    p("Rupia de la India Británica", "0,343 oz", 1, 1, null, null, "inde-britannique", 343, null, null),
    p("Corona sueca de plata", "0,225 oz", 1, 1, null, null, "suede", 225, null, null),
    p("5 francos Hércules", "0,804 oz", 1, 1, null, null, "france", 804, null, null),
    p("Dirham de Marruecos", "0,193 oz", 1, 1, null, null, "maroc", 193, null, null),
    p("Crown de Jersey", "0,911 oz", 1, 1, null, null, "jersey", 911, null, null),
    p("Ringgit de Malasia", "0,225 oz", 1, 1, null, null, "malaisie", 225, null, null),
    p("Piastra de Egipto", "0,180 oz", 1, 1, null, null, "egypte", 180, null, null),
    p("Escudo de Portugal", "0,321 oz", 1, 1, null, null, "portugal", 321, null, null),).sortedBy { it.family.lowercase() }

data class ProtoPiece(
    val title: String,
    val issuer: String,
    val year: Int?,
    val medal: Boolean,
    val typeId: Int,
    val weightGrams: Double?,
    val collections: List<String>,
) {
    val classified: Boolean get() = collections.isNotEmpty()
}

@Suppress("LongParameterList")
private fun c(
    title: String,
    issuer: String,
    year: Int?,
    medal: Boolean,
    id: Int,
    weightGrams: Number?,
    collections: List<String>,
) = ProtoPiece(title, issuer, year, medal, id, weightGrams?.toDouble(), collections)

val PROTO_PIECES: List<ProtoPiece> = listOf(
    c("Medal - 1200 Jahre Münzgeschichte (Euro)", "Alemania, República Fe…", 2002, true, 132242, 20, listOf()),
    c("1 Onza", "México", 1949, true, 13333, 33.625, listOf("Onza Troy de México")),
    c("1 Onza", "México", 1978, true, 13398, 33.625, listOf("Onza Troy de México")),
    c("1 ECU (Don Quixote of La Mancha)", "España", 1994, true, 18156, 6.72, listOf()),
    c("1 Euro (Ground forces)", "España", 1998, true, 18161, 6.72, listOf()),
    c("5 ECU (Charles V)", "España", 1989, true, 19125, 33.62, listOf()),
    c("2 Dollars - Charles III (Equilibrium)", "Niue", 2025, true, 477907, 31.31, listOf("Equilibrium")),
    c("2 Dollars - Charles III (Scrooge McDuck)", "Niue", 2025, true, 484131, 31.1, listOf()),
    c("2 Dollars - Charles III (Southern Cross)", "Niue", 2025, true, 485082, 31.1, listOf("Southern Cross bullion anual")),
    c("Medal - 400th Anniversary of Caracas", "Venezuela", null, true, 578835, 15.1, listOf()),
    c("Medalla Antonio José de Sucre 1795-1995 – Ba…", "Venezuela", null, true, 581856, 31.1, listOf()),
    c("10 Diners - Joan Martí i Alanis (Council of …", "Andorra", 1995, true, 59404, 31.6, listOf()),
    c("3 Euro (Discovery of the mainland)", "España", 1998, true, 67681, 20, listOf()),
    c("35 ECUs / 25 Pounds - Elizabeth II (Knight)", "Gibraltar", 1992, false, 104170, 28.28, listOf()),
    c("½ Rupee - George VI", "India Británica", 1942, false, 10613, 5.84, listOf()),
    c("5 Pesos", "México", 1947, false, 10919, 30, listOf()),
    c("20 Escudos (Financial Renewal)", "Portugal", 1953, false, 11158, 21, listOf()),
    c("5 Euros (Pope John XXI)", "Portugal", 2005, false, 11440, 14, listOf()),
    c("100 Pesos", "México", 1977, false, 11552, 27.78, listOf()),
    c("50 Schilling (Garden Exhibition)", "Austria", 1974, false, 12454, 20, listOf()),
    c("5 Kronor - Gustaf VI Adolf", "Suecia", 1954, false, 12994, 18, listOf()),
    c("50 Escudos (Os Lusiadas)", "Portugal", 1972, false, 13027, 18, listOf()),
    c("50 Escudos (Marechal Carmona)", "Portugal", 1969, false, 13173, 18, listOf()),
    c("Nummus - Licinius I (IOVI CONSERVATORI; Cyzi…", "Romano, Imperio (27 a.…", 316, false, 131809, 3.3, listOf()),
    c("10 Euros (Nebra Sky Disc)", "Alemania, República Fe…", 2008, false, 13203, 18, listOf()),
    c("8 Euros (Passarola)", "Portugal", 2007, false, 13289, 21.1, listOf()),
    c("5 Euros (People in Europe)", "Italia", 2003, false, 13695, 18, listOf()),
    c("10 Euros (People in Europe)", "Italia", 2003, false, 13706, 22, listOf()),
    c("½ Dirham - Abd al-Aziz (Paris)", "Marruecos", 1897, false, 14018, 1.4558, listOf()),
    c("10 Bolívares (Bolivar coins)", "Venezuela", 1973, false, 14538, 30, listOf()),
    c("2½ Pounds - Elizabeth II (Silver Wedding)", "Jersey", 1972, false, 15357, 27.1, listOf()),
    c("10 Euros (Guadeloupe, 1st type)", "Francia", 2010, false, 15486, 10, listOf()),
    c("10 Euros (French Guiana, 1st type)", "Francia", 2010, false, 15487, 10, listOf()),
    c("10 Euros (Réunion, 1st type)", "Francia", 2010, false, 15500, 10, listOf()),
    c("10 Pesos (Hidalgo Grande)", "México", 1955, false, 18816, 28.888, listOf()),
    c("8 Reales - Charles IV", "México", 1791, false, 18852, 27.07, listOf()),
    c("75 Bolívares (Antonio José de Sucre)", "Venezuela", 1980, false, 18940, 17, listOf()),
    c("50 Gourdes (Woman and Child)", "Haití (1804-presente)", 1973, false, 19085, 16.5, listOf()),
    c("2 Dollars - Elizabeth II (Lion King)", "Niue", 2019, false, 192181, 31.39, listOf()),
    c("50 Gourdes (The Mermaid)", "Haití (1804-presente)", 1973, false, 19328, 16.45, listOf()),
    c("5 Deutsche Mark", "Alemania, República Fe…", 1951, false, 1933, 11.2, listOf()),
    c("10 Rentenpfennig", "Alemania (1871-1948)", 1923, false, 1952, 4, listOf()),
    c("10 Euros (Charlemagne in the Untersberg)", "Austria", 2010, false, 21377, 17.3, listOf()),
    c("1500 Pesetas - Juan Carlos I (Peace)", "España", 2000, false, 22502, 20, listOf()),
    c("10 Euros (Picardy)", "Francia", 2011, false, 25292, 10, listOf()),
    c("50 Qirsh (Evacuation of the British)", "Egipto", 1956, false, 26190, 28, listOf()),
    c("2 Dollars - Elizabeth II (Bitcoin)", "Niue", 2021, false, 272140, 31.1035, listOf()),
    c("25 Ringgit - Agong VI (Conservation)", "Malasia", 1976, false, 27380, 35, listOf()),
    c("100 Bolívares (Birth of Andres Bello)", "Venezuela", 1981, false, 27573, 27, listOf()),
    c("30 Francs (450th Anniversary Galileo)", "República Democrática …", 2014, false, 277960, 31.1, listOf()),
    c("Denarius - Ulpia Severina (VENVS FELIX; Venus)", "Romano, Imperio (27 a.…", 270, false, 291255, 2.7, listOf()),
    c("100 Francs (La Fayette)", "Francia", 1987, false, 30, 15, listOf()),
    c("1100 Bolívares (Cristobal Colón)", "Venezuela", 1991, false, 31925, 27, listOf()),
    c("100 Francs (Charlemagne)", "Francia", 1990, false, 33, 15, listOf()),
    c("10 Euros (Akacorleone)", "Portugal", 2022, false, 334727, 27, listOf()),
    c("2 Reichsmark (Paul von Hindenburg)", "Alemania (1871-1948)", 1936, false, 3416, 8, listOf()),
    c("100 Bolívares (José M. Vargas)", "Venezuela", 1986, false, 34721, 31.1, listOf()),
    c("1 Peso 'Tepalcate'", "México", 1957, false, 3550, 16, listOf()),
    c("5 Dollars - Elizabeth II (Posthumous; Superm…", "Samoa", 2023, false, 368266, 31.1, listOf()),
    c("25 Bolívares (Jaguar)", "Venezuela", 1975, false, 37246, 28.28, listOf()),
    c("50 Bolívares (Armadillo)", "Venezuela", 1975, false, 37247, 35, listOf()),
    c("25 Pesos (Olympic Games)", "México", 1968, false, 3855, 22.5, listOf()),
    c("25 Dollars - Elizabeth II (Basketball)", "Canadá", 2016, false, 387614, 30.75, listOf()),
    c("1 Dollar - Elizabeth II (4th Portrait - Koal…", "Australia", 2017, false, 100525, 31.1035, listOf("Australian Koala")),
    c("2 Roubles (P.P. Semyonov-Tyan-Shansky)", "Federación de Rusia (1…", 2017, false, 101685, 17, listOf("Outstanding Personalities of Russia")),
    c("500 Escudos (Return of Macau to China)", "Portugal", 1999, false, 10207, 14, listOf("500 escudos conmemorativos de plata .500 de Portugal")),
    c("1 Bolívar", "Venezuela", 1879, false, 10338, 5, listOf("1 Bolívar de Venezuela")),
    c("2 Bolívares", "Venezuela", 1879, false, 10339, 10, listOf("2 Bolívares de Venezuela")),
    c("5 Bolívares", "Venezuela", 1879, false, 10340, 25, listOf("Fuertes de Venezuela")),
    c("1 Bolívar", "Venezuela", 1947, false, 10398, 5, listOf("1 Bolívar de Venezuela")),
    c("2 Bolívares", "Venezuela", 1947, false, 10399, 10, listOf("2 Bolívares de Venezuela")),
    c("1000 Escudos (Dom Manuel I)", "Portugal", 1998, false, 10658, 27, listOf("1000 escudos conmemorativos de plata .500 de Portugal")),
    c("3 Roubles (Surb-Khach Monastery)", "Federación de Rusia (1…", 2017, false, 107292, 39.94, listOf("Architectural Monuments of Russia")),
    c("5 Pounds - Elizabeth II (Red Dragon of Wales…", "Reino Unido", 2017, false, 107370, 62.42, listOf("The Queen's Beasts")),
    c("50 Francs (Year of the Rooster)", "Ruanda", 2017, false, 107917, 31.1, listOf("Lunar ounce")),
    c("12 Euros - Juan Carlos I (Spanish Presidency…", "España", 2010, false, 10814, 18, listOf("Serie de monedas de plata obtenidas a valor facial")),
    c("1 Dollar - Elizabeth II (3rd Portrait - Kook…", "Australia", 1995, false, 10841, 31.1035, listOf("Australian Kookaburra")),
    c("1 Dollar - Elizabeth II (Silver Jubilee)", "Canadá", 1977, false, 10973, 23.3276, listOf("Dólar conmemorativo de plata .500 de Canadá")),
    c("2 Roubles (K.D. Balmont)", "Federación de Rusia (1…", 2017, false, 110873, 17, listOf("Outstanding Personalities of Russia")),
    c("1000 Escudos (Pauliteiros Dancers)", "Portugal", 1997, false, 11120, 28.2, listOf("1000 escudos conmemorativos de plata .500 de Portugal")),
    c("30 Euros - Felipe VI (Treaty on the European…", "España", 2017, false, 111783, 18, listOf("Serie de monedas de plata obtenidas a valor facial")),
    c("1 Dollar - Elizabeth II (Calgary)", "Canadá", 1975, false, 11564, 23.3276, listOf("Dólar conmemorativo de plata .500 de Canadá")),
    c("500 Escudos (Banco de Portugal)", "Portugal", 1996, false, 11696, 14, listOf("500 escudos conmemorativos de plata .500 de Portugal")),
    c("1000 Escudos (D. João II)", "Portugal", 1995, false, 11697, 28, listOf("1000 escudos conmemorativos de plata .500 de Portugal")),
    c("1000 Escudos (Our Lady of Conception)", "Portugal", 1996, false, 11698, 28, listOf("1000 escudos conmemorativos de plata .500 de Portugal")),
    c("1000 Escudos (Oceanographic Expeditions)", "Portugal", 1997, false, 11699, 28, listOf("1000 escudos conmemorativos de plata .500 de Portugal")),
    c("1000 Escudos (International Year of Oceans)", "Portugal", 1998, false, 11700, 27, listOf("1000 escudos conmemorativos de plata .500 de Portugal")),
    c("1000 Escudos (Council of the European Union)", "Portugal", 2000, false, 11701, 28.2, listOf("1000 escudos conmemorativos de plata .500 de Portugal")),
    c("1000 Escudos (Cavalo Lusitano)", "Portugal", 2000, false, 11702, 27, listOf("1000 escudos conmemorativos de plata .500 de Portugal")),
    c("2 Roubles (I.K. Aivazovsky)", "Federación de Rusia (1…", 2017, false, 117327, 17, listOf("Outstanding Personalities of Russia")),
    c("3 Roubles (Queen Louise Bridge)", "Federación de Rusia (1…", 2017, false, 117328, 39.94, listOf("Architectural Monuments of Russia")),
    c("1 Dollar - Elizabeth II (4th Portrait - Aust…", "Australia", 2018, false, 124796, 31.1035, listOf("Australian Kookaburra")),
    c("5 Pounds - Elizabeth II (Unicorn of Scotland…", "Reino Unido", 2018, false, 127263, 62.42, listOf("The Queen's Beasts")),
    c("10 Yuan (Panda)", "China, República Popular", 2018, false, 127720, 30, listOf("Panda de plata 30 g bullion anual")),
    c("50 Francs (Santa Maria)", "Ruanda", 2017, false, 127972, 31.1, listOf("Nautical Ounce")),
    c("2 Roubles (Y. P. Lyubimov)", "Federación de Rusia (1…", 2017, false, 128809, 17, listOf("Outstanding Personalities of Russia")),
    c("1 Dollar - Elizabeth II (4th Portrait - Year…", "Australia", 2018, false, 129091, 31.1035, listOf("Lunar Series II")),
    c("500 Escudos (Saint Anthony)", "Portugal", 1995, false, 13042, 14, listOf("500 escudos conmemorativos de plata .500 de Portugal")),
    c("500 Escudos (Padre António Vieira)", "Portugal", 1997, false, 13043, 14, listOf("500 escudos conmemorativos de plata .500 de Portugal")),
    c("500 Escudos (Vasco da Gama Bridge)", "Portugal", 1998, false, 13044, 14, listOf("500 escudos conmemorativos de plata .500 de Portugal")),
    c("500 Escudos (Eça de Queiroz)", "Portugal", 2000, false, 13045, 14, listOf("500 escudos conmemorativos de plata .500 de Portugal")),
    c("500 Escudos (European Culture Capital)", "Portugal", 2001, false, 13046, 13.96, listOf("500 escudos conmemorativos de plata .500 de Portugal")),
    c("1 Dollar - Elizabeth II (4th Portrait - Koal…", "Australia", 2018, false, 132621, 31.1035, listOf("Australian Koala")),
    c("50 Francs (Year of the Dog)", "Ruanda", 2018, false, 133446, 31.1, listOf("Lunar ounce")),
    c("2 Roubles (M. I. Petipa)", "Federación de Rusia (1…", 2018, false, 133886, 17, listOf("Outstanding Personalities of Russia")),
    c("2 Roubles (M. Gorky)", "Federación de Rusia (1…", 2018, false, 133887, 17, listOf("Outstanding Personalities of Russia")),
    c("30 Euros - Felipe VI (50th Birthday of King …", "España", 2018, false, 133929, 18, listOf("Serie de monedas de plata obtenidas a valor facial")),
    c("2 Pounds - Elizabeth II (4th portrait; 1 oz …", "Reino Unido", 1998, false, 13410, 32.45, listOf("Silver Britannia .958 1 oz")),
    c("2 Pounds - Elizabeth II (4th portrait; 1 oz …", "Reino Unido", 2003, false, 13411, 32.45, listOf("Silver Britannia .958 1 oz")),
    c("2 Pounds - Elizabeth II (4th portrait; 1 oz …", "Reino Unido", 2005, false, 13413, 32.45, listOf("Silver Britannia .958 1 oz")),
    c("2 Pounds - Elizabeth II (4th portrait; 1 oz …", "Reino Unido", 2007, false, 13414, 32.45, listOf("Silver Britannia .958 1 oz")),
    c("2 Pounds - Elizabeth II (4th portrait; 1 oz …", "Reino Unido", 1999, false, 13417, 32.45, listOf("Silver Britannia .958 1 oz")),
    c("5 Pounds - Elizabeth II (Black Bull of Clare…", "Reino Unido", 2018, false, 135553, 62.42, listOf("The Queen's Beasts")),
    c("2000 Pesetas - Juan Carlos I (Miguel de Cerv…", "España", 1997, false, 13634, 18, listOf("Serie de monedas de plata obtenidas a valor facial")),
    c("3 Roubles (Trinity Monastery)", "Federación de Rusia (1…", 2018, false, 136521, 33.94, listOf("Architectural Monuments of Russia")),
    c("3 Roubles (Saint Trinity Cathedral)", "Federación de Rusia (1…", 2018, false, 136522, 33.94, listOf("Architectural Monuments of Russia")),
    c("1 Onza 'Libertad'", "México", 2000, false, 13855, 31.1, listOf("Onza Libertad bullion anual")),
    c("2 Roubles (V. Y. Struve)", "Federación de Rusia (1…", 2018, false, 138786, 17, listOf("Outstanding Personalities of Russia")),
    c("50 Francs (HMS Endeavour)", "Ruanda", 2018, false, 139377, 31.104, listOf("Nautical Ounce")),
    c("3 Roubles (Church of the Kazan Icon of the M…", "Federación de Rusia (1…", 2018, false, 141400, 33.94, listOf("Architectural Monuments of Russia")),
    c("2 Pounds - Elizabeth II (4th portrait; 1 oz …", "Reino Unido", 2001, false, 14363, 32.45, listOf("Silver Britannia .958 1 oz")),
    c("Silver Krugerrand - 1 Rand", "Sudáfrica", 2018, false, 143754, 31.1, listOf("Silver Krugerrand bullion anual desde 2018 (sin 2017 Premium Uncirculated)")),
    c("1 Onza 'Libertad'", "México", 1982, false, 14465, 31.1, listOf("Onza Libertad bullion anual")),
    c("2000 Pesetas - Juan Carlos I (Last issue of …", "España", 2001, false, 14673, 18, listOf("Serie de monedas de plata obtenidas a valor facial")),
    c("2 Roubles (V.S. Visotzky)", "Federación de Rusia (1…", 2018, false, 148206, 17, listOf("Outstanding Personalities of Russia")),
    c("1 Dollar 'Morgan Dollar'", "Estados Unidos", 1878, false, 1492, 26.73, listOf("Dólar de plata clásico · EE. UU. · Morgan y Peace")),
    c("1 Dollar 'American Silver Eagle' (Bullion Co…", "Estados Unidos", 1986, false, 1493, 31.103, listOf("American Silver Eagle bullion anual")),
    c("100 Dinara (Nikola Tesla, Alternating current)", "Serbia", 2018, false, 150352, 31.1, listOf("Nikola Tesla")),
    c("5 Pounds - Elizabeth II (Falcon of the Plant…", "Reino Unido", 2019, false, 150354, 62.42, listOf("The Queen's Beasts")),
    c("1 Dollar - Elizabeth II (4th Portrait - Year…", "Australia", 2019, false, 150358, 31.1035, listOf("Lunar Series II")),
    c("30 Euros - Felipe VI (Kingdom of Asturias)", "España", 2018, false, 152027, 18, listOf("Serie de monedas de plata obtenidas a valor facial")),
    c("2 Roubles (A.I. Solzhenitsyn)", "Federación de Rusia (1…", 2018, false, 153797, 17, listOf("Outstanding Personalities of Russia")),
    c("3 Roubles (Saint Vladimir’s Cathedral)", "Federación de Rusia (1…", 2018, false, 153799, 33.94, listOf("Architectural Monuments of Russia")),
    c("1 Dollar - Elizabeth II (6th Portrait - Aust…", "Australia", 2019, false, 153925, 31.1035, listOf("Australian Kangaroo bullion anual")),
    c("1 Dollar - Elizabeth II (4th Portrait - Aust…", "Australia", 2002, false, 15415, 31.135, listOf("Australian Kookaburra")),
    c("1000 Escudos (Encounter of Two Worlds)", "Portugal", 1992, false, 15463, 27, listOf("1000 escudos conmemorativos de plata .500 de Portugal")),
    c("10 Yuan (Panda)", "China, República Popular", 2019, false, 154933, 30, listOf("Panda de plata 30 g bullion anual")),
    c("1 Dollar - Elizabeth II (S.S. Frontenac)", "Canadá", 1991, false, 15517, 23.327, listOf("Dólar conmemorativo de plata .500 de Canadá")),
    c("1000 Escudos (D João de Castro)", "Portugal", 2000, false, 15566, 27, listOf("1000 escudos conmemorativos de plata .500 de Portugal")),
    c("2 Pounds - Elizabeth II (5th portrait; 1 oz …", "Reino Unido", 2017, false, 157350, 31.21, listOf("Silver Britannia .999 bullion anual")),
    c("2 Roubles (V.V. Bianki)", "Federación de Rusia (1…", 2019, false, 157874, 17, listOf("Outstanding Personalities of Russia")),
    c("50 Francs (Year of the Pig)", "Ruanda", 2019, false, 158491, 31.1, listOf("Lunar ounce")),
    c("1 Dollar - Elizabeth II (6th Portrait - Koala)", "Australia", 2019, false, 160928, 31.1035, listOf("Australian Koala")),
    c("1 Dollar - Elizabeth II (6th Portrait - Aust…", "Australia", 2019, false, 161560, 31.1035, listOf("Australian Kookaburra")),
    c("5 Pounds - Elizabeth II (Yale of Beaufort; 2…", "Reino Unido", 2019, false, 161586, 62.42, listOf("The Queen's Beasts")),
    c("2 Roubles (Amur Leopard)", "Federación de Rusia (1…", 2019, false, 161739, 17, listOf("Red Data Book")),
    c("2 Roubles (Japanese Crested Ibis)", "Federación de Rusia (1…", 2019, false, 161741, 17, listOf("Red Data Book")),
    c("2 Roubles (Beluga)", "Federación de Rusia (1…", 2019, false, 161742, 17, listOf("Red Data Book")),
    c("100 Dinara (Nikola Tesla, Remote Control)", "Serbia", 2019, false, 162242, 31.1, listOf("Nikola Tesla")),
    c("3 Roubles (Savior's Transfiguration Church)", "Federación de Rusia (1…", 2017, false, 162819, 33.94, listOf("Architectural Monuments of Russia")),
    c("1 Dollar - Elizabeth II (Davis Strait)", "Canadá", 1987, false, 16314, 23.3276, listOf("Dólar conmemorativo de plata .500 de Canadá")),
    c("1 Dollar - Elizabeth II (Griffon)", "Canadá", 1979, false, 16315, 23.3276, listOf("Dólar conmemorativo de plata .500 de Canadá")),
    c("3 Roubles (Aseyev Estate)", "Federación de Rusia (1…", 2019, false, 167168, 33.94, listOf("Architectural Monuments of Russia")),
    c("3 Roubles (Main Narzan Baths)", "Federación de Rusia (1…", 2019, false, 168992, 33.94, listOf("Architectural Monuments of Russia")),
    c("50 Francs (Victoria)", "Ruanda", 2019, false, 172069, 31.104, listOf("Nautical Ounce")),
    c("1 Dollar - Elizabeth II (3rd Portrait - Aust…", "Australia", 1993, false, 17335, 31.1035, listOf("Australian Kookaburra")),
    c("1 Dollar - Elizabeth II (3rd Portrait - Aust…", "Australia", 1994, false, 17336, 31.1035, listOf("Australian Kookaburra")),
    c("1 Dollar - Elizabeth II (3rd Portrait - Aust…", "Australia", 1996, false, 17339, 31.1035, listOf("Australian Kookaburra")),
    c("1 Dollar - Elizabeth II (3rd Portrait - Aust…", "Australia", 1997, false, 17340, 31.1035, listOf("Australian Kookaburra")),
    c("1 Dollar - Elizabeth II (4th Portrait - Aust…", "Australia", 1999, false, 17342, 31.635, listOf("Australian Kookaburra")),
    c("1 Dollar - Elizabeth II (4th Portrait - Aust…", "Australia", 1999, false, 17343, 31.1035, listOf("Australian Kookaburra")),
    c("1 Dollar - Elizabeth II (4th Portrait - Aust…", "Australia", 2000, false, 17357, 31.135, listOf("Australian Kookaburra")),
    c("1 Dollar - Elizabeth II (4th Portrait - Year…", "Australia", 2009, false, 17378, 31.1035, listOf("Lunar Series II")),
    c("1 Dollar - Elizabeth II (4th Portrait - Koal…", "Australia", 2009, false, 17379, 31.1035, listOf("Australian Koala")),
    c("1 Dollar - Elizabeth II (4th Portrait - Aust…", "Australia", 2009, false, 17382, 31.1035, listOf("Australian Kookaburra")),
    c("1 Dollar - Elizabeth II (4th Portrait - Year…", "Australia", 2010, false, 17383, 31.1035, listOf("Lunar Series II")),
    c("1 Dollar - Elizabeth II (4th Portrait - Koal…", "Australia", 2010, false, 17386, 31.1035, listOf("Australian Koala")),
    c("1 Dollar - Elizabeth II (4th Portrait - Aust…", "Australia", 2010, false, 17387, 31.1035, listOf("Australian Kookaburra")),
    c("1 Dollar - Elizabeth II (4th Portrait - Year…", "Australia", 2011, false, 17388, 31.1035, listOf("Lunar Series II")),
    c("20 Euros - Juan Carlos I (2010 FIFA World Cup)", "España", 2010, false, 17413, 18, listOf("Serie de monedas de plata obtenidas a valor facial")),
    c("1 Onza 'Libertad'", "México", 1996, false, 17818, 31.1, listOf("Onza Libertad bullion anual")),
    c("1 Dollar - Elizabeth II (2nd portrait; silver)", "Canadá", 1972, false, 17839, 23.3, listOf("Dólar conmemorativo de plata .500 de Canadá")),
    c("2 Roubles (M.T. Kalashnikov)", "Federación de Rusia (1…", 2019, false, 178572, 17, listOf("Outstanding Personalities of Russia")),
    c("2 Roubles (M. Karim)", "Federación de Rusia (1…", 2019, false, 178574, 17, listOf("Outstanding Personalities of Russia")),
    c("1 Dollar - Elizabeth II (6th Portrait - Year…", "Australia", 2020, false, 179438, 31.1035, listOf("Lunar Series III")),
    c("½ Bolívar", "Venezuela", 1879, false, 17945, 2.5, listOf("Reales de Venezuela")),
    c("5 Pounds - Elizabeth II (White Lion of Morti…", "Reino Unido", 2020, false, 180354, 62.42, listOf("The Queen's Beasts")),
    c("1 Dollar - Elizabeth II (6th Portrait - Kook…", "Australia", 2020, false, 183220, 31.1035, listOf("Australian Kookaburra")),
    c("10 Yuan (Panda)", "China, República Popular", 2020, false, 183654, 30, listOf("Panda de plata 30 g bullion anual")),
    c("2 Pounds - Elizabeth II (4th portrait; 1 oz …", "Reino Unido", 2010, false, 18524, 32.45, listOf("Silver Britannia .958 1 oz")),
    c("2 Pounds - Elizabeth II (4th portrait; 1 oz …", "Reino Unido", 2008, false, 18525, 32.45, listOf("Silver Britannia .958 1 oz")),
    c("1 Dollar - Elizabeth II (6th Portrait - Year…", "Australia", 2020, false, 185343, 31.1035, listOf("Lunar Series III")),
    c("30 Euros - Felipe VI (Prado Museum)", "España", 2019, false, 186086, 18, listOf("Serie de monedas de plata obtenidas a valor facial")),
    c("5 Dollars - Elizabeth II (2nd Portrait; SML …", "Canadá", 1988, false, 18655, 31.1035, listOf("Silver Maple Leaf bullion anual")),
    c("1 Dollar - Elizabeth II (Winnipeg)", "Canadá", 1974, false, 18797, 23.33, listOf("Dólar conmemorativo de plata .500 de Canadá")),
    c("1 Dollar - Elizabeth II (Parliamentary Libra…", "Canadá", 1976, false, 1880, 23.3, listOf("Dólar conmemorativo de plata .500 de Canadá")),
    c("50 Francs (Year of the Rat)", "Ruanda", 2020, false, 188048, 31.103, listOf("Lunar ounce")),
    c("100 Pesetas - Francisco Franco", "España", 1966, false, 1885, 19, listOf("100 Pesetas de Franco")),
    c("5 Dollars - Elizabeth II (Equilibrium)", "Tokelau", 2018, false, 188952, 31.31, listOf("Equilibrium")),
    c("10 Yuan (Panda)", "China, República Popular", 1999, false, 19043, 31.1, listOf("Panda de plata 1 oz bullion anual")),
    // Añadida a mano: hoy NINGÚN tipo curado vive en dos colecciones (0 de 723), así que sin
    // esta fila la lista de pertenencias nunca se vería con más de un elemento.
    c("1 Dollar 'Morgan Dollar'", "Estados Unidos", 1878, false, 1492, 26.73, listOf(
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
                // Por encima de la barra de destinos de B, que ahora ocupa el borde de abajo.
                .align(Alignment.BottomCenter)
                .padding(bottom = 84.dp)
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

private enum class BranchB { Collections, Coins }

/**
 * B, con lo decidido en la sesión: **se abre siempre en Colecciones**, y la barra de abajo lleva
 * a Monedas y de vuelta. La pantalla de bifurcación se cae — cobraba un toque en cada arranque
 * para elegir siempre lo mismo: el 100 % de lo que hace el padre empieza abriendo una lámina.
 */
@Composable
private fun VariantB() {
    var where by remember { mutableStateOf(BranchB.Collections) }
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            when (where) {
                BranchB.Collections -> PlatesList(
                    actions = true,
                    header = {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Eyebrow("Cuaderno de colección")
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
                            Eyebrow("Cuaderno de colección")
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
        BottomBar(where) { where = it }
    }
}

/** Los dos destinos del primer nivel, siempre a un toque. */
@Composable
private fun BottomBar(where: BranchB, onGo: (BranchB) -> Unit) {
    Column {
        HorizontalDivider(color = Paper.line)
        Row(modifier = Modifier.fillMaxWidth().background(Paper.paperDeep)) {
            BottomTab(
                text = "Colecciones · ${PROTO_PLATES.size}",
                selected = where == BranchB.Collections,
                modifier = Modifier.weight(1f),
            ) { onGo(BranchB.Collections) }
            BottomTab(
                text = "Monedas · $PIECES",
                selected = where == BranchB.Coins,
                modifier = Modifier.weight(1f),
            ) { onGo(BranchB.Coins) }
        }
    }
}

@Composable
private fun BottomTab(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .background(if (selected) Paper.ink else Paper.paperDeep)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Paper.paper else Paper.ink,
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


/**
 * Comparación de búsqueda: sin acentos y sin mayúsculas, porque el que escribe «panda» en el
 * móvil no escribe «Panda» y el que busca «bolivar» no pone la tilde de «Bolívar».
 */
private fun searchable(text: String): String = text
    .lowercase()
    .replace('á', 'a').replace('é', 'e').replace('í', 'i')
    .replace('ó', 'o').replace('ú', 'u').replace('ü', 'u').replace('ñ', 'n')


// ─── Orden y memoria de la estantería ─────────────────────────────────────────────────────

/** Un criterio de orden: su nombre y el comparador. */
private class Sorting<T>(val name: String, val by: Comparator<T>)

/**
 * Lo que la estantería recuerda: qué opción hay puesta en cada faceta y por qué se ordena. Lo
 * escrito en el buscador NO se guarda a propósito — volver a abrir la app con un texto viejo en
 * la caja y media colección escondida se lee como una app rota, no como una preferencia.
 */
private class Shelf(val chosen: List<Int>, val sort: Int)

private const val SHELF_STORE = "prototipo-18"

@Composable
private fun rememberShelf(key: String, facets: Int): Pair<Shelf, (Shelf) -> Unit> {
    val context = LocalContext.current
    val store = remember { context.getSharedPreferences(SHELF_STORE, Context.MODE_PRIVATE) }
    var shelf by remember {
        val saved = store.getString(key, null)
            ?.split(",")
            ?.mapNotNull(String::toIntOrNull)
            .orEmpty()
        mutableStateOf(
            if (saved.size == facets + 1) {
                Shelf(saved.dropLast(1), saved.last())
            } else {
                Shelf(List(facets) { 0 }, 0)
            },
        )
    }
    return shelf to { next: Shelf ->
        shelf = next
        store.edit().putString(key, (next.chosen + next.sort).joinToString(",")).apply()
    }
}

/**
 * Numista no da fecha de compra: `collected_items` trae id, cantidad, tipo, emisión, grado,
 * precio y colección, y nada más. Lo único que ordena por antigüedad es el id de la pieza, que
 * es creciente — o sea «orden de alta en Numista», no «compra». En el prototipo ese orden está
 * simulado, y la pantalla lo dice cuando está puesto.
 */
private const val RECENCY_NOTE =
    "Orden simulado: Numista no da fecha de compra. El dato real sería el id de la pieza, que " +
        "es creciente, así que diría «alta más reciente» y no «comprada más tarde»."

/**
 * Una faceta: un nombre y las opciones, cada una con el predicado que recorta la lista. Todas
 * las facetas se cruzan con Y; dentro de una faceta, la opción elegida sustituye a la anterior.
 */
private class Facet<T>(val name: String, val options: List<Pair<String, (T) -> Boolean>>)

private fun <T> facetedCount(
    items: List<T>,
    facets: List<Facet<T>>,
    chosen: List<Int>,
    skip: Int,
    option: Int,
): Int = items.count { item ->
    facets.indices.all { f ->
        val pick = if (f == skip) option else chosen[f]
        pick == 0 || facets[f].options[pick].second(item)
    }
}

/** Las colecciones, con los filtros que el índice de hoy no tiene. */
@Composable
private fun PlatesList(actions: Boolean, header: @Composable () -> Unit) {
    val facets = remember { plateFacets() }
    val sortings = remember { plateSortings() }
    val (shelf, setShelf) = rememberShelf("colecciones", facets.size)
    val chosen = shelf.chosen
    var query by remember { mutableStateOf("") }
    val needle = searchable(query)
    val platesByQuery = PROTO_PLATES.filter { plate ->
        needle.isBlank() || searchable(plate.family + " " + plate.issuer).contains(needle)
    }
    val shown = platesByQuery
        .filter { plate ->
            facets.indices.all { f -> chosen[f] == 0 || facets[f].options[chosen[f]].second(plate) }
        }
        .sortedWith(sortings[shelf.sort].by)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PAGE,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { header() }
        item {
            FacetShelf(
                facets = facets,
                chosen = chosen,
                items = platesByQuery,
                total = PROTO_PLATES.size,
                shown = shown.size,
                noun = "colecciones",
                query = query,
                onQuery = { query = it },
                sortings = sortings,
                sort = shelf.sort,
                onSort = { setShelf(Shelf(chosen, it)) },
                onChoose = { f, option ->
                    setShelf(Shelf(chosen.toMutableList().also { it[f] = option }, shelf.sort))
                },
                onClear = { setShelf(Shelf(List(facets.size) { 0 }, shelf.sort)) },
            )
        }
        items(shown) { plate -> PlateCard(plate, actions = actions) }
    }
}

private fun plateFacets(): List<Facet<ProtoPlate>> = listOf(
    Facet(
        "País",
        listOf<Pair<String, (ProtoPlate) -> Boolean>>("Todos" to { true }) +
            PROTO_PLATES.groupingBy { it.issuer }.eachCount()
                .entries.sortedByDescending { it.value }.take(7)
                .map { (issuer, _) -> issuer to { plate: ProtoPlate -> plate.issuer == issuer } },
    ),
    Facet(
        "Peso",
        listOf(
            "Cualquiera" to { _: ProtoPlate -> true },
            "Menos de ½ oz" to { it: ProtoPlate -> (it.weightMillioz ?: 0) < 500 },
            "½ – 1 oz" to { it: ProtoPlate -> (it.weightMillioz ?: 0) in 500..1099 },
            "Más de 1 oz" to { it: ProtoPlate -> (it.weightMillioz ?: 0) >= 1100 },
        ),
    ),
    Facet(
        "Empieza en",
        listOf(
            "Cualquier año" to { _: ProtoPlate -> true },
            "Antes de 1950" to { it: ProtoPlate -> (it.firstYear ?: 9999) < 1950 },
            "1950 – 1999" to { it: ProtoPlate -> (it.firstYear ?: 0) in 1950..1999 },
            "Desde 2000" to { it: ProtoPlate -> (it.firstYear ?: 0) >= 2000 },
        ),
    ),
    Facet(
        "Estado",
        listOf(
            "Todas" to { _: ProtoPlate -> true },
            "Completas" to { it: ProtoPlate -> it.total != null && it.filled == it.total },
            "A medias" to { it: ProtoPlate -> it.total != null && it.filled != it.total },
            "Una sola casilla" to { it: ProtoPlate -> it.total != null && it.filled == 1 },
            "Sin lámina" to { it: ProtoPlate -> it.total == null },
        ),
    ),
    Facet(
        "Serie",
        listOf(
            "Abiertas y cerradas" to { _: ProtoPlate -> true },
            "Abiertas" to { it: ProtoPlate -> it.status == "open" },
            "Cerradas" to { it: ProtoPlate -> it.status == "closed" },
        ),
    ),
)

/**
 * Las monedas. Cada ficha lleva las colecciones en las que vive, como enlaces de vuelta: la
 * moneda es la puerta a su colección, y la lista admite varias aunque hoy nunca haya más de una.
 */
@Composable
private fun CoinsList(onOpenCollection: (String) -> Unit, header: @Composable () -> Unit) {
    val facets = remember { pieceFacets() }
    val sortings = remember { pieceSortings() }
    val (shelf, setShelf) = rememberShelf("monedas", facets.size)
    val chosen = shelf.chosen
    var query by remember { mutableStateOf("") }
    val needle = searchable(query)
    val piecesByQuery = PROTO_PIECES.filter { piece ->
        needle.isBlank() || searchable(
            piece.title + " " + piece.issuer + " " +
                piece.year + " " + piece.collections.joinToString(" "),
        ).contains(needle)
    }
    val shown = piecesByQuery
        .filter { piece ->
            facets.indices.all { f -> chosen[f] == 0 || facets[f].options[chosen[f]].second(piece) }
        }
        .sortedWith(sortings[shelf.sort].by)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PAGE,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { header() }
        item {
            FacetShelf(
                facets = facets,
                chosen = chosen,
                items = piecesByQuery,
                total = PROTO_PIECES.size,
                shown = shown.size,
                noun = "tipos",
                query = query,
                onQuery = { query = it },
                sortings = sortings,
                sort = shelf.sort,
                onSort = { setShelf(Shelf(chosen, it)) },
                onChoose = { f, option ->
                    setShelf(Shelf(chosen.toMutableList().also { it[f] = option }, shelf.sort))
                },
                onClear = { setShelf(Shelf(List(facets.size) { 0 }, shelf.sort)) },
            )
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

private fun pieceFacets(): List<Facet<ProtoPiece>> = listOf(
    Facet(
        "País",
        listOf<Pair<String, (ProtoPiece) -> Boolean>>("Todos" to { true }) +
            PROTO_PIECES.groupingBy { it.issuer }.eachCount()
                .entries.sortedByDescending { it.value }.take(7)
                .map { (issuer, _) -> issuer to { piece: ProtoPiece -> piece.issuer == issuer } },
    ),
    Facet(
        "Peso",
        listOf(
            "Cualquiera" to { _: ProtoPiece -> true },
            "Menos de 10 g" to { it: ProtoPiece -> (it.weightGrams ?: 0.0) < 10 },
            "10 – 25 g" to { it: ProtoPiece -> (it.weightGrams ?: 0.0) in 10.0..24.99 },
            "Una onza (25 – 34 g)" to { it: ProtoPiece -> (it.weightGrams ?: 0.0) in 25.0..34.0 },
            "Más de 34 g" to { it: ProtoPiece -> (it.weightGrams ?: 0.0) > 34 },
        ),
    ),
    Facet(
        "Año",
        listOf(
            "Cualquiera" to { _: ProtoPiece -> true },
            "Antes de 1900" to { it: ProtoPiece -> (it.year ?: 9999) < 1900 },
            "1900 – 1979" to { it: ProtoPiece -> (it.year ?: 0) in 1900..1979 },
            "1980 – 1999" to { it: ProtoPiece -> (it.year ?: 0) in 1980..1999 },
            "Desde 2000" to { it: ProtoPiece -> (it.year ?: 0) >= 2000 },
            "Sin año" to { it: ProtoPiece -> it.year == null },
        ),
    ),
    Facet(
        "Clase",
        listOf(
            "Todo" to { _: ProtoPiece -> true },
            "Monedas" to { it: ProtoPiece -> !it.medal },
            "Medallas y fichas" to { it: ProtoPiece -> it.medal },
        ),
    ),
    Facet(
        "Colección",
        listOf(
            "Da igual" to { _: ProtoPiece -> true },
            "En alguna" to { it: ProtoPiece -> it.classified },
            "Sin colección" to { it: ProtoPiece -> !it.classified },
        ),
    ),
)


/**
 * Los órdenes de las colecciones. El primero es el de hoy —alfabético por familia, `Proposals.kt`—
 * y los demás son los que #17 echó en falta: el índice actual entierra las cuatro completas entre
 * las quince de una sola casilla porque ordena sin mirar cobertura.
 */
private fun plateSortings(): List<Sorting<ProtoPlate>> = listOf(
    Sorting("Alfabético") { a, b -> a.family.lowercase().compareTo(b.family.lowercase()) },
    Sorting("Más completas") { a, b -> b.coverage().compareTo(a.coverage()) },
    Sorting("Menos completas") { a, b -> a.coverage().compareTo(b.coverage()) },
    Sorting("Más pesadas") { a, b -> (b.weightMillioz ?: 0).compareTo(a.weightMillioz ?: 0) },
    Sorting("Más piezas") { a, b -> b.pieces.compareTo(a.pieces) },
    Sorting("Alta más reciente") { a, b -> b.recency().compareTo(a.recency()) },
)

private fun ProtoPlate.coverage(): Double =
    if (total == null || total == 0) -1.0 else (filled ?: 0).toDouble() / total

/** Ver [RECENCY_NOTE]: simulado y estable, para que el orden no baile entre recomposiciones. */
private fun ProtoPlate.recency(): Int = family.hashCode()

private fun pieceSortings(): List<Sorting<ProtoPiece>> = listOf(
    Sorting("Alfabético") { a, b -> a.title.lowercase().compareTo(b.title.lowercase()) },
    Sorting("Más nuevas") { a, b -> (b.year ?: 0).compareTo(a.year ?: 0) },
    Sorting("Más viejas") { a, b -> (a.year ?: 9999).compareTo(b.year ?: 9999) },
    Sorting("Más pesadas") { a, b ->
        (b.weightGrams ?: 0.0).compareTo(a.weightGrams ?: 0.0)
    },
    Sorting("Alta más reciente") { a, b -> b.typeId.compareTo(a.typeId) },
)

/**
 * La estantería de filtros: una fila de opciones por faceta, plegable, con el recuento de lo que
 * queda. Plegada dice cuántas facetas están puestas, porque una estantería abierta se come la
 * primera pantalla entera — que es justo lo que hay que mirar en las capturas.
 */
@Composable
private fun <T> FacetShelf(
    facets: List<Facet<T>>,
    chosen: List<Int>,
    items: List<T>,
    total: Int,
    shown: Int,
    noun: String,
    query: String,
    onQuery: (String) -> Unit,
    sortings: List<Sorting<T>>,
    sort: Int,
    onSort: (Int) -> Unit,
    onChoose: (Int, Int) -> Unit,
    onClear: () -> Unit,
) {
    // Plegada al entrar: abierta se come la primera pantalla entera y entierra las tarjetas,
    // que es lo que se venía a ver.
    var open by remember { mutableStateOf(false) }
    val active = chosen.count { it != 0 }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            label = { Text("Buscar") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                // Plegada dice lo que hay puesto: cuántos filtros y por qué está ordenada. Un
                // pliegue que esconde el estado obliga a abrirlo para saber qué estás viendo.
                when {
                    open -> "▾ Filtros y orden"
                    active == 0 -> "▸ Filtros · orden ${sortings[sort].name.lowercase()}"
                    active == 1 -> "▸ 1 filtro · orden ${sortings[sort].name.lowercase()}"
                    else -> "▸ $active filtros · orden ${sortings[sort].name.lowercase()}"
                },
                style = MaterialTheme.typography.titleMedium,
                // Con peso: el rótulo se parte antes que pisar al recuento de la derecha.
                modifier = Modifier.weight(1f).clickable { open = !open },
            )
            Text(
                if (shown == total) "$shown $noun" else "$shown de $total $noun",
                style = MaterialTheme.typography.labelLarge,
                color = if (active == 0) Paper.muted else Paper.rust,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
        if (open) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Eyebrow("Orden")
                sortings.indices.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { option ->
                            Chip(sortings[option].name, option == sort) { onSort(option) }
                        }
                    }
                }
                if (sortings[sort].name == "Alta más reciente") {
                    Text(
                        RECENCY_NOTE,
                        style = MaterialTheme.typography.labelMedium,
                        color = Paper.rust,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            facets.forEachIndexed { index, facet ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Eyebrow(facet.name)
                    facet.options.indices.chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            pair.forEach { option ->
                                val label = facet.options[option].first
                                val left = facetedCount(items, facets, chosen, index, option)
                                Chip(
                                    if (option == 0) label else "$label · $left",
                                    option == chosen[index],
                                ) { onChoose(index, option) }
                            }
                        }
                    }
                }
            }
            if (active > 0) {
                CardAction(text = "Quitar los filtros", onClick = onClear)
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
