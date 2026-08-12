#!/usr/bin/env python3
"""La maqueta de lo elegido: «Explorar» con la puerta al final del índice, en fantasma.

Cuatro pantallas del mismo camino, no cuatro alternativas. Hereda el andamio de
`build.py` para que las dos maquetas no se separen: mismo cromo leído del código, mismos
datos, mismo listón de densidad.
"""
from build import (
    CSS_PHOTOS, EXPLOR, MINE, SEWN, SHELF, SPOT, STATUS, STYLE, bar, eur, hole, masthead, write,
    HERE,
)
import collections

# ADR 0023: el código manda donde la tabla lo corrige, y si no, el nombre de la ficha.
CURED = {"allemagne-pre1945": "Alemania", "chine": "China", "russie": "Rusia"}
FICHA = {"australie": "Australia", "royaume-uni": "Reino Unido", "niue": "Niue",
         "mexique": "México", "rwanda": "Ruanda", "venezuela": "Venezuela", "samoa": "Samoa",
         "ancienne_urss": "Unión Soviética", "serbie": "Serbia"}


def country(code):
    return CURED.get(code) or FICHA.get(code) or code


TUDOR = next(c for c in EXPLOR if "Tudor" in c["full"])
WITH_COST = [c for c in EXPLOR if c["entry"]]


def card(c, cost_first=False):
    foot = (f'<div class="cost">{eur(c["entry"])}</div>' if c["entry"]
            else f'<div class="ratio q">{c["issued"]} casillas</div>')
    return (f'<div class="card">{hole(c["casillas"][0]["tid"], missing=True)}'
            f'<div class="cname">{c["name"]}</div>{foot}</div>')


# ── 1 · La puerta: el final del índice, y la fila que sale de él ────────────
def puerta():
    """Las últimas tarjetas propias y la puerta debajo, que es donde se decidió ponerla.

    Quince y no todas: la maqueta se abre ya al final del scroll, que es la única parte del
    índice donde esta pantalla decide algo.
    """
    tail = "".join(
        f'<div class="card">{hole(c["tid"])}<div class="cname">{c["name"]}</div>'
        f'<div class="ratio">{c["owned"]}/{c["issued"]}</div></div>'
        for c in MINE[-15:]
    )
    peek = "".join(hole(c["casillas"][0]["tid"], missing=True, size=40) for c in EXPLOR[:4])
    return f"""{STATUS}{SEWN}<div class="scroll" id="s1">
<div class="grid3">{tail}</div>
<a class="puerta"><div class="peek">{peek}</div>
<div class="ptext"><b>Y otras 20 láminas que no coleccionas</b>
<span>Catálogos curados sin ninguna pieza tuya dentro</span></div>
<div class="parrow">&#8594;</div></a></div>{bar()}"""


# ── 2 · Explorar: pantalla propia, con estante propio ───────────────────────
def explorar():
    cards = "".join(card(c) for c in EXPLOR)
    return f"""{STATUS}{masthead("Explorar")}
<div class="scroll" id="s2">
<div class="search">Buscar entre las que no coleccionas</div>
<div class="shelf"><span>Por casillas · Todos los países</span><em>Filtros</em></div>
<div class="grid3">{cards}</div>
<div class="foot">Trece de las veinte dicen lo que cuesta entrar. Las otras siete pasan de diez
casillas, y el coste de once huecos no es un plan: es un reproche.</div></div>{bar()}"""


# ── 3 · El estante desplegado: el orden que una hoja del índice no puede tener ──
def estante():
    tally = collections.Counter(c["issuer"] for c in EXPLOR)
    chips = "".join(
        f'<i>{country(code)} <u>{n}</u></i>'
        for code, n in sorted(tally.items(), key=lambda kv: (-kv[1], country(kv[0])))
    )
    cards = "".join(card(c) for c in sorted(WITH_COST, key=lambda c: c["entry"]))
    return f"""{STATUS}{masthead("Explorar")}
<div class="scroll" id="s3">
<div class="search">Buscar entre las que no coleccionas</div>
<div class="shelf open"><span>Por coste de entrar · Todos los países</span><em>Filtros</em></div>
<div class="facets">
<div class="facet"><label>Orden</label><div class="chips">
<i class="on">Por coste de entrar</i><i>Por casillas</i><i>Por país</i></div></div>
<div class="facet"><label>País</label><div class="chips"><i class="on">Todos <u>20</u></i>{chips}
</div></div></div>
<div class="grid3">{cards}</div></div>{bar()}"""


# ── 4 · La lámina: la que hoy dice «Lámina no disponible» ───────────────────
def lamina():
    cells = "".join(
        f'<div class="pcell">{hole(m["tid"], missing=True)}'
        f'<div class="pname">{m["label"]}</div><div class="tag">{m["year"] or ""}</div></div>'
        for m in TUDOR["casillas"]
    )
    head, *rest = TUDOR["full"].split(" · ")
    return f"""{STATUS}{masthead(head)}
<div class="scroll plate" id="s4">
<div class="eyebrow">Catálogo curado</div>
<div class="phead"><h1>{head}</h1><div class="stamp">0/10</div></div>
<div class="pcost">{eur(TUDOR["entry"])} · lo que cuesta entrar · al suelo de la plata</div>
<div class="speccard"><div><span>Serie</span><b>{" · ".join(rest)}</b></div>
<div><span>Actualizado</span><b>{TUDOR["updated"]}</b></div></div>
<div class="primary">Exportar la lámina</div>
<div class="link">Fuente en Numista &#8599;</div>
<div class="pgrid">{cells}</div></div>{bar()}"""


SCREENS = [
    ("1", "La puerta", puerta()),
    ("2", "Explorar", explorar()),
    ("3", "Su estante", estante()),
    ("4", "La lámina", lamina()),
]

EXTRA = """<style>
/* ── 1 · la puerta al final del índice ── */
.puerta{display:flex;align-items:center;gap:12px;margin:14px -12px 0;padding:14px 12px;
background:var(--deep);border-top:1px solid var(--hair);text-decoration:none;color:var(--ink)}
.peek{display:flex}
.peek .hole{margin-right:-14px}
.ptext{flex:1;display:flex;flex-direction:column;gap:2px;padding-left:6px}
.ptext b{font:400 17px/21px Bitter;font-weight:400}
.ptext span{font:600 10px/1.3 Barlow;font-feature-settings:'smcp','tnum';color:var(--muted)}
.parrow{font:18px/1 Bitter;color:var(--moss)}
/* ── 3 · el estante desplegado ── */
.shelf.open em{color:var(--ink)}
.facets{display:flex;flex-direction:column;gap:10px;padding:2px 0 12px}
.facet label{display:block;font:600 10px/1 Barlow;font-feature-settings:'smcp','tnum';
color:var(--muted);padding-bottom:5px}
.chips{display:flex;flex-wrap:wrap;gap:5px}
.chips i{font:600 11px/1 Barlow;font-feature-settings:'smcp','tnum';font-style:normal;
padding:6px 9px;border:1px solid var(--line);border-radius:2px;color:var(--ink)}
.chips i.on{background:var(--ink);color:var(--paper);border-color:var(--ink)}
.chips u{text-decoration:none;color:var(--muted);padding-left:3px}
.chips i.on u{color:var(--deep)}
/* ── 4 · la lámina, con los márgenes de PlateScreen.kt ── */
.plate{padding:24px 20px}
.eyebrow{font:600 11px/1 Barlow;font-feature-settings:'smcp','tnum';color:var(--rust);
padding-bottom:10px}
.phead{display:flex;gap:12px;align-items:flex-start;padding-bottom:10px}
.phead h1{flex:1;font:400 26px/30px Bitter}
.stamp{font:600 12px/1 Barlow;font-feature-settings:'smcp','tnum';color:var(--rust);
border:1px solid var(--rust);border-radius:2px;padding:5px 7px}
.pcost{font:600 12px/1 Barlow;font-feature-settings:'smcp','tnum';color:var(--rust);
padding-bottom:10px}
.speccard{background:rgba(255,252,242,.58);border:1px solid var(--hair);padding:14px;
display:flex;flex-direction:column;gap:8px;margin-bottom:10px}
.speccard div{display:flex;justify-content:space-between;gap:12px}
.speccard span{font:600 10px/14px Barlow;font-feature-settings:'smcp','tnum';color:var(--muted)}
.speccard b{font:400 14px/18px Bitter;font-weight:400;text-align:right}
.primary{background:var(--moss);color:var(--paper);text-align:center;padding:14px;
font:600 12px/1 Barlow;font-feature-settings:'smcp','tnum';margin-bottom:10px}
.link{font:400 14px/20px Bitter;color:var(--moss);padding-bottom:24px}
/* Rejilla de casillas: 104 dp adaptativo, gutter 16, y 32 dp entre filas (PlateSpacing.rowGap) */
.pgrid{display:grid;grid-template-columns:repeat(3,1fr);gap:32px 16px;padding-bottom:24px}
.pcell{display:flex;flex-direction:column;align-items:center}
.pname{font:400 13px/21px Bitter;text-align:center;height:42px;padding-top:6px;overflow:hidden}
.tag{font:600 11px/28px Barlow;font-feature-settings:'smcp','tnum';color:var(--muted);
width:48.3px;height:28px;text-align:center;background:var(--deep);border-radius:2px;
box-shadow:inset 0 1px 2px rgba(45,48,41,.28)}
</style>"""

TABS = "".join(
    f'<button data-v="{k}" onclick="setState(\'{k}\')">{k} · {n}</button>' for k, n, _ in SCREENS
)
PHONES = "".join(
    f'<div class="phone" id="v{k}" style="display:none">{h}</div>' for k, _, h in SCREENS
)

CAPS = {
    "1": "El índice acaba como hoy y debajo sale <b>la puerta</b>: cuatro monedas asomando, el "
         "recuento y una flecha. El índice sigue diciendo «Colecciones · 69» y sigue siendo tu "
         "colección — la puerta no es una tarjeta más.",
    "2": "Explorar, con <b>cromo de pantalla interior</b>: cabecera con «Volver» y ninguna celda "
         "nueva en la barra. Las veinte en fantasma, y trece con lo que cuesta entrar.",
    "3": "El estante propio, que es <b>lo que decidió la A frente a la hoja del final</b>: aquí "
         "están ordenadas por coste de entrar, de 100 a 576 €. Una hoja dentro del índice no puede "
         "tener esto — las veinte están todas a 0/N y el orden «por razón» no dice nada de ellas.",
    "4": "Y al abrir una, la lámina que hoy contesta <b>«Aún no tienes ninguna emisión oficial de "
         "este catálogo»</b>: las diez casillas vacías, el sello a 0/10 y el coste de entrar donde "
         "el ADR 0026 §10 dijo que vive, en la cabecera de la lámina.",
}

HTML = (STYLE.replace("<title>El escaparate de Coindex</title>",
                      "<title>La puerta de Explorar</title>") + EXTRA +
        f"""<header>{TABS}<span class="m" id="metric"></span></header>
<div class="cap" id="cap"></div>
<div class="stage">{PHONES}</div>
<div class="ledger">
<h2>Qué es esta maqueta</h2>
<dl>
<dt>Qué enseña</dt><dd><b>Un camino, no cuatro alternativas.</b> Es lo elegido el 12 de agosto de
2026 sobre las cinco variantes: la pantalla propia con la puerta al final del índice, y el fantasma
de siempre. Las cuatro pantallas son los cuatro pasos del mismo recorrido.</dd>
<dt>Lo mismo que la otra</dt><dd>Comparte el andamio con la maqueta de las cinco variantes: 411 ×
914 dp, el cromo leído del código, los datos de <code>data/</code> cruzados con la colección real
del padre y los mismos importes — <b>suelo de la plata a {SPOT:.2f} €/oz, no precio de
catálogo</b>.</dd>
<dt>Lo que hay que decidir todavía</dt><dd>Las facetas de Explorar están puestas a mano: orden por
coste, por casillas y por país, y el país con su recuento. Son una propuesta, no una medida — y el
estante de Colecciones tiene cinco filtros que aquí no tienen sentido.</dd>
<dt>Lo que no prueba</dt><dd><b>Nada de esto se ha visto en un teléfono.</b> Y la lámina de la
cuarta pantalla es la que ya existe: si «Explorar» se construye, lo que cambia de ella es una
cláusula de tamaño en <code>NoEvidence</code> y una cifra en la cabecera, no la hoja.</dd>
</dl>
</div>
<script>
const V={[[k, n] for k, n, _ in SCREENS]!r};
const CAPS={CAPS!r};
function setState(k){{
  V.forEach(([kk])=>document.getElementById("v"+kk).style.display=kk===k?"flex":"none");
  document.querySelectorAll("header button").forEach(b=>b.classList.toggle("on",b.dataset.v===k));
  document.getElementById("cap").innerHTML=CAPS[k]||"";
  location.hash=k; measure();
}}
function measure(){{
  const p=document.querySelector(".phone[style*='flex']"); if(!p) return;
  const sc=p.querySelector(".scroll"); if(!sc) return;
  const box=sc.getBoundingClientRect();
  const units=[...p.querySelectorAll(".card,.pcell")];
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
window.metrics=()=>{{const sc=document.querySelector(".phone[style*='flex'] .scroll");
 return {{fold:sc.clientHeight,scroll:sc.scrollHeight,
 text:document.getElementById("metric").textContent}};}};
window.scrollPhone=(px)=>{{document.querySelector(".phone[style*='flex'] .scroll").scrollTop=px;}};
window.setState=setState;
addEventListener("keydown",e=>{{
  const i=V.findIndex(([k])=>k===(location.hash.slice(1)||"1"));
  if(e.key==="ArrowRight")setState(V[(i+1)%V.length][0]);
  if(e.key==="ArrowLeft")setState(V[(i-1+V.length)%V.length][0]);
}});
setState(location.hash.slice(1)||"1");
</script>
""")

if __name__ == "__main__":
    write(f"{HERE}/elegida.html", HTML)
