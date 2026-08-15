# Prototipo · el mercado ausente se dice (#519)

Maqueta de forma para el [#519](https://github.com/jenarvaezg/coindex/issues/519). La pregunta que
contesta no es «¿hace falta decirlo?» —el ticket ya decidió que sí— sino la que deja abierta al
proponer que el patrón de Ajustes «viaje a donde falta»:

> **Ajustes distingue cinco motivos** («Esperan a que haya red», «Se acabó el presupuesto…»,
> «Faltan las credenciales…», «Se traen solos…», «Esperan a que termine el sincronizado»). ¿Viajan
> las cinco frases a «Las cifras» y a la lámina, o viaja sólo el patrón? ¿Y dónde cae la línea:
> ocupando el sitio de la sección, colgando del encabezado, o dibujada como un hueco?

Siete variantes × seis motivos × cuatro escenas, a dp real y con la de hoy de listón. En HTML y no
en Compose porque lo que se elige es **estructura** (`prototipar-forma-en-html`).

    python3 docs/ux/prototipo-mercado-ausente-519/extract.py
    python3 docs/ux/prototipo-mercado-ausente-519/build.py
    open /private/tmp/coindex-privado/mercado-ausente-519/maqueta.html

Barra `sticky` arriba con los tres ejes, y también por URL: `?e=holgada&m=presupuesto&v=A`. El
listón viaja siempre al lado de la variante elegida. **No se publica como artifact ni se versionan
capturas**: el estado de control lleva los importes de la colección del padre
(`dinero-fuera-del-repo-publico`).

## Lo elegido

**La F con la E encima: una frase fija en las dos pantallas, callada mientras el pase avanza solo.**
Elegido por Jose el 15 de agosto de 2026 con la maqueta delante.

    «Las cifras»            EL VALOR
                            Llega cuando llegue el mercado.
                            ────────────────────────────────
                            LA MATERIA

    la lámina               100 bolívares de plata      3/4
                            EL DINERO LLEGA CUANDO LLEGUE EL MERCADO.
                            ┌─ Emisor ─────────────────┐

- **Una frase y no cinco.** Las cinco de Ajustes se quedan en Ajustes. Lo que viaja es el *patrón*
  —la ausencia se dice en voz alta en vez de quedarse como un salto— y no la redacción, que es lo
  único que ADR 0026 §5 permite (ver más abajo: la mudanza literal rompe la cláusula 3).
- **Dice el porqué y no sólo la ausencia.** «Llega cuando llegue el mercado» contra «Todavía no
  está»: una página que acaba de prometer «lo que vale» le debe al lector una razón, y el mercado es
  toda ella.
- **El sujeto no se repite.** Bajo el eyebrow `EL VALOR` la frase arranca en el verbo. La primera
  vuelta decía «El valor llega cuando llegue el mercado» dos renglones bajo «EL VALOR», que es la
  misma palabra dos veces seguidas.
- **En la lámina, «el dinero» y no «el valor y el coste».** Cuál de las dos cifras iba a haber
  depende de si la lámina está cerrada y del umbral de ADR 0028 §1: nombrarlas prometería un «Coste
  de cerrar» a las **22 de 49** láminas que nunca tuvieron uno.
- **Callada con «en camino» y «sincronizando».** Los dos motivos que se arreglan solos en segundos,
  sin que nadie haga nada. Una línea que aparece y desaparece sola es mobiliario; los otros tres
  esperan al coleccionista o al calendario.
- **La línea sustituye a las cifras, nunca las acompaña.** Es lo que impide que vuelva por la puerta
  de atrás el total a medias que ADR 0028 §7 existe para prohibir.

## Las siete

| | tesis | veredicto |
| --- | --- | --- |
| **0** · Hoy · v1.4.1 | el dinero se va y no queda nada en su sitio | listón |
| **A** · la sección se queda y dice por qué | la explicación de Ajustes, mudada entera | descartada |
| **B** · un renglón del encabezado | no promete una sección: la frase cuelga de la frase | descartada |
| **C** · la ausencia aquí, el porqué en Ajustes | frase corta y una puerta | descartada |
| **D** · el importe dibujado como hueco | el vacío se ve, no se lee | descartada |
| **E** · sólo cuando esperar no sirve | callada mientras el pase avanza solo | **elegida, encima de la F** |
| **F** · una frase y no cinco | no distingue el motivo, así que no es la línea de Ajustes | **elegida** |

- **La A** es la lectura literal del ticket y es la que rompe ADR 0026 §5.3 (abajo). Además su
  longitud depende del motivo: de 5 palabras («Espera a que haya red») a 11 («Espera al mes que
  viene: se acabó el presupuesto de llamadas»), así que la cabecera cambia de alto por un estado que
  el coleccionista no ve.
- **La B** es la más barata (+26 dp) y la única que no promete un bloque, pero paga por ello: sin
  eyebrow encima la frase tiene que decir «El valor» ella misma, y en la lámina no dice nada —su
  tesis es que se dice una vez— así que la lámina se queda exactamente como está hoy, que es el
  segundo criterio de aceptación sin cumplir.
- **La C** es la más limpia de ADR y la más cara: **+122 dp**, y 34 de ellos son un botón «Por qué,
  en Ajustes» que duplica el glifo de Ajustes que el cromo de «Las cifras» ya lleva
  (`AlbumChrome.kt:53`). Y en la lámina la puerta no existe —el masthead lleva «Volver»— así que la
  variante se parte en dos formas distintas según la pantalla.
- **La D** dibuja el importe como un hueco de papel profundo. Es la más cara (+135 dp) y la que más
  se acerca a lo que ADR 0028 §7 prohíbe: un rectángulo del alto exacto de la cifra, en el sitio de
  la cifra, se lee como un importe que está cargando, y lo que hay que decir es que no hay ninguno.

## Lo medido, sobre el dibujo

Del borde de la pantalla al arranque de «La materia» («Las cifras») o al cartón del primer hueco (la
lámina), que es lo que cada variante cobra por decir que el mercado no está. Con el motivo «sin
red»; la A y la B se alargan con los motivos más largos.

| | «Las cifras» | holgada (3/4) | sobre el umbral (1/12) | cerrada (3/3) |
| --- | ---: | ---: | ---: | ---: |
| Hoy, sin mercado | 189 dp | 360 dp | 373 dp | 360 dp |
| A | 276 (+87) | 385 (+25) | 398 (+25) | 385 (+25) |
| B | 215 (+26) | 360 (+0) | 373 (+0) | 360 (+0) |
| C | 311 (+122) | 385 (+25) | 398 (+25) | 385 (+25) |
| D | 324 (+135) | 419 (+59) | 432 (+59) | 419 (+59) |
| **F** | **276 (+87)** | **385 (+25)** | **398 (+25)** | **385 (+25)** |
| *y con mercado fresco* | *391* | *404* | *398* | *385* |

Dos cosas que sólo se ven en esa última fila:

1. **Decir la ausencia siempre es más barato que la sección que falta.** En «Las cifras» el bloque
   del dinero empuja «La materia» a 391 dp; la variante más cara de las siete la deja en 324.
2. **En la lámina sobre el umbral y en la cerrada, la línea cae exactamente donde caía la cifra.**
   398 contra 398 y 385 contra 385: la página **no salta** cuando el mercado llega. En una lámina
   con las dos cifras sí —385 contra 404— porque llega una línea más de la que se fue.

## Lo que sólo se vio al dibujarlo

1. **La mudanza literal del ticket choca con ADR 0026 §5, cláusula 3.** Está escrita así: *«Ajustes
   y onboarding están exentos por la regla de frecuencia, y a cambio se vigilan al revés: **ninguna
   de sus explicaciones puede aparecer en una pantalla de cuaderno**»*. Las cinco frases de
   `valuationLabel` son exactamente eso. La F cumple las dos cosas a la vez porque lo que muda es el
   patrón y no el texto: dice que el mercado no ha llegado sin decir por qué no ha llegado.
2. **La misma ausencia no vale lo mismo en las cuatro escenas, y por poco se escribe que sí.** Una
   lámina cerrada y una por encima del umbral **nunca tuvieron «Coste de cerrar»**: la primera
   porque no le falta nada, la segunda porque ADR 0028 §1 no pide esos precios. Una línea que diga
   «el valor y el coste esperan» promete en 22 de las 49 láminas del padre una cifra que no va a
   llegar nunca. De ahí «el dinero», que cubre las dos sin reclamar ninguna.
3. **El `null` de la lámina hoy significa tres cosas y sólo una merece la línea.** `PlateSubject.value`
   lo dice por escrito: el mercado sin aterrizar, la lámina que no tiene nada dentro, y el cajón de
   la exportación con el dinero apagado (ADR 0021 §13). `PlateMoney()` vacío las funde en una, así
   que la ausencia tuvo que ganar un campo propio en vez de deducirse del hueco.
4. **La lámina del escaparate no espera nada**, y es la cuarta ausencia que parecía la misma: sus
   precios no dependen del pase de la colección (ADR 0030 §3), llegan por un gesto propio. Poner la
   línea ahí sería decirle al coleccionista que espere algo que sólo llega si lo pide.
5. **En «Las cifras» la puerta a Ajustes ya está**, en el glifo del cromo. Es lo que hunde a la C:
   paga 34 dp por un segundo camino al mismo sitio, en una pantalla que ADR 0026 §5 mide por
   palabras.
6. **El motivo transitorio no es un caso raro.** «Se traen solos con la app abierta» es el estado
   normal de un arranque con red: si la línea saliera ahí, el estreno de esta función sería un
   parpadeo en cada apertura de la app, y lo que el ticket quiere decir se gastaría en el caso que
   no hacía falta explicar.

## Cómo está hecha

`extract.py` saca el censo y las tres escaleras de la colección del padre reproduciendo
`collectionFigures` y `Ladders`; las cinco láminas y sus importes **se reutilizan tal cual** del
prototipo del [#493](https://github.com/jenarvaezg/coindex/issues/493), que ya los había extraído.
`build.py` escribe el HTML autocontenido con las 168 pantallas dentro y tres ruedas que eligen cuál
se ve. Ninguno de los dos vive en el APK ni en el pipeline.

- **411 × 914 dp**, 1 px CSS = 1 dp, el Pixel 7 de las capturas del #296.
- **El cromo y las medidas se leyeron en el código**: `AlbumChrome` (54 dp), la `LazyColumn` de
  `FiguresScreen` (margen 20, `spacedBy` 26, `Block` con su filete a 14), la rejilla de
  `PlateScreen` (margen 20, calle 16, hueco de 104, `PLATE_MONEY_LINE_GAP` 4) y las once escalas de
  `fieldTypography`.
- **Datos reales**: 572 piezas de 34 emisores, 6,93 kg, 15,26 m y unos 94 cm, del inventario del
  padre cruzado con `data/numista-type-cache.json`.

## Lo que la maqueta no prueba

- **Las siluetas de la escalera no están dibujadas** —hay 26 dp de hueco donde van, que es su alto
  real (`Silhouettes.kt:176`)— porque no es lo que se elige aquí. El pliegue es honesto; los bichos
  no.
- **El importe del control es el suelo de la plata**, con el spot que trajo el prototipo del #493 el
  14 de agosto. La app enseñaría el mayor de tres precios, así que el de verdad es más alto y el
  bloque del dinero es **más alto todavía** que los 391 dp medidos si el importe crece de dígito.
- **El pliegue no se ha visto en un teléfono** (`medir-en-el-movil-no-en-el-asset`). Lo elegido sí:
  la implementación se comprobó en el AVD con «Sincronizar» pulsado en modo avión —que es lo que
  pone `held = Offline`— y las dos pantallas dicen su línea. Las capturas están en el anexo privado.

## Lo privado y lo que se tira

`data.json`, las fotos, la maqueta y las capturas están en
`/private/tmp/coindex-privado/mercado-ausente-519/`. Aquí quedan el método y las proporciones.

`extract.py` y `build.py` son del prototipo y se borran cuando el ticket se cierre. Lo que sobrevive
es este README.
