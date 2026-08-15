#!/usr/bin/env python3
"""Maqueta del #519: la línea que dice el mercado ausente, en «Las cifras» y en la lámina.

PROTOTIPO — se tira cuando el ticket se decida. Lo que sobrevive es el README.

Siete variantes x seis motivos x cuatro escenas, a dp real y con la de hoy de listón. El motivo
importa porque hoy sólo Ajustes lo distingue, y la escena importa porque **la misma ausencia se
dice distinta según cuántas cifras iban a faltar**: la lámina cerrada y la que está por encima del
umbral de ADR 0028 §1 nunca tuvieron «Coste de cerrar» que perder.

Las medidas salen de donde las saca la app —`Theme.kt`, `FiguresScreen.kt`, `PlateScreen.kt`,
`ReferentLadder.kt`, `AlbumChrome.kt`— y están arriba, juntas, para que la maqueta no pueda mentir
por copia. Las frases de Ajustes son literales de `FiguresLabels.valuationLabel`.

**Sale al anexo privado y no al repo** (`dinero-fuera-del-repo-publico`): el estado de control lleva
los importes de la colección del padre.

    python3 docs/ux/prototipo-mercado-ausente-519/extract.py
    python3 docs/ux/prototipo-mercado-ausente-519/build.py
    open /private/tmp/coindex-privado/mercado-ausente-519/maqueta.html
"""
import base64
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.abspath(f"{HERE}/../../..")
OUT = "/private/tmp/coindex-privado/mercado-ausente-519"

# ── el papel (Theme.kt:27-36) ───────────────────────────────────────────────
INK, MUTED, PAPER = "#2D3029", "#686A5D", "#EEE8D7"
DEEP, LINE, MOSS, RUST = "#DDD3BB", "#7D806C", "#495C49", "#8B553C"
CARD, HAIR = "rgba(255,252,242,0.58)", "#878577"

# ── la pantalla ─────────────────────────────────────────────────────────────
SCREEN, FOLD = 411, 914          # el Pixel 7 de las capturas del #296, 1 px CSS = 1 dp

# ── «Las cifras» (FiguresScreen.kt:99-105, AlbumChrome.kt:38) ───────────────
CHROME_H = 54
FIG_MARGIN, FIG_TOP, FIG_BLOCKS = 20, 18, 26   # contentPadding y spacedBy de la LazyColumn
HEAD_GAP, BLOCK_GAP, BLOCK_RULE = 6, 6, 14     # RootHeading, Block y el filete de Block

# ── la lámina (PlateScreen.kt:322-347, PlateMetrics) ────────────────────────
PLATE_MARGIN, PLATE_TOP = 20, 24
GUTTER, ROW_GAP, LEAD_GAP = 16, 32, 10
HOLE, RING, CARD_PAD = 104, 5, 14
MONEY_GAP = 4                    # PLATE_MONEY_LINE_GAP
COLUMNS = 3
CELL = (SCREEN - 2 * PLATE_MARGIN - (COLUMNS - 1) * GUTTER) / COLUMNS
TAG_W, TAG_H = 48.3, 28          # YearTagMetrics
NAME_SIZE, NAME_LINE, NAME_PAD = 17, 21, 6
STAMP_W, STAMP_H, RATIO_DROP, RATIO_SIZE = 84, 76, 14, 18

# ── la escalera (ReferentLadder.kt:28-165, Silhouettes.kt:176) ──────────────
LADDER_GAP, SILHOUETTE_H, MARK_DROP, RUNG_TOP, LABEL_W = 4, 26, 22, 6, 58

MONEY_CRITERION = "al mayor de tres precios"   # FiguresLabels.kt:51
HOLE_CRITERION = "en sin circular"             # FiguresLabels.kt:96
VALUE_LABEL, COST_LABEL = "Valor actual", "Coste de cerrar"

DATA = json.load(open(f"{OUT}/data.json"))
PLATES = {p["role"]: p for p in DATA["plates"]}

# ── los seis motivos, con la frase literal que Ajustes ya dice hoy ──────────
# `FiguresLabels.valuationLabel` (FiguresLabels.kt:163-179). Van aquí enteras porque lo que el
# ticket propone es **mudarlas**, y para juzgar la mudanza hay que tener delante lo que dicen.
MOTIVOS = [
    ("red", "Sin red", "Esperan a que haya red."),
    ("presupuesto", "Presupuesto agotado",
     "Se acabó el presupuesto de llamadas de este mes: seguirán el mes que viene."),
    ("credenciales", "Sin credenciales", "Faltan las credenciales de Numista."),
    ("camino", "En camino", "Se traen solos con la app abierta."),
    ("sync", "Sincronizando", "Esperan a que termine el sincronizado."),
    ("mercado", "Con mercado fresco", None),
]
TRANSITORIOS = {"camino", "sync"}   # los dos que se arreglan solos sin que nadie haga nada

# ── lo que cada variante dice en «Las cifras» ───────────────────────────────
# La A y la B llevan una frase por motivo, que es la lectura literal del ticket: la explicación de
# Ajustes, reescrita sin el recuento de emisiones porque el recuento es de Ajustes.
CIFRAS_MOTIVO = {
    "red": "El valor espera a que haya red.",
    "presupuesto": "El valor espera al mes que viene: se acabó el presupuesto de llamadas.",
    "credenciales": "El valor espera a las credenciales de Numista.",
    "camino": "El valor se está trayendo con la app abierta.",
    "sync": "El valor espera a que termine el sincronizado.",
}
CIFRAS_CORTA = "El valor todavía no está."
CIFRAS_FIJA = "El valor llega cuando llegue el mercado."
PUERTA = "Por qué, en Ajustes"


def under_eyebrow(sentence):
    """Debajo de «EL VALOR» el sujeto ya está dicho, así que la frase no lo repite.

    Sólo la B lo lleva: es la única que no tiene bloque, y sin el eyebrow encima la frase se queda
    sin sujeto. Lo demás sería decir «El valor» dos veces en dos renglones seguidos.
    """
    rest = sentence.removeprefix("El valor ")
    return rest[0].upper() + rest[1:]

# ── y lo que dice en una lámina ─────────────────────────────────────────────
# El sujeto cambia con la lámina: una cerrada y una por encima del umbral de ADR 0028 §1 nunca
# tuvieron «Coste de cerrar», así que prometerlo sería inventar una cifra que no va a llegar.
LAMINA_MOTIVO = {
    "red": "esperan a que haya red",
    "presupuesto": "esperan al mes que viene: se acabó el presupuesto",
    "credenciales": "esperan a las credenciales de Numista",
    "camino": "se están trayendo con la app abierta",
    "sync": "esperan a que termine el sincronizado",
}
LAMINA_FIJA = "El dinero llega cuando llegue el mercado."


def subject(plate):
    """Cuántas cifras se han quedado sin decir, que es lo que la frase de la lámina nombra."""
    return "El valor y el coste" if plate["cost_possible"] else "El valor"


def lamina_motivo(plate, motivo):
    head = subject(plate)
    verb = LAMINA_MOTIVO[motivo]
    if head == "El valor":
        verb = verb.replace("esperan", "espera").replace("se están", "se está")
    return f"{head} {verb}."


def lamina_corta(plate):
    head = subject(plate)
    return f"{head} todavía no {'están' if head != 'El valor' else 'está'}."


def b64(path, mime):
    return f"data:{mime};base64," + base64.b64encode(open(path, "rb").read()).decode()


def font(name):
    return b64(f"{REPO}/app/src/main/res/font/{name}.ttf", "font/ttf")


def photo(tid):
    path = f"{OUT}/fotos/{tid}.img"
    head = open(path, "rb").read(4)
    return b64(path, "image/png" if head[:4] == b"\x89PNG" else "image/jpeg")


def eur(amount):
    """`eurosLabel`: sin decimales y con el punto de millar español."""
    return f"{amount:,.0f} €".replace(",", ".")


def decimal(value, places):
    return f"{value:,.{places}f}".replace(",", "~").replace(".", ",").replace("~", ".")


# ── «Las cifras» ────────────────────────────────────────────────────────────
def money_block():
    """El bloque del dinero cuando lo hay, que es el estado de control (FiguresScreen.kt:113-152)."""
    money, spot = DATA["money"], DATA["spot"]
    stamp = f'plata: {decimal(spot["eur_oz"], 2)} €/oz · hoy 09:14'
    coverage = ""
    if money["valued"] < money["pieces"]:
        coverage = (f'<div class="body muted">el valor de {money["valued"]} de tus '
                    f'{money["pieces"]} piezas</div>')
    return block("El valor", (
        f'<div class="display">{eur(money["eur"])}</div>'
        f'<div class="label muted">{stamp}</div>'
        f'{coverage}'
        '<div class="body muted">El mayor de tres precios en cada moneda: el catálogo de Numista, '
        'lo que pagaste o lo que vale su plata.</div>'))


def block(heading, inner, cls=""):
    return (f'<div class="block {cls}"><div class="eyebrow">{heading}</div>{inner}'
            '<div class="rule"></div></div>')


def ladder(reading):
    rungs = reading["rungs"]
    amount = decimal(reading["amount"], 0 if reading["unit"] == "cm" else 2)
    amount = f'{amount} {reading["unit"]}'
    if reading["approximate"]:
        amount = f"unos {amount}"
    place = reading["placement"]
    passed, nxt = place["passed"], place["next"]
    if passed and nxt:
        gap = nxt[1] - reading["amount"]
        gap = (f"{decimal(gap * 1000, 0)} g" if reading["unit"] == "kg" and gap < 1
               else f'{decimal(gap, 2)} {reading["unit"]}')
        comparison = f"más que {article(passed[0])} y a {gap} de {article(nxt[0])}"
    elif nxt:
        comparison = f"todavía por debajo de {article(nxt[0])}"
    else:
        comparison = f"por encima de {article(passed[0])}, que era el último referente"
    cells = "".join(
        f'<div class="rung"><span>{r["name"]}</span>'
        f'<span class="dim">{decimal(r["amount"], 0 if reading["unit"] == "cm" else 2)} '
        f'{reading["unit"]}</span></div>' for r in rungs)
    ticks = "".join(f'<i style="left:{i / (len(rungs) - 1) * 100}%"></i>'
                    for i in range(len(rungs)))
    return (f'<div class="ladder"><div class="label muted">{reading["statement"]}</div>'
            f'<div class="headline">{amount}</div>'
            f'<div class="body">{comparison}</div>'
            f'<div class="rungs"><div class="silhouettes"></div>'
            f'<div class="rail">{ticks}<b style="left:{place["fraction"] * 100}%"></b></div>'
            f'<div class="rungrow">{cells}</div></div></div>')


FEMENINOS = {"bici", "ballena", "bola de bolos", "encimera", "persona"}


def article(name):
    return f'{"una" if name in FEMENINOS else "un"} {name}'


def cifras_absence(variant, motivo):
    """Lo que cada variante pone donde iba la sección del dinero."""
    if variant == "0" or motivo == "mercado":
        return ""
    if variant == "A":
        return block("El valor",
                     f'<div class="body muted said">{under_eyebrow(CIFRAS_MOTIVO[motivo])}</div>',
                     cls="absence")
    if variant == "B":
        return ""      # la B no ocupa el sitio de la sección: cuelga del encabezado
    if variant in ("C", "E"):
        if variant == "E" and motivo in TRANSITORIOS:
            return ""  # la E calla cuando esperar basta
        return block("El valor", (
            f'<div class="body muted said">{under_eyebrow(CIFRAS_CORTA)}</div>'
            f'<div class="cardaction">{PUERTA}</div>'), cls="absence")
    if variant == "D":
        return block("El valor", (
            '<div class="voidamount"></div>'
            f'<div class="body muted said">{under_eyebrow(CIFRAS_CORTA)}</div>'), cls="absence")
    if variant == "F":
        return block("El valor",
                     f'<div class="body muted said">{under_eyebrow(CIFRAS_FIJA)}</div>',
                     cls="absence")
    return ""


def cifras_tail(variant, motivo):
    """La línea de la B, que no es una sección sino un renglón más del encabezado."""
    if variant != "B" or motivo == "mercado":
        return ""
    return f'<div class="body muted said tail">{CIFRAS_MOTIVO[motivo]}</div>'


def cifras_screen(variant, motivo):
    fig = DATA["figures"]
    head = ('<div class="head"><div class="eyebrow">Cuaderno de colección · Láminas de plata</div>'
            '<div class="display">Las cifras</div>'
            '<div class="bodylarge muted">Lo que pesa tu colección, y lo que vale.</div>'
            f'{cifras_tail(variant, motivo)}</div>')
    money = money_block() if motivo == "mercado" else cifras_absence(variant, motivo)
    matter = block("La materia", (
        f'<div class="bodylarge muted">{fig["pieces"]} piezas de {fig["issuers"]} emisores</div>'
        + "".join(ladder(r) for r in DATA["ladders"])), cls="matter")
    return (f'<div class="chrome"><b>Coindex</b><span>{fig["pieces"]} piezas · '
            f'{fig["types"]} tipos</span><i class="glyph"></i></div>'
            f'<div class="figures">{head}{money}{matter}</div>')


# ── la lámina ───────────────────────────────────────────────────────────────
def hole(cas):
    ghost = "" if cas["owned"] else " ghost"
    die = "" if cas["owned"] else '<div class="die"></div>'
    return (f'<div class="hole{ghost}"><div class="cardface"></div><div class="wall"></div>'
            f'<i class="p{cas["tid"]}"></i>{die}</div>')


def casilla(cas):
    year = f'<div class="tag">{cas["year"]}</div>' if cas["year"] else ""
    name = cas["label"] if cas["label"] != str(cas["year"]) else None
    printed = f'<div class="name">{name}</div>' if name else ""
    return f'<div class="cell">{hole(cas)}<div class="tagbox">{year}</div>{printed}</div>'


def plate_money(plate, variant, motivo):
    """Lo que la cabecera de la lámina dice del dinero, con mercado y sin él."""
    if motivo == "mercado":
        lines = [f'{VALUE_LABEL}: {eur(plate["value"])} · {MONEY_CRITERION}']
        if plate["cost_possible"]:
            lines.append(f'{COST_LABEL}: {eur(plate["cost"])} · {HOLE_CRITERION}')
        return ('<div class="moneylines">'
                + "".join(f'<div class="label rust">{line}</div>' for line in lines) + "</div>")
    if variant in ("0", "B"):
        return ""
    if variant == "A":
        text = lamina_motivo(plate, motivo)
    elif variant in ("C", "E"):
        if variant == "E" and motivo in TRANSITORIOS:
            return ""
        text = lamina_corta(plate)
    elif variant == "D":
        return ('<div class="moneylines"><div class="voidlines"></div>'
                f'<div class="label muted said">{lamina_corta(plate)}</div></div>')
    else:
        text = LAMINA_FIJA
    return f'<div class="moneylines"><div class="label muted said">{text}</div></div>'


def plate_screen(role, variant, motivo):
    plate = PLATES[role]
    ratio = f'{plate["owned"]}/{plate["issued"]}'
    stamp = '<div class="stamp"></div>' if plate["missing"] == 0 else ""
    spec = "".join(
        f'<div class="spec"><span>{k}</span><b>{v}</b></div>'
        for k, v in (("Emisor", plate["subtitle"].split(" ")[0]),
                     ("Casillas", str(plate["issued"])),
                     ("Curado", plate["updated"])))
    cells = "".join(casilla(c) for c in plate["casillas"])
    return (f'<div class="masthead"><div class="mastrow"><b>Coindex</b>'
            f'<span class="cardaction">Volver</span></div>'
            f'<div class="label muted sub">{plate["subtitle"]}</div></div>'
            f'<div class="plate"><div class="lead">'
            f'<div class="eyebrow">Catálogo curado</div>'
            f'<div class="platehead"><div class="headline">{plate["title"]}</div>'
            f'<div class="ratiobox">{stamp}<div class="ratio">{ratio}</div></div></div>'
            f'{plate_money(plate, variant, motivo)}'
            f'<div class="speccard">{spec}</div></div>'
            f'<div class="grid">{cells}</div></div>')


# ── las siete variantes ─────────────────────────────────────────────────────
VARIANTS = [
    ("0", "Hoy · v1.4.0", "el dinero se va y no queda nada en su sitio"),
    ("A", "La sección se queda y dice por qué", "la explicación de Ajustes, mudada"),
    ("B", "Un renglón del encabezado", "no promete una sección: la frase cuelga de la frase"),
    ("C", "La ausencia aquí, el porqué en Ajustes", "una frase fija y una puerta"),
    ("D", "El importe dibujado como hueco", "el vacío se ve, no se lee"),
    ("E", "Sólo cuando esperar no sirve", "callada mientras el pase avanza solo"),
    ("F", "Una frase y no cinco", "no distingue el motivo, así que no es la línea de Ajustes"),
]
ESCENAS = [
    ("cifras", "«Las cifras»"),
    ("holgada", "Lámina · las dos cifras"),
    ("umbral", "Lámina · sobre el umbral"),
    ("cerrada", "Lámina · cerrada"),
]


def screen(escena, variant, motivo):
    if escena == "cifras":
        return cifras_screen(variant, motivo)
    return plate_screen(escena, variant, motivo)


def phone(escena, variant, motivo):
    key, name, thesis = variant
    return (f'<figure class="phone" data-e="{escena}" data-m="{motivo}" data-v="{key}">'
            f'<figcaption><b>{key}</b> · {name}<i>{thesis}</i></figcaption>'
            f'<div class="screen">{screen(escena, key, motivo)}'
            f'<div class="foldline"></div></div>'
            f'<div class="readout"></div></figure>')


CSS = f"""
@font-face {{ font-family: Bitter; src: url({font('bitter_variable')}) format('truetype'); }}
@font-face {{ font-family: Barlow; font-weight: 600;
  src: url({font('barlow_condensed_semibold')}) format('truetype'); }}
* {{ box-sizing: border-box; margin: 0; padding: 0; }}
body {{ background: #1b1c19; color: {PAPER}; font-family: Barlow, sans-serif; }}
.bar {{ position: sticky; top: 0; z-index: 9; background: #24261f; padding: 10px 14px;
  display: flex; flex-wrap: wrap; gap: 14px; align-items: center;
  border-bottom: 1px solid #3a3d33; font-size: 13px; }}
.bar .grp {{ display: flex; gap: 4px; align-items: center; }}
.bar .grp > em {{ font-style: normal; opacity: .5; margin-right: 4px; }}
.bar a {{ color: {PAPER}; text-decoration: none; padding: 3px 8px; border: 1px solid #3f4237; }}
.bar a.on {{ background: {RUST}; border-color: {RUST}; }}
main {{ display: flex; gap: 22px; padding: 22px 14px 60px; align-items: flex-start;
  overflow-x: auto; }}
.phone[hidden] {{ display: none; }}
figcaption {{ width: {SCREEN}px; font-size: 13px; padding-bottom: 6px; line-height: 1.35; }}
figcaption i {{ display: block; font-style: normal; opacity: .55; }}
.readout {{ width: {SCREEN}px; font-size: 12px; padding-top: 6px; opacity: .75;
  line-height: 1.5; white-space: pre-line; }}
.screen {{ position: relative; width: {SCREEN}px; height: {FOLD}px; overflow: hidden;
  background: {PAPER}; color: {INK}; font-family: Bitter, serif; }}
.foldline {{ position: absolute; left: 0; right: 0; bottom: 0; height: 2px;
  background: repeating-linear-gradient(90deg,{RUST} 0 6px,transparent 6px 12px); }}

/* tipografía (Theme.kt:86-139) */
.display {{ font-size: 40px; line-height: 42px; }}
.headline {{ font-size: 26px; line-height: 30px; }}
.bodylarge {{ font-size: 16px; line-height: 23px; }}
.body {{ font-size: 14px; line-height: 20px; }}
.label, .eyebrow, .rung, .cardaction, .spec span, .tag, .ratio {{
  font-family: Barlow; font-weight: 600; font-variant: small-caps;
  font-feature-settings: 'smcp','tnum'; line-height: 1.25; }}
.label {{ font-size: 12px; }}
.eyebrow {{ font-size: 11px; color: {RUST}; }}
.muted {{ color: {MUTED}; }} .rust {{ color: {RUST}; }}

/* «Las cifras» */
.chrome {{ height: {CHROME_H}px; background: {INK}; color: {PAPER}; display: flex;
  align-items: center; gap: 10px; padding: 0 4px 0 12px; }}
.chrome b {{ font-size: 17px; font-weight: 400; }}
.chrome span {{ font-family: Barlow; font-size: 10px; font-variant: small-caps;
  color: {DEEP}; flex: 1; }}
.chrome .glyph {{ width: 48px; height: 48px; background:
  repeating-linear-gradient(180deg,transparent 0 10px,{PAPER} 10px 11.5px,transparent 11.5px 22px);
  background-position: 0 8px; background-size: 100% 44px; background-repeat: no-repeat; }}
.figures {{ padding: {FIG_TOP}px {FIG_MARGIN}px 28px; display: flex; flex-direction: column;
  gap: {FIG_BLOCKS}px; }}
.head {{ display: flex; flex-direction: column; gap: {HEAD_GAP}px; }}
.head .tail {{ margin-top: 0; }}
.block {{ display: flex; flex-direction: column; gap: {BLOCK_GAP}px; }}
.block .rule {{ height: 1px; background: {HAIR}; margin-top: {BLOCK_RULE}px; }}
.cardaction {{ align-self: flex-start; font-size: 12px; border: 1px solid {HAIR};
  padding: 6px 14px; }}
.voidamount {{ height: 42px; background: {DEEP};
  box-shadow: inset 0 1px 2px rgba(0,0,0,.18); }}

/* la escalera (ReferentLadder.kt) */
.ladder {{ display: flex; flex-direction: column; gap: {LADDER_GAP}px; }}
.rungs {{ padding-top: {RUNG_TOP}px; }}
.silhouettes {{ height: {SILHOUETTE_H}px; }}
.rail {{ position: relative; height: {MARK_DROP}px; border-top: 2px solid {INK}; }}
.rail i {{ position: absolute; top: 0; width: 2px; height: 35%; background: {LINE};
  transform: translateX(-1px); }}
.rail i:last-of-type {{ transform: translateX(-2px); }}
.rail b {{ position: absolute; top: 0; width: 3px; height: 100%; background: {RUST};
  transform: translateX(-1.5px); }}
.rungrow {{ display: flex; justify-content: space-between; padding-top: 2px; }}
.rung {{ width: {LABEL_W}px; display: flex; flex-direction: column; align-items: center;
  font-size: 10px; color: {MUTED}; text-align: center; }}
.rung .dim {{ color: {LINE}; }}

/* la lámina */
.masthead {{ border-bottom: 2px solid {INK}; }}
.mastrow {{ display: flex; justify-content: space-between; align-items: center;
  padding: 14px 20px 6px; }}
.mastrow b {{ font-size: 21px; font-weight: 400; }}
.masthead .sub {{ padding: 0 20px 8px; font-size: 11px; }}
.plate {{ padding: {PLATE_TOP}px {PLATE_MARGIN}px; }}
.lead {{ display: flex; flex-direction: column; gap: {LEAD_GAP}px; }}
.platehead {{ display: flex; gap: 12px; align-items: flex-start; }}
.platehead .headline {{ flex: 1; }}
.ratiobox {{ position: relative; width: {STAMP_W}px; height: {STAMP_H}px; }}
.ratio {{ position: absolute; right: 0; top: {RATIO_DROP}px; font-size: {RATIO_SIZE}px;
  color: {MOSS}; }}
.stamp {{ position: absolute; inset: 0; border: 2px solid {MOSS}; opacity: .5;
  transform: rotate(-5.5deg); }}
.moneylines {{ display: flex; flex-direction: column; gap: {MONEY_GAP}px; }}
.voidlines {{ height: 30px; background: {DEEP}; box-shadow: inset 0 1px 2px rgba(0,0,0,.18); }}
.speccard {{ background: {CARD}; border: 1px solid {LINE}; padding: {CARD_PAD}px; }}
.spec {{ display: flex; flex-direction: column; padding: 4px 0;
  border-bottom: 1px solid {DEEP}; }}
.spec:last-child {{ border-bottom: 0; }}
.spec span {{ font-size: 10px; color: {MUTED}; }}
.spec b {{ font-size: 14px; font-weight: 400; }}
.grid {{ display: flex; flex-wrap: wrap; gap: {ROW_GAP}px {GUTTER}px; padding-top: {LEAD_GAP}px; }}
.cell {{ width: {CELL}px; }}
.hole {{ position: relative; width: {HOLE}px; height: {HOLE}px; }}
.hole .cardface {{ position: absolute; inset: 0; background: {CARD};
  border: 1px solid {HAIR}; }}
.hole .wall {{ position: absolute; inset: {RING}px; border-radius: 50%;
  box-shadow: inset 0 1px 3px rgba(0,0,0,.35); }}
.hole i {{ position: absolute; inset: {RING}px; border-radius: 50%; background-size: cover;
  background-position: center; }}
.hole.ghost i {{ filter: grayscale(1) contrast(.35) brightness(1.12); opacity: .5; }}
.hole .die {{ position: absolute; inset: {RING}px; border-radius: 50%;
  border: 1px dashed {HAIR}; }}
.tagbox {{ height: {TAG_H}px; padding-top: 2px; }}
.tag {{ display: inline-block; width: {TAG_W}px; text-align: center; font-size: 12px;
  border: 1px solid {HAIR}; padding: 3px 0; }}
.name {{ font-size: {NAME_SIZE}px; line-height: {NAME_LINE}px; padding-top: {NAME_PAD}px;
  height: {NAME_LINE * 2 + NAME_PAD}px; overflow: hidden; }}
"""

JS = """
// Los tres ejes viven en la URL y en tres atributos del <html>; el CSS enseña la combinación.
const AXES = {e: 'cifras', m: 'red', v: 'todas'};
function apply() {
  const q = new URLSearchParams(location.search);
  const d = document.documentElement.dataset;
  for (const k in AXES) d[k] = q.get(k) || AXES[k];
  document.querySelectorAll('.bar a').forEach(a =>
    a.classList.toggle('on', d[a.dataset.k] === a.dataset.v));
  // El listón viaja siempre: una variante sola no se juzga contra nada.
  document.querySelectorAll('.phone').forEach(p => {
    p.hidden = !(p.dataset.e === d.e && p.dataset.m === d.m &&
      (d.v === 'todas' || p.dataset.v === d.v || p.dataset.v === '0'));
  });
  measure();
}
document.querySelectorAll('.bar a').forEach(a => a.onclick = ev => {
  ev.preventDefault();
  const q = new URLSearchParams(location.search);
  q.set(a.dataset.k, a.dataset.v);
  history.replaceState(null, '', '?' + q);
  apply();
});
// Lo medido: dónde cae el primer hueco (la lámina) o el filete de «La materia» («Las cifras»),
// que es lo que cada variante cobra por decir que el mercado no está.
const anchor = s => s.querySelector('.hole') || s.querySelector('.matter');
function measure() {
  const shown = [...document.querySelectorAll('.phone')].filter(p => p.offsetParent);
  const base = shown.find(p => p.dataset.v === '0');
  const at = p => {
    const s = p.querySelector('.screen'), a = anchor(s);
    return a ? a.getBoundingClientRect().top - s.getBoundingClientRect().top : null;
  };
  const zero = base ? at(base) : null;
  shown.forEach(p => {
    const top = at(p);
    if (top === null) return;
    const delta = zero === null ? '' :
      ` (${top - zero >= 0 ? '+' : ''}${Math.round(top - zero)})`;
    const said = p.querySelector('.said');
    const where = p.querySelector('.hole') ? 'primer hueco' : 'arranque de «La materia»';
    p.querySelector('.readout').textContent =
      `al ${where}: ${Math.round(top)} dp${delta}\\n` +
      (said ? `${said.textContent.trim().split(/\\s+/).length} palabras dichas` : 'nada dicho');
  });
}
apply();
"""


def bar():
    def group(name, key, options):
        links = "".join(f'<a href="#" data-k="{key}" data-v="{value}">{label}</a>'
                        for value, label in options)
        return f'<div class="grp"><em>{name}</em>{links}</div>'

    return ('<div class="bar">'
            + group("escena", "e", ESCENAS)
            + group("motivo", "m", [(k, n) for k, n, _ in MOTIVOS])
            + group("variante", "v", [("todas", "todas")] + [(k, k) for k, _, _ in VARIANTS])
            + "</div>")


def page():
    """Una sola página con las 168 pantallas dentro; el CSS enseña la combinación elegida."""
    phones = "".join(
        phone(escena, variant, motivo)
        for escena, _ in ESCENAS
        for motivo, _, _ in MOTIVOS
        for variant in VARIANTS)
    return (f'<!doctype html><meta charset="utf-8"><title>#519 · el mercado ausente</title>'
            f"<style>{CSS}</style>{bar()}<main>{phones}</main>"
            f"<script>{JS}</script>")


def photos_css():
    """Una clase por tipo: la misma foto se repite entre láminas y variantes."""
    tids = {c["tid"] for p in DATA["plates"] for c in p["casillas"]}
    return "".join(f'.p{t}{{background-image:url({photo(t)});}}'
                   for t in sorted(tids) if os.path.exists(f"{OUT}/fotos/{t}.img"))


def main():
    for plate in DATA["plates"]:
        # Una lámina sólo puede tener «Coste de cerrar» si le falta algo **y** está por debajo del
        # umbral de ADR 0028 §1: por encima, esos precios no se piden nunca.
        plate["cost_possible"] = plate["missing"] > 0 and plate["issued"] <= 10
    global CSS
    CSS += photos_css()
    path = f"{OUT}/maqueta.html"
    with open(path, "w") as handle:
        handle.write(page())
    size = os.path.getsize(path) / 1024 / 1024
    print(f"{path} — {len(ESCENAS) * len(MOTIVOS) * len(VARIANTS)} pantallas, {size:.1f} MB")


if __name__ == "__main__":
    main()
