# La copia de Coindex en cuatro cubos

El material del [#297](https://github.com/jenarvaezg/coindex/issues/297) para la poda del
[#305](https://github.com/jenarvaezg/coindex/issues/305), sobre `main` a `a5c5e60` (v0.16.0 más el
#290). **No decide qué se poda**: clasifica cada cadena que la capa de interfaz puede poner delante
del padre, dice en qué pantalla se ve, y en qué cubo cae.

Los cuatro cubos son los del mapa [#278](https://github.com/jenarvaezg/coindex/issues/278):

1. **Dice qué hacer ahora** — un botón, un campo, el rótulo de un control, la salida de un callejón.
2. **Explica un porqué de producto** — prosa que justifica una decisión: por qué la lista se deriva,
   por qué hay un techo de llamadas, por qué una colección desaparece.
3. **Repite lo que el dato ya dice** — palabras que no distinguen nada de nada.
4. **Podría ser forma** — un estado, un progreso o una identidad que un troquel, un sello, un borde o
   una textura dirían mejor y en menos sitio.

Un cubo por cadena, el que más pesa; la columna **nota** dice cuando el reparto es discutible y por
qué. La columna **siempre** distingue lo que ocupa sitio en cada visita de lo que sólo aparece cuando
algo pasa, con el mismo criterio de [la medida base](medida-base-296.md): la poda no cuesta lo mismo
en una que en otra.

## Dos correcciones al censo antes de empezar

**El ticket habla de trece ficheros y son doce.** `ui/PlateLabels.kt` se borró el 7 de agosto a las
16:02, en el #290 (que cierra el [#218](https://github.com/jenarvaezg/coindex/issues/218)), unas
horas después de que se midiera la base: la lámina viaja entera y su copia vive ahora en
`ui/PlateSubject.kt`, que no es un fichero de sólo copy. Las cifras de este informe son de los doce
vivos más `PlateSubject.kt`.

**Y de las 949 líneas, la mitad son comentarios.** Los doce ficheros suman **886 líneas, de las que
420 (el 47 %) son KDoc y 74 están en blanco: 392 líneas de código**. El KDoc no está delante de
nadie y no se poda; el número que la poda mueve es cuántas cadenas hay y cuántas palabras dicen.

| fichero | líneas | KDoc | código |
| --- | ---: | ---: | ---: |
| `ui/Labels.kt` | 177 | 75 | 85 |
| `ui/NotebookLabels.kt` | 125 | 68 | 47 |
| `ui/FichaLabels.kt` | 88 | 40 | 42 |
| `ui/SettingsEntry.kt` | 78 | 40 | 31 |
| `ui/SyncMessages.kt` | 77 | 15 | 57 |
| `ui/SheetLabels.kt` | 76 | 46 | 26 |
| `ui/print/PrintedLabels.kt` | 62 | 43 | 15 |
| `ui/shelf/ShelfLabels.kt` | 50 | 21 | 22 |
| `ui/PhotoCacheLabels.kt` | 43 | 14 | 26 |
| `ui/PiecesLabels.kt` | 33 | 23 | 8 |
| `ui/InstallMessages.kt` | 28 | 10 | 15 |
| `ui/MastheadLabels.kt` | 25 | 12 | 10 |
| `ui/UpdateNotes.kt` | 24 | 13 | 8 |
| **total** | **886** | **420** | **392** |

**Y la corrección que de verdad cambia el trabajo: los ficheros de copy no son donde está la copia.**
La medida base ya lo había medido en palabras —tres cuartas partes de la prosa siempre visible están
fuera del cuaderno—; contado por entradas de este informe, de las **176 sólo 66 salen de los doce
ficheros**. Otras 106 están escritas a mano en pantallas y componentes, y 4 en los dos sitios a la
vez. Un `Labels.kt` más delgado no adelgaza ninguna pantalla: la poda se juega pantalla a pantalla, y
por eso las tablas que siguen cubren la capa `ui/` entera y no los doce ficheros.

## El reparto

La unidad es la **entrada**: una cadena, o la familia de cadenas de un mismo `when` cuando se decide
entera (los seis interruptores del cuaderno, los trece metales). **176 entradas**: 138 en los cuatro
cubos, 21 en Ajustes y el alta, 9 en el papel y el PNG, y 8 que no lee nadie.

| cubo | entradas |
| --- | ---: |
| 1 · dice qué hacer ahora | **53** |
| 2 · explica un porqué de producto | **46** |
| 3 · repite lo que el dato ya dice | **20** |
| 4 · podría ser forma | **19** |

Y por pantalla. Una entrada que se ve en dos pantallas cuenta en las dos, así que las filas suman más
que 138:

| | cubo 1 | cubo 2 | cubo 3 | cubo 4 | total |
| --- | ---: | ---: | ---: | ---: | ---: |
| **Colecciones** | 25 | 20 | 8 | 9 | **62** |
| **Monedas** | 21 | 9 | 6 | 5 | **41** |
| **Piezas** | 8 | 10 | 3 | 2 | **23** |
| **Lámina** | 2 | 7 | 5 | 5 | **19** |
| **Cualquiera** (masthead, banda de versión, arranque) | 5 | 6 | 2 | 1 | **14** |
| **Papel** | 0 | 0 | 3 | 3 | **6** |
| **PNG exportado** | 0 | 0 | 1 | 2 | **3** |

Tres lecturas del reparto:

- **La pantalla de llegada es la más cargada, y de largo.** 62 entradas en Colecciones frente a 19 en
  la Lámina, que es la única que se diseñó. No es una sorpresa —el índice es donde están el sync, la
  exportación, la estantería y el presupuesto— pero sí es el orden en que la poda tiene que trabajar.
- **El cubo 3 es pequeño y el cubo 4 no lo es tanto.** Coindex casi no repite lo que el dato dice; lo
  que hace es **decir con palabras cosas que son estados** — `TENGO`, `ME FALTA`, `ANVERSO`, `Ficha
  traída hoy`, `22 / 22 emisiones`, `En ninguna colección`. Esas 19 entradas del cubo 4 son pocas y
  son las que se imprimen ocho, veinticuatro y ciento noventa y dos veces. **La poda no es un
  ejercicio de redacción: es de conversión.**
- **El cubo 1 es el más grande y es el que no se toca.** 53 entradas dicen qué hacer y casi ninguna
  sobra. Lo que sí tiene que discutir la poda es *cuántas veces* aparece una de ellas: `Actualizar la
  ficha · 1 llamada` y `Ver en Numista` son cubo 1 impecable, repetido por tarjeta.

## Cubo 1 · Dice qué hacer ahora

53 entradas. Se listan porque hay que saber que están, no porque sobren.

| cadena | fichero | pantalla | siempre | nota |
| --- | --- | --- | --- | --- |
| `Sincronizar` / `Sincronizando…` | `screens/IndexScreen.kt` | Colecciones | sí | |
| `Exportar N láminas` / `Nada que exportar` | `NotebookLabels.kt` | Colecciones | sí | |
| `Exportando…` | `screens/IndexScreen.kt` | Colecciones | no | |
| `Todavía no has sincronizado con Numista.` | `screens/IndexScreen.kt` | Colecciones | sí (sin sync) | La única pantalla del padre donde no aparece: la suya siempre tiene fecha. |
| `Sincronización incompleta` + «La última sincronización no terminó…» (18 pal.) | `screens/IndexScreen.kt` | Colecciones | no | Discutible con el cubo 2: el párrafo explica, pero acaba en «vuelve a sincronizar». |
| `Buscar` | `components/FilterShelf.kt` | Colecciones · Monedas | sí | |
| `Filtros y orden` | `shelf/ShelfLabels.kt` | Colecciones · Monedas | sí | El estado de reposo de la estantería plegada. |
| `N filtro` / `N filtros` · `orden X` | `shelf/ShelfLabels.kt` | Colecciones · Monedas | no | |
| `▸` / `▾` | `components/FilterShelf.kt` | Colecciones · Monedas | sí | Ya es forma; entra aquí porque es el control. |
| `Orden` · `País` · `Peso` · `Empieza en` · `Estado` · `Serie` | `screens/IndexScreen.kt` | Colecciones | no (plegada) | Los seis rótulos de facetas del índice. |
| `Orden` · `País` · `Peso` · `Año` · `Clase` · `Colección` | `screens/CoinsScreen.kt` | Monedas | no (plegada) | Los seis de Monedas. |
| `Todos` · `Cualquiera` (×3) · `Cualquier año` · `Todas` · `Todo` · `Da igual` | `screens/IndexScreen.kt`, `screens/CoinsScreen.kt` | Colecciones · Monedas | no | **Diez chips para decir «sin filtro» con seis palabras distintas.** El caso más claro de vocabulario que no decidió nadie. |
| `Más completas` · `Menos completas` · `Alfabético` · `Más pesadas` · `Más piezas` · `Alta más reciente` | `shelf/IndexShelf.kt` | Colecciones | no | Los seis órdenes del índice. |
| `Por país` · `Alfabético` · `Año más reciente` · `Año más antiguo` · `Más pesadas` · `Más piezas` | `shelf/CoinsShelf.kt` | Monedas | no | Los seis de Monedas. `Alfabético`, `Más pesadas` y `Más piezas` son los mismos de arriba y significan cosas distintas (colección vs. moneda). |
| `Completas` · `A medias` · `Sin lámina` | `shelf/IndexShelf.kt` | Colecciones | no | |
| `Abierta` · `Cerrada` | `Labels.kt` | Colecciones | no | Vocabulario de curador, y el ADR 0021 §3 sólo lo admite como filtro. |
| `Menos de ½ oz` · `½ – 1 oz` · `Más de 1 oz` · `Conjunto o caja` | `shelf/Bands.kt` | Colecciones | no | |
| `Antes de 1950` · `1950 – 1999` · `Desde 2000` · `Sin fecha` | `shelf/Bands.kt` | Colecciones | no | |
| `Menos de 10 g` · `10 – 25 g` · `Una onza (25 – 34 g)` · `Más de 34 g` · `Sin peso` | `shelf/Bands.kt` | Monedas | no | El peso en gramos aquí y en onzas en el índice: deliberado (una pieza vs. una variante). |
| `Antes de 1900` · `1900 – 1979` · `1980 – 1999` · `Desde 2000` · `Sin año` | `shelf/Bands.kt` | Monedas | no | |
| `Monedas` · `Medallas y fichas` | `Labels.kt` | Monedas | no | |
| `En alguna colección` · `Sin colección` | `shelf/CoinsShelf.kt` | Monedas | no | |
| `Quitar los filtros` | `screens/IndexScreen.kt`, `screens/CoinsScreen.kt` | Colecciones · Monedas | no | La salida del callejón; escrita dos veces. |
| `Ninguna colección pasa por lo que has puesto.` | `screens/IndexScreen.kt` | Colecciones | no | |
| `Ninguna moneda pasa por lo que has puesto.` | `screens/CoinsScreen.kt` | Monedas | no | |
| `Todavía no hay colecciones. Sincroniza para traer tu colección de Numista.` | `screens/IndexScreen.kt` | Colecciones | no | |
| `Todavía no hay monedas. Sincroniza para traer tu colección de Numista.` | `screens/CoinsScreen.kt` | Monedas | no | |
| `No hay ninguna colección que llevar al papel.` | `screens/IndexScreen.kt` | Colecciones | no | |
| `Cómo se exporta` | `screens/ExportOptions.kt` | Colecciones | no | |
| `Fotos` · `Ambas caras` · `Tamaño real` · `Compartir página` · `QR de Numista` · `Sin colección` | `NotebookLabels.kt` | Colecciones | no | Los seis interruptores del cuaderno. |
| `No hay monedas sueltas que imprimir` · `Sin fotos no hay nada que ajustar` | `NotebookLabels.kt` | Colecciones | no | Discutible con el cubo 2: explican por qué un control está gris, y las dos se deshacen solas. |
| `Exportar` · `Cancelar` (hoja de exportación) | `screens/ExportOptions.kt` | Colecciones | no | |
| `Cancelar` (progreso del cuaderno) | `screens/IndexScreen.kt` | Colecciones | no | Tercera y cuarta `Cancelar` de la app. |
| `Preguntando a Numista…` · `Sin presupuesto este mes` | `FichaLabels.kt` | Monedas · Piezas | no | |
| `Actualizar la ficha · 1 llamada` | `FichaLabels.kt` | Monedas · Piezas | sí, por fila | **192 veces en Monedas y una por pieza en Piezas.** El cubo es correcto y la repetición es el problema: 4 palabras × cada tarjeta. |
| `Ver en Numista` | `components/PieceCard.kt` | Piezas | sí, por fila | Lo mismo, 3 palabras por fila. |
| `Fuente en Numista` | `screens/PlateScreen.kt` | Lámina | sí | Una por lámina, no por casilla: aquí no duele. |
| `Exportar lámina como imagen` / `Preparando la lámina…` | `screens/PlateScreen.kt` | Lámina | sí | |
| `Exportar como imagen` / `Preparando la hoja…` | `screens/PiecesScreen.kt` | Piezas | sí | |
| `Renombrar` / `Cerrar el nombre` · `Deshacer la colección` | `screens/PiecesScreen.kt` | Piezas (caja) | sí, en cajas | |
| `Cómo se llama` · `Guardar el nombre` | `screens/PiecesScreen.kt` | Piezas (caja) | no | |
| `Quitar de la colección` | `screens/PiecesScreen.kt` | Piezas (caja) | sí, por fila | |
| `Agrupar piezas` · `Agrupar estas N` · `Nombrar la caja · N` · `Cancelar` | `components/PieceSelection.kt` | Monedas | sí | Cuatro cadenas de la cabecera de Monedas para un gesto que el padre no usa a diario. |
| `✓ Elegida` / `Elegir` | `components/PieceSelection.kt` | Monedas | no | Por tarjeta, sólo con el modo abierto. |
| `Tu caja` · `Agrupar N monedas` · `Cómo se llama` · `Crear` · `Cancelar` | `components/PieceSelection.kt` | Monedas (diálogo) | no | |
| `O añádelas a una que ya tienes:` | `components/PieceSelection.kt` | Monedas (diálogo) | no | |
| `Ponle un nombre a la colección y elige al menos una moneda.` | `BoxNaming.kt` | Monedas (diálogo) | no | |
| `El nombre de la colección no puede estar vacío.` | `BoxNaming.kt` | Piezas | no | |
| `← Volver` · `Ajustes` | `CoindexApp.kt` | cualquiera | sí | |
| `Instalar` / `Descargando…` · `Ver más` / `Ver menos` | `CoindexApp.kt`, `UpdateNotes.kt` | cualquiera | no | Sólo con una versión nueva publicada. |
| `Concede a Coindex permiso para instalar aplicaciones y vuelve a pulsar Instalar.` | `InstallMessages.kt` | cualquiera | no | Las cuatro de `installOutcomeMessage` acaban en algo que hacer (ADR 0011). |
| `Este dispositivo no permite conceder el permiso de instalación: descarga el APK desde GitHub e instálalo a mano.` | `InstallMessages.kt` | cualquiera | no | |
| `No hay instalador de paquetes en este dispositivo: instala el APK a mano.` | `InstallMessages.kt` | cualquiera | no | |

## Cubo 2 · Explica un porqué de producto

46 entradas. Aquí es donde la poda tiene sitio de verdad, y donde tiene que decidir entre **irse**,
**mudarse** a un onboarding que se lee una vez, o **quedarse** porque es la única vez que se dice.

| cadena | fichero | pantalla | siempre | nota |
| --- | --- | --- | --- | --- |
| «Referencia curada de las emisiones catalogadas de esta variante; no afirma que sea una serie cerrada.» (16 pal.) | `screens/PlateScreen.kt` | **Lámina** | **sí** | **La única prosa larga siempre visible dentro del cuaderno, en la pantalla que el padre exporta.** El primer candidato de la poda. |
| `Colecciones a partir de las piezas que tienes ahora mismo.` (10 pal.) | `screens/IndexScreen.kt` | Colecciones | **sí** | La frase que explica la derivación. Se lee una vez y se paga en cada arranque. |
| `Presupuesto de la API: N / M llamadas este mes` | `screens/IndexScreen.kt` | Colecciones | **sí** | La misma cuenta está en Ajustes con su párrafo. En el índice es la única línea que no habla de la colección. |
| `Última sincronización: hoy 18:47 · 231 piezas · 6 llamadas` (8 pal.) | `SyncMessages.kt` | Colecciones | **sí** | Discutible con el cubo 4: es un recibo, y un recibo es exactamente lo que un sello con la fecha dice en un cuarto del sitio. |
| `N piezas · N fichas nuevas · N llamadas` (+ ` · incompleto`) | `SyncMessages.kt` | Colecciones | no | El snackbar del sync. |
| `Se comparte cuando esté entero. Puedes cancelar sin perder nada.` | `screens/IndexScreen.kt` | Colecciones | no | |
| `Es lo que hay en el índice ahora mismo, con los filtros puestos.` (13 pal.) | `screens/ExportOptions.kt` | Colecciones | no | |
| `Cuaderno completo exportado · N páginas` (y las dos variantes con fotos que no cargaron) | `NotebookLabels.kt` | Colecciones | no | Tres cadenas: el recuento honesto del #67. |
| `Exportación cancelada en la página N de M. No se ha compartido nada.` (13 pal.) | `NotebookLabels.kt` | Colecciones | no | |
| `Exportación cancelada al descargar las fotos (N de M). Las descargadas se guardan para la próxima.` (16 pal.) | `NotebookLabels.kt` | Colecciones | no | |
| `No se pudo exportar el cuaderno: …` | `screens/NotebookExport.kt` | Colecciones | no | Escrita dos veces en el mismo fichero. |
| `Falta la API key de Numista. Añádela en Ajustes.` | `SyncMessages.kt` | Colecciones | no | Las nueve de `syncErrorLabel` traducen un fallo a lo que el padre puede hacer. |
| `Presupuesto de la API agotado este mes (N/M). Puedes subir el techo en Ajustes o esperar al día 1.` (19 pal.) | `SyncMessages.kt` | Colecciones | no | |
| `Sin conexión con Numista. Tu colección local sigue disponible.` | `SyncMessages.kt` | Colecciones | no | |
| `Numista rechazó tu API key. Revísala en Ajustes.` | `SyncMessages.kt` | Colecciones | no | |
| `Numista no encuentra ese identificador de usuario. Revísalo en Ajustes.` | `SyncMessages.kt` | Colecciones | no | |
| `Numista está limitando las peticiones. Vuelve a intentarlo dentro de un rato.` | `SyncMessages.kt` | Colecciones | no | |
| `Numista está caído ahora mismo. Tu colección local sigue disponible.` | `SyncMessages.kt` | Colecciones | no | |
| `Numista devolvió un error (N). Vuelve a intentarlo más tarde.` | `SyncMessages.kt` | Colecciones | no | |
| `Numista respondió algo que Coindex no entiende. Vuelve a intentarlo más tarde.` | `SyncMessages.kt` | Colecciones | no | |
| `Ficha de Numista N actualizada: el dato había cambiado.` | `FichaLabels.kt` | Monedas · Piezas | no | |
| `La ficha de Numista N sigue igual. Has gastado 1 llamada.` | `FichaLabels.kt` | Monedas · Piezas | no | |
| `Numista ya no publica el tipo N. La ficha que tenías sigue en el móvil.` (15 pal.) | `FichaLabels.kt` | Monedas · Piezas | no | |
| `Lámina no disponible` | `screens/PlateScreen.kt` | Lámina | no | |
| `No existe ese catálogo curado.` | `Labels.kt` | Lámina | no | |
| `Ya no tienes piezas de esta variante, así que esa colección no existe.` (13 pal.) | `Labels.kt` | Lámina | no | **Casi la misma frase que la de `PiecesScreen.kt` más abajo, en dos ficheros.** |
| `Aún no tienes ninguna emisión oficial de este catálogo.` | `Labels.kt` | Lámina | no | |
| `Lámina completa exportada · N casillas` (y las dos variantes con fotos que no cargaron) | `SheetLabels.kt` | Lámina | no | Tres cadenas, compartidas con la hoja de piezas (#219). |
| `No se pudo exportar la lámina` / `la hoja` | `SheetLabels.kt` | Lámina · Piezas | no | |
| `Ahora mismo no tienes ninguna de las piezas de esta colección. Sigue aquí por si vuelven.` (16 pal.) | `screens/PiecesScreen.kt` | Piezas | no | La caja sobrevive vacía (ADR 0021 §11). |
| `Ya no tienes piezas de esta variante, así que esta colección no existe. Vuelve al índice.` (16 pal.) | `screens/PiecesScreen.kt` | Piezas | no | |
| `Esta colección ya no existe. Vuelve al índice.` | `screens/PiecesScreen.kt` | Piezas | no | La tercera redacción del mismo hecho. |
| `Colección desconocida` | `screens/PiecesScreen.kt` | Piezas | no | |
| `Vienen elegidas las N que enseñaba el filtro. Quita las que no.` (12 pal.) | `components/PieceSelection.kt` | Monedas | no | |
| `Toca «Elegir» en cada moneda que quieras.` | `components/PieceSelection.kt` | Monedas | no | |
| `N/40 · tiene que caber en una tarjeta` | `BoxNaming.kt` | Monedas · Piezas | no | El contador explica el límite en cada pulsación de tecla. |
| `Son N caracteres y el límite son 40: tiene que caber en una tarjeta.` (14 pal.) | `BoxNaming.kt` | Monedas · Piezas | no | Repite el porqué que el contador de arriba ya lleva. |
| `Ya hay una colección que se llama «X». Ponle otro nombre.` | `BoxNaming.kt` | Monedas | no | |
| `Colección «X» creada.` | `BoxNaming.kt` | Monedas | no | |
| `Numista no guarda fecha de compra, así que este orden es el del alta en Numista.` (16 pal.) | `screens/IndexScreen.kt` | Colecciones | no | Sólo con «Alta más reciente» puesto: paga sólo quien lo elige. |
| `Ese enlace no describe ninguna variante de tu colección. Vuelve al índice.` (12 pal.) | `CoindexApp.kt` | cualquiera | no | |
| `Esta colección ya no existe: o has dejado de tener piezas de esta variante, o la ficha de Numista ha cambiado y sus monedas están ahora en otra colección. Vuelve al índice.` (32 pal.) | `CoindexApp.kt` | cualquiera | no | **El párrafo más largo de la app.** Nadie lo ve nunca salvo tras un `refreshFicha` que mueva la familia (#185). |
| `No se pudo arrancar` + «Los datos curados que viajan con la app no son válidos…» (23 pal.) | `CoindexApp.kt` | cualquiera | no | Pantalla de datos corruptos; no llega al padre si el APK está bien. |
| `Descargando la actualización…` | `InstallMessages.kt` | cualquiera | no | |
| `No se pudo descargar la actualización: …` | `InstallMessages.kt` | cualquiera | no | |
| `NUEVA VERSIÓN x.y.z` + las notas de la release | `CoindexApp.kt` | cualquiera | no | Las notas son contenido del manifiesto, no copy del repo. |

## Cubo 3 · Repite lo que el dato ya dice

20 entradas. Pocas, y de las que más veces se imprimen.

| cadena | fichero | pantalla | siempre | nota |
| --- | --- | --- | --- | --- |
| `Anverso` · `Reverso` | `components/FieldGuide.kt` | **Lámina · Piezas** + PNG | **sí, por casilla** | **La repetición más cara de la app: 16 palabras por pantalla de rejilla, 66 en la lámina de los Fuertes.** Las dos miniaturas están una al lado de otra y la lámina ya declara `printed_side`. Prueba de que se puede vivir sin ellas: **el cuaderno impreso no las imprime.** |
| `Inventario de campo · plata bullion` | `MastheadLabels.kt` | Colecciones · Monedas | **sí** | **Dos coletillas de identidad en la misma pantalla**, con la de abajo. |
| `Cuaderno de colección · Láminas de plata` | `screens/RootHeading.kt` | Colecciones · Monedas | **sí** | 6 palabras que dicen lo que la de arriba ya dijo con 5. |
| `Colecciones` (título) vs. `Colecciones · 70` (barra) | `screens/IndexScreen.kt`, `CoindexApp.kt` | Colecciones | **sí** | La palabra aparece dos veces en la pantalla de llegada, más el masthead. Igual con `Monedas`. |
| `70 colecciones` (recuento de la estantería) | `shelf/ShelfLabels.kt` | Colecciones | **sí** | **El tercero de los cuatro números de «cuánto tengo»** que la medida base encontró a la vista a la vez: `231 piezas`, `70 colecciones`, `Monedas · 192`, `574 monedas · 192 tipos`. |
| `N de M colecciones` · `N de M tipos` | `shelf/ShelfLabels.kt` | Colecciones · Monedas | no | |
| `574 monedas · 192 tipos` (frase de la cabecera de Monedas) | `Labels.kt` | Monedas | **sí** | El cuarto número. En Colecciones el mismo hueco lleva una frase de producto y aquí un recuento: la asimetría no la decidió nadie. |
| `En esta colección` / `En estas colecciones` | `screens/CoinsScreen.kt` | Monedas | sí, por fila | Debajo va la lista de nombres enlazados: la línea sólo la introduce. |
| `N# 1952` | `screens/CoinsScreen.kt` | Monedas | sí, por fila | El `N#` es el rótulo del número; en Piezas y en el papel el mismo dato se escribe `Numista 1952`. **Dos formas del mismo prefijo.** |
| `Numista N` (en `pieceLine`) | `Labels.kt` | Piezas + papel | sí, por fila | |
| `Tus piezas` | `screens/PiecesScreen.kt` | Piezas | **sí** | La pantalla se llama `Colección · X`, la cabecera cuenta las piezas y debajo hay una lista de piezas. |
| `Sin emitir` → `1 anunciada` / `N anunciadas` | `PlateSubject.kt` | Lámina + papel | no | Rótulo y valor son dos palabras para lo mismo. |
| `Sin ficha` → `1 emisión no medible` / `N emisiones no medibles` | `PlateSubject.kt` | Lámina + papel | no | Igual, y con el añadido de que `Sin ficha` es también un estado de casilla. |
| `Acabado sin confirmar` | `Labels.kt` | **Colecciones** | **sí** | **En 49 de las 68 tarjetas del padre**, siempre las mismas tres palabras. No dice nada de la moneda: dice que la ficha tiene un hueco. |
| `Sin confirmar` (acabado y metal, en la ficha de la lámina) | `Labels.kt` | Lámina | no | La misma ausencia, dos redacciones según haya rótulo encima o no. |
| `Conjunto de varias denominaciones` | `Labels.kt` | Colecciones · Lámina | no | La frase de un catálogo de set (ADR 0012). |
| `Sin metal dominante` | `Labels.kt` | Colecciones | no | |
| `Lámina · X` · `Colección · X` · `Ajustes` (subtítulo del masthead) | `MastheadLabels.kt` | cualquiera | **sí** | Cada pantalla ya se titula a sí misma dos dedos más abajo. |
| `· v0.16.0` | `MastheadLabels.kt` | cualquiera | **sí** | Y otra vez al pie de Ajustes. |
| `Cancelar` × 4 · `Sin colección` × 3 · `Cómo se llama` × 2 · `Quitar los filtros` × 2 | varios | varias | no | **Once literales para cuatro cadenas.** No es prosa de más, es copia sin dueño: la misma palabra escrita en cuatro ficheros puede divergir en la poda. |

## Cubo 4 · Podría ser forma

19 entradas. **El cubo pequeño donde está el trabajo**: si estas se convierten, el sitio para el
troquel y el sello sale de aquí.

| cadena | fichero | pantalla | siempre | nota |
| --- | --- | --- | --- | --- |
| `Tengo` · `Tengo · ×2` · `Me falta` · `Sin ficha` · `Sin emitir` | `PlateSubject.kt` | **Lámina** + PNG + papel | **sí, por casilla** | **8 rótulos por pantalla de rejilla y 22 por lámina.** Ya hay tres marcas para «me falta» —borde discontinuo, grayscale, rótulo— y [la medida base](medida-base-296.md) midió que sobre plata el grayscale casi no hace nada: la marca real es el borde. El rótulo es el candidato número uno del troquel. |
| `Progreso` → `22 / 22 emisiones` | `PlateSubject.kt` | **Lámina** + PNG + papel | **sí** | **En 22/22 no ocurre nada.** El completado es el estado que más pide sello y hoy es una fila de ficha con la misma tipografía que `4 / 11`. |
| `22 de 22` / `4 de 11 · te faltan 7` | `Labels.kt` | **Colecciones** | **sí** | La tercera línea de la tarjeta. Seis de las 70 están completas y la única diferencia es que faltan dos palabras. |
| `Ficha traída hoy` / `ayer` / `hace N días` / `hace N meses` / `hace N años` | `FichaLabels.kt` | **Monedas · Piezas** | **sí, por fila** | **192 veces en Monedas.** Una antigüedad es exactamente lo que un tono, una pátina o un punto dicen sin palabras — y la que no hay que decir en cada tarjeta. |
| `En ninguna colección` | `screens/CoinsScreen.kt` | Monedas | sí, por fila | Un estado de la moneda, en rojo óxido. Un borde o una marca de esquina lo dice igual. |
| `N piezas sin colección` | `screens/CoinsScreen.kt` | Monedas | sí, por fila | |
| `Sin año` | `Labels.kt`, `screens/CoinsScreen.kt` | Monedas · Piezas | no | Un hueco escrito. Dos ficheros, misma cadena. |
| `Medalla o ficha` | `Labels.kt` | Monedas | no | Se dice sólo cuando no es moneda (#40): es ya la excepción, y una excepción es una marca. |
| `Catálogo curado` (eyebrow) | `screens/PlateScreen.kt` | **Lámina** | **sí** | Un eyebrow *es* un sello impreso; hoy es texto en versalitas. |
| `Cuaderno de colección` (eyebrow del alta) | `screens/OnboardingScreen.kt` | alta | sí | |
| `COINDEX` | `CoindexApp.kt` | cualquiera | **sí** | El nombre del cuaderno en la cabecera: la cadena que más claramente quiere ser tipografía propia y no `titleLarge`. |
| `Abierta` / `Cerrada` como palabra vs. estado | `Labels.kt` | Colecciones | no | Discutible: hoy es cubo 1 (filtro) por decisión del ADR 0021 §3, y en el papel podría ser una marca del canto de la lámina. |
| `Leyendo tu colección…` | `screens/IndexScreen.kt` | Colecciones | no | Un esqueleto de tarjetas lo dice mejor. |
| `Página 3 de 90 · Fuertes` · `Descargando fotos · 12 de 340` · `Guardando el cuaderno · 90 páginas` | `NotebookLabels.kt` | Colecciones | no | Tres progresos en palabras, sin barra. |
| `N páginas · N láminas` (coste de la exportación) | `NotebookLabels.kt` | Colecciones | no | |
| `Exportando el cuaderno` (eyebrow) | `screens/IndexScreen.kt` | Colecciones | no | |
| `Bullion` · `Proof` · `Coloreado` · `Proof coloreado` · `Dorado` · `Envejecido` | `Labels.kt` | Colecciones · Lámina | **sí** | La línea de variante de cada tarjeta. Un acabado es la clase de dato que un sello dice: hoy son palabras en 68 tarjetas. |
| `Oro` · `Plata` · `Platino` · `Paladio` · `Cobre` · `Bronce` · `Latón` · `Cuproníquel` · `Níquel` · `Acero` · `Cinc` · `Aluminio` | `Labels.kt` | Colecciones | no | Sólo se dicen cuando **no** son plata (#40), o sea que ya funcionan como excepción marcada. Un color o un canto lo diría sin palabra. |
| `1 oz` · `0,25 oz` · `0,804 oz` | `Labels.kt` | Colecciones · Lámina + papel | **sí** | Contenido, no mobiliario; entra aquí porque el peso es el otro dato de la línea de variante y se decide con el acabado o no se decide. |

## Ajustes y el alta, aparte

El ticket manda marcar Ajustes aparte porque tiene excepción declarada. **La excepción no está
escrita en `spec.md §0.4` ni en ningún ADR**: lo más cerca que hay es el KDoc de `photoCacheLabel`
—«es una optimización, y una optimización que se anuncia se convierte en una tarea que supervisar»— y
el de `SettingsScreen`. Se marca aparte igualmente, y el #305 tiene que **escribir la excepción o
retirarla**, porque hoy es 98 de las 133 palabras de la pantalla más densa en prosa de la app.

Del **alta** el ticket no dice nada, y la medida base corrigió al mapa: existe, es la primera
pantalla de un teléfono nuevo, y son 73 palabras de las que 52 están en dos párrafos. Va aquí porque
comparte las cadenas del formulario con Ajustes, no porque esté excepta.

| cadena | fichero | pantalla | cubo | nota |
| --- | --- | --- | --- | --- |
| `Ajustes` (eyebrow) · `Credenciales y presupuesto` | `screens/SettingsScreen.kt` | Ajustes | 3 | El masthead ya dice `Ajustes` arriba. |
| «Se guardan cifradas en este teléfono y nunca salen de él. Si Numista rechaza tus sincronizaciones, la API key es lo primero que hay que revisar aquí.» (27 pal.) | `screens/SettingsScreen.kt` | Ajustes | 2 | |
| `API key de Numista` · `Identificador de usuario` · `Techo de llamadas al mes` | `screens/SettingsScreen.kt` | Ajustes | 1 | Las dos primeras, repetidas literalmente en el alta. |
| `Ocultar la API key` / `Mostrar la API key` | `screens/SettingsScreen.kt` | Ajustes | 1 | |
| «Llevas N llamadas este mes. La API gratuita de Numista ronda las 2.000, y el techo existe para que una tarde de pruebas no se coma el mes.» (27 pal.) | `screens/SettingsScreen.kt` | Ajustes | 2 | El sospechoso que la medida base fue a buscar. |
| `Guardar ajustes` | `screens/SettingsScreen.kt` | Ajustes | 1 | |
| `Fotos del catálogo` | `screens/SettingsScreen.kt` | Ajustes | 1 | |
| Las siete frases de `photoCacheLabel` («Las N fotos del catálogo están en este teléfono (X MB). Las láminas y el cuaderno se dibujan sin pedir nada.», «Se traerán cuando haya wifi…», …) | `PhotoCacheLabels.kt` | Ajustes | 2 | 20 palabras siempre visibles. Es la excepción dentro de la excepción: existe para distinguir «faltan 320 y están cayendo» de «faltan 320 porque estás con datos». |
| `Cerrar sesión` (título) + `Cerrar sesión` (botón) | `screens/SettingsScreen.kt` | Ajustes | 1 | La misma palabra dos veces en la misma tarjeta. |
| «Borra la API key y el identificador de este teléfono y vuelve al alta. Las piezas ya sincronizadas se quedan donde están.» (22 pal.) | `screens/SettingsScreen.kt` | Ajustes | 2 | |
| `Coindex v0.16.0` | `screens/SettingsScreen.kt` | Ajustes | 3 | La versión ya está en el masthead de esta misma pantalla. |
| `La API key no puede estar vacía.` | `SettingsEntry.kt` | Ajustes | 1 | |
| `El identificador de usuario es el número de la URL de tu perfil de Numista.` (15 pal.) | `SettingsEntry.kt` | Ajustes | 2 | Y el alta lo dice otra vez con otras palabras. |
| `El techo de presupuesto tiene que ser un número de llamadas mayor que cero.` (14 pal.) | `SettingsEntry.kt` | Ajustes | 2 | |
| `Ajustes guardados.` | `SettingsEntry.kt` | Ajustes | 2 | |
| `Cuaderno de colección` (eyebrow) · `Coindex` | `screens/OnboardingScreen.kt` | alta | 4 | |
| «Introduce tu API key de Numista y tu identificador de usuario. Se guardan cifrados en este teléfono y nunca salen de él: cada colección consume su propio presupuesto de la API.» (31 pal.) | `screens/OnboardingScreen.kt` | alta | 2 | **El párrafo más largo siempre visible de la app**, y dice lo mismo que el de 27 de Ajustes. |
| `API key de Numista` · `Identificador de usuario` | `screens/OnboardingScreen.kt` | alta | 1 | |
| `Guardar y empezar` | `screens/OnboardingScreen.kt` | alta | 1 | |
| «La API key se obtiene en numista.com › Mi perfil › API. El identificador de usuario aparece en la URL de tu perfil.» (21 pal.) | `screens/OnboardingScreen.kt` | alta | 2 | La única de las cuatro que enseña a hacer algo que no se puede adivinar. |
| `Introduce una API key y un identificador de usuario válidos.` | `SettingsEntry.kt` | alta | 1 | Una sola frase para los dos campos, a propósito. |

## El papel y el PNG: la copia que sale de la app

9 entradas que **el padre reparte**: viajan en el PNG que exporta y en las 90 páginas A4 del
cuaderno. Cualquier poda que las toque cambia lo que él enseña, y a la vez son la prueba de que la
app puede decir menos: **el papel ya dice menos que la pantalla.**

| cadena | fichero | dónde | cubo | nota |
| --- | --- | --- | --- | --- |
| `COINDEX · CATÁLOGO CURADO` | `print/NotebookSections.kt`, `screens/PlateSheet.kt` | papel · PNG de lámina | 4 | Escrita dos veces, una por drawer. |
| `COINDEX · COLECCIÓN` | `print/NotebookSections.kt`, `screens/PiecesSheet.kt` | papel · PNG de hoja | 4 | Idem. |
| `COINDEX · SIN COLECCIÓN` + `Sin colección` (título) | `print/NotebookSections.kt` | papel | 3 | El eyebrow y el título dicen lo mismo en la misma banda. |
| `País` · `Piezas` · `Variante` | `PiecesLabels.kt`, `print/NotebookSections.kt` | papel · PNG · Piezas | 3 | Tres rótulos de ficha escritos en dos sitios; `Piezas` es además el título de la sección de al lado. |
| `tu colección en Numista` · `Fuente: X` / `Fuentes: X · Y` | `print/PrintedLabels.kt`, `screens/PiecesSheet.kt` | papel · PNG | 2 | La procedencia sobrevive a la app; por eso está en el papel (#232). |
| `Pieza N` | `PiecesSubject.kt`, `print/NotebookSections.kt` | papel · PNG · Piezas | 3 | El nombre de una pieza sin ficha ni título: el id crudo. Escrito tres veces. |
| `PÁGINA N DE M` | `screens/NotebookSheet.kt` | papel | 4 | Sólo donde hay corte, nunca «página 1 de 1». |
| `50 MM · ESCALA 1:1` | `screens/NotebookSheet.kt` | papel | 4 | La regla al pie; la palabra al lado de la forma que ya lo dice. |
| `38,6 mm` | `print/PrintedLabels.kt` | papel | 2 | Se dice **porque** la página no está a tamaño real (#231, #233): lo que una regla no puede medir se mide en palabras. |
| `Progreso`, `Tengo`, `Me falta`, `Anverso`… | `PlateSubject.kt`, `FieldGuide.kt` | ver cubos 3 y 4 | | El papel hereda las casillas y **no** los `Anverso`/`Reverso`: la pantalla es la única que los imprime. |

## Cadenas que nadie llega a leer

Ocho entradas, y son un hallazgo y no un hueco.

| cadena | fichero | por qué no se lee |
| --- | --- | --- |
| `la lámina aún no se ha dibujado` | `PlateExport.kt:43` | Mensaje de un `require`. Está en español y parece copy; es una aserción para quien lea un stack trace. |
| `la página aún no se ha dibujado` | `print/NotebookPdf.kt:40` | Igual. |
| `un folio sin ninguna lámina no es una página` | `print/PrintPages.kt:174` | Igual, en un `init`. |
| `Coindex` (sin versión) | `screens/SettingsScreen.kt:150` | Sólo con `versionName` vacío, y `installedVersionName()` sólo devuelve vacío si el `PackageManager` falla: en un APK instalado nunca. |
| El subtítulo del masthead sin `· vX` | `MastheadLabels.kt:25` | La misma rama muerta, por el mismo motivo. |
| `Lámina` y `Colección` a secas | `MastheadLabels.kt:15,18` | Sólo con `subjectName` nulo, que es exactamente cuando la pantalla de debajo ya dice `Lámina no disponible` o `Colección desconocida`. |
| `Compartir lámina` · `Compartir el cuaderno` | `PlateExport.kt:60`, `print/NotebookPdf.kt:63` | Título del `Intent.createChooser`. La hoja de compartir del sistema lo ignora desde Android 10 (API 29), y `minSdk = 29`: ningún dispositivo soportado lo enseña. |
| `Cinc` · `Aluminio` · `Paladio` · `Latón`… | `Labels.kt` | Reachable en código, imposible con estos datos: ningún tipo sembrado de las dos colecciones tiene esos metales. No se borran —el criterio del curador manda— pero no son palabras que ocupen sitio. |

## Lo que este informe no dice

- **No propone recortes.** Qué se va, qué se muda y qué se convierte en forma es el
  [#305](https://github.com/jenarvaezg/coindex/issues/305). Las notas de la columna de al lado son
  material para esa discusión, no la discusión.
- **No cuenta palabras en pantalla.** Eso ya lo hizo [la medida base](medida-base-296.md) con el
  volcado de accesibilidad: 1636 palabras de copy en la capa `ui/`, 81 en la pantalla de llegada del
  índice. Aquí se cuentan cadenas, que es la unidad que la poda decide.
- **No entra en el dominio ni en los datos.** Los nombres de catálogo, los `short_name`, los países y
  los títulos de tipo vienen de `data/` y de Numista: son contenido, y ninguna poda de copy los toca.
- **No mira los tests.** Los trece ficheros de test que fijan estas cadenas (1171 líneas) se moverán
  con ellas; el [#306](https://github.com/jenarvaezg/coindex/issues/306) es quien decide qué test se
  pone rojo cuando la prosa vuelve.

## Cómo se reproduce

```bash
# las cadenas, con fichero y línea, sin KDoc ni comentarios
rg -n '"[^"]+"' app/src/main/kotlin/com/jenarvaezg/coindex/ui --type kotlin

# los sitios donde se lee cada rótulo, que es lo que dice en qué pantalla se ve
rg -n '\b(variantLabel|coverageLabel|fichaAgeLabel|plateSubject)\b' app/src/main/kotlin
```

La columna **pantalla** se resolvió siguiendo cada función de rótulo hasta su `@Composable`, no
leyendo el nombre del fichero: `FichaLabels` suena a lámina y se ve en Monedas y en Piezas, y
`Labels.kt` reparte cadenas por las cinco pantallas y por el papel.
