# Cada «Buscar» dice dónde busca, y el vacío responde a lo que se puso (#515)

Tres pantallas dibujaban la misma caja con la misma palabra sobre tres poblaciones distintas: las
tarjetas del índice, los tipos del inventario y las láminas curadas que el coleccionista no tiene.
Sólo «Explorar» decía cuál era la suya. Debajo, una tarjeta vacía respondía «Ninguna colección pasa
por lo que has puesto» a quien sólo había escrito, y ofrecía «Quitar los filtros», que además
vaciaba la caja aunque el nombre no lo dijera. Y al pie, la puerta del anexo seguía diciendo 55
láminas sobre un índice que la búsqueda había dejado en cero.

## Lo medido

`coindex-chrome` (Pixel 7, 1080 × 2400 a 420 dpi), base restaurada con `scripts/avd-db.sh restore`
(5 colecciones, 15 tipos, 55 láminas en el escaparate), misma navegación en las dos versiones:
teclear `zzz` en la caja de cada jerarquía.

| | Colecciones | Monedas |
| --- | --- | --- |
| antes (1.5.0) | ![antes](antes-colecciones.jpg) | ![antes](antes-monedas.jpg) |
| después | ![después](despues-colecciones.jpg) | ![después](despues-monedas.jpg) |

Y las tres cosas del issue a la vez (la caja, el vacío y la puerta) en una pantalla:

| | Colecciones | Monedas |
| --- | --- | --- |
| antes (1.5.0) | ![antes](antes-vacio.jpg) | ![antes](antes-monedas-vacio.jpg) |
| después | ![después](despues-vacio.jpg) | ![después](despues-monedas-vacio.jpg) |

Medido sobre los PNG a 1080 de `screencap`, antes de comprimirlos a JPEG para el repositorio:

| | antes | después |
| --- | --- | --- |
| tinta del *placeholder* del índice | 61 → 265 px | 61 → 684 px |
| blanco que le queda en la caja | 756 px · 288 dp | 337 px · 128 dp |
| frase del vacío | una, para tres casos | tres, una por caso |
| botón del vacío | «Quitar los filtros», siempre | el del estrechamiento, o ninguno |
| alto de la puerta del anexo | 111 px · 42 dp | 164 px · 62 dp mientras se escribe |

## El posesivo distingue las cajas

«Buscar entre tus colecciones», «Buscar entre tus monedas», «Buscar entre las láminas». La tercera
no lleva posesivo a propósito: el estante de «Explorar» está hecho de lo que el coleccionista no
tiene (ADR 0030 §1), y un «tus» ahí sería falso.

El parámetro `placeholder` de `SearchField` perdió su valor por defecto, que era lo que permitía a
dos de las tres cajas no declararse: quien dibuje esa caja tiene que decir sobre qué busca.

## Las tres buscan igual

La caja de «Explorar» no usaba `matchesQuery` sino un `contains` pelado: sensible a acentos y ciega
a dos palabras en cualquier orden. «aguila» no encontraba «Águila» y «plata aguila» no encontraba
nada, mientras las otras dos sí. Ninguna declaración de alcance lo habría hecho visible; se arregla
en `showcaseShelf`, que ahora dobla el nombre con `fold` como todo lo demás.

## Tres estrechamientos

`ShelfNarrowing` es lo que se ha puesto: los chips, la palabra, las dos o ninguna. Son cosas
distintas (los filtros sobreviven a un lanzamiento y se esconden tras un estante plegado; la palabra
se escribe en una caja siempre a la vista y se va con la aplicación, ADR 0021 §1), y el vacío las
distingue:

- filtros: «Ninguna colección pasa por los filtros.» → «Quitar los filtros»
- búsqueda: «Ninguna colección responde a lo que has escrito.» → «Borrar la búsqueda»
- las dos: «Ninguna colección pasa por lo que has puesto.» → «Quitar los filtros y la búsqueda»

La frase de antes queda sólo donde era cierta: «lo que has puesto» engloba un chip y una palabra a
la vez, y el botón de debajo nombra las dos. El verbo cambia con el sujeto porque son actos
distintos: una tarjeta *pasa* por un chip y *responde* a una palabra escrita.

El botón se llama como el acto que deshace: «Borrar la búsqueda» es el nombre que ya tenía el aspa
de la caja (`SEARCH_CLEAR_LABEL`). Y deshace sólo eso: `withoutFilters()` quita los chips sin
llevarse el eje y el orden, que no estrechan nada y que el `IndexShelf()` de antes tiraba de paso
(quien leía la hoja por país y pulsaba un botón sobre filtros volvía al eje por lámina).

Hay un cuarto caso que no es un estrechamiento. Los ejes de país y año pueden quedarse vacíos con el
estante limpio, y ahí se leía «pasa por lo que has puesto» sobre un botón que ofrecía quitar nada.
Ahora dice «Ninguna colección aparece en este eje.» y no ofrece salida, porque no la hay.

## La puerta avisa de que la caja no llega hasta ella

Su recuento se mide sobre la colección entera y nunca sobre el estrechamiento, y es correcto: lo que
hay detrás no está en la lista de arriba (el escaparate son láminas de las que no se tiene nada, y
las marcas son casillas y no tarjetas). Pero no debe parecer un número sin recalcular, así que lo
dice: «Lo que escribes arriba no llega hasta aquí.»

Dice «lo que escribes» y no «tu búsqueda» porque una puerta más adentro hay una pantalla llamada «Lo
que busco», y una línea sobre buscar junto a ella se leería como si hablara de ella.

Sólo aparece mientras hay algo escrito. Los filtros tampoco llegan al escaparate, pero sobreviven a
un lanzamiento: nombrarlos aquí imprimiría la línea en cada pantalla de cada sesión de quien dejó
puesto el chip de país, la frecuencia que limita el ADR 0026 §5. La búsqueda es lo que se está
haciendo ahora, en una caja a la vista, sobre una lista que puede haberse quedado en cero.

La nota va en `bodySmall` y no en el cuerpo del #513: allí la línea contestaba al control de encima,
y aquí va debajo del nombre de la puerta, que tiene que seguir siendo lo más alto de la fila. Está
dentro del área táctil, porque habla de la propia fila.

## Lo que queda fuera

- No hay captura del vacío por filtros solos porque en esta colección no se puede llegar: cada
  faceta cuenta sus chips con su propia elección descartada (`indexFacetCounts`), así que el estante
  nunca ofrece una combinación que devuelva cero (elegido «España · 1», la faceta de año sólo ofrece
  1966). Se alcanza cuando un filtro sobrevive a un lanzamiento y la colección cambia debajo. Lo
  cubre `ShelfLabelsTest.an empty shelf names the narrowing that emptied it`.
- «Explorar» no cambia de vacío. Ya decía «Ninguna lámina se llama así», que responde a lo que se
  puso, y no tiene chips que ofrecer quitar (ADR 0030 §8): la caja con su aspa está encima y es toda
  la salida. Sí cambió cómo compara.
