# Implementación · las dos puertas del índice (#520)

La puerta compuesta del índice se parte en dos filas, y la que nombra las marcas sube a la cabecera de
la hoja con sus casillas dibujadas. Forma elegida por Jose el 17 de agosto de 2026 sobre la maqueta
del prototipo (`docs/ux/prototipo-dos-puertas-520/`), variante **E**, con una enmienda suya al dibujo:
las casillas que buscas **no van en penumbra**.

## Lo que cambia en pantalla

| | antes | después |
| --- | --- | --- |
| cabecera del índice | nada entre el estante y la primera tarjeta | `antes-inicio.jpg` → `despues-inicio.jpg`: **«Lo que busco · 2»** con sus dos monedas |
| pie del índice | «Lo que busco · 2, y otras 20 láminas →», que abría **Explorar** | `antes-pie.jpg` → `despues-pie.jpg`: «Y otras 20 láminas que no coleccionas →» |
| «Lo que busco» | las casillas al 14 % | `antes-lista.jpg` → `despues-lista.jpg`: las monedas enteras |
| buscando | la nota del #515 al pie | `despues-buscando.jpg`: la nota bajo la fila de la cabecera, **una sola vez** |
| nada marcado | — | `despues-sin-marcas.jpg`: la fila no se imprime y la primera vista es la de siempre |
| «Explorar» | su puerta a la lista en la cabeza del estante | `despues-explorar.jpg`: **sigue ahí**, mismo nombre |

«Lo que busco» pasa de **dos puertas y cuatro nombres** a **un tap desde la primera vista**: la fila de
la cabecera abre la lista directamente, sin pasar por Explorar.

## Lo medido en el emulador

Pixel 7, densidad 2,625, la colección del padre sembrada con `scripts/avd-db.sh` (70 tarjetas, 580
piezas, 198 tipos, 2 casillas marcadas). Leído de los `bounds` que `uiautomator` publica, no del
pantallazo:

| | px | dp |
| --- | ---: | ---: |
| la fila de «Lo que busco», con dos monedas | 237 | **90** |
| alto de una celda de tarjeta | 441 | 168 |
| paso entre filas de tarjetas | 457 | 174 |
| pliegue (del canto a la barra) | 2148 | 818 |

De ahí, **la fila cuesta 1,61 tarjetas de la primera vista**: 10,65 visibles sin ella contra 9,04 con
ella. La maqueta había predicho 11,36 → 9,60, o sea un coste de 1,76: **es un 6 % más densa que la app**,
la misma desviación que el #279 midió, y la conclusión no se mueve. Y el coste se paga sólo cuando hay
algo marcado: al cero la fila no existe (`despues-sin-marcas.jpg`).

## Lo que cambia en el código

- **`showcaseDoorLabel(plates)`** sustituye a `annexDoorLabel(wishes, plates)`: un nombre, un destino y
  el cero sin imprimir. La forma compuesta desaparece, y con ella la única etiqueta de la app que
  nombraba dos habitaciones.
- **`wishDoorNote`** es la de #515 mudada de fila, con su porqué reescrito: dos filas cuentan
  poblaciones que el buscador no toca, y la frase se imprime **una** vez, en la de arriba.
- **`wishDoorMoreLabel(rest)`** es «y 4 más»: la fila dibuja tres casillas y cuenta el resto.
- **`IndexScreen`** recibe `wishes: List<DrawnWish>` en vez de un recuento —necesita el tipo y la cara
  de cada casilla para dibujarla— y un `onOpenShowcase` propio. `AnnexDoor` gana una ranura de
  contenido: **un dibujo para las tres filas de anexo** que la app tiene, que es lo que impide que la
  segunda salga un tono distinta.
- **`HoleAbsence`** sustituye al booleano `missing` de `AlbumHole`: `Filled`, `Missing` («te falta», la
  penumbra del 14 %) y `Wanted` («esto lo buscas», la moneda entera bajo el mismo filete de puntos).
  Ninguna de las dos ausencias brilla: el brillo es la luz del metal y en el hueco no hay metal.

## Lo que este ticket no toca

- **Las otras cuatro superficies siguen en penumbra**: el escaparate, la hoja de una moneda, los dos
  ejes del cuaderno y la página impresa. Que el fantasma signifique lo mismo en todas ellas es
  [#556](https://github.com/jenarvaezg/coindex/issues/556), abierto al elegir esta forma. Se ve en
  `despues-explorar.jpg`: las láminas del estante siguen apagadas.
- **El diámetro de la tira (40 dp) y las tres casillas dibujadas no han pasado por el banco** de
  ADR 0026 §15. Son la única cifra de esta implementación que no viene medida de la maqueta ni del
  emulador, y el banco es quien la aprueba si algún día se discute.

## ADR

- **ADR 0026 §8 cláusula 3**, enmendada: un anexo tiene **dos sitios posibles** —la cabecera para una
  lista de la compra, el pie para un escaparate— y la puerta deja de ser siempre «the last row». Las dos
  formas de la puerta compuesta se retiran.
- **ADR 0026 §15**, enmendada: el fantasma son **dos ausencias**, y sólo una es penumbra. El valor sigue
  siendo del banco; qué ausencia dibuja cada superficie es del mapa.
- **ADR 0030 §8 cláusula 5**, enmendada: el anexo pierde una habitación y gana un hermano. La puerta
  interna de «Explorar» sobrevive con el mismo nombre, así que la cláusula 1 —un anexo cuelga de una
  sola jerarquía— no se dispara: las dos cuelgan de Colecciones.

## Cómo se comprobó

    ./gradlew :domain:test :app:testDebugUnitTest     # 904 tests, 0 fallos
    ./gradlew :app:connectedDebugAndroidTest          # 89 tests en coindex-chrome, 0 fallos

Los instrumentados incluyen uno nuevo: una casilla marcada, dibujada entera, **tampoco brilla**.

Las capturas se tomaron en `coindex-ux` con el APK de debug de esta rama y el de `main` para el
«antes», sobre la misma base restaurada. Los PNG a 1080 y la base están en
`/private/tmp/coindex-privado/dos-puertas-520/`; aquí quedan los jpg. **La API key del emulador se
cambió por una falsa antes de darle red**: las fotos del catálogo bajan sin clave, y así ninguna
medición gasta cuota de nadie (`cuota-numista-compartida-con-el-padre`).
