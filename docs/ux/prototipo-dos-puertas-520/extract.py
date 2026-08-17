#!/usr/bin/env python3
"""Saca de la caché sembrada del móvil lo que el índice del padre enseña hoy (#520).

PROTOTIPO — se tira cuando el ticket se cierre. Lo que sobrevive es el README.

La fuente es `.local/padre/coindex.db`, que es lo que la app lee y no lo que `data/` dice
(`medir-en-el-movil-no-en-el-asset`): el inventario, los tipos sembrados con sus fotos, la caja
propia y **las dos casillas que el padre tiene marcadas de verdad**.

Reglas copiadas del dominio y no supuestas:

- casilla llena y evidencia: `CollectionCatalog.memberMatches` / `isEvidencedBy`
- casillas medibles: las que tienen `numista_type_id` (`CollectionCatalogAlbum.issuedMembers`:
  anunciadas y no listadas quedan fuera del divisor)
- ventana del estante: sin evidencia y de 1 a 19 casillas (`showcasePlate`, ADR 0030 §1)
- orden del índice: con fracción primero, fracción descendente, y el nombre en español
  (`indexOrder`)
- cara de la casilla: `printed_side` del catálogo y, si no lo declara, reverso antes que anverso

Lo que **no** reproduce: la derivación fina de `deriveCollection` (familia de set, familia de
Numista, agrupación curada, sistema monetario). Las tarjetas sin catálogo se agrupan por la
`family` que la caché trae por tipo, que es la tercera de esas cinco reglas. El recuento resultante
se imprime al final para poder compararlo con el canto cosido del teléfono.

    python3 docs/ux/prototipo-dos-puertas-520/extract.py
"""
import concurrent.futures
import glob
import json
import locale
import os
import re
import sqlite3
import subprocess
import urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.abspath(f"{HERE}/../../..")
OUT = "/private/tmp/coindex-privado/dos-puertas-520"
DB = f"{REPO}/.local/padre/coindex.db"

os.makedirs(f"{OUT}/fotos", exist_ok=True)

db = sqlite3.connect(DB)
db.row_factory = sqlite3.Row

# ── el inventario, tal como la app lo tiene ────────────────────────────────
items = []
for row in db.execute("select raw, quantity from collected_items"):
    it = json.loads(row["raw"])
    it["quantity"] = row["quantity"]
    if it["quantity"] > 0:
        items.append(it)

meta = {}
for row in db.execute(
    "select typeId, title, family, issuerName, weightGrams, obverseThumbnailUrl,"
    " reverseThumbnailUrl, obverseUrl, reverseUrl from type_meta"
):
    meta[row["typeId"]] = dict(row)

boxes = {}
for row in db.execute("select id, name from own_groupings"):
    boxes[row["id"]] = dict(name=row["name"], types=set())
for row in db.execute("select groupingId, typeId from own_grouping_members"):
    boxes[row["groupingId"]]["types"].add(row["typeId"])

marks = [dict(tid=row["typeId"], year=row["year"]) for row in db.execute("select * from wishes")]

catalogs = [json.load(open(f)) for f in sorted(glob.glob(f"{REPO}/data/collection-catalogs/*.json"))]


def face(tid, printed_side=None):
    """La foto que la casilla enseña: `printed_side` si el catálogo lo declara, si no reverso."""
    e = meta.get(tid) or {}
    order = ("obverse", "reverse") if printed_side == "obverse" else ("reverse", "obverse")
    for side in order:
        url = e.get(f"{side}ThumbnailUrl") or e.get(f"{side}Url")
        if url:
            return url
    return None


def matches(cat, mem, it):
    """`CollectionCatalog.memberMatches`, en las cuatro versiones de esquema que hay en `data/`."""
    tid = mem.get("numista_type_id")
    if not tid or it["type"]["id"] != tid:
        return False
    ids = mem.get("numista_issue_ids") or []
    iid = (it.get("issue") or {}).get("id")
    if ids and iid not in ids:
        return False
    sv = cat.get("schema_version")
    if sv == 5:
        return bool(ids)
    if sv == 2:
        iss = it.get("issue") or {}
        year = iss.get("year") if iss.get("year") is not None else iss.get("gregorian_year")
        return year == mem.get("year")
    return True


def claims(cat, it):
    """`isEvidencedBy` sobre una pieza: el tipo lo reclama alguna casilla, con su emisión si la fija."""
    return any(
        m.get("numista_type_id") == it["type"]["id"]
        and (
            not (m.get("numista_issue_ids") or [])
            or (it.get("issue") or {}).get("id") in m["numista_issue_ids"]
        )
        for m in cat.get("members", [])
    )


cards, showcase, claimed_types = [], [], set()
for cat in catalogs:
    mems = cat.get("members", [])
    issued = [m for m in mems if m.get("numista_type_id")]
    name = cat.get("short_name") or cat["name"]
    ps = cat.get("printed_side")
    if any(claims(cat, it) for it in items):
        filled = [m for m in issued if any(matches(cat, m, it) for it in items)]
        claimed_types |= {
            it["type"]["id"] for it in items if claims(cat, it)
        }
        rep = (filled or issued)[0]
        cards.append(dict(
            kind="plate",
            id=cat["id"],
            name=name,
            owned=len(filled),
            issued=len(issued),
            tid=rep.get("numista_type_id"),
            url=face(rep.get("numista_type_id"), ps),
        ))
    elif 0 < len(issued) < 20:
        showcase.append(dict(id=cat["id"], name=name, slots=len(issued)))

# ── la caja propia y los bultos que ninguna lámina reclama ─────────────────
for box in boxes.values():
    inside = [it for it in items if it["type"]["id"] in box["types"]]
    cards.append(dict(
        kind="box",
        id=f"box-{box['name']}",
        name=box["name"],
        types=len({it["type"]["id"] for it in inside}),
        qty=sum(it["quantity"] for it in inside),
        tid=(inside[0]["type"]["id"] if inside else None),
        url=(face(inside[0]["type"]["id"]) if inside else None),
    ))
    claimed_types |= box["types"]

GROUPINGS = {
    tid: g["family"]
    for f in sorted(glob.glob(f"{REPO}/data/groupings/*.json"))
    for g in [json.load(open(f))]
    for tid in g["type_ids"]
}

# ── los bultos: una tarjeta pide familia y peso, o la pieza es residuo ──────
#
# `deriveCollection` sólo saca familia de tres sitios —catálogo, serie de Numista, agrupación
# curada— y el **sistema monetario no es uno de ellos**: sin familia la pieza cae en «Sin
# colección» (#275), y sin peso también. Un artículo suelto por nombre («The») tampoco es una
# familia (#404). Lo que esta regla no distingue son las familias técnicas de ADR 0012, que
# necesitan una curada encima; así que este recuento es un **suelo** y no el número exacto.
loose, residue = {}, 0
for it in items:
    tid = it["type"]["id"]
    if tid in claimed_types:
        continue
    e = meta.get(tid) or {}
    family = GROUPINGS.get(tid) or e.get("family")
    if not family or family.strip().lower() in ("the", "la", "el", "los", "las") \
            or not e.get("weightGrams"):
        residue += it["quantity"]
        continue
    group = loose.setdefault(family, dict(kind="loose", id=f"loose-{family}", name=family,
                                          types=set(), qty=0, tid=tid, url=face(tid)))
    group["types"].add(tid)
    group["qty"] += it["quantity"]
for group in loose.values():
    group["types"] = len(group["types"])
    cards.append(group)

# ── el orden de `indexOrder`: con fracción primero, y el nombre en español ──
try:
    locale.setlocale(locale.LC_COLLATE, "es_ES.UTF-8")
except locale.Error:
    pass
cards.sort(key=lambda c: (
    0 if c["kind"] == "plate" else 1,
    -(c["owned"] / c["issued"]) if c["kind"] == "plate" else 0,
    -c.get("issued", 0),
    locale.strxfrm(c["name"]),
))

# ── las casillas marcadas: de qué lámina son, y qué moneda ──────────────────
for mark in marks:
    e = meta.get(mark["tid"]) or {}
    mark["title"] = e.get("title") or f"tipo {mark['tid']}"
    mark["url"] = face(mark["tid"])
    mark["plate"] = next(
        (
            (cat.get("short_name") or cat["name"])
            for cat in catalogs
            for m in cat.get("members", [])
            if m.get("numista_type_id") == mark["tid"]
        ),
        None,
    )
    mark["label"] = next(
        (
            re.split(r"[;·]", m["label"])[0].strip()
            for cat in catalogs
            for m in cat.get("members", [])
            if m.get("numista_type_id") == mark["tid"]
        ),
        mark["title"],
    )

sewn = dict(
    collections=len(cards),
    pieces=sum(it["quantity"] for it in items),
    types=len({it["type"]["id"] for it in items}),
)

data = dict(sewn=sewn, cards=cards, showcase=showcase, marks=marks,
            wish_plates=len({m["plate"] for m in marks if m["plate"]}))
json.dump(data, open(f"{OUT}/data.json", "w"), ensure_ascii=False, indent=1)

# ── las fotos: Numista responde 403 sin Referer de su propio sitio ──────────
urls = {c["tid"]: c["url"] for c in cards if c.get("url")}
urls.update({m["tid"]: m["url"] for m in marks if m.get("url")})
H = {"User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)",
     "Referer": "https://en.numista.com/"}


def get(kv):
    tid, url = kv
    path = f"{OUT}/fotos/{tid}.jpg"
    if os.path.exists(path) and os.path.getsize(path) > 2000:
        return True
    try:
        raw = urllib.request.urlopen(urllib.request.Request(url, headers=H), timeout=30).read()
    except Exception:
        return False
    open(path, "wb").write(raw)
    # 132 px y calidad 45, que es lo que deja la maqueta autocontenida sin engordarla (#279)
    subprocess.run(["sips", "-Z", "132", "-s", "formatOptions", "45", path, "--out", path],
                   capture_output=True)
    return True


with concurrent.futures.ThreadPoolExecutor(6) as ex:
    ok = sum(1 for r in ex.map(get, urls.items()) if r)

print(f"residuo (sin colección) {residue} piezas")
print(f"tarjetas {len(cards)} ({sum(1 for c in cards if c['kind'] == 'plate')} láminas, "
      f"{sum(1 for c in cards if c['kind'] == 'loose')} bultos, "
      f"{sum(1 for c in cards if c['kind'] == 'box')} cajas) · "
      f"estante {len(showcase)} · marcas {len(marks)} en {data['wish_plates']} láminas")
print(f"canto cosido: {sewn['collections']} colecciones · {sewn['pieces']} piezas · "
      f"{sewn['types']} tipos · fotos {ok}/{len(urls)}")
