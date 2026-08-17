# Prototipo · las dos puertas de Colecciones (#520)

Maqueta de forma para el [#520](https://github.com/jenarvaezg/coindex/issues/520). La pregunta que
contesta no es «¿se parte la puerta compuesta?» —el grillado del 16 de agosto de 2026 ya decidió que
sí— sino la que deja abierta al decidirlo:

> La decisión promete **«un tap de home con su recuento en la primera vista»**, y ADR 0026 §8
> cláusula 3 dice que la puerta del anexo es **lo último de la página**. Con 69 tarjetas, lo último
> de la página está a **4,2 pliegues** del arranque. ¿Dónde caen las dos filas, entonces, y cómo se
> dibujan?

Ocho formas × cinco estados, a dp real y con la de hoy de listón. En HTML y no en Compose porque lo
que se elige es estructura (`prototipar-forma-en-html`).

    python3 docs/ux/prototipo-dos-puertas-520/extract.py
    python3 docs/ux/prototipo-dos-puertas-520/build.py
    open /private/tmp/coindex-privado/dos-puertas-520/maqueta.html

Barra `sticky` arriba con los dos ejes, y también por URL: `#C-buscando`. Flechas ←→ cambian de
forma, ↑↓ de estado. Bajo el teléfono, la maqueta dice en cada combinación cuántas tarjetas entran
en el pliegue y cuánto hay que arrastrar para ver «Lo que busco».

## Lo medido, sobre el dibujo

Con las **dos marcas que el padre tiene de verdad** y las 20 láminas de la ventana del estante.
«Tarjetas visibles» es la fracción de tarjeta que entra en el pliegue al arrancar; el listón replica
11,36 contra las **11,04** medidas en el emulador en el #279, o sea un 3 % más densa.

| | dónde cae la fila | tarjetas en la primera vista | página | para ver «Lo que busco» |
| --- | --- | ---: | ---: | --- |
| **0** · Hoy · v1.5.0 | al pie, con dos destinos | 11,36 | 4247 dp | **3417 dp · 4,2 pliegues** |
| **A** · dos filas al pie | al pie | 11,36 | +56 | 3417 dp · 4,2 pliegues |
| **B** · un canto, dos renglones | al pie | 11,36 | +49 | 3417 dp · 4,2 pliegues |
| **C** · arriba, bajo el estante | bajo los filtros | 10,37 | +56 | 0 · a 166 dp del borde |
| **D** · arriba, con su censo | bajo los filtros | 10,10 | +71 | 0 · a 166 dp del borde |
| **E** · arriba, con las monedas | bajo los filtros | 9,60 | +99 | 0 · a 166 dp del borde |
| **F** · el canto cosido lo dice | en el canto | 11,36 | +0 | 0 · pero sin destino |
| **G** · arriba del buscador | colgando del canto | 10,37 | +56 | **0 · a 70 dp del borde** |

Tres cosas que sólo dice esa tabla:

1. **A y B cumplen la letra del ticket y no su promesa.** Parten el rótulo doble, sí; la visibilidad
   se queda exactamente donde estaba, porque el pie de una página de 69 tarjetas no es la primera
   vista de nadie.
2. **Subir la fila cuesta una tarjeta**, no cero: 11,36 → 10,37. La fila no sustituye a nada, empuja
   el índice.
3. **Con nada marcado, las de arriba dejan la primera vista idéntica a hoy** (11,36). La fila no se
   imprime al cero, que es la regla de siempre, así que el peaje se paga **sólo cuando hay algo que
   buscar** — y quien no marca nada no paga nada.

## Las ocho

| | tesis | qué la hunde o la sostiene |
| --- | --- | --- |
| **0** · Hoy | un rótulo, dos destinos prometidos, un tap | listón |
| **A** · dos filas al pie | la lectura literal del grillado | no toca la visibilidad |
| **B** · un canto con dos renglones | dos renglones de un objeto, no dos objetos | lo mismo, 7 dp más barato |
| **C** · arriba, bajo el estante | la primera vista es arriba o no es | queda **dentro** de la zona de buscar |
| **D** · arriba, con su censo | la fila le debe su censo al lector | hoy dice el mismo número dos veces |
| **E** · arriba, con las monedas | lo que engancha es la moneda | las marcas son huecos vacíos: no hay moneda |
| **F** · el canto cosido | la primera vista ya existe | un recuento que no se puede tocar |
| **G** · arriba del buscador | primero lo que hay, después la herramienta | **la que propongo** |

## Lo que sólo se vio al dibujarlo

1. **La decisión del 16 de agosto toca una cláusula más de la que dice.** El comentario anuncia
   enmienda a ADR 0026 §8 (las dos formas de la puerta) y a ADR 0030 §8 (el anexo hermano). Si la
   fila sube, hay que enmendar además la primera mitad de esa misma cláusula 3, que es la que dice
   *«Its door is the **last row** of that hierarchy's list, deeper paper»* — y que `AnnexDoor` cita
   en su propio comentario. A y B son las únicas dos que no la tocan, y son las dos que no cumplen la
   promesa de visibilidad: **la contradicción no se puede resolver sin enmendar una de las dos
   cosas.**
2. **Bajo el buscador, un recuento inmune al buscador se lee como un error.** En la C, escribir
   «venez» deja «Lo que busco · 2» entre un «3 de 69 colecciones» y una lista de tres tarjetas
   (`vC-buscando.jpg`). Es el mismo problema que el #515 arregló para la puerta del pie con una nota
   —«Lo que escribes arriba no llega hasta aquí»— y ahí está la trampa: con **dos** filas, las dos
   cuentan poblaciones que el buscador no toca, así que la nota tendría que imprimirse **dos veces**,
   que es el mueble que ADR 0026 §5 tasa. La G lo esquiva sin nota ninguna: encima del buscador, la
   fila no está en la conversación de buscar.
3. **Las casillas marcadas no enseñan monedas.** La E dibuja las marcas a 40 dp con el fantasma real
   de una casilla vacía (alfa 0,14 y su círculo de puntos): lo que sale son dos discos grises
   (`vE-monedas.jpg`). Lo que engancha no puede ser precisamente lo que falta, así que la tesis de la
   E se cae por su propio dibujo — y no por el ADR que ya descartó la sección fija.
4. **El censo de la D es hoy una redundancia.** Con las dos marcas de verdad dice «Lo que busco · 2»
   y debajo «2 casillas en 1 lámina»: el mismo número dos veces. Sólo aporta cuando las marcas
   cruzan láminas, y ése es un estado que el padre no tiene todavía.
5. **El canto cosido no recibe taps.** La F pone «busco 2» en el canto, que es la única línea de la
   primera vista que ya está arriba — pero en `AlbumChrome.kt` el canto es un `Row` con dos textos y
   **un solo** `clickable`, el glifo de Ajustes. Un recuento ahí es un número que no se puede tocar:
   el pecado inverso del rótulo que promete dos destinos. Y es un cuarto dato en una línea de 10 sp
   que ya va con `TextOverflow.Ellipsis`.
6. **Al buscar, la puerta del pie sí se ve.** Con «venez» escrito la página entera cae a 822 dp —cabe
   en el pliegue— y la fila del pie aparece sin arrastrar en las ocho formas. Es decir: **hoy la
   puerta compuesta sólo está en la primera vista cuando el índice está recortado**, que es
   exactamente cuando su recuento no habla de lo que se está mirando.

## Lo que propongo

**La G: la fila de «Lo que busco · 2» colgando del canto cosido, encima del buscador, y «Y otras 20
láminas que no coleccionas →» al pie, donde ya está.** No es la C con la fila movida 96 dp: es la
única de las ocho que pone el recuento en la primera vista sin meterlo en la zona de buscar, y la
que dibuja lo que el grillado dijo con palabras —«anexo **hermano** colgando de Colecciones»— porque
literalmente cuelga del canto de Colecciones, en su papel profundo, antes de cualquier herramienta.

- Cuesta **una tarjeta** de la primera vista, y sólo cuando hay algo marcado.
- No necesita la nota del #515 arriba, así que la app no imprime la misma frase dos veces.
- Deja la fila del estante intacta al pie: misma jerarquía, mismo nombre, la cláusula 1 del anexo
  sigue sin dispararse.
- Enmienda que pide: ADR 0026 §8 cláusula 3 pasa a tener **dos sitios** para un anexo —el pie para
  el que se pasea, el canto para el que se usa— y el criterio para elegir es si el recuento es una
  lista de la compra o un escaparate.

Si la elección es no enmendar la cláusula del pie, entonces **la B** antes que la A: el mismo coste
menos 7 dp, y un canto con dos renglones dice que son el borde del álbum y no dos tarjetas nuevas.
Pero conviene decir en el ticket que esa rama **no sube la visibilidad**: parte el rótulo y nada más.

## Cómo está hecha

`extract.py` lee la **caché sembrada del móvil del padre** (`.local/padre/coindex.db`), que es lo que
la app enseña y no lo que `data/` dice (`medir-en-el-movil-no-en-el-asset`): 69 tarjetas, 580 piezas,
198 tipos, 20 láminas en la ventana del estante y las dos casillas marcadas de verdad. Las
fracciones salen de `memberMatches`, la ventana de `showcasePlate`, el orden de `indexOrder` y la
cara de cada casilla de `printed_side`; el recuento se cotejó con `FieldReportTest` (más abajo). `build.py` escribe un HTML autocontenido con las 40 pantallas
dentro y dos ruedas que eligen cuál se ve; las 71 fotos van una vez cada una como clase CSS.

- **411 × 914 dp**, 1 px CSS = 1 dp, el Pixel 7 de las capturas del #296.
- El cromo se leyó en el código: `AlbumChrome` (54 dp), `SearchField` (40), la fila del estante (48),
  el margen de 12, la calle de 8, el paso de 6 y el hueco de 104 de `IndexScreen.kt`, `AnnexDoor`
  (papel profundo, 14 de aire, 8 encima) y las escalas de `fieldTypography`. **El canto cosido, el
  buscador y el estante scrollean**, porque en la app son filas de la misma rejilla que las tarjetas.

## Lo que la maqueta no prueba

- **Las 69 tarjetas están contrastadas, no supuestas.** La derivación de las que no tienen catálogo
  se reproduce aquí con la regla gruesa —familia sembrada y peso, sin las familias técnicas de
  ADR 0012— así que se cotejó contra el dominio de verdad:

      COINDEX_FIELD_SNAPSHOT=/Users/jose/jenarvaezg/coindex/.local/padre \
        ./gradlew :app:testDebugUnitTest --tests '*FieldReportTest*' --rerun

  `FieldReportTest` da **67 tarjetas** —49 con fracción y 18 sin lista de emisiones— sobre la captura
  de Numista del 3 de agosto (229 filas, 572 piezas). La maqueta lee la db del móvil, que va ocho
  piezas y siete tipos por delante (237 filas, 580 piezas), y añade exactamente dos tarjetas: la caja
  **Cofre Chino**, que la captura de Numista no contiene, y un bulto más. 67 y 69 son la misma cuenta
  con dos semanas de diferencia, y **el arrastre de 4,2 pliegues no depende de esa horquilla**.
- **El estado «feria» inventa siete marcas** sobre las dos que hay, y su tira de monedas enseña las
  dos reales con un «y 5 más».
- **Nada de esto se ha visto en un teléfono.** El navegador elige, el emulador confirma
  (`medir-en-el-movil-no-en-el-asset`). En particular el alto exacto de la fila y el corte del canto
  a 10 sp se comprueban en el AVD antes de dar la medida por buena.

## Lo privado y lo que se tira

`data.json`, las fotos, la maqueta y los PNG de las capturas están en
`/private/tmp/coindex-privado/dos-puertas-520/`. Aquí quedan el método, las proporciones y los jpg.
No hay ningún importe en euros en toda la maqueta, así que se puede publicar tal cual.

`extract.py` y `build.py` son del prototipo y se borran cuando el ticket se cierre. Lo que sobrevive
es este README.
