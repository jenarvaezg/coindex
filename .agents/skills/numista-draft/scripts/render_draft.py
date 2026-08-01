"""Validate and render a safe, sourced Numista contribution dossier."""

from __future__ import annotations

import argparse
import hashlib
import html
import json
from collections.abc import Mapping, Sequence
from dataclasses import dataclass
from datetime import date, datetime
from pathlib import Path
from typing import Any
from urllib.parse import urlparse

SUPPORTED_ACTIONS = frozenset(
    {
        "create_type",
        "edit_type",
        "edit_pending_submission",
        "propose_series",
        "add_dateline_or_variety",
        "ambiguous",
    }
)
REQUIRED_DUPLICATE_SCOPES = frozenset(
    {"coins", "exonumia", "unverified", "pending_submissions"}
)
REQUIRED_CREATE_FIELDS = (
    "title",
    "issuer",
    "type",
    "ruling_authority",
    "value",
    "currency",
    "obverse_description",
    "reverse_description",
    "composition",
    "weight",
    "diameter",
    "thickness",
    "technique",
)
BLOCKING_FIELD_STATUSES = frozenset({"unknown", "inferred", "conflict"})
VALID_FIELD_STATUSES = BLOCKING_FIELD_STATUSES | {"verified", "not_applicable"}
VALID_DUPLICATE_DISPOSITIONS = frozenset(
    {"exact_duplicate", "same_type", "near_neighbour", "unrelated", "not_duplicate"}
)
VALID_IMAGE_RIGHTS_BASES = frozenset(
    {"own_work", "authorized_source", "public_domain", "compatible_license"}
)
ACTION_TITLES = {
    "create_type": "Crear un tipo nuevo",
    "edit_type": "Propuesta de corrección de un tipo",
    "edit_pending_submission": "Revisar una solicitud pendiente",
    "propose_series": "Propuesta estructural de serie",
    "add_dateline_or_variety": "Añadir línea de fecha o variedad",
    "ambiguous": "Acción por determinar",
}


@dataclass(frozen=True)
class Evaluation:
    """Result of validating a dossier for a human-only handoff."""

    readiness: str
    blockers: tuple[str, ...]
    errors: tuple[str, ...]
    warnings: tuple[str, ...]


def _is_mapping(value: Any) -> bool:
    return isinstance(value, Mapping)


def _is_sequence(value: Any) -> bool:
    return isinstance(value, Sequence) and not isinstance(value, (str, bytes, bytearray))


def _valid_web_url(value: Any) -> bool:
    if not isinstance(value, str):
        return False
    if _url_has_unsafe_characters(value):
        return False
    parsed = urlparse(value)
    return (
        parsed.scheme == "https"
        and bool(parsed.hostname)
        and not parsed.username
        and not parsed.password
    )


def _valid_numista_url(value: Any, *, api: bool = False) -> bool:
    if not isinstance(value, str):
        return False
    if _url_has_unsafe_characters(value):
        return False
    parsed = urlparse(value)
    if parsed.scheme != "https" or parsed.username or parsed.password:
        return False
    hostname = (parsed.hostname or "").lower()
    if api:
        return hostname == "api.numista.com" and parsed.path.startswith("/api/v3/")
    return hostname == "numista.com" or hostname.endswith(".numista.com")


def _url_has_unsafe_characters(value: str) -> bool:
    markdown_delimiters = frozenset("<>()[]{}\"'`\\")
    return any(
        ord(character) <= 32
        or ord(character) == 127
        or character in markdown_delimiters
        for character in value
    )


def _source_errors(sources: Any, location: str) -> list[str]:
    if not _is_sequence(sources) or not sources:
        return [f"verified field has no source: {location}"]
    errors: list[str] = []
    for index, source in enumerate(sources):
        source_location = f"{location}.sources[{index}]"
        if not _is_mapping(source):
            errors.append(f"invalid source object: {source_location}")
            continue
        if not str(source.get("label", "")).strip():
            errors.append(f"source has no label: {source_location}")
        if not _valid_web_url(source.get("url")):
            errors.append(f"source has invalid URL: {source_location}")
        if not str(source.get("kind", "")).strip():
            errors.append(f"source has no kind: {source_location}")
    return errors


def _validate_sourced_item(
    item: Any,
    location: str,
    blockers: list[str],
    errors: list[str],
    *,
    require_value: bool = True,
) -> None:
    if not _is_mapping(item):
        errors.append(f"field dossier is not an object: {location}")
        return
    status = item.get("status")
    if not isinstance(status, str) or status not in VALID_FIELD_STATUSES:
        errors.append(f"invalid field status: {location}")
        return
    if status in BLOCKING_FIELD_STATUSES:
        blockers.append(f"field is {status}: {location}")
    if status == "verified":
        if require_value and item.get("value") in (None, ""):
            errors.append(f"verified field has no value: {location}")
        errors.extend(_source_errors(item.get("sources"), location))


def _validate_duplicate_check(
    duplicate_check: Any,
    today: date,
    blockers: list[str],
    errors: list[str],
) -> None:
    if not _is_mapping(duplicate_check):
        blockers.append("missing live duplicate check")
        return
    checked_at_raw = duplicate_check.get("checked_at")
    try:
        checked_at = date.fromisoformat(str(checked_at_raw))
    except ValueError:
        errors.append("duplicate check has invalid checked_at date")
    else:
        age = (today - checked_at).days
        if age < 0:
            errors.append("duplicate check date is in the future")
        elif age > 1:
            blockers.append("duplicate check is stale")

    scopes_raw = duplicate_check.get("scopes", [])
    if not _is_sequence(scopes_raw) or not all(
        isinstance(scope, str) for scope in scopes_raw
    ):
        errors.append("duplicate check scopes must be a list of strings")
        scopes: set[str] = set()
    else:
        scopes = set(scopes_raw)
    missing_scopes = sorted(REQUIRED_DUPLICATE_SCOPES - scopes)
    if missing_scopes:
        blockers.append(
            "duplicate check is missing scopes: " + ", ".join(missing_scopes)
        )
    queries = duplicate_check.get("queries")
    query_scopes: set[str] = set()
    if not _is_sequence(queries) or not queries:
        blockers.append("duplicate check has no recorded queries")
    else:
        for index, query in enumerate(queries):
            if not _is_mapping(query):
                errors.append(f"invalid duplicate query: queries[{index}]")
                continue
            scope = query.get("scope")
            if (
                isinstance(scope, str)
                and scope in REQUIRED_DUPLICATE_SCOPES
                and str(query.get("query", "")).strip()
            ):
                query_scopes.add(str(scope))
            else:
                errors.append(f"invalid duplicate query: queries[{index}]")
        missing_query_scopes = sorted(REQUIRED_DUPLICATE_SCOPES - query_scopes)
        if missing_query_scopes:
            blockers.append(
                "duplicate queries are missing scopes: " + ", ".join(missing_query_scopes)
            )
    candidates = duplicate_check.get("candidates")
    if not _is_sequence(candidates):
        errors.append("duplicate candidates must be a list")
    else:
        for index, candidate in enumerate(candidates):
            location = f"candidates[{index}]"
            if not _is_mapping(candidate) or not _valid_numista_url(candidate.get("url")):
                errors.append(f"invalid duplicate candidate: {location}")
                continue
            disposition = candidate.get("disposition")
            if (
                not isinstance(disposition, str)
                or disposition not in VALID_DUPLICATE_DISPOSITIONS
            ):
                blockers.append(
                    f"duplicate candidate has no valid disposition: {location}"
                )
            elif disposition in {"exact_duplicate", "same_type"}:
                blockers.append(f"duplicate candidate blocks contribution: {location}")
    result = duplicate_check.get("result")
    if result != "not_found":
        blockers.append(f"duplicate check result is not clear: {result or 'missing'}")


def _validate_action(
    dossier: Mapping[str, Any],
    blockers: list[str],
    errors: list[str],
) -> None:
    action = dossier.get("action")
    target = dossier.get("target")
    if not isinstance(action, str) or action not in SUPPORTED_ACTIONS:
        errors.append(f"unsupported action: {action or 'missing'}")
        return
    if action == "ambiguous":
        blockers.append("action is ambiguous")
        return
    if not _is_mapping(target):
        blockers.append("missing action target")
        return
    if action in {"edit_type", "edit_pending_submission", "add_dateline_or_variety"}:
        type_id = target.get("numista_type_id")
        if not isinstance(type_id, int) or isinstance(type_id, bool) or type_id <= 0:
            blockers.append(f"missing Numista type id for action: {action}")
    if action == "propose_series":
        member_ids = target.get("member_type_ids")
        valid_ids = (
            _is_sequence(member_ids)
            and len(member_ids) >= 2
            and all(
                isinstance(member_id, int)
                and not isinstance(member_id, bool)
                and member_id > 0
                for member_id in member_ids
            )
            and len(set(member_ids)) == len(member_ids)
        )
        if not valid_ids:
            blockers.append("series proposal needs at least two member type ids")
    if action == "add_dateline_or_variety" and dossier.get("change_kind") != "add":
        errors.append("date line or variety action must only add")


def _validate_fields(
    dossier: Mapping[str, Any],
    blockers: list[str],
    errors: list[str],
) -> None:
    fields = dossier.get("fields")
    if not _is_mapping(fields):
        blockers.append("missing field dossier")
        return
    if dossier.get("action") == "create_type":
        for field_name in REQUIRED_CREATE_FIELDS:
            if field_name not in fields:
                blockers.append(f"missing required field: {field_name}")
            elif not _is_mapping(fields[field_name]) or fields[field_name].get("status") != "verified":
                blockers.append(f"required field is not verified: {field_name}")
    action = dossier.get("action")
    if isinstance(action, str) and action in {"edit_type", "edit_pending_submission"}:
        verified_changes = [
            item
            for item in fields.values()
            if _is_mapping(item)
            and item.get("status") == "verified"
            and item.get("value") not in (None, "")
        ]
        if not verified_changes:
            blockers.append("edit has no verified proposed change")
    for field_name, item in fields.items():
        _validate_sourced_item(item, str(field_name), blockers, errors)


def _validate_date_lines(
    dossier: Mapping[str, Any],
    blockers: list[str],
    errors: list[str],
) -> None:
    date_lines = dossier.get("date_lines")
    action = dossier.get("action")
    if isinstance(action, str) and action in {
        "create_type",
        "add_dateline_or_variety",
    } and (
        not _is_sequence(date_lines) or not date_lines
    ):
        blockers.append("at least one sourced date line is required")
        return
    if date_lines is None:
        return
    if not _is_sequence(date_lines):
        errors.append("date_lines must be a list")
        return
    for index, line in enumerate(date_lines):
        location = f"date_lines[{index}]"
        _validate_sourced_item(
            line, location, blockers, errors, require_value=False
        )
        if _is_mapping(line) and not str(line.get("year", "")).strip():
            errors.append(f"date line has no year: {location}")


def _validate_image_rights(
    image_rights: Any,
    blockers: list[str],
    errors: list[str],
) -> None:
    if not _is_mapping(image_rights):
        blockers.append("missing image-rights decision")
        return
    mode = image_rights.get("mode")
    items = image_rights.get("items", [])
    if not isinstance(mode, str) or mode not in {"none", "candidate_images"}:
        errors.append("invalid image-rights mode")
    if mode == "none" and not image_rights.get("descriptions_complete_without_images"):
        blockers.append("descriptions or licensed image candidates are required")
    if mode == "candidate_images":
        if not _is_sequence(items) or not items:
            blockers.append("image candidates have no rights records")
        else:
            for index, item in enumerate(items):
                location = f"image_rights.items[{index}]"
                if not _is_mapping(item):
                    errors.append(f"invalid image-rights record: {location}")
                    continue
                if not _valid_web_url(item.get("source_url")):
                    errors.append(f"image has invalid source URL: {location}")
                rights_basis = item.get("rights_basis")
                if (
                    not isinstance(rights_basis, str)
                    or rights_basis not in VALID_IMAGE_RIGHTS_BASES
                ):
                    blockers.append(f"image has no verified rights basis: {location}")
                if not str(item.get("creator_or_credit", "")).strip():
                    blockers.append(f"image has no creator or credit: {location}")
                if item.get("rights_basis") == "compatible_license" and not str(
                    item.get("license", "")
                ).strip():
                    blockers.append(f"image has no compatible license: {location}")


def _validate_api_plan(
    api_plan: Any,
    blockers: list[str],
    errors: list[str],
    approved_api_plan_digest: str | None,
) -> None:
    if not _is_mapping(api_plan):
        blockers.append("missing API budget plan")
        return
    estimated_calls = api_plan.get("estimated_calls")
    if (
        not isinstance(estimated_calls, int)
        or isinstance(estimated_calls, bool)
        or estimated_calls < 0
    ):
        errors.append("API estimated_calls must be a non-negative integer")
        return
    calls = api_plan.get("calls")
    if not _is_sequence(calls):
        errors.append("API calls must be a list")
        calls = []
    if estimated_calls != len(calls):
        errors.append("API estimated_calls must match calls")
    for index, call in enumerate(calls):
        if (
            not _is_mapping(call)
            or call.get("method") != "GET"
            or not _valid_numista_url(call.get("url"), api=True)
        ):
            errors.append(f"API call is not an allowed read-only request: calls[{index}]")
    if api_plan.get("requires_confirmation") is not True:
        errors.append("API plan must require confirmation")
    if estimated_calls > 0:
        digest = api_plan_digest(api_plan)
        if approved_api_plan_digest != digest:
            blockers.append(
                "Numista API plan requires trusted approval digest: " + digest
            )


def api_plan_digest(api_plan: Mapping[str, Any]) -> str:
    """Return a stable digest that can be approved outside an untrusted dossier."""

    plan = {
        "estimated_calls": api_plan.get("estimated_calls"),
        "calls": api_plan.get("calls"),
    }
    encoded = json.dumps(
        plan, ensure_ascii=False, sort_keys=True, separators=(",", ":")
    ).encode("utf-8")
    return hashlib.sha256(encoded).hexdigest()


def _validate_browser_handoff(
    handoff: Any,
    action: Any,
    target: Any,
    blockers: list[str],
    errors: list[str],
) -> None:
    if not _is_mapping(handoff):
        blockers.append("missing browser review handoff")
        return
    if handoff.get("enabled") is not True:
        blockers.append("visible browser review is not enabled")
    if handoff.get("visible") is not True:
        errors.append("browser handoff must be explicitly visible")
    if handoff.get("headless") is not False:
        errors.append("browser handoff must explicitly disable headless mode")
    review_url = handoff.get("review_url")
    if not _valid_numista_url(review_url):
        errors.append("browser handoff must use an HTTPS Numista URL")
    else:
        parsed = urlparse(str(review_url))
        hostname = (parsed.hostname or "").lower()
        path = parsed.path.rstrip("/") or "/"
        route_ok = hostname != "api.numista.com" and not parsed.fragment
        if action == "create_type":
            route_ok = route_ok and path == "/catalogue/contributions/nouveau.php"
            route_ok = route_ok and parsed.query == "type=coin"
        elif action == "propose_series":
            route_ok = route_ok and path == "/forum" and not parsed.query
        elif isinstance(action, str) and action in {
            "edit_type",
            "edit_pending_submission",
            "add_dateline_or_variety",
        }:
            type_id = target.get("numista_type_id") if _is_mapping(target) else None
            route_ok = (
                route_ok
                and isinstance(type_id, int)
                and not isinstance(type_id, bool)
                and path == f"/catalogue/pieces{type_id}.html"
                and not parsed.query
            )
        else:
            route_ok = False
        if not route_ok:
            errors.append("browser handoff route does not match the action")
    for operation in ("save", "submit", "upload"):
        if handoff.get(f"allow_{operation}") is not False:
            errors.append(f"browser handoff may not {operation}")


def evaluate(
    dossier: Mapping[str, Any],
    today: date | None = None,
    approved_api_plan_digest: str | None = None,
) -> Evaluation:
    """Evaluate whether *dossier* is safe and sufficiently sourced for review."""

    current_date = today or datetime.now().astimezone().date()
    blockers: list[str] = []
    errors: list[str] = []
    warnings: list[str] = []
    if not _is_mapping(dossier):
        return Evaluation(
            readiness="not_ready",
            blockers=(),
            errors=("dossier root must be an object",),
            warnings=(),
        )
    if dossier.get("schema_version") != 1:
        errors.append("unsupported or missing schema_version")

    _validate_action(dossier, blockers, errors)
    _validate_duplicate_check(dossier.get("duplicate_check"), current_date, blockers, errors)
    _validate_fields(dossier, blockers, errors)
    _validate_date_lines(dossier, blockers, errors)
    _validate_image_rights(dossier.get("image_rights"), blockers, errors)
    _validate_api_plan(
        dossier.get("api_plan"), blockers, errors, approved_api_plan_digest
    )
    _validate_browser_handoff(
        dossier.get("browser_handoff"),
        dossier.get("action"),
        dossier.get("target"),
        blockers,
        errors,
    )

    conflicts = dossier.get("conflicts", [])
    if not _is_sequence(conflicts):
        errors.append("conflicts must be a list")
    elif conflicts:
        blockers.append("unresolved source conflicts remain")
    open_questions = dossier.get("open_questions", [])
    if not _is_sequence(open_questions):
        errors.append("open_questions must be a list")
    elif open_questions:
        blockers.append("open research questions remain")

    readiness = "ready" if not blockers and not errors else "not_ready"
    return Evaluation(
        readiness=readiness,
        blockers=tuple(dict.fromkeys(blockers)),
        errors=tuple(dict.fromkeys(errors)),
        warnings=tuple(dict.fromkeys(warnings)),
    )


def _display(value: Any) -> str:
    if value is None:
        return "—"
    if isinstance(value, (dict, list)):
        value = json.dumps(value, ensure_ascii=False, sort_keys=True)
    escaped = html.escape(str(value), quote=True).replace("\\", "\\\\")
    for character in ("|", "[", "]", "*", "_"):
        escaped = escaped.replace(character, f"\\{character}")
    return escaped.replace("\r", " ").replace("\n", " ")


def _source_links(sources: Any) -> str:
    if not _is_sequence(sources):
        return "—"
    links = []
    for source in sources:
        if not _is_mapping(source):
            continue
        label = _display(source.get("label", "Fuente"))
        url = source.get("url")
        kind = _display(source.get("kind")) if source.get("kind") else None
        if _valid_web_url(url):
            hostname = urlparse(str(url)).hostname or "dominio desconocido"
            link = f"[{label} — {_display(hostname)}](<{url}>)"
        else:
            link = label
        if kind:
            link += f" ({kind})"
        links.append(link)
    return "; ".join(links) or "—"


def render_markdown(dossier: Mapping[str, Any], result: Evaluation) -> str:
    """Render a dossier and its evaluation as review-first Spanish Markdown."""

    action = str(dossier.get("action", "ambiguous"))
    title = ACTION_TITLES.get(action, "Acción no compatible")
    target = dossier.get("target", {})
    duplicate = dossier.get("duplicate_check", {})
    handoff = dossier.get("browser_handoff", {})
    lines = [
        f"# Borrador Numista: {title}",
        "",
        f"**Estado:** `{result.readiness}`",
        f"**Objetivo:** {_display(target.get('summary') if _is_mapping(target) else None)}",
        "",
    ]
    if action == "propose_series":
        lines.extend(
            [
                "## Propuesta estructural",
                "",
                "Preparar el texto para el foro o para administración; no usar el formulario de alta de tipos.",
                "",
            ]
        )

    lines.extend(["## Evidencia por campo", "", "| Campo | Valor | Estado | Fuentes |", "|---|---|---|---|"])
    fields = dossier.get("fields", {})
    if _is_mapping(fields):
        for field_name, item in fields.items():
            if _is_mapping(item):
                lines.append(
                    f"| {_display(field_name)} | {_display(item.get('value'))} | "
                    f"{_display(item.get('status'))} | {_source_links(item.get('sources'))} |"
                )
            else:
                lines.append(f"| {_display(field_name)} | — | inválido | — |")

    date_lines = dossier.get("date_lines", [])
    if _is_sequence(date_lines) and date_lines:
        lines.extend(
            [
                "",
                "## Líneas de fecha y variedades",
                "",
                "| Año | Ceca | Tirada | Comentario | Estado | Fuentes |",
                "|---|---|---|---|---|---|",
            ]
        )
        for item in date_lines:
            if not _is_mapping(item):
                continue
            lines.append(
                f"| {_display(item.get('year'))} | {_display(item.get('mint'))} | "
                f"{_display(item.get('mintage'))} | {_display(item.get('comment'))} | "
                f"{_display(item.get('status'))} | {_source_links(item.get('sources'))} |"
            )

    queries = duplicate.get("queries", []) if _is_mapping(duplicate) else []
    candidates = duplicate.get("candidates", []) if _is_mapping(duplicate) else []
    lines.extend(
        [
            "",
            "## Comprobación de duplicados",
            "",
            f"- Fecha: {_display(duplicate.get('checked_at') if _is_mapping(duplicate) else None)}",
            f"- Resultado: {_display(duplicate.get('result') if _is_mapping(duplicate) else None)}",
            f"- Ámbitos: {_display(duplicate.get('scopes') if _is_mapping(duplicate) else None)}",
            f"- Consultas: {_display(queries)}",
            f"- Candidatos resueltos: {_display(candidates)}",
            "",
            "## Bloqueos y errores",
            "",
        ]
    )
    findings = list(result.blockers) + list(result.errors)
    lines.extend(f"- {_display(finding)}" for finding in findings)
    if not findings:
        lines.append("- Ninguno")

    review_url = handoff.get("review_url") if _is_mapping(handoff) else None
    api_plan = dossier.get("api_plan", {})
    plan_digest = api_plan_digest(api_plan) if _is_mapping(api_plan) else None
    lines.extend(
        [
            "",
            "## Plan de API",
            "",
            f"- Llamadas estimadas: {_display(api_plan.get('estimated_calls') if _is_mapping(api_plan) else None)}",
            f"- Llamadas previstas: {_display(api_plan.get('calls') if _is_mapping(api_plan) else None)}",
            f"- Hash del plan: {_display(plan_digest)}",
        ]
    )
    if result.readiness == "ready":
        browser_instruction = "**Abrir y rellenar; no guardar, subir ni enviar.**"
    else:
        browser_instruction = "**No abrir ni rellenar: resolver primero los bloqueos.**"
    lines.extend(
        [
            "",
            "## Entrega al navegador visible",
            "",
            browser_instruction,
            "",
            f"Ruta de revisión: {_display(review_url)}",
            "",
            "La persona usuaria debe revisar y ejecutar manualmente cualquier acción final.",
        ]
    )
    return "\n".join(lines) + "\n"


def _parse_today(raw: str) -> date:
    try:
        return date.fromisoformat(raw)
    except ValueError as exc:
        raise argparse.ArgumentTypeError("expected YYYY-MM-DD") from exc


def _build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Validate and render a safe Numista contribution draft."
    )
    parser.add_argument("dossier", type=Path, help="JSON dossier to validate")
    parser.add_argument(
        "--output",
        "-o",
        type=Path,
        required=True,
        help="write Markdown to a new file",
    )
    parser.add_argument(
        "--today", type=_parse_today, default=datetime.now().astimezone().date()
    )
    parser.add_argument(
        "--require-ready",
        action="store_true",
        help="return exit status 1 unless the dossier is ready",
    )
    parser.add_argument(
        "--approve-api-plan-digest",
        help="trusted SHA-256 approval for the exact read-only API plan",
    )
    return parser


def main(argv: Sequence[str] | None = None) -> int:
    parser = _build_parser()
    args = parser.parse_args(argv)
    try:
        if args.dossier.stat().st_size > 2 * 1024 * 1024:
            parser.error("dossier exceeds the 2 MiB safety limit")
        dossier = json.loads(args.dossier.read_text(encoding="utf-8"))
    except (OSError, UnicodeDecodeError, json.JSONDecodeError, RecursionError) as exc:
        parser.error(str(exc))
    if not _is_mapping(dossier):
        parser.error("dossier root must be an object")
    result = evaluate(
        dossier,
        today=args.today,
        approved_api_plan_digest=args.approve_api_plan_digest,
    )
    markdown = render_markdown(dossier, result)
    if args.output == args.dossier or args.output.exists() or args.output.is_symlink():
        parser.error("output must be a new file distinct from the dossier")
    try:
        with args.output.open("x", encoding="utf-8") as output_file:
            output_file.write(markdown)
    except OSError as exc:
        parser.error(str(exc))
    if args.require_ready and result.readiness != "ready":
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
