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

La llegada sale del `uiautomator dump`: el primer hueco empieza en `y=531`, el
paso de una fila es 457 px y el pliegue termina en `y=2211`, así que caben 3,68
filas de tres huecos. El AVD sólo tenía cinco colecciones, de modo que la
capacidad se calculó con la geometría de la rejilla.

Las 22 palabras de mobiliario son el canto (`COINDEX`, tres recuentos y hora),
`Buscar`, `Filtros y orden`, el recuento vivo, `Exportar N láminas` y las dos
celdas de la barra inferior. Las tarjetas sólo imprimen su nombre y la
fracción; el volcado no contiene eyebrow de país ni línea de variante.

El mosaico de fibra a opacidad 0,08 no se distingue a 1:1: sobre `despues.png`,
en una región de 920 × 600 px de papel vacío, el grano toca el 4,17 % de los
píxeles con 12 niveles de amplitud y 0,78 de desviación típica, y el mosaico se
repite exacto cada 256 px. Eso cumplía la condición de retirada del
[#300](https://github.com/jenarvaezg/coindex/issues/300); el
[#351](https://github.com/jenarvaezg/coindex/issues/351) lo resolvió subiendo
el grano hasta que se ve (`docs/ux/implementacion-351/`). El techo mensual y su medidor
tampoco aparecen en el volcado de Ajustes.

- [`antes.png`](antes.png): índice heredado del bloque 2.
- [`despues.png`](despues.png): canto, regleta y huecos troquelados.
- [`ajustes-despues.png`](ajustes-despues.png): credenciales sin presupuesto.
