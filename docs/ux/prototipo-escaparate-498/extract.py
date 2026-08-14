#!/usr/bin/env python3
"""Saca de `data/` y de `.local/padre` lo que la maqueta del #498 enseña.

PROTOTIPO — se tira cuando el ticket se decida. Lo que sobrevive es el README.

Reproduce las reglas de la app y no las parecidas:

- casilla llena: `CollectionCatalog.memberMatches` entera (`contar-completas-offline`)
- lámina alcanzable: la evidencia de `CoindexRepository.resolvePlate` (ADR 0021 §7)
- fuera del divisor: `announced` y `unlisted`, como `buildCollectionCatalogAlbum`
- **sin el umbral de 10 casillas**, que es la corrección que pide el #498: era la regla del
  reproche de *tus* huecos (ADR 0028 §1) y una lámina ajena no reprocha nada
- lo que cuesta tasar una lámina: un `/prices` por hueco más un `/types/{id}/issues` por tipo
  cuyo fichero curado no nombra la emisión (`valuationPlan` + `wishCallsPerMonth`)
- suelo de la plata: gramos x ley del `composition.text` (`Valuation.silverFineness`), con el
  spot y el cambio de los dos endpoints que lee `SilverSpot.kt`

**El dinero no se versiona** (`dinero-fuera-del-repo-publico`): `data.json`, las fotos y la
maqueta salen a /private/tmp/coindex-privado/escaparate-498/, nunca al repo.

    python3 docs/ux/prototipo-escaparate-498/extract.py
"""
import concurrent.futures
import glob
import json
import os
import re
import urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.abspath(f"{HERE}/../../..")
OUT = "/private/tmp/coindex-privado/escaparate-498"

SILVER_URL = "https://api.gold-api.com/price/XAG"                        # SilverSpot.kt:20
RATE_URL = "https://api.frankfurter.dev/v1/latest?base=USD&symbols=EUR"  # SilverSpot.kt:21
FIN = re.compile(r"(?:plata|silver)\s*\.?([0-9]{3}(?:[.,][0-9]+)?)")
SHELF_MAX_SLOTS = 20      # el corte del #282: las curadas sin evidencia de menos de 20 casillas
OUT_OF_ALBUM = ("unlisted", "announced")

# ── las siete marcas de «Lo que busco», y por qué son inventadas ────────────
# El #497 salió ayer (v1.3.0): el padre no ha marcado nada todavía, así que la lista de la
# maqueta se compone a mano. Cinco marcas caen en tres láminas suyas —el caso normal— y **dos
# en dos láminas del escaparate**, que es el caso que el #494 tiene abierto: un coste de entrar
# con dos edades. Se eligen por lámina y no por casilla: la primera casilla vacía con precio.
WISHED_MINE = 3           # cuántas láminas propias llevan marca
WISHED_MINE_SLOTS = 5     # y cuántas casillas entre las tres
WISHED_SHELF = 2          # cuántas láminas del escaparate llevan marca

cache = json.load(open(f"{REPO}/data/numista-type-cache.json"))
catalogs = [json.load(open(f)) for f in sorted(glob.glob(f"{REPO}/data/collection-catalogs/*.json"))]
items = json.load(open(f"{REPO}/.local/padre/collected_items.json"))
if isinstance(items, dict):
    items = items.get("items", [])


def spot():
    """El spot de la plata en euros por onza, de los dos endpoints de la app."""
    def read(url):
        request = urllib.request.Request(url, headers={"User-Agent": "coindex-prototipo"})
        return json.load(urllib.request.urlopen(request, timeout=20))
    silver, rate = read(SILVER_URL), read(RATE_URL)
    return dict(eur_oz=silver["price"] * rate["rates"]["EUR"], usd_oz=silver["price"],
                eurusd=rate["rates"]["EUR"], at=silver["updatedAt"], rate_at=rate["date"])


SPOT = spot()


def fineness(tid):
    comp = (cache.get(str(tid)) or {}).get("composition") or {}
    match = FIN.search((comp.get("text") or "").lower()) if isinstance(comp, dict) else None
    if not match:
        return None
    value = float(match.group(1).replace(",", ".")) / 1000
    return value if 0 < value <= 1 else None


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
        picture = entry.get(side) or {}
        url = picture.get("thumbnail") or picture.get("picture")
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


def evidenced(cat):
    """La evidencia que abre una lámina: un tipo del catálogo en la colección, con su emisión."""
    return any(
        item.get("quantity", 0) > 0 and any(
            member.get("numista_type_id") == item["type"]["id"] and (
                not (member.get("numista_issue_ids") or [])
                or (item.get("issue") or {}).get("id") in member["numista_issue_ids"]
            )
            for member in cat.get("members", [])
        )
        for item in items
    )


def album(cat):
    """Las casillas medibles de una lámina, con quién la llena y qué cuesta el hueco."""
    printed = cat.get("printed_side")
    casillas = []
    for member in cat["members"]:
        if member.get("status") in OUT_OF_ALBUM:
            continue
        tid = member.get("numista_type_id")
        held = [it for it in items if matches(cat, member, it)]
        casillas.append(dict(
            label=member["label"].split(" · ")[0].split(";")[0].strip(),
            year=member.get("year"), tid=tid, url=face(tid, printed),
            owned=bool(held), floor=floor_eur(tid),
            # Un hueco cuyo fichero curado nombra su emisión cuesta una llamada; los demás, dos.
            issues_named=bool(member.get("numista_issue_ids")),
        ))
    return casillas


def calls_for(holes):
    """`valuationPlan` leído a mano: un /prices por hueco y un /issues por tipo sin emisión."""
    lookups = {h["tid"] for h in holes if not h["issues_named"]}
    return len(holes) + len(lookups)


def head(cat):
    name, *rest = cat["name"].split(" · ")
    return cat.get("short_name") or name, " · ".join(rest)


mine, shelf = [], []
for cat in catalogs:
    casillas = album(cat)
    if not casillas:
        continue
    title, subtitle = head(cat)
    owned = [c for c in casillas if c["owned"]]
    holes = [c for c in casillas if not c["owned"]]
    row = dict(
        id=cat["id"], title=title, subtitle=subtitle, issuer=cat.get("issuer_code"),
        family=cat.get("family"), issued=len(casillas), owned=len(owned),
        updated=cat.get("updated_at") or (cat.get("source") or {}).get("read_at"),
        casillas=casillas,
        # El coste entero de la lámina, sin el umbral del ADR 0028 §1: lo que el #498 corrige.
        entry=sum(h["floor"] for h in holes if h["floor"]),
        unpriced=sum(1 for h in holes if not h["floor"]),
        calls=calls_for(holes),
        tid=(owned[0] if owned else casillas[0])["tid"],
    )
    (mine if evidenced(cat) else shelf).append(row)

shelf = [c for c in shelf if c["issued"] < SHELF_MAX_SLOTS]
mine.sort(key=lambda c: -(c["owned"] / c["issued"]))
shelf.sort(key=lambda c: c["issued"])

# ── las siete marcas ───────────────────────────────────────────────────────
# Se reparten por lámina: las tres propias más huecas primero (que es donde un coleccionista
# mira antes de una feria) y las dos del escaparate más baratas de tasar.
wishes = []
sources = [c for c in mine if any(not x["owned"] and x["floor"] for x in c["casillas"])]
sources.sort(key=lambda c: -(c["issued"] - c["owned"]))
for i, cat in enumerate(sources[:WISHED_MINE]):
    holes = [c for c in cat["casillas"] if not c["owned"] and c["floor"]]
    # 2, 2 y 1: cinco casillas en tres láminas, que es lo que dice `wishCensusLabel`.
    for hole in holes[: (2 if i < WISHED_MINE_SLOTS - WISHED_MINE else 1)]:
        wishes.append(dict(plate=cat["title"], plate_id=cat["id"], shelf=False, **hole))
for cat in sorted(shelf, key=lambda c: c["calls"])[:WISHED_SHELF]:
    hole = next((c for c in cat["casillas"] if c["floor"]), cat["casillas"][0])
    wishes.append(dict(plate=cat["title"], plate_id=cat["id"], shelf=True, **hole))

data = dict(spot=SPOT, mine=mine, shelf=shelf, wishes=wishes)
os.makedirs(f"{OUT}/fotos", exist_ok=True)
json.dump(data, open(f"{OUT}/data.json", "w"), ensure_ascii=False, indent=1)

# ── las fotos: Numista responde 403 sin Referer de su propio sitio ──────────
urls = {c["tid"]: next((x["url"] for x in c["casillas"] if x["tid"] == c["tid"] and x["url"]), None)
        for c in mine}
for cat in shelf:
    for cas in cat["casillas"]:
        if cas["url"]:
            urls[cas["tid"]] = cas["url"]
for wish in wishes:
    if wish["url"]:
        urls[wish["tid"]] = wish["url"]
urls = {tid: url for tid, url in urls.items() if tid and url}
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

types = {c["tid"] for cat in shelf for c in cat["casillas"] if c["tid"]}
print(f"spot {SPOT['eur_oz']:.2f} €/oz ({SPOT['at'][:10]}) · fotos {got}/{len(urls)}")
print(f"láminas propias {len(mine)} · escaparate {len(shelf)} · tipos del escaparate {len(types)}")
print(f"llamadas de las {len(shelf)}: {sum(c['calls'] for c in shelf)} · "
      f"la más cara {max(c['calls'] for c in shelf)} · "
      f"con suelo completo {sum(1 for c in shelf if not c['unpriced'])}")
print(f"casillas: {sum(1 for c in shelf if c['issued'] <= 10)} láminas de <=10 y "
      f"{sum(1 for c in shelf if c['issued'] > 10)} de 11-19")
for cat in shelf:
    print(f"  {cat['title'][:38]:<38} {cat['issued']:>2} casillas · {cat['calls']:>3} llamadas · "
          f"{cat['entry']:7.0f} € · sin precio {cat['unpriced']}")
print("marcas:", ", ".join(f"{w['plate']}/{w['label']}{'*' if w['shelf'] else ''}" for w in wishes))
