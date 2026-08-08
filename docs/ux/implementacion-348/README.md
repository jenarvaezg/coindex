# Nombres de colección completos y fracciones alineadas

Medido el 9 de agosto de 2026 en el AVD `coindex-ux` (Pixel 7, Android 36,
1080 × 2400 px, 420 dpi), con las animaciones del sistema desactivadas y la
captura local del padre que pide el ticket: 229 filas, 572 monedas, 191 tipos y
68 colecciones con los catálogos que viajan hoy en el repositorio. La misma
captura producía 47 cuando se redactó el ticket; los datos curados añadidos
después explican el recuento actual.

La cartela conserva las tres columnas del ADR 0026 y reserva dos líneas para
todos los nombres. Bitter se autoajusta de 17 a 13 sp antes de cortar; si ni a
13 sp cabe, Compose pinta una elipsis. La pasada completa por las 68 tarjetas
dio este resultado:

| resultado | tarjetas |
| --- | ---: |
| caben tras el autoajuste | 67 |
| terminan en elipsis | 1 |

La única elipsis corresponde al nombre derivado `1190e anniversaire du
couronnement de Charlemagne (800-1990).`; no hay un `short_name` curado que
acortar. Separar la cola tras `·` o retocar los catálogos no mejora la medida,
así que no se usan esas dos palancas.

[`despues.png`](despues.png) mezcla nombres de una y dos líneas. Las tres
fracciones de cada fila comparten altura porque la cartela reserva siempre las
dos líneas. [`elipsis-despues.png`](elipsis-despues.png) muestra el único corte
y su elipsis visible. El volcado de accesibilidad de esa búsqueda conserva el
texto entero, por lo que la búsqueda sigue indexando el nombre completo.

La captura anterior aprobada en el bloque 3 está en
[`../implementacion-334/despues.png`](../implementacion-334/despues.png): allí
`The Royal Tudor Beasts 2 oz` se veía como `The Royal Tudor Beasts 2`, sin señal
de que faltaba texto.
