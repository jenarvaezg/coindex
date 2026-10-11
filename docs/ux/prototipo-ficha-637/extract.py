#!/usr/bin/env python3
"""Saca del móvil del padre todo lo que la ficha de una moneda podría enseñar (#637).

Prototipo: se tira cuando el ticket se cierre; lo que sobrevive es el README.

La fuente es `.local/padre/coindex.db`, lo que la app lee: el cuerpo entero de cada ficha de
Numista (`type_meta.raw`), cada pieza tal como la devolvió la colección de Numista
(`collected_items.raw`, con fecha de compra, tirada, letra de ceca y nota privada), los precios
por emisión y grado (`issue_prices`) con su fecha de lectura, y el spot de la plata. Las láminas
salen de `data/collection-catalogs/`, que es lo que la APK siembra.

Cinco monedas del padre, las cuatro que pide el ticket y una de esfuerzo:

- real: compra y Numista, dos piezas, foto propia y nota del curador (8 reales de Carlos IV).
- hueco: una casilla que le falta (Unicorn of Scotland, The Queen's Beasts).
- medalla: exonumia sin precio en Numista (Cuatricentenario de Caracas, ficha suya).
- sinplata: cuproníquel, sin suelo de metal (2 euros de la Policía Nacional).
- fuerte: 31 piezas en 26 filas de un date run (5 bolívares, Fuertes).

Los importes no se versionan: todo lo que sale de aquí va a /private/tmp/coindex-privado/.

    python3 docs/ux/prototipo-ficha-637/extract.py
"""
import concurrent.futures
import glob
import html
import json
import os
import re
import sqlite3
import subprocess
import urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.abspath(f"{HERE}/../../..")
MAIN = "/Users/jose/jenarvaezg/coindex"
OUT = "/private/tmp/coindex-privado/ficha-637"
DB = f"{MAIN}/.local/padre/coindex.db"

os.makedirs(f"{OUT}/fotos", exist_ok=True)

CASES = [
    dict(key="real", name="Compra y Numista", tid=18852, here="historia-del-real"),
    dict(key="hueco", name="Un hueco", tid=127263, here="queens-beasts-uk-2oz"),
    dict(key="medalla", name="Una medalla", tid=578835, here=None),
    dict(key="sinplata", name="Sin plata", tid=401375, here="espana-2-euros-conmemorativos"),
    dict(key="fuerte", name="Muchas piezas", tid=10340, here="venezuela-fuertes", year=1929),
]

db = sqlite3.connect(DB)
db.row_factory = sqlite3.Row

items = []
for row in db.execute("select raw, quantity from collected_items where quantity > 0"):
    it = json.loads(row["raw"])
    it["quantity"] = row["quantity"]
    items.append(it)

meta = {r["typeId"]: dict(r) for r in db.execute("select * from type_meta")}

prices, reads = {}, {}
for r in db.execute("select * from issue_prices"):
    prices.setdefault(f'{r["typeId"]}:{r["issueId"]}', {})[r["grade"]] = r["eur"]
for r in db.execute("select * from issue_price_reads"):
    reads[f'{r["typeId"]}:{r["issueId"]}'] = dict(at=r["readAt"], has=bool(r["hasPrices"]))

issues = {}
for r in db.execute("select * from type_issues order by position"):
    issues.setdefault(r["typeId"], []).append(dict(id=r["issueId"], year=r["year"]))

spot = dict(db.execute("select * from metal_spot where symbol = 'XAG'").fetchone())

catalogs = {}
for f in sorted(glob.glob(f"{REPO}/data/collection-catalogs/*.json")):
    c = json.load(open(f))
    catalogs[c["id"]] = c


def matches(cat, mem, it):
    """`CollectionCatalog.memberMatches`, en las versiones de esquema que hay en `data/`."""
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


def face_url(tid, side, big):
    e = meta.get(tid) or {}
    return e.get(f"{side}Url") if big else (e.get(f"{side}ThumbnailUrl") or e.get(f"{side}Url"))


def text(s):
    return html.unescape(s) if isinstance(s, str) else s


def ficha(tid):
    """El cuerpo de la ficha, con lo que una pantalla podría imprimir y sin las fotos."""
    raw = json.loads(meta[tid]["raw"])
    side = lambda s: {
        k: text(v) for k, v in (raw.get(s) or {}).items()
        if k in ("description", "lettering", "unabridged_legend", "engravers", "designers")
    }
    edge = raw.get("edge") or {}
    return dict(
        title=raw.get("title"),
        category=raw.get("category"),
        issuer=(raw.get("issuer") or {}).get("name"),
        value=(raw.get("value") or {}).get("text"),
        currency=((raw.get("value") or {}).get("currency") or {}).get("full_name"),
        rulers=[r["name"] for r in raw.get("ruler") or []],
        series=raw.get("series"),
        topic=text(raw.get("commemorated_topic")),
        min_year=raw.get("min_year"),
        max_year=raw.get("max_year"),
        weight=raw.get("weight"),
        size=raw.get("size"),
        thickness=raw.get("thickness"),
        shape=raw.get("shape"),
        composition=text((raw.get("composition") or {}).get("text")),
        technique=(raw.get("technique") or {}).get("text"),
        orientation=raw.get("orientation"),
        demonetization=raw.get("demonetization"),
        mints=[m["name"] for m in raw.get("mints") or []],
        references=[f'{r["catalogue"]["code"]}# {r["number"]}' for r in raw.get("references") or []],
        tags=raw.get("tags") or [],
        edge=dict(description=text(edge.get("description")), lettering=edge.get("lettering"),
                  picture=bool(edge.get("picture"))),
        comments=raw.get("comments"),
        obverse=side("obverse"),
        reverse=side("reverse"),
        related=[dict(id=r["id"], country=r["country"]["name"], title=r["title"],
                      years=[r.get("minYear"), r.get("maxYear")])
                 for r in raw.get("related_types") or []],
        url=meta[tid].get("numistaUrl"),
        fetched_at=meta[tid]["fetchedAt"],
    )


def pieces(tid):
    out = []
    for it in items:
        if it["type"]["id"] != tid:
            continue
        iss = it.get("issue") or {}
        key = f'{tid}:{iss.get("id")}'
        out.append(dict(
            id=it["id"], quantity=it["quantity"], issue=iss.get("id"),
            year=iss.get("year"), mint_letter=iss.get("mint_letter"), mintage=iss.get("mintage"),
            issue_comment=iss.get("comment"), grade=it.get("grade"),
            paid=(it.get("price") or {}).get("value"), acquired=it.get("acquisition_date"),
            note=it.get("private_comment"), swap=it.get("for_swap"),
            pictures=[p["url"] for p in it.get("pictures") or []],
            prices=prices.get(key, {}), read=reads.get(key),
        ))
    return sorted(out, key=lambda p: (p["year"] or 0, p["mint_letter"] or "", p["id"]))


def plate(cat_id, tid):
    """La lámina desde la que se abre: sus casillas, cuáles tiene y la nota del curador."""
    if cat_id is None:
        return None
    cat = catalogs[cat_id]
    cells = []
    for mem in cat["members"]:
        mtid = mem.get("numista_type_id")
        owned = any(matches(cat, mem, it) for it in items)
        cells.append(dict(label=mem.get("label"), year=mem.get("year"), tid=mtid, owned=owned,
                          this=mtid == tid))
    notes = [m["variant_note"] for m in cat["members"]
             if m.get("numista_type_id") == tid and m.get("variant_note")]
    return dict(id=cat_id, name=cat.get("short_name") or cat["name"],
                printed_side=cat.get("printed_side"), cells=cells, note=notes[0] if notes else None)


def claims(tid):
    """Las láminas que reclaman el tipo, en el orden de `data/` (el índice ordena por cociente)."""
    return [c.get("short_name") or c["name"] for c in catalogs.values()
            if any(m.get("numista_type_id") == tid for m in c["members"])]


def hole_issue(tid, year):
    for i in issues.get(tid, []):
        if i["year"] == year:
            return i["id"]
    return None


data = dict(spot=spot, cases=[])
urls = {}
for case in CASES:
    tid = case["tid"]
    entry = dict(case, ficha=ficha(tid), pieces=pieces(tid), plate=plate(case["here"], tid),
                 claims=claims(tid), meta={k: meta[tid][k] for k in ("composition", "weightGrams")})
    if not entry["pieces"]:
        iid = hole_issue(tid, 2018)
        key = f"{tid}:{iid}"
        entry["hole"] = dict(issue=iid, prices=prices.get(key, {}), read=reads.get(key))
    data["cases"].append(entry)
    for side in ("obverse", "reverse"):
        u = face_url(tid, side, big=True)
        if u:
            urls[f"{tid}-{side}"] = (u, 640)
    edge = (json.loads(meta[tid]["raw"]).get("edge") or {}).get("picture")
    if edge:
        urls[f"{tid}-edge"] = (edge, 640)
    for p in entry["pieces"]:
        for n, u in enumerate(p["pictures"]):
            urls[f"pieza-{p['id']}-{n}"] = (u, 520)
    if entry["plate"]:
        side = "obverse" if entry["plate"]["printed_side"] == "obverse" else "reverse"
        for cell in entry["plate"]["cells"]:
            if cell["tid"]:
                u = face_url(cell["tid"], side, big=False) or face_url(cell["tid"], "obverse", False)
                if u:
                    urls.setdefault(f"{cell['tid']}-{side}-mini", (u, 132))

json.dump(data, open(f"{OUT}/data.json", "w"), ensure_ascii=False, indent=1)

H = {"User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)",
     "Referer": "https://en.numista.com/"}


def get(kv):
    name, (url, px) = kv
    path = f"{OUT}/fotos/{name}.jpg"
    if os.path.exists(path) and os.path.getsize(path) > 2000:
        return name, True
    try:
        raw = urllib.request.urlopen(urllib.request.Request(url, headers=H), timeout=30).read()
    except Exception as e:
        return name, f"{e}"
    open(path, "wb").write(raw)
    q = "70" if px > 200 else "45"
    subprocess.run(["sips", "-Z", str(px), "-s", "format", "jpeg", "-s", "formatOptions", q,
                    path, "--out", path], capture_output=True)
    return name, True


with concurrent.futures.ThreadPoolExecutor(6) as ex:
    results = list(ex.map(get, urls.items()))
failed = [(n, r) for n, r in results if r is not True]
print(f"{len(data['cases'])} monedas · {len(urls) - len(failed)}/{len(urls)} fotos en {OUT}")
for n, r in failed:
    print("  sin foto:", n, r)
