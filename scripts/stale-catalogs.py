#!/usr/bin/env python3
"""Informa qué catálogos abiertos se han quedado atrás — cero red, nunca rojo.

Lee `data/collection-catalogs/*.json`, se queda con `series_status: "open"` y saca
dos bloques (#94, #132):

- **La cola**: no hay ninguna casilla del año en curso, sin mirar el `status` del
  miembro. Leer «issued» delataría el trabajo deliberado de #72 (announced /
  unlisted con casilla del año).
- **Los huecos**: un año sin ninguna casilla entre el primero del catálogo y el
  año *anterior* al en curso. El año en curso ausente es cola, no hueco: los dos
  conjuntos son disjuntos. Los años declarados en `no_issue_years` (con su
  `no_issue_note`) no cuentan: la ceca no emitió, y eso ya está versionado en el
  fichero (#130, #131).

Cero llamadas a Numista: aritmética sobre los años de los miembros y la fecha de
hoy. Ejecutable a mano al sentarse a curar; en CI, `--sync` mantiene un issue
único del repo (abre/reescribe con deuda, cierra con comentario al quedar limpio).

    scripts/stale-catalogs.py
    scripts/stale-catalogs.py --sync
"""

from __future__ import annotations

import argparse
import json
import pathlib
import sys
from dataclasses import dataclass
from datetime import date

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))

from repo_issue import sync_report_issue  # noqa: E402

ROOT = pathlib.Path(__file__).resolve().parent.parent
CATALOGS = ROOT / "data" / "collection-catalogs"
ISSUE_TITLE = "Catálogos abiertos por detrás"
ISSUE_MARKER = "<!-- stale-catalogs-report -->"


@dataclass(frozen=True)
class CatalogYears:
    path: pathlib.Path
    catalog_id: str
    name: str
    years: frozenset[int]
    no_issue_years: frozenset[int]

    @property
    def first_year(self) -> int | None:
        return min(self.years) if self.years else None

    @property
    def last_year(self) -> int | None:
        return max(self.years) if self.years else None


@dataclass(frozen=True)
class Report:
    as_of: date
    open_count: int
    lagging: tuple[CatalogYears, ...]
    gapped: tuple[tuple[CatalogYears, tuple[int, ...]], ...]

    @property
    def has_debt(self) -> bool:
        return bool(self.lagging or self.gapped)


def load_open_catalogs(directory: pathlib.Path = CATALOGS) -> list[CatalogYears]:
    catalogs: list[CatalogYears] = []
    for path in sorted(directory.glob("*.json")):
        payload = json.loads(path.read_text(encoding="utf-8"))
        if payload.get("series_status") != "open":
            continue
        years = frozenset(
            member["year"]
            for member in payload.get("members", [])
            if member.get("year") is not None
        )
        no_issue_years = tuple(payload.get("no_issue_years") or ())
        no_issue_note = payload.get("no_issue_note")
        # Misma simetría que el validador Kotlin: años sin nota no se silencian en silencio.
        if no_issue_years and (
            no_issue_note is None or not str(no_issue_note).strip()
        ):
            sys.exit(
                f"{path.name}: declara `no_issue_years` sin `no_issue_note` no vacía"
            )
        if no_issue_note is not None and not no_issue_years:
            sys.exit(
                f"{path.name}: declara `no_issue_note` sin `no_issue_years`"
            )
        catalogs.append(
            CatalogYears(
                path=path,
                catalog_id=payload["id"],
                name=payload.get("name", payload["id"]),
                years=years,
                no_issue_years=frozenset(no_issue_years),
            )
        )
    return catalogs


def build_report(
    catalogs: list[CatalogYears],
    *,
    as_of: date | None = None,
) -> Report:
    today = as_of or date.today()
    current_year = today.year
    lagging: list[CatalogYears] = []
    gapped: list[tuple[CatalogYears, tuple[int, ...]]] = []
    for catalog in catalogs:
        if not catalog.years:
            continue
        if current_year not in catalog.years:
            lagging.append(catalog)
        # Huecos interiores: el año en curso ausente es cola, no hueco (#94).
        # Los `no_issue_years` versionados en el fichero tampoco son deuda.
        missing = tuple(
            year
            for year in range(catalog.first_year, current_year)
            if year not in catalog.years and year not in catalog.no_issue_years
        )
        if missing:
            gapped.append((catalog, missing))
    return Report(
        as_of=today,
        open_count=len(catalogs),
        lagging=tuple(lagging),
        gapped=tuple(gapped),
    )


def format_years(years: tuple[int, ...]) -> str:
    return ", ".join(str(year) for year in years)


def render_markdown(report: Report) -> str:
    current_year = report.as_of.year
    lines = [
        ISSUE_MARKER,
        f"# {ISSUE_TITLE}",
        "",
        f"Informe del **{report.as_of.isoformat()}** sobre {report.open_count} "
        f"catálogos abiertos. Cero red: aritmética sobre los años de los miembros.",
        "",
        "Generado por `scripts/stale-catalogs.py`. Un issue abierto significa deuda; "
        "se cierra solo cuando cola y huecos quedan vacíos. Los años en "
        "`no_issue_years` no son deuda. El histórico de cada pasada —y las notas "
        "humanas del tipo «Pressburg no ha emitido 2026, comprobado el …»— vive "
        "en los comentarios.",
        "",
        "## Cola",
        "",
        f"Sin ninguna casilla de **{current_year}**, sin mirar el `status` del miembro.",
        "",
    ]
    if report.lagging:
        lines.extend(
            [
                "| Catálogo | Último año |",
                "|---|---|",
            ]
        )
        for catalog in report.lagging:
            lines.append(
                f"| `{catalog.catalog_id}` · {catalog.name} | {catalog.last_year} |"
            )
    else:
        lines.append("_Ninguno._")
    lines.extend(
        [
            "",
            "## Huecos",
            "",
            f"Años sin casilla entre el primero del catálogo y {current_year - 1} "
            f"(el {current_year} ausente es cola, no hueco).",
            "",
        ]
    )
    if report.gapped:
        lines.extend(
            [
                "| Catálogo | Huecos |",
                "|---|---|",
            ]
        )
        for catalog, missing in report.gapped:
            lines.append(
                f"| `{catalog.catalog_id}` · {catalog.name} | {format_years(missing)} |"
            )
    else:
        lines.append("_Ninguno._")
    lines.append("")
    return "\n".join(lines)


def render_plain(report: Report) -> str:
    current_year = report.as_of.year
    lines = [
        f"Catálogos abiertos por detrás · {report.as_of.isoformat()} · "
        f"{report.open_count} abiertos",
        "",
        f"Cola (sin casilla de {current_year}):",
    ]
    if report.lagging:
        for catalog in report.lagging:
            lines.append(
                f"  - {catalog.catalog_id} · último {catalog.last_year}"
            )
    else:
        lines.append("  (ninguno)")
    lines.extend(["", f"Huecos (hasta {current_year - 1}):"])
    if report.gapped:
        for catalog, missing in report.gapped:
            lines.append(
                f"  - {catalog.catalog_id}: {format_years(missing)}"
            )
    else:
        lines.append("  (ninguno)")
    lines.append("")
    return "\n".join(lines)


def sync_issue(report: Report) -> None:
    sync_report_issue(
        title=ISSUE_TITLE,
        marker=ISSUE_MARKER,
        body=render_markdown(report),
        has_debt=report.has_debt,
        closing_comment=(
            f"Cola y huecos vacíos el {report.as_of.isoformat()}. "
            "Los catálogos abiertos están al día según la aritmética del informe."
        ),
    )


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--markdown",
        action="store_true",
        help="imprime el informe en markdown (cuerpo del issue)",
    )
    parser.add_argument(
        "--sync",
        action="store_true",
        help="sincroniza el issue único del repo (requiere gh autenticado)",
    )
    parser.add_argument(
        "--as-of",
        metavar="YYYY-MM-DD",
        help="fecha de referencia (por defecto: hoy); útil para reproducir un informe",
    )
    arguments = parser.parse_args()

    as_of = date.fromisoformat(arguments.as_of) if arguments.as_of else date.today()
    report = build_report(load_open_catalogs(), as_of=as_of)

    if arguments.sync:
        sync_issue(report)
    if arguments.markdown or arguments.sync:
        sys.stdout.write(render_markdown(report))
    else:
        sys.stdout.write(render_plain(report))


if __name__ == "__main__":
    main()
