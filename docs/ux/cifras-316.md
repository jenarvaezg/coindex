# Las cifras: la analítica es una página, y lo que vale una pieza es el mayor de tres números

La respuesta del [#316](https://github.com/jenarvaezg/coindex/issues/316), decidida el 8 de agosto de
2026 sobre las dos colecciones reales y la API de Numista consultada en vivo — 223 emisiones propias
y 35 huecos de muestra, no una lectura de la documentación.

**Las cifras absolutas en euros no están en este documento a propósito.** Este repositorio es
público y el patrimonio del coleccionista no lo es. Aquí van el método, las coberturas y las
proporciones, que es lo que hace falta para construirlo y para discutirlo; los importes viven en
`/private/tmp/coindex-privado/cifras-316.txt`, junto a las capturas del sync.

## Lo primero: dos de las tres premisas del ticket estaban mal

El ticket planteaba «`grade` al 100 % y `price` al 37 %» como dos capacidades medio pagadas que
había que dibujar o tirar. Medido, ninguna de las dos era lo que decía:

- **`price` no cubre el 37 %, cubre el 16 %.** Son 84 filas de 229 — pero esas 84 filas son **91
  piezas de 572**, porque lo que no tiene precio son los bultos venezolanos (x102, x59, x44…). La
  ausencia no es aleatoria: se concentra exactamente donde están cinco de cada seis piezas.
- **`grade` no es una analítica, es la clave de tasación.** Numista publica precio estimado **por
  emisión y por grado** en `/types/{id}/issues/{issue_id}/prices`, un endpoint que la app no llama
  hoy. Con el grado al 100 %, ese endpoint convierte cada fila en un precio. La pregunta del ticket
  —«¿y `grade`, que sí está completo?»— no tenía respuesta como dibujo porque no era un dato que
  enseñar: era un índice.

Y había una tercera fuente que el ticket no contemplaba, la que propuso el coleccionista: **el metal**.
`weight` y `composition` están en el **100 %** de los 187 tipos suyos que hay en
`numista-type-cache.json` (`size` también; `thickness` sólo en el 65 %).

## Las tres fuentes, y la regla

**El valor de una pieza es el máximo de las tres**, pieza a pieza y nunca por familias:

| fuente | de dónde sale | cobertura sobre las 572 piezas |
| --- | --- | ---: |
| **suelo de plata** | `weight` × ley de `composition` × spot | **98 %** |
| **mercado** | Numista, por emisión y grado | **96 %** |
| **lo pagado** | `price` de la colección | 16 % |
| **el máximo de las tres** | | **99,5 %** |

Del grado: **188 de las 229 filas** casan con su grado exacto, **22** sólo tienen precio en un grado
vecino y **19** no tienen ninguno. Sobre las emisiones consultadas, 204 de 223 traen precios (91 %).

### La regla no es un desempate ocasional: el orden se invierte con el metal

Esto es lo que obliga al máximo, y es lo que no se ve sin medirlo dos veces:

| spot | gana la plata | gana el mercado | gana lo pagado |
| ---: | ---: | ---: | ---: |
| **55,23 €/oz** (el de hoy) | 14 piezas | **517** | 41 |
| 74 €/oz (un 34 % más) | **338** | 198 | 36 |

Los precios de Numista son estimaciones de catálogo que **no siguen al metal**. Cuando la plata sube
los rebasa, y una app que hubiera elegido «el precio de mercado» como su única fuente diría que un
duro de plata vale menos que su propia plata. La tercera fuente tampoco sobra aunque cubra el 16 %:
gana 41 veces, y el 2 Bolívares de 1879 lo demuestra solo — Numista no le da precio, su plata son
unos pocos euros, y sin lo pagado su valor real desaparecería de la pantalla.

### La prima escandalosa era un espejismo de la medición

Contra el metal, la colección salía comprada con un **+181 % de prima media** (mediana +146 %), con
casos de +4867 %. Contra el máximo de las tres fuentes, lo pagado está a **−8 %** de lo que vale.

No es una corrección menor: es la diferencia entre una app que dice «has pagado casi el triple de lo
que tienes» y una que dice «compras a precio de mercado». La primera cifra era verdadera y sin
sentido — medía cuánto de lo que se paga por una moneda **no es su metal**, que es toda la
numismática.

## Lo que se enseña, y dónde

### Una tercera celda en el primer nivel: «Las cifras»

**Enmienda el ADR 0021 §1**, que dice «a bottom bar of two destinations». Pasan a ser tres.

El [#315](https://github.com/jenarvaezg/coindex/issues/315) había descartado una tercera celda hace
unas horas, con dos argumentos. Uno se cae y el otro se resuelve:

- **«Tendría que traer su propio recuento, y el de la hoja son casillas, que es otro grano.»** Cierto
  para la hoja, falso para esta página: tiene número propio y es **el peso**. La celda dice
  «Las cifras · 6,91 kg».
- **«Si una pantalla necesita un nombre que pelea con el que ya hay, no es una pantalla.»** El que
  mató a la hoja era «tu colección», que competía con «Colecciones». **«Las cifras»** no compite con
  nada, y no promete un contenido concreto: bajo ese paraguas caben el peso, el dinero, los años y
  los emisores.

**Y el recuento es el peso y no el dinero, deliberadamente.** Un importe en euros en una barra
permanente es un ticker de bolsillo: cambia solo, sin que nadie toque nada, y pone el patrimonio a la
vista de cualquiera que mire el móvil de reojo. El peso sólo cambia cuando entra una moneda.

Por qué esta página **sí** es un destino y la mancha y el eje **no** lo eran: aquéllos están hechos
de casillas y son por tanto órdenes de la misma hoja; ésta no tiene casillas. Es la respuesta partida
que recibe el [#317](https://github.com/jenarvaezg/coindex/issues/317).

### Lo que lleva dentro

Todo esto sale del APK y de dos llamadas, sin un dato nuevo que curar:

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

De propina, la cifra que retrata esta colección mejor que ninguna otra: **Venezuela es el 62 % de sus
piezas, el 33 % de su peso y el 34 % de su plata.** Los tres números a la vez dicen lo que ninguno
dice solo — que son muchas monedas pequeñas.

### El valor también en la pieza y en la colección

En la ficha que el [#305](https://github.com/jenarvaezg/coindex/issues/305) abrió para Monedas, y en
la cabecera de la lámina. Con **el origen dicho** («precio de catálogo en `unc`», «su plata»), porque
un número sin procedencia en una app de dos usuarios es un número que nadie puede comprobar.

## El grano es el criterio, y aparece dos veces

Es el hallazgo que más lejos llega, porque no es una decisión sino una regla que se aplicó sola en dos
sitios distintos:

> **Lo que se lee pieza a pieza o lámina a lámina es compañero de compra. Lo mismo, totalizado para
> toda la colección, es una herramienta de gestión patrimonial.**

- **La prima** sobre el metal, en una pieza, es la escala con la que juzgas *esa* compra. Sumada para
  la colección es el rendimiento de una cartera.
- **El coste de completar** —lo que falta gastarse— por lámina es accionable: hay **12 láminas suyas a
  una, dos o tres casillas de cerrarse**, y una de ellas se cierra por el precio de una cena. Sumado
  para la estantería entera es «te faltan decenas de miles de euros», que es el reproche perfecto y lo
  contrario del *revela, no reprocha* que firmaron el [#304](https://github.com/jenarvaezg/coindex/issues/304)
  y el #315.

Es construible: de una muestra de 35 huecos, **30 tienen precio (86 %)**.

## De dónde sale el precio de la plata

Dos llamadas al abrir la app, ambas **sin clave** y verificadas en vivo el 8 de agosto de 2026:

- `https://api.gold-api.com/price/XAG` → precio de la onza troy **en dólares**, con `updatedAt`.
- `https://api.frankfurter.dev/v1/latest?base=USD&symbols=EUR` → el cambio del BCE.

**Se enseña siempre con la fecha de su última lectura**, y esa fecha es lo que hace que el número no
sea una cotización: un vaivén del 3 % en la plata mueve el total un **1,9 %** con el reparto de hoy,
porque manda el catálogo.

## El coste en llamadas, que no es cero

| qué | llamadas | medido |
| --- | ---: | --- |
| precios de lo que ya tiene | **223** (una por emisión) | ~65 s |
| precios de los huecos de sus láminas | **~632** (dos por hueco: emisiones y precios) | no medido entero |
| el spot | 2 por apertura | 0,4 s |

Los precios de catálogo se mueven despacio, así que se cachean como las fichas. **Cuándo se
descargan, qué caduca y qué se enseña sin ellos no se decide aquí**: es el ticket que este deja abierto.

## Lo que se descarta

| | por qué se cae |
| --- | --- |
| **Enseñar sólo el suelo de plata** | Era la salida elegante al 63 % ausente —un suelo nunca miente— y la mata el reparto: hoy el metal sólo gana en 14 de 572 piezas. Diría que la colección vale menos de lo que cualquiera puede comprobar en el propio Numista. |
| **Un total de lo pagado** | Es lo que el ticket temía y tenía razón: sumar 84 filas y presentarlo como el valor es falso por construcción, porque la ausencia se concentra en el 84 % de las piezas. |
| **La prima agregada** | «Has pagado una media del +181 % sobre el metal» es el rendimiento de una cartera, y además la cifra es un artefacto de comparar contra la fuente equivocada. |
| **El coste total de completar la estantería** | Un reproche de decenas de miles de euros. Por lámina el mismo dato es un plan. |
| **Llamar «Analíticas» a la página** | Vocabulario de cuadro de mandos, que es lo que veta `spec.md §0.4`. |
| **El dinero como recuento de la celda** | Un importe que cambia solo en una barra permanente. El peso dice algo parecido y no se mueve. |
| **El retrato físico sólo en el colofón del PDF** | Se propuso dejar lo del peso y la pila fuera de la app, en la última página de lo impreso. Se descarta: es lo más compartible que tiene la colección y no había razón para esconderlo donde sólo llega quien exporta. |

## Los cabos que deja

- **El ADR 0021 §1 se enmienda**: el primer nivel pasa de dos destinos a tres. Lo recoge el
  [#308](https://github.com/jenarvaezg/coindex/issues/308) junto con el resto de la reescritura.
- **El dinero es el sexto interruptor de la exportación**, no un mecanismo nuevo: el
  [#228](https://github.com/jenarvaezg/coindex/issues/228) ya dejó cinco independientes (ADR 0021 §13).
  Se comparte «mira qué bonito» o «mira lo que vale», y lo decide quien exporta.
- **Esto no está dibujado.** Qué forma tiene la página, cuántas pantallas ocupa y cómo se ordenan sus
  cifras es un prototipo, como lo fueron el #300 y el #315.
- **Nada de esto se ha visto en un teléfono.** Los números salen del dominio y de la API; la primera
  sesión de implementación empieza confirmándolo en el AVD.
- **`thickness` falta en un tercio de los tipos**, así que la altura de la pila es la única cifra que
  se da extrapolada. O se dice «unos 95 cm» y se acepta, o se declara sobre cuántas piezas se midió.
- **Gestión patrimonial sigue fuera**, ahora por escrito y no por omisión: sin histórico del spot, sin
  evolución, sin agregados de rendimiento.
