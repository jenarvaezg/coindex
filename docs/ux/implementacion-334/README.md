# Implementación del bloque 3 · Índice como hoja de álbum

Medido el 8 de agosto de 2026 en el AVD `coindex-ux` (Pixel 7, Android 36,
1080 × 2400 px, 420 dpi), con las animaciones del sistema desactivadas y a
escala 1:1.

| medida | antes | después |
| --- | ---: | ---: |
| columnas del índice | 1 | 3 |
| colecciones por pantalla | 2,07 | 11,04 |
| palabras de mobiliario en el primer pliegue | 56 | 22 |
| fotos de moneda en el índice | 0 | 1 por colección |

La llegada se obtiene del `uiautomator dump`: el primer hueco empieza en
`y=531`, el paso medido de una fila es 457 px y el pliegue termina en `y=2211`.
Son 3,68 filas de tres huecos. El AVD conservaba cinco colecciones en esta
corrida, así que la capacidad se midió por la geometría de la rejilla, no
inventando filas que no estaban en el teléfono.

Las 22 palabras de mobiliario son el canto (`COINDEX`, tres recuentos y hora),
`Buscar`, `Filtros y orden`, el recuento vivo, `Exportar N láminas` y las dos
celdas de la barra inferior. Ninguna tarjeta aporta mobiliario: sólo imprime su
nombre y la fracción. El volcado no contiene eyebrow de país ni línea de
variante.

El mosaico de fibra a opacidad 0,08 se distingue en la captura a 1:1, por lo
que se conserva. El techo mensual y su medidor tampoco aparecen en el volcado
de Ajustes.

- [`antes.png`](antes.png): índice heredado del bloque 2.
- [`despues.png`](despues.png): canto, regleta y huecos troquelados.
- [`ajustes-despues.png`](ajustes-despues.png): credenciales sin presupuesto.
