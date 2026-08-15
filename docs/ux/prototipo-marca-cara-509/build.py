#!/usr/bin/env python3
"""Maqueta de la marca de cara del #509, a dp real, con el giro de verdad y el de hoy de listón.

PROTOTIPO — se tira cuando el ticket se decida. Lo que sobrevive es el README.

Dos preguntas y dos ejes, porque el ticket son dos problemas distintos:

1. **Qué declara que una casilla está volteada** — cuatro marcas sobre tres láminas, y la de
   hoy (que no declara nada) de listón. Se toca el hueco y gira de verdad: 420 ms, la
   perspectiva de `COIN_CAMERA_DISTANCE`, la cara lejana desde su propio cero.
2. **Qué hace una casilla cuya otra cara no ha bajado** — tres respuestas sobre la misma
   lámina con las fotos del reverso apagadas, que es lo que se vio el 14 de agosto de 2026.

Las medidas salen de donde las saca la app —`AlbumPaper.kt`, `PlateScreen.kt`, `PlateSpacing`,
`YearTagMetrics`, `Theme.kt`— y están arriba, juntas, para que la maqueta no pueda mentir por
copia.

    python3 docs/ux/prototipo-marca-cara-509/extract.py
    python3 docs/ux/prototipo-marca-cara-509/build.py
    open /private/tmp/coindex-privado/marca-cara-509/maqueta.html
"""
import base64
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.abspath(f"{HERE}/../../..")
OUT = "/private/tmp/coindex-privado/marca-cara-509"

# ── el papel (Theme.kt:27-35) ───────────────────────────────────────────────
INK, MUTED, PAPER = "#2D3029", "#686A5D", "#EEE8D7"
DEEP, LINE, MOSS, RUST = "#DDD3BB", "#7D806C", "#495C49", "#8B553C"
CARD, HAIR = "rgba(255,252,242,0.58)", "#878577"

# ── la pantalla y la rejilla ────────────────────────────────────────────────
SCREEN, FOLD = 411, 914          # el Pixel 7 de las capturas del #296, 1 px CSS = 1 dp
MARGIN, GUTTER, PAD_V = 20, 16, 24
HOLE, RING = 104, 5              # AlbumHole en la lámina, HOLE_CARD_PADDING_DP
ROW_GAP, COLUMNS = 32, 3
CELL = (SCREEN - 2 * MARGIN - (COLUMNS - 1) * GUTTER) / COLUMNS
HEAD_GAP, CARD_PAD = 10, 14

# ── el giro (AlbumPaper.kt:43-58) ───────────────────────────────────────────
TURN_MS = 420                    # COIN_TURN_MILLIS
CAMERA = 12                      # COIN_CAMERA_DISTANCE, en múltiplos de la densidad
# Compose multiplica `cameraDistance` por la densidad y lo mide contra el tamaño en px del
# elemento; en CSS la perspectiva se mide en px de layout, así que la equivalencia es el mismo
# cociente: 12 densidades sobre un hueco de 104 dp son 12 x 104 / 8 px de perspectiva, con el
# 8 del `DefaultCameraDistance` de Compose que es la unidad en la que está dicho el 12.
PERSPECTIVE = round(CAMERA * HOLE / 8)

# ── la chapa del año (YearTagMetrics) y el nombre ───────────────────────────
TAG_W, TAG_H, TAG_TARGET = 48.3, 28, 48
NAME_SIZE, NAME_LINE, NAME_PAD = 17, 21, 6
STAMP_W, STAMP_H, RATIO_DROP, RATIO_SIZE = 84, 76, 14, 18

DATA = json.load(open(f"{OUT}/data.json"))
PLATES = {p["role"]: p for p in DATA["plates"]}
# La escena del reverso que no ha bajado usa la lámina del #302: 22 casillas del mismo tipo, así
# que la respuesta se ve repetida veintidós veces, que es como se va a vivir.
PLATES["sinfoto"] = dict(PLATES["datarun"], role="sinfoto")


def b64(path, mime):
    return f"data:{mime};base64," + base64.b64encode(open(path, "rb").read()).decode()


def font(name):
    return b64(f"{REPO}/app/src/main/res/font/{name}.ttf", "font/ttf")


def photo(key):
    path = f"{OUT}/fotos/{key}.img"
    head = open(path, "rb").read(4)
    return b64(path, "image/png" if head[:4] == b"\x89PNG" else "image/jpeg")


# ── la casilla ──────────────────────────────────────────────────────────────
def hole(cas, variant, missing_back, turned=False):
    """El hueco troquelado, con sus dos caras dentro de la capa que gira.

    `missing_back` es la escena del wifi pendiente: la foto de la otra cara existe en el
    catálogo pero no está en el disco del teléfono, así que atrás sólo hay silueta.
    """
    ghost = " ghost" if not cas["owned"] else ""
    tid = cas["tid"]
    back = (f'<i class="face back p{tid}-back"></i>' if not missing_back
            else '<i class="face back blank"><s class="sil"></s>'
                 + ('<u class="says">no ha bajado</u>' if variant == "dice" else "")
                 + "</i>")
    die = "" if cas["owned"] else '<div class="die"></div>'
    edge = '<b class="edge"></b>' if variant == "canto" else ""
    letter = ('<b class="letter"><s>A</s><u>R</u></b>' if variant == "letra" else "")
    return (f'<div class="hole{ghost}{" turned" if turned else ""}" data-turn="1">'
            f'<div class="card"></div><div class="wall"></div><div class="rule"></div>'
            f'<div class="turner"><i class="face front p{tid}-front"></i>{back}</div>'
            f'{edge}{die}{letter}</div>')


def casilla(cas, variant, missing_back, turned=False):
    """`PlateCell` desde el #473: hueco -> chapa del año -> nombre, y el nombre va al final."""
    year = f'<div class="tag">{cas["year"]}</div>' if cas["year"] else ""
    name = cas["label"] if cas["label"] != str(cas["year"]) else None
    printed = f'<div class="name">{name}</div>' if name else ""
    # La E pone la letra en el cartón, al lado de la chapa: la celda mide 113 dp y la chapa 48,3
    # centrada, así que a cada lado quedan 32 dp de hoja libre donde no hay que pintar sobre
    # ninguna fotografía.
    aside = ('<b class="aside"><s>A</s><u>R</u></b>' if variant == "carton" else "")
    return (f'<div class="cell{" turned" if turned else ""}">'
            f'{hole(cas, variant, missing_back, turned)}'
            f'<div class="tagbox">{aside}{year}</div>{printed}</div>')


def strip(plate, variants):
    """La tira: la misma casilla llena en reposo y vuelta, una fila por variante.

    El teléfono entero contesta «¿se ve de un vistazo?»; esta tira contesta «¿qué es lo que se
    ve?», que es la pregunta de al lado y no se puede juzgar a 104 dp de lejos.
    """
    cas = next((c for c in plate["casillas"] if c["owned"]), plate["casillas"][0])
    rows = []
    for key, title, _ in variants:
        trio = (casilla(cas, key, False)
                + casilla(cas, key, False, turned=True)
                + f'<div class="big">{casilla(cas, key, False, turned=True)}</div>')
        rows.append(f'<div class="striprow"><h3>{title}</h3>'
                    f'<div class="phone duo" data-v="{key}" '
                    f'data-side="{plate["printed"]}">{trio}</div></div>')
    return ('<div class="strip"><div class="striphead"><span>en reposo</span>'
            '<span>vuelta</span><span>vuelta, al doble</span></div>'
            f'{"".join(rows)}</div>')


def phone(plate, variant):
    missing_back = plate["role"] == "sinfoto"
    cells = "".join(casilla(c, variant, missing_back) for c in plate["casillas"])
    ratio = f'{plate["owned"]}/{plate["issued"]}'
    return f"""<div class="phone" data-v="{variant}" data-side="{plate["printed"]}">
<div class="status"><span>9:41</span><span>&#9646;&#9646;&#9646;</span></div>
<div class="masthead"><div class="top"><span class="wordmark">COINDEX</span>
<span class="back">&#8592; Volver</span></div>
<div class="sub">Lámina · {plate["title"]}</div></div>
<div class="plate">
  <div class="eyebrow">Catálogo curado</div>
  <div class="phead"><h1>{plate["title"]}</h1>
    <div class="ratio"><span>{ratio}</span></div></div>
  <div class="speccard"><div class="row"><span>Reposa en</span>
    <b>{"anverso" if plate["printed"] == "obverse" else "reverso"}</b></div></div>
  <div class="primary">Exportar la lámina</div>
  <div class="grid">{cells}</div>
</div></div>"""


# ── las variantes ───────────────────────────────────────────────────────────
MARKS = [
    ("hoy", "Hoy · v1.4.3", "el giro no declara nada: una casilla volteada y una en reposo se "
                            "dibujan igual, y la única pista es haber visto el movimiento"),
    ("troquel", "A · el troquel al revés", "la pared del corte invierte su luz —clara arriba, "
                                           "oscura abajo—: la casilla está del otro lado. Cero "
                                           "tinta y cero dp, con una pieza que ya está dibujada"),
    ("canto", "B · la moneda no reposa plana", "vuelta, la moneda queda sobre su canto —172° y "
                                               "no 180°— y el filo del metal asoma por un lado. "
                                               "Lenguaje de moneda, sin una letra"),
    ("letra", "C · la letra sobre el metal", "«A» o «R» en pequeño al pie del disco, como el "
                                             "punzón de una ceca. Es lo que pide el ticket, y "
                                             "es tinta sobre la fotografía"),
    ("chapa", "D · lo dice la chapa del año", "el rebaje de la chapa se invierte mientras la "
                                              "casilla está vuelta: la pieza que ya declara por "
                                              "forma declara también esto"),
    ("carton", "E · la letra en el cartón", "la misma letra que la C, pero en la hoja y no sobre "
                                            "el metal: al lado de la chapa quedan 32 dp de "
                                            "cartón libre, y la fotografía no se toca"),
]

ANSWERS = [
    ("muda", "1 · no voltea", "la casilla cuya otra cara no está en el teléfono no toma el "
                              "toque: no pasa nada, y no se dice por qué"),
    ("tiron", "2 · el tirón", "arranca el giro 14° y vuelve, como una puerta cerrada: el dedo "
                              "recibe respuesta y la cara no llega a faltar"),
    ("dice", "3 · voltea y lo dice", "gira hasta el final y la silueta lleva escrito que la "
                                     "fotografía no ha bajado todavía"),
]

SCENES = [
    ("tira", "La tira · una casilla de cerca", "la misma casilla llena en reposo, vuelta y al "
                                               "doble: aquí no se juzga si se ve, sino qué es "
                                               "lo que se ve", MARKS),
    ("datarun", "Date run · 1 Bolívar", "22 casillas del mismo tipo: la marca se repite 22 veces "
                                        "sobre la misma moneda", MARKS),
    ("obverso", "Reposa en anverso · 2 euros", "6 de los 74 catálogos declaran `obverse`: aquí "
                                               "una marca que nombra la cara tiene que decir "
                                               "«anverso» en reposo", MARKS),
    ("variada", "Tipos distintos · Paquillos", "cinco monedas que no se parecen, para ver la "
                                               "marca sobre metales y diseños distintos", MARKS),
    ("sinfoto", "Sin la otra cara · wifi pendiente", "la lámina del 1 Bolívar con el reverso sin "
                                                     "descargar, que es lo que se vio el 14 de "
                                                     "agosto", ANSWERS),
]

KEYS = sorted({f'{c["tid"]}-{side}'
               for p in DATA["plates"] for c in p["casillas"]
               for side in ("front", "back") if c[side]})
PHOTO_CSS = "".join(f'.p{key}{{background-image:url("{photo(key)}")}}' for key in KEYS)

CSS = f"""
@font-face {{ font-family: Bitter; src: url("{font('bitter_variable')}"); }}
@font-face {{ font-family: Barlow; font-weight: 600;
              src: url("{font('barlow_condensed_semibold')}"); }}
* {{ box-sizing: border-box; margin: 0; padding: 0; }}
body {{ background: #3a3a36; color: {PAPER}; font-family: Barlow, sans-serif;
        padding: 0 24px 40px; }}

/* ── la barra, arriba y sticky: abajo tapa el teléfono ─────────────────────── */
.bar {{ position: sticky; top: 0; z-index: 9; display: flex; flex-wrap: wrap; gap: 8px;
        align-items: center; padding: 12px 0; margin-bottom: 18px;
        background: #3a3a36; border-bottom: 1px solid #4a4a44; }}
.bar button {{ font: 600 12px Barlow; letter-spacing: .06em; text-transform: uppercase;
   padding: 7px 12px; border: 1px solid #5c5c54; background: transparent; color: {PAPER};
   cursor: pointer; }}
.bar button.on {{ background: {PAPER}; color: #23231f; }}
.bar .sep {{ width: 1px; height: 22px; background: #4a4a44; margin: 0 6px; }}
.bar .note {{ font: 400 12px Bitter; opacity: .6; margin-left: auto; }}

.wall {{ display: grid; grid-auto-flow: column; gap: 26px; justify-content: start;
         align-items: start; }}
.wall.solo {{ grid-auto-flow: row; }}
.panel {{ display: none; }}
.panel.on {{ display: block; }}
.panel h2 {{ font: 600 15px/1.3 Barlow; letter-spacing: .05em; text-transform: uppercase; }}
.panel p {{ font: 400 12px/1.45 Bitter; opacity: .62; margin: 4px 0 10px;
            max-width: {SCREEN}px; min-height: 66px; }}

/* ── el teléfono: 411 x 914 dp ─────────────────────────────────────────────── */
.phone {{ width: {SCREEN}px; height: {FOLD}px; background: {PAPER}; color: {INK};
          overflow: hidden; position: relative; }}
.status {{ display: flex; justify-content: space-between; padding: 6px 16px;
           font: 600 11px Barlow; color: {MUTED}; }}
.masthead .top {{ display: flex; justify-content: space-between; align-items: center;
                  padding: 14px 20px 6px; }}
.wordmark {{ font: 400 22px/26px Bitter; letter-spacing: .02em; }}
.back {{ font: 600 12px Barlow; font-feature-settings: 'smcp','tnum'; padding: 6px 14px;
         border: 1px solid {HAIR}; }}
.masthead .sub {{ font: 600 11px Barlow; font-feature-settings: 'smcp','tnum'; color: {MUTED};
                  padding: 0 20px 8px; }}
/* Opaco y por encima: la lámina se desplaza por debajo del marco, como en el teléfono. */
.masthead, .status {{ background: {PAPER}; position: relative; z-index: 2; }}
.masthead {{ border-bottom: 2px solid {INK}; }}

/* ── la lámina (PlateScreen.kt) ────────────────────────────────────────────── */
.plate {{ padding: {PAD_V}px {MARGIN}px; }}
.plate > * {{ margin-bottom: {HEAD_GAP}px; }}
.eyebrow {{ font: 600 11px/1 Barlow; font-feature-settings: 'smcp','tnum'; color: {RUST}; }}
.phead {{ display: flex; gap: 12px; align-items: flex-start; }}
.phead h1 {{ flex: 1; font: 400 26px/30px Bitter; }}
.ratio {{ width: {STAMP_W}px; height: {STAMP_H}px; flex: none; display: flex;
          justify-content: center; }}
.ratio span {{ font: 600 {RATIO_SIZE}px Barlow; font-feature-settings: 'smcp','tnum';
               color: {RUST}; padding-top: {RATIO_DROP}px; }}
.speccard {{ background: {CARD}; border: 1px solid {LINE}; padding: {CARD_PAD}px; }}
.speccard .row {{ display: flex; justify-content: space-between; gap: 12px;
                  align-items: center; }}
.speccard span {{ font: 600 10px/14px Barlow; font-feature-settings: 'smcp','tnum';
                  color: {MUTED}; }}
.speccard b {{ font: 400 14px/20px Bitter; text-align: right; }}
.primary {{ background: {INK}; color: {PAPER}; text-align: center; padding: 14px;
            font: 600 12px/1 Barlow; font-feature-settings: 'smcp','tnum';
            margin-bottom: {PAD_V}px; }}

/* ── la rejilla de casillas ────────────────────────────────────────────────── */
.grid {{ display: grid; grid-template-columns: repeat({COLUMNS}, {CELL}px);
         column-gap: {GUTTER}px; row-gap: {ROW_GAP}px; }}
.cell {{ display: flex; flex-direction: column; align-items: center; }}
.hole {{ position: relative; width: {HOLE}px; height: {HOLE}px; flex: none; cursor: pointer;
         perspective: {PERSPECTIVE}px; }}
.hole .card {{ position: absolute; inset: 0; border-radius: 50%; background: {CARD}; }}
/* La pared del corte: oscura arriba, donde se da sombra, y clara abajo, donde el troquel dejó
   canto fresco. Es un solo barrido y no dos medios arcos (AlbumPaper.kt:151-158). */
.hole .wall {{ position: absolute; inset: 0; border-radius: 50%; transition: transform {TURN_MS}ms;
  background: conic-gradient(from 90deg,
    rgba(255,255,255,0) 0deg, rgba(255,255,255,.85) 90deg, rgba(255,255,255,0) 176.4deg,
    rgba(45,48,41,0) 183.6deg, rgba(45,48,41,.22) 270deg, rgba(45,48,41,0) 356.4deg,
    rgba(255,255,255,0) 360deg);
  -webkit-mask: radial-gradient(circle at 50% 50%,
    transparent calc(50% - {RING}px), #000 calc(50% - {RING}px)); }}
.hole .rule {{ position: absolute; inset: 0; border-radius: 50%; border: 1px solid {HAIR}; }}

/* ── el giro: sólo la fotografía entra en la capa, el cartón no se mueve ───── */
.turner {{ position: absolute; inset: {RING}px; transform-style: preserve-3d;
           transition: transform {TURN_MS}ms cubic-bezier(.4,0,.2,1); }}
.hole.turned .turner {{ transform: rotateY(180deg); }}
/* El color de respaldo va en background-color y nunca en el atajo: `.face` (0,1,0) empataría
   con `.p123-front` y ganaría la última declarada, comiéndose la foto. */
.face {{ position: absolute; inset: 0; border-radius: 50%; backface-visibility: hidden;
         background-color: {DEEP}; background-size: cover; background-position: center; }}
/* La cara lejana se dibuja desde su propio cero para no salir con la leyenda espejada
   (AlbumPaper.kt:112-114). */
.face.back {{ transform: rotateY(180deg); }}
.face.blank {{ background-color: {DEEP}; }}
/* `Silhouette` (FieldGuide.kt:394-402): el 0x14646559 de Compose es AARRGGBB, o sea el
   #646559 al 8 %, y no el #146465 que leería un CSS de ocho dígitos. */
.face .sil {{ position: absolute; inset: 0; border-radius: 50%;
              background: rgba(100,101,89,.078); border: 1px solid rgba(45,48,41,.10); }}
.hole.ghost .face {{ opacity: .14; }}
.hole .die {{ position: absolute; inset: 6px; border-radius: 50%;
              border: 1px dashed rgba(45,48,41,.48); }}
/* El brillo del metal (#303): negro -> transparente -> blanco, dentro de la misma capa que
   gira, para que la luz pertenezca a la cara que se está viendo (#338). */
.face::after {{ content: ''; position: absolute; inset: 0; border-radius: 50%;
  mix-blend-mode: soft-light;
  background: linear-gradient(105deg, rgba(0,0,0,.55) 0%, rgba(0,0,0,0) 42%,
              rgba(255,255,255,0) 58%, rgba(255,255,255,.55) 100%); }}

/* ── A · el troquel al revés ───────────────────────────────────────────────── */
.phone[data-v="troquel"] .hole.turned .wall {{ transform: rotate(180deg); }}

/* ── B · la moneda descansa sobre su canto ─────────────────────────────────── */
/* Vuelta, la moneda queda recostada 1,5 dp contra la pared del troquel y por el otro lado
   asoma su canto. **No se escorza**: la primera vuelta la giró a 172° y lo que se leía no era
   una moneda de canto sino una moneda más pequeña. */
.phone[data-v="canto"] .hole.turned .turner {{
  transform: rotateY(180deg) translateX(1.5px); }}
.edge {{ position: absolute; inset: 0; border-radius: 50%; opacity: 0;
         transition: opacity {TURN_MS}ms;
         box-shadow: inset 2.5px 0 0 -0.5px rgba(255,252,242,.95),
                     inset 4px 0 5px -3px rgba(45,48,41,.30),
                     inset -3px 0 4px -2px rgba(45,48,41,.60); }}
.phone[data-v="canto"] .hole.turned .edge {{ opacity: 1; }}

/* ── C · la letra sobre el metal ───────────────────────────────────────────── */
.letter {{ position: absolute; left: 50%; bottom: 9px; transform: translateX(-50%);
   font: 600 11px/1 Barlow; font-feature-settings: 'smcp','tnum'; letter-spacing: .08em;
   color: rgba(45,48,41,.62); text-shadow: 0 1px 0 rgba(255,252,242,.55); }}
.letter u, .aside u {{ display: none; text-decoration: none; }}
.hole.turned .letter s, .cell.turned .aside s {{ display: none; }}
.hole.turned .letter u, .cell.turned .aside u {{ display: inline; }}
/* En una lámina que reposa en anverso, la letra de reposo es la A y la de vuelta la R. */
.phone[data-side="obverse"] :is(.letter,.aside) s::before {{ content: 'A'; }}
.phone[data-side="obverse"] :is(.letter,.aside) u::before {{ content: 'R'; }}
.phone[data-side="reverse"] :is(.letter,.aside) s::before {{ content: 'R'; }}
.phone[data-side="reverse"] :is(.letter,.aside) u::before {{ content: 'A'; }}
.letter s, .letter u, .aside s, .aside u {{ text-decoration: none; font-size: 0; }}
.letter s::before, .letter u::before {{ font-size: 11px; }}

/* ── el segundo eje: una marca que está siempre o sólo cuando se ha volteado ─ */
/* Con la letra siempre puesta, una lámina de 22 casillas lleva 22 letras en reposo — que es la
   prosa que el #302 podó. Con la letra sólo en la vuelta, la hoja en reposo queda como está y
   la marca se convierte en la excepción; a cambio deja de decir qué cara se mira cuando nadie
   ha tocado nada, que es la mitad del ticket. */
.wall[data-when="vuelta"] .cell:not(.turned) :is(.aside, .letter) {{ display: none; }}

/* ── E · la letra en el cartón, al lado de la chapa ────────────────────────── */
.aside {{ position: absolute; right: calc(50% + {TAG_W / 2 + 7}px);
   font: 600 11px/1 Barlow; font-feature-settings: 'smcp','tnum'; letter-spacing: .08em;
   color: rgba(45,48,41,.55); }}
.aside s::before, .aside u::before {{ font-size: 11px; }}
.tagbox {{ position: relative; }}

/* ── la chapa del año, y la D que la invierte ─────────────────────────────── */
.tagbox {{ height: {TAG_TARGET}px; display: flex; align-items: center; flex: none; }}
.tag {{ min-width: {TAG_W}px; height: {TAG_H}px; display: flex; align-items: center;
        justify-content: center; padding: 0 6px; background: rgba(221,211,187,.90);
        border-top: 2px solid rgba(45,48,41,.34);
        border-bottom: 1px solid rgba(255,255,255,.55);
        font: 600 12px Barlow; font-feature-settings: 'smcp','tnum'; color: {INK};
        transition: border-color {TURN_MS}ms, background {TURN_MS}ms; }}
.phone[data-v="chapa"] .cell.turned .tag {{ background: rgba(221,211,187,.62);
        border-top: 1px solid rgba(255,255,255,.55);
        border-bottom: 2px solid rgba(45,48,41,.34); }}
.name {{ font: 400 {NAME_SIZE}px/{NAME_LINE}px Bitter; text-align: center;
         padding: {NAME_PAD}px 0 0; }}

/* ── la escena sin la otra cara ───────────────────────────────────────────── */
/* La cara lejana ya vuelve a su propio cero (`.face.back` gira 180° dentro de un `.turner` que
   gira otros 180°), así que lo que escribe encima **no** hay que espejarlo. */
.says {{ position: absolute; left: 50%; top: 50%; transform: translate(-50%,-50%);
   text-decoration: none; white-space: nowrap; font: 600 10px Barlow;
   font-feature-settings: 'smcp','tnum'; color: {MUTED}; }}
.phone[data-v="muda"] .hole {{ cursor: default; }}
.hole.jolt .turner {{ transform: rotateY(14deg); }}

/* ── la tira: la misma casilla en reposo, vuelta y al doble ───────────────── */
.panel.wide p {{ max-width: 720px; }}
.strip {{ background: {PAPER}; color: {INK}; padding: 18px 20px 24px; width: 700px; }}
.striphead, .striprow {{ display: grid;
                         grid-template-columns: 150px {CELL}px {CELL}px {2 * HOLE + 16}px;
                         align-items: center; gap: 10px; }}
.striphead {{ font: 600 10px Barlow; font-feature-settings: 'smcp','tnum'; color: {MUTED};
              padding-bottom: 8px; border-bottom: 1px solid {DEEP}; }}
.striphead span:first-child {{ grid-column: 2; }}
.striprow {{ padding: 14px 0; border-bottom: 1px solid {DEEP}; }}
.striprow h3 {{ font: 400 13px/17px Bitter; }}
.duo {{ display: contents; }}
/* El doble es para mirar el hueco, no para leer el año dos veces: la chapa y el nombre se
   quedan fuera, salvo en la D, que es la variante que habla por la chapa. */
.duo .big {{ zoom: 2; }}
.duo .big .tagbox, .duo .big .name {{ display: none; }}
.phone[data-v="chapa"] .big .tagbox {{ display: flex; }}
{PHOTO_CSS}
"""

PANELS = "".join(
    f'<div class="panel" data-v="{key}" data-s="{scene}"><h2>{title}</h2>'
    f'<p>{blurb}</p>{phone(PLATES[scene], key)}</div>'
    for scene, _, _, variants in SCENES if scene != "tira"
    for key, title, blurb in variants
) + (
    # La tira enseña las cinco a la vez por construcción: un panel y no cinco.
    '<div class="panel wide" data-v="todas" data-s="tira"><h2>La tira · una casilla de cerca</h2>'
    '<p>La misma casilla llena en reposo, vuelta y al doble. Arriba se juzga si la marca se ve; '
    'aquí, qué es lo que se ve — y si sobrevive a que la moneda de al lado sea otra.</p>'
    f'{strip(PLATES["variada"], MARKS)}</div>'
)

JS = f"""
const params = new URLSearchParams(location.search);
// La tira no está aquí: enseña las cinco marcas a la vez, así que ningún botón la reduce.
const VARIANTS = {json.dumps({s: [k for k, _, _ in v] for s, _, _, v in SCENES if s != 'tira'})};
let scene = params.get('s') || 'tira';
let solo = params.get('v') || 'todas';
let when = params.get('w') || 'siempre';
function paint() {{
  document.querySelector('.wall').dataset.when = when;
  document.querySelectorAll('.panel').forEach(p => {{
    const mine = (VARIANTS[scene] || []).includes(solo);
    p.classList.toggle('on',
      p.dataset.s === scene && (solo === 'todas' || !mine || p.dataset.v === solo));
  }});
  document.querySelector('.wall').classList.toggle('solo',
    solo !== 'todas' && (VARIANTS[scene] || []).includes(solo));
  document.querySelectorAll('.bar button').forEach(b => {{
    if (b.dataset.s !== undefined) b.classList.toggle('on', b.dataset.s === scene);
    else if (b.dataset.w !== undefined) b.classList.toggle('on', b.dataset.w === when);
    else if (b.dataset.v !== undefined) b.classList.toggle('on', b.dataset.v === solo);
  }});
}}
// Los ganchos para capturar y medir sin depender de clics.
window.scrollPhone = px => document.querySelectorAll('.panel.on .plate')
  .forEach(p => p.style.transform = `translateY(${{-px}}px)`);
window.metrics = () => [...document.querySelectorAll('.panel.on .phone')].map(f => {{
  const hole = f.querySelector('.hole').getBoundingClientRect();
  return {{ variant: f.dataset.v, side: f.dataset.side, hole: hole.width,
           turned: f.querySelectorAll('.hole.turned').length,
           holes: f.querySelectorAll('.hole').length }};
}});
// El toque en el cuerpo del hueco: el mismo objetivo que reparte `PlateCell`. La chapa del año
// no gira nada, abre la ficha (#508), así que aquí no responde.
document.addEventListener('click', event => {{
  const hole = event.target.closest('.hole');
  if (!hole) return;
  const variant = hole.closest('.phone').dataset.v;
  const missing = hole.closest('.panel').dataset.s === 'sinfoto';
  if (missing && variant === 'muda') return;
  if (missing && variant === 'tiron') {{
    hole.classList.add('jolt');
    setTimeout(() => hole.classList.remove('jolt'), {TURN_MS} / 2);
    return;
  }}
  hole.classList.toggle('turned');
  hole.closest('.cell').classList.toggle('turned');
}});
// Tres vueltas y diecinueve no: el estado mezclado que un cartón no puede tener, y que es donde
// se juzga si la marca se lee de un vistazo (ADR 0026 §3).
function scatter() {{
  document.querySelectorAll('.panel.on .grid').forEach(grid => {{
    const cells = [...grid.children];
    cells.forEach(c => {{ c.classList.remove('turned');
                          c.querySelector('.hole').classList.remove('turned'); }});
    [2, 7, 12].forEach(i => {{
      const cell = cells[i % cells.length];
      cell.classList.add('turned');
      cell.querySelector('.hole').classList.add('turned');
    }});
  }});
}}
document.querySelectorAll('.bar button').forEach(b => b.addEventListener('click', () => {{
  if (b.dataset.act === 'scatter') return scatter();
  if (b.dataset.s !== undefined) scene = b.dataset.s;
  else if (b.dataset.w !== undefined) when = b.dataset.w;
  else solo = b.dataset.v;
  paint();
}}));
document.fonts.ready.then(paint);
paint();
"""

BAR = (
    "".join(f'<button data-s="{key}">{name.split(" · ")[0]}</button>'
            for key, name, _, _ in SCENES)
    + '<span class="sep"></span><button data-v="todas">Todas</button>'
    + "".join(f'<button data-v="{key}">{title.split(" · ")[0]}</button>'
              for key, title, _ in MARKS + ANSWERS)
    + '<span class="sep"></span><button data-w="siempre">La letra siempre</button>'
    + '<button data-w="vuelta">Sólo al voltear</button>'
    + '<span class="sep"></span><button data-act="scatter">Voltear tres</button>'
    + '<span class="note">toca un hueco para girarlo · 411 × 914 dp</span>'
)

HTML = f"""<!doctype html>
<html lang="es"><head><meta charset="utf-8">
<title>Prototipo #509 · qué cara mira una casilla volteada</title>
<style>{CSS}</style></head>
<body>
<div class="bar">{BAR}</div>
<div class="wall">{PANELS}</div>
<script>{JS}</script>
</body></html>
"""

out = f"{OUT}/maqueta.html"
open(out, "w").write(HTML)
print(f"{out}  ({len(HTML) / 1024:.0f} kB) · perspectiva {PERSPECTIVE} px")
