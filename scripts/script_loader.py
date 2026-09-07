#!/usr/bin/env python3
"""Carga por ruta un guion de `scripts/`, que lleva guion en el nombre y no se importa.

`weight-deviations.py` y sus hermanos no son módulos importables —el guion del nombre lo
impide—, así que sus tests los cargan por ruta. La carga vive aquí porque son cuatro y
necesitan lo mismo: `scripts/` en el `sys.path`, porque los guiones se importan entre sí
(`repo_issue`).
"""

from __future__ import annotations

import importlib.util
import pathlib
import sys
from types import ModuleType

SCRIPTS = pathlib.Path(__file__).resolve().parent


def load_script(module_name: str, file_name: str) -> ModuleType:
    """El guion `file_name` cargado como `module_name`."""
    if str(SCRIPTS) not in sys.path:
        sys.path.insert(0, str(SCRIPTS))
    spec = importlib.util.spec_from_file_location(module_name, SCRIPTS / file_name)
    assert spec is not None and spec.loader is not None
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module
