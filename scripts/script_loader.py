#!/usr/bin/env python3
"""Carga por ruta los scripts de `scripts/` para sus tests.

El guion del nombre (`weight-deviations.py`) impide importarlos. Pone `scripts/` en `sys.path`
porque los scripts se importan entre sí (`repo_issue`).
"""

from __future__ import annotations

import importlib.util
import pathlib
import sys
from types import ModuleType

SCRIPTS = pathlib.Path(__file__).resolve().parent


def load_script(module_name: str, file_name: str) -> ModuleType:
    """Carga `scripts/<file_name>` como el módulo `module_name`."""
    if str(SCRIPTS) not in sys.path:
        sys.path.insert(0, str(SCRIPTS))
    spec = importlib.util.spec_from_file_location(module_name, SCRIPTS / file_name)
    assert spec is not None and spec.loader is not None
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module
