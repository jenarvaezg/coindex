#!/usr/bin/env python3
"""Tests for scripts/type-claims.py — run with `python3 -m unittest scripts/test_type_claims.py`."""

from __future__ import annotations

import importlib.util
import json
import pathlib
import sys
import tempfile
import unittest

ROOT = pathlib.Path(__file__).resolve().parent.parent
SCRIPT = ROOT / "scripts" / "type-claims.py"


def load_claims_module():
    # El guion lleva guion en el nombre: se carga por ruta, como el de stale-catalogs.
    spec = importlib.util.spec_from_file_location("type_claims", SCRIPT)
    assert spec is not None and spec.loader is not None
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


claims = load_claims_module()


def catalog(file_id: str, type_id: int, issues: tuple[int, ...] = ()) -> object:
    return claims.Claim(
        species="catálogo",
        file_id=file_id,
        type_id=type_id,
        issue_ids=frozenset(issues),
    )


class JudgeTests(unittest.TestCase):
    def test_one_file_never_stops(self) -> None:
        verdict = claims.judge(235_118, [catalog("bullion", 235_118)])
        self.assertFalse(verdict.stop)

    def test_two_catalogs_without_issue_qualifiers_stop(self) -> None:
        verdict = claims.judge(
            235_118,
            [catalog("bullion", 235_118), catalog("proof", 235_118, (582_778,))],
        )
        self.assertTrue(verdict.stop)
        self.assertIn("numista_issue_ids", verdict.reason)

    def test_issue_qualified_and_disjoint_is_legitimate(self) -> None:
        # Los tres años en que Numista archiva bullion y proof coloured bajo un tipo.
        verdict = claims.judge(
            235_118,
            [
                catalog("bullion", 235_118, (582_780,)),
                catalog("proof", 235_118, (582_778, 585_569)),
            ],
        )
        self.assertFalse(verdict.stop)

    def test_shared_issue_stops_even_when_both_qualify(self) -> None:
        verdict = claims.judge(
            235_118,
            [
                catalog("bullion", 235_118, (582_780,)),
                catalog("proof", 235_118, (582_780, 585_569)),
            ],
        )
        self.assertTrue(verdict.stop)
        self.assertIn("582780", verdict.reason)

    def test_a_grouping_beside_a_catalog_stops(self) -> None:
        # La agrupación pierde siempre la familia, así que el tipo no se archivaría bajo ella.
        verdict = claims.judge(
            1_492,
            [
                claims.Claim(species="agrupación", file_id="dolar", type_id=1_492),
                catalog("morgan", 1_492, (100,)),
            ],
        )
        self.assertTrue(verdict.stop)
        self.assertIn("agrupación", verdict.reason)

    def test_a_set_beside_a_catalog_stops(self) -> None:
        # El conjunto gana la familia y se lleva la moneda de la tarjeta de la denominación.
        verdict = claims.judge(
            9_830,
            [
                claims.Claim(
                    species="catálogo",
                    file_id="estuche-1983",
                    type_id=9_830,
                    is_set=True,
                ),
                catalog("cinco-escudos", 9_830, (100,)),
            ],
        )
        self.assertTrue(verdict.stop)
        self.assertIn("conjunto", verdict.reason)

    def test_a_programme_coexists_with_a_catalog(self) -> None:
        verdict = claims.judge(
            6_071,
            [
                claims.Claim(species="programa", file_id="herculano", type_id=6_071),
                catalog("dos-cincuenta", 6_071),
            ],
        )
        self.assertFalse(verdict.stop)
        self.assertTrue(verdict.notes)

    def test_two_programmes_stop(self) -> None:
        verdict = claims.judge(
            6_071,
            [
                claims.Claim(species="programa", file_id="herculano", type_id=6_071),
                claims.Claim(species="programa", file_id="alimentacion", type_id=6_071),
            ],
        )
        self.assertTrue(verdict.stop)


class CrossTests(unittest.TestCase):
    def test_a_bare_candidate_id_stops_against_a_catalog(self) -> None:
        verdicts = claims.cross(
            [catalog("bullion", 235_118, (582_780,))],
            claims.parse_candidate_ids(["235118"]),
        )
        self.assertEqual(1, len(verdicts))
        self.assertTrue(verdicts[0].stop)

    def test_a_qualified_candidate_with_disjoint_issues_does_not_stop(self) -> None:
        verdicts = claims.cross(
            [catalog("bullion", 235_118, (582_780,))],
            claims.parse_candidate_ids(["235118:582778,585569"]),
        )
        self.assertFalse(verdicts[0].stop)

    def test_a_free_id_produces_no_verdict(self) -> None:
        verdicts = claims.cross(
            [catalog("bullion", 235_118, (582_780,))],
            claims.parse_candidate_ids(["999999999"]),
        )
        self.assertEqual([], verdicts)

    def test_a_candidate_id_only_a_programme_names_does_not_stop(self) -> None:
        verdicts = claims.cross(
            [claims.Claim(species="programa", file_id="herculano", type_id=6_071)],
            claims.parse_candidate_ids(["6071"]),
        )
        self.assertFalse(verdicts[0].stop)


class LoadClaimsTests(unittest.TestCase):
    def test_reads_the_three_species_and_skips_typeless_members(self) -> None:
        with tempfile.TemporaryDirectory() as raw:
            data = pathlib.Path(raw)
            for name in ("collection-catalogs", "groupings", "programmes"):
                (data / name).mkdir()
            (data / "collection-catalogs" / "run.json").write_text(
                json.dumps(
                    {
                        "id": "run",
                        "schema_version": 2,
                        "members": [
                            {"numista_type_id": 10, "numista_issue_ids": [1, 2]},
                            # Announced: sin tipo, así que no reclama nada.
                            {"status": "announced", "design_type_id": 99},
                        ],
                    }
                ),
                encoding="utf-8",
            )
            (data / "groupings" / "box.json").write_text(
                json.dumps({"id": "box", "type_ids": [20, 21]}), encoding="utf-8"
            )
            (data / "programmes" / "centenary.json").write_text(
                json.dumps(
                    {"id": "centenary", "members": [{"numista_type_id": 10}]}
                ),
                encoding="utf-8",
            )
            loaded = claims.load_claims(data)

        self.assertEqual(
            {("catálogo", "run", 10), ("agrupación", "box", 20),
             ("agrupación", "box", 21), ("programa", "centenary", 10)},
            {(one.species, one.file_id, one.type_id) for one in loaded},
        )
        self.assertEqual(
            frozenset({1, 2}),
            next(one for one in loaded if one.species == "catálogo").issue_ids,
        )

    def test_the_shipped_files_have_no_overlap_that_stops(self) -> None:
        # El mismo cruce que el validador de semillas hace fatal, sobre `data/` real.
        stopping = [verdict for verdict in claims.audit(claims.load_claims()) if verdict.stop]
        self.assertEqual([], [verdict.reason for verdict in stopping])


if __name__ == "__main__":
    unittest.main()
