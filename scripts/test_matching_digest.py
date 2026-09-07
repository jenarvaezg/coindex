#!/usr/bin/env python3
"""Afirma las copias de estos guiones contra el digest de la suite Kotlin.

`python3 -m unittest scripts/test_matching_digest.py`

Los dos informes de curación son un puerto a mano del emparejamiento del dominio: la tolerancia
y los pesos comunes del imán, la normalización de familia, la familia técnica, la especie que da
la versión de esquema y la regla de reclamación cruzada que el arranque de la app hace fatal.
Hasta este fichero cada suite probaba su propia copia, así que cambiar una constante sólo en
Kotlin no ponía nada en rojo.

El contrato es `fixtures/matching-digest.json`, que emite `MatchingDigestTest` de `:domain` y
que se versiona. Aquí no se afirma nada contra números escritos a mano: lo que dice el digest es
lo que la app hace, y una copia que no lo siga rompe CI. **El puerto es unidireccional**: si el
digest y la copia discrepan, lo que se corrige es la copia.

Donde el juez de la curación es más severo que la compuerta de arranque, la diferencia va
declarada en `STRICTER_THAN_THE_RUNTIME` y en ningún otro sitio.
"""

from __future__ import annotations

import json
import pathlib
import sys
import tempfile
import unittest

ROOT = pathlib.Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / "scripts"))

from script_loader import load_script  # noqa: E402

DIGEST = ROOT / "fixtures" / "matching-digest.json"

#: La forma del digest que esta suite sabe leer. Si `MatchingDigestTest` la sube, esto falla
#: diciéndolo en vez de afirmar sobre campos que ya no existen.
DIGEST_VERSION = 1

#: Lo que el digest trae y esta suite lee.
SECTIONS = ("snapping", "normalized_weights", "families", "catalog_species", "cross_claims")

#: Los casos donde el juez de la curación para y el arranque de la app no.
#:
#: Responden a preguntas distintas: `parseAll` deja los conjuntos fuera del cruce porque un
#: conjunto no es la tarjeta de la denominación (ADR 0012), y `type-claims.py` para igual porque
#: lo que juzga es si el fichero puede versionarse — el conjunto gana la familia y se llevaría la
#: moneda de la otra tarjeta. La lista está aquí para que esa severidad de más se declare y no se
#: herede sin darse cuenta: un caso nuevo donde discrepen rompe este test.
STRICTER_THAN_THE_RUNTIME = {"set_beside_catalog"}

weights = load_script("weight_deviations", "weight-deviations.py")
claims = load_script("type_claims", "type-claims.py")
digest = json.loads(DIGEST.read_text(encoding="utf-8"))


class DigestShapeTests(unittest.TestCase):
    def test_the_digest_has_the_shape_this_suite_reads(self) -> None:
        self.assertEqual(DIGEST_VERSION, digest["digest_version"])
        for section in SECTIONS:
            with self.subTest(section=section):
                self.assertTrue(digest[section], f"el digest trae `{section}` vacío")


class SnappingTests(unittest.TestCase):
    def setUp(self) -> None:
        self.snapping = digest["snapping"]

    def test_the_copied_constants_are_the_ones_the_app_uses(self) -> None:
        self.assertEqual(self.snapping["grams_per_troy_ounce"], weights.GRAMS_PER_TROY_OUNCE)
        self.assertEqual(
            tuple(self.snapping["common_weights_millioz"]), weights.COMMON_WEIGHTS_MILLIOZ
        )
        self.assertEqual(self.snapping["snap_tolerance_millioz"], weights.SNAP_TOLERANCE_MILLIOZ)

    def test_every_gram_vector_normalizes_the_same(self) -> None:
        # Por el mismo camino que el informe: la ficha convierte a onzas y el imán decide.
        for vector in digest["normalized_weights"]:
            with self.subTest(grams=vector["grams"]):
                ficha = weights.Ficha(
                    numista_type_id=1,
                    title=None,
                    series=None,
                    grams=vector["grams"],
                    composition=None,
                )
                measured = (
                    None
                    if ficha.weight_oz is None
                    else weights.normalize_weight_millioz(ficha.weight_oz)
                )
                self.assertEqual(vector["millioz"], measured)


class FamilyTests(unittest.TestCase):
    def test_every_family_vector_normalizes_the_same(self) -> None:
        for vector in digest["families"]:
            with self.subTest(family=vector["family"]):
                self.assertEqual(
                    vector["normalized"], weights.normalize_family(vector["family"])
                )

    def test_every_family_vector_is_technical_the_same(self) -> None:
        for vector in digest["families"]:
            with self.subTest(family=vector["family"]):
                self.assertEqual(
                    vector["technical"], weights.is_technical_family(vector["family"])
                )


class CatalogSpeciesTests(unittest.TestCase):
    """La única especie que las copias leen es el conjunto, y la lee cada una a su manera."""

    def setUp(self) -> None:
        self.species = digest["catalog_species"]
        self.sets = [
            one["schema_version"] for one in self.species if one["species"] == "set"
        ]

    def test_the_set_schema_version_is_the_one_the_app_calls_a_set(self) -> None:
        self.assertEqual(self.sets, [claims.SET_SCHEMA_VERSION])

    def test_the_report_reads_a_set_out_of_the_same_schema_version(self) -> None:
        # `load_catalogs` lleva la regla incrustada, así que se le pregunta con ficheros.
        with tempfile.TemporaryDirectory() as raw:
            directory = pathlib.Path(raw)
            for one in self.species:
                version = one["schema_version"]
                (directory / f"schema-{version}.json").write_text(
                    json.dumps({"id": f"schema-{version}", "schema_version": version}),
                    encoding="utf-8",
                )
            loaded = {
                catalog.catalog_id: catalog.is_set
                for catalog in weights.load_catalogs(directory)
            }

        self.assertEqual(
            {
                f"schema-{one['schema_version']}": one["species"] == "set"
                for one in self.species
            },
            loaded,
        )


class CrossClaimTests(unittest.TestCase):
    """El juez de la curación contra la compuerta de arranque, caso a caso.

    Lo que se exige es una sola dirección: donde la app se niega a arrancar, el juez para. Puede
    parar además donde la app arranca —para eso está `STRICTER_THAN_THE_RUNTIME`—, pero nunca
    dar por bueno lo que el arranque rechaza, que es justo lo que este informe evita.
    """

    def stops_on(self, case: dict) -> bool:
        with tempfile.TemporaryDirectory() as raw:
            data = pathlib.Path(raw)
            (data / "collection-catalogs").mkdir()
            for payload in case["catalogs"]:
                (data / "collection-catalogs" / f"{payload['id']}.json").write_text(
                    json.dumps(payload), encoding="utf-8"
                )
            verdicts = claims.audit(claims.load_claims(data))
        return any(verdict.stop for verdict in verdicts)

    def test_the_judge_stops_wherever_the_app_refuses_to_start(self) -> None:
        for case in digest["cross_claims"]:
            if not case["rejected"]:
                continue
            with self.subTest(case=case["case"]):
                self.assertTrue(
                    self.stops_on(case),
                    f"la app no arranca con {case['reads']} y el juez lo da por bueno",
                )

    def test_the_extra_strictness_of_the_judge_is_the_declared_one(self) -> None:
        stricter = {
            case["case"]
            for case in digest["cross_claims"]
            if not case["rejected"] and self.stops_on(case)
        }
        self.assertEqual(STRICTER_THAN_THE_RUNTIME, stricter)


if __name__ == "__main__":
    unittest.main()
