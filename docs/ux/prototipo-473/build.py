#!/usr/bin/env python3
"""Maqueta de las dos salidas del #473, a dp real y con la lámina de hoy de listón.

PROTOTIPO — se tira cuando el ticket se decida. Lo que sobrevive es el README.

Un solo HTML autocontenido: las fuentes de la app y las cuatro fotos del catálogo van
embebidas en base64. Los números salen de donde los saca la app —`PlateSpacing`,
`YearTagMetrics`, la tipografía del tema y `AlbumToneConfig`— y están arriba, juntos,
para que la maqueta no pueda mentir por copia.

    python3 docs/ux/prototipo-473/build.py && open docs/ux/prototipo-473/maqueta.html
"""
import base64
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.abspath(f"{HERE}/../../..")

# ── lo que la app mide, copiado de un sitio y no de la cabeza ───────────────
INK = "#2D3029"
PAPER = "#EEE8D7"
PAPER_DEEP = "#DDD3BB"
CARD = "rgba(255,252,242,0.58)"  # Paper.card = 0x94FFFCF2
HAIRLINE = "#878577"
RUST = "#8B553C"

HOLE = 104  # AlbumHole en la lámina
RING = 5  # HOLE_CARD_PADDING_DP
CELL_MIN = 104  # GridCells.Adaptive
GUTTER = 16  # PlateMetrics.gutter
MARGIN = 20  # PLATE_MARGIN
SCREEN = 411  # el teléfono con el que se mide desde el #337

NAME_SIZE = 17  # titleMedium
NAME_LINE = 21  # titleMedium.lineHeight == PlateSpacing.nameLine
NAME_MIN_SIZE = 13  # PLATE_CELL_NAME_MIN_SIZE
NAME_PAD = 6  # PlateSpacing.namePadding

TAG_W, TAG_H, TAG_TARGET = 48.3, 28, 48  # YearTagMetrics
SLACK = (TAG_TARGET - TAG_H) / 2  # 10 dp de aire transparente sobre y bajo la tinta

ROW_GAP_TODAY = 32  # PlateSpacing.rowGap
ROW_GAP_B = 56  # lo que la salida B propone

CELL = (SCREEN - 2 * MARGIN - 2 * GUTTER) / 3  # 113 dp


def b64(path, mime):
    return f"data:{mime};base64," + base64.b64encode(open(path, "rb").read()).decode()


def font(name):
    return b64(f"{REPO}/app/src/main/res/font/{name}.ttf", "font/ttf")


def photo(tid):
    return b64(f"{HERE}/fotos/{tid}.jpg", "image/jpeg")


# ── la lámina de verdad: el 1 Bolívar del padre, sus cuatro últimas casillas ─
CATALOG = json.load(open(f"{REPO}/data/collection-catalogs/venezuela-1-bolivar.json"))
MEMBERS = CATALOG["members"]
# Las cuatro que el padre tiene son las casillas 19 a 22 de 22 (ADR 0026 §3).
OWNED = set(range(18, 22))
# La fila mixta es la 6 (índices 18-20) y se enseña con su vecina de arriba y la de abajo.
ROWS = [MEMBERS[15:18], MEMBERS[18:21], MEMBERS[21:22]]
FIRST = 15

PHOTOS = {m["numista_type_id"]: photo(m["numista_type_id"]) for m in MEMBERS[15:22]}


def printed_name(m):
    """`DrawnCell.printedName`: nada cuando el rótulo ya es el año."""
    return m["label"] if m["label"] != str(m.get("year")) else None


def hole(m, owned):
    tid = m["numista_type_id"]
    ghost = "" if owned else " ghost"
    return (
        f'<div class="hole{ghost}">'
        f'<div class="card"></div><div class="wall"></div><div class="rule"></div>'
        f'<img src="{PHOTOS[tid]}" alt="">'
        f'{"" if owned else "<div class=\'die\'></div>"}'
        f"</div>"
    )


def tag(m):
    return f'<div class="tagbox"><div class="tag">{m["year"]}</div></div>'


def name(m):
    n = printed_name(m)
    return (
        f'<div class="name" data-name="{"" if n is None else 1}">'
        f'<span>{n or ""}</span></div>'
    )


def cell(m, index, variant):
    """Una casilla. En A el nombre va debajo de la chapa y no reserva por fila."""
    owned = index in OWNED
    if variant == "a":
        inner = hole(m, owned) + tag(m) + name(m)
    else:
        inner = hole(m, owned) + name(m) + tag(m)
    return f'<div class="cell" data-i="{index}">{inner}</div>'


def sheet(variant):
    rows = []
    for r, row in enumerate(ROWS):
        cells = "".join(
            cell(m, FIRST + r * 3 + c, variant) for c, m in enumerate(row)
        )
        rows.append(f'<div class="row">{cells}</div>')
    return f'<div class="sheet {variant}">{"".join(rows)}</div>'


VARIANTS = [
    ("hoy", "Hoy (v1.2.16)", "moneda → nombre → chapa, y la fila reserva la caja"),
    ("a", "A · el orden invertido", "moneda → chapa → nombre, sin reserva por fila"),
    ("b", "B · más aire entre filas", f"igual que hoy, con rowGap {ROW_GAP_B} dp"),
]

CSS = f"""
@font-face {{ font-family: Bitter; src: url("{font('bitter_variable')}"); }}
@font-face {{ font-family: Barlow; font-weight: 600;
              src: url("{font('barlow_condensed_semibold')}"); }}

* {{ box-sizing: border-box; margin: 0; padding: 0; }}
body {{ background: #3a3a36; color: {PAPER}; font-family: Barlow, sans-serif;
        padding: 28px 28px 96px; }}

.wall {{ display: grid; grid-auto-flow: column; gap: 28px; justify-content: start;
         align-items: start; }}
.wall.solo {{ grid-auto-flow: row; }}
.panel {{ display: none; }}
.panel.on {{ display: block; }}
.panel h2 {{ font: 600 15px/1.3 Barlow; letter-spacing: .06em; text-transform: uppercase; }}
.panel p {{ font: 400 12px/1.4 Bitter; opacity: .62; margin: 3px 0 10px; max-width: {SCREEN}px; }}

/* ── el pliegue: 411 dp de teléfono, 1 dp = 1 px ─────────────────────────── */
.fold {{ width: {SCREEN}px; background: {PAPER}; overflow: hidden;
         padding: {MARGIN}px {MARGIN}px 24px; position: relative; }}
.fold.cut {{ height: 430px; padding-top: 0; }}
.fold.cut .sheet {{ margin-top: -76px; }}

.row {{ display: grid; grid-template-columns: repeat(3, {CELL}px); gap: {GUTTER}px;
        align-items: start; }}
.sheet {{ display: grid; }}
.sheet.hoy, .sheet.a {{ row-gap: {ROW_GAP_TODAY}px; }}
.sheet.b {{ row-gap: {ROW_GAP_B}px; }}

.cell {{ display: flex; flex-direction: column; align-items: center; }}

/* ── el hueco troquelado ──────────────────────────────────────────────────── */
.hole {{ position: relative; width: {HOLE}px; height: {HOLE}px; flex: none; }}
.hole .card {{ position: absolute; inset: 0; border-radius: 50%; background: {CARD}; }}
.hole .wall {{ position: absolute; inset: 0; border-radius: 50%;
  background: conic-gradient(from 90deg,
    rgba(255,255,255,0) 0deg, rgba(255,255,255,.85) 90deg, rgba(255,255,255,0) 176.4deg,
    rgba(45,48,41,0) 183.6deg, rgba(45,48,41,.22) 270deg, rgba(45,48,41,0) 356.4deg,
    rgba(255,255,255,0) 360deg);
  -webkit-mask: radial-gradient(circle at 50% 50%,
    transparent calc(50% - {RING}px), #000 calc(50% - {RING}px)); }}
.hole .rule {{ position: absolute; inset: 0; border-radius: 50%;
               border: 1px solid {HAIRLINE}; }}
.hole img {{ position: absolute; inset: {RING}px; border-radius: 50%;
             width: {HOLE - 2 * RING}px; height: {HOLE - 2 * RING}px;
             object-fit: cover; background: {PAPER_DEEP}; }}
.hole.ghost img {{ opacity: .14; }}
.hole .die {{ position: absolute; inset: 6px; border-radius: 50%;
  border: 1px dashed rgba(45,48,41,.48); }}

/* ── el nombre ────────────────────────────────────────────────────────────── */
.name {{ width: 100%; display: flex; align-items: center; justify-content: center;
         text-align: center; font-family: Bitter; color: {INK};
         font-size: {NAME_SIZE}px; line-height: {NAME_LINE}px;
         padding: {NAME_PAD}px 0; overflow: hidden; }}
/* Lo que la casilla hace cuando ni el autosize basta: elipsis, nunca media línea (#348). */
.name span {{ display: -webkit-box; -webkit-box-orient: vertical; overflow: hidden; }}

/* ── la chapa hundida ─────────────────────────────────────────────────────── */
.tagbox {{ height: {TAG_TARGET}px; display: flex; align-items: center; flex: none; }}
.tag {{ min-width: {TAG_W}px; height: {TAG_H}px; display: flex; align-items: center;
        justify-content: center; padding: 0 6px;
        background: rgba(221,211,187,.90);
        border-top: 2px solid rgba(45,48,41,.34);
        border-bottom: 1px solid rgba(255,255,255,.55);
        font: 600 12px Barlow; font-feature-settings: 'smcp','tnum';
        font-variant-caps: small-caps; color: {INK}; }}

/* ── las cotas, encendidas aparte ─────────────────────────────────────────── */
.fold .cota {{ display: none; }}
.fold.cotas .cota {{ display: block; position: absolute; z-index: 3; width: 20px; }}
.cota .bar {{ position: absolute; inset: 0; border-top: 1px solid {RUST};
              border-bottom: 1px solid {RUST}; }}
.cota .bar::before {{ content: ''; position: absolute; left: 50%; top: 0; bottom: 0;
                      border-left: 1px dashed {RUST}; }}
.cota b {{ position: absolute; left: 100%; top: 50%; transform: translateY(-50%);
           margin-left: 4px; font: 600 11px Barlow; color: {RUST}; white-space: nowrap;
           background: rgba(238,232,215,.86); padding: 1px 2px; }}
.cota.mala .bar {{ border-color: #a3312a; }}
.cota.mala b {{ color: #a3312a; }}

/* ── la barra ─────────────────────────────────────────────────────────────── */
.bar-bottom {{ position: fixed; left: 0; right: 0; bottom: 0; display: flex; gap: 8px;
   padding: 10px 14px; background: #23231f; border-top: 1px solid #4a4a44;
   align-items: center; z-index: 9; }}
.bar-bottom button {{ font: 600 12px Barlow; letter-spacing: .06em; text-transform: uppercase;
   padding: 7px 12px; border: 1px solid #5c5c54; background: transparent; color: {PAPER};
   cursor: pointer; }}
.bar-bottom button.on {{ background: {PAPER}; color: #23231f; }}
.bar-bottom .sep {{ width: 1px; height: 22px; background: #4a4a44; margin: 0 6px; }}
"""

PANELS = "".join(
    f"""<div class="panel" data-v="{key}">
      <h2>{title}</h2><p>{blurb}</p>
      <div class="fold" data-v="{key}">{sheet(key)}</div>
    </div>"""
    for key, title, blurb in VARIANTS
)

JS = f"""
const NAME_MAX = {NAME_SIZE}, NAME_MIN = {NAME_MIN_SIZE}, BASE_LINE = {NAME_LINE};
const PAD = {NAME_PAD}, MIN_LINES = 2, MAX_LINES = 3, SLACK = {SLACK};
const TAG_SIZE = 12, ROW_GAP = {{hoy: {ROW_GAP_TODAY}, a: {ROW_GAP_TODAY}, b: {ROW_GAP_B}}};

/** La escala de tipo del coleccionista: sp crece, dp no. Es el primo del #473. */
let scale = 1;
const line = () => BASE_LINE * scale;

/** `PlateSpacing.insideMemberCentred` < `betweenMembers`, que es `plateNameLinesCeiling`. */
function ceiling(variant) {{
  const between = SLACK + ROW_GAP[variant];
  let last = MIN_LINES;
  for (let n = MIN_LINES; n <= MAX_LINES; n++) {{
    if (PAD + SLACK + line() * (n - 1) / 2 < between) last = n;
  }}
  return last;
}}

/** La escalera del #348: Bitter encoge de 17 a 13 sp en pasos de 0,5 antes de cortar. */
function linesAt(el, size, width) {{
  const probe = document.createElement('div');
  probe.style.cssText = `position:absolute;visibility:hidden;width:${{width}}px;
    font-family:Bitter;font-size:${{size}}px;line-height:${{line()}}px;text-align:center`;
  probe.textContent = el.dataset.text;
  document.body.appendChild(probe);
  const n = Math.round(probe.offsetHeight / line());
  probe.remove();
  return n;
}}

/** `plateRowNameLines`: la fila reserva lo que pide su nombre más alto, entre 2 y su techo. */
function layout(fold) {{
  const variant = fold.dataset.v;
  const top = ceiling(variant);
  fold.querySelectorAll('.tag').forEach(t => {{ t.style.fontSize = TAG_SIZE * scale + 'px'; }});
  fold.querySelectorAll('.row').forEach(row => {{
    const names = [...row.querySelectorAll('.name')];
    // La B en su mejor versión: una fila mixta no compra la tercera línea, porque quien la
    // pagaría es la casilla sin nombre, que no tiene con qué partir el blanco. El techo entra
    // antes que el autosize, o el nombre encogería contra una caja que la fila no le va a dar.
    const mixed = names.some(n => n.dataset.name) && names.some(n => !n.dataset.name);
    const rowTop = variant === 'b' && mixed ? Math.min(top, MIN_LINES) : top;
    let reserved = 0;
    names.forEach(n => {{
      n.style.lineHeight = line() + 'px';
      if (!n.dataset.name) return;
      const w = n.offsetWidth;
      let size = NAME_MAX * scale, lines = linesAt(n, size, w);
      while (lines > rowTop && size > NAME_MIN * scale) {{
        size = Math.max(NAME_MIN * scale, size - 0.5);
        lines = linesAt(n, size, w);
      }}
      n.style.fontSize = size + 'px';
      n.dataset.lines = Math.min(lines, rowTop);
      reserved = Math.max(reserved, Math.min(lines, rowTop));
    }});
    if (reserved) reserved = Math.max(reserved, MIN_LINES);
    names.forEach(n => {{
      const own = n.dataset.name ? Number(n.dataset.lines) : 0;
      // Sin reserva de fila en A: cada nombre ocupa lo que mide, y el que no existe no ocupa.
      const box = variant === 'a' ? own : reserved;
      n.style.height = (box ? box * line() + 2 * PAD : 0) + 'px';
      n.querySelector('span').style.webkitLineClamp = Math.max(1, box);
    }});
  }});
}}

/** Las dos distancias que el #411 comparó, medidas sobre el dibujo y no sobre la fórmula. */
function cotas(fold) {{
  fold.querySelectorAll('.cota').forEach(c => c.remove());
  const cell = fold.querySelector('.cell[data-i="20"]');       // el 1960, la sin nombre
  const below = fold.querySelector('.cell[data-i="21"]');       // el 1965, la fila de abajo
  if (!cell || !below) return;
  const base = fold.getBoundingClientRect();
  const hole = cell.querySelector('.hole').getBoundingClientRect();
  const tag = cell.querySelector('.tag').getBoundingClientRect();
  const next = below.querySelector('.hole').getBoundingClientRect();
  const draw = (top, bottom, label, bad) => {{
    const d = document.createElement('div');
    d.className = 'cota' + (bad ? ' mala' : '');
    d.style.left = (hole.left - base.left + 18) + 'px';
    d.style.top = (top - base.top) + 'px';
    d.style.height = (bottom - top) + 'px';
    d.innerHTML = `<div class="bar"></div><b>${{label}} ${{Math.round(bottom - top)}} dp</b>`;
    fold.appendChild(d);
  }};
  const dentro = tag.top - hole.bottom, entre = next.top - tag.bottom;
  draw(hole.bottom, tag.top, 'dentro', dentro >= entre);
  draw(tag.bottom, next.top, 'entre', false);
}}

function render() {{
  document.querySelectorAll('.fold').forEach(f => {{ layout(f); cotas(f); }});
}}

document.querySelectorAll('.name').forEach(n => {{ n.dataset.text = n.textContent.trim(); }});

const params = new URLSearchParams(location.search);
let solo = params.get('v') || 'todas';
let cut = params.get('corte') === '1', measured = params.get('cotas') === '1';
scale = params.get('letra') === '1' ? 1.3 : 1;

function paint() {{
  document.querySelectorAll('.panel').forEach(p => {{
    p.classList.toggle('on', solo === 'todas' || p.dataset.v === solo);
  }});
  document.querySelector('.wall').classList.toggle('solo', solo !== 'todas');
  document.querySelectorAll('.fold').forEach(f => {{
    f.classList.toggle('cut', cut);
    f.classList.toggle('cotas', measured);
  }});
  document.querySelectorAll('[data-set]').forEach(b => {{
    b.classList.toggle('on', b.dataset.set === solo ||
      (b.dataset.set === 'corte' && cut) || (b.dataset.set === 'cotas' && measured) ||
      (b.dataset.set === 'letra' && scale !== 1));
  }});
  render();
}}

document.querySelectorAll('[data-set]').forEach(b => b.addEventListener('click', () => {{
  const k = b.dataset.set;
  if (k === 'corte') cut = !cut;
  else if (k === 'cotas') measured = !measured;
  else if (k === 'letra') scale = scale === 1 ? 1.3 : 1;
  else solo = k;
  paint();
}}));

document.fonts.ready.then(paint);
"""

HTML = f"""<!doctype html>
<html lang="es"><head><meta charset="utf-8">
<title>Prototipo #473 · la casilla sin nombre de una fila con nombre</title>
<style>{CSS}</style></head>
<body>
<div class="wall">{PANELS}</div>
<div class="bar-bottom">
  <button data-set="todas">Las tres</button>
  <button data-set="hoy">Hoy</button>
  <button data-set="a">A · invertido</button>
  <button data-set="b">B · más aire</button>
  <span class="sep"></span>
  <button data-set="corte">Fila cortada arriba</button>
  <button data-set="letra">Letra ×1,3</button>
  <button data-set="cotas">Cotas</button>
</div>
<script>{JS}</script>
</body></html>
"""

out = f"{HERE}/maqueta.html"
open(out, "w").write(HTML)
print(f"{out}  ({len(HTML) / 1024:.0f} kB)")
