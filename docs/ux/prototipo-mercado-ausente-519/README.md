# Prototipo · el mercado ausente se dice (#519)

Maqueta de forma para el [#519](https://github.com/jenarvaezg/coindex/issues/519). El ticket ya
decidió que hay que decirlo; la maqueta contesta la pregunta que deja abierta al proponer que el
patrón de Ajustes «viaje a donde falta»:

> Ajustes distingue cinco motivos («Esperan a que haya red», «Se acabó el presupuesto…», «Faltan las
> credenciales…», «Se traen solos…», «Esperan a que termine el sincronizado»). ¿Viajan las cinco
> frases a «Las cifras» y a la lámina, o viaja sólo el patrón? ¿Y dónde cae la línea: ocupando el
> sitio de la sección, colgando del encabezado, o dibujada como un hueco?

Siete variantes × seis motivos × cuatro escenas, a dp real y con la de hoy de listón, en HTML y no
en Compose porque lo que se elige es estructura.

    python3 docs/ux/prototipo-mercado-ausente-519/extract.py
    python3 docs/ux/prototipo-mercado-ausente-519/build.py
    open /private/tmp/coindex-privado/mercado-ausente-519/maqueta.html

Barra `sticky` arriba con los tres ejes, y también por URL: `?e=holgada&m=presupuesto&v=A`. El
listón va siempre al lado de la variante elegida. No se publica como artifact ni se versionan
capturas, porque el estado de control lleva los importes de la colección del padre.

## Lo elegido

La F con la E encima: una frase fija en las dos pantallas, callada mientras el pase avanza solo.
Elegido por Jose el 15 de agosto de 2026 con la maqueta delante.

    «Las cifras»            EL VALOR
                            Llega cuando llegue el mercado.
                            ────────────────────────────────
                            LA MATERIA

    la lámina               100 bolívares de plata      3/4
                            EL DINERO LLEGA CUANDO LLEGUE EL MERCADO.
                            ┌─ Emisor ─────────────────┐

- **Una frase y no cinco.** Las cinco de Ajustes se quedan en Ajustes. Viaja el patrón (la ausencia
  se dice en vez de quedarse como un salto), no la redacción, que es lo único que permite el
  ADR 0026 §5 (ver más abajo: la mudanza literal rompe la cláusula 3).
- **Dice el porqué, no sólo la ausencia.** «Llega cuando llegue el mercado» frente a «Todavía no
  está»: una página que acaba de prometer «lo que vale» le debe al lector una razón, y la razón es
  el mercado.
- **El sujeto no se repite.** Bajo el eyebrow `EL VALOR` la frase arranca en el verbo. La primera
  versión decía «El valor llega cuando llegue el mercado» dos renglones bajo «EL VALOR».
- **En la lámina, «el dinero» y no «el valor y el coste».** Cuál de las dos cifras habría depende de
  si la lámina está cerrada y del umbral del ADR 0028 §1: nombrarlas prometería un «Coste de cerrar»
  a las 22 de 49 láminas que nunca tuvieron uno.
- **Callada con «en camino» y «sincronizando»**, los dos motivos que se arreglan solos en segundos.
  Una línea que aparece y desaparece sola es mobiliario; los otros tres esperan al coleccionista o
  al calendario.
- **La línea sustituye a las cifras, nunca las acompaña.** Así no vuelve por la puerta de atrás el
  total a medias que prohíbe el ADR 0028 §7.

## Las siete

| | tesis | veredicto |
| --- | --- | --- |
| **0** · Hoy · v1.4.1 | el dinero se va y no queda nada en su sitio | listón |
| **A** · la sección se queda y dice por qué | la explicación de Ajustes, mudada entera | descartada |
| **B** · un renglón del encabezado | no promete una sección: la frase cuelga de la frase | descartada |
| **C** · la ausencia aquí, el porqué en Ajustes | frase corta y una puerta | descartada |
| **D** · el importe dibujado como hueco | el vacío se ve, no se lee | descartada |
| **E** · sólo cuando esperar no sirve | callada mientras el pase avanza solo | elegida, encima de la F |
| **F** · una frase y no cinco | no distingue el motivo, así que no es la línea de Ajustes | elegida |

- La A es la lectura literal del ticket y la que rompe el ADR 0026 §5.3 (abajo). Además su longitud
  depende del motivo: de 5 palabras («Espera a que haya red») a 11 («Espera al mes que viene: se
  acabó el presupuesto de llamadas»), así que la cabecera cambia de alto por un estado que el
  coleccionista no ve.
- La B es la más barata (+26 dp) y la única que no promete un bloque, pero lo paga: sin eyebrow
  encima la frase tiene que decir «El valor» ella misma, y en la lámina no dice nada (su tesis es
  decirlo una vez), así que la lámina queda como hoy y el segundo criterio de aceptación sin
  cumplir.
- La C es la más limpia de ADR y de las más caras: +122 dp, y 34 de ellos son un botón «Por qué, en
  Ajustes» que duplica el glifo de Ajustes que ya lleva el cromo de «Las cifras» (`AlbumChrome.kt`),
  en una pantalla que el ADR 0026 §5 mide por palabras. En la lámina la puerta no existe (la
  cabecera lleva «Volver»), así que la variante tiene dos formas distintas según la pantalla.
- La D dibuja el importe como un hueco de papel profundo. Es la más cara (+135 dp) y la que más se
  acerca a lo que prohíbe el ADR 0028 §7: un rectángulo del alto exacto de la cifra, en el sitio de
  la cifra, se lee como un importe que está cargando, y lo que hay que decir es que no hay ninguno.

## Lo medido, sobre el dibujo

Del borde de la pantalla al arranque de «La materia» («Las cifras») o al cartón del primer hueco (la
lámina): lo que cada variante cobra por decir que el mercado no está. Con el motivo «sin red»; la A
y la B se alargan con los motivos más largos.

| | «Las cifras» | holgada (3/4) | sobre el umbral (1/12) | cerrada (3/3) |
| --- | ---: | ---: | ---: | ---: |
| Hoy, sin mercado | 189 dp | 360 dp | 373 dp | 360 dp |
| A | 276 (+87) | 385 (+25) | 398 (+25) | 385 (+25) |
| B | 215 (+26) | 360 (+0) | 373 (+0) | 360 (+0) |
| C | 311 (+122) | 385 (+25) | 398 (+25) | 385 (+25) |
| D | 324 (+135) | 419 (+59) | 432 (+59) | 419 (+59) |
| F | 276 (+87) | 385 (+25) | 398 (+25) | 385 (+25) |
| *y con mercado fresco* | *391* | *404* | *398* | *385* |

Lo que dice la última fila:

1. Decir la ausencia siempre cuesta menos que la sección que falta. En «Las cifras» el bloque del
   dinero empuja «La materia» a 391 dp; la variante más cara de las siete la deja en 324.
2. En la lámina sobre el umbral y en la cerrada, la línea cae justo donde caía la cifra (398 contra
   398 y 385 contra 385): la página no salta cuando llega el mercado. En una lámina con las dos
   cifras sí (385 contra 404), porque llega una línea más de la que se fue.

## Lo que sólo se vio al dibujarlo

1. La mudanza literal del ticket choca con el ADR 0026 §5, cláusula 3, que dice: *«Ajustes y
   onboarding están exentos por la regla de frecuencia, y a cambio se vigilan al revés: **ninguna de
   sus explicaciones puede aparecer en una pantalla de cuaderno**»*. Las cinco frases de
   `valuationLabel` son eso. La F cumple las dos cosas porque muda el patrón y no el texto: dice que
   el mercado no ha llegado sin decir por qué.
2. La misma ausencia no vale lo mismo en las cuatro escenas: una lámina cerrada no tiene «Coste de
   cerrar» porque no le falta nada, y una por encima del umbral porque el ADR 0028 §1 no pide esos
   precios. De ahí «el dinero», que cubre las dos cifras sin prometer ninguna.
3. El `null` de la lámina significaba tres cosas y sólo una merece la línea. `PlateSubject.value` lo
   dice por escrito: el mercado sin aterrizar, la lámina sin nada dentro, y el cajón de la
   exportación con el dinero apagado (ADR 0021 §13). `PlateMoney()` vacío las fundía en una, así que
   la ausencia pasó a tener un campo propio en vez de deducirse del hueco.
4. La lámina del escaparate no espera nada, y es la cuarta ausencia que parecía la misma: sus
   precios no dependen del pase de la colección (ADR 0030 §3) y llegan por un gesto propio. Poner la
   línea ahí sería pedirle al coleccionista que espere algo que sólo llega si lo pide.
5. El motivo transitorio no es un caso raro. «Se traen solos con la app abierta» es el estado normal
   de un arranque con red: si la línea saliera ahí, parpadearía en cada apertura de la app, justo en
   el caso que no hacía falta explicar.

## Cómo está hecha

`extract.py` saca el censo y las tres escaleras de la colección del padre reproduciendo
`collectionFigures` y `Ladders`; las cinco láminas y sus importes se reutilizan tal cual del
prototipo del [#493](https://github.com/jenarvaezg/coindex/issues/493). `build.py` escribe el HTML
autocontenido con las 168 pantallas dentro y tres ruedas que eligen cuál se ve. Ninguno de los dos
va en el APK ni en el pipeline.

- 411 × 914 dp, 1 px CSS = 1 dp, el Pixel 7 de las capturas del #296.
- El cromo y las medidas se leyeron en el código: `AlbumChrome` (54 dp), la `LazyColumn` de
  `FiguresScreen` (margen 20, `spacedBy` 26, `Block` con su filete a 14), la rejilla de
  `PlateScreen` (margen 20, calle 16, hueco de 104, `PLATE_MONEY_LINE_GAP` 4) y las once escalas de
  `fieldTypography`.
- Datos reales: 572 piezas de 34 emisores, 6,93 kg, 15,26 m y unos 94 cm, del inventario del padre
  cruzado con `data/numista-type-cache.json`.

## Lo que la maqueta no prueba

- Las siluetas de la escalera no están dibujadas (queda un hueco de 26 dp donde van, su alto real
  según `Silhouettes.kt`) porque no es lo que se elige aquí. El pliegue es fiel; los dibujos, no.
- El importe del control es el suelo de la plata, con el spot que trajo el prototipo del #493 el 14
  de agosto. La app enseñaría el mayor de tres precios, así que el real es más alto, y el bloque del
  dinero pasaría de los 391 dp medidos si el importe gana un dígito.
- El pliegue no se ha visto en un teléfono. Lo elegido sí: la implementación se comprobó en el AVD
  con «Sincronizar» pulsado en modo avión (lo que pone `held = Offline`), y las dos pantallas dicen
  su línea. Las capturas están en el anexo privado.

## Lo privado y lo que se tira

`data.json`, las fotos, la maqueta y las capturas están en
`/private/tmp/coindex-privado/mercado-ausente-519/`. Aquí quedan el método y las proporciones.

`extract.py` y `build.py` son del prototipo y se borran cuando el ticket se cierre; lo que sobrevive
es este README.
