# Prototipo · «Explorar» con dos cosas dentro (#498)

Maqueta de forma para el [#498](https://github.com/jenarvaezg/coindex/issues/498), la segunda
entrega de la cola del [#283](https://github.com/jenarvaezg/coindex/issues/283). La pregunta que
contesta **no** es «¿cómo se pinta el estante?» —eso lo eligió el
[#279](https://github.com/jenarvaezg/coindex/issues/279): la tarjeta de 104 dp, el fantasma, la
pantalla propia— sino la que aquel ticket no pudo hacerse porque la puerta estaba vacía:

> **«Explorar» ya tiene «Lo que busco» dentro.** El [#497](https://github.com/jenarvaezg/coindex/issues/497)
> la creó ayer, y `ExploreScreen.kt` lo dice por escrito: *«es su primera sección, y hoy es toda
> ella»*. ¿Qué es esa pantalla cuando además entran veinte láminas ajenas — y qué enseña el estante
> el día uno, cuando el coleccionista no ha tasado ninguna?

Cinco variantes × tres estados de tasación, a dp real, con el estado de hoy como listón. En HTML y
no en Compose porque lo que se elige es **estructura** (`prototipar-forma-en-html`).

    python3 docs/ux/prototipo-escaparate-498/extract.py
    python3 docs/ux/prototipo-escaparate-498/build.py
    open /private/tmp/coindex-privado/escaparate-498/maqueta.html

Barra `sticky` arriba con los dos ejes, y también por URL: `?v=B&t=algunas`. **No se publica como
artifact ni se versionan capturas**: lleva los importes de la colección del padre
(`dinero-fuera-del-repo-publico`).

## Lo elegido

**La D′: un solo estante de «lo que te falta», y «Lo que busco» conservada entera detrás de su
puerta.** Elegido por Jose el 14 de agosto de 2026 con la maqueta delante.

- **Explorar es un estante y no dos secciones.** Entran las veinte láminas ajenas **y las láminas
  tuyas que tienen alguna casilla marcada** —tres de las 49 hoy—, ordenadas por «primero lo que
  busco». La marca es un estado de la casilla y no una sección de la pantalla, que es la tesis de
  la D.
- **La lista sigue siendo pantalla.** La fila de papel profundo «Lo que busco · 7 casillas →» abre
  la pantalla del #497 intacta: las siete casillas como huecos con su precio dentro, «Quitar» y
  «Exportar la lista». Se conserva porque **es la hoja que se lleva a la feria**, y una tarjeta de
  índice con «2 lo busco» no se lleva a ninguna parte.
- **Se descartó la D″** —borrar la pantalla y dejar sólo `Exportar lo que busco` en el estante—:
  exporta lo mismo, pero deja el móvil sin ningún sitio donde ver las siete juntas, obliga a abrir
  cinco láminas para saber qué marcaste, y gasta el vocabulario de una pantalla publicada el día
  antes (el destino, la puerta, `screenTitle`, lo que `PrunedVocabularyTest` sujeta). Es más limpia
  de tesis y mucho más cara de deshacer; si la lista se demuestra inútil como pantalla, la D′ se
  convierte en la D″ sin haber gastado nada dos veces.
- **Y la D‴** —la puerta *más* un botón de exportar en el estante— se descartó por lo mismo que la
  D″ cuesta pliegue sin comprar nada: dos gestos sobre lo mismo en una pantalla, y la primera
  tarjeta a unos 250 dp.
- **La A, la B y la C** quedan como registro: la A porque su primera sección no tiene techo, la B
  porque su tesis es que la lista es un anexo del anexo —que es la D′ sin el orden— y la C porque
  cobra una fila de cromo permanente y una pregunta nueva, en cuál abre.

Lo que **no** decide esta maqueta y hay que escribir antes de tocar código: los dos renglones de
ADR que la familia D obliga (§8 de ADR 0026 y §2 de ADR 0021, ver más abajo), la enmienda de ADR
0028 §3 y §5 que el ticket ya manda, y las cifras del #498, que hay que rehacer.

## Las cinco variantes

Las tres primeras contestan la misma pregunta con tres jerarquías distintas; la cuarta se niega a
que haya dos cosas. La columna del pliegue es de la maqueta, sin tasar.

| | tesis | en el pliegue |
| --- | --- | --- |
| **0** | **Hoy · v1.3.0**: Explorar es «Lo que busco» y nada más | 6,6 de las 7 casillas |
| **A** | **Es una hoja, y lo tuyo va primero**: las marcas, una regla, y debajo el estante | 6,6 casillas · **0 láminas** |
| **B** | **El estante manda y tu lista vuelve a ser una puerta** (una fila de papel profundo) | 11,1 de 20 láminas |
| **C** | **Dos lengüetas**: dos hojas enteras que comparten puerta, ninguna scrollea sobre la otra | 11,3 de 20 · pliegue 40 dp más corto |
| **D** | **La marca es un estado de la casilla, no una sección**: un solo estante de «lo que te falta» | 11,1 de 23 láminas |
| **D′** | La D **con la lista detrás de su puerta**: el estante se queda el orden, la hoja de la feria sigue siendo pantalla | 10,2 de 23 |
| **D″** | La D pura: **«Lo que busco» deja de ser pantalla** y se vuelve la exportación del estante | 9,8 de 23 |

Los tres estados del eje de tasación son **sin tasar** (el día uno, con el orden por coste vacío),
**seis tasadas** (cuatro a mano y las dos que llevan casilla marcada) y **las veinte tasadas**.

La D′ y la D″ salieron después de que Jose se inclinara por la D, y las dos pagan lo mismo en
pliegue: la primera tarjeta cae de **173 dp** (D) a **223** (D′, la fila de la puerta) o **246**
(D″, el botón de exportar). En tarjetas, la D enseña 11,1 y las otras dos 10,2 y 9,8.

## Lo que sólo se ve con la maqueta delante

1. **La primera sección de la A no tiene techo.** Con siete marcas, la primera lámina del
   escaparate cae a **1.104 dp, que es 1,34 pliegues**: entrar en Explorar y no ver ni una de las
   veinte. Y «Lo que busco» crece con cada marca, así que no es un mal día, es la tendencia.
2. **El día uno el orden por coste no tiene nada que ordenar.** El [#282](https://github.com/jenarvaezg/coindex/issues/282)
   eligió «por coste de entrar» cuando el pase tasaba solo; con la tasación a mano del #498, el
   estante nace sin una sola cifra. Cada variante propone otra cosa en su sitio —por casillas, por
   coste con el aviso «cuando tases», como las curé, primero lo que busco— y **esto hay que
   decidirlo**, no es cromo.
3. **El nombre de la pantalla es una decisión pendiente y ya está escrita a medias.**
   `WishLabels.DESTINATION` dice que la pantalla se llama «Lo que busco» *sólo mientras el estante
   no exista*, y `screenTitle` la nombra con esa misma constante. Con el estante dentro, la B, la C
   y la D se llaman «Explorar» y la A también tendría que hacerlo: es un cambio de rótulo que
   `PrunedVocabularyTest` mira, y la puerta del índice pasa a la forma larga que ADR 0026 §8 dejó
   escrita —«Lo que busco · 7, y otras 20 láminas →»— que **ninguna variante pinta todavía**.
4. **El total con dos edades del [#494](https://github.com/jenarvaezg/coindex/issues/494) se ve en
   el estante, no sólo en la lámina.** Dos de las veinte llevan casilla marcada, así que su precio
   ya lo pide el pase de este mes mientras el resto es de cuando se tasó a mano: en la tarjeta eso
   son dos fechas bajo un solo importe. La maqueta lo escribe «12 y 14 ago» para verlo; **no es una
   propuesta de copy**.
5. **La marca sobre la tarjeta es invención de la maqueta.** Un «lo busco» dentro del hueco de una
   tarjeta del estante dice «esta lámina tiene una casilla que buscas», y una tarjeta de índice no
   es una casilla. Está dibujado para poder juzgarlo; si sobrevive, hay que decidir si es marca o
   pie de tarjeta.
6. **La D mezcla los dos regímenes en una rejilla**: una tarjeta con fracción y «2 lo busco» junto
   a otra en fantasma con su coste. Es su tesis, no un descuido — y es lo que hay que mirar de
   ella.
7. **Si se elige la familia D, la puerta del índice deja de ser la que ADR 0026 §8 escribió.** §8
   dejó dicho «Lo que busco · 7, y otras 20 láminas →», y una pantalla que además contiene tres
   láminas *tuyas* no se anuncia con esa frase: la puerta pasa a nombrar «lo que te falta» con las
   23. Es un renglón del ADR, no un detalle de copy, y va con la enmienda de §3 y §5 que el #498 ya
   manda escribir antes de tocar código.
8. **Las láminas propias que entran en la D son sólo las que tienen marca** —tres de las 49— así
   que no es un segundo índice de la colección: es «donde te falta algo que buscas». Pero **una
   lámina propia pasa a estar en dos listas**, y eso es lo que hay que aceptar por escrito.

## Y lo medido del ticket ya no cuadra

El #498 dice «14 láminas de ≤10 casillas y 6 de 11-19, 17 de 20 con suelo de plata, 4 láminas a 34
llamadas, 245 las veinte». Con `data/` de hoy y la aritmética de `valuationPlan`:

| | el ticket | hoy |
| --- | ---: | ---: |
| láminas de ≤10 casillas | 14 | **15** |
| láminas de 11-19 | 6 | **5** |
| con suelo de plata completo | 17 | **20** |
| la lámina más cara de tasar | 34 llamadas ×4 | **34, y sólo una** |
| tasar las veinte | 245 | **227** |

Los catálogos y la caché de tipos han crecido desde el #282. El orden de magnitud aguanta —cero
coste fijo al mes, ninguna lámina por encima de 34 consultas— pero **las cifras del ticket hay que
rehacerlas antes de implementarlo**, y con las veinte con precio completo desaparece el caso «qué
dice una lámina explorable sin precio» que el #282 dejó como fleco 3.

## Cómo está hecha

`extract.py` saca los datos y las fotos al anexo privado; `build.py` escribe el HTML autocontenido.
Ninguno de los dos vive en el APK ni en el pipeline: son la maqueta, y se borran cuando dejen de
hacer falta.

- **411 × 914 dp**, 1 px CSS = 1 dp, el Pixel 7 de las capturas del #296.
- **El cromo se leyó en el código**: `CoindexApp.Masthead` con «Volver» y sin Ajustes, la rejilla de
  `IndexScreen` (margen 12, calle 8, tarjeta de 104 dp, nombre de dos líneas fijas que encoge hasta
  13 sp), la de `ExploreScreen` (margen 20, `PlateSpacing.rowGap` 32) y el hueco de `AlbumPaper`.
  **Sin barra inferior**: Explorar no es raíz (`Routes.isRoot`), se entra por la puerta del índice y
  se sale con «Volver».
- **Datos reales**: los 69 catálogos medibles de `data/` cruzados con la colección del padre (229
  filas), con `memberMatches` entera y la evidencia de `resolvePlate`. Las veinte son las curadas
  sin evidencia de menos de 20 casillas.
- **Sin el umbral de 10 casillas**, que es la corrección que el #498 manda hacerle al prototipo del
  #279 (`extract.py:104` de aquél): era la regla del reproche de *tus* huecos y una lámina ajena no
  reprocha nada.
- **Las siete marcas son inventadas.** El #497 se publicó ayer y el padre no ha marcado nada: cinco
  caen en tres láminas suyas y **dos en dos del escaparate**, que es lo que hace visible el #494.
- **Los importes son el suelo de la plata**, no precio de catálogo: gramos × ley del
  `composition.text` × el spot de `SilverSpot.kt`. La app enseñaría el mayor de tres precios, así
  que los de verdad son más altos.

## Lo que la maqueta no prueba

La **densidad**. El listón del #279 salía un 10 % más denso que el emulador, y aquí no hay captura
del AVD de esta pantalla contra la que calibrar: los números del pliegue son de la maqueta y sirven
para comparar variantes entre sí, no para cerrar cuántas tarjetas entran.
**Nada de esto se ha visto en un teléfono** (`medir-en-el-movil-no-en-el-asset`).

Y **ninguna variante ofrece tasar las veinte de golpe**: el ticket lo prohíbe —«nunca de golpe»— así
que las 227 consultas sólo se gastan lámina a lámina. El gesto que las nombra vive dentro de la
lámina, que es la otra mitad del #498 y **no está en esta maqueta**.
