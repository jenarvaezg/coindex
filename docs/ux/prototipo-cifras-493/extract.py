#!/usr/bin/env python3
"""Saca de `data/` y de `.local/padre` las tres láminas que la maqueta del #493 enseña.

PROTOTIPO — se tira cuando el ticket se decida. Lo que sobrevive es el README.

Reproduce las reglas de la app y no las parecidas:

- casilla llena: `CollectionCatalog.memberMatches` entera, con el año del date run y los
  `numista_issue_ids` del issue run (`contar-completas-offline`)
- fuera del divisor: `announced` y `unlisted`, como `buildCollectionCatalogAlbum`
- lámina alcanzable: `CollectionCatalog.isEvidencedBy`
- coste de cerrar: sólo las láminas a <= 10 casillas, que es `HOLE_THRESHOLD_SLOTS` (ADR 0028 §1)
- suelo de la plata: gramos x ley del `composition.text` (`Valuation.silverFineness`), con el
  spot y el cambio de los dos endpoints que lee `SilverSpot.kt`

**El dinero no se versiona** (`dinero-fuera-del-repo-publico`): `data.json`, las fotos y la
maqueta salen a /private/tmp/coindex-privado/cifras-493/, nunca al repo.

    python3 docs/ux/prototipo-cifras-493/extract.py
"""
import concurrent.futures
import glob
import json
import os
import re
import urllib.request

REPO = os.path.abspath(f"{os.path.dirname(os.path.abspath(__file__))}/../../..")
OUT = "/private/tmp/coindex-privado/cifras-493"

# ── las tres láminas, y por qué estas tres ──────────────────────────────────
# Las dos primeras son las que hoy tienen las dos cifras a la vez: a una casilla de cerrarse.
# La tercera es una completa, para ver qué hace cada variante cuando ya no hay hueco (la
# tercera pregunta del ticket). Se eligen a mano porque son el caso del ticket, no una muestra.
# La cuarta se añadió al elegir los sellos de la C: con un solo hueco el sello se ve, pero lo
# que hay que ver es qué hace en una lámina con siete, y si la cabecera queda dominada por el
# coste cuando lo que falta vale más que lo que hay.
PLATES = [
    ("venezuela-100-bolivares-plata", "holgada"),
    ("portugal-20-escudos-plata", "colision"),
    ("queens-beasts-uk-2oz", "varios"),
    ("australia-silver-kangaroo-1oz-bullion", "umbral"),
    ("portugal-1983-exposicion-europea-de-arte", "cerrada"),
]

SILVER_URL = "https://api.gold-api.com/price/XAG"      # SilverSpot.kt:20
RATE_URL = "https://api.frankfurter.dev/v1/latest?base=USD&symbols=EUR"  # SilverSpot.kt:21
FIN = re.compile(r"(?:plata|silver)\s*\.?([0-9]{3}(?:[.,][0-9]+)?)")
HOLE_THRESHOLD_SLOTS = 10

cache = json.load(open(f"{REPO}/data/numista-type-cache.json"))
items = json.load(open(f"{REPO}/.local/padre/collected_items.json"))
if isinstance(items, dict):
    items = items.get("items", [])


def spot():
    """El spot de la plata en euros por onza, de los dos endpoints de la app."""
    def read(url):
        req = urllib.request.Request(url, headers={"User-Agent": "coindex-prototipo"})
        return json.load(urllib.request.urlopen(req, timeout=20))
    silver, rate = read(SILVER_URL), read(RATE_URL)
    return dict(eur_oz=silver["price"] * rate["rates"]["EUR"], usd_oz=silver["price"],
                eurusd=rate["rates"]["EUR"], at=silver["updatedAt"], rate_at=rate["date"])


SPOT = spot()


def fineness(tid):
    comp = (cache.get(str(tid)) or {}).get("composition") or {}
    m = FIN.search((comp.get("text") or "").lower()) if isinstance(comp, dict) else None
    if not m:
        return None
    v = float(m.group(1).replace(",", ".")) / 1000
    return v if 0 < v <= 1 else None


def floor_eur(tid):
    """El suelo de la plata de un tipo. La app enseñaría el mayor de tres precios: más alto."""
    grams = (cache.get(str(tid)) or {}).get("weight")
    fine = fineness(tid)
    return float(grams) * fine / 31.1035 * SPOT["eur_oz"] if grams and fine else None


def face(tid, printed_side=None):
    """`AlbumFaces`: la cara que el catálogo declara y, si no declara ninguna, el reverso."""
    entry = cache.get(str(tid)) or {}
    order = ("obverse", "reverse") if printed_side == "obverse" else ("reverse", "obverse")
    for side in order:
        pic = entry.get(side) or {}
        url = pic.get("thumbnail") or pic.get("picture")
        if url:
            return url
    return None


def matches(cat, member, item):
    """`CollectionCatalog.memberMatches`, entera."""
    tid = member.get("numista_type_id")
    if not tid or item.get("quantity", 0) <= 0 or item["type"]["id"] != tid:
        return False
    ids = member.get("numista_issue_ids") or []
    issue = item.get("issue") or {}
    if ids and issue.get("id") not in ids:
        return False
    sv = cat.get("schema_version")
    if sv == 5:
        return bool(ids)
    if sv == 2:
        recorded = issue.get("year") if issue.get("year") is not None else issue.get("gregorian_year")
        return recorded == member.get("year")
    return True


def plate(catalog_id, role):
    cat = json.load(open(f"{REPO}/data/collection-catalogs/{catalog_id}.json"))
    printed = cat.get("printed_side")
    casillas, value, cost, pieces = [], 0.0, 0.0, 0
    for member in cat["members"]:
        if member.get("status") in ("unlisted", "announced"):
            continue
        tid = member.get("numista_type_id")
        floor = floor_eur(tid)
        held = [it for it in items if matches(cat, member, it)]
        quantity = sum(max(1, it.get("quantity", 1)) for it in held)
        if held and floor:
            value += floor * quantity
            pieces += quantity
        elif not held and floor:
            cost += floor
        casillas.append(dict(
            label=member["label"].split(" · ")[0].strip(), year=member.get("year"), tid=tid,
            owned=bool(held), quantity=quantity, floor=floor, url=face(tid, printed),
        ))
    owned = sum(1 for c in casillas if c["owned"])
    missing = len(casillas) - owned
    head, *rest = cat["name"].split(" · ")
    return dict(
        id=cat["id"], role=role, title=cat.get("short_name") or head, subtitle=" · ".join(rest),
        family=cat.get("family"), weight_millioz=cat.get("weight_millioz"),
        updated=cat.get("updated_at"), source=cat.get("source"), sv=cat.get("schema_version"),
        owned=owned, issued=len(casillas), missing=missing, casillas=casillas,
        value=value, pieces=pieces,
        # El umbral del ADR 0028 §1 decide si el coste existe, no si cabe en pantalla.
        cost=cost if 0 < missing <= HOLE_THRESHOLD_SLOTS else None,
    )


data = dict(spot=SPOT, plates=[plate(cid, role) for cid, role in PLATES])
os.makedirs(f"{OUT}/fotos", exist_ok=True)
json.dump(data, open(f"{OUT}/data.json", "w"), ensure_ascii=False, indent=1)

# ── las fotos: Numista responde 403 sin Referer de su propio sitio ──────────
urls = {c["tid"]: c["url"] for p in data["plates"] for c in p["casillas"] if c["url"]}
HEADERS = {"User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)",
           "Referer": "https://en.numista.com/"}


def fetch(pair):
    tid, url = pair
    path = f"{OUT}/fotos/{tid}.img"
    if os.path.exists(path) and os.path.getsize(path) > 2000:
        return True
    try:
        request = urllib.request.Request(url, headers=HEADERS)
        open(path, "wb").write(urllib.request.urlopen(request, timeout=30).read())
        return True
    except Exception:
        return False


with concurrent.futures.ThreadPoolExecutor(6) as pool:
    got = sum(1 for ok in pool.map(fetch, urls.items()) if ok)

print(f"spot {SPOT['eur_oz']:.2f} €/oz ({SPOT['at']}) · fotos {got}/{len(urls)}")
for p in data["plates"]:
    cost = "—" if p["cost"] is None else f"{p['cost']:.0f} €"
    print(f"  {p['title']:<34} {p['owned']}/{p['issued']} · dentro {p['value']:6.0f} € · "
          f"cerrar {cost:>6} · {p['role']}")
