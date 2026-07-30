#!/usr/bin/env python3
"""Graba un fixture de la API de Numista, gastando presupuesto de forma deliberada.

Los tests nunca tocan la red: leen `fixtures/numista/`. Este script es la única forma de
refrescar ese conjunto, y exige `--confirm-live-api` justamente para que no ocurra por
accidente. Sustituye al binario Rust `record-fixtures`, retirado con el workspace Rust
(recuperable en el tag `rust-frozen`).

    scripts/record-fixture.py --confirm-live-api --type-id 404044

Las capturas de colección son privadas y nunca van al repositorio: para eso, `--user-id`
exige un `--output-dir` fuera del árbol.
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
PUBLIC_FIXTURES = pathlib.Path(__file__).resolve().parent.parent / "fixtures" / "numista"


def request(path: str, api_key: str, token: str | None = None) -> dict:
    request_object = urllib.request.Request(f"{BASE_URL}{path}")
    request_object.add_header("Numista-API-Key", api_key)
    if token:
        request_object.add_header("Authorization", f"Bearer {token}")
    try:
        with urllib.request.urlopen(request_object, timeout=30) as response:
            return json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as error:
        body = error.read().decode("utf-8", "replace")[:400]
        sys.exit(f"Numista devolvió HTTP {error.code} en {path}: {body}")


def access_token(api_key: str) -> str:
    # Omitir scope=view_collection produce un 401 con un mensaje engañoso: es el error más
    # común con esta API.
    payload = request(
        "/oauth_token?grant_type=client_credentials&scope=view_collection", api_key
    )
    token = payload.get("access_token")
    if not token:
        sys.exit("la respuesta de oauth_token no trae access_token")
    return token


def write(path: pathlib.Path, payload: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8") as handle:
        json.dump(payload, handle, ensure_ascii=False, indent=2)
        handle.write("\n")
    print(f"escrito {path} ({path.stat().st_size} bytes)")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--confirm-live-api",
        action="store_true",
        help="obligatorio: confirma que esta ejecución gasta presupuesto real de la API",
    )
    parser.add_argument("--type-id", type=int, help="captura pública de /types/{id}?lang=es")
    parser.add_argument(
        "--user-id", type=int, help="captura privada de la colección (requiere --output-dir)"
    )
    parser.add_argument(
        "--output-dir",
        type=pathlib.Path,
        help="destino obligatorio para capturas de colección, fuera del repositorio",
    )
    arguments = parser.parse_args()

    if not arguments.confirm_live_api:
        sys.exit("falta --confirm-live-api: este script gasta presupuesto real de la API")
    if not arguments.type_id and not arguments.user_id:
        sys.exit("indica --type-id o --user-id")

    api_key = os.environ.get("NUMISTA_API_KEY", "").strip()
    if not api_key:
        sys.exit("exporta NUMISTA_API_KEY con tu clave de Numista")

    if arguments.type_id:
        payload = request(f"/types/{arguments.type_id}?lang=es", api_key)
        destination = arguments.output_dir or PUBLIC_FIXTURES
        write(destination / f"type_{arguments.type_id}_es.json", payload)

    if arguments.user_id:
        if not arguments.output_dir:
            sys.exit(
                "una captura de colección es privada: pasa --output-dir fuera del repositorio"
            )
        if PUBLIC_FIXTURES in arguments.output_dir.resolve().parents or (
            arguments.output_dir.resolve() == PUBLIC_FIXTURES
        ):
            sys.exit("--output-dir no puede apuntar al conjunto público de fixtures")
        payload = request(
            f"/users/{arguments.user_id}/collected_items", api_key, access_token(api_key)
        )
        write(arguments.output_dir / "collected_items.json", payload)


if __name__ == "__main__":
    main()
