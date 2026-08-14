# Prototipo · dos cifras de dinero en la cabecera de una lámina (#493)

Maqueta de forma para el [#493](https://github.com/jenarvaezg/coindex/issues/493), que viaja dentro
del [#497](https://github.com/jenarvaezg/coindex/issues/497). La pregunta que contesta no es «¿cómo
se pinta esto?» —el dinero de la cabecera ya está pintado— sino las tres que el ticket dejó escritas
y sin decidir:

> - Si las dos caben a la vez o una manda y la otra baja de jerarquía.
> - Qué palabra distingue «lo que tienes» de «lo que falta» sin que la segunda suene a reproche.
> - Si el coste de cerrar desaparece al cerrarse la lámina o se queda a cero.

Ocho cabeceras a dp real —la de hoy de listón y siete salidas— sobre cinco láminas del padre. En HTML
y no en Compose porque lo que se elige es **estructura** (`prototipar-forma-en-html`).

    python3 docs/ux/prototipo-cifras-493/extract.py
    python3 docs/ux/prototipo-cifras-493/build.py
    open /private/tmp/coindex-privado/cifras-493/maqueta.html

Barra `sticky` arriba: las elegidas, las siete o una sola, y las cinco láminas. También por URL:
`?s=colision&v=e`.

## Lo elegido

**Las dos cifras dichas con su etiqueta entera en la cabecera, y el precio de cada hueco dentro del
hueco.** Elegido por Jose el 14 de agosto de 2026 con la maqueta delante (variante **E**), y escrito
con la procedencia de cada cifra corregida (variante **G**, la que se implementa):

    Valor actual: XXX €     · al mayor de tres precios
    Coste de cerrar: YY €   · en sin circular          ← sólo si no está completa

más el sello de la C: cada casilla vacía con precio lleva **su** precio dentro del hueco, como el
papelito de un bolsillo vacío.

Las tres respuestas del ticket, entonces: **caben las dos**, distinguidas por la etiqueta y no por el
tamaño; la palabra es «Coste de cerrar», que nombra una compra y no una carencia; y **desaparece** al
cerrarse la lámina, sin cero que redactar.

### Por qué la G y no la E que se eligió

La E ponía **un solo renglón de criterio para las dos cifras** —«al mayor de tres precios»—, y al
verificarlo contra el ADR 0028 §8 resulta que es **falso para la segunda**: un hueco no tiene «lo que
pagaste», así que sus precios son dos y no tres, y **se tasa en `unc`**, que la app dice «sin
circular». Así que el criterio vuelve a viajar con su importe, que es lo que pedía el
[#408](https://github.com/jenarvaezg/coindex/issues/408) y lo que ya hace `plateValueLabel`. Sale
además más corta que la E: **+25 dp** de cabecera en vez de +42.

La **F** —callar el renglón del coste cuando sólo falta un hueco, porque el sello ya lo dice— se
**descartó**: haría que la cabecera cambiara de forma según cuántos huecos queden, y ésa es una regla
que el coleccionista tiene que descubrir. Con un hueco la repetición no es ruido: el renglón dice
*cuánto* y el sello dice *cuál*.

## Las cinco láminas, y por qué estas cinco

Hoy el coste de cerrar **no está en pantalla**: `valuationPlan` pide el precio de los huecos de las
láminas a ≤ 10 casillas (ADR 0028 §1) y `plateValue` deja dicho por escrito que el coste «no está
aquí a propósito». Las dos cifras coexisten por primera vez en este bloque.

| | qué es | la proporción |
| --- | --- | ---: |
| **Holgada** · 100 bolívares de plata | 3/4, un hueco | la de dentro es **5,5×** la de cerrar |
| **Colisión** · 20 escudos de plata | 2/3, un hueco | la de dentro es **1,4×** la de cerrar |
| **Varios huecos** · The Queen's Beasts | 4/11, siete huecos | cerrar es **1,75×** lo de dentro |
| **Sobre el umbral** · Australian Kangaroo | 1/12, once huecos | no hay coste que decir |
| **Cerrada** · XVII Exposición Europea de Arte | 3/3 | no hay segunda cifra |

En la colisión el tamaño no distingue nada: **la etiqueta hace el 100 % del trabajo**. Ése es el caso
que decidió.

## Las ocho

| | tesis | veredicto |
| --- | --- | --- |
| **Hoy** · v1.2.20 | una cifra, el valor de lo que hay dentro | listón |
| **A · dos renglones hermanos** | las dos con una palabra corta delante —«dentro» / «cerrarla»— y un solo criterio para las dos | descartada |
| **B · una manda, la otra baja a la ficha** | cerrar es un dato de la lámina y va en la `SpecificationCard` | descartada |
| **C · el precio cuelga del hueco** | cada hueco lleva su precio dentro; la cabecera no crece | **elegida, dentro de la G** |
| **D · una sola cifra, y el estado elige** | mientras falte algo sólo el coste; cerrada sólo el valor | descartada |
| **E · las dos dichas, y el sello en cada hueco** | las etiquetas enteras más los sellos, con un criterio para las dos | elegida, y corregida por la G |
| **F · la E sin decir dos veces lo mismo** | el renglón del coste sólo con más de un hueco | descartada |
| **G · la E con el criterio de cada cifra** | la E, con la procedencia de cada importe pegada a él | **la que se implementa** |

## Lo medido, sobre el dibujo

Del borde de la pantalla al cartón del primer hueco, que es lo que cada variante cobra por decir el
dinero.

| | un hueco | varios huecos | sobre el umbral | cerrada |
| --- | ---: | ---: | ---: | ---: |
| Hoy | 437 dp | 437 dp | 437 dp | 404 dp |
| C | 437 dp (0) | 437 dp (0) | 437 dp (0) | 404 dp (0) |
| E | 479 dp (+42) | 479 dp (+42) | 458 dp (+21) | 425 dp (+21) |
| F | 458 dp (+21) | 479 dp (+42) | 458 dp (+21) | 425 dp (+21) |
| **G** | **462 dp (+25)** | **462 dp (+25)** | **444 dp (+7)** | **411 dp (+7)** |

Sobre las **49 láminas alcanzables** del padre: 27 tienen coste de cerrar (121 huecos por debajo del
umbral), 16 están por encima del umbral y 6 están completas. **22 de 49 no tienen segunda cifra** y
pagan de todos modos los 7 dp del renglón etiquetado.

## Lo que sólo se vio al dibujarlo

1. **La etiqueta entera se sostiene sola, y una palabra corta no.** La primera vuelta probó «dentro» /
   «cerrarla» (variante A): cerrada, la lámina seguía diciendo «dentro» sobre un importe que ya no
   tenía hermano del que distinguirse. «Valor actual» no tiene ese problema — se lee igual de bien
   solo que en pareja, que es exactamente el caso de 22 de las 49 láminas.
2. **Las dos cifras no tienen el mismo criterio, y por poco se escribe que sí.** La E compartía un
   renglón de procedencia para las dos —«al mayor de tres precios»—, y es falso para el coste: un
   hueco no tiene «lo que pagaste», así que sus precios son **dos**, y el ADR 0028 §8 ya decidió que
   un hueco se tasa **en `unc`**. La procedencia vuelve a viajar con su importe (#408) y de paso la
   cabecera sale 17 dp más corta que la E.
3. **Los sellos hacen visible el umbral del ADR 0028 §1.** La lámina del Kangaroo tiene once huecos —
   uno más que el umbral— así que el pase nunca pidió su precio y **no hay un solo sello**, al lado de
   una de diez que llevaría diez. La regla no está mal dicha: sin precio en el teléfono no hay sello
   que poner. Pero es la primera vez que el umbral se ve, y lo que se ve es que unas láminas tienen
   sellos y otras no por una razón que no está escrita en ninguna parte de la pantalla.
4. **En un date run de bullion los siete sellos dicen el mismo número.** Con el suelo de la plata, las
   siete Queen's Beasts que faltan valen lo mismo —mismo peso, misma ley— y la lámina queda empapelada
   del mismo importe siete veces. Con el mayor de tres precios se separarán, porque el precio de
   catálogo va por emisión y grado; pero conviene mirarlo cuando haya precios de verdad, porque si no
   se separan, siete sellos iguales son un sello repetido siete veces.
5. **El sello tapa el fantasma, que es lo que dice qué falta.** El chip se pone sobre el diseño del
   catálogo, la única cosa de una casilla vacía que cuenta qué moneda es. En una casilla de 104 dp no
   hay sitio para las dos cosas a la vez.
6. **El interior del hueco lo reclama también el #497.** Un deseo es *una casilla marcada*: la marca y
   el sello van al mismo centímetro. Se decide en el mismo bloque, así que hay que dibujarlas juntas
   antes de implementar ninguna de las dos.
7. **El sello no compite con la ceremonia**: el sello de «completa» cae sobre el cociente `3/4`
   (ADR 0026 §3), en la otra esquina, y ninguna variante lo toca.

## Lo que la maqueta no prueba

- **Los importes son el suelo de la plata**, con el spot y el cambio de los dos endpoints que lee
  `SilverSpot.kt`. La app enseñaría el mayor de tres precios, así que **los de verdad son más altos**
  y las proporciones se moverán. Lo que no se mueve es que en una lámina corta las dos cifras son del
  mismo orden.
- **El cruce es de casa, no del móvil** (`medir-en-el-movil-no-en-el-asset`): reproduce
  `memberMatches`, el álbum y el umbral, sobre el inventario del padre del 3 de agosto. Da **2**
  láminas a una sola casilla donde el ticket dice 3, y la diferencia es la fecha del inventario contra
  los catálogos de hoy. El número que manda es el del teléfono.
- **Nada de esto se ha visto en un teléfono.**

## Lo privado y lo que se tira

Los importes no se versionan (`dinero-fuera-del-repo-publico`): `data.json`, las fotos, la maqueta y
las cinco capturas —`holgada.jpg`, `colision.jpg`, `varios.jpg`, `umbral.jpg`, `cerrada.jpg`, con el
listón, la E y la G lado a lado— están en `/private/tmp/coindex-privado/cifras-493/`. Aquí quedan el
método y las proporciones.

`extract.py` y `build.py` son del prototipo y se borran cuando el ticket se cierre. Lo que sobrevive
es este README.

## Lo que falta antes de implementar

Está escrito como especificación en el propio [#493](https://github.com/jenarvaezg/coindex/issues/493).
En una línea cada cosa:

1. **El dato no llega a la UI.** `type_issues` sólo se lee dentro del pase (`Daos.kt:248`, `suspend`),
   así que 111 de los 121 huecos no saben aún qué emisión son. Hay que observarlo.
2. **El sello del hueco y la marca del deseo del #497 quieren el mismo centímetro**: se dibujan juntos
   o se pisan.
3. **El papel se decide aparte.** El PNG exportado es la página impresa (`SheetPngExport.kt:118`), que
   ya imprime «Valor» como dato (`NotebookSections.kt:99`), y el dinero es el sexto interruptor de la
   exportación (ADR 0021 §13).
