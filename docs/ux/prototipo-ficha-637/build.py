#!/usr/bin/env python3
"""Maqueta del #637: qué lleva la ficha de una moneda, y en qué orden.

Prototipo: se tira cuando el ticket se cierre; lo que sobrevive es el README.

Cinco fichas a dp real (411 × 914, el Pixel 7 de las capturas del #296) sobre cinco monedas del
padre. La 0 es la hoja de hoy (`CoinSheet.kt`, `CoinFicha`) sobre la lámina desde la que se abre;
las otras cuatro son la ficha a pantalla entera, que es lo que Jose pidió el 11 de octubre de 2026
(«siéntete libre de hacer un diseño chulo con una vista entera y sacar incluso más detalles»), y
cada una defiende una tesis distinta sobre qué manda:

- A · apilada: un documento largo, nada escondido, las dos caras a la vez.
- B · plegada: cabe en una pantalla; cada sección dice su resumen cerrada.
- C · la escalera: el dinero manda; tres precios con su etiqueta entera y el grado como escala.
- D · a tamaño real: el objeto manda; la moneda a 1:1, su canto y su leyenda, y una cartela.

Los textos de la 0 son los literales del código; los de las otras cuatro, propuestas.

    python3 docs/ux/prototipo-ficha-637/extract.py
    python3 docs/ux/prototipo-ficha-637/build.py
    open /private/tmp/coindex-privado/ficha-637/maqueta.html
"""
import base64
import datetime
import html
import json
import os
import re
from string import Template
from zoneinfo import ZoneInfo

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.abspath(f"{HERE}/../../..")
OUT = "/private/tmp/coindex-privado/ficha-637"

D = json.load(open(f"{OUT}/data.json"))
SPOT = D["spot"]
ZONE = ZoneInfo("Europe/Madrid")
NOW = datetime.datetime(2026, 10, 11, 18, 0, tzinfo=ZONE)

# ── el papel (Theme.kt:24-33, con el gris del #633) ─────────────────────────
INK, MUTED, PAPER = "#2D3029", "#56584C", "#EEE8D7"
DEEP, LINE, MOSS, RUST, HAIR = "#DDD3BB", "#7D806C", "#495C49", "#8B553C", "#878577"
CARD = "rgba(255,252,242,.58)"
SCREEN, TALL, STATUS, NAVBAR = 411, 914, 24, 16
DP_PER_MM = 160 / 25.4

GRADES = ["g", "vg", "f", "vf", "xf", "au", "unc"]
MONTHS = "ene feb mar abr may jun jul ago sep oct nov dic".split()
OZ = 31.1034768


def b64(path, mime):
    return f"data:{mime};base64," + base64.b64encode(open(path, "rb").read()).decode()


def font(name):
    return b64(f"{REPO}/app/src/main/res/font/{name}.ttf", "font/ttf")


PHOTOS = {f[:-4]: b64(f"{OUT}/fotos/{f}", "image/jpeg")
          for f in sorted(os.listdir(f"{OUT}/fotos")) if f.endswith(".jpg")}
CSS_PHOTOS = "\n".join(f'.p-{k}{{background-image:url("{u}")}}' for k, u in PHOTOS.items())


# ── formatos, como FiguresLabels.kt y DateLabels.kt ─────────────────────────
def esc(s):
    return html.escape(str(s), quote=True)


def num(x, d=0):
    s = f"{x:,.{d}f}"
    return s.replace(",", "·").replace(".", ",").replace("·", ".")


def eur(x):
    return f"{num(x)} €"


def day(iso_or_ms):
    if isinstance(iso_or_ms, (int, float)):
        d = datetime.datetime.fromtimestamp(iso_or_ms / 1000, ZONE).date()
    else:
        d = datetime.date.fromisoformat(iso_or_ms)
    return f"{d.day} {MONTHS[d.month - 1]} {d.year}"


def price_age(ms):
    """`priceAgeLabel`: días naturales, y la fecha pasado un mes."""
    read = datetime.datetime.fromtimestamp(ms / 1000, ZONE).date()
    days = (NOW.date() - read).days
    if days <= 0:
        return "hoy"
    if days == 1:
        return "ayer"
    if days < 30:
        return f"hace {days} días"
    return f"el {day(ms)}"


def ficha_age(ms):
    """`fichaAgeLabel`."""
    brought = datetime.datetime.fromtimestamp(ms / 1000, ZONE).date()
    days = (NOW.date() - brought).days
    if days <= 0:
        return "Ficha traída hoy"
    if days == 1:
        return "Ficha traída ayer"
    if days < 30:
        return f"Ficha traída hace {days} días"
    months = max(1, (NOW.year - brought.year) * 12 + NOW.month - brought.month)
    return f"Ficha traída hace {months} {'mes' if months == 1 else 'meses'}"


def spot_stamp():
    t = datetime.datetime.fromtimestamp(SPOT["readAt"] / 1000, ZONE)
    when = f"hoy {t:%H:%M}" if t.date() == NOW.date() else price_age(SPOT["readAt"])
    return f"plata: {num(SPOT['eurPerTroyOunce'], 2)} €/oz · {when}"


def plural(n, one, many):
    return f"{n} {one if n == 1 else many}"


# ── el dinero, como Valuation.kt ───────────────────────────────────────────
def fineness(comp):
    """`silverFineness`: la ley de la plata leída del texto, o nada si no es plata."""
    if not comp or not re.match(r"\s*(plata|silver)", comp.lower()):
        return None
    m = re.search(r"(?:plata|silver)\s*\.?([0-9]{3}(?:[.,][0-9]+)?)", comp.lower())
    return float(m.group(1).replace(",", ".")) / 1000 if m else None


def fine_grams(case):
    f = fineness(case["meta"]["composition"])
    w = case["meta"]["weightGrams"]
    return w * f if f and w else None


def silver_value(case):
    g = fine_grams(case)
    return g / OZ * SPOT["eurPerTroyOunce"] if g else None


def market(p):
    """`marketValue`: su grado, o el vecino más cercano, y a igualdad el peor."""
    grade = (p["grade"] or "unc").lower()
    grade = grade if grade in GRADES else "unc"
    if grade in p["prices"]:
        return dict(eur=p["prices"][grade], grade=grade, neighbour=False)
    i = GRADES.index(grade)
    for j in sorted((j for j in range(len(GRADES)) if j != i), key=lambda j: (abs(j - i), j)):
        if GRADES[j] in p["prices"]:
            return dict(eur=p["prices"][GRADES[j]], grade=GRADES[j], neighbour=True)
    return None


def three(case, p):
    """Los tres precios de una pieza y cuál manda (`pieceValue`: el mayor, en este orden)."""
    q = max(1, p["quantity"])
    rows = [
        ("numista", market(p)),
        ("silver", silver_value(case) and dict(eur=silver_value(case))),
        ("paid", p["paid"] and p["paid"] > 0 and dict(eur=p["paid"] / q)),
    ]
    rows = [(k, v or None) for k, v in rows]
    live = [(k, v) for k, v in rows if v]
    win = max(live, key=lambda kv: kv[1]["eur"])[0] if live else None
    return dict(rows), win


def hole_prices(case):
    h = case["hole"]
    rows = dict(numista=h["prices"].get("unc") and dict(eur=h["prices"]["unc"], grade="unc",
                                                         neighbour=False),
                silver=silver_value(case) and dict(eur=silver_value(case)), paid=None)
    live = [(k, v) for k, v in rows.items() if v]
    return rows, (max(live, key=lambda kv: kv[1]["eur"])[0] if live else None)


def coin_value(case):
    """`coinValue`: la suma de los mayores, con su origen sólo si todas las piezas coinciden."""
    total, n, origins = 0.0, 0, set()
    for p in case["pieces"]:
        rows, win = three(case, p)
        if not win:
            continue
        q = max(1, p["quantity"])
        total += rows[win]["eur"] * q
        n += q
        origins.add((win, rows[win].get("grade"), rows[win].get("neighbour")))
    if not n:
        return None
    return dict(eur=total, pieces=n, origin=origins.pop() if len(origins) == 1 else None)


def origin_label(origin):
    """`FiguresLabels.valueOrigin`."""
    kind, grade, neighbour = origin
    if kind == "numista":
        return f"precio de catálogo en {grade}" + (", el grado vecino" if neighbour else "")
    return {"silver": "su plata", "paid": "lo que pagaste"}[kind]


def coin_value_label(case):
    """`coinValueLabel`, lo que la hoja de hoy dice en rust."""
    v = coin_value(case)
    if not v:
        return None
    parts = [eur(v["eur"])]
    if v["pieces"] > 1:
        parts.append(f"{v['pieces']} piezas")
    if v["origin"]:
        parts.append(origin_label(v["origin"]))
    return " · ".join(parts)


# ── lo que la ficha sabe ────────────────────────────────────────────────────
def years(case):
    ys = sorted({p["year"] for p in case["pieces"] if p["year"]})
    if not ys:
        f = case["ficha"]
        ys = sorted({y for y in (f["min_year"], f["max_year"]) if y})
    return ys


def years_label(ys):
    """`coinYearsLabel`."""
    if not ys:
        return "sin año"
    return str(ys[0]) if len(ys) == 1 else f"{ys[0]} – {ys[-1]}"


def quantity(case):
    return sum(max(1, p["quantity"]) for p in case["pieces"])


def identity(case):
    """`coinFichaIdentity`."""
    f = case["ficha"]
    q = quantity(case)
    return " · ".join(x for x in [
        f["issuer"], years_label(years(case)), f"N# {case['tid']}",
        "Medalla o ficha" if f["category"] == "exonumia" else None,
        f"×{q}" if q > 1 else None,
    ] if x)


def clean(s):
    return re.sub(r"^\(en\)\s*", "", s or "").replace("\r\n", "\n").strip()


def first_sentence(s, n=96):
    s = re.sub(r"<[^>]+>", " ", s or "")
    s = re.sub(r"\s+", " ", html.unescape(s)).strip()
    s = clean(s)
    return s if len(s) <= n else s[:n].rsplit(" ", 1)[0] + "…"


def prose(comments):
    """El HTML de Numista reducido a párrafos, negritas y enlaces que salen de la app."""
    s = clean(comments or "")
    s = s.replace("\r\n", " ")
    s = re.sub(r"<br\s*/?>\s*<br\s*/?>", "</p><p>", s)
    s = re.sub(r"<a [^>]*href=\"([^\"]+)\"[^>]*>(.*?)</a>",
               lambda m: f'<span class="ext">{m.group(2)}</span>', s)
    s = re.sub(r"<(?!/?(p|strong|br|span)[ >/])[^>]+>", "", s)
    s = re.sub(r"<p>\s*</p>", "", s)
    if not s.startswith("<p>"):
        s = f"<p>{s}</p>"
    return s


def orientation(o):
    return {"medal": "medalla ↑↑", "coin": "moneda ↑↓"}.get(o, o)


def legal(f):
    d = f["demonetization"] or {}
    if d.get("is_demonetized") is True:
        return f"desmonetizada el {day(d['demonetization_date'])}" if d.get(
            "demonetization_date") else "desmonetizada"
    if d.get("is_demonetized") is False:
        return "sigue siendo dinero"
    return None


def spec(case):
    """Las filas de la cartela técnica, sólo las que la ficha trae."""
    f = case["ficha"]
    g = fine_grams(case)
    hands = sorted(set((f["obverse"].get("engravers") or []) + (f["obverse"].get("designers") or [])
                       + (f["reverse"].get("engravers") or []) + (f["reverse"].get("designers") or [])))
    rows = [
        ("Valor facial", f["value"] and f"{f['value']}"),
        ("Moneda", f["currency"]),
        ("Gobernante", ", ".join(f["rulers"]) or None),
        ("Serie", f["series"]),
        ("Conmemora", clean(f["topic"]) or None),
        ("Años", f["min_year"] and years_label([f["min_year"], f["max_year"]])
         if f["min_year"] != f["max_year"] else f["min_year"] and str(f["min_year"])),
        ("Ceca", ", ".join(f["mints"]) or None),
        ("Metal", f["composition"] and re.sub(r"\s*\(.*$", "", f["composition"])),
        ("Peso", f["weight"] and f"{num(f['weight'], 2)} g"),
        ("Plata fina", g and f"{num(g, 2)} g · {num(g / OZ, 2)} oz"),
        ("Diámetro", f["size"] and f"{num(f['size'], 2).rstrip('0').rstrip(',')} mm"),
        ("Grosor", f["thickness"] and f"{num(f['thickness'], 1)} mm"),
        ("Canto", f["edge"]["description"] and {"Reed": "estriado"}.get(
            f["edge"]["description"], f["edge"]["description"])),
        ("Orientación", f["orientation"] and orientation(f["orientation"])),
        ("Técnica", f["technique"]),
        ("Curso legal", legal(f)),
        ("Grabadores", ", ".join(hands) or None),
        ("Referencias", " · ".join(f["references"]) or None),
    ]
    return [(k, v) for k, v in rows if v]


# ── piezas de cromo ─────────────────────────────────────────────────────────
def photo_key(tid, side, mini=False):
    k = f"{tid}-{side}" + ("-mini" if mini else "")
    return k if k in PHOTOS else None


def hole(tid, size, side="reverse", missing=False, lit=False, flip=False, other="obverse",
         ring=None, mini=False, extra=""):
    """El hueco troquelado de `AlbumPaper.kt`; con `flip`, las dos caras y el giro al tocarlo."""
    r = ring if ring is not None else max(2, round(5 * size / 104))
    cls = "hole" + (" miss" if missing else "") + (" lit" if lit else "") + (" flip" if flip else "")
    a = photo_key(tid, side, mini) or photo_key(tid, other, mini)
    face = lambda k, back="": (f'<i class="face{back} p-{k}" style="inset:{r}px"></i>' if k
                               else f'<i class="face{back}" style="inset:{r}px"></i>')
    inner = face(a)
    if flip:
        b = photo_key(tid, other, mini)
        inner += face(b, " rev")
    title = ' title="Tócala para darle la vuelta" role="button" tabindex="0"' if flip else ""
    return (f'<div class="{cls}" style="width:{size}px;height:{size}px"{title}>'
            f'{inner}{extra}</div>')


def eyebrow(text, sec=None):
    attr = f' data-sec="{esc(sec or text)}"' if True else ""
    return f'<div class="eyebrow"{attr}>{esc(text)}</div>'


def spec_card(rows, cls=""):
    body = "".join(f'<div class="srow"><span class="sl">{esc(k)}</span>'
                   f'<span class="sv">{esc(v)}</span></div>' for k, v in rows)
    return f'<div class="fcard spec {cls}">{body}</div>'


def ext(text, cls="ext"):
    return f'<span class="{cls}">{esc(text)}<u class="arrow"></u></span>'


def back_row(case):
    where = case["plate"]["name"] if case["plate"] else "Monedas"
    return (f'<div class="backrow"><u class="back"></u><span>{esc(where)}</span></div>')


def footer(case, center=False):
    f = case["ficha"]
    c = " center" if center else ""
    return (f'<div class="foot{c}"><div class="age">{ficha_age(f["fetched_at"])}</div>'
            f'<div class="cardaction">Actualizar la ficha · 1 consulta</div>'
            f'{ext("Ver en Numista")}</div>')


def lettering(text):
    return esc((text or "").replace("\r\n", "\n").strip())


def faces(case, compact=False, legends=True):
    f = case["ficha"]
    out = []
    for name, side in (("Anverso", "obverse"), ("Reverso", "reverse")):
        s = f[side]
        if not s:
            continue
        hands = (s.get("engravers") or []) + (s.get("designers") or [])
        out.append(
            f'<div class="faceblock"><div class="fhead">{name}</div>'
            + (f'<div class="legend">{lettering(s.get("lettering"))}</div>' if s.get("lettering") and legends else "")
            + (f'<div class="unab">{esc(s["unabridged_legend"].replace(chr(13) + chr(10), " · "))}</div>'
               if s.get("unabridged_legend") and legends else "")
            + (f'<p class="desc">{esc(clean(s.get("description")))}</p>' if s.get("description") and not compact else "")
            + (f'<div class="hands">{esc(", ".join(hands))}</div>' if hands and not compact else "")
            + "</div>")
    e = f["edge"]
    if e.get("lettering"):
        out.append(f'<div class="faceblock"><div class="fhead">Canto</div>'
                   f'<div class="legend">{lettering(e["lettering"])}</div>'
                   f'<p class="desc">{esc(e["description"])}</p></div>')
    return "".join(out)


def history(case):
    f = case["ficha"]
    return f'<div class="prose">{prose(f["comments"])}</div>' if f["comments"] else ""


def related(case):
    rows = case["ficha"]["related"]
    if not rows:
        return ""
    body = "".join(
        f'<div class="rel"><span class="relc">{esc(r["country"])}</span>'
        f'{ext(r["title"])}<span class="rely">{esc(years_label([y for y in r["years"] if y]))}</span></div>'
        for r in rows)
    return body


def strip(case):
    """La lámina de aquí en miniatura: sus casillas, las que tiene y ésta marcada."""
    p = case["plate"]
    if not p:
        return ""
    side = "obverse" if p["printed_side"] == "obverse" else "reverse"
    big = len(p["cells"]) <= 6
    size = 62 if big else 34
    cells = []
    for c in p["cells"]:
        here = c["this"] and (case.get("year") is None or c["year"] == case.get("year"))
        h = hole(c["tid"], size, side=side, missing=not c["owned"], mini=True, ring=2 if not big else 3)
        label = (f'<span class="clab">{esc(c["label"])}</span>' if big
                 else (f'<span class="clab y">{c["year"]}</span>' if p["id"] == "venezuela-fuertes" else ""))
        cells.append(f'<div class="cell{" here" if here else ""}{" big" if big else ""}">{h}{label}</div>')
    owned = sum(c["owned"] for c in p["cells"])
    return (f'<div class="strip{" big" if big else ""}">{"".join(cells)}</div>'
            f'<div class="stripnote">{owned} de {len(p["cells"])}</div>')


def claims_block(case):
    """«En esta colección», con la de aquí sin enlace y su «· aquí» (#635)."""
    names = case["claims"]
    if not names:
        return ""
    here = case["plate"]["name"] if case["plate"] else None
    head = "En esta colección" if len(names) == 1 else "En estas colecciones"
    rows = "".join(
        (f'<div class="claim"><span class="cl here">{esc(n)}</span><span class="aqui"> · aquí</span></div>'
         if n == here else f'<div class="claim"><span class="cl link">{esc(n)}</span></div>')
        for n in names)
    return head, rows


def note(case):
    p = case["plate"]
    return (f'<div class="notelab">Nota del catálogo</div><blockquote class="note">{esc(p["note"])}</blockquote>'
            if p and p["note"] else "")


def piece_head(p):
    bits = [str(p["year"]) if p["year"] else "sin año"]
    if p["mint_letter"]:
        bits.append(p["mint_letter"])
    head = " · ".join(bits)
    grade = f'<span class="grade">{esc(p["grade"])}</span>' if p["grade"] else ""
    q = f'<span class="qty">×{p["quantity"]}</span>' if p["quantity"] > 1 else ""
    return f'<span class="pyear">{esc(head)}</span>{grade}{q}'


def piece_facts(p):
    facts = []
    if p["paid"] and p["acquired"]:
        facts.append(f"pagaste {eur(p['paid'])} el {day(p['acquired'])}")
    elif p["paid"]:
        facts.append(f"pagaste {eur(p['paid'])}")
    elif p["acquired"]:
        facts.append(f"llegó el {day(p['acquired'])}")
    if p["mintage"]:
        facts.append(f"tirada de {num(p['mintage'])}")
    if p["swap"]:
        facts.append("para cambio")
    return facts


def piece_photo(p, size=44):
    if not p["pictures"]:
        return ""
    k = f"pieza-{p['id']}-0"
    return (f'<div class="pphoto p-{k}" style="width:{size}px;height:{round(size * 1.41)}px" '
            f'title="Tu foto en Numista"></div>') if k in PHOTOS else ""


def money_rows(case, rows, win, hole_mode=False):
    """La escalera: las tres cifras con su etiqueta entera y su procedencia pegada."""
    out = []
    labels = dict(numista="Catálogo de Numista", paid="Lo que pagaste", silver="Lo que vale su plata")
    order = ["numista", "paid", "silver"] if not hole_mode else ["numista", "silver"]
    top = max([v["eur"] for v in rows.values() if v] or [1])
    for k in order:
        v = rows.get(k)
        lab = labels[k]
        if k == "numista" and v:
            lab += f' en <span class="g">{v["grade"]}</span>' + (" (el grado vecino)" if v["neighbour"] else "")
        if v:
            w = max(4, round(100 * v["eur"] / top))
            src = {"numista": None, "paid": None, "silver": spot_stamp()}[k]
            if k == "silver":
                src = f"{num(fine_grams(case), 2)} g finos · {spot_stamp()}"
            out.append(
                f'<div class="rung{" win" if k == win else ""}">'
                f'<div class="rl"><span class="rlab">{lab}</span><span class="ramt">{eur(v["eur"])}</span></div>'
                f'<div class="bar"><i style="width:{w}%"></i></div>'
                + (f'<div class="rsrc">{esc(src)}</div>' if src else "") + "</div>")
        else:
            why = {"numista": "Numista no le pone precio",
                   "paid": "no anotaste lo que pagaste",
                   "silver": "no lleva plata"}[k]
            out.append(f'<div class="rung none"><div class="rl"><span class="rlab">{labels[k]}</span>'
                       f'<span class="rnone">{why}</span></div><div class="bar dashed"></div></div>')
    return "".join(out)


def numista_stamp(case, p=None):
    reads = [x["read"]["at"] for x in ([p] if p else case["pieces"])
             if x.get("read") and x["read"]["has"]]
    if case.get("hole") and case["hole"]["read"] and case["hole"]["read"]["has"]:
        reads.append(case["hole"]["read"]["at"])
    return f"Numista: {price_age(min(reads))}" if reads else None


def stamps(case):
    """Una cláusula por reloj (#594): Numista si algún precio sale de ahí, la plata si la lleva."""
    parts = [x for x in (numista_stamp(case), silver_value(case) and spot_stamp()) if x]
    return f'<div class="stamp">{esc(" · ".join(parts))}</div>' if parts else ""


def grade_chart(prices, mine, w=371, label="tu pieza"):
    """El precio de la emisión en cada grado que Numista publica, y el de la pieza marcado."""
    top = max(prices.values()) if prices else 1
    cols = []
    for g in GRADES:
        v = prices.get(g)
        h = round(52 * v / top) if v else 0
        cls = "gcol" + (" mine" if g == mine else "") + ("" if v else " empty")
        cols.append(
            f'<div class="{cls}"><span class="gv">{eur(v) if v else "—"}</span>'
            f'<div class="gb"><i style="height:{max(h, 2) if v else 52}px"></i></div>'
            f'<span class="gg">{g}</span></div>')
    return (f'<div class="gchart">{"".join(cols)}</div>'
            f'<div class="gnote"><b></b>{esc(label)}</div>')


# ── la 0: la hoja de hoy ────────────────────────────────────────────────────
def plate_background(case):
    """La lámina de debajo, o Monedas si se abre desde ahí; sólo de fondo bajo el velo."""
    p = case["plate"]
    if not p:
        holes = "".join(
            f'<div class="pcell">{hole(c["tid"], 104, side="reverse")}'
            f'<div class="pname">{esc(first_sentence(c["ficha"]["title"], 40))}</div></div>'
            for c in D["cases"] if c["pieces"])
        return (f'<div class="chrome"><b>COINDEX</b><span>monedas</span></div>'
                f'<div class="pgrid">{holes}</div>')
    side = "obverse" if p["printed_side"] == "obverse" else "reverse"
    cells = "".join(
        f'<div class="pcell">{hole(c["tid"], 104, side=side, missing=not c["owned"], mini=True)}'
        f'<div class="pname">{esc(c["label"])}</div></div>' for c in p["cells"])
    owned = sum(c["owned"] for c in p["cells"])
    return (f'<div class="plate"><div class="eyebrow">Catálogo curado</div>'
            f'<div class="phead"><span>{esc(p["name"])}</span><b>{owned}/{len(p["cells"])}</b></div>'
            f'<div class="pgrid">{cells}</div></div>')


def v0(case):
    f = case["ficha"]
    missing = not case["pieces"]
    value = coin_value_label(case)
    # `row.claims.filterNot { it.destination == here }` (CoinSheet.kt:129)
    here = case["plate"]["name"] if case["plate"] else None
    doors = [n for n in case["claims"] if n != here]
    doors_html = ""
    if doors:
        head = "En esta colección" if len(doors) == 1 else "En estas colecciones"
        doors_html = (f'<div class="s0doors">{head}</div>'
                      + "".join(f'<div class="s0link">{esc(n)}</div>' for n in doors))
    side = "obverse" if case["plate"] and case["plate"]["printed_side"] == "obverse" else "reverse"
    sheet = (
        f'<div class="sheet" data-sec="la hoja">'
        f'<div class="s0hole">{hole(case["tid"], 104, side=side, missing=missing, flip=True, other="obverse" if side == "reverse" else "reverse")}</div>'
        f'<div class="s0title">{esc(f["title"])}</div>'
        f'<div class="s0id">{esc(identity(case))}</div>'
        + (f'<div class="s0val">{esc(value)}</div>' if value else "")
        + f'<div class="s0age">{ficha_age(f["fetched_at"])}</div>'
        f'<div class="cardaction">Actualizar la ficha · 1 consulta</div>'
        f'<div class="s0num">{ext("Ver en Numista")}</div>'
        f'{doors_html}</div>')
    return (f'<div class="scroll bg">{plate_background(case)}</div>'
            f'<div class="scrim"></div>{sheet}')


# ── A · apilada ─────────────────────────────────────────────────────────────
def value_head(case, cls="vhead"):
    if case["pieces"]:
        v = coin_value(case)
        if not v:
            return f'<div class="{cls} muted">Sin precio: ni Numista, ni lo que pagaste, ni plata</div>'
        src = origin_label(v["origin"]) if v["origin"] else "al mayor de tres precios"
        return f'<div class="{cls}">Valor actual: {eur(v["eur"])} · {esc(src)}</div>'
    rows, win = hole_prices(case)
    return f'<div class="{cls}">Te costaría: {eur(rows[win]["eur"])} · en sin circular</div>'


def piece_prices_line(case, p):
    rows, win = three(case, p)
    bits = []
    for k, lab in (("numista", "Numista"), ("paid", "pagaste"), ("silver", "plata")):
        v = rows[k]
        if not v:
            continue
        g = f' en <span class="g">{v["grade"]}</span>' if k == "numista" else ""
        bits.append(f'<span class="pp{" win" if k == win else ""}">{lab}{g} {eur(v["eur"])}</span>')
    return f'<div class="pprices">{"".join(bits)}</div>'


def pieces_list(case, prices=True, notes=True):
    out = []
    for p in case["pieces"]:
        here = case.get("year") and p["year"] == case["year"]
        facts = piece_facts(p)
        out.append(
            f'<div class="piece{" here" if here else ""}"><div class="pmain">'
            f'<div class="phead">{piece_head(p)}'
            + ('<span class="aqui">· esta casilla</span>' if here else "") + "</div>"
            + (f'<div class="pfacts">{esc(" · ".join(facts))}</div>' if facts else "")
            + (piece_prices_line(case, p) if prices else "")
            + (f'<div class="pissue">Numista: {esc(p["issue_comment"])}</div>'
               if p["issue_comment"] and notes else "")
            + (f'<div class="pnote">«{esc(p["note"])}»</div>' if p["note"] and notes else "")
            + f'</div>{piece_photo(p)}</div>')
    return "".join(out)


def vA(case):
    f = case["ficha"]
    missing = not case["pieces"]
    side_l, side_r = "obverse", "reverse"
    heroes = (f'<div class="pair">'
              f'<figure>{hole(case["tid"], 168, side=side_l, missing=missing, lit=missing)}<figcaption>anverso</figcaption></figure>'
              f'<figure>{hole(case["tid"], 168, side=side_r, missing=missing, lit=missing)}<figcaption>reverso</figcaption></figure></div>')
    sub = " · ".join(x for x in [f["value"], ", ".join(f["rulers"]) or None, clean(f["topic"]) or None] if x)
    claims = claims_block(case)
    parts = [
        back_row(case), heroes,
        f'<h1 class="h1">{esc(f["title"])}</h1>',
        f'<div class="sub">{esc(sub)}</div>' if sub else "",
        f'<div class="idl">{esc(identity(case))}</div>',
        '<div class="falta">Te falta</div>' if missing else "",
        f'<section>{eyebrow("Lo que vale" if not missing else "Lo que costaría")}{value_head(case)}'
        + stamps(case)
        + "</section>",
    ]
    if case["pieces"]:
        parts.append(f'<section>{eyebrow("Tus piezas" if quantity(case) > 1 else "Tu pieza")}'
                     f'{pieces_list(case)}</section>')
    else:
        rows, win = hole_prices(case)
        parts.append(f'<section class="tight">{money_rows(case, rows, win, hole_mode=True)}</section>')
    if claims:
        head, rows = claims
        parts.append(f'<section>{eyebrow(head)}{rows}{strip(case)}{note(case)}</section>')
    parts += [
        f'<section>{eyebrow("La medalla" if f["category"] == "exonumia" else "La moneda")}{spec_card(spec(case))}</section>',
        f'<section>{eyebrow("Las caras")}{faces(case)}</section>',
        f'<section>{eyebrow("Lo que cuenta Numista")}{history(case)}</section>' if f["comments"] else "",
        f'<section>{eyebrow("Emparentadas en Numista")}{related(case)}</section>' if f["related"] else "",
        footer(case),
    ]
    return f'<div class="scroll page">{"".join(parts)}</div>'


# ── B · plegada ─────────────────────────────────────────────────────────────
def fold(title, summary, body, open_=False, sec=None):
    return (f'<details class="fold"{" open" if open_ else ""} data-sec="{esc(sec or title)}">'
            f'<summary><span class="ft">{esc(title)}</span><span class="fs">{summary}</span>'
            f'<u class="chev"></u></summary><div class="fb">{body}</div></details>')


def vB(case):
    f = case["ficha"]
    missing = not case["pieces"]
    head = (f'<div class="bhead">{hole(case["tid"], 116, side="reverse", missing=missing, lit=missing, flip=True)}'
            f'<div class="bcol"><div class="btitle">{esc(f["title"])}</div>'
            f'<div class="idl">{esc(identity(case))}</div>'
            + ('<div class="falta">Te falta</div>' if missing else "")
            + f'{value_head(case, "vline")}</div></div>')
    folds = []
    if case["pieces"]:
        v = coin_value(case)
        n = quantity(case)
        ys = years(case)
        if len(case["pieces"]) <= 3:
            summ = " · ".join(f'{p["year"] or "sin año"}{(" " + p["mint_letter"]) if p["mint_letter"] else ""} '
                              f'<span class="g">{p["grade"] or ""}</span>' for p in case["pieces"])
        else:
            summ = f'{plural(len(case["pieces"]), "fila", "filas")} · {years_label(ys)}'
        folds.append(fold(f'Tus piezas · {n}' if n > 1 else "Tu pieza", summ, pieces_list(case), sec="Tus piezas"))
        if len(case["pieces"]) == 1:
            rows, win = three(case, case["pieces"][0])
            folds.append(fold("Los tres precios", esc(origin_label((win, rows[win].get("grade"), rows[win].get("neighbour"))) if win else ""),
                              money_rows(case, rows, win)))
    else:
        rows, win = hole_prices(case)
        folds.append(fold("Los dos precios", f"Numista en <span class=\"g\">unc</span> · su plata",
                          money_rows(case, rows, win, hole_mode=True)))
    claims = claims_block(case)
    if claims:
        h, rows_html = claims
        p = case["plate"]
        owned = sum(c["owned"] for c in p["cells"]) if p else 0
        summ = esc(" · ".join(case["claims"])) + (f' · {owned} de {len(p["cells"])}' if p else "")
        folds.append(fold(h, summ, rows_html + strip(case) + note(case)))
    s = dict(spec(case))
    summ = " · ".join(x for x in [s.get("Metal"), s.get("Peso"), s.get("Diámetro"), s.get("Ceca")] if x)
    folds.append(fold("La medalla" if f["category"] == "exonumia" else "La moneda", esc(summ), spec_card(spec(case))))
    leg = f["obverse"].get("lettering") or f["reverse"].get("lettering") or ""
    folds.append(fold("Las caras", f'<span class="caps">{esc(first_sentence(leg, 44))}</span>', faces(case)))
    if f["comments"]:
        folds.append(fold("Lo que cuenta Numista", esc(first_sentence(f["comments"], 70)), history(case)))
    if f["related"]:
        folds.append(fold(f"Emparentadas · {len(f['related'])}",
                          esc(", ".join(r["country"] for r in f["related"])), related(case)))
    return (f'<div class="scroll page">{back_row(case)}{head}<div class="folds">{"".join(folds)}</div>'
            f'{footer(case)}</div>')


# ── C · la escalera ─────────────────────────────────────────────────────────
def ledger_table(case):
    rows = []
    for p in case["pieces"]:
        r, win = three(case, p)
        here = case.get("year") and p["year"] == case["year"]
        cell = lambda k: (f'<td class="{"win" if k == win else ""}">{eur(r[k]["eur"])}</td>' if r[k]
                          else '<td class="nil">—</td>')
        rows.append(f'<tr class="{"here" if here else ""}"><th>{p["year"] or "—"}'
                    f'{(" ×" + str(p["quantity"])) if p["quantity"] > 1 else ""}</th>'
                    f'<td class="g">{p["grade"] or ""}</td>{cell("numista")}{cell("paid")}{cell("silver")}</tr>')
    return ('<table class="ledger"><thead><tr><th>año</th><th>grado</th><th>Numista</th>'
            '<th>pagaste</th><th>plata</th></tr></thead><tbody>' + "".join(rows) + "</tbody></table>")


def vC(case):
    f = case["ficha"]
    missing = not case["pieces"]
    hero = (f'<div class="chero">{hole(case["tid"], 196, side="reverse", missing=missing, lit=missing, flip=True)}'
            f'<div class="hint">tócala para darle la vuelta</div></div>')
    parts = [back_row(case), hero,
             f'<h1 class="h1 c">{esc(f["title"])}</h1>',
             f'<div class="idl c">{esc(identity(case))}</div>']
    if missing:
        rows, win = hole_prices(case)
        parts.append(f'<section>{eyebrow("Lo que costaría")}{value_head(case, "vbig")}'
                     f'<div class="ladder">{money_rows(case, rows, win, hole_mode=True)}</div>'
                     f'<div class="stamp">{esc(numista_stamp(case))}</div>'
                     f'<div class="sub2">El precio de esta emisión en cada grado</div>'
                     f'{grade_chart(case["hole"]["prices"], "unc", label="lo que se pide a una casilla: sin circular")}</section>')
    elif len(case["pieces"]) <= 3:
        parts.append(f'<section>{eyebrow("Lo que vale")}{value_head(case, "vbig")}</section>')
        for p in case["pieces"]:
            rows, win = three(case, p)
            facts = piece_facts(p)
            parts.append(
                f'<section class="pc" data-sec="pieza {p["year"]}"><div class="phead big">{piece_head(p)}</div>'
                + (f'<div class="pfacts">{esc(" · ".join(facts))}</div>' if facts else "")
                + f'<div class="ladder">{money_rows(case, rows, win)}</div>'
                + (f'<div class="stamp">{esc(numista_stamp(case, p))}</div>' if numista_stamp(case, p) else "")
                + (f'<div class="sub2">Esta emisión en cada grado</div>{grade_chart(p["prices"], (p["grade"] or "unc").lower())}'
                   if p["prices"] else "")
                + (f'<div class="pnote">«{esc(p["note"])}»</div>' if p["note"] else "")
                + "</section>")
    else:
        parts.append(f'<section>{eyebrow("Lo que vale")}{value_head(case, "vbig")}'
                     f'{stamps(case)}'
                     f'<div class="sub2">Pieza a pieza: manda la cifra en rojo</div>{ledger_table(case)}</section>')
    claims = claims_block(case)
    if claims:
        h, rows_html = claims
        parts.append(f'<section>{eyebrow(h)}{rows_html}{strip(case)}</section>')
    parts.append(f'<section>{eyebrow("La medalla" if f["category"] == "exonumia" else "La moneda")}{spec_card(spec(case))}</section>')
    parts.append(f'<section>{eyebrow("Las caras")}{faces(case, compact=True)}</section>')
    if f["comments"]:
        parts.append(f'<section>{eyebrow("Lo que cuenta Numista")}{history(case)}</section>')
    parts.append(footer(case))
    return f'<div class="scroll page">{"".join(parts)}</div>'


# ── D · a tamaño real ───────────────────────────────────────────────────────
def ruler(mm):
    """Una regla en milímetros debajo de la moneda, a la misma escala: 1 mm = 6,3 dp."""
    w = mm * DP_PER_MM
    ticks = []
    for i in range(int(mm) + 1):
        x = round(i * DP_PER_MM, 2)
        h = 10 if i % 10 == 0 else 6 if i % 5 == 0 else 4
        ticks.append(f'<line x1="{x}" y1="0" x2="{x}" y2="{h}"/>')
        if i % 10 == 0:
            ticks.append(f'<text x="{x}" y="22">{i}</text>')
    return (f'<svg class="ruler" width="{round(w) + 16}" height="26" viewBox="-8 0 {round(w) + 16} 26">'
            f'<line x1="0" y1="0" x2="{round(w, 2)}" y2="0"/>{"".join(ticks)}</svg>')


def edge_profile(case):
    f = case["ficha"]
    if not f["size"]:
        return ""
    w = f["size"] * DP_PER_MM
    t = (f["thickness"] or 2.2) * DP_PER_MM
    reed = (f["edge"]["description"] or "").lower() in ("reed", "estriado")
    cls = "edge" + (" reed" if reed else "") + ("" if f["thickness"] else " guess")
    return f'<div class="{cls}" style="width:{w:.1f}px;height:{t:.1f}px"></div>'


def vD(case):
    f = case["ficha"]
    missing = not case["pieces"]
    size = (f["size"] or 30) * DP_PER_MM
    leg_o = f["obverse"].get("lettering")
    leg_r = f["reverse"].get("lettering")
    hero = (f'<div class="dhero">'
            f'{hole(case["tid"], round(size + 12), side="obverse", missing=missing, lit=missing, flip=True, other="reverse", ring=6)}'
            f'{ruler(f["size"] or 30)}'
            f'<div class="dcap">a tamaño real · {num(f["size"], 2).rstrip("0").rstrip(",")} mm'
            + (f' · {num(f["thickness"], 1)} mm de canto' if f["thickness"] else "") + "</div>"
            f'{edge_profile(case)}</div>')
    legend = ""
    if leg_o or leg_r:
        legend = (f'<div class="dleg" data-sec="la leyenda">'
                  + (f'<div class="legend big">{lettering(leg_o)}</div>' if leg_o else "")
                  + (f'<div class="dsep">·</div><div class="legend big">{lettering(leg_r)}</div>' if leg_r else "")
                  + "</div>")
    s = dict(spec(case))
    place = " · ".join(x for x in [f["issuer"], ", ".join(f["rulers"]) or None] if x)
    label = (f'<div class="cartela" data-sec="la cartela">'
             f'<div class="ctitle">{esc(f["title"])}</div>'
             f'<div class="cplace">{esc(place)}{(" · " + esc(years_label(years(case)))) if years(case) else ""}</div>'
             f'<div class="cmat">{esc(" · ".join(x for x in [s.get("Metal"), s.get("Peso"), s.get("Ceca")] if x))}</div>'
             + (f'<p class="cdesc">{esc(clean(f["reverse"].get("description") or f["obverse"].get("description")))}</p>')
             + f'<div class="cref">N# {case["tid"]}{(" · " + esc(s["Referencias"])) if s.get("Referencias") else ""}</div></div>')
    parts = [back_row(case), hero, legend, label]
    if missing:
        rows, win = hole_prices(case)
        parts.append(f'<section>{eyebrow("Te falta")}{value_head(case)}'
                     f'<div class="ladder">{money_rows(case, rows, win, hole_mode=True)}</div></section>')
    else:
        reg = []
        for i, p in enumerate(case["pieces"], 1):
            facts = piece_facts(p)
            here = case.get("year") and p["year"] == case["year"]
            reg.append(f'<div class="reg{" here" if here else ""}"><span class="rn">{i}</span><div class="rb">'
                       f'<div class="phead">{piece_head(p)}</div>'
                       + (f'<div class="pfacts">{esc(" · ".join(facts))}</div>' if facts else "")
                       + piece_prices_line(case, p)
                       + (f'<div class="pnote">«{esc(p["note"])}»</div>' if p["note"] else "")
                       + f'</div>{piece_photo(p, 40)}</div>')
        parts.append(f'<section>{eyebrow("El registro de tus piezas")}{value_head(case)}{"".join(reg)}</section>')
    claims = claims_block(case)
    if claims:
        h, rows_html = claims
        parts.append(f'<section>{eyebrow(h)}{rows_html}{strip(case)}{note(case)}</section>')
    parts.append(f'<section>{eyebrow("Ficha técnica")}{spec_card(spec(case))}</section>')
    parts.append(f'<section>{eyebrow("Las caras")}{faces(case, legends=False)}</section>')
    if f["comments"]:
        parts.append(f'<section>{eyebrow("Lo que cuenta Numista")}{history(case)}</section>')
    if f["related"]:
        parts.append(f'<section>{eyebrow("Emparentadas en Numista")}{related(case)}</section>')
    parts.append(footer(case))
    return f'<div class="scroll page">{"".join(parts)}</div>'


VARIANTS = {
    "0": ("Hoy", v0, "La hoja de hoy (v1.7.4 con el #632): sube desde abajo sobre la lámina. "
          "Un valor, la ficha traída y las colecciones menos la de aquí."),
    "A": ("Apilada", vA, "Pantalla entera y un solo documento: las dos caras a la vez, el valor, "
          "tus piezas con sus tres precios, la lámina en miniatura con la nota del catálogo, la "
          "cartela técnica, las leyendas, lo que cuenta Numista y las emparentadas. Nada plegado."),
    "B": ("Plegada", vB, "Cabe en una pantalla. Cabecera con la moneda, el valor y la identidad; "
          "debajo, una fila por sección que dice su resumen cerrada y se abre al tocarla."),
    "C": ("La escalera", vC, "Manda el dinero. Cada pieza con sus tres precios en escalera, la "
          "etiqueta entera y la procedencia pegada, y el precio de su emisión en cada grado con el "
          "suyo marcado. Con muchas piezas, una tabla."),
    "D": ("A tamaño real", vD, "Manda el objeto. La moneda a 1:1 con su regla y su canto, la "
          "leyenda transcrita, una cartela de museo y el registro de tus piezas."),
}

SLOTS = "".join(
    f'<div class="slot" data-v="{v}" data-c="{c["key"]}" hidden>'
    f'<div class="vtag">{v} · {esc(name)}</div>'
    f'<div class="phone" id="ph-{v}-{c["key"]}"><div class="status">18:00</div>'
    f'<div class="screen">{fn(c)}</div><div class="navbar"></div></div></div>'
    for v, (name, fn, _) in VARIANTS.items() for c in D["cases"])

VTABS = "".join(f'<button data-axis="v" data-k="{k}">{k} · {esc(n)}</button>'
                for k, (n, _, _) in VARIANTS.items())
CTABS = "".join(f'<button data-axis="c" data-k="{c["key"]}">{esc(c["name"])}</button>'
                for c in D["cases"])
MTABS = ('<button data-axis="m" data-k="tel">Teléfono</button>'
         '<button data-axis="m" data-k="tira">Tira entera</button>'
         '<button data-axis="m" data-k="todas">Las cinco a la vez</button>')
THESES = json.dumps({k: d for k, (_, _, d) in VARIANTS.items()}, ensure_ascii=False)
CASES_NOTE = json.dumps({c["key"]: f'{c["ficha"]["title"]} · N# {c["tid"]} · '
                         + (f'abierta desde «{c["plate"]["name"]}»' if c["plate"] else "abierta desde Monedas")
                         for c in D["cases"]}, ensure_ascii=False)

CSS = Template("""
/* La ficha de una moneda en cinco formas, sobre el papel de Coindex: el marco sigue el tema del
   visor y el teléfono es siempre papel, como la app (ADR 0026 §2). */
:root{
  --bg:#E6E2D6; --fg:#24261F; --soft:#5B5D52; --btn:#F6F2E7; --btn-fg:#2D3029; --btn-line:#B9B4A3;
  --on:#2D3029; --on-fg:#EEE8D7; --shadow:rgba(45,48,41,.28); --code:#DCD6C6;
}
@media (prefers-color-scheme: dark){:root:not([data-theme="light"]){
  --bg:#1B1C19; --fg:#E9E3D1; --soft:#B8B3A4; --btn:#2C2E28; --btn-fg:#E8E2D2; --btn-line:#494C41;
  --on:#EEE8D7; --on-fg:#2D3029; --shadow:rgba(0,0,0,.55); --code:#26271F; color-scheme:dark}}
:root[data-theme="dark"]{
  --bg:#1B1C19; --fg:#E9E3D1; --soft:#B8B3A4; --btn:#2C2E28; --btn-fg:#E8E2D2; --btn-line:#494C41;
  --on:#EEE8D7; --on-fg:#2D3029; --shadow:rgba(0,0,0,.55); --code:#26271F; color-scheme:dark}
@font-face{font-family:Bitter;src:url($BITTER) format('truetype')}
@font-face{font-family:Barlow;font-weight:400;src:url($BARLOW4) format('truetype')}
@font-face{font-family:Barlow;font-weight:600;src:url($BARLOW6) format('truetype')}
*{box-sizing:border-box;margin:0;padding:0}
[hidden]{display:none!important}
body{background:var(--bg);color:var(--fg);font:13px/1.5 -apple-system,system-ui,sans-serif;
  padding-block:0 48px}
header{position:sticky;top:env(safe-area-inset-top,0px);z-index:9;background:var(--bg);
  border-bottom:1px solid var(--btn-line);padding:8px 16px;display:flex;flex-direction:column;gap:6px}
header h1{font:600 13px/1.2 -apple-system,system-ui,sans-serif;letter-spacing:.02em}
header h1 span{font-weight:400;color:var(--soft)}
header .row{display:flex;gap:6px;flex-wrap:wrap;align-items:center}
header .row>em{font-style:normal;color:var(--soft);font-size:11px;width:58px;letter-spacing:.04em}
button{font:600 12px/1 -apple-system,system-ui,sans-serif;padding:7px 9px;cursor:pointer;
  background:var(--btn);color:var(--btn-fg);border:1px solid var(--btn-line);border-radius:3px}
button.on{background:var(--on);color:var(--on-fg);border-color:var(--on)}
button:focus-visible,[role=button]:focus-visible,summary:focus-visible{outline:2px solid $RUST;outline-offset:2px}
.stagewrap{overflow-x:auto;padding:16px 16px 0}
.stage{display:flex;gap:18px;justify-content:center;width:max-content;min-width:100%}
.slot{display:flex;flex-direction:column;gap:6px;align-items:flex-start}
.vtag{font:600 11px/1 -apple-system,system-ui,sans-serif;color:var(--soft);letter-spacing:.05em;
  text-transform:uppercase;display:none}
body.todas .vtag{display:block}
.cap{max-width:640px;margin:14px auto 0;padding:0 16px;font-size:13px;line-height:1.55;color:var(--soft)}
.cap b{color:var(--fg)}
.readout{max-width:640px;margin:8px auto 0;padding:0 16px;font:11.5px/1.6 ui-monospace,Menlo,monospace;
  color:var(--fg);white-space:pre-wrap}
.ledgerx{max-width:680px;margin:30px auto 0;padding:0 16px;font-size:13.5px;line-height:1.62;color:var(--fg)}
.ledgerx h2{font:600 12px/1 -apple-system,system-ui,sans-serif;letter-spacing:.09em;text-transform:uppercase;
  color:var(--soft);padding-bottom:10px;border-bottom:1px solid var(--btn-line);margin:26px 0 12px}
.ledgerx p,.ledgerx li{max-width:65ch}
.ledgerx ul{padding-left:18px;display:flex;flex-direction:column;gap:6px}
.ledgerx code{font:11.5px/1 ui-monospace,Menlo,monospace;background:var(--code);padding:2px 4px}

/* ── el teléfono ── */
.phone{width:${SCREEN}px;height:${TALL}px;overflow:hidden;position:relative;background:$PAPER;
  color:$INK;font-family:Bitter,Georgia,serif;display:flex;flex-direction:column;
  box-shadow:0 10px 34px var(--shadow);flex:0 0 auto}
body.tira .phone{height:auto}
.status{height:${STATUS}px;flex:0 0 ${STATUS}px;display:flex;align-items:center;justify-content:flex-end;
  padding-right:14px;font:600 10px/1 Barlow,sans-serif;font-feature-settings:'smcp','tnum';color:$MUTED}
.screen{flex:1;position:relative;overflow:hidden;display:flex;flex-direction:column;min-height:0}
.navbar{height:${NAVBAR}px;flex:0 0 ${NAVBAR}px;display:flex;justify-content:center;align-items:center}
.navbar:after{content:'';width:108px;height:4px;border-radius:2px;background:$INK;opacity:.55}
.scroll{flex:1;overflow-y:auto;overflow-x:hidden;min-height:0}
body.tira .scroll{overflow:visible}
.fold-line{position:absolute;left:0;right:0;border-top:1.5px dashed $RUST;pointer-events:none;z-index:30}
.fold-line span{position:absolute;right:4px;top:-15px;font:600 10px/1 Barlow,sans-serif;color:$RUST;
  font-feature-settings:'smcp','tnum';background:$PAPER;padding:1px 3px}

/* tipos de Theme.kt */
.eyebrow{font:600 11px/1.2 Barlow,sans-serif;font-feature-settings:'smcp','tnum';color:$RUST;letter-spacing:.02em}
.idl{font:600 12px/1.35 Barlow,sans-serif;font-feature-settings:'smcp','tnum';color:$MUTED}
.c{text-align:center}
.g{font-family:Barlow,sans-serif;font-weight:600;font-feature-settings:'smcp','tnum'}
.caps{font:400 13px/1.3 Bitter,serif;letter-spacing:.08em}
.ext{font:400 16px/23px Bitter,serif;color:$MOSS;text-decoration:underline;text-underline-offset:2px;
  cursor:pointer}
.arrow{display:inline-block;width:.8em;height:.8em;margin-left:.2em;position:relative;top:.06em;
  background:linear-gradient(45deg,transparent 46%,$MOSS 46% 56%,transparent 56%) no-repeat,
  linear-gradient($MOSS,$MOSS) no-repeat right top/48% 1.4px,
  linear-gradient($MOSS,$MOSS) no-repeat right top/1.4px 48%}
.cardaction{align-self:flex-start;border:1px solid $HAIR;padding:9px 14px;font:600 12px/1 Barlow,sans-serif;
  font-feature-settings:'smcp','tnum';color:$INK;margin-top:6px}
.back{display:inline-block;width:13px;height:10px;position:relative;
  background:linear-gradient($INK,$INK) no-repeat 1px 50%/12px 1.4px}
.back:before,.back:after{content:'';position:absolute;left:0;top:50%;width:6px;height:1.4px;background:$INK;
  transform-origin:0 50%}
.back:before{transform:rotate(-45deg)}.back:after{transform:rotate(45deg)}
.backrow{height:48px;display:flex;align-items:center;gap:12px;padding:0 20px;
  font:600 12px/1 Barlow,sans-serif;font-feature-settings:'smcp','tnum';color:$INK}

/* el hueco troquelado (AlbumPaper.kt) */
.hole{position:relative;border-radius:50%;background-color:$CARD;flex:0 0 auto;
  box-shadow:inset 0 1.6px 2.4px rgba(45,48,41,.22),inset 0 -1.6px 2.4px rgba(255,255,255,.85);
  outline:1px solid $HAIR;outline-offset:-1px;perspective:900px}
.hole i{position:absolute;border-radius:50%;display:block;background-color:$DEEP;
  background-repeat:no-repeat;background-position:center;background-size:cover;
  backface-visibility:hidden;transition:transform .5s cubic-bezier(.4,.1,.2,1)}
.hole:not(.miss) i:after,.hole.lit i:after{content:'';position:absolute;inset:0;border-radius:50%;
  background:linear-gradient(118deg,rgba(255,255,255,.34) 0 18%,rgba(255,255,255,0) 42%)}
.hole.miss i{opacity:.14;filter:grayscale(.2)}
.hole.miss.lit i{opacity:1;filter:none}
.hole.miss:before{content:'';position:absolute;inset:4px;border-radius:50%;z-index:2;
  border:1px dashed rgba(45,48,41,.48)}
.hole.miss.lit:before{inset:-6px;border:1.5px dashed $RUST}
.hole.flip{cursor:pointer}
.hole.flip i.rev{transform:rotateY(180deg)}
.hole.flip.turned i:not(.rev){transform:rotateY(-180deg)}
.hole.flip.turned i.rev{transform:rotateY(0)}
@media (prefers-reduced-motion:reduce){.hole i{transition:none}}

/* la hoja de hoy */
.bg{padding:0 20px}
.chrome{height:54px;background:$INK;color:$PAPER;display:flex;align-items:center;gap:10px;padding:0 12px;margin:0 -20px 16px}
.chrome b{font:400 17px/21px Bitter}
.chrome span{font:600 10px/1 Barlow;font-feature-settings:'smcp';color:$DEEP}
.plate{padding-top:24px;display:flex;flex-direction:column;gap:10px}
.phead{display:flex;gap:12px;align-items:flex-start}
.plate .phead span{flex:1;font:400 26px/30px Bitter}
.plate .phead b{font:600 17px/1 Barlow;color:$RUST;font-feature-settings:'smcp','tnum'}
.pgrid{display:grid;grid-template-columns:repeat(3,104px);gap:18px 8px;padding-top:8px}
.pcell{display:flex;flex-direction:column;align-items:center;gap:6px}
.pname{font:400 14px/18px Bitter;text-align:center;height:36px;overflow:hidden}
.scrim{position:absolute;inset:0;background:rgba(0,0,0,.32);z-index:5}
.sheet{position:absolute;left:0;right:0;bottom:0;z-index:6;background:$PAPER;border-radius:28px 28px 0 0;
  padding:0 24px 32px;display:flex;flex-direction:column;align-items:center;gap:4px;max-height:100%;overflow-y:auto}
body.tira .slot[data-v="0"] .phone{height:${TALL}px}
.s0hole{padding:20px 0 12px}
.s0title{font:400 21px/25px Bitter;text-align:center;width:100%}
.s0id{font:600 12px/1.35 Barlow;font-feature-settings:'smcp','tnum';color:$MUTED;text-align:center;width:100%}
.s0val{font:600 12px/1.35 Barlow;font-feature-settings:'smcp','tnum';color:$RUST;text-align:center;width:100%;padding-top:6px}
.s0age{font:600 10px/1.2 Barlow;font-feature-settings:'smcp','tnum';color:$MUTED;width:100%;padding-top:8px}
.sheet .cardaction{margin-top:6px}
.s0num{width:100%;padding:6px 0}
.s0doors{font:600 12px/1 Barlow;font-feature-settings:'smcp';color:$MUTED;width:100%;padding-top:8px}
.s0link{font:400 16px/23px Bitter;color:$MOSS;text-decoration:underline;width:100%;padding:6px 0}

/* la página entera */
.page{padding:0 0 28px}
.page>section,.page>.h1,.page>.sub,.page>.idl,.page>.falta,.page>.foot,.page>.folds,.page>.bhead,
.page>.cartela,.page>.dleg{margin-left:20px;margin-right:20px}
section{display:flex;flex-direction:column;gap:8px;margin-top:26px}
section.tight{margin-top:10px}
.pair{display:flex;justify-content:center;gap:16px;padding:4px 0 6px}
.pair figure{display:flex;flex-direction:column;align-items:center;gap:6px}
figcaption{font:600 10px/1 Barlow;font-feature-settings:'smcp';color:$MUTED}
.h1{font:400 26px/30px Bitter;margin-top:14px;text-wrap:balance}
.sub{font:400 14px/20px Bitter;margin-top:6px}
.idl{margin-top:6px}
.falta{display:inline-block;margin-top:10px;font:600 11px/1 Barlow;font-feature-settings:'smcp';
  color:$RUST;border:1.5px dashed $RUST;padding:5px 8px;align-self:flex-start}
.vhead{font:600 12px/1.35 Barlow;font-feature-settings:'smcp','tnum';color:$RUST}
.vhead.muted{color:$MUTED}
.vline{font:600 12px/1.35 Barlow;font-feature-settings:'smcp','tnum';color:$RUST;margin-top:6px}
.vbig{font:400 21px/25px Bitter;color:$RUST}
.stamp{font:600 10px/1.3 Barlow;font-feature-settings:'smcp','tnum';color:$MUTED}
.piece{display:flex;gap:12px;padding:10px 0;border-top:1px solid $DEEP}
.piece:first-of-type{border-top:0;padding-top:2px}
.piece.here{background:linear-gradient(90deg,rgba(139,85,60,.08),transparent);margin:0 -8px;padding-left:8px;padding-right:8px}
.pmain{flex:1;min-width:0;display:flex;flex-direction:column;gap:3px}
.phead{display:flex;align-items:baseline;gap:8px;flex-wrap:wrap}
.pyear{font:400 16px/22px Bitter}
.phead.big .pyear{font-size:21px;line-height:25px}
.grade{font:600 13px/1 Barlow;font-feature-settings:'smcp';color:$INK;border:1px solid $LINE;padding:2px 5px 3px}
.qty{font:600 12px/1 Barlow;font-feature-settings:'tnum';color:$MUTED}
.aqui{font:600 11px/1 Barlow;font-feature-settings:'smcp';color:$RUST}
.pfacts{font:400 14px/20px Bitter;color:$INK}
.pprices{display:flex;flex-wrap:wrap;gap:4px 12px;font:600 12px/1.4 Barlow;font-feature-settings:'smcp','tnum';color:$MUTED}
.pp.win{color:$RUST}
.pp.win:before{content:'▸ ';}
.pissue{font:600 11px/1.35 Barlow;color:$MUTED}
.pnote{font:400 14px/20px Bitter;color:$MUTED}
.pphoto{flex:0 0 auto;background-size:cover;background-position:center top;border:1px solid $LINE;
  box-shadow:0 1px 2px rgba(45,48,41,.2)}
.claim{display:flex;align-items:baseline;gap:4px}
.cl{font:400 16px/23px Bitter}
.cl.link{color:$MOSS;text-decoration:underline}
.strip{display:flex;flex-wrap:wrap;gap:8px 6px;margin-top:6px}
.strip.big{gap:10px}
.cell{display:flex;flex-direction:column;align-items:center;gap:3px;position:relative}
.cell.big{width:76px}
.cell.here .hole{outline:2px solid $RUST;outline-offset:2px}
.clab{font:400 11px/14px Bitter;text-align:center;color:$INK}
.clab.y{font:600 9px/1 Barlow;font-feature-settings:'tnum';color:$MUTED}
.stripnote{font:600 11px/1 Barlow;font-feature-settings:'smcp','tnum';color:$RUST}
.notelab{font:600 10px/1 Barlow;font-feature-settings:'smcp';color:$MUTED;margin-top:10px}
.note{font:400 14px/20px Bitter;color:$INK;border-left:2px solid $DEEP;padding:2px 0 2px 12px;margin-top:4px}
.fcard{background:$CARD;border:1px solid $LINE;padding:14px}
.srow{display:flex;justify-content:space-between;align-items:baseline;gap:16px;padding:6px 0;border-top:1px solid $DEEP}
.srow:first-child{border-top:0;padding-top:0}.srow:last-child{padding-bottom:0}
.sl{font:600 10px/1.2 Barlow;font-feature-settings:'smcp';color:$MUTED;flex:0 0 auto}
.sv{font:400 14px/20px Bitter;text-align:right;min-width:0}
.faceblock{display:flex;flex-direction:column;gap:4px;padding:10px 0;border-top:1px solid $DEEP}
.faceblock:first-of-type{border-top:0;padding-top:2px}
.fhead{font:600 11px/1 Barlow;font-feature-settings:'smcp';color:$MUTED}
.legend{font:400 14px/21px Bitter;letter-spacing:.12em;white-space:pre-line;color:$INK}
.unab{font:400 13px/18px Bitter;color:$MUTED}
.desc{font:400 14px/20px Bitter;white-space:pre-line}
.hands{font:600 11px/1.3 Barlow;font-feature-settings:'smcp';color:$MUTED}
.prose{font:400 14px/21px Bitter;display:flex;flex-direction:column;gap:8px}
.prose .ext{font-size:14px;line-height:21px}
.rel{display:flex;flex-direction:column;padding:6px 0;border-top:1px solid $DEEP}
.rel:first-of-type{border-top:0}
.relc{font:600 11px/1 Barlow;font-feature-settings:'smcp';color:$MUTED}
.rel .ext{font-size:15px;line-height:21px}
.rely{font:600 11px/1 Barlow;font-feature-settings:'tnum';color:$MUTED}
.foot{display:flex;flex-direction:column;gap:2px;margin-top:30px;padding-top:14px;border-top:1px solid $DEEP}
.age{font:600 10px/1.2 Barlow;font-feature-settings:'smcp','tnum';color:$MUTED}
.foot .ext{margin-top:6px}

/* B */
.bhead{display:flex;gap:16px;align-items:center;margin-top:4px}
.bcol{flex:1;min-width:0;display:flex;flex-direction:column}
.btitle{font:400 21px/25px Bitter;text-wrap:balance}
.bcol .falta{align-self:flex-start}
.folds{margin-top:18px;border-top:1px solid $LINE}
.fold{border-bottom:1px solid $LINE}
.fold summary{list-style:none;cursor:pointer;display:grid;grid-template-columns:1fr 14px;
  grid-template-rows:auto auto;column-gap:10px;padding:12px 0;min-height:56px}
.fold summary::-webkit-details-marker{display:none}
.ft{font:400 17px/21px Bitter;grid-column:1}
.fs{font:600 12px/1.35 Barlow;font-feature-settings:'smcp','tnum';color:$MUTED;grid-column:1;margin-top:3px;
  white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.chev{grid-column:2;grid-row:1/3;align-self:center;width:9px;height:9px;border-right:1.5px solid $INK;
  border-bottom:1.5px solid $INK;transform:rotate(45deg);margin-top:-4px;transition:transform .2s}
.fold[open] .chev{transform:rotate(-135deg);margin-top:4px}
.fb{padding:0 0 16px;display:flex;flex-direction:column;gap:8px}

/* C */
.chero{display:flex;flex-direction:column;align-items:center;gap:8px;padding:6px 0 0}
.hint{font:600 10px/1 Barlow;font-feature-settings:'smcp';color:$MUTED}
.h1.c{text-align:center;font-size:24px;line-height:28px}
.pc{border-top:1px solid $LINE;padding-top:16px}
.ladder{display:flex;flex-direction:column;gap:12px;margin-top:4px}
.rung{display:flex;flex-direction:column;gap:4px}
.rl{display:flex;justify-content:space-between;align-items:baseline;gap:12px}
.rlab{font:400 15px/20px Bitter;min-width:0}
.ramt{white-space:nowrap;font:600 17px/1 Barlow;font-feature-settings:'tnum';color:$INK}
.rnone{font:600 11px/1.2 Barlow;font-feature-settings:'smcp';color:$MUTED;text-align:right}
.bar{height:10px;background:transparent;border:1px solid $LINE}
.bar i{display:block;height:100%;background:$DEEP}
.bar.dashed{border:1px dashed $HAIR}
.rung.win .ramt{color:$RUST;font-size:19px}
.rung.win .bar{border-color:$RUST}
.rung.win .bar i{background:$RUST}
.rung.win .rlab:after{content:'el mayor';margin-left:8px;font:600 10px/1 Barlow;font-feature-settings:'smcp';
  color:$PAPER;background:$RUST;padding:2px 5px 3px;vertical-align:2px;white-space:nowrap;display:inline-block}
.rung.none .rlab{color:$MUTED}
.rsrc{font:600 10px/1.3 Barlow;font-feature-settings:'smcp','tnum';color:$MUTED}
.sub2{font:600 11px/1.2 Barlow;font-feature-settings:'smcp';color:$MUTED;margin-top:10px}
.gchart{display:grid;grid-template-columns:repeat(7,1fr);gap:4px;align-items:end;margin-top:2px}
.gcol{display:flex;flex-direction:column;align-items:center;gap:3px}
.gv{font:600 10px/1 Barlow;font-feature-settings:'tnum';color:$MUTED;white-space:nowrap}
.gb{height:52px;width:100%;display:flex;align-items:flex-end;justify-content:center}
.gb i{display:block;width:70%;background:$DEEP}
.gcol.empty .gb i{background:transparent;border:1px dashed $HAIR;border-bottom:0}
.gcol.mine .gb i{background:$RUST}
.gcol.mine .gv{color:$RUST}
.gg{font:600 12px/1 Barlow;font-feature-settings:'smcp';color:$INK;padding-top:3px;border-top:1px solid $LINE;width:100%;text-align:center}
.gcol.mine .gg{color:$RUST;border-top:2px solid $RUST}
.gnote{font:600 10px/1 Barlow;font-feature-settings:'smcp';color:$MUTED;display:flex;gap:6px;align-items:center}
.gnote b{width:10px;height:10px;background:$RUST;display:inline-block}
table.ledger{width:100%;border-collapse:collapse;font:600 12px/1 Barlow;font-feature-settings:'tnum'}
.ledger th,.ledger td{padding:6px 4px;text-align:right;border-bottom:1px solid $DEEP}
.ledger thead th{font-feature-settings:'smcp';color:$MUTED;font-size:11px;border-bottom:1px solid $LINE}
.ledger tbody th{text-align:left;font:400 14px/1 Bitter}
.ledger td.g{text-align:center;font-feature-settings:'smcp'}
.ledger td.win{color:$RUST}
.ledger td.nil{color:$HAIR}
.ledger tr.here{background:rgba(139,85,60,.10)}

/* D */
.dhero{display:flex;flex-direction:column;align-items:center;gap:6px;padding:8px 0 4px}
.ruler{display:block;overflow:visible}
.ruler line{stroke:$INK;stroke-width:1}
.ruler text{font:600 9px Barlow;fill:$MUTED;text-anchor:middle;font-feature-settings:'tnum'}
.dcap{font:600 11px/1 Barlow;font-feature-settings:'smcp','tnum';color:$MUTED}
.edge{margin-top:6px;background:linear-gradient(#cfc7b0,#a9a189 60%,#8f8873);border-radius:2px;
  box-shadow:0 1px 1px rgba(45,48,41,.25)}
.edge.reed{background:repeating-linear-gradient(90deg,#b5ad96 0 1.4px,#8a8370 1.4px 2.6px)}
.edge.guess{opacity:.45}
.dleg{margin-top:18px;text-align:center;display:flex;flex-direction:column;gap:6px}
.legend.big{font-size:15px;line-height:22px;letter-spacing:.16em}
.dsep{color:$HAIR}
.cartela{margin-top:22px;background:$CARD;border:1px solid $LINE;border-width:1px 0;padding:14px 2px;
  display:flex;flex-direction:column;gap:4px}
.ctitle{font:400 19px/23px Bitter}
.cplace{font:400 14px/20px Bitter;color:$INK}
.cmat{font:600 12px/1.35 Barlow;font-feature-settings:'smcp','tnum';color:$MUTED}
.cdesc{font:400 14px/20px Bitter;color:$INK;margin-top:4px;white-space:pre-line}
.cref{font:600 11px/1.2 Barlow;font-feature-settings:'smcp','tnum';color:$RUST;margin-top:4px}
.reg{display:flex;gap:12px;padding:10px 0;border-top:1px solid $DEEP;align-items:flex-start}
.reg.here{background:rgba(139,85,60,.08)}
.rn{font:600 13px/22px Barlow;font-feature-settings:'tnum';color:$RUST;width:18px;text-align:right}
.rb{flex:1;min-width:0;display:flex;flex-direction:column;gap:3px}
$PHOTOS
""").substitute(
    BITTER=font("bitter_variable"), BARLOW4=font("barlow_condensed_regular"),
    BARLOW6=font("barlow_condensed_semibold"), INK=INK, MUTED=MUTED, PAPER=PAPER, DEEP=DEEP,
    LINE=LINE, MOSS=MOSS, RUST=RUST, HAIR=HAIR, CARD=CARD, SCREEN=SCREEN, TALL=TALL,
    STATUS=STATUS, NAVBAR=NAVBAR, PHOTOS=CSS_PHOTOS)

JS = Template("""
const THESES = $THESES, CASES = $CASES;
const st = {v:'A', c:'real', m:'tel'};
const parse = () => {
  const h = location.hash.replace('#','').split('-');
  if (h[0] && THESES[h[0]]) st.v = h[0];
  if (h[1] && CASES[h[1]]) st.c = h[1];
  if (['tel','tira','todas'].includes(h[2])) st.m = h[2];
};
const TALL = $TALL;
function measure(ph){
  ph.querySelectorAll('.fold-line').forEach(n => n.remove());
  const sc = ph.querySelector('.scroll.page') || ph.querySelector('.sheet');
  if (!sc) return '';
  const top = ph.getBoundingClientRect().top;
  if (sc.classList.contains('sheet')) return 'la hoja mide ' + sc.offsetHeight + ' dp de los ' + TALL + '\\n' +
    'se ve sin tocar nada: toda la hoja, sobre la lámina';
  const kids = [...sc.children].filter(n => n.offsetParent !== null);
  const last = kids.length ? Math.max(...kids.map(n => n.offsetTop + n.offsetHeight)) - sc.offsetTop : sc.scrollHeight;
  const h = last + parseFloat(getComputedStyle(sc).paddingBottom) + $STATUS + $NAVBAR;
  const first = [...ph.querySelectorAll('[data-sec]')]
    .filter(n => n.offsetParent !== null && n.getBoundingClientRect().top - top < TALL - $NAVBAR)
    .map(n => n.dataset.sec.trim().toLowerCase());
  if (document.body.classList.contains('tira')) {
    for (let y = TALL; y < h; y += TALL - $STATUS - $NAVBAR) {
      const l = document.createElement('div'); l.className = 'fold-line'; l.style.top = y + 'px';
      l.innerHTML = '<span>pliegue ' + Math.round(y) + ' dp</span>'; ph.appendChild(l);
    }
  }
  return 'la ficha mide ' + Math.round(h) + ' dp: ' + (h / TALL).toFixed(1).replace('.', ',') + ' pantallas\\n' +
    'se ve sin tocar nada: ' + (first.length ? first.join(' · ') : '—');
}
function render(){
  document.body.classList.toggle('tira', st.m === 'tira');
  document.body.classList.toggle('todas', st.m === 'todas');
  document.querySelectorAll('.slot').forEach(s => {
    s.hidden = !(s.dataset.c === st.c && (st.m === 'todas' || s.dataset.v === st.v));
  });
  document.querySelectorAll('header button').forEach(b => {
    b.classList.toggle('on', st[b.dataset.axis] === b.dataset.k);
  });
  document.getElementById('cap').innerHTML = st.m === 'todas'
    ? '<b>Las cinco, ' + CASES[st.c] + '.</b> Desplaza en horizontal.'
    : '<b>' + st.v + '.</b> ' + THESES[st.v] + '<br><b>Moneda:</b> ' + CASES[st.c] + '.';
  requestAnimationFrame(() => {
    const out = [];
    document.querySelectorAll('.slot:not([hidden]) .phone').forEach(ph => {
      out.push((st.m === 'todas' ? ph.id.split('-')[1] + ' · ' : '') + measure(ph));
    });
    document.getElementById('readout').textContent = out.join('\\n\\n');
  });
}
function go(axis, k){
  st[axis] = k;
  history.replaceState(null, '', '#' + st.v + '-' + st.c + '-' + st.m);
  render();
}
document.querySelectorAll('header button').forEach(b => b.addEventListener('click', () => go(b.dataset.axis, b.dataset.k)));
document.addEventListener('click', e => {
  const h = e.target.closest('.hole.flip'); if (h) h.classList.toggle('turned');
});
document.addEventListener('keydown', e => {
  const h = e.target.closest && e.target.closest('.hole.flip');
  if (h && (e.key === 'Enter' || e.key === ' ')) { e.preventDefault(); h.classList.toggle('turned'); return; }
  if (e.target.closest && e.target.closest('input,textarea,[contenteditable]')) return;
  const ks = Object.keys(THESES), cs = Object.keys(CASES);
  if (e.key === 'ArrowRight' || e.key === 'ArrowLeft') {
    const d = e.key === 'ArrowRight' ? 1 : -1;
    go('v', ks[(ks.indexOf(st.v) + d + ks.length) % ks.length]);
  }
  if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
    e.preventDefault();
    const d = e.key === 'ArrowDown' ? 1 : -1;
    go('c', cs[(cs.indexOf(st.c) + d + cs.length) % cs.length]);
  }
});
document.addEventListener('toggle', () => render(), true);
window.setState = (v, c, m) => { if (v) st.v = v; if (c) st.c = c; if (m) st.m = m; render(); };
window.addEventListener('hashchange', () => { parse(); render(); });
parse(); render();
""").substitute(THESES=THESES, CASES=CASES_NOTE, TALL=TALL, STATUS=STATUS, NAVBAR=NAVBAR)

LEDGER = """
<div class="ledgerx">
<h2>La pregunta</h2>
<p>«La ficha de una moneda está un poco anémica. A lo mejor meterle más cosas: en qué colecciones
está, etc. Podríamos meter también precios (fundición, Numista, compra, etc.).» ¿Qué lleva la
ficha, y en qué orden? Y Jose, el 11 de octubre: «Siéntete libre de hacer un diseño chulo con una
vista entera y sacar incluso más detalles de lo que tenemos».</p>
<h2>Lo que hay en el móvil y hoy no se enseña</h2>
<ul>
<li><b>La ficha entera de Numista</b>, guardada en <code>type_meta.raw</code>: valor facial y su
moneda, gobernante, serie o tema, ceca, metal, peso, diámetro, grosor, canto (con su leyenda),
orientación, técnica, curso legal, referencias de catálogo, grabadores, las dos caras con su
descripción y su leyenda (y la leyenda desarrollada), los comentarios y las fichas emparentadas.</li>
<li><b>Cada pieza</b>, en <code>collected_items.raw</code>: año y letra de ceca, grado, lo que
pagaste, cuándo la compraste (149 de 287 filas del padre), la tirada de su emisión (233), su nota
privada (60), el comentario de Numista sobre la emisión, si es para cambio y sus fotos (5 filas).</li>
<li><b>Los precios de Numista en todos los grados</b> de cada emisión leída
(<code>issue_prices</code>), no sólo el de la pieza, con su fecha de lectura.</li>
<li><b>La nota del catálogo</b> (<code>variant_note</code>): por qué esa moneda está en esa
lámina, escrita al curarla. Hoy sólo la lee la validación.</li>
</ul>
<h2>Cómo se mira</h2>
<ul>
<li>Barra de arriba: variante (← →), moneda (↑ ↓) y modo. <b>Tira entera</b> despliega el teléfono
y marca cada pliegue de 914 dp; <b>Las cinco a la vez</b> las pone en fila.</li>
<li>Toca la moneda donde lleva el giro (0, B, C y D). En B, toca una fila para abrirla.</li>
<li>Debajo del teléfono: lo que mide la ficha y qué secciones se ven sin tocar nada.</li>
</ul>
<h2>Lo que la maqueta no prueba</h2>
<ul>
<li>Nada de esto se ha visto en un teléfono: el navegador elige y el emulador confirma.</li>
<li>Las colecciones son sólo las láminas curadas que reclaman el tipo, en el orden de
<code>data/</code>; la app añade las derivadas y ordena por el índice.</li>
<li>Los importes son los del móvil del padre a 11 de octubre de 2026. No se versionan: esta página
vive en <code>/private/tmp/coindex-privado/</code> y en un artifact privado.</li>
<li>El «a tamaño real» de la D supone 160 dp por pulgada; un Pixel 7 se desvía un 1 %.</li>
</ul>
</div>
"""

PAGE = f"""<meta charset="utf-8">
<title>Ficha de moneda #637</title>
<style>{CSS}</style>
<header>
<h1>Ficha de una moneda <span>· #637 · maqueta a dp real (411 × 914)</span></h1>
<div class="row"><em>variante</em>{VTABS}</div>
<div class="row"><em>moneda</em>{CTABS}</div>
<div class="row"><em>modo</em>{MTABS}</div>
</header>
<div class="stagewrap"><div class="stage">{SLOTS}</div></div>
<div class="cap" id="cap"></div>
<div class="readout" id="readout"></div>
{LEDGER}
<script>{JS}</script>
"""

open(f"{OUT}/maqueta.html", "w").write(PAGE)
print(f"{OUT}/maqueta.html · {len(PAGE) / 1e6:.1f} MB")
