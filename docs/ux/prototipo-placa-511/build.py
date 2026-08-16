#!/usr/bin/env python3
"""Maqueta de la casilla cuyo año no discrimina (#511, fleco 4b).

PROTOTIPO — se tira cuando el ticket se decida. Lo que sobrevive es el README.

Cuatro formas de la misma casilla, a dp real y con la lámina de hoy (v1.4.7) de listón.
Los números salen de donde los saca la app —`YearTagMetrics`, `PlateSpacing`,
`PlateMetrics`, la tipografía del tema y `AlbumToneConfig`— y están arriba, juntos,
para que la maqueta no pueda mentir por copia.

    python3 docs/ux/prototipo-placa-511/build.py && open docs/ux/prototipo-placa-511/maqueta.html
"""
import base64
import os

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.abspath(f"{HERE}/../../..")

# ── lo que la app mide, copiado de un sitio y no de la cabeza ───────────────
INK = "#2D3029"
PAPER = "#EEE8D7"
PAPER_DEEP = "#DDD3BB"
MUTED = "#6E6A5C"
RUST = "#8B553C"

SCREEN = 411          # el teléfono con el que se mide desde el #337
MARGIN = 20           # PLATE_MARGIN
GUTTER = 16           # PlateMetrics.gutter
HOLE = 104            # AlbumHole / GridCells.Adaptive(104.dp)
RING = 5              # HOLE_CARD_PADDING_DP

TAG_W, TAG_H, TAG_TARGET = 48.3, 28, 48     # YearTagMetrics
SLACK = (TAG_TARGET - TAG_H) / 2            # YearTagMetrics.slack == PlateSpacing.underTheHole
NAME_PAD = 6                                # PlateSpacing.namePadding
INSIDE = SLACK + NAME_PAD                   # PlateSpacing.insideMember
ROW_GAP = 32                                # PlateSpacing.rowGap

TAG_SIZE = 12         # labelLarge
NAME_SIZE = 17        # titleMedium
NAME_LINE = 21        # titleMedium.lineHeight

CARTOUCHE_ALPHA = 0.90        # AlbumToneConfig.cartoucheAlpha
TOP_RULE_ALPHA = 0.34         # AlbumToneConfig.cartoucheTopRuleAlpha

COLUMNS = max(1, int((SCREEN - MARGIN * 2 + GUTTER) / (HOLE + GUTTER)))
CELL = (SCREEN - MARGIN * 2 - GUTTER * (COLUMNS - 1)) / COLUMNS

# ── las láminas cuyo año no discrimina, tal como están curadas ──────────────
PLATES = [
    {
        "id": "espana-paquillos",
        "title": "Paquillos · España · 100 pesetas de Franco",
        "year": "1966",
        "type": "Numista 1885",
        "cells": ["Estrella 66", "Estrella 67", "Estrella 68", "Estrella 69", "Estrella 70"],
        "note": "Cinco casillas, un tipo, un año: las cinco chapas abren la misma ficha.",
    },
    {
        "id": "venezuela-1975-conservacion-plata",
        "title": "Conservación de la Naturaleza · Venezuela 1975 · estuche de dos monedas de plata .925",
        "year": "1975",
        "type": None,
        "cells": ["25 bolívares · jaguar · 28,28 g", "50 bolívares · cachicamo gigante · 35 g"],
        "note": "El caso que decide el ancho: 31 caracteres no caben en una chapa de 48,3 dp.",
    },
]

VARIANTS = [
    ("hoy", "Hoy · v1.4.7", "La chapa dice el año y el nombre va debajo. El año ya está en la ficha de la lámina, así que la lámina lo dice N+1 veces."),
    ("a", "A · la chapa dice lo que distingue", "El nombre entra en la chapa y la línea de debajo desaparece (`printedNameOf` ya calla el nombre que repite la chapa). La chapa crece a lo que mida el nombre."),
    ("b", "B · el nombre ocupa el sitio hundido", "Sin chapa de año: el nombre se hunde en el cartón a lo ancho de la casilla, que es la física que el cartucho de Monedas ya usa (#337). Conserva la puerta a Numista, y paga que el nombre se lea en versalitas de etiqueta."),
    ("b2", "B′ · el sitio hundido en letra de nombre", "La B con la letra del nombre y no la de la chapa: Bitter dentro del recess, que es exactamente lo que el cartucho de Monedas hace con su tema (`AlbumCartouche`). Misma puerta, y el nombre sigue siendo un nombre."),
    ("c", "C · sin chapa", "El año vive sólo en la ficha de la lámina y la casilla queda hueco + nombre en texto plano. Pierde el gesto que abre la ficha (#508)."),
]


def font(name):
    with open(f"{REPO}/app/src/main/res/font/{name}", "rb") as handle:
        return base64.b64encode(handle.read()).decode()


def hole():
    return (
        f'<div class="hole"><div class="ring"></div></div>'
    )


def tag(text, wide=False, serif=False):
    cls = "tag" + (" wide" if wide else "") + (" serif" if serif else "")
    return f'<div class="{cls}"><span>{text}</span></div>'


def cell_html(variant, label, year):
    if variant == "hoy":
        return f'<div class="cell">{hole()}{tag(year)}<div class="name">{label}</div></div>'
    if variant == "a":
        return f'<div class="cell">{hole()}{tag(label)}</div>'
    if variant == "b":
        return f'<div class="cell">{hole()}{tag(label, wide=True)}</div>'
    if variant == "b2":
        return f'<div class="cell">{hole()}{tag(label, wide=True, serif=True)}</div>'
    return f'<div class="cell">{hole()}<div class="name plain">{label}</div></div>'


def plate_html(variant, plate):
    facts = [("Año", plate["year"])]
    if plate["type"]:
        facts.insert(0, ("Tipo", plate["type"]))
    rows = "".join(
        f'<div class="fact"><span>{k}</span><b>{v}</b></div>' for k, v in facts
    )
    cells = "".join(cell_html(variant, label, plate["year"]) for label in plate["cells"])
    return (
        f'<section class="plate" data-plate="{plate["id"]}">'
        f'<h3>{plate["title"]}</h3>'
        f'<div class="spec">{rows}</div>'
        f'<div class="grid">{cells}</div>'
        f'<p class="note">{plate["note"]}</p>'
        f"</section>"
    )


def build():
    bitter = font("bitter_variable.ttf")
    barlow = font("barlow_condensed_semibold.ttf")
    panels = "".join(
        f'<article class="variant" id="v-{key}" data-variant="{key}">'
        f'<header><h2>{title}</h2><p>{blurb}</p></header>'
        + "".join(plate_html(key, plate) for plate in PLATES)
        + "</article>"
        for key, title, blurb in VARIANTS
    )
    buttons = "".join(
        f'<button data-show="{key}">{title.split(" · ")[0]}</button>' for key, title, _ in VARIANTS
    )
    html = f"""<!doctype html>
<meta charset="utf-8">
<title>Prototipo · la casilla cuyo año no discrimina (#511)</title>
<style>
@font-face {{ font-family: Bitter; src: url(data:font/ttf;base64,{bitter}); }}
@font-face {{ font-family: Barlow; src: url(data:font/ttf;base64,{barlow}); font-weight: 600; }}
* {{ box-sizing: border-box; }}
body {{ margin: 0; background: #3A3A38; color: {INK}; font-family: Bitter, serif; }}
.bar {{ position: sticky; top: 0; z-index: 9; display: flex; gap: 8px; padding: 10px 14px;
       background: #26261F; }}
.bar button, .bar label {{ font: 600 12px Barlow, sans-serif; letter-spacing: .06em;
       text-transform: uppercase; padding: 6px 12px; border: 1px solid #7C7768; background: none;
       color: #E7E1D0; cursor: pointer; }}
.bar button.on {{ background: {RUST}; border-color: {RUST}; }}
.deck {{ display: flex; gap: 26px; padding: 22px; align-items: flex-start; overflow-x: auto; }}
.variant {{ width: {SCREEN}px; flex: 0 0 auto; background: {PAPER}; padding: 0 0 20px; }}
.variant header {{ padding: 14px {MARGIN}px 4px; }}
.variant h2 {{ font: 600 13px Barlow, sans-serif; letter-spacing: .08em; text-transform: uppercase;
       color: {RUST}; margin: 0 0 6px; }}
.variant header p {{ font-size: 13px; line-height: 18px; color: {MUTED}; margin: 0; }}
.plate {{ padding: 16px {MARGIN}px 0; }}
.plate h3 {{ font: 400 17px/21px Bitter, serif; margin: 0 0 8px; }}
.spec {{ background: rgba(255,252,242,.58); border: 1px solid #B9B2A0; margin-bottom: 18px; }}
.fact {{ display: flex; justify-content: space-between; padding: 7px 10px; border-bottom: 1px solid #CFC7B2; }}
.fact:last-child {{ border-bottom: 0; }}
.fact span {{ font: 600 11px Barlow, sans-serif; letter-spacing: .06em; text-transform: uppercase; color: {MUTED}; }}
.fact b {{ font: 400 15px Bitter, serif; }}
.grid {{ display: flex; flex-wrap: wrap; gap: {ROW_GAP}px {GUTTER}px; }}
.cell {{ width: {CELL:.1f}px; display: flex; flex-direction: column; align-items: center; }}
.hole {{ width: {HOLE}px; height: {HOLE}px; border-radius: 50%; background: {PAPER_DEEP};
       position: relative; box-shadow: inset 0 2px 3px rgba(45,48,41,.30); }}
.ring {{ position: absolute; inset: {RING}px; border-radius: 50%; border: 1px dashed #A9A493; }}
.tag {{ margin-top: {SLACK}px; min-width: {TAG_W}px; height: {TAG_H}px; display: flex;
       align-items: center; justify-content: center; padding: 0 7px;
       background: rgba(221,211,187,{CARTOUCHE_ALPHA});
       border-top: 2px solid rgba(45,48,41,{TOP_RULE_ALPHA});
       border-bottom: 1px solid rgba(255,255,255,.55); }}
.tag.wide {{ width: 100%; height: auto; min-height: {TAG_H}px; padding: 5px 6px; }}
.tag span {{ font: 600 {TAG_SIZE}px Barlow, sans-serif; letter-spacing: .05em;
       text-transform: uppercase; font-feature-settings: 'smcp','tnum'; text-align: center; }}
.tag.serif span {{ font: 400 15px/19px Bitter, serif; letter-spacing: 0; text-transform: none;
       font-feature-settings: normal; }}
.name {{ margin-top: {NAME_PAD}px; font: 400 {NAME_SIZE}px/{NAME_LINE}px Bitter, serif;
       text-align: center; }}
.name.plain {{ margin-top: {SLACK}px; }}
.note {{ font-size: 12px; line-height: 17px; color: {MUTED}; margin: 16px 0 0; }}
body.cotas .cell {{ outline: 1px dashed rgba(139,85,60,.45); }}
body.cotas .tag {{ outline: 1px solid rgba(139,85,60,.65); }}
body.letra .variant {{ font-size: 130%; }}
body.letra .name {{ font-size: {NAME_SIZE * 1.3:.0f}px; line-height: {NAME_LINE * 1.3:.0f}px; }}
body.letra .tag span {{ font-size: {TAG_SIZE * 1.3:.0f}px; }}
body.letra .tag {{ height: auto; min-height: {TAG_H}px; }}
</style>
<div class="bar">
  <button data-show="todas" class="on">Todas</button>
  {buttons}
  <label><input type="checkbox" id="cotas"> cotas</label>
  <label><input type="checkbox" id="letra"> letra ×1,3</label>
</div>
<div class="deck">{panels}</div>
<script>
const deck = document.querySelector('.deck');
function show(which) {{
  document.querySelectorAll('.bar button').forEach(b => b.classList.toggle('on', b.dataset.show === which));
  document.querySelectorAll('.variant').forEach(v => {{
    v.style.display = (which === 'todas' || v.dataset.variant === which) ? '' : 'none';
  }});
}}
document.querySelectorAll('.bar button').forEach(b => b.onclick = () => show(b.dataset.show));
cotas.onchange = e => document.body.classList.toggle('cotas', e.target.checked);
letra.onchange = e => document.body.classList.toggle('letra', e.target.checked);
const params = new URLSearchParams(location.search);
if (params.get('v')) show(params.get('v'));
if (params.get('cotas')) {{ cotas.checked = true; document.body.classList.add('cotas'); }}
if (params.get('letra')) {{ letra.checked = true; document.body.classList.add('letra'); }}
</script>
"""
    out = f"{HERE}/maqueta.html"
    with open(out, "w", encoding="utf-8") as handle:
        handle.write(html)
    print(f"{out} · {COLUMNS} columnas de {CELL:.1f} dp")


if __name__ == "__main__":
    build()
