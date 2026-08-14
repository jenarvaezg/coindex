#!/usr/bin/env python3
"""Maqueta de «Explorar» con dos cosas dentro (#498), a dp real y con lo de hoy de listón.

PROTOTIPO — se tira cuando el ticket se decida. Lo que sobrevive es el README.

La pregunta no es «¿cómo se pinta un estante?» —el #279 ya eligió la tarjeta y el fantasma—
sino la que ese ticket no pudo hacerse porque la puerta estaba vacía: **«Explorar» ya tiene «Lo
que busco» dentro** (`ExploreScreen.kt`: «es su primera sección, y hoy es toda ella»), así que
qué es esa pantalla cuando además entran veinte láminas ajenas, y qué enseña el estante el día
uno, cuando el coleccionista no ha tasado ninguna.

Cinco variantes por tres estados de tasación. Un solo HTML autocontenido: las dos fuentes del
APK y una foto por tipo en base64, con una clase CSS por tipo para no embeberlas por casilla.

Las medidas salen del código y no se suponen: `IndexScreen` (PAGE_MARGIN 12, INDEX_GUTTER 8,
tarjeta de 104 dp y nombre de dos líneas fijas), `ExploreScreen` (margen 20, `PlateSpacing.rowGap`
32), `CoindexApp.Masthead` y `AlbumPaper` para el hueco. **No lleva barra inferior**: Explorar no
es raíz (`Routes.isRoot`), se entra por la puerta del índice y se sale con «Volver».

**Sale al anexo privado y no al repo** (`dinero-fuera-del-repo-publico`): lleva los importes de
la colección del padre.

    python3 docs/ux/prototipo-escaparate-498/extract.py
    python3 docs/ux/prototipo-escaparate-498/build.py
    open /private/tmp/coindex-privado/escaparate-498/maqueta.html
"""
import base64
import json
import os
import subprocess

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.abspath(f"{HERE}/../../..")
OUT = "/private/tmp/coindex-privado/escaparate-498"

# ── el papel (Theme.kt:27-35) ───────────────────────────────────────────────
INK, MUTED, PAPER = "#2D3029", "#686A5D", "#EEE8D7"
DEEP, LINE, MOSS, RUST = "#DDD3BB", "#7D806C", "#495C49", "#8B553C"
CARD, HAIR = "rgba(255,252,242,0.58)", "#878577"

# ── la pantalla ─────────────────────────────────────────────────────────────
SCREEN, FOLD = 411, 914          # el Pixel 7 de las capturas del #296, 1 px CSS = 1 dp

# la rejilla del índice (IndexScreen.kt:117-131)
IDX_MARGIN, IDX_GUTTER, IDX_ROWGAP, IDX_PAD_V = 12, 8, 6, 8
IDX_COLS = 3
IDX_CELL = (SCREEN - 2 * IDX_MARGIN - (IDX_COLS - 1) * IDX_GUTTER) / IDX_COLS

# la rejilla de casillas de «Lo que busco» (ExploreScreen.kt: margen 20, Adaptive(104))
WISH_MARGIN, WISH_GUTTER, WISH_ROWGAP, WISH_PAD_V = 20, 16, 32, 24
WISH_COLS = 3
WISH_CELL = (SCREEN - 2 * WISH_MARGIN - (WISH_COLS - 1) * WISH_GUTTER) / WISH_COLS

HOLE, RING = 104, 5              # AlbumHole y HOLE_CARD_PADDING_DP
TAG_W, TAG_H, TAG_TARGET = 48.3, 28, 48   # YearTagMetrics

HOY = "14 ago"                   # la maqueta se hizo el 14 de agosto de 2026
ANTES = "12 ago"                 # la edad vieja del #494

DATA = json.load(open(f"{OUT}/data.json"))
SHELF, MINE, WISHES, SPOT = DATA["shelf"], DATA["mine"], DATA["wishes"], DATA["spot"]
WISHED_PLATES = {w["plate_id"] for w in WISHES}
# Las cuatro que el estado intermedio enseña tasadas, más las dos que llevan marca: una lámina
# con una casilla marcada ya tiene ese precio pedido por el pase (ADR 0029 §4), así que su total
# nace con dos edades — el caso abierto del #494.
TASADAS_ALGUNAS = [c["id"] for c in SHELF[:4]] + [
    c["id"] for c in SHELF if c["id"] in WISHED_PLATES
]


def b64(path, mime):
    return f"data:{mime};base64," + base64.b64encode(open(path, "rb").read()).decode()


def font(name):
    return b64(f"{REPO}/app/src/main/res/font/{name}.ttf", "font/ttf")


def thumb(tid):
    """La foto reducida: 185 tipos inlineados a tamaño original no caben en un artifact."""
    src, dst = f"{OUT}/fotos/{tid}.img", f"{OUT}/fotos-min/{tid}.jpg"
    if not os.path.exists(dst) and os.path.exists(src):
        subprocess.run(["sips", "-Z", "132", "-s", "format", "jpeg", "-s", "formatOptions", "45",
                        src, "--out", dst], capture_output=True)
    return b64(dst, "image/jpeg") if os.path.exists(dst) else None


def eur(amount):
    """`eurosLabel`: sin decimales y con el punto de millar español."""
    return f"{amount:,.0f} €".replace(",", ".")


def plural(count, one, many):
    return f"{count} {one if count == 1 else many}"


# ── el cromo ────────────────────────────────────────────────────────────────
STATUS = '<div class="status"><span>16:24</span></div>'


def masthead(subtitle):
    """`CoindexApp.Masthead`: pantalla interior, con «Volver» y sin Ajustes."""
    return (f'<div class="masthead"><div class="mh-row"><b>COINDEX</b>'
            f'<span class="action"><u class="chev"></u> Volver</span></div>'
            f'<div class="mh-sub">{subtitle}</div></div>')


def hole(tid, missing=True, size=HOLE, stamp=None, wished=False):
    """El hueco troquelado de `AlbumPaper`: cartón, pared del corte, filete y la foto dentro."""
    ring = max(2, round(RING * size / HOLE))
    inner = f'<i class="p{tid}" style="inset:{ring}px"></i>' if tid in PHOTOS else ""
    chip = f'<b class="chip">{stamp}</b>' if stamp else ""
    mark = '<em class="mark">lo busco</em>' if wished else ""
    return (f'<div class="hole{" miss" if missing else ""}" '
            f'style="width:{size}px;height:{size}px">{inner}{chip}{mark}</div>')


def search(placeholder="Buscar"):
    return f'<div class="search">{placeholder}</div>'


def shelfline(text, extra=""):
    """La fila plegada del estante (`indexShelfSummary`): «▸ …», y la cuenta a la derecha."""
    return (f'<div class="shelfline"><span>&#9656; {text}</span>'
            f'<em>{extra}</em></div>')


# ── «Lo que busco», que es lo que hay hoy ───────────────────────────────────
def wish_cells(remove=True):
    cells = ""
    for wish in WISHES:
        year = f'<div class="tag">{wish["year"]}</div>' if wish["year"] else ""
        name = wish["label"] if wish["label"] != str(wish["year"]) else None
        cells += (
            f'<div class="wcell">'
            f'{hole(wish["tid"], stamp=eur(wish["floor"]) if wish["floor"] else None)}'
            f'<div class="tagbox">{year}</div>'
            f'{f"<div class=\x27wname\x27>{name}</div>" if name else ""}'
            f'<div class="wplate">{wish["plate"]}</div>'
            f'{"<div class=\x27cardaction\x27>Quitar</div>" if remove else ""}</div>'
        )
    return f'<div class="wgrid">{cells}</div>'


def wish_head(with_action=True):
    census = f"{plural(len(WISHES), 'casilla', 'casillas')} en " \
             f"{plural(len({w['plate_id'] for w in WISHES}), 'lámina', 'láminas')}"
    action = '<div class="primary">Exportar la lista</div>' if with_action else ""
    return (f'<div class="whead"><div class="sentence">Las casillas que marcaste, para '
            f'llevártelas a la feria.</div><div class="census">{census}</div>{action}</div>')


# ── el estante de las veinte ────────────────────────────────────────────────
def cname(title):
    """`CollectionName`: dos líneas fijas y `TextAutoSize.StepBased(13..17)`, a ojo por longitud.

    En la app el nombre encoge hasta 13 sp antes de truncar; sin esto la maqueta corta con
    puntos suspensivos nombres que en el teléfono entran enteros.
    """
    size = 17 if len(title) <= 28 else 15 if len(title) <= 34 else 13
    line = round(size * 1.24)
    # 6 + 2 dp de padding del `CollectionName`, que van dentro de la caja: sin sumarlos aquí la
    # segunda línea se corta por la mitad y la maqueta miente sobre lo que cabe.
    return (f'<div class="cname" style="font-size:{size}px;line-height:{line}px;'
            f'height:{line * 2 + 8}px">{title}</div>')


def foot(cat, tasadas):
    """El pie de una tarjeta del escaparate: casillas mientras no esté tasada, importe después."""
    if cat["id"] not in tasadas:
        return f'<div class="count">{plural(cat["issued"], "casilla", "casillas")}</div>'
    # Una lámina con casilla marcada tiene ese precio del pase de este mes y el resto de cuando
    # la tasaste a mano: un total entero con dos edades (#494).
    when = f"12 y {HOY}" if cat["id"] in WISHED_PLATES else ANTES
    return (f'<div class="cost">{eur(cat["entry"])}<u>{when}</u></div>')


def card(cat, tasadas, mine=False):
    """La tarjeta de 104 dp de `IndexScreen`, en fantasma cuando la lámina no es tuya."""
    if mine:
        marks = sum(1 for w in WISHES if w["plate_id"] == cat["id"])
        pie = f'<div class="ratio">{cat["owned"]}/{cat["issued"]}</div>'
        badge = f'<div class="marked">{marks} lo busco</div>' if marks else ""
        return (f'<div class="card">{hole(cat["tid"], missing=False)}'
                f'{cname(cat["title"])}{pie}{badge}</div>')
    wished = cat["id"] in WISHED_PLATES
    return (f'<div class="card">{hole(cat["casillas"][0]["tid"], wished=wished)}'
            f'{cname(cat["title"])}{foot(cat, tasadas)}</div>')


def shelf_grid(cats, tasadas, mine_ids=()):
    cards = "".join(card(c, tasadas, mine=c["id"] in mine_ids) for c in cats)
    return f'<div class="grid3">{cards}</div>'


FREE = ("Hojear no cuesta nada. Cada lámina se tasa cuando la abres, y dice antes lo que "
        "va a preguntar.")


def by_slots():
    return sorted(SHELF, key=lambda c: c["issued"])


def by_cost(tasadas):
    """Por coste de entrar, y las que no lo tienen caen al final por casillas."""
    return sorted(SHELF, key=lambda c: (c["id"] not in tasadas,
                                        -c["entry"] if c["id"] in tasadas else c["issued"]))


# ── las cinco variantes ─────────────────────────────────────────────────────
def v_hoy(tasadas):
    """0 · Hoy: Explorar es «Lo que busco» y nada más."""
    return (f'{STATUS}{masthead("Lo que busco")}'
            f'<div class="scroll wish">{wish_head()}{wish_cells()}</div>')


def v_a(tasadas):
    """A · Un solo scroll, dos secciones, y las marcas primero porque son tuyas."""
    return (f'{STATUS}{masthead("Explorar")}<div class="scroll wish">'
            f'<div class="eyebrow">Lo que busco · {len(WISHES)}</div>'
            f'{wish_head()}{wish_cells()}'
            f'<div class="section"><div class="eyebrow">Láminas que no coleccionas · '
            f'{len(SHELF)}</div><div class="note">{FREE}</div></div>'
            f'<div class="bleed">{search("Buscar entre las veinte")}'
            f'{shelfline("Orden por casillas")}'
            f'{shelf_grid(by_slots(), tasadas)}</div></div>')


def v_b(tasadas):
    """B · El estante se lleva la pantalla y tu lista vuelve a ser una puerta."""
    sort = "Orden por coste de entrar" if tasadas else "Orden por casillas"
    aside = "" if tasadas else '<em class="dim">por coste, cuando tases</em>'
    return (f'{STATUS}{masthead(f"Explorar · {len(SHELF)} láminas")}'
            f'<div class="scroll idx">'
            f'<div class="door"><span>Lo que busco · {len(WISHES)}</span><u class="fwd"></u></div>'
            f'<div class="note">{FREE}</div>'
            f'{search("Buscar entre las veinte")}{shelfline(sort, aside)}'
            f'{shelf_grid(by_cost(tasadas) if tasadas else by_slots(), tasadas)}</div>')


def v_c(tasadas):
    """C · Dos lengüetas de cartón: dos hojas que comparten puerta y no una lista partida."""
    tabs = (f'<div class="tabs"><span>Lo que busco · {len(WISHES)}</span>'
            f'<span class="on">Láminas · {len(SHELF)}</span></div>')
    return (f'{STATUS}{masthead("Explorar")}{tabs}'
            f'<div class="scroll idx">'
            f'<div class="note">{FREE}</div>'
            f'{search("Buscar entre las veinte")}{shelfline("Orden como las curé")}'
            f'{shelf_grid(sorted(SHELF, key=lambda c: c["updated"] or "", reverse=True), tasadas)}'
            f'</div>')


def d_shelf(tasadas):
    """El estante de la D: tus láminas con marca primero, luego las veinte por casillas."""
    marked_mine = [c for c in MINE if c["id"] in WISHED_PLATES]
    marked_shelf = [c for c in SHELF if c["id"] in WISHED_PLATES]
    rest = [c for c in SHELF if c["id"] not in WISHED_PLATES]
    cats = marked_mine + marked_shelf + sorted(rest, key=lambda c: c["issued"])
    grid = shelf_grid(cats, tasadas, mine_ids={c["id"] for c in marked_mine})
    return len(cats), grid


def v_d(tasadas):
    """D · Un solo estante: la marca es un estado de la casilla, no una sección.

    Tal cual salió del primer dibujo, y con su agujero a la vista: no hay lista ni exportación,
    así que la hoja que el #497 hizo para llevarse a la feria no está en ninguna parte.
    """
    total, grid = d_shelf(tasadas)
    return (f'{STATUS}{masthead("Explorar · lo que te falta")}'
            f'<div class="scroll idx">'
            f'<div class="note">Las láminas donde te falta algo: las tuyas con casillas marcadas '
            f'y las veinte que no coleccionas. {FREE}</div>'
            f'{search("Buscar")}'
            f'{shelfline("Orden primero lo que busco", f"{total} láminas")}{grid}</div>')


def v_d1(tasadas):
    """D′ · La D con la lista conservada detrás de su puerta: la hoja de la feria sigue existiendo."""
    total, grid = d_shelf(tasadas)
    return (f'{STATUS}{masthead("Explorar · lo que te falta")}'
            f'<div class="scroll idx">'
            f'<div class="door"><span>Lo que busco · {len(WISHES)} casillas</span>'
            f'<u class="fwd"></u></div>'
            f'<div class="note">Las láminas donde te falta algo: las tuyas con casillas marcadas '
            f'y las veinte que no coleccionas. {FREE}</div>'
            f'{search("Buscar")}'
            f'{shelfline("Orden primero lo que busco", f"{total} láminas")}{grid}</div>')


def v_d2(tasadas):
    """D″ · La D pura: no hay pantalla de lista, y lo que se lleva a la feria sale de aquí."""
    total, grid = d_shelf(tasadas)
    return (f'{STATUS}{masthead("Explorar · lo que te falta")}'
            f'<div class="scroll idx">'
            f'<div class="note">Las láminas donde te falta algo: las tuyas con casillas marcadas '
            f'y las veinte que no coleccionas. {FREE}</div>'
            f'<div class="primary">Exportar lo que busco · {len(WISHES)} casillas</div>'
            f'<div class="undernote">Las casillas marcadas se ven y se quitan en su lámina.</div>'
            f'{search("Buscar")}'
            f'{shelfline("Orden primero lo que busco", f"{total} láminas")}{grid}</div>')


VARIANTS = [
    ("0", "Hoy · v1.3.0", v_hoy,
     "<b>El listón.</b> «Explorar» es «Lo que busco» y nada más: siete casillas marcadas, su "
     "precio dentro del hueco y «Quitar» debajo. El estante de veinte no existe todavía, y la "
     "pantalla se llama por lo único que hay detrás de su puerta."),
    ("A", "Dos secciones, un scroll", v_a,
     "<b>Tesis: es una hoja, y lo tuyo va primero.</b> Se lee literalmente el "
     "<code>ExploreScreen</code> de hoy —«su primera sección»— y debajo, tras una regla, el "
     "estante con su buscador y su orden. El precio de esto es que las veinte empiezan bajo el "
     "pliegue: hay que pasar por tus siete marcas para llegar al escaparate."),
    ("B", "El estante manda, tu lista es una puerta", v_b,
     "<b>Tesis: lo que se hojea son las láminas; la lista es un anexo del anexo.</b> La pantalla "
     "arranca en el estante y «Lo que busco · 7 →» vuelve a ser una fila de papel profundo, la "
     "misma que la trajo desde el índice. El nombre de la pantalla pasa a ser «Explorar», que es "
     "lo que <code>WishLabels.DESTINATION</code> dejó escrito para el día que existiera el estante."),
    ("C", "Dos lengüetas", v_c,
     "<b>Tesis: son dos cosas distintas que comparten puerta, no una lista con dos mitades.</b> "
     "Dos hojas con lengüeta de cartón: cada una entera, con su orden y su acción primaria, y "
     "ninguna hace scroll sobre la otra. Cuesta una fila de cromo permanente y una pregunta nueva "
     "—en cuál abre—, y es lo único que evita elegir quién va antes."),
    ("D", "Un solo estante, y lo que buscas es un orden", v_d,
     "<b>Tesis: la marca es un estado de la casilla, no una sección.</b> No hay dos cosas: hay un "
     "estante de «lo que te falta» donde entran tus tres láminas con marcas y las veinte ajenas, "
     "ordenado por «primero lo que busco». Mezcla los dos regímenes en una rejilla —una tarjeta "
     "con fracción junto a otra con coste— y a cambio la pantalla tiene una sola tesis. "
     "<b>Su agujero:</b> no hay lista ni exportación, así que la hoja que el #497 hizo para "
     "llevarse a la feria no está en ninguna parte."),
    ("D1", "D′ · la D con la lista detrás de su puerta", v_d1,
     "<b>La D, y «Lo que busco» sigue siendo una pantalla.</b> La puerta de papel profundo la "
     "conserva entera —sus siete casillas, «Quitar» y «Exportar la lista»— y el estante se queda "
     "con el orden. Cuesta que la marca esté dicha <b>dos veces</b>: en la puerta como cuenta y en "
     "la tarjeta como «2 lo busco»; y deja en pie la pregunta de qué es esa pantalla, que es lo que "
     "la D quería quitarse."),
    ("D2", "D″ · la D pura: la lista es una exportación", v_d2,
     "<b>La D hasta el final: «Lo que busco» deja de ser pantalla.</b> Lo único que hacía falta de "
     "ella era la hoja de la feria, así que se convierte en la acción primaria del estante, y las "
     "casillas marcadas se ven y se quitan donde se marcaron: en su lámina. Cuesta <b>borrar una "
     "pantalla publicada ayer</b> con su nombre, su puerta y su rótulo, y que «Quitar» se vaya a "
     "cincuenta y una casillas en vez de estar en una lista de siete filas."),
]

STATES = [
    ("nada", "Sin tasar · día uno", "Ninguna de las veinte tasada, que es el estado en que nace la "
     "pantalla: <b>el orden por coste de entrar no tiene nada que ordenar</b>. Cada variante "
     "contesta distinto a qué se enseña en su lugar."),
    ("algunas", "Seis tasadas", "Cuatro tasadas a mano y las dos que llevan casilla marcada, cuyo "
     "precio ya lo pide el pase: su total nace <b>con dos edades</b> — el caso abierto del #494."),
    ("todas", "Las veinte tasadas", "El estado al que se llega tasando lámina a lámina, nunca de "
     "golpe: 227 consultas repartidas en veinte gestos que dicen su precio antes de pulsarse."),
]

TASADAS = {"nada": set(), "algunas": set(TASADAS_ALGUNAS), "todas": {c["id"] for c in SHELF}}

# ── las fotos, una clase por tipo y una sola vez ────────────────────────────
TIDS = sorted(
    {c["casillas"][0]["tid"] for c in SHELF}
    | {w["tid"] for w in WISHES}
    | {c["id"] and c["tid"] for c in MINE if c["id"] in WISHED_PLATES}
)
os.makedirs(f"{OUT}/fotos-min", exist_ok=True)
PHOTOS = {tid: thumb(tid) for tid in TIDS if tid}
PHOTOS = {tid: url for tid, url in PHOTOS.items() if url}
PHOTO_CSS = "".join(f'.p{tid}{{background-image:url("{url}")}}' for tid, url in PHOTOS.items())

CSS = f"""
@font-face {{ font-family: Bitter; src: url("{font('bitter_variable')}"); }}
@font-face {{ font-family: Barlow; font-weight: 400;
              src: url("{font('barlow_condensed_regular')}"); }}
@font-face {{ font-family: Barlow; font-weight: 600;
              src: url("{font('barlow_condensed_semibold')}"); }}
* {{ box-sizing: border-box; margin: 0; padding: 0; }}
body {{ background: #3a3a36; color: #eae4d4;
        font: 13px/1.5 -apple-system, system-ui, sans-serif; padding: 0 0 48px; }}

/* ── la barra, arriba y sticky: abajo tapa el teléfono ────────────────────── */
header {{ position: sticky; top: 0; z-index: 9; background: #22221f;
          border-bottom: 1px solid #55554e; padding: 8px 12px; display: flex; gap: 6px;
          flex-wrap: wrap; align-items: center; }}
header button {{ font: 600 12px/1 -apple-system, system-ui, sans-serif; padding: 7px 10px;
   cursor: pointer; background: #3c3c37; color: #e8e2d2; border: 1px solid #55554e;
   border-radius: 3px; }}
header button.on {{ background: {PAPER}; color: {INK}; border-color: {PAPER}; }}
header .sep {{ width: 1px; height: 22px; background: #4a4a44; margin: 0 4px; }}
header .m {{ margin-left: auto; font: 11px/1 ui-monospace, monospace; color: #9d9a8c; }}
.cap {{ max-width: 640px; margin: 14px auto 10px; padding: 0 16px;
        font: 13px/1.5 -apple-system, system-ui, sans-serif; color: #c3bdad; }}
.cap b {{ color: #efe9d8; }}
.cap code {{ font: 11px/1 ui-monospace, monospace; background: #2a2a26; padding: 2px 4px;
             border-radius: 2px; }}
.cap .st {{ display: block; padding-top: 6px; color: #a8a294; }}
.stage {{ display: flex; justify-content: center; padding: 8px; }}

/* ── el teléfono: 411 x 914 dp. Sin barra inferior: Explorar no es raíz ───── */
.phone {{ width: {SCREEN}px; height: {FOLD}px; background: {PAPER}; color: {INK};
   overflow: hidden; position: relative; font-family: Bitter; display: none;
   flex-direction: column; box-shadow: 0 8px 30px rgba(0,0,0,.5); }}
.phone.on {{ display: flex; }}
.status {{ height: 24px; flex: 0 0 24px; display: flex; align-items: center;
   justify-content: flex-end; padding-right: 14px; font: 600 10px/1 Barlow;
   font-feature-settings: 'smcp','tnum'; color: {MUTED}; }}
.masthead {{ flex: 0 0 auto; border-bottom: 2px solid {INK}; }}
.mh-row {{ display: flex; justify-content: space-between; align-items: center;
           padding: 14px 20px 6px; }}
.mh-row b {{ font: 400 21px/25px Bitter; }}
.action {{ font: 600 12px/1 Barlow; font-feature-settings: 'smcp','tnum'; color: {MOSS};
           display: flex; align-items: center; gap: 5px; }}
.chev, .fwd {{ width: 7px; height: 7px; border-left: 1.5px solid {MOSS};
               border-bottom: 1.5px solid {MOSS}; transform: rotate(45deg); display: block; }}
.fwd {{ transform: rotate(-135deg); border-color: {INK}; }}
.mh-sub {{ padding: 0 20px 8px; font: 600 11px/1 Barlow; font-feature-settings: 'smcp','tnum';
           color: {MUTED}; }}
.scroll {{ flex: 1; overflow-y: auto; overflow-x: hidden; }}
.scroll.wish {{ padding: {WISH_PAD_V}px {WISH_MARGIN}px; }}
.scroll.idx {{ padding: {IDX_PAD_V}px {IDX_MARGIN}px; }}

/* ── la lengüeta de la C, y la puerta de la B (AnnexDoor) ─────────────────── */
.tabs {{ display: flex; gap: 6px; padding: 8px 12px 0; background: {PAPER};
         border-bottom: 2px solid {INK}; }}
.tabs span {{ padding: 9px 12px; background: {DEEP}; color: {MUTED};
   font: 600 11px/1 Barlow; font-feature-settings: 'smcp','tnum';
   border: 1px solid {HAIR}; border-bottom: none; }}
.tabs span.on {{ background: {INK}; color: {PAPER}; border-color: {INK}; }}
.door {{ display: flex; justify-content: space-between; align-items: center;
   background: {DEEP}; padding: 14px; margin-bottom: 10px;
   font: 600 12px/1 Barlow; font-feature-settings: 'smcp','tnum'; color: {INK}; }}

/* ── «Lo que busco» (ExploreScreen.kt) ───────────────────────────────────── */
.whead {{ display: flex; flex-direction: column; gap: 10px; padding-bottom: 10px; }}
.sentence {{ font: 400 16px/22px Bitter; color: {MUTED}; }}
.census {{ font: 600 12px/1 Barlow; font-feature-settings: 'smcp','tnum'; color: {RUST}; }}
.primary {{ background: {INK}; color: {PAPER}; text-align: center; padding: 14px;
            font: 600 12px/1 Barlow; font-feature-settings: 'smcp','tnum'; }}
.wgrid {{ display: grid; grid-template-columns: repeat({WISH_COLS}, {WISH_CELL}px);
          column-gap: {WISH_GUTTER}px; row-gap: {WISH_ROWGAP}px; }}
.wcell {{ display: flex; flex-direction: column; align-items: center; }}
.wname {{ font: 400 17px/21px Bitter; text-align: center; padding-top: 6px; }}
.wplate {{ font: 600 11px/15px Barlow; font-feature-settings: 'smcp','tnum'; color: {MUTED};
           text-align: center; }}
.cardaction {{ font: 600 11px/1 Barlow; font-feature-settings: 'smcp','tnum'; color: {MOSS};
               padding-top: 6px; }}
.tagbox {{ height: {TAG_TARGET}px; display: flex; align-items: center; flex: none; }}
.tag {{ min-width: {TAG_W}px; height: {TAG_H}px; display: flex; align-items: center;
        justify-content: center; padding: 0 6px; background: rgba(221,211,187,.90);
        border-top: 2px solid rgba(45,48,41,.34);
        border-bottom: 1px solid rgba(255,255,255,.55);
        font: 600 12px Barlow; font-feature-settings: 'smcp','tnum'; color: {INK}; }}

/* ── el estante y sus tarjetas (IndexScreen.kt) ───────────────────────────── */
.eyebrow {{ font: 600 11px/1 Barlow; font-feature-settings: 'smcp','tnum'; color: {RUST};
            padding-bottom: 6px; }}
.section {{ border-top: 1px solid {HAIR}; margin-top: 20px; padding-top: 14px; }}
.note {{ font: 400 14px/20px Bitter; color: {MUTED}; padding-bottom: 10px; }}
.undernote {{ font: 400 12px/17px Bitter; color: {MUTED}; padding: 6px 0 10px; }}
.bleed {{ margin: 0 -{WISH_MARGIN - IDX_MARGIN}px; }}
.search {{ height: 44px; display: flex; align-items: center; padding: 0 12px;
   margin-bottom: 6px; border: 1px solid {LINE}; font: 400 14px/20px Bitter; color: {MUTED}; }}
.shelfline {{ display: flex; justify-content: space-between; align-items: center;
   padding: 4px 0 10px; font: 600 11px/1 Barlow; font-feature-settings: 'smcp','tnum';
   color: {MUTED}; }}
.shelfline em {{ font-style: normal; color: {MOSS}; }}
.shelfline em.dim {{ color: {HAIR}; }}
.grid3 {{ display: grid; grid-template-columns: repeat({IDX_COLS}, {IDX_CELL}px);
          column-gap: {IDX_GUTTER}px; row-gap: {IDX_ROWGAP}px; }}
.card {{ display: flex; flex-direction: column; align-items: center; }}
/* Dos líneas fijas, que es lo que forma una sola línea base en toda la fila. */
.cname {{ font: 400 17px/21px Bitter; text-align: center; padding: 6px 0 2px; height: 44px;
          overflow: hidden; display: -webkit-box; -webkit-line-clamp: 2;
          -webkit-box-orient: vertical; }}
.ratio {{ font: 600 12px/14px Barlow; font-feature-settings: 'smcp','tnum'; color: {RUST}; }}
.count {{ font: 600 12px/14px Barlow; font-feature-settings: 'smcp','tnum'; color: {MUTED}; }}
.cost {{ font: 600 13px/15px Barlow; font-feature-settings: 'smcp','tnum'; color: {MOSS};
         text-align: center; }}
.cost u {{ display: block; text-decoration: none; font-size: 10px; color: {MUTED};
           font-weight: 400; }}
.marked {{ font: 600 10px/13px Barlow; font-feature-settings: 'smcp','tnum'; color: {MOSS}; }}

/* ── el hueco troquelado (AlbumPaper.kt) ─────────────────────────────────── */
.hole {{ position: relative; border-radius: 50%; background: {CARD}; flex: 0 0 auto;
   box-shadow: inset 0 1.6px 2.4px rgba(45,48,41,.22),
               inset 0 -1.6px 2.4px rgba(255,255,255,.85);
   outline: 1px solid {HAIR}; outline-offset: -1px; }}
/* El color de respaldo va en background-color y nunca en el atajo: `.hole i` (0,1,1) le gana el
   background-image a `.p123` (0,1,0) y se comería la foto. */
.hole i {{ position: absolute; border-radius: 50%; display: block; background-color: {DEEP};
   background-repeat: no-repeat; background-position: center; background-size: cover; }}
.hole:not(.miss) i:after {{ content: ''; position: absolute; inset: 0; border-radius: 50%;
   background: linear-gradient(118deg, rgba(255,255,255,.34) 0 18%, rgba(255,255,255,0) 42%); }}
/* El fantasma de una casilla vacía: alfa 0,14 y el círculo de puntos. */
.hole.miss i {{ opacity: .14; filter: grayscale(.2); }}
.hole.miss:before {{ content: ''; position: absolute; inset: 6px; border-radius: 50%;
   border: 1px dashed rgba(45,48,41,.48); }}
/* El sello del precio (#493) y la marca del deseo (#497), al mismo centímetro del hueco. */
.hole .chip {{ position: absolute; left: 50%; top: 50%; transform: translate(-50%,-50%);
   font: 600 13px Barlow; font-feature-settings: 'smcp','tnum'; color: {RUST};
   background: rgba(238,232,215,.92); border: 1px solid rgba(139,85,60,.5);
   padding: 3px 7px; white-space: nowrap; }}
.hole .mark {{ position: absolute; left: 50%; bottom: 12px; transform: translateX(-50%);
   font: 400 11px/1 Bitter; font-style: normal; color: {MOSS};
   background: rgba(238,232,215,.92); border: 1px solid rgba(73,92,73,.45); padding: 2px 6px;
   white-space: nowrap; }}
{PHOTO_CSS}
"""

PANELS = "".join(
    f'<div class="phone" data-v="{key}" data-t="{state}">{render(TASADAS[state])}</div>'
    for key, _, render, _ in VARIANTS
    for state, _, _ in STATES
)

BAR = (
    "".join(f'<button data-v="{k}">{k} · {n}</button>' for k, n, _, _ in VARIANTS)
    + '<span class="sep"></span>'
    + "".join(f'<button data-t="{k}">{n}</button>' for k, n, _ in STATES)
    + f'<span class="m" id="metric"></span>'
)

JS = f"""
const CAPS = {json.dumps({k: c for k, _, _, c in VARIANTS})};
const STATES = {json.dumps({k: c for k, _, c in STATES})};
let v = new URLSearchParams(location.search).get('v') || '0';
let t = new URLSearchParams(location.search).get('t') || 'nada';
function paint() {{
  document.querySelectorAll('.phone').forEach(p =>
    p.classList.toggle('on', p.dataset.v === v && p.dataset.t === t));
  document.querySelectorAll('header button').forEach(b => b.classList.toggle('on',
    b.dataset.v !== undefined ? b.dataset.v === v : b.dataset.t === t));
  document.getElementById('cap').innerHTML =
    CAPS[v] + '<span class="st">' + STATES[t] + '</span>';
  history.replaceState(null, '', '?v=' + v + '&t=' + t);
  measure();
}}
/* Cuenta lo que de verdad entra en el pliegue: la fracción de cada unidad que solapa con el
   viewport del scroll, para que el número no dependa de mi aritmética de paddings. */
function measure() {{
  const phone = document.querySelector('.phone.on');
  if (!phone) return;
  const sc = phone.querySelector('.scroll');
  const box = sc.getBoundingClientRect();
  const units = [...phone.querySelectorAll('.card, .wcell')];
  let vis = 0, h = 0;
  units.forEach(u => {{
    const r = u.getBoundingClientRect();
    h = Math.max(h, r.height);
    const ov = Math.min(r.bottom, box.bottom) - Math.max(r.top, box.top);
    if (ov > 0) vis += Math.min(1, ov / r.height);
  }});
  document.getElementById('metric').textContent =
    `pliegue ${{sc.clientHeight}} dp · unidad ${{h.toFixed(0)}} dp · ` +
    `${{vis.toFixed(1)}} visibles de ${{units.length}}`;
}}
document.querySelectorAll('header button').forEach(b => b.addEventListener('click', () => {{
  if (b.dataset.v !== undefined) v = b.dataset.v; else t = b.dataset.t;
  paint();
}}));
addEventListener('keydown', e => {{
  const keys = {json.dumps([k for k, _, _, _ in VARIANTS])};
  const i = keys.indexOf(v);
  if (e.key === 'ArrowRight') {{ v = keys[(i + 1) % keys.length]; paint(); }}
  if (e.key === 'ArrowLeft') {{ v = keys[(i - 1 + keys.length) % keys.length]; paint(); }}
}});
window.setState = (nv, nt) => {{ if (nv) v = nv; if (nt) t = nt; paint(); }};
window.scrollPhone = px => {{ document.querySelector('.phone.on .scroll').scrollTop = px; }};
window.metrics = () => {{
  const sc = document.querySelector('.phone.on .scroll');
  return {{fold: sc.clientHeight, scroll: sc.scrollHeight,
           text: document.getElementById('metric').textContent}};
}};
document.fonts.ready.then(paint);
paint();
"""

LEDGER = f"""
<h2>Cómo está hecha esta maqueta</h2>
<dl>
<dt>Tamaño</dt><dd>411 × 914 dp, el Pixel 7 de las capturas del #296, con 1 px CSS = 1 dp. El
cromo se leyó en el código: <code>CoindexApp.Masthead</code> con «Volver» y sin Ajustes, la
rejilla de <code>IndexScreen</code> (margen 12, calle 8, tarjeta de 104 dp y nombre de dos líneas
fijas), la de <code>ExploreScreen</code> (margen 20, <code>PlateSpacing.rowGap</code> 32) y el
hueco de <code>AlbumPaper</code>. <b>No lleva barra inferior</b>: Explorar no es raíz, se entra por
la puerta del índice y se sale con «Volver».</dd>
<dt>Datos</dt><dd>Los {len(MINE) + len(SHELF)} catálogos medibles de <code>data/</code> cruzados
con la colección del padre (229 filas), con <code>memberMatches</code> entera y la evidencia de
<code>resolvePlate</code>. Las {len(SHELF)} del escaparate son las curadas sin evidencia de menos
de 20 casillas. La cara de cada casilla con <code>printed_side</code> y, si no lo declara, el
reverso primero.</dd>
<dt>Las siete marcas son inventadas</dt><dd>El #497 se publicó ayer: el padre no ha marcado
nada todavía. Cinco caen en tres láminas suyas y <b>dos en dos del escaparate</b>, que es el caso
que el #494 tiene abierto — un total con dos edades.</dd>
<dt>Precios</dt><dd><b>Suelo de la plata y no precio de catálogo</b>: gramos × ley del
<code>composition.text</code> × {SPOT['eur_oz']:.2f} €/oz ({SPOT['at'][:10]}). La app enseñaría el
mayor de tres precios, así que los de verdad son <b>más altos</b>. <b>Sin el umbral de 10
casillas</b>, que es lo que el #498 manda corregir del prototipo del #279.</dd>
<dt>Lo medido no cuadra con el ticket</dt><dd>El #498 dice «14 láminas de ≤10 y 6 de 11-19, 17 de
20 con suelo de plata, 4 láminas a 34 llamadas, 245 las veinte». Hoy <code>data/</code> da
<b>{sum(1 for c in SHELF if c['issued'] <= 10)} de ≤10 y
{sum(1 for c in SHELF if c['issued'] > 10)} de 11-19</b>,
<b>{sum(1 for c in SHELF if not c['unpriced'])} de 20 con suelo completo</b>,
<b>una sola lámina a 34 llamadas</b> y <b>{sum(c['calls'] for c in SHELF)} las veinte</b>. Los
catálogos y la caché de tipos han crecido desde el #282; el orden de magnitud aguanta y las
cifras concretas del ticket hay que rehacerlas.</dd>
<dt>Lo que la maqueta no prueba</dt><dd>La densidad. El listón del #279 salía un 10 % más denso
que el emulador, y aquí no hay captura del AVD de esta pantalla contra la que calibrar: los
números del pliegue son de la maqueta. <b>Nada de esto se ha visto en un teléfono</b>
(<code>medir-en-el-movil-no-en-el-asset</code>).</dd>
<dt>Lo que ninguna variante hace</dt><dd>Ofrecer tasar las veinte de golpe. El #498 lo dice
—«nunca de golpe»— y las {sum(c['calls'] for c in SHELF)} consultas del estante entero sólo se
gastan lámina a lámina, cada una nombrando su precio antes de pulsarse.</dd>
</dl>
"""

HTML = f"""<!doctype html>
<html lang="es"><head><meta charset="utf-8">
<title>Prototipo #498 · «Explorar» con dos cosas dentro</title>
<style>{CSS}
.ledger {{ max-width: 640px; margin: 26px auto 0; padding: 0 16px;
   font: 13px/1.6 -apple-system, system-ui, sans-serif; color: #c8c2b2; }}
.ledger h2 {{ font: 600 13px/1 -apple-system, system-ui, sans-serif; letter-spacing: .09em;
   text-transform: uppercase; color: #8d8878; padding-bottom: 12px;
   border-bottom: 1px solid #4a4a44; margin-bottom: 14px; }}
.ledger dt {{ font-weight: 600; color: #e6e0cf; padding-top: 10px; }}
.ledger b {{ color: #e8dfa8; }}
.ledger code {{ font: 11px/1 ui-monospace, monospace; background: #2a2a26; padding: 2px 4px;
   border-radius: 2px; color: #cfc9b6; }}
</style></head>
<body>
<header>{BAR}</header>
<div class="cap" id="cap"></div>
<div class="stage">{PANELS}</div>
<div class="ledger">{LEDGER}</div>
<script>{JS}</script>
</body></html>
"""

out = f"{OUT}/maqueta.html"
open(out, "w").write(HTML)
print(f"{out}  ({len(HTML) / 1024 / 1024:.2f} MB · {len(PHOTOS)} fotos)")
