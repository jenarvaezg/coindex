#!/usr/bin/env python3
"""Busca la costura del anillo de un hueco troquelado, sobre una captura del AVD a 1080.

Es el recuento que pide el plan de prueba del #357. Un arco de `sweepAngle = 180f` acaba de
golpe, así que su costura es un **salto de luminancia entre dos muestras contiguas** justo a
las 3 y a las 9 en punto, que es donde terminan los dos arcos.

    python3 medir-anillo.py colecciones-despues.png
    python3 medir-anillo.py captura.png --centro 540 698

El número no depende de acertar el radio: se barre toda la banda del anillo en pasos de 0,25
dp y se informa del **peor** salto que se encuentra. Eso importa porque la geometría cambió
entre las dos versiones —el anillo opaco de la v0.18.6 sobresalía 1,5 dp del borde del
cartón y la pared del #357 no sobresale nada—, así que un radio fijo mediría cosas distintas
en cada captura.

El hueco se localiza por el filete, que es `Paper.hairline` opaco (`#878577`) y por tanto un
color exacto en el PNG. Se toman los extremos **horizontales** del filete, que es donde los
arcos terminan y por tanto donde ninguno de los dos tapa al otro; con el borde superior
—que ningún arco pálido cubre— se cierra el centro.
"""

import argparse
import math
import statistics

from PIL import Image

# 420 dpi del Pixel 7 de las capturas: 1 dp = 2,625 px.
DENSITY = 2.625
HAIRLINE = (135, 133, 119)


def luminance(pixel):
    red, green, blue = pixel[:3]
    return 0.2126 * red + 0.7152 * green + 0.0722 * blue


def sample(image, centre_x, centre_y, radius, degrees):
    """Luminancia interpolada en un punto del anillo.

    0° son las 3 en punto y crecen en el sentido del reloj, como los ángulos de Compose.
    """
    angle = math.radians(degrees)
    x = centre_x + radius * math.cos(angle)
    y = centre_y + radius * math.sin(angle)
    left, top = int(x), int(y)
    fraction_x, fraction_y = x - left, y - top
    pixels = image.load()
    upper = (
        luminance(pixels[left, top]) * (1 - fraction_x)
        + luminance(pixels[left + 1, top]) * fraction_x
    )
    lower = (
        luminance(pixels[left, top + 1]) * (1 - fraction_x)
        + luminance(pixels[left + 1, top + 1]) * fraction_x
    )
    return upper * (1 - fraction_y) + lower * fraction_y


def find_hole(image, window=None, reach=200):
    """Centro y borde exterior del anillo, por los píxeles exactos del filete."""
    pixels = image.load()
    width, height = image.size
    if window:
        centre_x, centre_y = window
        span = range(max(0, centre_y - reach), min(height, centre_y + reach))
        columns = range(max(0, centre_x - reach), min(width, centre_x + reach))
    else:
        span, columns = range(height), range(width)
    hits = [(x, y) for y in span for x in columns if pixels[x, y] == HAIRLINE]
    if not hits:
        raise SystemExit("no se encuentra el filete del hueco")
    xs = [x for x, _ in hits]
    left, right = min(xs), max(xs)
    radius = (right - left) / 2
    top = min(y for _, y in hits)
    return (left + right) / 2, top + radius, radius


def seam(image, centre_x, centre_y, radius):
    """Salto de luminancia en los 2° que abrazan las 3 y las 9 en punto."""
    at = lambda degrees: sample(image, centre_x, centre_y, radius, degrees)
    return abs(at(359) - at(1)), abs(at(179) - at(181))


def report(path, window, reach):
    image = Image.open(path).convert("RGB")
    centre_x, centre_y, edge = find_hole(image, window, reach)
    print(f"\n{path}")
    print(f"  centro ({centre_x:.1f}, {centre_y:.1f}), borde exterior del anillo en "
          f"r = {edge:.1f} px, hueco de ~{2 * edge / DENSITY:.0f} dp")

    # De 5 dp por dentro del borde a 2 por fuera: el ancho del anillo en las dos versiones,
    # y ni un dp más adentro, que ahí ya empieza la fotografía.
    worst = max(
        (seam(image, centre_x, centre_y, edge + step / 4 * DENSITY), step / 4)
        for step in range(-20, 9)
    )
    (three, nine), offset = worst
    print(f"  peor costura de toda la banda: {three:.1f} niveles a las 3 y {nine:.1f} a las 9, "
          f"en r{offset:+.2f} dp")

    middle = edge - 2.5 * DENSITY
    values = {
        step / 2: sample(image, centre_x, centre_y, middle, step / 2)
        for step in range(720)
    }
    sides = [v for angle, v in values.items()
             if angle <= 20 or angle >= 340 or 160 <= angle <= 200]
    steps = [abs(values[(i + 1) / 2] - values[i / 2]) for i in range(719)]
    print(f"  a r−2,50 dp: arriba = {values[270.0]:.1f}, abajo = {values[90.0]:.1f}, "
          f"lados = {statistics.mean(sides):.1f} (amplitud {max(sides) - min(sides):.1f}), "
          f"peor salto de 0,5° = {max(steps):.1f}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("capturas", nargs="+")
    parser.add_argument("--centro", nargs=2, type=int, metavar=("X", "Y"),
                        help="centro aproximado, si hay más de un hueco en la captura")
    parser.add_argument("--alcance", type=int, default=200,
                        help="medio lado de la ventana de búsqueda en px (por defecto 200); "
                             "el hueco de 166 dp del banco necesita 240")
    arguments = parser.parse_args()
    for capture in arguments.capturas:
        report(capture, arguments.centro, arguments.alcance)
