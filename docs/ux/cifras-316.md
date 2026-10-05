# Las cifras: la analítica es una página, y lo que vale una pieza es el mayor de tres números

Respuesta del [#316](https://github.com/jenarvaezg/coindex/issues/316), decidida el 8 de agosto de
2026 sobre las dos colecciones reales y la API de Numista consultada en vivo (223 emisiones propias
y 35 huecos de muestra).

Las cifras absolutas en euros no están en este documento: el repositorio es público y el
patrimonio del coleccionista no. Aquí van el método, las coberturas y las proporciones; los
importes viven en `/private/tmp/coindex-privado/cifras-316.txt`, junto a las capturas del sync.

## Dos premisas del ticket estaban mal

El ticket planteaba «`grade` al 100 % y `price` al 37 %» como dos datos a medias que había que
dibujar o tirar. Medido:

- **`price` no cubre el 37 % sino el 16 %.** Son 84 filas de 229, pero esas 84 filas son 91 piezas
  de 572, porque lo que no tiene precio son los bultos venezolanos (x102, x59, x44…). La ausencia
  se concentra donde están cinco de cada seis piezas.
- **`grade` es la clave de tasación.** Numista publica precio estimado por emisión y por grado en
  `/types/{id}/issues/{issue_id}/prices`, un endpoint que la app no llamaba. Con el grado al 100 %,
  ese endpoint da un precio a cada fila: el grado no es un dato que enseñar sino un índice.

Y había una tercera fuente que el ticket no contemplaba, propuesta por el coleccionista: el metal.
`weight` y `composition` están en el 100 % de sus 187 tipos en `numista-type-cache.json` (`size`
también; `thickness` sólo en el 65 %).

## Las tres fuentes, y la regla

El valor de una pieza es el máximo de las tres, pieza a pieza y nunca por familias:

| fuente | de dónde sale | cobertura sobre las 572 piezas |
| --- | --- | ---: |
| **suelo de plata** | `weight` × ley de `composition` × spot | **98 %** |
| **mercado** | Numista, por emisión y grado | **96 %** |
| **lo pagado** | `price` de la colección | 16 % |
| **el máximo de las tres** | | **99,5 %** |

Del grado: 188 de las 229 filas casan con su grado exacto, 22 sólo tienen precio en un grado
vecino y 19 no tienen ninguno. De las emisiones consultadas, 204 de 223 traen precios (91 %).

### El orden se invierte con el metal

Por esto hace falta el máximo y no una fuente fija:

| spot | gana la plata | gana el mercado | gana lo pagado |
| ---: | ---: | ---: | ---: |
| **55,23 €/oz** (el de hoy) | 14 piezas | **517** | 41 |
| 74 €/oz (un 34 % más) | **338** | 198 | 36 |

Los precios de Numista son estimaciones de catálogo que no siguen al metal. Si la plata sube, los
rebasa, y una app que usara sólo «el precio de mercado» diría que un duro de plata vale menos que su
propia plata. Lo pagado tampoco sobra aunque cubra el 16 %: gana 41 veces. El 2 Bolívares de 1879
no tiene precio en Numista y su plata vale poco; sin lo pagado, su valor real no aparecería.

### La prima sobre el metal era un artefacto de la medición

Contra el metal, la colección salía comprada con un +181 % de prima media (mediana +146 %), con
casos de +4867 %. Contra el máximo de las tres fuentes, lo pagado está un 8 % por debajo de lo que
vale. La primera cifra medía cuánto de lo que se paga por una moneda no es su metal, que es la
numismática entera.

## Lo que se enseña, y dónde

### Una tercera celda en el primer nivel: «Las cifras»

Enmienda el ADR 0021 §1 («a bottom bar of two destinations»): pasan a ser tres.

El [#315](https://github.com/jenarvaezg/coindex/issues/315) había descartado horas antes una
tercera celda por dos motivos, y ninguno vale aquí:

- **Necesitaría su propio recuento**, y el de la hoja eran casillas, otro grano. Esta página sí
  tiene número propio: el peso. La celda dice «Las cifras · 6,91 kg».
- **El nombre competía con el que ya había**: el de la hoja era «tu colección», frente a
  «Colecciones». «Las cifras» no compite con nada y admite el peso, el dinero, los años y los
  emisores.

El recuento es el peso y no el dinero a propósito. Un importe en euros en una barra permanente
cambia solo, sin que nadie toque nada, y enseña el patrimonio a cualquiera que mire el móvil. El
peso sólo cambia cuando entra una moneda.

Esta página es un destino y la mancha y el eje del #315 no, porque aquéllos están hechos de
casillas (son órdenes de la misma hoja) y ésta no. Es la respuesta que recibe el
[#317](https://github.com/jenarvaezg/coindex/issues/317).

### Lo que lleva dentro

Todo sale del APK y de dos llamadas, sin datos nuevos que curar:

| cifra | cobertura |
| --- | ---: |
| **6,91 kg** | 99 % de las piezas |
| **190 oz de plata fina** | 98 % |
| **apiladas, unos 95 cm** | 78 % (`thickness` falta en un tercio de los tipos) |
| **en fila, 15,22 m** | 99 % |
| **extendidas, 0,35 m² — 5,6 folios A4** | 99 % |
| **de 270 a 2026: 1.756 años** | 99 % |
| **34 emisores, 572 piezas** | 100 % |
| **el valor**, con la fecha de la última lectura del spot | 99,5 % |

La cifra que mejor retrata esta colección: Venezuela es el 62 % de sus piezas, el 33 % de su peso
y el 34 % de su plata. Juntas dicen que son muchas monedas pequeñas.

### El valor también en la pieza y en la lámina

En la ficha que el [#305](https://github.com/jenarvaezg/coindex/issues/305) abrió para Monedas, y
en la cabecera de la lámina. Siempre con el origen dicho («precio de catálogo en `unc`», «su
plata»): en una app de dos usuarios, un número sin procedencia no lo puede comprobar nadie.

## El grano decide

La misma regla aparece en dos sitios:

> **Lo que se lee pieza a pieza o lámina a lámina ayuda a decidir una compra. Lo mismo, totalizado
> para toda la colección, es gestión patrimonial.**

- **La prima** sobre el metal, en una pieza, sirve para juzgar esa compra. Sumada para la colección
  es el rendimiento de una cartera.
- **El coste de completar** (lo que falta gastarse) por lámina es accionable: hay 12 láminas suyas a
  una, dos o tres casillas de cerrarse. Sumado para toda la estantería es un reproche, lo contrario
  del *revela, no reprocha* del [#304](https://github.com/jenarvaezg/coindex/issues/304) y el #315.

Es construible: de una muestra de 35 huecos, 30 tienen precio (86 %).

## De dónde sale el precio de la plata

Dos llamadas al abrir la app, ambas sin clave y verificadas en vivo el 8 de agosto de 2026:

- `https://api.gold-api.com/price/XAG` → precio de la onza troy en dólares, con `updatedAt`.
- `https://api.frankfurter.dev/v1/latest?base=USD&symbols=EUR` → el cambio del BCE.

Se enseña siempre con la fecha de su última lectura, para que no se lea como una cotización: con el
reparto de hoy, un vaivén del 3 % en la plata mueve el total un 1,9 %, porque manda el catálogo.

## El coste en llamadas

| qué | llamadas | medido |
| --- | ---: | --- |
| precios de lo que ya tiene | **223** (una por emisión) | ~65 s |
| precios de los huecos de sus láminas | **~632** (dos por hueco: emisiones y precios) | no medido entero |
| el spot | 2 por apertura | 0,4 s |

Los precios de catálogo se mueven despacio, así que se cachean como las fichas. Cuándo se
descargan, qué caduca y qué se enseña sin ellos quedó para otro ticket (ADR 0028).

## Lo que se descarta

| | por qué se cae |
| --- | --- |
| **Enseñar sólo el suelo de plata** | Era la salida segura a la falta de precios, porque un suelo nunca exagera, pero hoy el metal sólo gana en 14 de 572 piezas: diría que la colección vale menos de lo que cualquiera comprueba en Numista. |
| **Un total de lo pagado** | Sumar 84 filas y presentarlo como el valor es falso por construcción, porque la ausencia se concentra en el 84 % de las piezas. |
| **La prima agregada** | «Has pagado una media del +181 % sobre el metal» es el rendimiento de una cartera, y además la cifra sale de comparar contra la fuente equivocada. |
| **El coste total de completar la estantería** | Un reproche. Por lámina, el mismo dato es un plan. |
| **Llamar «Analíticas» a la página** | Vocabulario de cuadro de mandos, que veta `spec.md §0.4`. |
| **El dinero como recuento de la celda** | Un importe que cambia solo en una barra permanente. El peso dice algo parecido y no se mueve. |
| **El retrato físico sólo en el colofón del PDF** | Es lo más compartible de la colección; no había razón para dejarlo sólo a quien exporta. |

## Los cabos que deja

- **El ADR 0021 §1 se enmienda**: el primer nivel pasa de dos destinos a tres. Lo recoge el
  [#308](https://github.com/jenarvaezg/coindex/issues/308) con el resto de la reescritura.
- **El dinero es el sexto interruptor de la exportación**, no un mecanismo nuevo: el
  [#228](https://github.com/jenarvaezg/coindex/issues/228) ya dejó cinco independientes (ADR 0021
  §13). Quien exporta decide si comparte la colección con su valor o sin él.
- **La forma de la página** (cuántas pantallas, en qué orden van las cifras) queda para un
  prototipo, como el #300 y el #315.
- **Nada de esto se ha visto en un teléfono.** Los números salen del dominio y de la API; la
  implementación empieza confirmándolo en el AVD.
- **`thickness` falta en un tercio de los tipos**, así que la altura de la pila es la única cifra
  extrapolada: o se dice «unos 95 cm», o se declara sobre cuántas piezas se midió.
- **La gestión patrimonial queda fuera**, por escrito: sin histórico del spot, sin evolución, sin
  agregados de rendimiento.
