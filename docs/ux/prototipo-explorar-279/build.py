#!/usr/bin/env python3
"""Genera la maqueta de «Explorar» (#279) a dp real, con datos y fotos de verdad.

Un solo HTML autocontenido: fuentes y fotos van embebidas en base64 para que
valga igual servido en local que publicado como artifact.
"""
import base64
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))
D = json.load(open(f"{HERE}/data.json"))
MINE, EXPLOR, SPOT = D["mine"], D["explor"], D["spot"]


def b64(path):
    return base64.b64encode(open(path, "rb").read()).decode()


def font(name):
    return f"data:font/woff2;base64,{b64(f'{HERE}/{name}.woff2')}"


def photo(tid):
    p = f"{HERE}/fotos-min/{tid}.jpg"
    if not tid or not os.path.exists(p):
        return None
    return f"data:image/jpeg;base64,{b64(p)}"


# ── qué fotos se usan de verdad, para no embeber las 182 ────────────────────
used = {}
for c in MINE:
    used[c["tid"]] = photo(c["tid"])
for c in EXPLOR:
    for m in c["casillas"]:
        used[m["tid"]] = photo(m["tid"])
used = {t: u for t, u in used.items() if u}

CSS_PHOTOS = "\n".join(f'.p{t}{{background-image:url("{u}")}}' for t, u in used.items())


def eur(v):
    return f"{v:,.0f} €".replace(",", ".")


def hole(tid, missing=False, size=104):
    """Un hueco troquelado: cartón, pared del corte, filete y la foto dentro."""
    cls = f"hole{' miss' if missing else ''}"
    ring = max(2, round(5 * size / 104))
    inner = f'<i class="p{tid}" style="inset:{ring}px"></i>' if tid in used else ""
    return f'<div class="{cls}" style="width:{size}px;height:{size}px">{inner}</div>'


# ── piezas de cromo comunes ────────────────────────────────────────────────
STATUS = '<div class="status"><span>16:24</span></div>'

SEWN = """<div class="sewn"><b>COINDEX</b><span>69 colecciones · 574 piezas · 192 tipos</span>
<u class="glyph"></u></div>"""


def masthead(subtitle):
    return f"""<div class="masthead"><div class="mh-row"><b>COINDEX</b>
<span class="action">&#8592; Volver</span></div><div class="mh-sub">{subtitle}</div></div>"""


def bar(selected="col"):
    cells = [("col", "Colecciones · 69"), ("mon", "Monedas · 192"), ("cif", "Las cifras · 6,91 kg")]
    return '<div class="bar">' + "".join(
        f'<span class="{"on" if k == selected else ""}">{v}</span>' for k, v in cells
    ) + "</div>"


SHELF = """<div class="search">Buscar en la colección</div>
<div class="shelf"><span>Por lámina · Por razón · Todos los países</span><em>Filtros</em></div>"""


# ── V0 · Hoy: el índice del padre, y las 20 no existen ─────────────────────
def v0():
    cards = "".join(
        f'<div class="card">{hole(c["tid"])}<div class="cname">{c["name"]}</div>'
        f'<div class="ratio">{c["owned"]}/{c["issued"]}</div></div>'
        for c in MINE
    )
    return f"""{STATUS}{SEWN}<div class="scroll" id="s0">{SHELF}
<div class="grid3">{cards}</div></div>{bar()}"""


# ── VA · El estante ajeno: la misma tarjeta, en fantasma ────────────────────
def va():
    cards = ""
    for c in EXPLOR:
        foot = (
            f'<div class="cost">{eur(c["entry"])}</div>'
            if c["entry"]
            else f'<div class="ratio q">{c["issued"]} casillas</div>'
        )
        cards += (
            f'<div class="card">{hole(c["casillas"][0]["tid"], missing=True)}'
            f'<div class="cname">{c["name"]}</div>{foot}</div>'
        )
    return f"""{STATUS}{masthead("Explorar · 20 láminas que no coleccionas")}
<div class="scroll" id="sa"><div class="note">Curadas y completas, sin ninguna pieza tuya dentro.
El importe es lo que costaría cerrarlas.</div><div class="grid3">{cards}</div>
<div class="foot">El precio es el suelo de la plata a {SPOT:.2f} €/oz. Numista no da todavía
precio de catálogo de estos huecos.</div></div>{bar()}"""


# ── VB · El escaparate: una pared de monedas, sin nombres ───────────────────
def vb():
    """Una moneda por lámina y no dos: dos años seguidos de una serie son el mismo dibujo."""
    coins = "".join(hole(c["casillas"][0]["tid"], missing=True) for c in EXPLOR)
    return f"""{STATUS}{masthead("Explorar · el escaparate")}
<div class="scroll" id="sb"><div class="note">Monedas que existen y no tienes. La lámina está
detrás de cada una.</div><div class="wall">{coins}</div></div>{bar()}"""


# ── VC · La carta: una lámina por pantalla, se hojea de lado ────────────────
def vc():
    c = next(x for x in EXPLOR if "Tudor" in x["full"])
    # La etiqueta y no el año: esta lámina lleva dos bestias por año, y el año solo no
    # distingue una casilla de su hermana.
    cells = "".join(
        f'<div class="cell">{hole(m["tid"], missing=True, size=88)}'
        f'<div class="yr">{m["label"]}<br>{m["year"] or ""}</div></div>'
        for m in c["casillas"]
    )
    head, *rest = c["full"].split(" · ")
    return f"""{STATUS}{masthead("Explorar · 8 de 20")}
<div class="scroll" id="sc"><div class="carta">
<div class="pager"><span>&#8592;</span><em>8 de 20</em><span>&#8594;</span></div>
<h1>{head}</h1>
<div class="sub">{" · ".join(rest)}</div>
<div class="grid-cells">{cells}</div>
<div class="entrada"><b>{eur(c["entry"])}</b><span>lo que cuesta entrar · {c["issued"]} casillas
· suelo de la plata</span></div></div></div>{bar()}"""


# ── VD · La contraportada: la última hoja del índice ────────────────────────
def vd():
    tail = "".join(
        f'<div class="card">{hole(c["tid"])}<div class="cname">{c["name"]}</div>'
        f'<div class="ratio">{c["owned"]}/{c["issued"]}</div></div>'
        for c in MINE[-3:]
    )
    small = "".join(
        f'<div class="mini">{hole(c["casillas"][0]["tid"], missing=True, size=64)}'
        f'<div class="mname">{c["name"]}</div>'
        f'<div class="mcost">{eur(c["entry"]) if c["entry"] else str(c["issued"]) + " casillas"}</div></div>'
        for c in EXPLOR
    )
    return f"""{STATUS}{SEWN}<div class="scroll" id="sd">
<div class="grid3">{tail}</div>
<div class="leaf"><div class="leaf-head">Y estas no las coleccionas</div>
<div class="leaf-sub">Veinte láminas curadas de las que no tienes ninguna pieza</div>
<div class="grid-mini">{small}</div></div></div>{bar()}"""


VARIANTS = [
    ("0", "Hoy · las 20 no existen", v0()),
    ("A", "El estante ajeno", va()),
    ("B", "El escaparate", vb()),
    ("C", "La carta", vc()),
    ("D", "La contraportada", vd()),
]

TABS = "".join(
    f'<button data-v="{k}" onclick="setState(\'{k}\')">{k} · {n}</button>' for k, n, _ in VARIANTS
)
PHONES = "".join(
    f'<div class="phone" id="v{k}" style="display:none">{h}</div>' for k, _, h in VARIANTS
)

STYLE = f"""<meta charset="utf-8">
<title>El escaparate de Coindex</title>
<style>
@font-face{{font-family:Bitter;src:url("{font('bitter')}") format("woff2");font-weight:400}}
@font-face{{font-family:Barlow;src:url("{font('barlow_condensed_regular')}") format("woff2");font-weight:400}}
@font-face{{font-family:Barlow;src:url("{font('barlow_condensed_semibold')}") format("woff2");font-weight:600}}
*{{box-sizing:border-box;margin:0;padding:0}}
:root{{--ink:#2D3029;--muted:#686A5D;--paper:#EEE8D7;--deep:#DDD3BB;--line:#7D806C;
--moss:#495C49;--rust:#8B553C;--hair:#878577}}
body{{background:#3a3a36;color:#eae4d4;font:13px/1.5 -apple-system,system-ui,sans-serif;
padding:0 0 40px}}
header{{position:sticky;top:0;z-index:9;background:#22221f;border-bottom:1px solid #55554e;
padding:8px 12px;display:flex;gap:6px;flex-wrap:wrap;align-items:center}}
header button{{font:600 12px/1 -apple-system,system-ui,sans-serif;padding:7px 10px;cursor:pointer;
background:#3c3c37;color:#e8e2d2;border:1px solid #55554e;border-radius:3px}}
header button.on{{background:#EEE8D7;color:#2D3029;border-color:#EEE8D7}}
header .m{{margin-left:auto;font:11px/1 ui-monospace,monospace;color:#9d9a8c}}
header button:focus-visible{{outline:2px solid #cbbf8a;outline-offset:2px}}
.stage{{display:flex;justify-content:center;padding:16px 8px}}
.cap{{max-width:411px;margin:0 auto 10px;font:12px/1.45 -apple-system,system-ui,sans-serif;
color:#b8b3a4}}
/* El cuaderno de obra: deliberadamente en otra letra y otro fondo que el teléfono, para que
   nadie confunda el banco de pruebas con la pantalla que se está juzgando. */
.ledger{{max-width:640px;margin:24px auto 0;padding:0 16px;
font:13px/1.6 -apple-system,system-ui,sans-serif;color:#c8c2b2}}
.ledger h2{{font:600 13px/1 -apple-system,system-ui,sans-serif;letter-spacing:.09em;
text-transform:uppercase;color:#8d8878;padding-bottom:12px;border-bottom:1px solid #4a4a44;
margin-bottom:14px}}
.ledger dl{{display:grid;grid-template-columns:auto;gap:12px}}
.ledger dt{{font-weight:600;color:#e6e0cf}}
.ledger dd{{padding-top:2px}}
.ledger b{{color:#e8dfa8;font-weight:600}}
.ledger code{{font:11px/1 ui-monospace,monospace;background:#2a2a26;padding:2px 4px;
border-radius:2px;color:#cfc9b6}}

/* ── el teléfono: 411 x 914 dp, 1 px CSS = 1 dp ── */
.phone{{width:411px;height:914px;overflow:hidden;position:relative;background:var(--paper);
color:var(--ink);font-family:Bitter;display:flex;flex-direction:column;
box-shadow:0 8px 30px rgba(0,0,0,.5)}}
.status{{height:24px;flex:0 0 24px;display:flex;align-items:center;justify-content:flex-end;
padding-right:14px;font:600 10px/1 Barlow;font-feature-settings:'smcp','tnum';color:var(--muted)}}
.sewn{{height:54px;flex:0 0 54px;background:var(--ink);color:var(--paper);display:flex;
align-items:center;gap:10px;padding:0 4px 0 12px}}
.sewn b{{font:400 17px/21px Bitter}}
.sewn span{{flex:1;font:600 10px/1 Barlow;font-feature-settings:'smcp','tnum';color:var(--deep)}}
/* Tres reglas con su cuenta encima, que es el glifo de Ajustes de AlbumChrome.kt:
   48 dp de área con 13 dp de padding, o sea 22 dp de dibujo. */
.glyph{{width:48px;height:48px;flex:0 0 48px;position:relative}}
.glyph:before{{content:'';position:absolute;inset:13px;background:
linear-gradient(var(--paper),var(--paper)) no-repeat 0 4px/100% 1.5px,
linear-gradient(var(--paper),var(--paper)) no-repeat 0 11px/100% 1.5px,
linear-gradient(var(--paper),var(--paper)) no-repeat 0 18px/100% 1.5px}}
.glyph:after{{content:'';position:absolute;inset:13px;background:
radial-gradient(circle 2.8px at 28% 4.7px,var(--ink) 55%,var(--paper) 56% 70%,transparent 71%),
radial-gradient(circle 2.8px at 66% 11.7px,var(--ink) 55%,var(--paper) 56% 70%,transparent 71%),
radial-gradient(circle 2.8px at 43% 18.7px,var(--ink) 55%,var(--paper) 56% 70%,transparent 71%)}}
.masthead{{flex:0 0 auto;border-bottom:2px solid var(--ink)}}
.mh-row{{display:flex;justify-content:space-between;align-items:center;padding:14px 20px 6px}}
.mh-row b{{font:400 21px/25px Bitter}}
.action{{font:600 12px/1 Barlow;font-feature-settings:'smcp','tnum';color:var(--moss)}}
.mh-sub{{padding:0 20px 8px;font:600 11px/1 Barlow;font-feature-settings:'smcp','tnum';
color:var(--muted)}}
.scroll{{flex:1;overflow-y:auto;overflow-x:hidden;padding:8px 12px}}
.bar{{flex:0 0 auto;display:flex;border-top:2px solid var(--ink);padding-bottom:24px}}
.bar span{{flex:1;text-align:center;padding:16px 0;background:var(--deep);color:var(--ink);
font:600 12px/1 Barlow;font-feature-settings:'smcp','tnum'}}
.bar span.on{{background:var(--ink);color:var(--paper)}}

/* ── el hueco troquelado (AlbumPaper.kt): cartón, pared, filete, foto ── */
.hole{{position:relative;border-radius:50%;background:rgba(255,252,242,.58);
box-shadow:inset 0 1.6px 2.4px rgba(45,48,41,.22),inset 0 -1.6px 2.4px rgba(255,255,255,.85);
outline:1px solid var(--hair);outline-offset:-1px;flex:0 0 auto}}
/* El color de respaldo y la imagen se declaran por separado a propósito: con el atajo
   `background:` esta regla (0,1,1) le ganaba el background-image a `.pNNN` (0,1,0). */
.hole i{{position:absolute;border-radius:50%;display:block;background-color:var(--deep);
background-repeat:no-repeat;background-position:center;background-size:cover}}
.hole:not(.miss) i:after{{content:'';position:absolute;inset:0;border-radius:50%;
background:linear-gradient(118deg,rgba(255,255,255,.34) 0 18%,rgba(255,255,255,0) 42%)}}
/* El fantasma exacto de una casilla vacía: alfa 0,14 y el círculo de puntos (AlbumPaper.kt). */
.hole.miss i{{opacity:.14;filter:grayscale(.2)}}
.hole.miss:before{{content:'';position:absolute;inset:6px;border-radius:50%;
border:1px dashed rgba(45,48,41,.48)}}
/* Y la otra lectura: en un escaparate no falta nada, así que la moneda se ve entera y lo
   que dice que no es tuya es el filete de puntos, no la penumbra. */
body.lit .hole.miss i{{opacity:1;filter:none}}
body.lit .hole.miss i:after{{content:'';position:absolute;inset:0;border-radius:50%;
background:linear-gradient(118deg,rgba(255,255,255,.34) 0 18%,rgba(255,255,255,0) 42%)}}

/* ── la tarjeta del índice (IndexScreen.kt) ── */
.grid3{{display:grid;grid-template-columns:repeat(3,104px);gap:6px 8px;justify-content:start}}
.card{{width:104px;display:flex;flex-direction:column;align-items:center}}
.cname{{font:400 15px/19px Bitter;text-align:center;padding:6px 0 2px;height:44px;overflow:hidden;
display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical}}
.ratio{{font:600 12px/14px Barlow;font-feature-settings:'smcp','tnum';color:var(--rust)}}
.ratio.q{{color:var(--muted)}}
.cost{{font:600 12px/14px Barlow;font-feature-settings:'smcp','tnum';color:var(--moss)}}
.search{{height:44px;display:flex;align-items:center;padding:0 12px;margin-bottom:6px;
border:1px solid var(--line);border-radius:2px;font:400 14px/20px Bitter;color:var(--muted)}}
.shelf{{display:flex;justify-content:space-between;align-items:center;padding:6px 0 10px;
font:600 11px/1 Barlow;font-feature-settings:'smcp','tnum';color:var(--muted)}}
.shelf em{{color:var(--moss);font-style:normal}}
.note{{font:400 14px/20px Bitter;color:var(--muted);padding:2px 0 10px}}
.foot{{font:400 12px/17px Bitter;color:var(--muted);padding:14px 0 24px;border-top:1px solid
var(--hair);margin-top:14px}}

/* ── VB · la pared ── */
.wall{{display:grid;grid-template-columns:repeat(3,104px);gap:8px;justify-content:start;
padding-bottom:24px}}

/* ── VC · la carta ── */
.carta{{padding:2px 0 24px}}
.pager{{display:flex;justify-content:space-between;align-items:center;padding-bottom:12px;
font:600 11px/1 Barlow;font-feature-settings:'smcp','tnum';color:var(--muted)}}
.pager span{{font-size:18px;color:var(--moss)}}
.carta h1{{font:400 26px/30px Bitter;padding-bottom:4px}}
.carta .sub{{font:400 14px/20px Bitter;color:var(--muted);padding-bottom:16px}}
.grid-cells{{display:grid;grid-template-columns:repeat(4,88px);gap:10px 8px;justify-content:start}}
.cell{{display:flex;flex-direction:column;align-items:center}}
.yr{{font:600 10px/13px Barlow;font-feature-settings:'smcp','tnum';color:var(--muted);
text-align:center;padding-top:3px;height:29px;overflow:hidden}}
.entrada{{margin-top:22px;padding-top:14px;border-top:1px solid var(--hair);display:flex;
flex-direction:column;gap:2px}}
.entrada b{{font:400 40px/42px Bitter;color:var(--moss)}}
.entrada span{{font:600 11px/15px Barlow;font-feature-settings:'smcp','tnum';color:var(--muted)}}

/* ── VD · la contraportada ── */
.leaf{{margin:18px -12px 0;padding:16px 12px 28px;background:var(--deep);
border-top:2px solid var(--ink)}}
.leaf-head{{font:400 21px/25px Bitter}}
.leaf-sub{{font:600 11px/1 Barlow;font-feature-settings:'smcp','tnum';color:var(--muted);
padding:4px 0 14px}}
.grid-mini{{display:grid;grid-template-columns:repeat(2,1fr);gap:12px 10px}}
.mini{{display:flex;flex-direction:column;align-items:center;text-align:center}}
.mname{{font:400 13px/17px Bitter;padding:4px 0 1px;height:34px;overflow:hidden;
display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical}}
.mcost{{font:600 11px/13px Barlow;font-feature-settings:'smcp','tnum';color:var(--moss)}}
{CSS_PHOTOS}
</style>
"""

HTML = STYLE + f"""<header>{TABS}<button id="lit" onclick="toggleLit()">Luz: fantasma</button>
<span class="m" id="metric"></span></header>
<div class="cap" id="cap"></div>
<div class="stage">{PHONES}</div>
<div class="ledger">
<h2>Cómo está hecha esta maqueta</h2>
<dl>
<dt>Tamaño</dt><dd>411 × 914 dp, que es el Pixel 7 de las capturas del #296, con 1 px CSS = 1 dp.
El cromo está copiado del código y no supuesto: el canto cosido de <code>AlbumChrome.kt</code>
(54 dp), la cabecera de <code>CoindexApp.Masthead</code>, las tres celdas de
<code>HierarchyBar</code> y la tarjeta de 104 dp de <code>IndexScreen.kt</code>. Las dos
tipografías son las del APK, subseteadas a woff2.</dd>
<dt>Datos</dt><dd>Los 75 catálogos de <code>data/</code> cruzados con la colección real del padre
(229 filas). Casilla llena y evidencia con las reglas de <code>CollectionCatalog.memberMatches</code>
e <code>isEvidencedBy</code>; la cara de cada casilla con <code>printed_side</code> y, si el
catálogo no lo declara, el reverso primero, como <code>AlbumFaces.kt</code>. Las fotos son las de
Numista que ya viajan en la caché sembrada del APK.</dd>
<dt>Precios</dt><dd><b>Son el suelo de la plata y no precio de catálogo</b>: gramos × ley leída del
<code>composition.text</code> × {SPOT:.2f} €/oz (spot del 12 de agosto de 2026). La app enseñaría
el mayor de tres precios, así que los importes de verdad serán <b>más altos</b>. Trece de las
veinte láminas los tienen completos; en las otras siete falta la ley de alguna casilla.</dd>
<dt>Lo que la maqueta no prueba</dt><dd>La réplica de hoy da <b>12,2 tarjetas</b> en el pliegue
contra las <b>11,04</b> medidas en el emulador: el buscador y el estante van a ojo, así que la
maqueta es un 10 % más densa que la app. Sirve para juzgar estructura, no para cerrar densidad.
<b>Nada de esto se ha visto en un teléfono.</b></dd>
<dt>La pregunta que asoma</dt><dd>El conmutador <b>Luz</b> no es un adorno. El fantasma de alfa
0,14 se diseñó para decir «esto te falta», y en un escaparate no falta nada: la moneda no es tuya
y nunca lo ha sido. A plena luz las veinte se leen como monedas; en fantasma, como un álbum
vacío.</dd>
</dl>
</div>
<script>
const V={json.dumps([[k, n] for k, n, _ in VARIANTS])};
const CAPS={{
 "0":"El índice del padre tal cual está hoy: 69 tarjetas, 49 de ellas láminas curadas. Las 20 que no colecciona <b>no existen</b> — <code>PlateUnavailable.NoEvidence</code>. Es el listón de densidad.",
 "A":"<b>Tesis: es el mismo objeto, sólo que no es tuyo.</b> La misma tarjeta de 104 dp, la moneda en fantasma (alfa 0,14 y el círculo de puntos de una casilla vacía) y, donde va la fracción, el coste de cerrarla.",
 "B":"<b>Tesis: lo que engancha es la moneda, no el catálogo.</b> No hay tarjetas: hay una pared de monedas sin nombre. La lámina es el segundo nivel, detrás de cada hueco.",
 "C":"<b>Tesis: cada lámina es una decisión de compra, no una fila de listado.</b> Una por pantalla, con sus casillas en fantasma y el precio de entrar en cuerpo 40.",
 "D":"<b>Tesis: la puerta aparte es un pie del álbum.</b> Ninguna pantalla nueva: al final del índice, tras las 69 tuyas, una hoja de otro papel con las veinte.",
}};
function setState(k){{
  V.forEach(([kk])=>document.getElementById("v"+kk).style.display=kk===k?"flex":"none");
  document.querySelectorAll("header button").forEach(b=>b.classList.toggle("on",b.dataset.v===k));
  document.getElementById("cap").innerHTML=CAPS[k]||"";
  location.hash=k; measure();
}}
/* Cuenta lo que de verdad entra en el pliegue: fracción de cada unidad que solapa
   con el viewport del scroll. Así el número no depende de mi aritmética de paddings. */
function measure(){{
  const p=document.querySelector(".phone[style*='flex']"); if(!p) return;
  const sc=p.querySelector(".scroll"); if(!sc) return;
  const box=sc.getBoundingClientRect();
  const units=[...p.querySelectorAll(".card,.mini,.cell,.wall>.hole")];
  let vis=0,h=0;
  units.forEach(u=>{{
    const r=u.getBoundingClientRect(); h=Math.max(h,r.height);
    const ov=Math.min(r.bottom,box.bottom)-Math.max(r.top,box.top);
    if(ov>0) vis+=Math.min(1,ov/r.height);
  }});
  document.getElementById("metric").textContent=
    `pliegue ${{sc.clientHeight}} dp · unidad ${{h.toFixed(0)}} dp · ${{vis.toFixed(2)}} visibles `+
    `de ${{units.length}}`;
}}
window.metrics=()=>{{const p=document.querySelector(".phone[style*='flex']");
 const sc=p.querySelector(".scroll");return {{fold:sc.clientHeight,scroll:sc.scrollHeight,
 text:document.getElementById("metric").textContent}};}};
window.scrollPhone=(px)=>{{document.querySelector(".phone[style*='flex'] .scroll").scrollTop=px;}};
function toggleLit(){{
  const on=document.body.classList.toggle("lit");
  document.getElementById("lit").textContent="Luz: "+(on?"a plena luz":"fantasma");
  document.getElementById("lit").classList.toggle("on",on);
}}
window.setLit=(on)=>{{if(!!on!==document.body.classList.contains("lit"))toggleLit();}};
window.toggleLit=toggleLit;
window.setState=setState;
addEventListener("keydown",e=>{{
  const i=V.findIndex(([k])=>k===(location.hash.slice(1)||"0"));
  if(e.key==="ArrowRight")setState(V[(i+1)%V.length][0]);
  if(e.key==="ArrowLeft")setState(V[(i-1+V.length)%V.length][0]);
}});
setState(location.hash.slice(1)||"0");
</script>
"""

def write(path, html):
    open(path, "w").write(html)
    print(f"{path} · {len(html)/1024:.0f} KB · {len(used)} fotos embebidas")


if __name__ == "__main__":
    write(f"{HERE}/explorar.html", HTML)
