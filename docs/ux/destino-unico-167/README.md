# Una tarjeta, un destino — corriendo (#167)

Capturas de la implementación del ADR 0021 §9, tomadas en el AVD `coindex-ux` sobre la v0.12.0.
No son de un prototipo: es la app.

| | |
| --- | --- |
| **1 · El índice** | Ninguna tarjeta lleva ya «Ver lámina»: el título es lo único que abre, y las tarjetas encogen. «Silver Krugerrand» abre su lámina de un toque. |
| **2 · Una tarjeta sin lista de emisiones** | Gibraltar, una sola pieza. Eyebrow de país, no de especie; el recuento es el mismo de la tarjeta; «Exportar como imagen» y «Agrupar piezas». |
| **3 · Una caja propia** | «Bandeja del abuelo», dos países, sin eyebrow. **Es la misma pantalla que la 2**, más el `if` del mantenimiento: renombrar, deshacer, y quitar por fila. Sin «Agrupar piezas», que nunca tuvo. |
| **4 · La hoja exportada** | `coindex-bandeja-del-abuelo.png`, 1680×975. Dice «COLECCIÓN» donde una lámina dice «CATÁLOGO CURADO», y **ninguna casilla vacía**: una caja no puede tener un hueco (ADR 0020). |

![El índice](1-indice.png)

![Una tarjeta sin lista](2-tarjeta-sin-lista.png)

![Una caja propia](3-caja-propia.png)

![La hoja exportada](4-hoja-exportada.png)
