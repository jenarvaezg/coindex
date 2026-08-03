# Prototipo del primer nivel · ticket #18

Las formas rivales del primer nivel de Coindex, corriendo en el AVD `coindex-ux` (pixel_7,
android-36) sobre la v0.11.0, con la barra de variantes del prototipo abajo. **El código era
desechable y ya no existe** —vivió en la rama `prototipo/primer-nivel-18`, borrada al resolver el
ticket—; estas capturas son lo que queda, y son la prueba de dónde salió la decisión.

Los datos son los medidos en [#17](https://github.com/jenarvaezg/coindex/issues/17) sobre el móvil
del padre —58 colecciones, 33 con lámina, 15 a 1/N, 50 tipos sin colección, 572 piezas de 191
tipos— con los nombres, pesos, años y estados de serie reales de `data/`. No es su inventario: es
su **densidad**.

## B, la que gana, con lo pedido en la sesión

| Captura | Qué enseña |
| --- | --- |
| [`b15-colecciones-barra.png`](b15-colecciones-barra.png) | **La forma final**: abre en Colecciones, barra inferior de dos destinos |
| [`b16-monedas-barra.png`](b16-monedas-barra.png) | Monedas, a un toque desde la misma barra |
| [`b1-bifurcacion.png`](b1-bifurcacion.png) | La bifurcación como casa, **descartada**: cobraba un toque por arranque |
| [`b6-colecciones-plegado.png`](b6-colecciones-plegado.png) | Colecciones al entrar: buscador visible, filtros **plegados** |
| [`b12-orden-completas.png`](b12-orden-completas.png) | La estantería abierta: orden + país, peso, año de inicio, estado, serie |
| [`b13-orden-reciente.png`](b13-orden-reciente.png) | «Alta más reciente» y la nota de que Numista no da fecha de compra |
| [`b14-persistencia.png`](b14-persistencia.png) | Tras matar la app y volver: «1 filtro · orden alta más reciente» |
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
- El orden **«alta más reciente» está simulado**: `collected_items` de Numista trae id, cantidad,
  tipo, emisión, grado, precio y colección, y **ninguna fecha**. Lo único que ordena por antigüedad
  es el id de la pieza, que es creciente — «alta en Numista», no «compra». La pantalla lo dice
  cuando ese orden está puesto.
- **Lo que persiste son los filtros y el orden, no el buscador.** Volver a abrir la app con un
  texto viejo en la caja y media colección escondida se lee como una app rota.
- Las facetas no cuestan API: `weight` está en el 100 % de los 804 tipos sembrados, `min_year` en
  el 99,8 % y `issuer` en el 100 %, todo dentro del `raw` que ya guarda `type_meta`.

## Qué se decidió con esto

[¿Qué vive en el primer nivel: colecciones, monedas, o las dos
cosas?](https://github.com/jenarvaezg/coindex/issues/18), del mapa [La arquitectura de información
de Coindex](https://github.com/jenarvaezg/coindex/issues/16): en el primer nivel viven **las dos**,
como jerarquías hermanas. La app abre en **Colecciones** y una barra inferior de dos destinos cruza
a **Monedas**. La resolución completa, con lo que se descartó y por qué, está en el ticket.
