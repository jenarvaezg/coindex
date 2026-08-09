#!/usr/bin/env python3
"""Mide el brillo de una casilla sobre capturas del AVD a resolución nativa.

Todo se mide por el eje del propio gradiente —105°, el ángulo de la variante H— y
dentro del disco de la fotografía, nunca sobre el cartón.

    python3 medir-brillo.py --centro 194 698 --radio 123 \\
        --reposo colecciones-h-reposo.png \\
        --izquierda colecciones-h-izquierda.png \\
        --derecha colecciones-h-derecha.png \\
        --base colecciones-antes.png

Las tres cifras que decidieron el calibrado:

* **recorrido**: amplitud pico a pico del perfil de la *diferencia* entre las dos
  poses extremas. Es cuánto mueve el efecto la superficie al inclinar el móvil.
* **señal contra la hoja de hoy**: diferencia media y máxima entre la casilla de
  hoy y la misma casilla en reposo con el brillo puesto.
* **ruido de la propia foto**: amplitud pico a pico del perfil de luminancia de la
  fotografía por ese mismo eje. Es la vara con la que se compara la señal, y cambia
  con la moneda: el #338 midió 162 niveles sobre el 1 Bolívar de
  `implementacion-336/monedas.png` y aquí salen 91,9 sobre la casilla de los
  Fuertes. Lo que no cambia es el término de comparación — el reflejo fijo del
  acetato movía 13.
"""

import argparse
import math

import numpy as np
from PIL import Image

ANGLE_DEGREES = 105.0


def load(path):
    return np.asarray(Image.open(path).convert("L"), dtype=float)


def disc(shape, centre, radius):
    """Máscara del disco de la fotografía, con dos píxeles de margen por el borde."""
    ys, xs = np.ogrid[: shape[0], : shape[1]]
    return (xs - centre[0]) ** 2 + (ys - centre[1]) ** 2 <= (radius - 2) ** 2


def profile(image, centre, radius, bins=48):
    """Luminancia media por bandas perpendiculares al eje del gradiente."""
    ys, xs = np.nonzero(disc(image.shape, centre, radius))
    angle = math.radians(ANGLE_DEGREES)
    axis = (xs - centre[0]) * math.cos(angle) + (ys - centre[1]) * math.sin(angle)
    edges = np.linspace(-radius, radius, bins + 1)
    index = np.clip(np.digitize(axis, edges) - 1, 0, bins - 1)
    values = image[ys, xs]
    return np.array(
        [values[index == b].mean() if (index == b).any() else np.nan for b in range(bins)]
    )


def peak_to_peak(values):
    values = values[~np.isnan(values)]
    return float(values.max() - values.min())


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--centro", nargs=2, type=int, required=True)
    parser.add_argument("--radio", type=int, required=True)
    parser.add_argument("--reposo", required=True)
    parser.add_argument("--izquierda")
    parser.add_argument("--derecha")
    parser.add_argument("--base", help="la misma casilla antes del brillo")
    parser.add_argument("--etiqueta", default="")
    args = parser.parse_args()

    centre = tuple(args.centro)
    rest = load(args.reposo)
    mask = disc(rest.shape, centre, args.radio)

    print(f"{args.etiqueta or args.reposo}")
    print(f"  ruido de la foto por el eje ....... {peak_to_peak(profile(rest, centre, args.radio)):6.1f}")

    if args.izquierda and args.derecha:
        left = load(args.izquierda)
        right = load(args.derecha)
        swing = profile(right - left, centre, args.radio)
        print(f"  recorrido del brillo (pico a pico)  {peak_to_peak(swing):6.1f}")
        print(f"  el mismo, en el peor píxel ........ {np.abs((right - left)[mask]).max():6.1f}")

    if args.base:
        base = load(args.base)
        delta = (rest - base)[mask]
        print(f"  contra la hoja de hoy · media ..... {np.abs(delta).mean():6.1f}")
        print(f"  contra la hoja de hoy · máximo .... {np.abs(delta).max():6.1f}")
        print(f"  luminancia media, antes → después . {base[mask].mean():6.1f} → {rest[mask].mean():.1f}")
        print(f"  desviación típica, antes → después  {base[mask].std():6.1f} → {rest[mask].std():.1f}")


if __name__ == "__main__":
    main()
