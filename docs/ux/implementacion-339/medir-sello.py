#!/usr/bin/env python3
"""Mide el sello de completado sobre una captura del AVD a resolución nativa.

Dos preguntas, que son las dos que el #339 dejó abiertas para el banco:

1. **¿El `multiply` deja pasar el papel?** Un sello opaco pintaría el trazo de un
   color plano; uno multiplicado deja el grano del papel debajo, atenuado por el
   mismo factor que la luminancia. Se compara el detalle de alta frecuencia dentro
   del trazo con el del papel vacío de al lado, y se predice el primero a partir
   del segundo.
2. **¿Cuánto ocupa en la hoja?** La caja envolvente de la tinta, en dp. No es el
   rectángulo declarado —mide los píxeles que pasan el umbral, y el segundo marco
   asoma por dos lados— así que sirve para contrastar el orden de magnitud contra
   los 84 × 76 dp del #304, no para deducirlos.

    python3 medir-sello.py lamina-completa.png --sello 760 430 1070 740 \
        --papel 990 760 1070 900

Las ventanas van en píxeles de la captura nativa (1080 × 2400 a 420 dpi), nunca
sobre un pantallazo reescalado.
"""

from __future__ import annotations

import argparse

import numpy as np
from PIL import Image, ImageFilter

# 420 dpi del Pixel 7 del AVD: 2,625 px por dp.
DEFAULT_DPI = 420.0

# El óxido del sello contra el papel: el canal rojo se despega del azul.
INK_THRESHOLD = 45


def window(shape, box):
    mask = np.zeros(shape, bool)
    x0, y0, x1, y1 = box
    mask[y0:y1, x0:x1] = True
    return mask


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("capture")
    parser.add_argument("--sello", nargs=4, type=int, required=True, metavar=("X0", "Y0", "X1", "Y1"))
    parser.add_argument("--papel", nargs=4, type=int, required=True, metavar=("X0", "Y0", "X1", "Y1"))
    parser.add_argument("--dpi", type=float, default=DEFAULT_DPI)
    args = parser.parse_args()

    image = Image.open(args.capture).convert("RGB")
    pixels = np.asarray(image, dtype=np.float32)
    # El grano es alta frecuencia: lo que queda al restarle a la imagen su propio desenfoque.
    detail = (pixels - np.asarray(image.filter(ImageFilter.GaussianBlur(3)), dtype=np.float32))[:, :, 0]

    ink = (pixels[:, :, 0] - pixels[:, :, 2]) > INK_THRESHOLD
    stroke = ink & window(ink.shape, args.sello)
    paper = window(ink.shape, args.papel)

    ys, xs = np.nonzero(stroke)
    per_dp = args.dpi / 160.0
    width, height = (xs.max() - xs.min() + 1) / per_dp, (ys.max() - ys.min() + 1) / per_dp
    luminance = pixels[:, :, 0][stroke].mean() / pixels[:, :, 0][paper].mean()
    print(f"caja del sello        {width:.1f} × {height:.1f} dp")
    print(f"tinta / papel         {luminance:.3f}")
    print(f"grano del papel       σ {detail[paper].std():.2f}")
    print(f"grano bajo la tinta   σ {detail[stroke].std():.2f}")
    print(f"  predicho si multiplica  σ {detail[paper].std() * luminance:.2f}")
    print(f"  predicho si es opaco    σ 0,00")


if __name__ == "__main__":
    main()
