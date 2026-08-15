#!/usr/bin/env python3
"""Saca de `data/` y de `.local/padre` las tres láminas que la maqueta del #509 enseña.

PROTOTIPO — se tira cuando el ticket se decida. Lo que sobrevive es el README.

Este prototipo necesita **las dos caras de cada tipo**, no la de reposo: lo que se elige es
cómo se declara cuál de las dos estás mirando. Reproduce las reglas de la app y no las
parecidas:

- casilla llena: `CollectionCatalog.memberMatches` entera, con el año del date run
  (`contar-completas-offline`)
- fuera del divisor: `announced` y `unlisted`, como `buildCollectionCatalogAlbum`
- las dos caras de una casilla: `printedFaces` (`AlbumFaces.kt:40-44`), que **no descarta
  nada** por falta de foto — que es justo el fallo que el ticket persigue
- `hasPicture`: `candidates = listOfNotNull(thumbnail, picture)` (`CoinPhotos.kt:10-23`)

Aquí no hay dinero, así que la maqueta sí puede vivir en el repo
(`dinero-fuera-del-repo-publico` no aplica).

    python3 docs/ux/prototipo-marca-cara-509/extract.py
"""
import concurrent.futures
import json
import os
import urllib.request

REPO = os.path.abspath(f"{os.path.dirname(os.path.abspath(__file__))}/../../..")
OUT = "/private/tmp/coindex-privado/marca-cara-509"

# ── las tres láminas, y por qué estas tres ──────────────────────────────────
# La primera es la del #302, donde se decidió el giro: 22 casillas del **mismo tipo**, así que
# la marca de cara se repite 22 veces sobre la misma moneda y es el peor caso de ruido.
# La segunda declara `obverse` (6 de los 74 catálogos lo hacen), que es la única manera de ver
# si una marca que nombra la cara dice la verdad en reposo o sólo cuando se ha volteado.
# La tercera es una lámina de tipos distintos con caras muy diferentes entre sí, para ver la
# marca sobre monedas que no se parecen.
PLATES = [
    ("venezuela-1-bolivar", "datarun"),
    ("espana-2-euros-conmemorativos", "obverso"),
    ("espana-paquillos", "variada"),
]

cache = json.load(open(f"{REPO}/data/numista-type-cache.json"))
items = json.load(open(f"{REPO}/.local/padre/collected_items.json"))
if isinstance(items, dict):
    items = items.get("items", [])


def photo(tid, side):
    """`CoinPhoto`: los candidatos son thumbnail y picture, y `hasPicture` es tenerlos."""
    pic = (cache.get(str(tid)) or {}).get(side) or {}
    url = pic.get("thumbnail") or pic.get("picture")
    return dict(url=url, desc=(pic.get("description") or "").strip(),
                credit=pic.get("picture_copyright")) if url else None


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
    # ADR 0020: el reverso es la cara de reposo salvo que el catálogo declare otra cosa.
    printed = cat.get("printed_side") or "reverse"
    other = "obverse" if printed == "reverse" else "reverse"
    casillas = []
    for member in cat["members"]:
        if member.get("status") in ("unlisted", "announced"):
            continue
        tid = member.get("numista_type_id")
        held = [it for it in items if matches(cat, member, it)]
        casillas.append(dict(
            label=member["label"].split(" · ")[0].strip(), year=member.get("year"), tid=tid,
            owned=bool(held), front=photo(tid, printed), back=photo(tid, other),
        ))
    head, *rest = cat["name"].split(" · ")
    return dict(
        id=cat["id"], role=role, title=cat.get("short_name") or head, subtitle=" · ".join(rest),
        printed=printed, other=other, sv=cat.get("schema_version"),
        owned=sum(1 for c in casillas if c["owned"]), issued=len(casillas), casillas=casillas,
    )


data = dict(plates=[plate(cid, role) for cid, role in PLATES])
os.makedirs(f"{OUT}/fotos", exist_ok=True)
json.dump(data, open(f"{OUT}/data.json", "w"), ensure_ascii=False, indent=1)

# ── las fotos: Numista responde 403 sin Referer de su propio sitio ──────────
urls = {}
for p in data["plates"]:
    for c in p["casillas"]:
        for side in ("front", "back"):
            if c[side]:
                urls[f'{c["tid"]}-{side}'] = c[side]["url"]
HEADERS = {"User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)",
           "Referer": "https://en.numista.com/"}


def fetch(pair):
    key, url = pair
    path = f"{OUT}/fotos/{key}.img"
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

print(f"fotos {got}/{len(urls)}")
for p in data["plates"]:
    sin = sum(1 for c in p["casillas"] if not c["back"])
    print(f'  {p["title"]:<28} {p["owned"]}/{p["issued"]} · reposa en {p["printed"]} · '
          f'{sin} casillas sin segunda cara · {p["role"]}')
