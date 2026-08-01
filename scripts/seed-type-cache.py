#!/usr/bin/env python3
"""Siembra fichas de tipo en `data/numista-type-cache.json`, una llamada por tipo nuevo.

Curar un catálogo incluye sembrar sus fichas (#39): la caché existe para que la lámina
dibuje también lo que al coleccionista le falta, así que un hueco solo se ve en el móvil
que **no** tiene la moneda. `TypeCacheSeedTest` lo exige, y hasta ahora la siembra se hacía
a mano.

    set -a; . ./.env; set +a
    scripts/seed-type-cache.py --confirm-live-api 45416 45417 45418

Los tipos que ya están en la caché se omiten sin gastar llamada: el gasto es exactamente el
número de tipos nuevos, y `--dry-run` lo dice antes de gastarlo.
"""

from __future__ import annotations

import argparse
import json
import os
import pathlib
import sys
import urllib.error
import urllib.request

BASE_URL = "https://api.numista.com/v3"
TYPE_CACHE = pathlib.Path(__file__).resolve().parent.parent / "data" / "numista-type-cache.json"


def request(path: str, api_key: str) -> dict:
    request_object = urllib.request.Request(f"{BASE_URL}{path}")
    request_object.add_header("Numista-API-Key", api_key)
    try:
        with urllib.request.urlopen(request_object, timeout=30) as response:
            return json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as error:
        body = error.read().decode("utf-8", "replace")[:400]
        sys.exit(f"Numista devolvió HTTP {error.code} en {path}: {body}")


def load_cache() -> dict:
    with TYPE_CACHE.open(encoding="utf-8") as handle:
        return json.load(handle)


def write_cache(cache: dict) -> None:
    # Las claves del primer nivel van ordenadas como cadena, y el valor conserva el orden de
    # campos que trajo la API: así el diff de una siembra es solo lo sembrado.
    ordered = {key: cache[key] for key in sorted(cache)}
    with TYPE_CACHE.open("w", encoding="utf-8") as handle:
        json.dump(ordered, handle, ensure_ascii=False, indent=1)
        handle.write("\n")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--confirm-live-api",
        action="store_true",
        help="obligatorio: confirma que esta ejecución gasta presupuesto real de la API",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="dice cuántas llamadas costaría y no gasta ninguna",
    )
    parser.add_argument("type_ids", type=int, nargs="+", help="ids de tipo de Numista a sembrar")
    arguments = parser.parse_args()

    cache = load_cache()
    pending = [
        type_id
        for type_id in dict.fromkeys(arguments.type_ids)
        if str(type_id) not in cache
    ]
    already = len(dict.fromkeys(arguments.type_ids)) - len(pending)
    print(f"{len(pending)} tipos por sembrar, {already} ya en la caché de {len(cache)} fichas")
    if arguments.dry_run or not pending:
        return
    if not arguments.confirm_live_api:
        sys.exit("falta --confirm-live-api: este script gasta presupuesto real de la API")

    for index, type_id in enumerate(pending, start=1):
        payload = request(f"/types/{type_id}?lang=es", api_key=api_key())
        if payload.get("id") != type_id:
            sys.exit(f"la ficha de {type_id} dice id {payload.get('id')}: no se siembra nada")
        cache[str(type_id)] = payload
        title = payload.get("title", "(sin título)")
        print(f"[{index}/{len(pending)}] {type_id} · {title}")

    write_cache(cache)
    print(f"caché escrita con {len(cache)} fichas")


def api_key() -> str:
    key = os.environ.get("NUMISTA_API_KEY", "").strip()
    if not key:
        sys.exit("exporta NUMISTA_API_KEY con tu clave de Numista")
    return key


if __name__ == "__main__":
    main()
