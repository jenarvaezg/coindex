#!/usr/bin/env python3
"""Tests for scripts/stale-catalogs.py — run with `python3 -m unittest scripts/test_stale_catalogs.py`."""

from __future__ import annotations

import importlib.util
import pathlib
import sys
import tempfile
import unittest
from datetime import date

ROOT = pathlib.Path(__file__).resolve().parent.parent
SCRIPT = ROOT / "scripts" / "stale-catalogs.py"


def load_stale():
    # Avoid dataclasses looking up a missing module entry during import.
    spec = importlib.util.spec_from_file_location("stale_catalogs", SCRIPT)
    assert spec is not None and spec.loader is not None
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


stale = load_stale()


class BuildReportTests(unittest.TestCase):
    def test_no_issue_years_drop_out_of_interior_gaps(self) -> None:
        catalog = stale.CatalogYears(
            path=pathlib.Path("britannia.json"),
            catalog_id="uk-silver-britannia-quarter-oz-bullion",
            name="Britannia ¼ oz",
            years=frozenset({2013, 2014, 2015, 2021, 2023, 2024, 2025, 2026}),
            no_issue_years=frozenset({2016, 2017, 2018, 2019, 2020, 2022}),
        )
        report = stale.build_report([catalog], as_of=date(2026, 8, 3))
        self.assertEqual(report.gapped, ())
        self.assertEqual(report.lagging, ())

    def test_undeclared_gaps_remain_debt(self) -> None:
        catalog = stale.CatalogYears(
            path=pathlib.Path("monuments.json"),
            catalog_id="architectural-monuments-russia-3-roubles",
            name="Monumentos",
            years=frozenset({1993, 2000, 2002, 2026}),
            no_issue_years=frozenset(),
        )
        report = stale.build_report([catalog], as_of=date(2026, 8, 3))
        self.assertEqual(len(report.gapped), 1)
        _, missing = report.gapped[0]
        self.assertIn(2001, missing)
        self.assertIn(2025, missing)
        self.assertNotIn(2026, missing)

    def test_cola_ignores_no_issue_years(self) -> None:
        # Silenciar un hueco interior no saca el catálogo de la cola: la cola mira
        # solo si existe alguna casilla del año en curso.
        catalog = stale.CatalogYears(
            path=pathlib.Path("libertad.json"),
            catalog_id="mexico-libertad-1oz-bullion",
            name="Libertad",
            years=frozenset({2023, 2024, 2025}),
            no_issue_years=frozenset(),
        )
        with_silenced_gap = stale.CatalogYears(
            path=catalog.path,
            catalog_id=catalog.catalog_id,
            name=catalog.name,
            years=frozenset({2022, 2024, 2025}),
            no_issue_years=frozenset({2023}),
        )
        as_of = date(2026, 8, 3)
        report = stale.build_report([with_silenced_gap], as_of=as_of)
        self.assertEqual([c.catalog_id for c in report.lagging], [catalog.catalog_id])
        self.assertEqual(report.gapped, ())
        # Sin silenciar, el 2023 sería hueco — y la cola seguiría igual.
        unsilenced = stale.build_report(
            [
                stale.CatalogYears(
                    path=catalog.path,
                    catalog_id=catalog.catalog_id,
                    name=catalog.name,
                    years=frozenset({2022, 2024, 2025}),
                    no_issue_years=frozenset(),
                )
            ],
            as_of=as_of,
        )
        self.assertEqual([c.catalog_id for c in unsilenced.lagging], [catalog.catalog_id])
        self.assertEqual(unsilenced.gapped[0][1], (2023,))


class LoadOpenCatalogsTests(unittest.TestCase):
    def test_reads_no_issue_years_from_json(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            path = pathlib.Path(tmp) / "sample.json"
            path.write_text(
                """
                {
                  "id": "sample-open",
                  "name": "Sample",
                  "series_status": "open",
                  "no_issue_years": [2001, 2020],
                  "no_issue_note": "La ceca saltó esos años.",
                  "members": [
                    {"id": "1993-a", "year": 1993},
                    {"id": "2026-b", "year": 2026}
                  ]
                }
                """,
                encoding="utf-8",
            )
            catalogs = stale.load_open_catalogs(pathlib.Path(tmp))
            self.assertEqual(len(catalogs), 1)
            self.assertEqual(catalogs[0].no_issue_years, frozenset({2001, 2020}))

    def test_load_rejects_no_issue_years_without_note(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            path = pathlib.Path(tmp) / "broken.json"
            path.write_text(
                """
                {
                  "id": "broken-open",
                  "name": "Broken",
                  "series_status": "open",
                  "no_issue_years": [2001],
                  "members": [{"id": "1993-a", "year": 1993}, {"id": "2026-b", "year": 2026}]
                }
                """,
                encoding="utf-8",
            )
            with self.assertRaises(SystemExit):
                stale.load_open_catalogs(pathlib.Path(tmp))


if __name__ == "__main__":
    unittest.main()
