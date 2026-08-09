# El nombre de la casilla, y los años de una fila en una línea

Primero de los dos PR del bloque 6 del ADR 0026 ([#337](https://github.com/jenarvaezg/coindex/issues/337)),
el que va **antes del banco**: no depende del giro ni de los 420 ms, y la chapa
hundida del año necesita saber cuánto alto se lleva el nombre para calcular su
rebaje.

Medido el 9 de agosto de 2026 en el AVD `coindex-ux` (Pixel 7, Android 36,
1080 × 2400 px, 420 dpi), con las animaciones del sistema desactivadas y la
captura de la colección del padre del 5 de agosto sembrada por SQL —69
colecciones, 575 monedas, 193 tipos— más una moneda rusa añadida a mano, que es
lo que hace visible en el índice la lámina con la que el ticket manda medir.

## Los años de una fila caían en tres líneas distintas

La celda de la lámina era la única de las tres superficies que imprimen un
nombre bajo un hueco sin tratar: `titleMedium` sin autoajuste, sin `maxLines` y
sin altura reservada, así que la altura de la celda la decidía el nombre más
largo de la fila.

Recorriendo entera la lámina de **Monumentos arquitectónicos de Rusia · 3
rublos** —103 casillas, 32 filas completas de tres— y comparando la `y` de los
tres años de cada fila:

| lámina de Rusia, 32 filas de tres | antes | después |
| --- | ---: | ---: |
| filas con los tres años a la misma altura | 7 | **32** |
| filas con desnivel | **25** | **0** |
| peor desnivel de una fila | **224 px · 85 dp** | **0** |

| antes | después |
| --- | --- |
| ![La lámina de Rusia con los nombres a su aire](rusia-antes.png) | ![La misma lámina con la altura del nombre reservada](rusia-despues.png) |

## Lo que se hace, que es lo que ya hacían las otras dos superficies

1. **Altura reservada**: la caja del nombre mide siempre dos líneas de
   `titleMedium` más su aire —21 sp × 2 + 6 dp × 2—, y el nombre se cuelga del
   hueco por arriba. Reservada **en dp y no en `minLines`**: el autoajuste
   descubrió el segundo defecto, porque dos líneas a 13 sp son más bajas que dos
   líneas a 17 sp, y con `minLines = 2` los años de una fila con nombres
   autoajustados y sin autoajustar seguían separándose 13 px.
2. **Autoajuste 17 → 13 sp** antes de truncar, la misma escalera que baja la
   tarjeta del índice ([#348](https://github.com/jenarvaezg/coindex/issues/348)).
3. **Elipsis** cuando ni a 13 sp entra. Con 73 caracteres no hay autoajuste que
   valga, y un corte que se ve es mejor que siete líneas.

| superficie | tratamiento |
| --- | --- |
| tarjeta del índice | autoajuste 17→13 sp, altura fija, elipsis (#348) |
| cartela de Monedas | autoajuste 12→8 sp, elipsis en el tema (#350) |
| celda de la lámina | **las tres cosas, aquí** |

El `label` es del curador y no se toca: 1.086 de los 1.188 miembros de `data/`
no son un año, y muchos son descripciones legítimas de la emisión. Lo que cede
es la tipografía en pantalla. Un nombre cortado **sigue entero en accesibilidad
y en la búsqueda**, y una prueba instrumentada lo fija.

## `1 Venezolano` sigue en dos líneas, y es correcto que siga

![Fuertes antes](fuertes-antes.png) ![Fuertes después](fuertes-despues.png)

En `Fuertes` la casilla de 1876 es la única de las 22 cuyo `label` no es un año.
`1 Venezolano ↗` cabe en dos líneas a 17 sp, así que el autoajuste no tiene
motivo para reducirlo y la celda lo parte en `1` / `Venezolano ↗` igual que
antes. Lo que la arregla es el **punto 2 del bloque**, que se lleva el enlace del
título al año: sin el `↗` —un espacio duro más un glifo de `0.85.em`, unos 19 dp
al 17 % de la celda— el nombre entra en una línea **sin lógica de presupuesto de
glifo**, que es exactamente la razón por la que el #361 se fusionó aquí en vez de
escribirse aparte.

Lo que sí cambia en `Fuertes` es que las casillas de una fila ya no miden
distinto por culpa de la primera.

## Lo que no se toca

- **La celda sigue en 104 dp de hueco y tres columnas.** Ensancharla es la
  palanca que el [#338](https://github.com/jenarvaezg/coindex/issues/338)
  prohíbe expresamente para el brillo, y aquí vale lo mismo.
- **El papel se mide aparte**: la celda del PNG y la del PDF tienen otra anchura
  y otro presupuesto, y no pasan por `PlateCellName`.
- **La tarjeta del índice** comparte la forma `minLines` + autoajuste que aquí se
  quedó corta, pero hoy no lo exhibe: en las 68 tarjetas del #348 sólo una
  llegaba a autoajustarse. Queda anotado, no arreglado de paso.
