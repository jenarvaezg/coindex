#!/usr/bin/env python3
"""Saca de `data/` y de `.local/padre` lo que la maqueta enseña, con las reglas de la app.

- evidencia y casilla llena: `CollectionCatalog.memberMatches` / `isEvidencedBy`
- cara de la casilla: `printed_side` del catálogo, y si no lo declara, reverso antes que
  anverso (`AlbumFaces.coinAlbumFaces`)
- suelo de la plata: gramos x ley leída del `composition.text` (`Valuation.silverFineness`)
"""
import json
import glob
import os
import re
import urllib.request
import concurrent.futures

REPO = "/Users/jose/jenarvaezg/coindex"
HERE = os.path.dirname(os.path.abspath(__file__))
SPOT_USD, EURUSD = 66.304001, 0.86655
SPOT = SPOT_USD * EURUSD
FIN = re.compile(r"(?:plata|silver)\s*\.?([0-9]{3}(?:[.,][0-9]+)?)")

cache = json.load(open(f"{REPO}/data/numista-type-cache.json"))
cats = [json.load(open(f)) for f in glob.glob(f"{REPO}/data/collection-catalogs/*.json")]
items = json.load(open(f"{REPO}/.local/padre/collected_items.json"))
if isinstance(items, dict):
    items = items.get("items", [])


def face(tid, printed_side=None):
    """La foto que la casilla enseña: printed_side si el catálogo lo declara, si no reverso."""
    e = cache.get(str(tid)) or {}
    order = ("obverse", "reverse") if printed_side == "obverse" else ("reverse", "obverse")
    for side in order:
        s = e.get(side) or {}
        u = s.get("thumbnail") or s.get("picture")
        if u:
            return u
    return None


def fineness(tid):
    c = (cache.get(str(tid)) or {}).get("composition") or {}
    m = FIN.search((c.get("text") or "").lower()) if isinstance(c, dict) else None
    if not m:
        return None
    v = float(m.group(1).replace(",", ".")) / 1000
    return v if 0 < v <= 1 else None


def floor_eur(tid):
    g = (cache.get(str(tid)) or {}).get("weight")
    f = fineness(tid)
    return float(g) * f / 31.1035 * SPOT if g and f else None


def matches(cat, mem, it):
    tid = mem.get("numista_type_id")
    if not tid or it.get("quantity", 0) <= 0 or it["type"]["id"] != tid:
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
        ry = iss.get("year") if iss.get("year") is not None else iss.get("gregorian_year")
        return ry == mem.get("year")
    return True


def evidenced(cat):
    return any(
        it.get("quantity", 0) > 0
        and any(
            m.get("numista_type_id") == it["type"]["id"]
            and (not (m.get("numista_issue_ids") or [])
                 or (it.get("issue") or {}).get("id") in m["numista_issue_ids"])
            for m in cat.get("members", [])
        )
        for it in items
    )


mine, explor = [], []
for c in cats:
    mems, n, ps = c.get("members", []), len(c.get("members", [])), c.get("printed_side")
    name = c.get("short_name") or c["name"]
    if evidenced(c):
        owned = sum(1 for m in mems if any(matches(c, m, it) for it in items))
        rep = next((m for m in mems if any(matches(c, m, it) for it in items)), mems[0])
        mine.append(dict(id=c["id"], name=name, owned=owned, issued=n,
                         tid=rep.get("numista_type_id"), url=face(rep.get("numista_type_id"), ps)))
    elif n < 20:
        cas = [dict(label=m["label"].split(";")[0].strip(), year=m.get("year"),
                    tid=m.get("numista_type_id"), url=face(m.get("numista_type_id"), ps),
                    floor=floor_eur(m.get("numista_type_id"))) for m in mems]
        floors = [m["floor"] for m in cas]
        explor.append(dict(
            id=c["id"], name=name, full=c["name"], issued=n, status=c.get("series_status"),
            issuer=c.get("issuer_code"), updated=c.get("updated_at") or c.get("source", {}).get("read_at"),
            entry=(sum(floors) if n <= 10 and all(floors) else None),
            casillas=cas, tid=cas[0]["tid"], url=cas[0]["url"]))

mine.sort(key=lambda c: -(c["owned"] / c["issued"]))
explor.sort(key=lambda c: c["issued"])
json.dump(dict(spot=SPOT, mine=mine, explor=explor),
          open(f"{HERE}/data.json", "w"), ensure_ascii=False, indent=1)

# ── las fotos: Numista responde 403 sin Referer de su propio sitio ──────────
urls = {c["tid"]: c["url"] for c in mine if c["url"]}
for c in explor:
    for m in c["casillas"]:
        if m["url"]:
            urls[m["tid"]] = m["url"]
os.makedirs(f"{HERE}/fotos", exist_ok=True)
H = {"User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)",
     "Referer": "https://en.numista.com/"}


def get(kv):
    tid, u = kv
    p = f"{HERE}/fotos/{tid}.img"
    if os.path.exists(p) and os.path.getsize(p) > 2000:
        return True
    try:
        open(p, "wb").write(urllib.request.urlopen(urllib.request.Request(u, headers=H),
                                                   timeout=30).read())
        return True
    except Exception:
        return False


with concurrent.futures.ThreadPoolExecutor(6) as ex:
    ok = sum(1 for r in ex.map(get, urls.items()) if r)
print(f"spot {SPOT:.2f} €/oz · mías {len(mine)} · explorables {len(explor)} · fotos {ok}/{len(urls)}")
