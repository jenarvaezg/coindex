# Prototipo · «Explorar», la puerta a las láminas que no coleccionas (#279)

Maqueta de forma para el ticket
[#279](https://github.com/jenarvaezg/coindex/issues/279) del mapa
[#15](https://github.com/jenarvaezg/coindex/issues/15). La pregunta que contesta **no** es «¿cómo se
pinta una pantalla nueva?», sino la que el ADR 0021 §7 dejó escrita y sin decidir:

> *Cutting the toll does **not** open the 51 catalogs to navigation — that would be new capability
> against ADR 0007, and is not decided here.*

Cinco variantes a dp real, el estado de hoy entre ellas como listón. En HTML y no en Compose porque
lo que se estaba eligiendo era **estructura**, y en Compose habría salido una variante por sesión.

## Lo elegido

**La A, con la puerta al final del índice, y en fantasma** — dibujado luego en
`e1`…`e4`. Decidido el 12 de agosto de 2026 con la
maqueta delante.

- La pantalla propia de la A gana el orden y el buscador que una hoja no puede tener: las veinte
  están todas a 0/N, así que el orden «por razón» del estante no dice nada de ellas.
- La puerta va donde la D ponía su hoja —una fila al final del índice— así que se conserva el camino
  de descubrimiento de la D y se le añade una pantalla que sí se puede ordenar y buscar.
- **En fantasma**, con el alfa 0,14 y el círculo de puntos de `AlbumPaper.kt`. La sesión propuso
  «a plena luz» —en un escaparate no falta nada, la moneda no es tuya y nunca lo ha sido— y **se
  descartó**: el fantasma mantiene el idioma del álbum y dice la verdad. Las capturas `-luz` quedan
  como registro de la alternativa descartada, no como propuesta viva.

## El camino elegido, en cuatro pantallas

Segunda maqueta (`build_elegida.py`), hecha después de elegir y sobre el mismo andamio: no son
cuatro alternativas, son los cuatro pasos de un recorrido.

| | qué es | en el pliegue |
| --- | --- | --- |
| `e1-puerta.jpg` | El final del índice y la puerta que sale de él | 13,6 tarjetas |
| `e2-explorar.jpg` | Explorar, con cromo de pantalla interior y ninguna celda nueva | 12,0 de 20 |
| `e3-estante.jpg` | Su estante propio, ordenado por coste de entrar: de 100 a 576 € | 9,0 de 13 |
| `e4-lamina.jpg` | La lámina que hoy contesta «Aún no tienes ninguna emisión oficial» | 6,5 de 10 |

Tres cosas que sólo se vieron al dibujarlo, y que no decide este ticket:

1. **«Exportar la lámina» sobra en una lámina que no tienes.** El PNG serían diez huecos vacíos.
   La acción primaria del `PlateScreen` tiene que saber si la lámina es tuya.
2. **La cabecera de la lámina puede llevar dos cifras de dinero**: el valor de lo que hay dentro
   (`plateValueLabel`, hoy) y el coste de cerrarla. En estas veinte la primera es cero, así que no
   compiten; en una lámina a una casilla, sí.
3. **La faceta de país no se gana el sitio**: veinte láminas dan doce países y nueve de ellos con
   uno. El orden por coste y por casillas sí.

## Las variantes

| | tesis | en el pliegue |
| --- | --- | --- |
| `v0.jpg` | **Hoy**: las veinte no existen (`PlateUnavailable.NoEvidence`) | 12,2 tarjetas |
| `vA.jpg` | **El estante ajeno**: es el mismo objeto, sólo que no es tuyo | 12,4 de 20 |
| `vB.jpg` | **El escaparate**: lo que engancha es la moneda, no el catálogo | 18,4 de 20 |
| `vC.jpg` | **La carta**: cada lámina es una decisión de compra | la lámina entera, sin scroll |
| `vD.jpg` | **La contraportada**: la puerta es un pie del álbum | 11,3 |

`vA-luz.jpg` y `vB-luz.jpg` son las mismas dos con el fantasma apagado: la alternativa que se midió
y se descartó.

## Cómo está hecha

`extract.py` saca los datos y las fotos; `build.py` escribe la maqueta de las cinco variantes y
`build_elegida.py` la del camino elegido, heredando de la primera el cromo, los datos y el listón
para que las dos no se separen. Ninguno de los dos vive en el
APK ni en el pipeline: son la maqueta, y se borran cuando dejen de hacer falta.

- **411 × 914 dp**, 1 px CSS = 1 dp, el Pixel 7 de las capturas del #296.
- **El cromo se leyó en el código, no se supuso**: el canto cosido de `AlbumChrome.kt` (54 dp), la
  cabecera de `CoindexApp.Masthead`, las **tres** celdas de `HierarchyBar` y la tarjeta de 104 dp de
  `IndexScreen.kt`. Las dos tipografías del APK, subseteadas a woff2.
- **Datos reales**: los 75 catálogos de `data/` cruzados con la colección del padre (229 filas).
  Evidencia y casilla llena con `CollectionCatalog.isEvidencedBy` y `memberMatches`; la cara de cada
  casilla con `printed_side` y, si el catálogo no lo declara, el reverso primero, como
  `AlbumFaces.kt`. Al padre le quedan **26 láminas invisibles**, **20** de ellas por debajo de las 20
  casillas.
- **Los importes son el suelo de la plata**, no precio de catálogo: gramos × ley leída del
  `composition.text` (`Valuation.silverFineness`) × 57,46 €/oz, spot del 12 de agosto de 2026. La app
  enseñaría el mayor de tres precios, así que **los de verdad son más altos**. Trece de las veinte
  los tienen completos.

## Lo que la maqueta no prueba

La réplica de hoy da **12,2 tarjetas** en el pliegue contra las **11,04** que mide el emulador: el
buscador y el estante van a ojo, así que la maqueta es un 10 % más densa que la app. Sirve para
juzgar estructura, no para cerrar densidad — y **nada de esto se ha visto en un teléfono**
(`medir-en-el-movil-no-en-el-asset`).
