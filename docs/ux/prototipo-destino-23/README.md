# Prototipo: el destino de una tarjeta (#23)

Asset de [#23](https://github.com/jenarvaezg/coindex/issues/23), del mapa
[#16](https://github.com/jenarvaezg/coindex/issues/16). Rama desechable
`prototipo/lista-de-piezas-23`, encima de `prototipo/primer-nivel-18` para que el destino se juzgue
**dentro** del modelo de dos jerarquías que decidió #18. AVD `coindex-ux`, v0.11.0.

## Las dos formas rivales

| Fichero | Qué enseña |
| --- | --- |
| `02-d-indice.png` | **D · Hoy** — el índice, con el orden por defecto de #22 (completas arriba) |
| `03-d-piezas.png` | D — la tarjeta abre «lo que tengo», con «Ver lámina» dentro: **primer salto** |
| `04-d-lamina.png` | D — la lámina, a dos toques de la tarjeta: **segundo salto** |
| `05-e-indice.png` | **E · Destino único** — el mismo índice |
| `06-e-lamina-directa.png` | E — la misma tarjeta abre su lámina de una: **un toque** |
| `10-e-lamina-con-hueco.png` | E — Paquillos a 4/5: la casilla dice «TENGO» o «ME FALTA» |
| `07-e-sin-lista.png` | E — el tramo sin ratio: **«Las francesas» es indistinguible de «Tribute to the Spanish Army»** |
| `08-e-tarjeta-sin-lista.png` | E — una tarjeta sin lista abre sus piezas |
| `09-e-caja-propia.png` | E — la caja propia abre **la misma pantalla**, más su mantenimiento |

## Avisos sobre el fixture

Nada de esto es dato real: el estado vive en memoria y no hay fotos.

- Los **tamaños** son los medidos en #17 sobre el móvil del padre, con nombres de `data/`.
- El fixture de #18 se hizo para el primer nivel y sólo etiqueta una muestra de piezas por
  colección, así que la lista de piezas se completa con monedas reales del mismo emisor hasta el
  tamaño que la tarjeta declara. Sin eso, la tarjeta decía «38 piezas» y la lista enseñaba dos, que
  es justo el contraste que había que ver.
- Las casillas de la lámina se rotulan `primer año + índice`, así que **los Paquillos salen
  1966-1970 cuando en realidad son seis emisiones de 1966 que se distinguen por la estrella**. Es
  ruido del prototipo, no una propuesta: el rótulo verdadero lo pone `emissionLabelFor`.
