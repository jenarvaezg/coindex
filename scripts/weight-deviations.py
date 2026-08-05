#!/usr/bin/env python3
"""Informa qué pesos no cuadran — cero red, nunca rojo.

La auditoría del emparejamiento es del curador y vive **fuera** de la app (#20, ADR 0021
§12). Este es el informe, con la forma de `stale-catalogs.py`: se lee `data/` y nada más,
se ejecuta a mano al sentarse a curar y `--sync` mantiene un issue único del repo.

Tres bloques:

- **Lo que el catálogo corrige**: miembros cuyo peso normalizado desde los gramos de
  Numista no es el `weight_millioz` que declara su catálogo. Ordenados por distancia
  relativa al declarado, porque es lo que separa la variación de gramos del intruso.
- **Lo que el imán mueve**: tipos que ningún catálogo reclama y a los que
  `normalizeWeightMillioz` mueve el peso —a un peso común de bullion o al declarado por
  un catálogo ajeno—. Son los únicos casos donde la variante se decide sin que nadie la
  haya verificado a mano.
- **Las tarjetas que la autoridad del catálogo evita**: catálogos cuyos miembros tomarían
  más de una clave de peso si el fichero no mandara. Es el número que justifica el
  ADR 0016, y hacerlo visible es todo lo que el informe puede hacer con él.

**No es un test, y no por comodidad.** Se pondría rojo más de cien veces el primer día y
casi todas esas notas dirían «Numista varía los gramos». El cruce de metal
(`metalDeviations`) sí es un test porque su hallazgo es raro: rojo cuando el hallazgo es
raro, informe cuando es rutina. Una línea de aquí no dice «arregla el catálogo», dice
«míralo»: casi siempre es la ficha la que varía, a veces es la ley (los 3 rublos de plata
900 y de plata 925 son la misma onza fina), y de vez en cuando es una moneda que no va en
esa lámina.

Sin inventario a propósito: `data/` no sabe qué tiene nadie, y el informe tiene que correr
sin el móvil de nadie. Las filas del inventario que no casan con ninguna casilla son del
informe de campo (#168), no de aquí.

    scripts/weight-deviations.py
    scripts/weight-deviations.py --markdown
    scripts/weight-deviations.py --sync
"""

from __future__ import annotations

import argparse
import html
import json
import math
import pathlib
import sys
from dataclasses import dataclass
from datetime import date

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))

from repo_issue import sync_report_issue  # noqa: E402

ROOT = pathlib.Path(__file__).resolve().parent.parent
CATALOGS = ROOT / "data" / "collection-catalogs"
GROUPINGS = ROOT / "data" / "groupings"
TYPE_CACHE = ROOT / "data" / "numista-type-cache.json"

ISSUE_TITLE = "Desviaciones de peso entre la ficha y el catálogo"
ISSUE_MARKER = "<!-- weight-deviations-report -->"
NUMISTA_TYPE_URL = "https://en.numista.com/catalogue/pieces{type_id}.html"

# Espejo de domain/…/Weight.kt. Si allí cambian la tolerancia o los pesos comunes, el
# informe deja de medir lo que la app hace: es la única duplicación que este script tiene
# y la única que hay que mirar cuando el imán cambie.
GRAMS_PER_TROY_OUNCE = 31.1034768
COMMON_WEIGHTS_MILLIOZ = (250, 500, 1_000, 2_000, 5_000, 10_000)
SNAP_TOLERANCE_MILLIOZ = 10

# Los tres tramos con los que el curador decide si abrir Numista. El corte de arriba no es
# físico: dos por ciento es más de lo que cualquier ceca se desvía y menos que cualquier ley.
NEAR_PERCENT = 2.0
FAR_PERCENT = 5.0


def normalize_weight_millioz(
    weight_oz: float,
    curated_targets: tuple[int, ...] = (),
) -> int | None:
    """Espejo de `normalizeWeightMillioz`: imán a los pesos comunes y a los curados."""
    if not math.isfinite(weight_oz) or weight_oz <= 0.0:
        return None
    # `Math.round` de Kotlin es floor(x + 0.5), no el redondeo bancario de `round`.
    measured = math.floor(weight_oz * 1_000.0 + 0.5)
    if measured <= 0:
        return None
    targets = list(COMMON_WEIGHTS_MILLIOZ) + sorted(curated_targets)
    near = [
        target
        for target in targets
        if abs(measured - target) <= SNAP_TOLERANCE_MILLIOZ
    ]
    if not near:
        return measured
    # Gana el objetivo más cercano, y el más pequeño desempata.
    return min(near, key=lambda target: (abs(measured - target), target))


def normalize_family(family: str | None) -> str | None:
    """Espejo de `normalizeFamily`: colapsa espacios y trata el vacío como ausencia."""
    if family is None:
        return None
    collapsed = " ".join(family.split())
    return collapsed or None


def is_technical_family(family: str) -> bool:
    """Espejo de `isTechnicalFamily`: `System YYYY[-YYYY]` es sistema monetario, no serie."""
    period = family.removeprefix("System ")
    if period == family or not period:
        return False
    return all(
        len(year) == 4 and year.isdigit() and year.isascii()
        for year in period.split("-")
    )


@dataclass(frozen=True)
class Member:
    member_id: str
    label: str | None
    year: int | None
    numista_type_id: int | None
    variant_note: str | None


@dataclass(frozen=True)
class Catalog:
    catalog_id: str
    name: str
    weight_millioz: int | None
    members: tuple[Member, ...]
    is_set: bool


@dataclass(frozen=True)
class Ficha:
    numista_type_id: int
    title: str | None
    series: str | None
    grams: float | None
    composition: str | None

    @property
    def weight_oz(self) -> float | None:
        if self.grams is None or self.grams <= 0:
            return None
        return self.grams / GRAMS_PER_TROY_OUNCE


@dataclass(frozen=True)
class Deviation:
    """Un miembro cuya ficha pesa otra cosa que lo que declara su catálogo."""

    catalog_id: str
    catalog_name: str
    member_id: str
    member_label: str | None
    numista_type_id: int
    declared_millioz: int
    measured_millioz: int
    observed_millioz: int
    grams: float
    composition: str | None
    variant_note: str | None
    key_declared_by: tuple[str, ...]

    @property
    def relative_percent(self) -> float:
        """Distancia de la clave al declarado, que es la que decide si hay dos tarjetas."""
        return abs(self.observed_millioz - self.declared_millioz) * 100.0 / self.declared_millioz

    @property
    def magnet_moved(self) -> bool:
        """Si la clave no son los gramos de la ficha, es que la movió un objetivo ajeno."""
        return self.observed_millioz != self.measured_millioz


@dataclass(frozen=True)
class MagnetPull:
    """Un tipo sin catálogo al que el imán le cambia el peso."""

    numista_type_id: int
    title: str | None
    measured_millioz: int
    snapped_millioz: int
    grams: float
    composition: str | None
    declaring_catalogs: tuple[str, ...]
    family: str | None

    @property
    def snapped_to_common(self) -> bool:
        return self.snapped_millioz in COMMON_WEIGHTS_MILLIOZ

    @property
    def pull_millioz(self) -> int:
        return abs(self.snapped_millioz - self.measured_millioz)


@dataclass(frozen=True)
class SplitCatalog:
    """Un catálogo cuyos miembros tomarían más de una clave de peso sin su autoridad."""

    catalog_id: str
    name: str
    declared_millioz: int
    keys: tuple[int, ...]

    @property
    def extra_cards(self) -> int:
        return len(self.keys) - 1


@dataclass(frozen=True)
class Report:
    as_of: date
    catalog_count: int
    ficha_count: int
    deviations: tuple[Deviation, ...]
    pulls: tuple[MagnetPull, ...]
    splits: tuple[SplitCatalog, ...]
    unclaimed_count: int
    unclaimed_without_weight: int

    @property
    def has_findings(self) -> bool:
        return bool(self.deviations or self.pulls or self.splits)

    @property
    def buckets(self) -> tuple[int, int, int]:
        near = far = beyond = 0
        for deviation in self.deviations:
            percent = deviation.relative_percent
            if percent <= NEAR_PERCENT:
                near += 1
            elif percent <= FAR_PERCENT:
                far += 1
            else:
                beyond += 1
        return near, far, beyond

    @property
    def extra_cards(self) -> int:
        return sum(split.extra_cards for split in self.splits)


def load_catalogs(directory: pathlib.Path = CATALOGS) -> list[Catalog]:
    catalogs: list[Catalog] = []
    for path in sorted(directory.glob("*.json")):
        payload = json.loads(path.read_text(encoding="utf-8"))
        members = tuple(
            Member(
                member_id=member["id"],
                label=member.get("label"),
                year=member.get("year"),
                numista_type_id=member.get("numista_type_id"),
                variant_note=member.get("variant_note"),
            )
            for member in payload.get("members", [])
        )
        catalogs.append(
            Catalog(
                catalog_id=payload["id"],
                name=payload.get("short_name") or payload.get("name", payload["id"]),
                weight_millioz=payload.get("weight_millioz"),
                members=members,
                # Espejo de `CollectionCatalog.isSet`: el conjunto es la unidad y no
                # declara variante física de ninguna clase (ADR 0012).
                is_set=payload.get("schema_version") == 3,
            )
        )
    return catalogs


def load_grouping_families(directory: pathlib.Path = GROUPINGS) -> dict[int, str]:
    families: dict[int, str] = {}
    for path in sorted(directory.glob("*.json")):
        payload = json.loads(path.read_text(encoding="utf-8"))
        for type_id in payload.get("type_ids", []):
            families.setdefault(int(type_id), payload["family"])
    return families


def load_fichas(path: pathlib.Path = TYPE_CACHE) -> dict[int, Ficha]:
    payload = json.loads(path.read_text(encoding="utf-8"))
    fichas: dict[int, Ficha] = {}
    for raw_id, ficha in payload.items():
        grams = ficha.get("weight")
        composition = (ficha.get("composition") or {}).get("text")
        fichas[int(raw_id)] = Ficha(
            numista_type_id=int(raw_id),
            title=ficha.get("title"),
            series=ficha.get("series"),
            grams=grams if isinstance(grams, (int, float)) else None,
            composition=html.unescape(composition) if composition else None,
        )
    return fichas


def derived_family(ficha: Ficha, grouping_families: dict[int, str]) -> str | None:
    """A qué familia iría el tipo sin catálogo, con el orden de `deriveCollection`."""
    numista = normalize_family(ficha.series)
    curated = grouping_families.get(ficha.numista_type_id)
    if numista is None:
        return curated
    if is_technical_family(numista):
        return curated or numista
    return numista


def curated_targets(catalogs: list[Catalog]) -> tuple[int, ...]:
    return tuple(
        sorted(
            {
                catalog.weight_millioz
                for catalog in catalogs
                if catalog.weight_millioz is not None
            }
        )
    )


def catalogs_by_weight(catalogs: list[Catalog]) -> dict[int, tuple[str, ...]]:
    """Quién declara cada objetivo curado, que es de dónde sale el imán que cruza catálogos."""
    by_weight: dict[int, list[str]] = {}
    for catalog in catalogs:
        if catalog.weight_millioz is not None:
            by_weight.setdefault(catalog.weight_millioz, []).append(catalog.catalog_id)
    return {weight: tuple(sorted(ids)) for weight, ids in by_weight.items()}


def find_deviations(
    catalogs: list[Catalog],
    fichas: dict[int, Ficha],
    targets: tuple[int, ...],
) -> list[Deviation]:
    declared_by = catalogs_by_weight(catalogs)
    deviations: list[Deviation] = []
    for catalog in catalogs:
        declared = catalog.weight_millioz
        if declared is None:
            continue
        for member in catalog.members:
            if member.numista_type_id is None:
                continue
            ficha = fichas.get(member.numista_type_id)
            # Un tipo que nadie ha sembrado no dice nada; el test de la semilla es lo que
            # convierte eso en un fallo.
            if ficha is None or ficha.weight_oz is None or ficha.grams is None:
                continue
            observed = normalize_weight_millioz(ficha.weight_oz, targets)
            if observed is None or observed == declared:
                continue
            deviations.append(
                Deviation(
                    catalog_id=catalog.catalog_id,
                    catalog_name=catalog.name,
                    member_id=member.member_id,
                    member_label=member.label,
                    numista_type_id=member.numista_type_id,
                    declared_millioz=declared,
                    measured_millioz=math.floor(ficha.weight_oz * 1_000.0 + 0.5),
                    observed_millioz=observed,
                    grams=ficha.grams,
                    composition=ficha.composition,
                    variant_note=member.variant_note,
                    key_declared_by=declared_by.get(observed, ()),
                )
            )
    deviations.sort(
        key=lambda deviation: (
            -deviation.relative_percent,
            deviation.catalog_id,
            deviation.member_id,
        )
    )
    return deviations


def find_magnet_pulls(
    catalogs: list[Catalog],
    grouping_families: dict[int, str],
    fichas: dict[int, Ficha],
    targets: tuple[int, ...],
) -> tuple[list[MagnetPull], int, int]:
    """Los tipos sin catálogo a los que el imán mueve el peso, y cuántos quedan fuera."""
    claimed = {
        member.numista_type_id
        for catalog in catalogs
        for member in catalog.members
        if member.numista_type_id is not None
    }
    declared_by = catalogs_by_weight(catalogs)
    pulls: list[MagnetPull] = []
    unclaimed = 0
    without_weight = 0
    for type_id, ficha in fichas.items():
        if type_id in claimed:
            continue
        unclaimed += 1
        weight_oz = ficha.weight_oz
        if weight_oz is None:
            # Sin gramos no hay imán que mover: la pieza acaba en el residuo por peso
            # desconocido, y eso ya lo dice la app.
            without_weight += 1
            continue
        measured = math.floor(weight_oz * 1_000.0 + 0.5)
        snapped = normalize_weight_millioz(weight_oz, targets)
        if snapped is None or snapped == measured:
            continue
        pulls.append(
            MagnetPull(
                numista_type_id=type_id,
                title=ficha.title,
                measured_millioz=measured,
                snapped_millioz=snapped,
                grams=ficha.grams or 0.0,
                composition=ficha.composition,
                declaring_catalogs=declared_by.get(snapped, ()),
                family=derived_family(ficha, grouping_families),
            )
        )
    pulls.sort(
        key=lambda pull: (-pull.pull_millioz, pull.numista_type_id),
    )
    return pulls, unclaimed, without_weight


def find_split_catalogs(
    catalogs: list[Catalog],
    fichas: dict[int, Ficha],
    targets: tuple[int, ...],
) -> list[SplitCatalog]:
    """Catálogos cuyos miembros tomarían más de una clave de peso sin la autoridad del fichero."""
    splits: list[SplitCatalog] = []
    for catalog in catalogs:
        declared = catalog.weight_millioz
        if declared is None:
            continue
        keys: set[int] = set()
        for member in catalog.members:
            if member.numista_type_id is None:
                continue
            ficha = fichas.get(member.numista_type_id)
            if ficha is None or ficha.weight_oz is None:
                continue
            observed = normalize_weight_millioz(ficha.weight_oz, targets)
            if observed is not None:
                keys.add(observed)
        if len(keys) > 1:
            splits.append(
                SplitCatalog(
                    catalog_id=catalog.catalog_id,
                    name=catalog.name,
                    declared_millioz=declared,
                    keys=tuple(sorted(keys)),
                )
            )
    splits.sort(key=lambda split: (-split.extra_cards, split.catalog_id))
    return splits


def build_report(
    catalogs: list[Catalog],
    grouping_families: dict[int, str],
    fichas: dict[int, Ficha],
    *,
    as_of: date | None = None,
) -> Report:
    targets = curated_targets(catalogs)
    pulls, unclaimed, without_weight = find_magnet_pulls(
        catalogs, grouping_families, fichas, targets
    )
    return Report(
        as_of=as_of or date.today(),
        catalog_count=len(catalogs),
        ficha_count=len(fichas),
        deviations=tuple(find_deviations(catalogs, fichas, targets)),
        pulls=tuple(pulls),
        splits=tuple(find_split_catalogs(catalogs, fichas, targets)),
        unclaimed_count=unclaimed,
        unclaimed_without_weight=without_weight,
    )


def format_decimal(value: float, decimals: int = 2) -> str:
    rendered = f"{value:.{decimals}f}"
    if "." in rendered:
        rendered = rendered.rstrip("0").rstrip(".")
    return (rendered or "0").replace(".", ",")


def format_grams(grams: float) -> str:
    return f"{format_decimal(grams, 4)} g"


def format_percent(percent: float) -> str:
    """Dos decimales fijos: la columna de distancias se lee comparando, no una a una."""
    return f"{percent:.2f}".replace(".", ",")


def cell(text: str | None, *, limit: int = 56) -> str:
    """Una celda de tabla que no rompe la tabla ni la desborda."""
    if not text:
        return "—"
    flat = " ".join(text.split()).replace("|", "\\|")
    if len(flat) > limit:
        flat = flat[: limit - 1].rstrip() + "…"
    return flat


def numista_link(type_id: int) -> str:
    return f"[{type_id}]({NUMISTA_TYPE_URL.format(type_id=type_id)})"


def magnet_origin(millioz: int, declared_by: tuple[str, ...]) -> str:
    """De qué objetivo tira el imán: un peso común de bullion o el de un catálogo ajeno."""
    if millioz in COMMON_WEIGHTS_MILLIOZ:
        return "peso común"
    if declared_by:
        return "declarado por " + ", ".join(
            f"`{catalog_id}`" for catalog_id in declared_by
        )
    return "objetivo curado"


def magnet_target(pull: MagnetPull) -> str:
    return magnet_origin(pull.snapped_millioz, pull.declaring_catalogs)


def render_markdown(report: Report) -> str:
    near, far, beyond = report.buckets
    lines = [
        ISSUE_MARKER,
        f"# {ISSUE_TITLE}",
        "",
        f"Informe del **{report.as_of.isoformat()}** sobre {report.catalog_count} "
        f"catálogos y {report.ficha_count} fichas de `data/`. Cero red: aritmética sobre "
        "los gramos que Numista tiene guardados y los pesos que declaran los catálogos.",
        "",
        "Generado por `scripts/weight-deviations.py`, que decidió "
        "[#20](https://github.com/jenarvaezg/coindex/issues/20): el emparejamiento se "
        "audita fuera de la app y lo audita el curador. **Una línea no dice «arregla el "
        "catálogo», dice «míralo»**: casi siempre es Numista variando los gramos, a veces "
        "es la ley —los 3 rublos de plata 900 y de plata 925 son la misma onza fina— y de "
        "vez en cuando es una moneda que no va en esa lámina. Lo que se decide se escribe "
        "en el fichero, y el histórico de cada pasada vive en los comentarios.",
        "",
        "## Lo que el catálogo corrige",
        "",
        f"{len(report.deviations)} miembros cuyo peso normalizado desde los gramos de la "
        "ficha no es el `weight_millioz` que declara su catálogo. Por el ADR 0016 manda el "
        "catálogo, así que estas casillas se emparejan hoy contra lo que dice su ficha. "
        "Ordenados por distancia relativa al declarado, que es lo que separa la variación "
        "del intruso.",
        "",
    ]
    if report.deviations:
        lines.extend(
            [
                "| distancia | miembros |",
                "|---|---|",
                f"| ≤ {format_decimal(NEAR_PERCENT)} % | {near} |",
                f"| {format_decimal(NEAR_PERCENT)}-{format_decimal(FAR_PERCENT)} % | {far} |",
                f"| > {format_decimal(FAR_PERCENT)} % | {beyond} |",
                "",
                "La **composición** va en cada línea porque sin la ley el informe obliga a "
                "abrir Numista para entender la desviación. `ficha` son los gramos de "
                "Numista tal cual; `clave` es dónde los deja el imán, y cuando no son el "
                "mismo número es que tiró de ellos un objetivo ajeno —el que se nombra al "
                "lado—. La distancia se mide sobre la clave, que es la que decidiría la "
                "tarjeta. La columna `nota` marca los miembros cuyo `variant_note` ya "
                "explica la desviación: el curador la escribió a mano y no hay nada que mirar.",
                "",
                "| distancia | catálogo | casilla | Numista | declara | ficha | clave | composición | nota |",
                "|---|---|---|---|---|---|---|---|---|",
            ]
        )
        for deviation in report.deviations:
            key = str(deviation.observed_millioz)
            if deviation.magnet_moved:
                key += " · " + magnet_origin(
                    deviation.observed_millioz, deviation.key_declared_by
                )
            lines.append(
                f"| {format_percent(deviation.relative_percent)} % "
                f"| `{deviation.catalog_id}` "
                f"| {cell(deviation.member_label or deviation.member_id, limit=40)} "
                f"| {numista_link(deviation.numista_type_id)} "
                f"| {deviation.declared_millioz} "
                f"| {deviation.measured_millioz} ({format_grams(deviation.grams)}) "
                f"| {key} "
                f"| {cell(deviation.composition, limit=32)} "
                f"| {'sí' if deviation.variant_note else ''} |"
            )
    else:
        lines.append("_Ninguno._")
    lines.extend(
        [
            "",
            "## Lo que el imán mueve",
            "",
            f"{len(report.pulls)} de los {report.unclaimed_count} tipos que ningún "
            "catálogo reclama tienen el peso movido por `normalizeWeightMillioz`: a un "
            "peso común de bullion o al que declara un catálogo ajeno. **Son los únicos "
            "casos donde la variante se decide sin que nadie la haya verificado a mano.** "
            "La columna `familia` dice de dónde saldría la tarjeta —una agrupación curada, "
            "la serie de Numista— y `—` significa que el tipo no llega a tarjeta ninguna, "
            "así que el imán no decide nada por ahora.",
            "",
        ]
    )
    if report.pulls:
        lines.extend(
            [
                "| Numista | título | ficha | imán | familia | composición |",
                "|---|---|---|---|---|---|",
            ]
        )
        for pull in report.pulls:
            lines.append(
                f"| {numista_link(pull.numista_type_id)} "
                f"| {cell(pull.title, limit=44)} "
                f"| {pull.measured_millioz} ({format_grams(pull.grams)}) "
                f"| {pull.snapped_millioz} · {magnet_target(pull)} "
                f"| {cell(pull.family, limit=36)} "
                f"| {cell(pull.composition, limit=32)} |"
            )
    else:
        lines.append("_Ninguno._")
    lines.extend(
        [
            "",
            "## Las tarjetas que la autoridad del catálogo evita",
            "",
            f"{len(report.splits)} catálogos tienen miembros que tomarían más de una clave "
            f"de peso, o sea **{report.extra_cards} tarjetas duplicadas** que el fichero "
            "evita hoy en silencio (ADR 0016). El informe es lo que hace visible ese "
            "silencio; no hay nada que arreglar mientras las claves sean la misma moneda "
            "con los gramos mal contados.",
            "",
        ]
    )
    if report.splits:
        lines.extend(
            [
                "| catálogo | declara | claves que tomarían sus miembros | tarjetas de más |",
                "|---|---|---|---|",
            ]
        )
        for split in report.splits:
            keys = ", ".join(str(key) for key in split.keys)
            lines.append(
                f"| `{split.catalog_id}` · {cell(split.name, limit=40)} "
                f"| {split.declared_millioz} | {keys} | {split.extra_cards} |"
            )
    else:
        lines.append("_Ninguno._")
    lines.append("")
    return "\n".join(lines)


def render_plain(report: Report) -> str:
    near, far, beyond = report.buckets
    lines = [
        f"Desviaciones de peso · {report.as_of.isoformat()} · "
        f"{report.catalog_count} catálogos · {report.ficha_count} fichas",
        "",
        f"Lo que el catálogo corrige ({len(report.deviations)} miembros): "
        f"≤{format_decimal(NEAR_PERCENT)}% {near} · "
        f"{format_decimal(NEAR_PERCENT)}-{format_decimal(FAR_PERCENT)}% {far} · "
        f">{format_decimal(FAR_PERCENT)}% {beyond}",
    ]
    if report.deviations:
        for deviation in report.deviations:
            note = " · nota" if deviation.variant_note else ""
            magnet = ""
            if deviation.magnet_moved:
                origin = magnet_origin(
                    deviation.observed_millioz, deviation.key_declared_by
                ).replace("`", "")
                magnet = f", clave {deviation.observed_millioz} ({origin})"
            lines.append(
                f"  - {format_percent(deviation.relative_percent)}% "
                f"{deviation.catalog_id}/{deviation.member_id} "
                f"(Numista {deviation.numista_type_id}): declara "
                f"{deviation.declared_millioz}, la ficha dice "
                f"{deviation.measured_millioz} ({format_grams(deviation.grams)})"
                f"{magnet}"
                f" · {deviation.composition or 'sin composición'}{note}"
            )
    else:
        lines.append("  (ninguno)")
    lines.extend(
        [
            "",
            f"Lo que el imán mueve ({len(report.pulls)} de {report.unclaimed_count} "
            "tipos sin catálogo):",
        ]
    )
    if report.pulls:
        for pull in report.pulls:
            lines.append(
                f"  - Numista {pull.numista_type_id}: {pull.measured_millioz} → "
                f"{pull.snapped_millioz} ({format_grams(pull.grams)}), "
                f"{magnet_target(pull).replace('`', '')}"
                f" · familia {pull.family or '(ninguna)'}"
                f" · {pull.composition or 'sin composición'}"
            )
    else:
        lines.append("  (ninguno)")
    lines.extend(
        [
            "",
            f"Tarjetas que la autoridad del catálogo evita ({len(report.splits)} "
            f"catálogos, {report.extra_cards} tarjetas):",
        ]
    )
    if report.splits:
        for split in report.splits:
            keys = ", ".join(str(key) for key in split.keys)
            lines.append(
                f"  - {split.catalog_id}: declara {split.declared_millioz}; "
                f"sus miembros tomarían {keys}"
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
        has_debt=report.has_findings,
        closing_comment=(
            f"Ni una desviación de peso el {report.as_of.isoformat()}: los gramos de "
            "Numista y los pesos declarados dicen lo mismo, y ningún tipo sin catálogo "
            "depende del imán."
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
    report = build_report(
        load_catalogs(),
        load_grouping_families(),
        load_fichas(),
        as_of=as_of,
    )

    if arguments.sync:
        sync_issue(report)
    if arguments.markdown or arguments.sync:
        sys.stdout.write(render_markdown(report))
    else:
        sys.stdout.write(render_plain(report))


if __name__ == "__main__":
    main()
