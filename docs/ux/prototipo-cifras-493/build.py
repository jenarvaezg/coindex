#!/usr/bin/env python3
"""Maqueta de las ocho cabeceras del #493, a dp real y con la de hoy de listón.

PROTOTIPO — se tira cuando el ticket se decida. Lo que sobrevive es el README.

Ocho variantes por lámina y cinco láminas: la holgada (5,5x entre las dos cifras), la que
colisiona (1,4x), una de siete huecos, una por encima del umbral y una cerrada, que es la
tercera pregunta del ticket. Un solo HTML autocontenido: las dos fuentes del APK y las fotos
del catálogo van en base64, una clase CSS por tipo para no embeberlas por casilla.

Las medidas salen de donde las saca la app —`PlateScreen.kt`, `PlateSpacing`, `YearTagMetrics`,
`CompletionStamp.kt`, `Theme.kt`— y están arriba, juntas, para que la maqueta no pueda mentir
por copia.

**Sale al anexo privado y no al repo** (`dinero-fuera-del-repo-publico`): la maqueta lleva los
importes de la colección del padre.

    python3 docs/ux/prototipo-cifras-493/extract.py
    python3 docs/ux/prototipo-cifras-493/build.py
    open /private/tmp/coindex-privado/cifras-493/maqueta.html
"""
import base64
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.abspath(f"{HERE}/../../..")
OUT = "/private/tmp/coindex-privado/cifras-493"

# ── el papel (Theme.kt:27-35) ───────────────────────────────────────────────
INK, MUTED, PAPER = "#2D3029", "#686A5D", "#EEE8D7"
DEEP, LINE, MOSS, RUST = "#DDD3BB", "#7D806C", "#495C49", "#8B553C"
CARD, HAIR = "rgba(255,252,242,0.58)", "#878577"

# ── la pantalla y la rejilla ────────────────────────────────────────────────
SCREEN, FOLD = 411, 914          # el Pixel 7 de las capturas del #296, 1 px CSS = 1 dp
MARGIN = 20                      # PLATE_MARGIN
GUTTER = 16                      # PlateMetrics.gutter
PAD_V = 24                       # contentPadding vertical del LazyVerticalGrid
HOLE, RING = 104, 5              # AlbumHole en la lámina, HOLE_CARD_PADDING_DP
ROW_GAP = 32                     # PlateSpacing.rowGap
COLUMNS = 3                      # plateColumns(371 dp) con Adaptive(104)
CELL = (SCREEN - 2 * MARGIN - (COLUMNS - 1) * GUTTER) / COLUMNS
HEAD_GAP = 10                    # Arrangement.spacedBy del item de cabecera
CARD_PAD = 14                    # PlateMetrics.cardPadding

# ── la chapa del año (YearTagMetrics) y el nombre ───────────────────────────
TAG_W, TAG_H, TAG_TARGET = 48.3, 28, 48
NAME_SIZE, NAME_LINE, NAME_PAD = 17, 21, 6   # titleMedium + PlateSpacing.namePadding

# ── el sello y su cociente (CompletionStamp.kt:179-194) ─────────────────────
STAMP_W, STAMP_H, RATIO_DROP, RATIO_SIZE, STAMP_TILT = 84, 76, 14, 18, 5.5

MONEY_CRITERION = "al mayor de tres precios"   # FiguresLabels.kt:51
# Un hueco no tiene «lo que pagaste», así que no son tres precios; y se tasa en `unc`,
# que en la app se dice «sin circular» (ADR 0028 §8, `uncirculatedSentence`).
HOLE_CRITERION = "en sin circular"

DATA = json.load(open(f"{OUT}/data.json"))
PLATES = {p["role"]: p for p in DATA["plates"]}


def b64(path, mime):
    return f"data:{mime};base64," + base64.b64encode(open(path, "rb").read()).decode()


def font(name):
    return b64(f"{REPO}/app/src/main/res/font/{name}.ttf", "font/ttf")


def photo(tid):
    path = f"{OUT}/fotos/{tid}.img"
    head = open(path, "rb").read(4)
    mime = "image/png" if head[:4] == b"\x89PNG" else "image/jpeg"
    return b64(path, mime)


def eur(amount):
    """`eurosLabel`: sin decimales y con el punto de millar español."""
    return f"{amount:,.0f} €".replace(",", ".")


# ── la casilla ──────────────────────────────────────────────────────────────
def hole(cas, chip=None):
    """El hueco troquelado. `chip` es el precio pegado dentro, que sólo pone la variante C."""
    ghost = "" if cas["owned"] else " ghost"
    # El precio es lo que cuesta el hueco, así que sólo lo lleva el hueco: una casilla llena no
    # tiene coste, tiene valor, y ése es el de la cabecera.
    empty_priced = chip and cas["floor"] and not cas["owned"]
    price = f'<b class="chip">{eur(cas["floor"])}</b>' if empty_priced else ""
    return (f'<div class="hole{ghost}"><div class="card"></div><div class="wall"></div>'
            f'<div class="rule"></div><i class="p{cas["tid"]}"></i>'
            f'{"" if cas["owned"] else chr(60) + "div class=\x27die\x27></div>"}{price}</div>')


def casilla(cas, chip=False):
    """`PlateCell` desde el #473: hueco -> chapa del año -> nombre, y el nombre va al final."""
    year = f'<div class="tag">{cas["year"]}</div>' if cas["year"] else ""
    name = cas["label"] if cas["label"] != str(cas["year"]) else None
    printed = f'<div class="name">{name}</div>' if name else ""
    return (f'<div class="cell">{hole(cas, chip)}'
            f'<div class="tagbox">{year}</div>{printed}</div>')


# ── la cabecera, que es lo que se está eligiendo ────────────────────────────
def money(text, cls="money"):
    return f'<div class="{cls}">{text}</div>'


def head_money(plate, variant):
    """Las cifras de dinero de la cabecera, una versión por variante."""
    inside, cost = eur(plate["value"]), plate["cost"] and eur(plate["cost"])
    if variant == "hoy":
        return money(f"{inside} · {MONEY_CRITERION}")
    if variant == "a":
        rows = [f'<div class="line"><i>dentro</i><b>{inside}</b></div>']
        if cost:
            rows.append(f'<div class="line"><i>cerrarla</i><b>{cost}</b></div>')
        return (f'<div class="pair">{"".join(rows)}'
                f'<div class="crit">{MONEY_CRITERION}</div></div>')
    if variant == "b":
        return money(f"{inside} · {MONEY_CRITERION}")
    if variant == "c":
        return money(f"{inside} · {MONEY_CRITERION}")
    if variant == "d":
        if cost:
            return money(f'<i>cerrarla</i> {cost} · {MONEY_CRITERION}', "money one")
        return money(f"{inside} · {MONEY_CRITERION}")
    if variant == "g":
        # La E con el criterio de cada cifra, que no es el mismo: un hueco no tiene «lo que
        # pagaste», así que sus precios son dos y no tres, y se tasa en `unc` (ADR 0028 §8). El
        # criterio viaja con su importe, que es lo que pedía el #408.
        rows = [f'<div class="line"><i>Valor actual:</i><b>{inside}</b>'
                f'<u>· {MONEY_CRITERION}</u></div>']
        if cost:
            rows.append(f'<div class="line"><i>Coste de cerrar:</i><b>{cost}</b>'
                        f'<u>· {HOLE_CRITERION}</u></div>')
        return f'<div class="pair said tailed">{"".join(rows)}</div>'
    if variant in ("e", "f"):
        # Lo que pidió Jose, con las dos etiquetas dichas enteras y el coste sólo si falta algo.
        # La F es la misma menos la repetición: con un solo hueco el sello del hueco ya dice el
        # coste, así que el renglón no lo dice otra vez.
        rows = [f'<div class="line"><i>Valor actual:</i><b>{inside}</b></div>']
        say_cost = cost and (variant == "e" or plate["missing"] > 1)
        if say_cost:
            rows.append(f'<div class="line"><i>Coste de cerrar:</i><b>{cost}</b></div>')
        return (f'<div class="pair said">{"".join(rows)}'
                f'<div class="crit">{MONEY_CRITERION}</div></div>')
    raise AssertionError(variant)


def spec(plate, variant):
    """`SpecificationCard` con `plateEntriesBesideRatio`: el cociente ya está sobre el título."""
    rows = []
    if plate["weight_millioz"]:
        whole, frac = divmod(plate["weight_millioz"], 1000)
        ounces = f"{whole} oz" if not frac else f"{whole},{str(frac).rjust(3, '0').rstrip('0')} oz"
        rows.append(("Peso", ounces))
    rows.append(("Actualizado", plate["updated"]))
    # La B es la única que baja el coste de cerrar a la ficha, que es donde viven los datos
    # de la lámina: la jerarquía la pone la superficie y no el tamaño de la letra.
    if variant == "b" and plate["cost"]:
        rows.insert(0, ("Cerrarla", eur(plate["cost"])))
    cells = "".join(
        f'<div class="row"><span>{label}</span><b>{value}</b></div>' for label, value in rows
    )
    return f'<div class="speccard">{cells}</div>'


def phone(plate, variant):
    complete = plate["missing"] == 0
    stamp = '<div class="stamp"><b>COMPLETA</b></div>' if complete else ""
    ratio = f'{plate["owned"]}/{plate["issued"]}'
    # Un sello sólo puede decir un precio que el pase haya pedido, y el pase no pregunta por los
    # huecos de una lámina por encima del umbral (ADR 0028 §1): sin coste, sin sellos.
    chip = variant in ("c", "e", "f", "g") and plate["cost"] is not None
    cells = "".join(casilla(c, chip) for c in plate["casillas"])
    return f"""<div class="phone">
<div class="status"><span>9:41</span><span>▮▮▮</span></div>
<div class="masthead"><div class="top"><span class="wordmark">COINDEX</span>
<span class="back">&#8592; Volver</span></div>
<div class="sub">Lámina · {plate["title"]}</div></div>
<div class="plate">
  <div class="eyebrow">Catálogo curado</div>
  <div class="phead"><h1>{plate["title"]}</h1>
    <div class="ratio{" done" if complete else ""}"><span>{ratio}</span>{stamp}</div></div>
  {head_money(plate, variant)}
  {spec(plate, variant)}
  <div class="primary">Exportar la lámina</div>
  <div class="link">Fuente en Numista &#8599;</div>
  <div class="grid">{cells}</div>
</div></div>"""


VARIANTS = [
    ("hoy", "Hoy · v1.2.20", "una cifra: el valor de lo que hay dentro. El coste de cerrar no "
                             "está en pantalla todavía"),
    ("a", "A · dos renglones hermanos", "las dos en la cabecera, cada una con su palabra "
                                        "delante y un solo criterio para las dos"),
    ("b", "B · una manda, la otra baja a la ficha", "la cabecera sigue siendo el valor; cerrar "
                                                    "es un dato de la lámina, en la ficha"),
    ("c", "C · el precio cuelga del hueco", "la cabecera no crece: cada hueco lleva su propio "
                                            "precio dentro, y el grano es la casilla"),
    ("d", "D · una sola cifra, y el estado elige", "mientras falte algo, sólo el coste de "
                                                   "cerrar; cerrada, sólo el valor"),
    ("e", "E · las dos dichas, y el sello en cada hueco", "«Valor actual» y «Coste de cerrar» "
                                                          "con su etiqueta entera, más el precio "
                                                          "dentro de cada hueco vacío"),
    ("f", "F · la E sin decir dos veces lo mismo", "igual que la E, pero el renglón del coste "
                                                   "sólo cuando hay más de un hueco: con uno, "
                                                   "el sello ya lo dice"),
    ("g", "G · la E con el criterio de cada cifra", "los dos criterios no son el mismo: un hueco "
                                                    "no tiene «lo que pagaste» y se tasa en sin "
                                                    "circular (ADR 0028 §8)"),
]

# Las que se miran después de elegir: el listón, la E que eligió Jose y la G, que es la E con la
# procedencia corregida. La A, la B, la C sola, la D y la F quedan en el código como registro.
CHOSEN = ("hoy", "e", "g")

SCENES = [
    ("holgada", "Holgada · 100 bolívares", "3/4, un hueco · la de dentro es 5,5x la de cerrar"),
    ("colision", "Colisión · 20 escudos", "2/3, un hueco · la de dentro es 1,4x la de cerrar"),
    ("varios", "Varios huecos · Queen's Beasts", "4/11, siete huecos · cerrar es 1,75x lo que "
                                                 "hay dentro"),
    ("umbral", "Sobre el umbral · Kangaroo", "1/12, once huecos: uno más que el umbral, así que "
                                             "no hay precio que sellar"),
    ("cerrada", "Cerrada · Exposición de Arte", "3/3 · ya no hay hueco que costar"),
]

PHOTO_CSS = "".join(
    f'.p{tid}{{background-image:url("{photo(tid)}")}}'
    for tid in sorted({c["tid"] for p in DATA["plates"] for c in p["casillas"] if c["tid"]})
)

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
            max-width: {SCREEN}px; min-height: 52px; }}

/* ── el teléfono: 411 x 914 dp ─────────────────────────────────────────────── */
.phone {{ width: {SCREEN}px; height: {FOLD}px; background: {PAPER}; color: {INK};
          overflow: hidden; position: relative; }}
.status {{ display: flex; justify-content: space-between; padding: 6px 16px;
           font: 600 11px Barlow; color: {MUTED}; }}

/* ── el masthead (CoindexApp.Masthead) ─────────────────────────────────────── */
.masthead .top {{ display: flex; justify-content: space-between; align-items: center;
                  padding: 14px 20px 6px; }}
.wordmark {{ font: 400 22px/26px Bitter; letter-spacing: .02em; }}
.back {{ font: 600 12px Barlow; font-feature-settings: 'smcp','tnum'; padding: 6px 14px;
         border: 1px solid {HAIR}; }}
.masthead .sub {{ font: 600 11px Barlow; font-feature-settings: 'smcp','tnum'; color: {MUTED};
                  padding: 0 20px 8px; }}
.masthead {{ border-bottom: 2px solid {INK}; }}

/* ── la lámina (PlateScreen.kt) ────────────────────────────────────────────── */
.plate {{ padding: {PAD_V}px {MARGIN}px; }}
.plate > * {{ margin-bottom: {HEAD_GAP}px; }}
.eyebrow {{ font: 600 11px/1 Barlow; font-feature-settings: 'smcp','tnum'; color: {RUST}; }}
.phead {{ display: flex; gap: 12px; align-items: flex-start; }}
.phead h1 {{ flex: 1; font: 400 26px/30px Bitter; }}
.ratio {{ width: {STAMP_W}px; height: {STAMP_H}px; flex: none; position: relative;
          display: flex; justify-content: center; }}
.ratio span {{ font: 600 {RATIO_SIZE}px Barlow; font-feature-settings: 'smcp','tnum';
               color: {RUST}; padding-top: {RATIO_DROP}px; }}
.ratio.done span {{ color: rgba(139,85,60,.72); }}
.stamp {{ position: absolute; inset: 0; border: 2px solid rgba(139,85,60,.82);
          transform: rotate({STAMP_TILT}deg); mix-blend-mode: multiply; }}
.stamp b {{ position: absolute; inset: 2px; border: 1px solid rgba(139,85,60,.72);
            display: block; text-align: center; padding-top: 7px;
            font: 600 10px Barlow; letter-spacing: .8px; color: rgba(139,85,60,.82); }}

/* ── el dinero: labelLarge en rust, que es lo que hay hoy ──────────────────── */
.money {{ font: 600 12px/1 Barlow; font-feature-settings: 'smcp','tnum'; color: {RUST}; }}
.money i {{ font-style: normal; }}
/* A: dos renglones del mismo peso, distinguidos sólo por la palabra de delante */
.pair .line {{ display: flex; gap: 8px; align-items: baseline; padding-bottom: 4px; }}
.pair i {{ font: 600 11px Barlow; font-feature-settings: 'smcp','tnum'; font-style: normal;
           color: {MUTED}; min-width: 62px; }}
.pair b {{ font: 600 14px Barlow; font-feature-settings: 'smcp','tnum'; color: {RUST};
           font-weight: 600; }}
.pair .crit {{ font: 600 10px/1 Barlow; font-feature-settings: 'smcp','tnum'; color: {MUTED};
               padding-top: 2px; }}
/* E y F: la etiqueta dicha entera, con sus dos puntos, y el importe pegado a ella */
.pair.said i {{ min-width: 0; font-size: 12px; }}
.pair.said .line {{ gap: 6px; }}
/* G: cada cifra con su criterio, porque los dos criterios no son el mismo */
.pair.tailed b {{ font-size: 12px; }}
.pair.tailed u {{ text-decoration: none; font: 600 10px Barlow;
                  font-feature-settings: 'smcp','tnum'; color: {MUTED}; }}

/* ── la ficha, el botón y el enlace ───────────────────────────────────────── */
.speccard {{ background: {CARD}; border: 1px solid {LINE}; padding: {CARD_PAD}px; }}
.speccard .row {{ display: flex; justify-content: space-between; gap: 12px;
                  align-items: center; }}
.speccard .row + .row {{ border-top: 1px solid {DEEP}; margin-top: 6px; padding-top: 6px; }}
.speccard span {{ font: 600 10px/14px Barlow; font-feature-settings: 'smcp','tnum';
                  color: {MUTED}; }}
.speccard b {{ font: 400 14px/20px Bitter; text-align: right; }}
.primary {{ background: {INK}; color: {PAPER}; text-align: center; padding: 14px;
            font: 600 12px/1 Barlow; font-feature-settings: 'smcp','tnum'; }}
.link {{ font: 400 14px/20px Bitter; color: {MOSS}; text-decoration: underline;
         margin-bottom: {PAD_V}px; }}

/* ── la rejilla de casillas ────────────────────────────────────────────────── */
.grid {{ display: grid; grid-template-columns: repeat({COLUMNS}, {CELL}px);
         column-gap: {GUTTER}px; row-gap: {ROW_GAP}px; }}
.cell {{ display: flex; flex-direction: column; align-items: center; }}
.hole {{ position: relative; width: {HOLE}px; height: {HOLE}px; flex: none; }}
.hole .card {{ position: absolute; inset: 0; border-radius: 50%; background: {CARD}; }}
.hole .wall {{ position: absolute; inset: 0; border-radius: 50%;
  background: conic-gradient(from 90deg,
    rgba(255,255,255,0) 0deg, rgba(255,255,255,.85) 90deg, rgba(255,255,255,0) 176.4deg,
    rgba(45,48,41,0) 183.6deg, rgba(45,48,41,.22) 270deg, rgba(45,48,41,0) 356.4deg,
    rgba(255,255,255,0) 360deg);
  -webkit-mask: radial-gradient(circle at 50% 50%,
    transparent calc(50% - {RING}px), #000 calc(50% - {RING}px)); }}
.hole .rule {{ position: absolute; inset: 0; border-radius: 50%; border: 1px solid {HAIR}; }}
/* El color de respaldo va en background-color y nunca en el atajo: `.hole i` (0,1,1) le gana
   el background-image a `.p123` (0,1,0) y se comería la foto. */
.hole i {{ position: absolute; inset: {RING}px; border-radius: 50%; background-color: {DEEP};
           background-size: cover; background-position: center; }}
.hole.ghost i {{ opacity: .14; }}
.hole .die {{ position: absolute; inset: 6px; border-radius: 50%;
              border: 1px dashed rgba(45,48,41,.48); }}
/* C: el precio dentro del hueco, como el papelito de un bolsillo vacío */
.hole .chip {{ position: absolute; left: 50%; top: 50%; transform: translate(-50%,-50%);
   font: 600 13px Barlow; font-feature-settings: 'smcp','tnum'; color: {RUST};
   background: rgba(238,232,215,.92); border: 1px solid rgba(139,85,60,.5);
   padding: 3px 7px; white-space: nowrap; }}
.tagbox {{ height: {TAG_TARGET}px; display: flex; align-items: center; flex: none; }}
.tag {{ min-width: {TAG_W}px; height: {TAG_H}px; display: flex; align-items: center;
        justify-content: center; padding: 0 6px; background: rgba(221,211,187,.90);
        border-top: 2px solid rgba(45,48,41,.34);
        border-bottom: 1px solid rgba(255,255,255,.55);
        font: 600 12px Barlow; font-feature-settings: 'smcp','tnum'; color: {INK}; }}
.name {{ font: 400 {NAME_SIZE}px/{NAME_LINE}px Bitter; text-align: center;
         padding: {NAME_PAD}px 0 0; }}
{PHOTO_CSS}
"""

PANELS = "".join(
    f'<div class="panel" data-v="{key}" data-s="{scene}"><h2>{title}</h2>'
    f'<p>{blurb}</p>{phone(PLATES[scene], key)}</div>'
    for scene, _, _ in SCENES
    for key, title, blurb in VARIANTS
)

JS = f"""
const CHOSEN = {list(CHOSEN)};
const params = new URLSearchParams(location.search);
let scene = params.get('s') || 'holgada';
let solo = params.get('v') || 'elegidas';
function shown(v) {{
  if (solo === 'todas') return true;
  if (solo === 'elegidas') return CHOSEN.includes(v);
  return v === solo;
}}
function paint() {{
  document.querySelectorAll('.panel').forEach(p => {{
    p.classList.toggle('on', p.dataset.s === scene && shown(p.dataset.v));
  }});
  document.querySelector('.wall').classList.toggle('solo',
    solo !== 'todas' && solo !== 'elegidas');
  document.querySelectorAll('.bar button').forEach(b => {{
    b.classList.toggle('on', b.dataset.s !== undefined
      ? b.dataset.s === scene : b.dataset.v === solo);
  }});
}}
document.querySelectorAll('.bar button').forEach(b => b.addEventListener('click', () => {{
  if (b.dataset.s !== undefined) scene = b.dataset.s; else solo = b.dataset.v;
  paint();
}}));
document.fonts.ready.then(paint);
paint();
"""

SPOT = DATA["spot"]
BAR = (
    "".join(f'<button data-s="{key}">{name}</button>' for key, name, _ in SCENES)
    + '<span class="sep"></span><button data-v="elegidas">Las elegidas</button>'
    + '<button data-v="todas">Las ocho</button>'
    + "".join(f'<button data-v="{key}">{title.split(" · ")[0]}</button>'
              for key, title, _ in VARIANTS)
    + f'<span class="note">suelo de la plata · {SPOT["eur_oz"]:.2f} €/oz · '
      f'{SPOT["at"][:10]}</span>'
)

HTML = f"""<!doctype html>
<html lang="es"><head><meta charset="utf-8">
<title>Prototipo #493 · dos cifras de dinero en la cabecera de una lámina</title>
<style>{CSS}</style></head>
<body>
<div class="bar">{BAR}</div>
<div class="wall">{PANELS}</div>
<script>{JS}</script>
</body></html>
"""

out = f"{OUT}/maqueta.html"
open(out, "w").write(HTML)
print(f"{out}  ({len(HTML) / 1024:.0f} kB)")
