# Prototipo: desde dónde nace una caja propia (#161)

Asset de [#161](https://github.com/jenarvaezg/coindex/issues/161), del mapa
[#16](https://github.com/jenarvaezg/coindex/issues/16). Rama desechable
`prototipo/caja-propia-161`, encima de `prototipo/lista-de-piezas-23` para que el gesto se juzgue
dentro del modelo que ya decidieron [#18](https://github.com/jenarvaezg/coindex/issues/18) —Monedas
como jerarquía hermana, con filtros y buscador— y
[#23](https://github.com/jenarvaezg/coindex/issues/23) —una tarjeta, un destino—. AVD `coindex-ux`,
v0.12.0.

## Las dos formas rivales

| Fichero | Qué enseña |
| --- | --- |
| `01-f-monedas.png` | **F · Una a una** — el gesto de hoy mudado a Monedas: «Agrupar piezas» sobre 191 tipos |
| `02-f-una-a-una.png` | F — el modo abierto: **«Nombrar la caja · 0»** y un «Elegir» por tocar en cada una de las 191 |
| `03-g-sin-filtro.png` | **G · El filtro siembra** — sin filtro **no siembra**: 191 tipos y el botón dice «Agrupar piezas» |
| `04-g-filtrado.png` | G — buscando «Francia»: 6 de 191, y el botón dice **«Agrupar estas 6»** |
| `05-g-sembrado.png` | G — entra con las **6 ya elegidas**: «Vienen elegidas las 6 que enseñaba el filtro. Quita las que no.» |
| `06-g-descartada.png` | G — una desmarcada: la de Guadalupe vuelve a «Elegir» y el contador baja a 5 |
| `07-g-nombre-repetido.png` | El bautizo rechaza el nombre que ya existe: **«Ya hay una colección que se llama «French regions»»** y «Crear» apagado |
| `08-g-nombre-bueno.png` | «Las francesas», **13/40 · tiene que caber en una tarjeta** |
| `09-g-en-el-indice.png` | La caja en el índice: **rotula FRANCIA y dice «5 monedas · 5 tipos»**, y la vaciada dice «0 monedas · 0 tipos» sin eyebrow |
| `10-g-cincuenta-y-nueve.png` | El caso grande: sembrar desde «Sin colección · 59» — 59 premarcadas |

## Lo que cambió al construirlo

**La siembra necesita un filtro puesto.** La primera versión sembraba siempre, y sin filtro el botón
ofrecía **«Agrupar estas 191»**: agrupar la colección entera. Peor, una caja arbitraria —dos monedas
de dos países, que es la forma del caso real del estuche del BCV— se haría **desmarcando 189**. Así
que el botón siembra sólo cuando el filtro ya ha recortado algo, y sin filtro entra vacío como F. Las
dos formas no eran rivales: son el mismo gesto con la semilla dentro o fuera.

`10-g-cincuenta-y-nueve.png` es la razón de que el botón diga el número: **el coste va escrito en el
botón antes de pulsarlo**. Con 59 premarcadas el trabajo se invierte y se ve que toca afinar el filtro
antes de agrupar, no después.

## Un defecto real que destapó el prototipo

El `GroupingDialog` que la v0.12.0 tiene en producción **se transparenta sobre el scrim**:
`FieldCard` pinta `Paper.card`, que es `Color(0x57FFFCF2)` —34 % de alfa, pensado para ir encima del
papel—, y dentro de un `Dialog` no hay papel debajo. El prototipo le pone `Paper.paper` opaco detrás
para que la captura se lea. Nadie lo había visto porque nadie ha llegado nunca a ese diálogo (#17).

## Avisos sobre el fixture

Nada de esto es dato real: el estado vive en memoria, no hay fotos y las cajas se pierden al cerrar.

- Los **tamaños** son los medidos en #17 sobre el móvil del padre, con nombres de `data/`.
- El fixture arranca con una caja **ya vaciada** —«Las que cambié», con tipos que no existen— para
  poder mirar la tarjeta a cero sin vender nada.
- La barra de variantes del prototipo **se solapa con los últimos chips** de la estantería de
  filtros abierta. Es ruido del conmutador desechable, que en la app no existe.
- El pie sigue diciendo «Colecciones · 58» aunque la lista pase a 60: el número del pie es una
  constante del prototipo, no un recuento.
