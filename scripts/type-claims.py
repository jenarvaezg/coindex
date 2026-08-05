#!/usr/bin/env python3
"""Dice quién reclama ya un tipo de Numista en `data/` — cero red (#171).

Antes de versionar un catálogo o una agrupación hay que cruzar sus tipos contra
los que ya están versionados: dos ficheros que nombran el mismo tipo no dan una
tarjeta rara, sino un fichero que la app rechaza al arrancar, y el fichero viaja
al móvil del padre en la release. El validador de semillas
(`CatalogSeeds.validateCrossCatalogClaims`) lo caza cuando el fichero ya existe;
esto lo caza antes, cuando lo único que hay es una lista de ids.

    scripts/type-claims.py 235118 307024        # ¿está ocupado lo que voy a nombrar?
    scripts/type-claims.py 235118:582778,585569 # nombrándolo con desempate por emisión
    scripts/type-claims.py --file data/collection-catalogs/nuevo.json
    scripts/type-claims.py --all                # audita todo data/

Sale con 1 cuando algo obliga a parar. Los solapes legítimos —dos catálogos que
desempatan por `numista_issue_ids` con emisiones disjuntas, o un programa
conmemorativo, que es otra lectura de la misma moneda (ADR 0022)— salen con 0.
"""

from __future__ import annotations

import argparse
import json
import pathlib
import sys
from dataclasses import dataclass, field

ROOT = pathlib.Path(__file__).resolve().parent.parent
DATA = ROOT / "data"
CATALOGS = DATA / "collection-catalogs"
GROUPINGS = DATA / "groupings"
PROGRAMMES = DATA / "programmes"

#: Un catálogo de conjunto es `schema_version: 3` (ADR 0012).
SET_SCHEMA_VERSION = 3


@dataclass(frozen=True)
class Claim:
    """Un fichero de `data/` nombrando un tipo, y con qué desempate."""

    species: str
    file_id: str
    type_id: int
    issue_ids: frozenset[int] = frozenset()
    is_set: bool = False

    @property
    def qualified(self) -> bool:
        return bool(self.issue_ids)

    def describe(self) -> str:
        species = "conjunto" if self.is_set else self.species
        if self.qualified:
            emissions = ", ".join(str(issue) for issue in sorted(self.issue_ids))
            return f"{species} `{self.file_id}` (emisiones {emissions})"
        return f"{species} `{self.file_id}`"


@dataclass
class Verdict:
    """Qué dice el cruce de un tipo: `stop` es lo único que cambia el trabajo."""

    type_id: int
    claims: list[Claim]
    stop: bool
    reason: str
    notes: list[str] = field(default_factory=list)


def _read(path: pathlib.Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def load_claims(data: pathlib.Path = DATA) -> list[Claim]:
    """Toda reclamación versionada de un tipo, de las tres especies de fichero."""
    claims: list[Claim] = []
    for path in sorted((data / "collection-catalogs").glob("*.json")):
        payload = _read(path)
        is_set = payload.get("schema_version") == SET_SCHEMA_VERSION
        for member in payload.get("members", []):
            type_id = member.get("numista_type_id")
            # Un miembro announced o unlisted no tiene tipo, así que no reclama nada.
            if type_id is None:
                continue
            claims.append(
                Claim(
                    species="catálogo",
                    file_id=payload["id"],
                    type_id=type_id,
                    issue_ids=frozenset(member.get("numista_issue_ids") or ()),
                    is_set=is_set,
                )
            )
    for path in sorted((data / "groupings").glob("*.json")):
        payload = _read(path)
        for type_id in payload.get("type_ids", []):
            claims.append(
                Claim(species="agrupación", file_id=payload["id"], type_id=type_id)
            )
    for path in sorted((data / "programmes").glob("*.json")):
        payload = _read(path)
        for member in payload.get("members", []):
            claims.append(
                Claim(
                    species="programa",
                    file_id=payload["id"],
                    type_id=member["numista_type_id"],
                )
            )
    return claims


def claims_by_type(claims: list[Claim]) -> dict[int, list[Claim]]:
    grouped: dict[int, list[Claim]] = {}
    for claim in claims:
        grouped.setdefault(claim.type_id, []).append(claim)
    return grouped


def judge(type_id: int, claims: list[Claim]) -> Verdict:
    """La regla, en un sitio: qué solape es legítimo y qué solape obliga a parar.

    Vive aquí y en `CatalogSeeds.validateCrossCatalogClaims`, que es lo que la
    hace fatal. Esto no la sustituye: la aplica a ids que todavía no son fichero.
    """
    files = {(claim.species, claim.file_id, claim.is_set) for claim in claims}
    if len(files) < 2:
        return Verdict(type_id, claims, stop=False, reason="un solo fichero lo nombra")

    notes: list[str] = []
    programmes = [claim for claim in claims if claim.species == "programa"]
    programme_files = {claim.file_id for claim in programmes}
    if len(programme_files) > 1:
        return Verdict(
            type_id,
            claims,
            stop=True,
            reason="dos programas conmemorativos lo nombran, y una moneda se acuñó "
            "para una sola conmemoración",
        )
    if programmes:
        notes.append(
            "el programa conmemorativo es otra lectura de la misma moneda y no "
            "compite con la tarjeta de la denominación (ADR 0022)"
        )

    collections = [claim for claim in claims if claim.species != "programa"]
    collection_files = {(claim.species, claim.file_id) for claim in collections}
    if len(collection_files) < 2:
        return Verdict(
            type_id,
            claims,
            stop=False,
            reason="lo nombra una sola colección",
            notes=notes,
        )

    if any(claim.species == "agrupación" for claim in collections):
        return Verdict(
            type_id,
            claims,
            stop=True,
            reason="una agrupación no desempata por emisión y pierde ante cualquier "
            "otro fichero: el tipo nunca se archivaría bajo ella (ADR 0013)",
            notes=notes,
        )
    if any(claim.is_set for claim in collections):
        return Verdict(
            type_id,
            claims,
            stop=True,
            reason="el conjunto gana la familia y se lleva la moneda de la tarjeta "
            "de la denominación, que es lo contrario de sumar una segunda "
            "pertenencia (ADR 0012, ADR 0022)",
            notes=notes,
        )
    if any(not claim.qualified for claim in collections):
        bare = sorted(
            {claim.file_id for claim in collections if not claim.qualified}
        )
        return Verdict(
            type_id,
            claims,
            stop=True,
            reason="dos catálogos lo nombran y no todas las identidades desempatan "
            f"por `numista_issue_ids` ({', '.join(f'`{one}`' for one in bare)})",
            notes=notes,
        )

    owners: dict[int, str] = {}
    for claim in collections:
        for issue_id in claim.issue_ids:
            previous = owners.setdefault(issue_id, claim.file_id)
            if previous != claim.file_id:
                return Verdict(
                    type_id,
                    claims,
                    stop=True,
                    reason=f"la emisión {issue_id} la reclaman `{previous}` y "
                    f"`{claim.file_id}`: los conjuntos no son disjuntos",
                    notes=notes,
                )
    return Verdict(
        type_id,
        claims,
        stop=False,
        reason="dos catálogos desempatados por emisión, con emisiones disjuntas",
        notes=notes,
    )


def audit(claims: list[Claim]) -> list[Verdict]:
    """Cada tipo con más de un fichero detrás, en orden de id."""
    grouped = claims_by_type(claims)
    verdicts = []
    for type_id in sorted(grouped):
        shared = grouped[type_id]
        if len({(claim.species, claim.file_id) for claim in shared}) < 2:
            continue
        verdicts.append(judge(type_id, shared))
    return verdicts


def render(verdicts: list[Verdict], *, total_types: int, queried: bool) -> str:
    lines: list[str] = []
    if not verdicts:
        scope = "Ninguno de los tipos consultados" if queried else "Ningún tipo de `data/`"
        lines.append(f"{scope} está reclamado por dos ficheros.")
        lines.append("")
        lines.append(f"{total_types} tipos versionados.")
        return "\n".join(lines) + "\n"
    for verdict in verdicts:
        mark = "PARAR" if verdict.stop else "ok"
        lines.append(f"[{mark}] tipo {verdict.type_id}: {verdict.reason}")
        for claim in sorted(verdict.claims, key=lambda one: (one.species, one.file_id)):
            lines.append(f"    - {claim.describe()}")
        for note in verdict.notes:
            lines.append(f"    · {note}")
        lines.append("")
    if any(verdict.stop for verdict in verdicts):
        lines.append(
            "Un solape que obliga a parar no se versiona: o la forma del fichero "
            "está mal —un tipo repetido en dos casillas sólo es legal dentro de un "
            "date run— o es la primera colección solapada de verdad y exige antes "
            "el cambio de dominio de https://github.com/jenarvaezg/coindex/issues/149."
        )
        lines.append("")
    return "\n".join(lines)


def cross(versioned: list[Claim], candidate: list[Claim]) -> list[Verdict]:
    """Lo que se piensa nombrar, contra lo ya versionado.

    El candidato entra en el cruce como una reclamación más, así que lo juzga la
    misma regla: un id suelto sin `numista_issue_ids` no desempata y para; el
    mismo id con sus emisiones, si son disjuntas de las del otro catálogo, no.
    """
    grouped = claims_by_type(versioned)
    verdicts = []
    for claim in candidate:
        others = grouped.get(claim.type_id, [])
        if not others:
            continue
        verdicts.append(judge(claim.type_id, [*others, claim]))
    return verdicts


def parse_candidate_ids(raw: list[str]) -> list[Claim]:
    """`235118` o `235118:582778,585569` — el tipo y, si lo hay, su desempate."""
    claims = []
    for token in raw:
        type_part, _, issue_part = token.partition(":")
        try:
            type_id = int(type_part)
            issue_ids = frozenset(
                int(one) for one in issue_part.split(",") if one.strip()
            )
        except ValueError:
            sys.exit(f"no entiendo `{token}`: se espera ID o ID:emisión,emisión")
        claims.append(
            Claim(
                species="catálogo",
                file_id="<candidato>",
                type_id=type_id,
                issue_ids=issue_ids,
            )
        )
    return claims


def candidate_claims_of_file(path: pathlib.Path) -> list[Claim]:
    """Un fichero aún sin versionar (o ya versionado) leído como candidato."""
    payload = _read(path)
    file_id = payload.get("id", path.stem)
    if "type_ids" in payload:
        return [
            Claim(species="agrupación", file_id=file_id, type_id=type_id)
            for type_id in payload["type_ids"]
        ]
    species = "programa" if path.parent.name == "programmes" else "catálogo"
    is_set = payload.get("schema_version") == SET_SCHEMA_VERSION
    return [
        Claim(
            species=species,
            file_id=file_id,
            type_id=member["numista_type_id"],
            issue_ids=frozenset(member.get("numista_issue_ids") or ()),
            is_set=is_set,
        )
        for member in payload.get("members", [])
        if member.get("numista_type_id") is not None
    ]


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "type_ids",
        nargs="*",
        metavar="ID[:EMISIÓN,…]",
        help="tipos de Numista que se piensa nombrar, con su desempate si lo hay",
    )
    parser.add_argument(
        "--file",
        metavar="RUTA",
        help="cruza los tipos de un fichero candidato (aún sin versionar)",
    )
    parser.add_argument(
        "--all",
        action="store_true",
        help="audita todos los solapes de `data/`",
    )
    arguments = parser.parse_args()

    claims = load_claims()
    total_types = len(claims_by_type(claims))

    candidate = parse_candidate_ids(arguments.type_ids)
    if arguments.file:
        path = pathlib.Path(arguments.file)
        if not path.is_file():
            sys.exit(f"no existe el fichero {path}")
        candidate.extend(candidate_claims_of_file(path))

    if not candidate and not arguments.all:
        parser.error("da al menos un ID, o --file, o --all")

    if arguments.all:
        verdicts = audit(claims)
    else:
        # Un fichero ya versionado no se cruza contra sí mismo: lo que se juzga es
        # lo que dice ahora, no la copia que ya está en `data/`.
        own = {claim.file_id for claim in candidate}
        verdicts = cross([one for one in claims if one.file_id not in own], candidate)

    sys.stdout.write(
        render(verdicts, total_types=total_types, queried=not arguments.all)
    )
    if any(verdict.stop for verdict in verdicts):
        raise SystemExit(1)


if __name__ == "__main__":
    main()
