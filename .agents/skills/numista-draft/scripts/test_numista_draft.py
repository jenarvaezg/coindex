"""Contract tests for the Numista draft renderer."""

from __future__ import annotations

import importlib.util
import json
import subprocess
import sys
import tempfile
import unittest
from datetime import date
from pathlib import Path

SCRIPT = Path(__file__).with_name("render_draft.py")


def load_renderer():
    spec = importlib.util.spec_from_file_location("numista_draft_renderer", SCRIPT)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


def sourced(value):
    return {
        "value": value,
        "status": "verified",
        "sources": [
            {
                "label": "The Perth Mint",
                "url": "https://www.perthmint.com/example",
                "kind": "official",
            }
        ],
    }


def ready_create_dossier():
    return {
        "schema_version": 1,
        "action": "create_type",
        "issue": {
            "number": 50,
            "title": "Submit a Numista del conejo 2023",
            "url": "https://github.com/jenarvaezg/coindex/issues/50",
        },
        "target": {"summary": "2023 Rabbit 1oz Silver Proof Coloured"},
        "duplicate_check": {
            "checked_at": "2026-08-01",
            "result": "not_found",
            "scopes": ["coins", "exonumia", "unverified", "pending_submissions"],
            "queries": [
                {"scope": scope, "query": "2023 Rabbit 1oz Silver Proof Coloured"}
                for scope in ("coins", "exonumia", "unverified", "pending_submissions")
            ],
            "candidates": [],
        },
        "fields": {
            "title": sourced("1 Dollar - Year of the Rabbit - Silver Proof Coloured"),
            "issuer": sourced("Australia"),
            "type": sourced("Non-circulating coin"),
            "ruling_authority": sourced("Elizabeth II"),
            "value": sourced("1 Dollar"),
            "currency": sourced("Australian dollar"),
            "obverse_description": sourced("Sixth portrait of Elizabeth II"),
            "reverse_description": sourced("Two coloured rabbits"),
            "composition": sourced("Silver (.9999)"),
            "weight": sourced("31.107 g"),
            "diameter": sourced("40.90 mm"),
            "thickness": sourced("3.50 mm maximum"),
            "technique": sourced("Milled, Coloured"),
        },
        "date_lines": [
            {
                "year": "2023",
                "mint": "P",
                "mintage": "7,500 maximum",
                "comment": "Proof - Coloured",
                "status": "verified",
                "sources": [
                    {
                        "label": "The Perth Mint",
                        "url": "https://www.perthmint.com/example",
                        "kind": "official",
                    }
                ],
            }
        ],
        "image_rights": {
            "mode": "none",
            "descriptions_complete_without_images": True,
            "items": [],
        },
        "conflicts": [],
        "open_questions": [],
        "api_plan": {"estimated_calls": 0, "calls": [], "requires_confirmation": True},
        "browser_handoff": {
            "enabled": True,
            "visible": True,
            "headless": False,
            "review_url": "https://en.numista.com/catalogue/contributions/nouveau.php?type=coin",
            "allow_save": False,
            "allow_submit": False,
            "allow_upload": False,
        },
    }


class EvaluateTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.renderer = load_renderer()

    def test_complete_sourced_create_is_ready(self):
        result = self.renderer.evaluate(ready_create_dossier(), today=date(2026, 8, 1))
        self.assertEqual("ready", result.readiness)

    def test_unknown_field_blocks_readiness(self):
        dossier = ready_create_dossier()
        dossier["fields"]["weight"]["status"] = "unknown"
        result = self.renderer.evaluate(dossier, today=date(2026, 8, 1))
        self.assertEqual("not_ready", result.readiness)

    def test_inferred_field_blocks_readiness(self):
        dossier = ready_create_dossier()
        dossier["fields"]["title"]["status"] = "inferred"
        result = self.renderer.evaluate(dossier, today=date(2026, 8, 1))
        self.assertEqual("not_ready", result.readiness)

    def test_stale_duplicate_check_blocks_readiness(self):
        dossier = ready_create_dossier()
        dossier["duplicate_check"]["checked_at"] = "2026-07-25"
        result = self.renderer.evaluate(dossier, today=date(2026, 8, 1))
        self.assertIn("duplicate check is stale", result.blockers)

    def test_all_duplicate_scopes_are_required(self):
        dossier = ready_create_dossier()
        dossier["duplicate_check"]["scopes"].remove("pending_submissions")
        result = self.renderer.evaluate(dossier, today=date(2026, 8, 1))
        self.assertIn("duplicate check is missing scopes: pending_submissions", result.blockers)

    def test_missing_required_field_blocks_readiness(self):
        dossier = ready_create_dossier()
        del dossier["fields"]["issuer"]
        result = self.renderer.evaluate(dossier, today=date(2026, 8, 1))
        self.assertIn("missing required field: issuer", result.blockers)

    def test_verified_field_without_source_is_rejected(self):
        dossier = ready_create_dossier()
        dossier["fields"]["composition"]["sources"] = []
        result = self.renderer.evaluate(dossier, today=date(2026, 8, 1))
        self.assertIn("verified field has no source: composition", result.errors)

    def test_required_field_without_value_is_rejected(self):
        dossier = ready_create_dossier()
        del dossier["fields"]["issuer"]["value"]
        result = self.renderer.evaluate(dossier, today=date(2026, 8, 1))
        self.assertIn("verified field has no value: issuer", result.errors)

    def test_required_field_cannot_be_not_applicable(self):
        dossier = ready_create_dossier()
        dossier["fields"]["issuer"]["status"] = "not_applicable"
        result = self.renderer.evaluate(dossier, today=date(2026, 8, 1))
        self.assertIn("required field is not verified: issuer", result.blockers)

    def test_browser_submit_is_forbidden(self):
        dossier = ready_create_dossier()
        dossier["browser_handoff"]["allow_submit"] = True
        result = self.renderer.evaluate(dossier, today=date(2026, 8, 1))
        self.assertIn("browser handoff may not submit", result.errors)

    def test_browser_save_is_forbidden(self):
        dossier = ready_create_dossier()
        dossier["browser_handoff"]["allow_save"] = True
        result = self.renderer.evaluate(dossier, today=date(2026, 8, 1))
        self.assertIn("browser handoff may not save", result.errors)

    def test_browser_upload_is_forbidden(self):
        dossier = ready_create_dossier()
        dossier["browser_handoff"]["allow_upload"] = True
        result = self.renderer.evaluate(dossier, today=date(2026, 8, 1))
        self.assertIn("browser handoff may not upload", result.errors)

    def test_browser_visibility_must_be_explicit(self):
        dossier = ready_create_dossier()
        del dossier["browser_handoff"]["visible"]
        result = self.renderer.evaluate(dossier, today=date(2026, 8, 1))
        self.assertIn("browser handoff must be explicitly visible", result.errors)

    def test_browser_url_must_be_https_numista(self):
        dossier = ready_create_dossier()
        dossier["browser_handoff"]["review_url"] = "http://numista.example/login"
        result = self.renderer.evaluate(dossier, today=date(2026, 8, 1))
        self.assertIn("browser handoff must use an HTTPS Numista URL", result.errors)

    def test_api_call_count_must_match_plan(self):
        dossier = ready_create_dossier()
        dossier["api_plan"]["calls"] = [
            {"method": "GET", "url": "https://api.numista.com/api/v3/types/353213"}
        ]
        result = self.renderer.evaluate(dossier, today=date(2026, 8, 1))
        self.assertIn("API estimated_calls must match calls", result.errors)

    def test_mutating_api_call_is_rejected(self):
        dossier = ready_create_dossier()
        dossier["api_plan"].update(
            {
                "estimated_calls": 1,
                "confirmed_by_user": True,
                "calls": [
                    {"method": "POST", "url": "https://api.numista.com/api/v3/types"}
                ],
            }
        )
        result = self.renderer.evaluate(dossier, today=date(2026, 8, 1))
        self.assertIn("API call is not an allowed read-only request: calls[0]", result.errors)

    def test_api_confirmation_inside_dossier_is_not_trusted(self):
        dossier = ready_create_dossier()
        dossier["api_plan"].update(
            {
                "estimated_calls": 1,
                "confirmed_by_user": True,
                "calls": [
                    {
                        "method": "GET",
                        "url": "https://api.numista.com/api/v3/types/353213",
                    }
                ],
            }
        )
        result = self.renderer.evaluate(dossier, today=date(2026, 8, 1))
        self.assertIn("trusted approval digest", "\n".join(result.blockers))

    def test_matching_external_api_digest_is_trusted(self):
        dossier = ready_create_dossier()
        dossier["api_plan"].update(
            {
                "estimated_calls": 1,
                "calls": [
                    {
                        "method": "GET",
                        "url": "https://api.numista.com/api/v3/types/353213",
                    }
                ],
            }
        )
        digest = self.renderer.api_plan_digest(dossier["api_plan"])
        result = self.renderer.evaluate(
            dossier,
            today=date(2026, 8, 1),
            approved_api_plan_digest=digest,
        )
        self.assertEqual("ready", result.readiness)

    def test_duplicate_queries_must_cover_each_scope(self):
        dossier = ready_create_dossier()
        dossier["duplicate_check"]["queries"] = dossier["duplicate_check"]["queries"][:1]
        result = self.renderer.evaluate(dossier, today=date(2026, 8, 1))
        self.assertIn("duplicate queries are missing scopes", "\n".join(result.blockers))

    def test_duplicate_candidate_requires_disposition(self):
        dossier = ready_create_dossier()
        dossier["duplicate_check"]["candidates"] = [
            {"url": "https://en.numista.com/353213", "relation": "near_neighbour"}
        ]
        result = self.renderer.evaluate(dossier, today=date(2026, 8, 1))
        self.assertIn("duplicate candidate has no valid disposition: candidates[0]", result.blockers)

    def test_add_dateline_must_be_an_addition(self):
        dossier = ready_create_dossier()
        dossier["action"] = "add_dateline_or_variety"
        dossier["target"]["numista_type_id"] = 1885
        dossier["change_kind"] = "update"
        result = self.renderer.evaluate(dossier, today=date(2026, 8, 1))
        self.assertIn("date line or variety action must only add", result.errors)

    def test_nested_unhashable_enum_values_do_not_crash(self):
        mutations = (
            lambda dossier: dossier.update(action=[]),
            lambda dossier: dossier["fields"]["issuer"].update(status=[]),
            lambda dossier: dossier["duplicate_check"]["queries"][0].update(scope=[]),
            lambda dossier: dossier["duplicate_check"].update(
                candidates=[
                    {
                        "url": "https://en.numista.com/353213",
                        "disposition": [],
                    }
                ]
            ),
            lambda dossier: dossier["image_rights"].update(mode=[]),
            lambda dossier: dossier["image_rights"].update(
                mode="candidate_images",
                items=[
                    {
                        "source_url": "https://www.perthmint.com/example",
                        "rights_basis": [],
                        "creator_or_credit": "The Perth Mint",
                    }
                ],
            ),
        )
        for mutate in mutations:
            with self.subTest(mutate=mutate):
                dossier = ready_create_dossier()
                mutate(dossier)
                result = self.renderer.evaluate(dossier, today=date(2026, 8, 1))
                self.assertEqual("not_ready", result.readiness)

    def test_ambiguous_action_is_never_ready(self):
        dossier = ready_create_dossier()
        dossier["action"] = "ambiguous"
        result = self.renderer.evaluate(dossier, today=date(2026, 8, 1))
        self.assertEqual("not_ready", result.readiness)

    def test_pending_submission_is_not_described_as_published(self):
        dossier = ready_create_dossier()
        dossier["action"] = "edit_pending_submission"
        dossier["target"]["numista_type_id"] = 596807
        result = self.renderer.evaluate(dossier, today=date(2026, 8, 1))
        markdown = self.renderer.render_markdown(dossier, result)
        self.assertNotIn("Publicar solicitud pendiente", markdown)

    def test_series_proposal_uses_structural_route(self):
        dossier = ready_create_dossier()
        dossier["action"] = "propose_series"
        dossier["target"]["member_type_ids"] = [7962, 7963, 7964, 7965, 7966]
        result = self.renderer.evaluate(dossier, today=date(2026, 8, 1))
        markdown = self.renderer.render_markdown(dossier, result)
        self.assertIn("Propuesta estructural", markdown)

    def test_markdown_contains_field_sources_and_safety_handoff(self):
        dossier = ready_create_dossier()
        result = self.renderer.evaluate(dossier, today=date(2026, 8, 1))
        markdown = self.renderer.render_markdown(dossier, result)
        self.assertIn("Abrir y rellenar; no guardar, subir ni enviar", markdown)
        self.assertIn(
            "[The Perth Mint — www.perthmint.com](<https://www.perthmint.com/example>)",
            markdown,
        )

    def test_blocked_markdown_forbids_browser_prefill(self):
        dossier = ready_create_dossier()
        dossier["fields"]["weight"]["status"] = "unknown"
        result = self.renderer.evaluate(dossier, today=date(2026, 8, 1))
        markdown = self.renderer.render_markdown(dossier, result)
        self.assertIn("No abrir ni rellenar", markdown)
        self.assertNotIn("**Abrir y rellenar; no guardar, subir ni enviar.**", markdown)

    def test_markdown_escapes_untrusted_labels_and_shows_domain(self):
        dossier = ready_create_dossier()
        dossier["fields"]["title"]["sources"][0]["label"] = "<script>alert(1)</script> [fake]"
        result = self.renderer.evaluate(dossier, today=date(2026, 8, 1))
        markdown = self.renderer.render_markdown(dossier, result)
        self.assertNotIn("<script>", markdown)
        self.assertIn("perthmint.com", markdown)

    def test_markdown_rejects_unsafe_source_url_delimiters(self):
        dossier = ready_create_dossier()
        dossier["fields"]["title"]["sources"][0]["url"] = (
            "https://www.perthmint.com/x>) [fake](https://evil.example"
        )
        result = self.renderer.evaluate(dossier, today=date(2026, 8, 1))
        markdown = self.renderer.render_markdown(dossier, result)
        self.assertEqual("not_ready", result.readiness)
        self.assertNotIn("evil.example>)", markdown)

    def test_edit_route_must_reference_target_contribution(self):
        dossier = ready_create_dossier()
        dossier["action"] = "edit_type"
        dossier["target"]["numista_type_id"] = 353213
        result = self.renderer.evaluate(dossier, today=date(2026, 8, 1))
        self.assertIn("browser handoff route does not match the action", result.errors)

    def test_edit_route_accepts_only_safe_target_landing_page(self):
        dossier = ready_create_dossier()
        dossier["action"] = "edit_type"
        dossier["target"]["numista_type_id"] = 353213
        dossier["browser_handoff"]["review_url"] = (
            "https://en.numista.com/catalogue/pieces353213.html"
        )
        result = self.renderer.evaluate(dossier, today=date(2026, 8, 1))
        self.assertEqual("ready", result.readiness)

    def test_edit_requires_a_verified_change(self):
        dossier = ready_create_dossier()
        dossier["action"] = "edit_type"
        dossier["target"]["numista_type_id"] = 353213
        dossier["fields"] = {"edge": {"status": "not_applicable"}}
        dossier["browser_handoff"]["review_url"] = (
            "https://en.numista.com/catalogue/pieces353213.html"
        )
        result = self.renderer.evaluate(dossier, today=date(2026, 8, 1))
        self.assertIn("edit has no verified proposed change", result.blockers)

    def test_source_url_requires_a_real_hostname(self):
        dossier = ready_create_dossier()
        dossier["fields"]["title"]["sources"][0]["url"] = "https://:443/path"
        result = self.renderer.evaluate(dossier, today=date(2026, 8, 1))
        self.assertEqual("not_ready", result.readiness)

    def test_diagnostics_escape_untrusted_field_names(self):
        dossier = ready_create_dossier()
        dossier["fields"]["<img src=x onerror=alert(1)>"] = {
            "status": "verified",
            "sources": [],
        }
        result = self.renderer.evaluate(dossier, today=date(2026, 8, 1))
        markdown = self.renderer.render_markdown(dossier, result)
        self.assertNotIn("<img", markdown)
        self.assertIn("&lt;img", markdown)


class CliTests(unittest.TestCase):
    def test_output_file_is_required(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            input_path = Path(temp_dir) / "draft.json"
            input_path.write_text(json.dumps(ready_create_dossier()), encoding="utf-8")
            completed = subprocess.run(
                [sys.executable, str(SCRIPT), str(input_path)],
                check=False,
                capture_output=True,
                text=True,
            )
        self.assertEqual(2, completed.returncode)

    def test_non_object_json_is_reported_without_traceback(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            input_path = Path(temp_dir) / "draft.json"
            output_path = Path(temp_dir) / "draft.md"
            input_path.write_text("[]", encoding="utf-8")
            completed = subprocess.run(
                [sys.executable, str(SCRIPT), str(input_path), "-o", str(output_path)],
                check=False,
                capture_output=True,
                text=True,
            )
        self.assertEqual(2, completed.returncode)
        self.assertNotIn("Traceback", completed.stderr)

    def test_require_ready_returns_one_for_blocked_draft(self):
        dossier = ready_create_dossier()
        dossier["fields"]["weight"]["status"] = "conflict"
        with tempfile.TemporaryDirectory() as temp_dir:
            input_path = Path(temp_dir) / "draft.json"
            output_path = Path(temp_dir) / "draft.md"
            input_path.write_text(json.dumps(dossier), encoding="utf-8")
            completed = subprocess.run(
                [
                    sys.executable,
                    str(SCRIPT),
                    str(input_path),
                    "--today",
                    "2026-08-01",
                    "--require-ready",
                    "-o",
                    str(output_path),
                ],
                check=False,
                capture_output=True,
                text=True,
            )
        self.assertEqual(1, completed.returncode)


if __name__ == "__main__":
    unittest.main()
