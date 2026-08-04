package com.jenarvaezg.coindex.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.ui.components.CardAction
import com.jenarvaezg.coindex.ui.components.Eyebrow
import com.jenarvaezg.coindex.ui.components.FieldCard
import com.jenarvaezg.coindex.ui.components.LinkText
import com.jenarvaezg.coindex.ui.components.PrimaryAction
import com.jenarvaezg.coindex.ui.theme.Paper
import com.jenarvaezg.coindex.ui.theme.PlateMetrics

/*
 * PROTOTIPO DESECHABLE — ticket #23 del mapa #16.
 *
 * La pregunta no es la del cuerpo del ticket («¿colapsan tres pantallas?»): la auditoría midió
 * que la lista de piezas de una tarjeta con lámina no enseña **ni una** pieza que la lámina no
 * enseñe (0 de 1033 casillas curadas). Lo que queda por ver es el destino:
 *
 *   D · Hoy — dos saltos. La tarjeta abre «lo que tengo» y ahí hay un botón «Ver lámina».
 *   E · Destino único — la tarjeta CON lista abre la lámina; la tarjeta SIN lista y la caja
 *       propia abren la MISMA lista de piezas, sin distinguirse (decidido al grillar).
 *
 * Sin datos reales, sin navegación de verdad, sin fotos: el estado vive en memoria y las
 * casillas se dibujan vacías con su rótulo. Los datos son los de #17 sobre el móvil del padre,
 * con nombres y tamaños de `data/`. Se borra al resolver el ticket.
 */

private enum class Dest23 { Index, Pieces, Plate }

/** Las piezas del fixture que dicen pertenecer a esta tarjeta. */
private fun piecesOf(family: String): List<ProtoPiece> =
    PROTO_PIECES.filter { piece -> piece.collections.any { it.startsWith(family.take(18)) } }

/** La caja propia del ejemplo de #12: la hizo el coleccionista y no puede tener un hueco. */
private const val OWN_BOX_NAME = "Las francesas"

private val OWN_BOX_PIECES: List<ProtoPiece> =
    PROTO_PIECES.filter { it.issuer == "Francia" }.ifEmpty { PROTO_PIECES.take(3) }

/**
 * El orden por defecto que decidió #22: tiene ratio ↓, ratio ↓, denominador ↓, nombre ↑.
 *
 * La caja propia cae en el tramo sin ratio, sin privilegio: es lo que este prototipo tiene que
 * dejar ver, porque hoy va en cabeza y está vacía en dos móviles.
 */
private val ORDERED_23: List<ProtoPlate> = PROTO_PLATES.sortedWith(
    compareByDescending<ProtoPlate> { it.total != null }
        .thenByDescending { plate ->
            val filled = plate.filled
            val total = plate.total
            if (filled != null && total != null && total > 0) filled.toDouble() / total else -1.0
        }
        .thenByDescending { it.total ?: -1 }
        .thenBy { it.family.lowercase() },
)

// ─── D · Hoy: dos saltos ──────────────────────────────────────────────────────────────────

@Composable
internal fun VariantD() {
    var where by remember { mutableStateOf(Dest23.Index) }
    var subject by remember { mutableStateOf<ProtoPlate?>(null) }
    var own by remember { mutableStateOf(false) }

    when (where) {
        Dest23.Index -> Index23(
            note = "Hoy toda tarjeta abre lo mismo: «lo que tengo». La lámina cuelga de un " +
                "botón dentro, y la caja propia tiene pantalla aparte.",
            onOpen = { plate -> subject = plate; own = false; where = Dest23.Pieces },
            onOpenOwn = { own = true; where = Dest23.Pieces },
        )

        Dest23.Pieces -> if (own) {
            OwnBox23(onBack = { where = Dest23.Index })
        } else {
            val plate = subject
            if (plate == null) {
                where = Dest23.Index
            } else {
                Pieces23(
                    eyebrow = "Propuesta de colección",
                    title = plate.family,
                    subtitle = plate.variant,
                    pieces = piecesOf(plate.family),
                    upkeep = false,
                    onBack = { where = Dest23.Index },
                    catalogCard = if (plate.total != null) {
                        { CatalogCard23(plate) { where = Dest23.Plate } }
                    } else {
                        null
                    },
                )
            }
        }

        Dest23.Plate -> subject?.let { plate ->
            Plate23(plate, onBack = { where = Dest23.Pieces })
        }
    }
}

// ─── E · Destino único ────────────────────────────────────────────────────────────────────

@Composable
internal fun VariantE() {
    var where by remember { mutableStateOf(Dest23.Index) }
    var subject by remember { mutableStateOf<ProtoPlate?>(null) }
    var own by remember { mutableStateOf(false) }

    when (where) {
        Dest23.Index -> Index23(
            note = "Una tarjeta, un destino. Con lista de emisiones abre su lámina; sin lista " +
                "abre sus piezas — y la caja propia abre exactamente esa misma pantalla.",
            onOpen = { plate ->
                subject = plate
                own = false
                where = if (plate.total != null) Dest23.Plate else Dest23.Pieces
            },
            onOpenOwn = { own = true; where = Dest23.Pieces },
        )

        Dest23.Pieces -> if (own) {
            OwnBox23(onBack = { where = Dest23.Index })
        } else {
            val plate = subject
            if (plate == null) {
                where = Dest23.Index
            } else {
                Pieces23(
                    // Ni «propuesta» (retirada por #21) ni «agrupación»: lo que la pantalla es.
                    eyebrow = plate.issuer,
                    title = plate.family,
                    subtitle = plate.variant,
                    pieces = piecesOf(plate.family),
                    upkeep = false,
                    onBack = { where = Dest23.Index },
                    catalogCard = null,
                )
            }
        }

        Dest23.Plate -> subject?.let { plate ->
            Plate23(plate, onBack = { where = Dest23.Index })
        }
    }
}

// ─── El índice, igual en las dos ──────────────────────────────────────────────────────────

@Composable
private fun Index23(
    note: String,
    onOpen: (ProtoPlate) -> Unit,
    onOpenOwn: () -> Unit,
) {
    val conLista = ORDERED_23.count { it.total != null }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Eyebrow("Cuaderno de colección")
                Text("Colecciones", style = MaterialTheme.typography.displayLarge)
                Text(
                    "${ORDERED_23.size + 1} tarjetas · $conLista con lista de emisiones",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Paper.muted,
                )
                Text(note, style = MaterialTheme.typography.bodyMedium, color = Paper.rust)
            }
        }
        // El tramo con ratio primero, como decidió #22.
        items(ORDERED_23.filter { it.total != null }) { plate ->
            IndexCard23(plate, own = false, onOpen = { onOpen(plate) })
        }
        item { HorizontalDivider(color = Paper.line) }
        item {
            Text(
                "Sin lista de emisiones",
                style = MaterialTheme.typography.titleMedium,
                color = Paper.muted,
            )
        }
        items(ORDERED_23.filter { it.total == null }) { plate ->
            IndexCard23(plate, own = false, onOpen = { onOpen(plate) })
        }
        item { OwnCard23(onOpen = onOpenOwn) }
    }
}

@Composable
private fun IndexCard23(plate: ProtoPlate, own: Boolean, onOpen: () -> Unit) {
    FieldCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Eyebrow(plate.issuer)
        LinkText(
            text = plate.family,
            style = MaterialTheme.typography.titleLarge,
            onClick = onOpen,
        )
        Text(
            // Las dos frases exactas que fijó #19, según haya lista o no.
            if (plate.filled != null && plate.total != null) {
                val faltan = plate.total - plate.filled
                if (faltan > 0) {
                    "${plate.filled} de ${plate.total} · te faltan $faltan"
                } else {
                    "${plate.filled} de ${plate.total} · completa"
                }
            } else {
                "${plate.pieces} ${if (plate.pieces == 1) "moneda" else "monedas"} · " +
                    "${plate.types} ${if (plate.types == 1) "tipo" else "tipos"}"
            },
            style = MaterialTheme.typography.labelLarge,
            color = Paper.muted,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun OwnCard23(onOpen: () -> Unit) {
    FieldCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        // Sin palabra de procedencia y sin privilegio de orden: #19 y #22.
        Eyebrow("Francia")
        LinkText(
            text = OWN_BOX_NAME,
            style = MaterialTheme.typography.titleLarge,
            onClick = onOpen,
        )
        Text(
            "${OWN_BOX_PIECES.size} ${if (OWN_BOX_PIECES.size == 1) "moneda" else "monedas"} · " +
                "${OWN_BOX_PIECES.map { it.typeId }.distinct().size} tipos",
            style = MaterialTheme.typography.labelLarge,
            color = Paper.muted,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

// ─── La lista de piezas, la misma para las dos clases sin lista ────────────────────────────

@Composable
private fun Pieces23(
    eyebrow: String,
    title: String,
    subtitle: String?,
    pieces: List<ProtoPiece>,
    upkeep: Boolean,
    onBack: () -> Unit,
    catalogCard: (@Composable () -> Unit)?,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LinkText(
                    text = "◀ Colecciones",
                    style = MaterialTheme.typography.labelLarge,
                    onClick = onBack,
                )
                Eyebrow(eyebrow)
                Text(title, style = MaterialTheme.typography.headlineMedium)
                subtitle?.let {
                    Text(it, style = MaterialTheme.typography.bodyLarge, color = Paper.muted)
                }
                Text(
                    "${pieces.size} ${if (pieces.size == 1) "moneda" else "monedas"} · " +
                        "${pieces.map { it.typeId }.distinct().size} tipos",
                    style = MaterialTheme.typography.labelLarge,
                    color = Paper.muted,
                )
                if (upkeep) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 10.dp),
                    ) {
                        CardAction(text = "Renombrar", onClick = {})
                        CardAction(text = "Deshacer la agrupación", onClick = {})
                    }
                }
            }
        }
        catalogCard?.let { card -> item { card() } }
        if (pieces.isEmpty()) {
            item {
                FieldCard(dashed = true, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "No tienes ninguna pieza de esta tarjeta.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Paper.muted,
                    )
                }
            }
        }
        items(pieces) { piece ->
            FieldCard(modifier = Modifier.fillMaxWidth()) {
                Text(piece.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${piece.year?.toString() ?: "Sin año"} · N# ${piece.typeId}",
                    style = MaterialTheme.typography.labelLarge,
                    color = Paper.muted,
                    modifier = Modifier.padding(top = 2.dp),
                )
                if (upkeep) {
                    CardAction(
                        text = "Quitar de la agrupación",
                        onClick = {},
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun OwnBox23(onBack: () -> Unit) {
    Pieces23(
        eyebrow = "Francia",
        title = OWN_BOX_NAME,
        subtitle = null,
        pieces = OWN_BOX_PIECES,
        upkeep = true,
        onBack = onBack,
        catalogCard = null,
    )
}

/** La tarjeta de hoy que hace el salto: catálogo curado con su «Ver lámina». */
@Composable
private fun CatalogCard23(plate: ProtoPlate, onOpenPlate: () -> Unit) {
    FieldCard(modifier = Modifier.fillMaxWidth()) {
        Eyebrow("Catálogo curado")
        Text(
            plate.family,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 4.dp),
        )
        CardAction(
            text = "Ver lámina",
            onClick = onOpenPlate,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

// ─── La lámina, tosca: casillas con rótulo, sin fotos ──────────────────────────────────────

private data class Cell23(val label: String, val owned: Boolean)

private fun cellsOf(plate: ProtoPlate): List<Cell23> {
    val total = plate.total ?: return emptyList()
    val filled = plate.filled ?: 0
    val first = plate.firstYear
    return (0 until total).map { index ->
        Cell23(
            label = first?.let { (it + index).toString() } ?: "Emisión ${index + 1}",
            owned = index < filled,
        )
    }
}

@Composable
private fun Plate23(plate: ProtoPlate, onBack: () -> Unit) {
    val cells = remember(plate.family) { cellsOf(plate) }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(PlateMetrics.minPlateCell),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 140.dp),
        horizontalArrangement = Arrangement.spacedBy(PlateMetrics.gutter),
        verticalArrangement = Arrangement.spacedBy(PlateMetrics.gutter),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LinkText(
                    text = "◀ Colecciones",
                    style = MaterialTheme.typography.labelLarge,
                    onClick = onBack,
                )
                Eyebrow(plate.issuer)
                Text(plate.family, style = MaterialTheme.typography.headlineMedium)
                Text(
                    "${plate.filled} / ${plate.total} emisiones · ${plate.variant}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Paper.muted,
                )
                Text(
                    "Aquí están tus piezas y tus huecos a la vez: la casilla dice «Tengo» o " +
                        "«Me falta», con su ×N cuando hay más de una.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Paper.rust,
                )
                PrimaryAction(text = "Exportar lámina como imagen", onClick = {}, share = true)
            }
        }
        items(cells) { cell ->
            FieldCard(emphasized = cell.owned, dashed = !cell.owned) {
                Text(
                    if (cell.owned) "TENGO" else "ME FALTA",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (cell.owned) Paper.rust else Paper.muted,
                )
                Text(
                    cell.label,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}
