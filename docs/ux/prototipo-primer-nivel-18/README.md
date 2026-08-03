# Prototipo del primer nivel · ticket #18

Las formas rivales del primer nivel de Coindex, corriendo en el AVD `coindex-ux` (pixel_7,
android-36) sobre la v0.11.0, con la barra de variantes del prototipo abajo. **Rama desechable
`prototipo/primer-nivel-18`; el código se borra al resolver el ticket, estas capturas se quedan.**

Los datos son los medidos en [#17](https://github.com/jenarvaezg/coindex/issues/17) sobre el móvil
del padre —58 colecciones, 33 con lámina, 15 a 1/N, 50 tipos sin colección, 572 piezas de 191
tipos— con los nombres, pesos, años y estados de serie reales de `data/`. No es su inventario: es
su **densidad**.

## B, la que gana, con lo pedido en la sesión

| Captura | Qué enseña |
| --- | --- |
| [`b1-bifurcacion.png`](b1-bifurcacion.png) | Las dos puertas: Colecciones 58 · Monedas 572 |
| [`b6-colecciones-plegado.png`](b6-colecciones-plegado.png) | Colecciones al entrar: buscador visible, filtros **plegados** |
| [`b7-colecciones-filtros.png`](b7-colecciones-filtros.png) | La estantería abierta: país, peso, año de inicio, estado, serie |
| [`b8-busqueda.png`](b8-busqueda.png) | «bolivar», sin tilde, en tiempo real → 2 de 58 |
| [`b9-monedas.png`](b9-monedas.png) | Monedas: 191 tipos, cada ficha con sus colecciones |
| [`b10-monedas-filtros.png`](b10-monedas-filtros.png) | Facetas de monedas: país, peso, año, clase, colección |
| [`b11-busqueda-monedas.png`](b11-busqueda-monedas.png) | «panda» → 4 de 191 |
| [`b5-dos-colecciones.png`](b5-dos-colecciones.png) | Una pieza en **dos** colecciones |

## Las descartadas, para el contraste

| Captura | Qué enseña |
| --- | --- |
| [`a-arriba.png`](a-arriba.png) · [`a-scroll.png`](a-scroll.png) | A · el índice de hoy y su muro de 58 tarjetas alfabéticas |
| [`c-1-laminas.png`](c-1-laminas.png) · [`c-2-monedas.png`](c-2-monedas.png) | C · el mismo material en pestañas de un cuaderno |

## Avisos sobre el fixture, para que nadie lo cite como medición

- **«Completas · 5»**, no 4: el reparto de casillas llenas es plausible, no el del padre. El número
  medido en #17 es **4**.
- La fila del **Morgan Dollar en dos colecciones está puesta a mano**. Hoy, de los 723 tipos
  curados de `data/`, **ninguno vive en más de una colección**: la pertenencia múltiple es una
  capacidad nueva, no un dato que la app ya tenga.
- Las facetas no cuestan API: `weight` está en el 100 % de los 804 tipos sembrados, `min_year` en
  el 99,8 % y `issuer` en el 100 %, todo dentro del `raw` que ya guarda `type_meta`.
