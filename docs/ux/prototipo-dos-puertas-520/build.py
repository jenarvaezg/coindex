#!/usr/bin/env python3
"""Maqueta del #520: dónde cae «Lo que busco» cuando la puerta compuesta se parte en dos.

PROTOTIPO — se tira cuando el ticket se cierre. Lo que sobrevive es el README.

El ticket ya decidió **qué**: dos filas, cada una con un nombre estable y un solo destino, y «Lo
que busco» como anexo hermano colgando de Colecciones. Lo que no decidió es **dónde caen esas dos
filas ni cómo se dibujan**, y ahí hay una contradicción que sólo se ve a tamaño real: la decisión
promete «un tap de home con su recuento en la primera vista», y ADR 0026 §8 cláusula 3 dice que la
puerta del anexo es *lo último de la página*. Con 69 tarjetas, lo último de la página está a cinco
pliegues del arranque.

Siete formas x cinco estados, a dp real y con la de hoy de listón. En HTML y no en Compose porque
lo que se elige es estructura (`prototipar-forma-en-html`).

Las medidas salen del código y no de la vista: `IndexScreen.kt` (margen 12, calle 8, paso 6, hueco
104, `indexColumns`), `AlbumChrome.kt` (54), `FilterShelf.kt` (buscador 40, fila 48), `AnnexDoor`
(papel profundo, 14 de aire, 8 encima) y las once escalas de `fieldTypography`.

    python3 docs/ux/prototipo-dos-puertas-520/extract.py
    python3 docs/ux/prototipo-dos-puertas-520/build.py
    open /private/tmp/coindex-privado/dos-puertas-520/maqueta.html
"""
import base64
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.abspath(f"{HERE}/../../..")
OUT = "/private/tmp/coindex-privado/dos-puertas-520"

D = json.load(open(f"{OUT}/data.json"))
CARDS, SHOWCASE, MARKS, SEWN = D["cards"], D["showcase"], D["marks"], D["sewn"]

# ── el papel (Theme.kt:27-36) ───────────────────────────────────────────────
INK, MUTED, PAPER = "#2D3029", "#686A5D", "#EEE8D7"
DEEP, LINE, MOSS, RUST, HAIR = "#DDD3BB", "#7D806C", "#495C49", "#8B553C", "#878577"

# ── la pantalla: el Pixel 7 de las capturas del #296, 1 px CSS = 1 dp ───────
SCREEN, TALL = 411, 914
STATUS = 24
CHROME = 54                       # AlbumChrome
MARGIN, GUTTER, PITCH = 12, 8, 6  # contentPadding, horizontalArrangement, verticalArrangement
HOLE, RING = 104, 5
SEARCH_H, SHELF_H = 40, 48        # SEARCH_FIELD_HEIGHT y la fila del estante
DOOR_PAD, DOOR_TOP = 14, 8        # AnnexDoor
NAME_H = 2 * 21 + 6 + 2           # dos líneas de titleMedium con su aire (COLLECTION_NAME_LINES)

# ── los rótulos, literales del código ──────────────────────────────────────
SEWN_LABEL = f"{SEWN['collections']} colecciones · {SEWN['pieces']} piezas · {SEWN['types']} tipos"
SEARCH_PLACEHOLDER = "Buscar entre tus colecciones"
SHELF_SUMMARY = "Filtros y orden"
EXPORT_LABEL = "Exportar láminas"
WISH_NAME = "Lo que busco"
KILOS = "6,93 kg"                 # el peso de la colección del padre, del prototipo del #519


def b64(path, mime):
    return f"data:{mime};base64," + base64.b64encode(open(path, "rb").read()).decode()


def font(name):
    return b64(f"{REPO}/app/src/main/res/font/{name}.ttf", "font/ttf")


PHOTOS = {}
for entry in CARDS + MARKS:
    tid = entry.get("tid")
    path = f"{OUT}/fotos/{tid}.jpg"
    if tid and tid not in PHOTOS and os.path.exists(path):
        PHOTOS[tid] = b64(path, "image/jpeg")

CSS_PHOTOS = "\n".join(f'.p{t}{{background-image:url("{u}")}}' for t, u in PHOTOS.items())


def plural(n, one, many):
    return f"{n} {one if n == 1 else many}"


def wish_census(slots, plates):
    """`wishCensusLabel`: dos unidades, porque la lista cruza láminas."""
    return f"{plural(slots, 'casilla', 'casillas')} en {plural(plates, 'lámina', 'láminas')}"


def other_plates(plates):
    """`otherPlates`: el número se va con el singular."""
    return "otra lámina" if plates == 1 else f"otras {plates} láminas"


def composed_label(wishes, plates):
    """`annexDoorLabel`, la puerta de hoy en sus tres formas."""
    if wishes > 0 and plates > 0:
        return f"{WISH_NAME} · {wishes}, y {other_plates(plates)}"
    if wishes > 0:
        return f"{WISH_NAME} · {wishes}"
    if plates > 0:
        return f"Y {other_plates(plates)} que no coleccionas"
    return None


# ── piezas de cromo ─────────────────────────────────────────────────────────
def hole(tid, size=HOLE, missing=False, lit=False):
    """El hueco troquelado de `AlbumPaper.kt`: cartón, pared del corte, filete y la foto dentro.

    `lit` es la enmienda de Jose del 17 de agosto de 2026 al elegir la E: una casilla que **buscas**
    no es una que te falta de una lámina que sigues, así que la moneda se ve entera y lo que dice que
    no es tuya es el filete de puntos. El idioma completo se decide en el #556.
    """
    ring = max(2, round(RING * size / HOLE))
    inner = f'<i class="p{tid}" style="inset:{ring}px"></i>' if tid in PHOTOS else ""
    marks = (" miss" if missing else "") + (" lit" if lit else "")
    return (f'<div class="hole{marks}" '
            f'style="width:{size}px;height:{size}px">{inner}</div>')


def card(c):
    # `indexCoverageLabel` cuando hay fracción, `countLabel` cuando la tarjeta no divide por nada.
    count = (f'{c["owned"]}/{c["issued"]}' if c["kind"] == "plate"
             else f'{plural(c["qty"], "moneda", "monedas")} · '
                  f'{plural(c["types"], "tipo", "tipos")}')
    return (f'<div class="card">{hole(c.get("tid"))}'
            f'<div class="cname">{c["name"]}</div><div class="ratio">{count}</div></div>')


CHROME_HTML = (f'<div class="chrome"><b>COINDEX</b><span>{SEWN_LABEL}</span>'
               f'<u class="glyph"></u></div>')


def head(shown, total, query, over=""):
    box = (f'<span class="typed">{query}</span>' if query
           else f'<span class="ph">{SEARCH_PLACEHOLDER}</span>')
    tally = (f"{shown} de {plural(total, 'colección', 'colecciones')}" if shown != total
             else plural(total, "colección", "colecciones"))
    return (f'{CHROME_HTML}{over}<div class="search"><u class="lens"></u>{box}</div>'
            f'<div class="shelf"><span class="sum">&#9656; {SHELF_SUMMARY}</span>'
            f'<span class="tally">{tally}</span><span class="export">{EXPORT_LABEL}</span></div>')


BAR = (f'<div class="bar"><span class="on">Colecciones · {SEWN["collections"]}</span>'
       f'<span>Monedas · {SEWN["types"]}</span><span>Las cifras · {KILOS}</span></div>')


def door(label, note=None, extra="", mark=False, sole=True):
    """Una fila de anexo: papel profundo, su nombre y la flecha. `mark` la marca para medirla."""
    body = f'<div class="dlabel">{label}</div>'
    if note:
        body += f'<div class="dnote">{note}</div>'
    body += extra
    cls = "door" + (" first" if not sole else "") + (" wish" if mark else "")
    return f'<div class="{cls}"><div class="dcol">{body}</div><u class="fwd"></u></div>'


def marks_strip(wishes):
    """Las casillas marcadas como monedas, con el resto dicho en palabras."""
    shown = MARKS[:3]
    strip = "".join(hole(m["tid"], size=40, missing=True, lit=True) for m in shown)
    rest = wishes - len(shown)
    tail = f'<em>y {rest} más</em>' if rest > 0 else ""
    return f'<div class="strip">{strip}{tail}</div>'


# ── las siete formas ────────────────────────────────────────────────────────
def wish_row(state, censo=False, monedas=False):
    wishes, plates = state["wishes"], state["plates"]
    if wishes == 0:
        return ""
    extra = marks_strip(wishes) if monedas else ""
    note = wish_census(wishes, PLATES_OF[wishes]) if censo else None
    return door(f"{WISH_NAME} · {wishes}", note=note, extra=extra, mark=True)


def shelf_row(state, note=None):
    plates = state["plates"]
    if plates == 0:
        return ""
    return door(f"Y {other_plates(plates)} que no coleccionas", note=note)


# Cuántas láminas guardan las marcas: 1 de verdad hoy, y en la feria preparada las cinco que
# `wishCensusLabel` contaría si el padre marcase siete casillas.
PLATES_OF = {0: 0, 2: D["wish_plates"], 7: 5}


def variant_0(state, note):
    label = composed_label(state["wishes"], state["plates"])
    return "" if label is None else door(label, note=note, mark=state["wishes"] > 0)


def variant_a(state, note):
    return wish_row(state) + shelf_row(state, note=note)


def variant_b(state, note):
    wishes, plates = state["wishes"], state["plates"]
    rows = []
    if wishes:
        rows.append(door(f"{WISH_NAME} · {wishes}", mark=True, sole=False))
    if plates:
        rows.append(door(f"Y {other_plates(plates)} que no coleccionas", note=note, sole=False))
    if not rows:
        return ""
    return f'<div class="sewnblock">{"".join(rows)}</div>'


def variant_c(state, note):
    return shelf_row(state, note=note)          # la fila de arriba la pone `screen`


def variant_f(state, note):
    return shelf_row(state, note=note)


VARIANTS = {
    "0": ("Hoy · una fila y dos destinos", "pie", variant_0),
    "A": ("Dos filas al pie", "pie", variant_a),
    "B": ("Un canto con dos renglones", "pie", variant_b),
    "C": ("La lista arriba, el estante al pie", "arriba", variant_c),
    "D": ("Arriba, con su censo", "arriba-censo", variant_c),
    "E": ("Arriba, con las monedas marcadas", "arriba-monedas", variant_c),
    "F": ("El canto cosido lo dice", "canto", variant_f),
    "G": ("Arriba del buscador", "sobre", variant_c),
}

STATES = {
    "hoy": dict(name="Hoy · 2 marcas", wishes=2, plates=len(SHOWCASE), query=None, shown=None),
    "feria": dict(name="Una feria preparada · 7 marcas", wishes=7, plates=len(SHOWCASE),
                  query=None, shown=None),
    "sin": dict(name="Nada marcado", wishes=0, plates=len(SHOWCASE), query=None, shown=None),
    "cerrado": dict(name="Sin estante · 7 marcas", wishes=7, plates=0, query=None, shown=None),
    "buscando": dict(name="Buscando «venez» · 3 tarjetas", wishes=2, plates=len(SHOWCASE),
                     query="venez", shown=6),
}


def screen(key, state):
    label, where, foot = VARIANTS[key]
    note = "Lo que escribes arriba no llega hasta aquí." if state["query"] else None
    cards = CARDS if state["shown"] is None else [
        c for c in CARDS if "venez" in c["name"].lower() or "bolívar" in c["name"].lower()
    ][:state["shown"]]
    grid = f'<div class="grid">{"".join(card(c) for c in cards)}</div>'
    top, over = "", ""
    if where.startswith("arriba"):
        top = wish_row(state, censo=where.endswith("censo"), monedas=where.endswith("monedas"))
    if where == "sobre":
        over = wish_row(state)
    sewn = SEWN_LABEL
    if where == "canto" and state["wishes"]:
        sewn = f"{SEWN_LABEL} · busco {state['wishes']}"
    header = head(len(cards), SEWN["collections"], state["query"], over=over) \
        .replace(SEWN_LABEL, sewn)
    return (f'<div class="phone"><div class="status"><span>16:24</span></div>'
            f'<div class="scroll">{header}{top}{grid}{foot(state, note)}</div>{BAR}</div>')


SCREENS = "".join(
    f'<div class="slot" data-v="{v}" data-e="{e}" style="display:none">{screen(v, st)}</div>'
    for v in VARIANTS for e, st in STATES.items()
)

VTABS = "".join(f'<button data-axis="v" data-k="{k}" onclick="pick(\'v\',\'{k}\')">'
                f'{k} · {n}</button>' for k, (n, _, _) in VARIANTS.items())
ETABS = "".join(f'<button data-axis="e" data-k="{k}" onclick="pick(\'e\',\'{k}\')">'
                f'{st["name"]}</button>' for k, st in STATES.items())

STYLE = f"""<meta charset="utf-8"><title>Las dos puertas de Colecciones</title>
<meta name="viewport" content="width=device-width,initial-scale=1">
<style>
@font-face{{font-family:Bitter;src:url({font('bitter_variable')}) format('truetype')}}
@font-face{{font-family:Barlow;font-weight:400;
  src:url({font('barlow_condensed_regular')}) format('truetype')}}
@font-face{{font-family:Barlow;font-weight:600;
  src:url({font('barlow_condensed_semibold')}) format('truetype')}}
*{{box-sizing:border-box;margin:0;padding:0}}
body{{background:#1b1c19;color:{PAPER};font:13px/1.5 -apple-system,system-ui,sans-serif;
  padding-bottom:48px}}
header{{position:sticky;top:0;z-index:9;background:#111210;border-bottom:1px solid #3a3d33;
  padding:8px 12px;display:flex;flex-direction:column;gap:6px}}
header .row{{display:flex;gap:6px;flex-wrap:wrap;align-items:center}}
header .row > em{{font-style:normal;opacity:.45;font-size:11px;width:56px}}
button{{font:600 12px/1 -apple-system,system-ui,sans-serif;padding:7px 9px;cursor:pointer;
  background:#2c2e28;color:#e8e2d2;border:1px solid #494c41;border-radius:3px}}
button.on{{background:{PAPER};color:{INK};border-color:{PAPER}}}
.stage{{display:flex;justify-content:center;padding:14px 8px 0}}
.cap{{max-width:{SCREEN}px;margin:12px auto 0;font-size:12.5px;line-height:1.5;color:#b8b3a4}}
.cap b{{color:#e9e3d1}}
.readout{{max-width:{SCREEN}px;margin:8px auto 0;font:11px/1.6 ui-monospace,monospace;
  color:#a7c39a;white-space:pre-wrap}}
.ledger{{max-width:660px;margin:26px auto 0;padding:0 16px;font-size:13px;line-height:1.62;
  color:#c8c2b2}}
.ledger h2{{font:600 12px/1 -apple-system,system-ui,sans-serif;letter-spacing:.09em;
  text-transform:uppercase;color:#8d8878;padding-bottom:10px;border-bottom:1px solid #3a3d33;
  margin-bottom:12px}}
.ledger dt{{font-weight:600;color:#e6e0cf;margin-top:12px}}
.ledger code{{font:11px/1 ui-monospace,monospace;background:#26271f;padding:2px 4px;color:#cfc9b6}}

/* ── el teléfono ── */
.phone{{width:{SCREEN}px;height:{TALL}px;overflow:hidden;position:relative;background:{PAPER};
  color:{INK};font-family:Bitter,serif;display:flex;flex-direction:column;
  box-shadow:0 10px 34px rgba(0,0,0,.55)}}
.status{{height:{STATUS}px;flex:0 0 {STATUS}px;display:flex;align-items:center;
  justify-content:flex-end;padding-right:14px;font:600 10px/1 Barlow;
  font-feature-settings:'smcp','tnum';color:{MUTED}}}
.scroll{{flex:1;overflow-y:auto;overflow-x:hidden;padding:8px {MARGIN}px}}
.chrome{{height:{CHROME}px;background:{INK};color:{PAPER};display:flex;align-items:center;
  gap:10px;padding:0 4px 0 12px}}
.chrome b{{font:400 17px/21px Bitter}}
.chrome span{{flex:1;font:600 10px/1 Barlow;font-feature-settings:'smcp','tnum';color:{DEEP};
  white-space:nowrap;overflow:hidden;text-overflow:ellipsis}}
/* Tres reglas con su cuenta encima: el glifo de Ajustes de AlbumChrome.kt, 48 dp con 13 de aire. */
.glyph{{width:48px;height:48px;flex:0 0 48px;position:relative}}
.glyph:before{{content:'';position:absolute;inset:13px;background:
  linear-gradient({PAPER},{PAPER}) no-repeat 0 4px/100% 1.5px,
  linear-gradient({PAPER},{PAPER}) no-repeat 0 11px/100% 1.5px,
  linear-gradient({PAPER},{PAPER}) no-repeat 0 18px/100% 1.5px}}
.glyph:after{{content:'';position:absolute;inset:13px;background:
  radial-gradient(circle 2.8px at 28% 4.7px,{INK} 55%,{PAPER} 56% 70%,transparent 71%),
  radial-gradient(circle 2.8px at 66% 11.7px,{INK} 55%,{PAPER} 56% 70%,transparent 71%),
  radial-gradient(circle 2.8px at 43% 18.7px,{INK} 55%,{PAPER} 56% 70%,transparent 71%)}}
.search{{height:{SEARCH_H}px;display:flex;align-items:center;gap:10px;padding:0 10px;
  background:rgba(255,252,242,.58);border:1px solid {LINE};margin-top:8px}}
.search .ph{{font:400 16px/23px Bitter;color:{MUTED}}}
.search .typed{{font:400 16px/23px Bitter;color:{INK}}}
.lens{{width:18px;height:18px;flex:0 0 18px;border:1.5px solid {MUTED};border-radius:50%;
  position:relative}}
.lens:after{{content:'';position:absolute;width:1.5px;height:7px;background:{MUTED};
  right:-2px;bottom:-6px;transform:rotate(-45deg)}}
.shelf{{height:{SHELF_H}px;display:flex;align-items:center;gap:10px}}
.shelf .sum{{font:400 17px/21px Bitter;flex:1}}
.shelf .tally{{font:600 12px/1 Barlow;font-feature-settings:'smcp','tnum';color:{RUST}}}
.shelf .export{{font:600 12px/1 Barlow;font-feature-settings:'smcp','tnum';color:{MOSS};
  border:1px solid {LINE};padding:11px 10px}}
.bar{{flex:0 0 auto;display:flex;border-top:2px solid {INK};padding-bottom:19px}}
.bar span{{flex:1;text-align:center;padding:16px 0;background:{DEEP};color:{INK};
  font:600 12px/15px Barlow;font-feature-settings:'smcp','tnum'}}
.bar span.on{{background:{INK};color:{PAPER}}}

/* ── el hueco troquelado (AlbumPaper.kt) ── */
.hole{{position:relative;border-radius:50%;background-color:rgba(255,252,242,.58);
  box-shadow:inset 0 1.6px 2.4px rgba(45,48,41,.22),inset 0 -1.6px 2.4px rgba(255,255,255,.85);
  outline:1px solid {HAIR};outline-offset:-1px;flex:0 0 auto}}
.hole i{{position:absolute;border-radius:50%;display:block;background-color:{DEEP};
  background-repeat:no-repeat;background-position:center;background-size:cover}}
.hole:not(.miss) i:after{{content:'';position:absolute;inset:0;border-radius:50%;
  background:linear-gradient(118deg,rgba(255,255,255,.34) 0 18%,rgba(255,255,255,0) 42%)}}
.hole.miss i{{opacity:.14;filter:grayscale(.2)}}
.hole.miss:before{{content:'';position:absolute;inset:4px;border-radius:50%;
  border:1px dashed rgba(45,48,41,.48)}}
/* La casilla que buscas, a plena luz: la moneda entera y el filete de puntos como única marca. */
.hole.miss.lit i{{opacity:1;filter:none}}
.hole.miss.lit i:after{{content:'';position:absolute;inset:0;border-radius:50%;
  background:linear-gradient(118deg,rgba(255,255,255,.34) 0 18%,rgba(255,255,255,0) 42%)}}

/* ── la tarjeta del índice (IndexScreen.kt) ── */
.grid{{display:grid;grid-template-columns:repeat(3,{HOLE}px);gap:{PITCH}px {GUTTER}px;
  justify-content:start;padding-top:{PITCH}px}}
.card{{width:{HOLE}px;display:flex;flex-direction:column;align-items:center}}
.cname{{font:400 15px/21px Bitter;text-align:center;padding:6px 0 2px;height:{NAME_H}px;
  overflow:hidden;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical}}
.ratio{{font:600 12px/15px Barlow;font-feature-settings:'smcp','tnum';color:{RUST}}}

/* ── la fila del anexo (AnnexDoor) ── */
.door{{margin-top:{DOOR_TOP}px;background:{DEEP};display:flex;align-items:center;
  justify-content:space-between;gap:10px;padding:{DOOR_PAD}px}}
.door.first{{margin-top:0}}
.sewnblock{{margin-top:{DOOR_TOP}px;background:{DEEP}}}
.sewnblock .door + .door{{border-top:1px solid {HAIR}}}
.dlabel{{font:600 12px/15px Barlow;font-feature-settings:'smcp','tnum';color:{INK}}}
.dnote{{font:400 12px/16px -apple-system,system-ui,sans-serif;color:{MUTED};padding-top:4px}}
.strip{{display:flex;align-items:center;gap:6px;padding-top:8px}}
.strip em{{font:600 11px/1 Barlow;font-feature-settings:'smcp','tnum';color:{MUTED};
  font-style:normal}}
.fwd{{width:20px;height:20px;flex:0 0 20px;position:relative}}
.fwd:before{{content:'';position:absolute;left:5px;top:5px;width:9px;height:9px;
  border-top:1.6px solid {MOSS};border-right:1.6px solid {MOSS};transform:rotate(45deg)}}
{CSS_PHOTOS}
</style>
"""

CAPS = {
    "0": "<b>Listón · v1.5.0.</b> Una fila al pie con dos destinos prometidos en un rótulo: el tap "
         "abre «Explorar», y «Lo que busco» está una puerta más allá. Con 69 tarjetas, esta fila "
         "está a cinco pliegues del arranque.",
    "A": "<b>Tesis: dos filas gemelas, la lectura literal del ticket.</b> Cada una con su nombre y "
         "su destino, las dos al pie. Resuelve el rótulo doble y <b>no</b> la primera vista: sigue "
         "haciendo falta bajar el índice entero para leer el recuento.",
    "B": "<b>Tesis: es un objeto con dos renglones, no dos objetos.</b> Un solo canto de papel "
         "profundo con un filete dentro. Dice que las dos filas son el borde cosido del álbum y no "
         "dos tarjetas más.",
    "C": "<b>Tesis: la primera vista es arriba o no es.</b> «Lo que busco» sube bajo el estante, "
         "antes de la primera tarjeta; el estante ajeno se queda al pie, que es donde ADR 0026 §8 "
         "lo pone. Dos filas, dos sitios, un nombre cada una.",
    "D": "<b>Tesis: la fila de arriba le debe su censo al lector.</b> Como la C, más «2 casillas en "
         "1 lámina» debajo: qué hay detrás, antes de tocar.",
    "E": "<b>Elegida el 17 de agosto de 2026.</b> Tesis: lo que engancha son las monedas, no el "
         "recuento. Como la C, con las casillas marcadas dibujadas en la fila — y <b>a plena luz</b>, "
         "que es la enmienda de la misma decisión: una casilla que buscas no es una que te falta de "
         "una lámina que sigues, así que la moneda se ve entera y lo que dice que no es tuya es el "
         "filete de puntos. El idioma completo de los fantasmas se decide en el #556.",
    "G": "<b>Tesis: entre el buscador y la lista es el peor sitio para un recuento que el "
         "buscador no mueve.</b> La misma fila de la C, pero <b>encima</b> del buscador, pegada al "
         "canto: primero lo que hay, después la herramienta de mirar. Pon el estado «buscando» y "
         "compárala con la C.",
    "F": "<b>Tesis: la primera vista ya existe, y es el canto cosido.</b> Ninguna fila nueva "
         "arriba: el canto añade «busco 2» a su recuento y el pie se queda sólo con el estante. "
         "Cero dp de coste — y hay que comprobar si el canto puede recibir un tap.",
}

HTML = STYLE + f"""<header>
<div class="row"><em>forma</em>{VTABS}</div>
<div class="row"><em>estado</em>{ETABS}</div>
</header>
<div class="stage">{SCREENS}</div>
<div class="cap" id="cap"></div>
<div class="readout" id="readout"></div>
<div class="ledger">
<h2>Cómo está hecha, y qué no prueba</h2>
<dl>
<dt>Tamaño y cromo</dt><dd>411 × 914 dp, 1 px CSS = 1 dp, el Pixel 7 de las capturas del #296. El
canto cosido (54 dp), el buscador (40), la fila del estante (48), el margen de 12, la calle de 8, el
paso de 6 y el hueco de 104 se leyeron en <code>IndexScreen.kt</code>,
<code>FilterShelf.kt</code> y <code>AlbumChrome.kt</code>. Las tipografías son las del APK.
<b>El canto cosido, el buscador y el estante scrollean</b>, porque en la app son filas de la misma
rejilla que las tarjetas.</dd>
<dt>Datos</dt><dd>La caché sembrada del móvil del padre (<code>.local/padre/coindex.db</code>), que
es lo que la app lee: {SEWN['collections']} tarjetas, {SEWN['pieces']} piezas, {SEWN['types']}
tipos, {len(SHOWCASE)} láminas en la ventana del estante y <b>las dos casillas que tiene marcadas de
verdad</b>. Las fracciones salen de <code>memberMatches</code> y la ventana de
<code>showcasePlate</code>.</dd>
<dt>Lo que no prueba</dt><dd>La derivación de las tarjetas sin catálogo es la regla gruesa
—familia sembrada y peso— así que las {SEWN['collections']} son un <b>suelo</b>: el índice real es
igual o más largo, y la fila del pie está igual o más lejos. El estado «feria» inventa siete marcas
sobre las dos reales, y su tira de monedas enseña las dos que hay. Y <b>nada de esto se ha visto en
un teléfono</b> (<code>medir-en-el-movil-no-en-el-asset</code>): el navegador elige, el emulador
confirma.</dd>
</dl>
</div>
<script>
const V={json.dumps(list(VARIANTS))}, E={json.dumps(list(STATES))};
const CAPS={json.dumps(CAPS)};
let cur={{v:"0",e:"hoy"}};
function slot(){{return document.querySelector(`.slot[data-v="${{cur.v}}"][data-e="${{cur.e}}"]`);}}
function pick(axis,k){{
  cur[axis]=k; localStorage.setItem("dos-puertas",JSON.stringify(cur));
  document.querySelectorAll(".slot").forEach(s=>s.style.display="none");
  slot().style.display="block";
  document.querySelectorAll("button").forEach(b=>
    b.classList.toggle("on",cur[b.dataset.axis]===b.dataset.k));
  document.getElementById("cap").innerHTML=CAPS[cur.v]||"";
  location.hash=cur.v+"-"+cur.e; measure();
}}
/* Lo que se mide: cuánto pliegue queda para las tarjetas, cuántas entran, y cuánto hay que
   arrastrar para que la fila de «Lo que busco» aparezca. El dp no lo pongo yo: lo dice el layout. */
function measure(){{
  const p=slot(), sc=p.querySelector(".scroll"), box=sc.getBoundingClientRect();
  const cards=[...p.querySelectorAll(".card")];
  let vis=0;
  cards.forEach(u=>{{const r=u.getBoundingClientRect();
    const ov=Math.min(r.bottom,box.bottom)-Math.max(r.top,box.top);
    if(ov>0) vis+=Math.min(1,ov/r.height);}});
  const wish=p.querySelector(".door.wish"), canto=p.querySelector(".chrome span");
  let line;
  if(wish){{
    const r=wish.getBoundingClientRect();
    const drag=Math.max(0,Math.round(r.bottom-box.bottom));
    line=drag===0
      ? `«Lo que busco» se ve sin arrastrar · su fila arranca a ${{Math.round(r.top-box.top)}} dp del borde de arriba`
      : `«Lo que busco» pide ${{drag}} dp de arrastre · ${{(drag/box.height).toFixed(1)}} pliegues`;
  }} else if(cur.v==="F" && canto.textContent.includes("busco")) {{
    line="«Lo que busco» se lee en el canto cosido, sin arrastrar · 0 dp de página";
  }} else {{
    line="nada marcado: la fila no se imprime";
  }}
  document.getElementById("readout").textContent=
    `pliegue ${{Math.round(box.height)}} dp · ${{vis.toFixed(2)}} tarjetas visibles de ${{cards.length}}\\n`+
    `alto total de la página ${{sc.scrollHeight}} dp · ${{line}}`;
}}
window.pick=pick;
window.metrics=()=>document.getElementById("readout").textContent;
window.scrollPhone=(px)=>{{slot().querySelector(".scroll").scrollTop=px; measure();}};
addEventListener("keydown",ev=>{{
  const iv=V.indexOf(cur.v), ie=E.indexOf(cur.e);
  if(ev.key==="ArrowRight")pick("v",V[(iv+1)%V.length]);
  if(ev.key==="ArrowLeft")pick("v",V[(iv-1+V.length)%V.length]);
  if(ev.key==="ArrowDown")pick("e",E[(ie+1)%E.length]);
  if(ev.key==="ArrowUp")pick("e",E[(ie-1+E.length)%E.length]);
}});
const hash=(location.hash.slice(1)||"").split("-");
if(V.includes(hash[0])) cur.v=hash[0];
if(E.includes(hash[1])) cur.e=hash[1];
pick("v",cur.v);
</script>
"""

path = f"{OUT}/maqueta.html"
open(path, "w").write(HTML)
print(f"{path} · {len(HTML)/1024:.0f} KB · {len(PHOTOS)} fotos · "
      f"{len(VARIANTS)}x{len(STATES)} pantallas")
