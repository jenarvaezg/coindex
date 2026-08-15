#!/usr/bin/env python3
"""Saca lo que la maqueta del #519 necesita para dibujar el pliegue de «Las cifras» y el de una lámina.

PROTOTIPO — se tira cuando el ticket se decida. Lo que sobrevive es el README.

La pregunta del #519 es **una línea de copy y su sitio**, así que lo que hay que dibujar con datos de
verdad es el pliegue en el que esa línea cae: la altura del bloque del dinero cuando lo hay, y lo que
sube cuando no lo hay. De ahí lo que se extrae:

- el censo y las tres escaleras de «Las cifras», reproduciendo `collectionFigures` y `Ladders`
  (`domain/.../Figures.kt:279`, `domain/.../Referents.kt:89`) sobre la colección del padre
- las láminas y sus importes, que **se reutilizan tal cual** del prototipo del #493
  (`/private/tmp/coindex-privado/cifras-493/data.json` y sus fotos), porque son las mismas cinco y
  volver a pedir el spot no cambia ninguna decisión de esta maqueta

El importe de «Las cifras» es el **suelo de la plata** —gramos x ley x spot—, no el mayor de tres
precios: sin el pase no hay `listings` fuera del teléfono. Es el estado de control («con mercado») y
sólo tiene que ser una cifra plausible del tamaño correcto; el que manda es el del móvil.

**El dinero no se versiona** (`dinero-fuera-del-repo-publico`): la salida va a
/private/tmp/coindex-privado/mercado-ausente-519/, nunca al repo.

    python3 docs/ux/prototipo-mercado-ausente-519/extract.py
"""
import json
import os
import re
import shutil

REPO = os.path.abspath(f"{os.path.dirname(os.path.abspath(__file__))}/../../..")
OUT = "/private/tmp/coindex-privado/mercado-ausente-519"
FROM_493 = "/private/tmp/coindex-privado/cifras-493"

FIN = re.compile(r"(?:plata|silver)\s*\.?([0-9]{3}(?:[.,][0-9]+)?)")
TROY_OZ_G = 31.1034768

# ── las tres escaleras (domain/.../Referents.kt:89-125) ─────────────────────
LADDERS = [
    ("weight", "kg", "todas juntas pesan",
     [("ladrillo", 2.0), ("gato", 4.5), ("bola de bolos", 7.26), ("neumático", 9.5),
      ("labrador", 30.0)]),
    ("row", "m", "una al lado de otra llegan a",
     [("bici", 1.8), ("coche", 4.4), ("autobús", 12.0), ("camión", 16.5), ("ballena", 25.0)]),
    ("stack", "cm", "una encima de otra levantan",
     [("taburete", 45.0), ("pastor", 60.0), ("encimera", 90.0), ("pomo", 100.0),
      ("persona", 170.0)]),
]

cache = json.load(open(f"{REPO}/data/numista-type-cache.json"))
items = json.load(open(f"{REPO}/.local/padre/collected_items.json"))
if isinstance(items, dict):
    items = items.get("items", [])


def meta(type_id):
    return cache.get(str(type_id)) or {}


def positive(value):
    """`MagnitudeSum.add`: lo no finito y lo <= 0 no entra, y tampoco cuenta como medido."""
    try:
        value = float(value)
    except (TypeError, ValueError):
        return None
    return value if value > 0 else None


def fineness(type_id):
    comp = meta(type_id).get("composition") or {}
    match = FIN.search((comp.get("text") or "").lower()) if isinstance(comp, dict) else None
    if not match:
        return None
    value = float(match.group(1).replace(",", ".")) / 1000
    return value if 0 < value <= 1 else None


def figures():
    """`collectionFigures`, con el denominador de cada magnitud y la extrapolación de la pila."""
    pieces, issuers = 0, set()
    sums = {name: [0.0, 0] for name in ("weight", "silver", "row", "stack")}

    def add(name, amount, quantity):
        amount = positive(amount)
        if amount is None:
            return
        sums[name][0] += amount * quantity
        sums[name][1] += quantity

    for item in items:
        quantity = max(int(item.get("quantity") or 1), 1)
        pieces += quantity
        type_id = (item.get("type") or {}).get("id")
        info = meta(type_id)
        issuer = (info.get("issuer") or {}).get("code")
        if issuer:
            issuers.add(issuer)
        grams = positive(info.get("weight"))
        add("weight", grams, quantity)
        ley = fineness(type_id)
        add("silver", grams * ley if grams and ley else None, quantity)
        add("row", (positive(info.get("size")) or 0) / 1_000.0, quantity)
        add("stack", (positive(info.get("thickness")) or 0) / 10.0, quantity)

    def magnitude(name):
        total, measured = sums[name]
        extrapolated = None if measured <= 0 else (
            total if measured == pieces else total * pieces / measured)
        return dict(value=total, measured=measured, complete=measured == pieces,
                    extrapolated=extrapolated)

    return dict(pieces=pieces, issuers=len(issuers),
                types=len({(i.get("type") or {}).get("id") for i in items}),
                **{name: magnitude(name) for name in sums})


def place(rungs, amount):
    """`Ladder.place`: ordinal, la colección interpolada entre sus dos vecinos."""
    last = len(rungs) - 1
    if amount <= rungs[0][1]:
        return dict(fraction=0.0, passed=None, next=rungs[0])
    if amount >= rungs[last][1]:
        return dict(fraction=1.0, passed=rungs[last], next=None)
    lower = max(i for i, r in enumerate(rungs) if r[1] <= amount)
    upper = lower + 1
    span = rungs[upper][1] - rungs[lower][1]
    within = 0.0 if span <= 0 else (amount - rungs[lower][1]) / span
    return dict(fraction=(lower + within) / last, passed=rungs[lower], next=rungs[upper])


def ladders(fig):
    amounts = dict(
        weight=fig["weight"]["value"] / 1_000.0,
        row=fig["row"]["value"],
        stack=fig["stack"]["extrapolated"] or 0.0,
    )
    out = []
    for kind, unit, statement, rungs in LADDERS:
        amount = amounts[kind]
        out.append(dict(kind=kind, unit=unit, statement=statement, amount=amount,
                        # Sólo la pila se declara extrapolada, que es el «unos» del #316.
                        approximate=kind == "stack" and not fig["stack"]["complete"],
                        rungs=[dict(name=n, amount=a) for n, a in rungs],
                        placement=place(rungs, amount)))
    return out


def main():
    os.makedirs(OUT, exist_ok=True)
    plates = json.load(open(f"{FROM_493}/data.json"))
    fig = figures()
    spot = plates["spot"]
    silver_eur_g = spot["eur_oz"] / TROY_OZ_G
    data = dict(
        figures=fig,
        ladders=ladders(fig),
        # El control «con mercado»: el suelo de la plata de la colección entera. La app enseñaría el
        # mayor de tres precios, así que el de verdad es más alto.
        money=dict(eur=fig["silver"]["value"] * silver_eur_g,
                   valued=fig["silver"]["measured"], pieces=fig["pieces"]),
        spot=spot,
        plates=plates["plates"],
    )
    json.dump(data, open(f"{OUT}/data.json", "w"), ensure_ascii=False)
    if not os.path.isdir(f"{OUT}/fotos"):
        shutil.copytree(f"{FROM_493}/fotos", f"{OUT}/fotos")
    print(f"{fig['pieces']} piezas de {fig['issuers']} emisores -> {OUT}/data.json")
    for ladder in data["ladders"]:
        print(f"  {ladder['kind']}: {ladder['amount']:.2f} {ladder['unit']}")


if __name__ == "__main__":
    main()
