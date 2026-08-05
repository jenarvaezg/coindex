#!/usr/bin/env python3
"""Un issue único del repo por informe, idempotente: abrir, reescribir, cerrar.

Lo comparten los informes de curación (`stale-catalogs.py`, `weight-deviations.py`).
Cada uno trae su título exacto y su marcador HTML, y el par identifica su issue: el
título lo hace legible en la lista y el marcador lo hace inequívoco, porque la búsqueda
de GitHub es por subcadena y también pilla los tickets del mapa que hablan del informe.

Nunca falla en silencio: si `gh` devuelve error, o si hay más de un issue con el mismo
marcador, sale con mensaje. Lo que no hace es decidir *cuándo* hay deuda — eso es del
informe que lo llama.
"""

from __future__ import annotations

import json
import os
import subprocess
import sys


def run_gh(args: list[str], *, input_text: str | None = None) -> str:
    env = os.environ.copy()
    if "GH_TOKEN" not in env and "GITHUB_TOKEN" in env:
        env["GH_TOKEN"] = env["GITHUB_TOKEN"]
    completed = subprocess.run(
        ["gh", *args],
        check=False,
        capture_output=True,
        text=True,
        input=input_text,
        env=env,
    )
    if completed.returncode != 0:
        detail = (completed.stderr or completed.stdout or "").strip()
        sys.exit(f"gh {' '.join(args)} falló ({completed.returncode}): {detail}")
    return completed.stdout.strip()


def find_report_issue(title: str, marker: str) -> dict | None:
    # Título exacto + marcador en el cuerpo: el issue es único e idempotente.
    # La búsqueda por subcadena también pilla los tickets del mapa que lo citan; se filtran.
    raw = run_gh(
        [
            "issue",
            "list",
            "--state",
            "all",
            "--limit",
            "50",
            "--json",
            "number,title,state,body",
            "--search",
            f'in:title "{title}"',
        ]
    )
    issues = json.loads(raw or "[]")
    by_marker = sorted(
        (issue for issue in issues if marker in (issue.get("body") or "")),
        key=lambda issue: issue["number"],
    )
    if len(by_marker) > 1:
        numbers = ", ".join(f"#{issue['number']}" for issue in by_marker)
        sys.exit(
            f"hay {len(by_marker)} issues con el marcador del informe ({numbers}): "
            "deja uno solo antes de sincronizar"
        )
    if by_marker:
        return by_marker[0]
    by_title = sorted(
        (issue for issue in issues if issue.get("title") == title),
        key=lambda issue: issue["number"],
    )
    if len(by_title) > 1:
        numbers = ", ".join(f"#{issue['number']}" for issue in by_title)
        sys.exit(
            f"hay {len(by_title)} issues con el título exacto ({numbers}): "
            "deja uno solo antes de sincronizar"
        )
    return by_title[0] if by_title else None


def sync_report_issue(
    *,
    title: str,
    marker: str,
    body: str,
    has_debt: bool,
    closing_comment: str,
) -> None:
    """Deja el issue del informe en el estado que dice `has_debt`, y solo eso."""
    existing = find_report_issue(title, marker)
    if has_debt:
        if existing is None:
            url = run_gh(["issue", "create", "--title", title, "--body", body])
            print(f"issue creado: {url}", file=sys.stderr)
            return
        number = str(existing["number"])
        run_gh(["issue", "edit", number, "--body", body])
        if existing.get("state") == "CLOSED":
            run_gh(["issue", "reopen", number])
            print(f"issue #{number} reabierto y reescrito", file=sys.stderr)
        else:
            print(f"issue #{number} reescrito", file=sys.stderr)
        return

    if existing is None:
        print("sin deuda y sin issue previo: nada que sincronizar", file=sys.stderr)
        return
    number = str(existing["number"])
    if existing.get("state") == "OPEN":
        run_gh(["issue", "close", number, "--comment", closing_comment])
        print(f"issue #{number} cerrado: sin deuda", file=sys.stderr)
    else:
        print(f"issue #{number} ya cerrado y sin deuda", file=sys.stderr)
