#!/usr/bin/env python3
"""Tests de scripts/weight-deviations.py — `python3 -m unittest scripts/test_weight_deviations.py`."""

from __future__ import annotations

import importlib.util
import pathlib
import sys
import tempfile
import unittest
from datetime import date

ROOT = pathlib.Path(__file__).resolve().parent.parent
SCRIPT = ROOT / "scripts" / "weight-deviations.py"


def load_weights():
    # El guion del nombre impide importarlo, y `repo_issue` vive al lado.
    sys.path.insert(0, str(ROOT / "scripts"))
    spec = importlib.util.spec_from_file_location("weight_deviations", SCRIPT)
    assert spec is not None and spec.loader is not None
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


weights = load_weights()

GRAMS_PER_TROY_OUNCE = weights.GRAMS_PER_TROY_OUNCE


def member(
    member_id: str,
    type_id: int | None,
    *,
    label: str | None = None,
    variant_note: str | None = None,
    year: int | None = 2024,
):
    return weights.Member(
        member_id=member_id,
        label=label,
        year=year,
        numista_type_id=type_id,
        variant_note=variant_note,
    )


def catalog(
    catalog_id: str,
    weight_millioz: int | None,
    *members,
    is_set: bool = False,
    source_note: str | None = None,
):
    return weights.Catalog(
        catalog_id=catalog_id,
        name=catalog_id,
        weight_millioz=weight_millioz,
        members=tuple(members),
        is_set=is_set,
        source_note=source_note,
    )


def ficha(type_id: int, grams: float | None, *, series: str | None = None, title: str = "Moneda"):
    return weights.Ficha(
        numista_type_id=type_id,
        title=title,
        series=series,
        grams=grams,
        composition="Plata 925",
    )


def report_for(catalogs, fichas, groupings=None):
    return weights.build_report(
        catalogs,
        groupings or {},
        {item.numista_type_id: item for item in fichas},
        as_of=date(2026, 8, 5),
    )


class NormalizeWeightTests(unittest.TestCase):
    def test_snaps_to_the_ounce_but_not_thirty_grams(self) -> None:
        ounce = weights.normalize_weight_millioz(31.1 / GRAMS_PER_TROY_OUNCE * 0 + 1.0)
        self.assertEqual(1_000, ounce)
        near = weights.normalize_weight_millioz(30.0 / GRAMS_PER_TROY_OUNCE)
        self.assertEqual(965, near)

    def test_a_declared_weight_is_not_a_target(self) -> None:
        # Los 13,96 g del Porto se quedan en 449 aunque su catálogo declare 450: lo que junta
        # a los siete hermanos es el fichero que los nombra, no el imán (#288).
        self.assertEqual(449, weights.normalize_weight_millioz(13.96 / GRAMS_PER_TROY_OUNCE))
        self.assertEqual(450, weights.normalize_weight_millioz(14.0 / GRAMS_PER_TROY_OUNCE))
        # 26,73 g son el peso legal del Morgan dollar y nada los mueve al 868 de un ajeno.
        self.assertEqual(859, weights.normalize_weight_millioz(26.73 / GRAMS_PER_TROY_OUNCE))

    def test_only_the_common_weights_pull_and_only_within_tolerance(self) -> None:
        # Diez milionzas entran y once no. Dos pesos comunes no pueden estar los dos en
        # tolerancia —el más cercano son 250 milionzas—, así que el desempate no se alcanza.
        self.assertEqual(1_000, weights.normalize_weight_millioz(1_010 / 1_000))
        self.assertEqual(1_011, weights.normalize_weight_millioz(1_011 / 1_000))
        self.assertEqual(1_000, weights.normalize_weight_millioz(990 / 1_000))
        self.assertEqual(989, weights.normalize_weight_millioz(989 / 1_000))

    def test_rejects_what_is_not_a_weight(self) -> None:
        self.assertIsNone(weights.normalize_weight_millioz(0.0))
        self.assertIsNone(weights.normalize_weight_millioz(-1.0))
        self.assertIsNone(weights.normalize_weight_millioz(float("inf")))


class FamilyTests(unittest.TestCase):
    def test_a_technical_system_yields_to_a_grouping(self) -> None:
        technical = ficha(1, 31.1, series="System 1998-2024")
        self.assertEqual(
            "Onzas sueltas",
            weights.derived_family(technical, {1: "Onzas sueltas"}),
        )
        self.assertEqual("System 1998-2024", weights.derived_family(technical, {}))

    def test_a_real_series_outranks_the_grouping(self) -> None:
        real = ficha(2, 31.1, series="  Lunar   ounce ")
        self.assertEqual("Lunar ounce", weights.derived_family(real, {2: "Agrupación"}))

    def test_no_series_and_no_grouping_is_no_family(self) -> None:
        self.assertIsNone(weights.derived_family(ficha(3, 31.1), {}))


class DeviationTests(unittest.TestCase):
    def test_a_member_whose_grams_land_elsewhere_is_reported(self) -> None:
        report = report_for(
            [catalog("monumentos", 1_121, member("2017-puente", 117_328))],
            [ficha(117_328, 39.94)],
        )
        self.assertEqual(1, len(report.deviations))
        deviation = report.deviations[0]
        self.assertEqual(1_121, deviation.declared_millioz)
        self.assertEqual(1_284, deviation.observed_millioz)
        self.assertAlmostEqual(14.54, deviation.relative_percent, places=2)

    def test_the_declared_weight_is_its_own_magnet(self) -> None:
        # A menos de diez milésimas del declarado el imán ya lo pega: no hay desviación.
        report = report_for(
            [catalog("britannia", 1_000, member("2024", 42))],
            [ficha(42, 31.21)],
        )
        self.assertEqual((), report.deviations)

    def test_a_set_declares_no_weight_so_nothing_can_contradict_it(self) -> None:
        report = report_for(
            [catalog("exposicion-1983", None, member("500-escudos", 22_178), is_set=True)],
            [ficha(22_178, 7.0)],
        )
        self.assertEqual((), report.deviations)

    def test_an_unseeded_or_weightless_ficha_says_nothing(self) -> None:
        report = report_for(
            [
                catalog(
                    "lamina",
                    1_000,
                    member("sin-ficha", 99),
                    member("sin-peso", 100),
                    member("sin-tipo", None),
                )
            ],
            [ficha(100, None)],
        )
        self.assertEqual((), report.deviations)

    def test_ordered_by_relative_distance_and_a_note_does_not_silence(self) -> None:
        report = report_for(
            [
                catalog(
                    "portugal-20-escudos-plata",
                    675,
                    member("1966-puente", 6_580, variant_note="Módulo reducido de 1966."),
                    member("1986-otra", 6_581),
                )
            ],
            [ficha(6_580, 10.12), ficha(6_581, 22.0)],
        )
        self.assertEqual(
            ["1966-puente", "1986-otra"],
            [deviation.member_id for deviation in report.deviations],
        )
        self.assertIsNotNone(report.deviations[0].variant_note)

    def test_the_note_of_the_lamina_explains_every_member_at_once(self) -> None:
        # #204: los pesos de plata 900 del Libro Rojo son una sola ley, y la explica el
        # `source_note` del catálogo. Sin esto, una nota escrita no callaría ni una línea.
        report = report_for(
            [
                catalog(
                    "red-data-book-russia",
                    547,
                    member("1993-markhor", 28_070),
                    member("1994-casarca", 28_075),
                    source_note="Media onza de plata fina en las dos leyes.",
                )
            ],
            [ficha(28_070, 17.44), ficha(28_075, 17.44)],
        )
        self.assertEqual(
            ["lámina", "lámina"],
            [deviation.explained_by for deviation in report.deviations],
        )
        self.assertEqual(2, report.explained_count)
        body = weights.render_markdown(report)
        self.assertIn("| lámina |", body)
        self.assertIn("nota en la lámina", weights.render_plain(report))

    def test_the_note_of_the_casilla_wins_over_the_one_of_the_lamina(self) -> None:
        report = report_for(
            [
                catalog(
                    "monumentos",
                    1_121,
                    member("2009-voronezh", 39_904, variant_note="Lleva oro incrustado."),
                    source_note="Una onza de plata fina en las dos leyes.",
                )
            ],
            [ficha(39_904, 35.66)],
        )
        self.assertEqual("casilla", report.deviations[0].explained_by)

    def test_a_lamina_without_a_note_leaves_the_line_unexplained(self) -> None:
        report = report_for(
            [catalog("lamina", 1_000, member("uno", 1))],
            [ficha(1, 35.0)],
        )
        self.assertIsNone(report.deviations[0].explained_by)
        self.assertEqual(0, report.explained_count)

    def test_a_foreign_catalog_no_longer_writes_the_key(self) -> None:
        # Los 33,94 g rusos miden 1091. Acababan en el 1081 que declara la onza mexicana, así
        # que la desviación contra los 1121 de su propia lámina la terminaba de escribir un
        # catálogo ajeno; desde el #288 la fila dice los gramos de la ficha y nada más.
        report = report_for(
            [
                catalog("monumentos", 1_121, member("2005-kropotkinskaya", 29_017)),
                catalog("mexico-onza-troy-925", 1_081, member("1949", 1_000_002)),
            ],
            [ficha(29_017, 33.94), ficha(1_000_002, 33.625)],
        )
        deviation = report.deviations[0]
        self.assertEqual(1_091, deviation.measured_millioz)
        self.assertEqual(1_091, deviation.observed_millioz)
        self.assertFalse(deviation.magnet_moved)

    def test_a_key_the_magnet_left_alone_says_so(self) -> None:
        report = report_for(
            [catalog("monumentos", 1_121, member("2017-puente", 117_328))],
            [ficha(117_328, 39.94)],
        )
        self.assertFalse(report.deviations[0].magnet_moved)

    def test_the_lines_without_a_note_are_the_only_ones_listed_one_by_one(self) -> None:
        report = report_for(
            [
                catalog(
                    "lamina",
                    1_000,
                    member("explicada", 1, variant_note="La ceca varió los gramos."),
                    member("sin-mirar", 2),
                )
            ],
            [ficha(1, 35.0), ficha(2, 35.0)],
        )
        self.assertEqual(
            ["sin-mirar"], [deviation.member_id for deviation in report.unexplained]
        )
        self.assertEqual(1, len(report.explained_clusters))
        self.assertEqual("casilla", report.explained_clusters[0].first.explained_by)

    def test_the_lines_that_say_the_same_collapse_into_one_cumulo(self) -> None:
        # Los 59 monumentos de 33,94 g repetían 59 veces la misma línea: es un hallazgo.
        report = report_for(
            [
                catalog(
                    "monumentos",
                    1_121,
                    member("2005-uno", 1, year=2005),
                    member("2010-dos", 2, year=2010),
                    member("2012-tres", 3, year=2012),
                    source_note="Una onza de plata fina en las dos leyes.",
                )
            ],
            [ficha(1, 33.94), ficha(2, 33.94), ficha(3, 33.94)],
        )
        self.assertEqual(3, report.explained_count)
        self.assertEqual(1, len(report.explained_clusters))
        cluster = report.explained_clusters[0]
        self.assertEqual(3, cluster.count)
        self.assertEqual("2005-2012", cluster.years)
        self.assertIn("| ×3 |", weights.render_markdown(report))

    def test_two_gramajes_del_mismo_catalogo_no_se_juntan(self) -> None:
        report = report_for(
            [
                catalog(
                    "monumentos",
                    1_121,
                    member("uno", 1),
                    member("otro", 2),
                    source_note="Una onza de plata fina en las dos leyes.",
                )
            ],
            [ficha(1, 33.94), ficha(2, 35.66)],
        )
        self.assertEqual(2, len(report.explained_clusters))
        self.assertEqual([1, 1], [cluster.count for cluster in report.explained_clusters])

    def test_the_refresh_command_names_only_the_lines_without_a_note(self) -> None:
        # La caché no se refresca sola: sin esto, una corrección aceptada sigue saliendo.
        report = report_for(
            [
                catalog(
                    "lamina",
                    1_000,
                    member("explicada", 1, variant_note="La ceca varió los gramos."),
                    member("sin-mirar", 2),
                )
            ],
            [ficha(1, 35.0), ficha(2, 35.0)],
        )
        self.assertEqual((2,), report.refresh_type_ids)
        body = weights.render_markdown(report)
        self.assertIn("seed-type-cache.py --refresh --confirm-live-api 2", body)
        self.assertIn("--refresh", weights.render_plain(report))

    def test_a_report_with_every_line_explained_asks_for_no_refresh(self) -> None:
        report = report_for(
            [
                catalog(
                    "lamina",
                    1_000,
                    member("explicada", 1, variant_note="La ceca varió los gramos."),
                )
            ],
            [ficha(1, 35.0)],
        )
        self.assertEqual((), report.refresh_type_ids)
        self.assertNotIn("--refresh", weights.render_markdown(report))
        self.assertIn("todas las desviaciones tienen su explicación escrita", weights.render_markdown(report))

    def test_buckets_split_at_two_and_five_per_cent(self) -> None:
        # 1000 declarado: 1020 son el 2 % justo, 1050 el 5 % justo y 1051 se pasa.
        report = report_for(
            [
                catalog(
                    "lamina",
                    1_000,
                    member("dos", 1),
                    member("cinco", 2),
                    member("mas", 3),
                )
            ],
            [
                ficha(1, 1_020 * GRAMS_PER_TROY_OUNCE / 1_000),
                ficha(2, 1_050 * GRAMS_PER_TROY_OUNCE / 1_000),
                ficha(3, 1_051 * GRAMS_PER_TROY_OUNCE / 1_000),
            ],
        )
        self.assertEqual((1, 1, 1), report.buckets)
        self.assertEqual(len(report.deviations), sum(report.buckets))


class MagnetPullTests(unittest.TestCase):
    def test_only_types_no_catalog_claims_are_measured(self) -> None:
        report = report_for(
            [catalog("britannia", 1_000, member("2024", 42))],
            [ficha(42, 31.21), ficha(43, 31.21)],
        )
        self.assertEqual([43], [pull.numista_type_id for pull in report.pulls])
        self.assertEqual(1, report.unclaimed_count)

    def test_a_weight_the_magnet_does_not_move_is_not_a_finding(self) -> None:
        report = report_for([], [ficha(43, 25.0)])
        self.assertEqual((), report.pulls)
        self.assertEqual(1, report.unclaimed_count)
        self.assertEqual(0, report.unclaimed_without_weight)

    def test_a_type_without_grams_is_counted_apart(self) -> None:
        report = report_for([], [ficha(43, None)])
        self.assertEqual((), report.pulls)
        self.assertEqual(1, report.unclaimed_count)
        self.assertEqual(1, report.unclaimed_without_weight)

    def test_a_declared_weight_does_not_pull_a_type_its_catalog_does_not_claim(self) -> None:
        # El caso del #288: el Morgan dollar pesa 26,73 g de verdad y ningún catálogo lo
        # reclama, así que el 868 que declara uno ajeno no le toca la clave. Los 31,39 g sí
        # se mueven, porque la onza de bullion es una convención real.
        report = report_for(
            [catalog("dolar-de-plata", 868, member("1921", 1_000_001))],
            [ficha(1_000_001, 27.0), ficha(1_492, 26.73), ficha(192_181, 31.39)],
        )
        by_type = {pull.numista_type_id: pull for pull in report.pulls}
        self.assertNotIn(1_492, by_type)
        self.assertEqual(1_000, by_type[192_181].snapped_millioz)

    def test_ordered_by_how_far_the_magnet_pulled(self) -> None:
        report = report_for(
            [],
            [ficha(1, 31.39), ficha(2, 31.21)],
        )
        self.assertEqual(
            [9, 3],
            [pull.pull_millioz for pull in report.pulls],
        )


class SplitCatalogTests(unittest.TestCase):
    def test_counts_the_cards_the_file_avoids(self) -> None:
        report = report_for(
            [
                catalog(
                    "monumentos",
                    1_121,
                    member("a", 1),
                    member("b", 2),
                    member("c", 3),
                ),
                catalog("britannia", 1_000, member("d", 4)),
            ],
            [
                ficha(1, 34.88),
                ficha(2, 33.94),
                ficha(3, 39.94),
                ficha(4, 31.21),
            ],
        )
        self.assertEqual(["monumentos"], [split.catalog_id for split in report.splits])
        split = report.splits[0]
        self.assertEqual((1_091, 1_121, 1_284), split.keys)
        self.assertEqual(2, split.extra_cards)
        self.assertEqual(2, report.extra_cards)


class RenderTests(unittest.TestCase):
    def test_an_empty_report_renders_and_has_nothing_to_sync(self) -> None:
        report = report_for([], [])
        self.assertFalse(report.has_findings)
        body = weights.render_markdown(report)
        self.assertIn(weights.ISSUE_MARKER, body)
        self.assertEqual(3, body.count("_Ninguno._"))
        self.assertIn("(ninguno)", weights.render_plain(report))

    def test_a_pipe_in_a_composition_does_not_break_the_table(self) -> None:
        self.assertEqual("Plata 925 \\| oro", weights.cell("Plata 925 | oro"))
        self.assertEqual("—", weights.cell(None))
        self.assertEqual("abc…", weights.cell("abcdef", limit=4))


class LoadTests(unittest.TestCase):
    def test_reads_the_pieces_of_a_catalog_it_needs(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            path = pathlib.Path(tmp) / "sample.json"
            path.write_text(
                """
                {
                  "schema_version": 1,
                  "id": "sample",
                  "name": "Sample largo",
                  "short_name": "Sample",
                  "weight_millioz": 1000,
                  "source_note": "La ley cambia y el fino no.",
                  "members": [
                    {"id": "2024-a", "year": 2024, "numista_type_id": 42,
                     "label": "Uno", "variant_note": "Otra ley."}
                  ]
                }
                """,
                encoding="utf-8",
            )
            catalogs = weights.load_catalogs(pathlib.Path(tmp))
            self.assertEqual(1, len(catalogs))
            self.assertEqual("Sample", catalogs[0].name)
            self.assertEqual(1_000, catalogs[0].weight_millioz)
            self.assertFalse(catalogs[0].is_set)
            self.assertEqual("La ley cambia y el fino no.", catalogs[0].source_note)
            self.assertEqual("Otra ley.", catalogs[0].members[0].variant_note)

    def test_schema_version_three_is_a_set(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            path = pathlib.Path(tmp) / "set.json"
            path.write_text(
                '{"schema_version": 3, "id": "conjunto", "name": "Conjunto", "members": []}',
                encoding="utf-8",
            )
            self.assertTrue(weights.load_catalogs(pathlib.Path(tmp))[0].is_set)

    def test_fichas_unescape_the_composition_numista_stores_as_html(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            path = pathlib.Path(tmp) / "cache.json"
            path.write_text(
                """
                {"578835": {"title": "Medalla", "weight": 15.1,
                            "composition": {"text": "Plata 999,9 (&quot;PLATA 1000&quot;)"}}}
                """,
                encoding="utf-8",
            )
            fichas = weights.load_fichas(path)
            self.assertEqual('Plata 999,9 ("PLATA 1000")', fichas[578_835].composition)


class RealDataTests(unittest.TestCase):
    """Sobre `data/` de verdad, sin fijar recuentos: lo que se fija es lo que el informe promete."""

    def setUp(self) -> None:
        self.report = weights.build_report(
            weights.load_catalogs(),
            weights.load_grouping_families(),
            weights.load_fichas(),
            as_of=date(2026, 8, 5),
        )

    def test_every_deviation_really_deviates(self) -> None:
        for deviation in self.report.deviations:
            self.assertNotEqual(deviation.declared_millioz, deviation.observed_millioz)
            self.assertGreater(deviation.relative_percent, 0.0)
        self.assertEqual(len(self.report.deviations), sum(self.report.buckets))

    def test_the_two_laminas_of_two_silver_finenesses_are_explained(self) -> None:
        # Los 1 y 2 rublos rusos son la misma media onza (#204) y los 3 rublos la misma onza
        # (#160): entre los dos son la mayoría del informe, y su nota está escrita.
        explained_laminas = {
            "red-data-book-russia",
            "architectural-monuments-russia-3-roubles",
        }
        for deviation in self.report.deviations:
            if deviation.catalog_id in explained_laminas:
                self.assertIsNotNone(
                    deviation.explained_by,
                    f"{deviation.catalog_id}/{deviation.member_id} sin nota",
                )

    def test_every_pull_moves_the_weight_to_a_common_one(self) -> None:
        for pull in self.report.pulls:
            self.assertNotEqual(pull.measured_millioz, pull.snapped_millioz)
            self.assertLessEqual(pull.pull_millioz, weights.SNAP_TOLERANCE_MILLIOZ)
            self.assertIn(pull.snapped_millioz, weights.COMMON_WEIGHTS_MILLIOZ)

    def test_every_split_would_take_more_than_one_key(self) -> None:
        for split in self.report.splits:
            self.assertGreater(len(split.keys), 1)
            self.assertEqual(len(split.keys) - 1, split.extra_cards)

    def test_the_issue_body_fits_in_a_github_issue(self) -> None:
        body = weights.render_markdown(self.report)
        self.assertIn(weights.ISSUE_MARKER, body)
        self.assertLess(len(body), 65_536)


if __name__ == "__main__":
    unittest.main()
