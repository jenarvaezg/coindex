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

## Lo elegido

**La E: «Lo que busco · N» arriba, bajo el estante, con las casillas marcadas dibujadas en la fila —
y a plena luz.** Elegido por Jose el 17 de agosto de 2026 con la maqueta delante (`vE-monedas.jpg`).

    COINDEX   69 colecciones · 580 piezas · 198 tipos
    ┌ Buscar entre tus colecciones ─────────────┐
    ▸ Filtros y orden        69 colecciones  [Exportar láminas]
    ┌───────────────────────────────────────────┐
    │ LO QUE BUSCO · 7                        › │
    │ (○) (○) (○)  y 4 más                      │
    └───────────────────────────────────────────┘
     ● Plata a valor facial   ● Fuertes   ● 1000 escudos…

- **La fila sube a la primera vista** y cuesta 1,76 tarjetas del pliegue (11,36 → 9,60). Al cero no
  se imprime, así que quien no marca nada no paga nada.
- **«Y otras 20 láminas que no coleccionas →» se queda al pie**, donde ADR 0026 §8 la pone, con su
  nombre estable y su único destino. La puerta compuesta desaparece.
- **Las marcas van a plena luz, no en fantasma** — enmienda de la misma decisión: *«sólo tiene
  sentido fantasma cuando no la tienes en una colección que sigues»*. A 40 dp el fantasma de alfa
  0,14 son dos discos grises (`vE-fantasma.jpg` conserva la prueba); a plena luz se ven las monedas y
  lo que dice que no son tuyas es el filete de puntos. **El idioma completo de los fantasmas —las
  seis superficies donde se dibujan— se decide en el
  [#556](https://github.com/jenarvaezg/coindex/issues/556)**, y esta fila hereda lo que salga de ahí.
- Lo que queda por calibrar al implementar: **el diámetro de la tira** (40 dp aquí, sin pasar por el
  banco de ADR 0026 §15) y cuántas casillas se dibujan antes del «y N más» (tres aquí).
- Enmiendas que pide: ADR 0026 §8 cláusula 3 —dos sitios para un anexo, y la puerta deja de ser
  siempre la última fila— y ADR 0030 §8 —el anexo pierde una habitación y gana un hermano—.

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
| **E** · arriba, con las monedas · **elegida** | bajo los filtros | 9,60 | +99 | 0 · a 166 dp del borde |
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
| **E** · arriba, con las monedas | lo que engancha es la moneda | **elegida**, con las marcas a plena luz |
| **F** · el canto cosido | la primera vista ya existe | un recuento que no se puede tocar |
| **G** · arriba del buscador | primero lo que hay, después la herramienta | la que propuse; descartada |

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
   fila no está en la conversación de buscar. **Con la E elegida esto queda como fleco de
   implementación**: la fila cae bajo el buscador, así que hay que decidir qué dice —o si calla— la
   fila de arriba mientras se escribe, sin imprimir la nota del #515 dos veces.
3. **Las casillas marcadas no enseñaban monedas, y eso no hundió la tesis: hundió el fantasma.** La
   primera vuelta de la E dibujó las marcas a 40 dp con el fantasma real de una casilla vacía (alfa
   0,14 y su círculo de puntos) y salieron **dos discos grises** (`vE-fantasma.jpg`). La lectura de
   Jose con la maqueta delante fue la contraria a la mía: el dibujo no sobra, sobra la penumbra —
   *«sólo tiene sentido fantasma cuando no la tienes en una colección que sigues»*—. A plena luz la
   misma fila enseña monedas (`vE-monedas.jpg`), y el idioma de los fantasmas en las **seis**
   superficies donde se dibujan se abre como ticket propio, el
   [#556](https://github.com/jenarvaezg/coindex/issues/556).
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

## Lo que propuse, y por qué no fue

Propuse **la G** —la misma fila, pero colgando del canto cosido, **encima** del buscador— por un
argumento que sigue en pie: es la única de las ocho que pone el recuento en la primera vista sin
meterlo en la zona de buscar, y ahorra la nota del #515 arriba. Jose eligió la E, que dibuja las
monedas, y las dos decisiones no se contradicen: **la tira de casillas es una razón para la fila que
la G no tiene**, porque encima del buscador esa tira empujaría el buscador y el estante hacia abajo
sin que nada más arriba explique de qué son esas monedas. Lo que la G aporta se rescata como fleco:
qué dice la fila mientras se escribe.

La rama que no se tomó, escrita para que nadie la reabra sin verla: **B antes que A** si algún día se
decide no enmendar la cláusula del pie —el mismo coste menos 7 dp, y un canto con dos renglones dice
que son el borde del álbum y no dos tarjetas nuevas—. Pero esa rama **no sube la visibilidad**: parte
el rótulo y nada más.

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
