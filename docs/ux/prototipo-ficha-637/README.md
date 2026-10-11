# Prototipo · la ficha de una moneda (#637)

Maqueta de forma para el [#637](https://github.com/jenarvaezg/coindex/issues/637), del mapa
[#640](https://github.com/jenarvaezg/coindex/issues/640). La pregunta: ¿qué lleva la ficha, y en
qué orden? Jose añadió el 11 de octubre de 2026: «siéntete libre de hacer un diseño chulo con una
vista entera y sacar incluso más detalles de lo que tenemos». Así que la 0 es la hoja de hoy y las
otras cuatro son la ficha a pantalla entera.

    python3 docs/ux/prototipo-ficha-637/extract.py
    python3 docs/ux/prototipo-ficha-637/build.py
    open /private/tmp/coindex-privado/ficha-637/maqueta.html

La barra de arriba elige variante (← →), moneda (↑ ↓) y modo: un teléfono, la tira entera con sus
pliegues de 914 dp, o las cinco en fila. Por URL: `#C-fuerte-tira`. La moneda gira al tocarla
donde lleva el giro (0, B, C y D).

## Lo que hay en el móvil y hoy no se enseña

Todo cuesta cero llamadas:

- **La ficha entera de Numista** (`type_meta.raw`): valor facial y su moneda, gobernante, serie o
  tema, ceca, peso, diámetro, grosor, canto con su leyenda, orientación, técnica, curso legal,
  referencias, grabadores, las dos caras con descripción y leyenda, los comentarios y las fichas
  emparentadas.
- **Cada pieza** (`collected_items.raw`): letra de ceca, fecha de compra (149 de 287 filas del
  padre), tirada de su emisión (233), nota privada (60), comentario de la emisión, si es para
  cambio y fotos propias (5).
- **Los precios de Numista en todos los grados** de cada emisión leída (`issue_prices`), no sólo
  el del grado de la pieza.
- **La nota del catálogo** (`variant_note`): por qué esa moneda está en esa lámina. Hoy sólo la
  lee la validación.

## Las cinco monedas

| | moneda | por qué |
| --- | --- | --- |
| **Compra y Numista** | 8 reales de Carlos IV, N#18852, desde «Historia del real» | dos piezas con lo pagado, Numista en su grado, nota privada, foto propia y nota del catálogo |
| **Un hueco** | Unicorn of Scotland 2 oz, N#127263, desde «The Queen's Beasts» | no la tiene: dos precios (ADR 0028 §8) |
| **Una medalla** | Cuatricentenario de Caracas, N#578835, desde Monedas | Numista la leyó y no le pone precio; no está en ninguna colección |
| **Sin plata** | 2 euros de la Policía Nacional, N#401375, desde «2 euros conmemorativos» | cuproníquel: no hay suelo de metal |
| **Muchas piezas** | 5 bolívares (Fuertes), N#10340, desde la casilla de 1929 | 31 piezas en 26 filas de un date run |

## Las cinco formas

| | tesis |
| --- | --- |
| **0 · Hoy** | la hoja de `CoinSheet.kt`: sube sobre la lámina, un valor, la ficha traída y las colecciones menos la de aquí |
| **A · Apilada** | pantalla entera, un solo documento, nada plegado; las dos caras a la vez |
| **B · Plegada** | cabe en una pantalla; cada sección dice su resumen cerrada y se abre al tocarla |
| **C · La escalera** | manda el dinero: tres precios por pieza con la etiqueta entera y la procedencia pegada, y el precio de su emisión en cada grado |
| **D · A tamaño real** | manda el objeto: la moneda a 1:1 con regla y canto, la leyenda transcrita, una cartela y el registro de piezas |

## Lo medido, sobre el dibujo

Alto de la ficha en dp (914 es una pantalla):

| | real | hueco | medalla | sin plata | fuerte |
| --- | ---: | ---: | ---: | ---: | ---: |
| 0 · la hoja | 348 | 347 | 373 | 348 | 348 |
| A | 2875 | 2468 | 1693 | 2637 | 4903 |
| B, cerrada | 775 | 812 | 710 | 775 | 710 |
| C | 2838 | 2270 | 1669 | 2736 | 2610 |
| D | 3127 | 2695 | 1881 | 2805 | 4659 |

Sin tocar nada, A enseña el valor, las piezas y la colección; C, el valor y la primera pieza; D,
la moneda, su leyenda y la cartela; B, todas las secciones cerradas.

## Lo que sólo se vio al dibujarlo

1. **Los tres precios son de la pieza, no de la moneda.** El Fuerte tiene lo pagado en 2 de sus
   26 filas y precio de Numista en 22: un «Lo que pagaste» por moneda sería un total a medias
   (ADR 0028 §7). La cifra de la moneda sigue siendo el «Valor actual», la suma de los mayores, y
   la escalera va pieza a pieza; con más de tres piezas, la C la pone en tabla.
2. **La ficha de un hueco no dice hoy ningún precio** (`value` es nulo sin piezas), aunque el
   móvil tiene los dos del ADR 0028 §8. Las variantes dicen «Te costaría: … · en sin circular»
   con Numista en `unc` y la plata debajo.
3. **Cada peldaño vacío dice por qué**: «Numista no le pone precio» (la medalla se leyó y vino
   sin precio, que no es lo mismo que no haberla leído), «no lleva plata», «no anotaste lo que
   pagaste».
4. **La nota del catálogo está escrita para el curador**: habla del padre en tercera persona
   («El padre tiene dos, el 1803 (FT) y el 1805 (TH)») y cita issues. La maqueta la enseña tal
   cual; en la app pediría otra voz o un campo aparte.
5. **Numista pone precio a pocos grados y los repite**: el 8 reales de 1803 vale lo mismo en `vf`
   que en `xf`. El gráfico de la C enseña los huecos como huecos.
6. **El tamaño real cabe siempre**: la moneda más grande del padre mide 42 mm, que son 265 dp de
   los 371 útiles.
7. **La foto propia de una pieza puede ser un documento**: la del 8 reales de 1803 es el
   certificado de autenticidad del vendedor. Va como miniatura junto a la pieza y nunca dentro
   del hueco.
8. **Media ficha llega en inglés**: el título, la ceca («Mexican Mint»), los comentarios de
   emisión y, en la medalla, las descripciones, que el padre escribió en inglés.
9. **El empate decide la etiqueta**: en los 2 euros, Numista y lo pagado coinciden, y
   `pieceValue` se queda con Numista por orden. Es inofensivo, pero la escalera marca sólo uno.

## Lo que la maqueta no prueba

- Nada de esto se ha visto en un teléfono: el navegador elige y el emulador confirma.
- Las colecciones son sólo las láminas curadas que reclaman el tipo, en el orden de `data/`; la
  app añade las derivadas y ordena por el índice. La de aquí lleva «· aquí» como pide el #635.
- El «a tamaño real» supone 160 dp por pulgada; un Pixel 7 se desvía un 1 %.

## Lo privado y lo que se tira

Los importes, las notas privadas del padre y su foto no se versionan: `data.json`, `fotos/` y
`maqueta.html` viven en `/private/tmp/coindex-privado/ficha-637/`. Aquí quedan el método y las
medidas. `extract.py` y `build.py` son del prototipo y se tiran cuando el ticket se cierre; lo que
sobrevive es este README.

## Lo elegido

Pendiente de Jose.
