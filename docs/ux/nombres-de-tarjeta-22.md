# Nombres de tarjeta: la pasada de curación de los 52 ficheros

Asset de [#22](https://github.com/jenarvaezg/coindex/issues/22). Este fichero **no** es la decisión
—esa vive en la resolución del ticket y se escribirá en el ADR 0021 desde
[#25](https://github.com/jenarvaezg/coindex/issues/25)—: es la pasada de curación que la decisión
obliga, revisada moneda a moneda, para que la sesión que la implemente aplique un commit mecánico y
no improvise 52 nombres.

## Las tres reglas

1. **`short_name` es obligatorio** en los 50 catálogos de `data/collection-catalogs/` y en las 2
   agrupaciones curadas de `data/groupings/`. La validación falla al arrancar si falta.
2. **Es único** entre los 52. Es la regla que hace el trabajo: obliga a desambiguar en vez de
   confiar en un corte mecánico que iguala doce ficheros en cinco nombres.
3. **Es prefijo literal de `name`.** Se valida sola y evita que el nombre corto derive del largo.

`schema_version` **no se toca**: no es una versión de formato sino el discriminador de especie de
catálogo (`CollectionCatalog.kt:9-16` — 1 tipo único, 2 date run, 3 set, 5 issue run).

## La regla de idioma que la pasada destapó

El corpus mezclaba idiomas sin criterio escrito. El que se sigue aquí, y que el ADR debería recoger:

- **El nombre de la serie se queda en el idioma de la ceca cuando ya está en alfabeto latino** y es
  el nombre comercial que el coleccionista reconoce: `Silver Britannia`, `Noah's Ark`,
  `Vienna Philharmonic`, `American Silver Eagle`, `The Queen's Beasts`.
- **Se traduce cuando el original no está en alfabeto latino**, porque ahí la traducción *es* el
  nombre legible: `Libro Rojo de Rusia`, `Personalidades destacadas de Rusia`,
  `Monumentos arquitectónicos de Rusia`.
- **Todo lo que escribe el curador alrededor va en español**, incluidos los países: `Canadá`,
  `Países Bajos`, `Sudáfrica`, `EE. UU.`, y **`Ruanda`, no `Rwanda`**.

## La tabla

`name` en negrita = cambia. 26 de los 52 se tocan; los otros 26 solo ganan el campo nuevo.

| fichero | `short_name` | `name` |
| --- | --- | --- |
| architectural-monuments-russia-3-roubles | Monumentos arquitectónicos de Rusia | sin cambio |
| armenia-noahs-ark-1oz-bullion | Noah's Ark 1 oz | **Noah's Ark 1 oz · Armenia · bullion anual desde 2011** |
| armenia-noahs-ark-half-oz-bullion | Noah's Ark ½ oz | **Noah's Ark ½ oz · Armenia · bullion anual desde 2011** |
| armenia-noahs-ark-quarter-oz-bullion | Noah's Ark ¼ oz | **Noah's Ark ¼ oz · Armenia · bullion anual desde 2011** |
| australia-silver-kangaroo-1oz-bullion | Australian Kangaroo | sin cambio |
| australian-koala-perth-1oz | Australian Koala | sin cambio |
| australian-kookaburra-perth-1oz | Australian Kookaburra | sin cambio |
| austria-vienna-philharmonic-1oz-bullion | Vienna Philharmonic | sin cambio |
| canada-dolar-conmemorativo-plata-500 | Dólar conmemorativo de plata .500 | sin cambio |
| canada-dolar-plata-800 | Dólar de plata .800 | sin cambio |
| canada-silver-maple-leaf-1oz-bullion | Silver Maple Leaf | sin cambio |
| china-silver-panda-1oz-bullion | Panda de plata 1 oz | **Panda de plata 1 oz · China · bullion anual 1989-2015 (sin proof de colección separada, conmemorativas de banco/expo, ni 1983-1987 de otra métrica)** |
| china-silver-panda-30g-bullion | Panda de plata 30 g | **Panda de plata 30 g · China · bullion anual desde 2016 (sin proof de colección separada ni conmemorativas de banco/expo)** |
| equilibrium-pressburg-1oz-silver | Equilibrium | sin cambio |
| espana-capitales-de-provincia-5-euros | Capitales de provincia | **Capitales de provincia y ciudades autónomas · España · 5 € de plata** |
| espana-paquillos | Paquillos | **Paquillos · España · 100 pesetas de Franco** |
| lion-eagle-uk-1oz-bullion | The Lion and the Eagle | sin cambio |
| lunar-ii-perth-1oz-bullion | Lunar Series II | sin cambio |
| lunar-iii-perth-1oz-bullion | Lunar Series III bullion | **Lunar Series III bullion · Perth Mint · 1 oz** |
| lunar-iii-perth-1oz-proof-coloured | Lunar Series III proof coloured | **Lunar Series III proof coloured · Perth Mint · 1 oz** |
| mexico-libertad-1oz-bullion | Onza Libertad | sin cambio |
| mexico-onza-troy-925 | Onza Troy | sin cambio |
| nikola-tesla-serbia-1oz | Nikola Tesla | **Nikola Tesla · Serbia · 1 oz de plata** |
| niue-southern-cross-1oz-bullion | Southern Cross | sin cambio |
| outstanding-personalities-russia-2-roubles | Personalidades destacadas de Rusia | sin cambio |
| paises-bajos-10-gulden-beatrix | 10 gulden de Beatrix | **10 gulden de Beatrix · Países Bajos · conmemorativos 1994-1999** |
| portugal-1000-escudos-plata-500 | 1000 escudos de plata .500 | **1000 escudos de plata .500 · Portugal 1992-2001 · conmemorativos** |
| portugal-1983-exposicion-europea-de-arte | XVII Exposición Europea de Arte | sin cambio |
| portugal-500-escudos-plata-500 | 500 escudos de plata .500 | **500 escudos de plata .500 · Portugal 1995-2001 · conjunto anual conmemorativo** |
| queens-beasts-uk-2oz | The Queen's Beasts | **The Queen's Beasts · Reino Unido · 2 oz de plata** |
| red-data-book-russia | Libro Rojo de Rusia | sin cambio |
| rwanda-lunar-50-francs | Lunar Ounce de Ruanda | **Lunar Ounce de Ruanda · 50 francos de plata** |
| rwanda-nautical-50-francs | Nautical Ounce de Ruanda | **Nautical Ounce de Ruanda · 50 francos de plata** |
| saltwater-crocodile-australia-1oz | Australian Saltwater Crocodile | **Australian Saltwater Crocodile · Australia · 1 oz de plata** |
| south-africa-silver-krugerrand-1oz-bullion | Silver Krugerrand | sin cambio |
| spain-face-value-18g | Plata a valor facial | sin cambio |
| st-george-dragon-uk-1oz-bullion | St George and the Dragon | sin cambio |
| tudor-beasts-uk-1oz-proof | The Royal Tudor Beasts 1 oz | **The Royal Tudor Beasts 1 oz · Reino Unido · proof** |
| tudor-beasts-uk-2oz-bullion | The Royal Tudor Beasts 2 oz | **The Royal Tudor Beasts 2 oz · Reino Unido · bullion** |
| uk-silver-britannia-1oz-958 | Silver Britannia 1 oz .958 | **Silver Britannia 1 oz .958 · Reino Unido · cerrada 1998-2012 (sin 1997 proof ni plated)** |
| uk-silver-britannia-1oz-bullion | Silver Britannia 1 oz .999 | **Silver Britannia 1 oz .999 · Reino Unido · bullion anual desde 2013 (sin proof, BU de campos lisos, Oriental Border ni Coronation; el privy del mismo tipo rellena el año; 2023 parte Elizabeth/Charles)** |
| uk-silver-britannia-quarter-oz-bullion | Silver Britannia ¼ oz | **Silver Britannia ¼ oz · Reino Unido · .999 bullion (años con emisión bullion verificada; sin proof ni Gairsoppa; 2023 parte Elizabeth/Charles)** |
| united-russian-state-500th-3-roubles | 500 años del Estado ruso unificado | **500 años del Estado ruso unificado · 3 rublos** |
| us-american-silver-eagle-1oz-bullion | American Silver Eagle | sin cambio |
| us-independence-250th-spain-10-euros | 250.º aniversario de la Independencia | **250.º aniversario de la Independencia de EE. UU. · España · 10 € de plata** |
| venezuela-1-bolivar | 1 Bolívar | sin cambio |
| venezuela-2-bolivares | 2 Bolívares | sin cambio |
| venezuela-fuertes | Fuertes | **Fuertes · Venezuela · plata 25 g · 1876-1936** |
| venezuela-medios | Medios | sin cambio |
| venezuela-reales | Reales | sin cambio |
| uk-royal-mint-1oz-silver-sueltas | Onzas sueltas de la Royal Mint | **Onzas sueltas de la Royal Mint · Reino Unido · 2 £ de plata** |
| us-classic-silver-dollar | Dólar de plata clásico | sin cambio |

`short_name` más largo: **37 caracteres** («250.º aniversario de la Independencia»). Mediana 19,
mínimo 7 («Fuertes»). Los 52 caben en una línea de tarjeta.

## Los defectos que la pasada encontró

Ordenados por gravedad. Los cinco primeros los arregla la tabla de arriba; el último no es nuestro.

1. **«Conjunto anual» era el nombre de una tarjeta.** `portugal-500-escudos-plata-500` tenía
   `name: 'Conjunto anual · 500 escudos de plata .500 · Portugal 1995-2001'`, así que con la regla
   de prefijo el padre habría leído **«Conjunto anual»** en la tarjeta: una especie de catálogo, no
   un nombre. El cualificador se va al final.
2. **Dos pares ya salían con título idéntico en el índice, hoy, sin necesidad de cortar nada.**
   `lunar-iii-perth-1oz-bullion` y `lunar-iii-perth-1oz-proof-coloured` declaran la **misma
   `family`** («Lunar Series III»), y las dos Tudor Beasts también. Son cuatro tarjetas con dos
   títulos, distinguibles solo por acabado y peso, que la tarjeta no muestra. Es el mismo defecto
   que los cinco escudos de [#157](https://github.com/jenarvaezg/coindex/issues/157) por el mismo
   motivo, y la unicidad de `short_name` lo cierra por construcción.
3. **La `family` del Krugerrand lleva dentro una cláusula de exclusión**:
   `'Silver Krugerrand bullion anual desde 2018 (sin 2017 Premium Uncirculated)'`. Una `family` es
   identidad y clave de agrupación, no una definición de alcance — eso es trabajo de `name`, que ya
   lo dice. Debe quedar en `'Silver Krugerrand bullion anual'`. Desde
   [#21](https://github.com/jenarvaezg/coindex/issues/21) renombrar una familia no cierra ninguna
   lámina, así que el cambio es gratis. **Es el único cambio de `family` de esta pasada.**
4. **Ortografía y consistencia del español.** «Capitales de Provincia y Ciudades Autónomas» estaba
   en *Title Case* inglés; «Estado Ruso Unificado» igual; «250 aniversario» le faltaba el ordinal
   («250.º»); `Rwanda` estaba sin traducir en dos ficheros.
5. **Tres tarjetas decían «1 oz» sin decir de qué.** `nikola-tesla-serbia-1oz`,
   `queens-beasts-uk-2oz` y `saltwater-crocodile-australia-1oz`, cuando sus 49 hermanos dicen «de
   plata» o «bullion». Al cocodrilo le faltaba además el segmento de país que tienen todos.
6. **Una errata que no es nuestra.** La familia de Numista de N#104170 (Gibraltar) es
   `'Charlemagme - Mounted Knight'` — «Charlemagme» por Charlemagne. No se tapa con un alias en
   código: se abre como issue contra Numista. Secundarios del mismo tipo, menos claros: N#31925
   trae `'Ibero-American'` truncado y N#33 trae
   `'1190e anniversaire du couronnement de Charlemagne (800-1990).'` con punto final.

## Lo que la pasada deja para el que implemente

- **Los seis alias editoriales de `Family.kt:33-40` mueren enteros.** Cinco son de familias con
  catálogo y pasan a ser `short_name` de su fichero. El sexto, `SML` → «Silver Maple Leaf», es
  **inalcanzable con los datos sembrados**: sus 6 tipos están todos dentro de
  `canada-silver-maple-leaf-1oz-bullion`, que declara `family: Silver Maple Leaf bullion anual`, y
  por el ADR 0016 el catálogo manda.
- **Sobrevive una sola regla de etiquetado en código**, `collectionProposalFamilyLabel`:
  `System 1879-1936` → «Sistema monetario 1879-1936». No es un alias editorial sino el formateo de
  una cadena generada, y el ADR 0012 §41 ya dice que el sistema técnico llega al usuario solo por
  esa vía.
- **Las 18 tarjetas de familia cruda de Numista no ganan nada.** Se pintan verbatim, en el idioma
  en que Numista las escribió, y arreglar una fea es curar un fichero o abrir un issue.
