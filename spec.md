# spec.md — Coindex

> Especificación viva de Coindex. **Los ADR de `docs/adr/` son la autoridad**: cuando discrepen
> con este fichero, mandan ellos, que son posteriores. Aquí vive lo que no es una decisión de
> arquitectura —la forma del producto, la API de Numista y lo que costó aprender curando— más el
> mapa de qué ADR decide cada cosa.
>
> **Lee la sección que toca, no el fichero entero:**
>
> | § | qué contesta |
> | --- | --- |
> | 0.1 | qué es Coindex, y cómo se distribuye |
> | 0.2 | qué activos del repo lee la app, y cómo identifica sus casillas cada `schema_version` |
> | 0.3 | los invariantes del dominio: clave de variante, familias, nombres, orden |
> | 0.4 | el eje de identidad visual, y dónde se decide cada parte de la forma |
> | 0.5 | cómo se habla con la API de Numista sin tropezar |
> | 0.6 | cómo se cura un catálogo |
> | 0.7 | lo que costó aprender curando, caso a caso |
> | 0.8 | las dos reglas del arranque que no son tarea cumplida |
> | 0.9 | las cuatro cuestiones abiertas de la fase Android, y cómo se cerraron |
>
> **PIVOT (29 de julio de 2026): Coindex es una app de Android local-first.** Shuttle dejó de
> funcionar y se descartó el despliegue web. El dominio se portó a Kotlin y el **30 de julio de
> 2026 la implementación Rust se retiró del árbol de trabajo**: vive en el tag `rust-frozen`
> (`git checkout rust-frozen`) junto con la especificación de aquella fase, que describía un
> modelo —`Slot`, emparejamiento heurístico, `ManualOverride`— que no se portó y no vuelve
> (ADR 0010 §2, ADR 0021 §12).
>
> **Y aquí no hay censo** (§0.7): cuántos catálogos, agrupaciones, programas o veredictos hay se
> mide sobre `data/`, cambia cada semana, y cualquier cifra escrita aquí nace caducada — y pone
> roja la sesión de al lado.

---

## 0. Fase Android — especificación de arranque

### 0.1 Decisión y forma del producto

- **App de Android** instalada por **APK directo** (sideload). Dos usuarios reales: Jose
  y su padre. Sin Play Store, sin cuenta de desarrollador, sin backend propio.
- **Local-first**: en el primer arranque el usuario introduce su **API key de Numista**
  (y su user id); a partir de ahí todo funciona contra la API de Numista y una base
  **SQLite local**. Cada usuario gasta su propio presupuesto de API (~1.500-2.000
  llamadas/mes por key), así que desaparece el ledger compartido.
- **Framework**: recomendación acordada **Kotlin + Jetpack Compose** (la app es
  Android-only: grids de imágenes + datos locales). Stack sugerido: Room (SQLite),
  Retrofit o Ktor para la API, Coil para imágenes, EncryptedSharedPreferences/Keystore
  para la API key. Flutter solo si se quisiera abrir la puerta a iOS más adelante;
  React Native descartado.
- **Fuera de alcance**: compartir enlaces/URLs públicas, cuentas, sincronización entre
  dispositivos, escritura hacia Numista. **Dentro como extra**: exportar una lámina como
  imagen (render a bitmap + share intent de Android).
- Firma del APK: generar un keystore y **conservarlo** (las actualizaciones deben ir
  firmadas con la misma clave). Distribución por release privado de GitHub o similar.

### 0.2 Activos del repo que la app reutiliza tal cual

1. **`data/collection-catalogs/*.json` — los catálogos curados** (el activo más caro de
   reproducir). Todos los `numista_type_id` fueron verificados contra numista.com antes de
   versionarse. Se empaquetan como assets de la app. **Qué afirma un catálogo —su
   `series_status`, el `status` de cada miembro, su fuente, su denominador y la cara que
   imprime— lo especifica el ADR 0020**; lo que sigue es sólo cómo identifica sus casillas.
   Cuáles hay de cada clase se lee en `data/`, no aquí:
   - `schema_version: 1`: miembros identificados por `numista_type_id` único; posesión =
     poseer el tipo. Fuente obligatoria: página de serie de Numista
     (`catalogue/series.php?id=N`).
   - `schema_version: 2` (**date runs**, ADR 0009): los miembros repiten un mismo tipo con años
     distintos (único por `(numista_type_id, year)`); posesión = poseer el tipo **y** que
     `issue_year` (o `gregorian_year`) del item coincida con el año del miembro. Un item sin año
     nunca rellena un hueco. La fuente puede ser la ficha del tipo (`catalogue/piecesNNN.html`).
   - `schema_version: 3` (**conjuntos emitidos como set**, ADR 0012): los miembros abarcan
     varias variantes físicas, así que el catálogo no declara `weight_millioz`, `finish` ni
     `metal`, y su clave de variante lleva el **peso ausente** (`-1` al persistirse). Un tipo
     listado aquí deriva esa clave incluso si Numista le da familia: nombrar los tipos exactos
     que se emitieron juntos es una afirmación más específica que la agrupación de Numista.
     Criterio estrecho a propósito: **solo** conjuntos emitidos como un producto. El bullion
     fraccional no es un conjunto: ¼ oz y 1 oz son la misma moneda en dos tamaños y siguen
     siendo colecciones separadas.
   - `schema_version: 5` (**issue runs**, ADR 0014): los miembros son emisiones de un mismo tipo
     que comparten año y se distinguen por variedad, identificadas por `numista_issue_ids`. Un
     catálogo simple o un date run puede **cualificar** una casilla con ese mismo campo sin ser
     un issue run (ADR 0019).
   - `schema_version: 4` **sigue libre**, y por qué lo cuenta §0.9.
2. **`data/numista-type-cache.json`** — snapshot de la caché de metadatos de tipos (respuestas
   íntegras de `GET /types/{id}?lang=es`), empaquetado como seed de la tabla de caché para que
   ningún usuario gaste esas llamadas. Cubre **todos** los tipos referenciados por los ficheros
   curados, así que las láminas muestran todos los diseños (incluidos los «me falta») sin tocar
   el presupuesto. Sembrar es parte de curar: `scripts/seed-type-cache.py` omite lo ya cacheado
   y dice el coste con `--dry-run`, y `TypeCacheSeedTest` se pone rojo con la lista de los que
   falten.
3. **`data/series/*.json`** — **retirado**: las dos series curadas históricas (Lunar III, Tudor
   Beasts) no las leía nadie (ADR 0010 §2) y viajaban en el APK como assets muertos. El material
   de Lunar III (doce casillas con etiqueta, motivo y `release_status`, sin un solo
   `numista_type_id`) queda en el commit `9fc2582` para el catálogo que se cure. Ya no hay código
   que las valide en `main`.
4. **`docs/adr/`** — las decisiones de dominio son la especificación del comportamiento, y su
   índice es el propio directorio: enumerarlas aquí sólo produce una lista que envejece. El
   ADR 0020 es el que dice qué afirma un catálogo.

### 0.3 Modelo de dominio (invariantes)

- **Clave de variante física** (identidad de tarjeta, de preferencia y de catálogo):
  `(familia Numista cruda, peso normalizado en mili-onzas, acabado)`.
  - Peso: `round(oz*1000)`, con imán a los pesos comunes `[250, 500, 1000, 2000, 5000,
    10000]` y **sólo** a ésos si la diferencia es ≤10 (31,1 g → 1000; 30 g → 965, nunca
    1000). Lo que declara un catálogo manda sobre la variante de **sus** miembros (ADR 0016)
    y no imanta a nadie más: los 13,96 g del Oporto se quedan en 449 y lo que los junta con
    sus hermanos de 450 es el fichero que nombra a los siete (#288). Ausente (`-1` al
    persistirse) en los conjuntos emitidos como set, que no tienen un peso único.
  - Acabado (`Finish`): inferido del título del tipo con reglas auditables
    (`proof`+colour → ProofColoured; `proof`; colour/`coloriz`/colores lunares; `gild`/
    `dorad`; `antiqu`; `bullion` o series Lunar III / Tudor Beasts → Bullion; si no →
    desconocido). Numista no expone un campo de acabado estable. La excepción es el
    dorado, que se lee de `composition.text` **antes** que nada del título (#573): las 15
    libras redondas doradas se titulan «Silver Proof» palabra por palabra como las 32 que
    no lo son, y lo único que las parte es «(with selective gold plating)» en la
    composición. Una moneda **de** oro no cuenta, y el guardarraíl es el metal dominante:
    la cabeza de esa frase dice de qué está hecha y lo que sigue al paréntesis, cómo está
    acabada.
- **Colecciones derivadas**: agrupan solo piezas actuales del usuario por clave de
  variante exacta. Sin familias difusas, sin mezclar pesos ni acabados. Cantidades: tipos
  distintos + piezas. Desde el ADR 0021 §8 no se llaman «propuestas»: la palabra murió con
  el gesto de seguirlas.
- **Precedencia de familia** (ADR 0009, 0012 y 0013), de la afirmación más específica a la
  más débil: catálogo de conjunto (`schema_version: 3`) que nombra el tipo → catálogo de
  colección seleccionado para el tipo y la pieza → familia real de Numista → agrupación
  curada que nombra el tipo → familia técnica `System YYYY[-YYYY]`. Una familia técnica ya
  no descarta la pieza: es la familia más débil, y se formatea «Sistema monetario
  YYYY-YYYY». Los cinco peldaños siguen resolviendo la familia y **dejan de ser dato de
  pantalla** (ADR 0021 §3). Y desde el ADR 0021 §1 ninguna pieza se pierde por no tener
  familia: vive en Monedas, con el filtro «Sin colección».
- **Disposiciones**: retiradas enteras (ADR 0021 §7, que supera al 0008).
  `Available`/`Followed`/`Ignored`, `DerivedCollectionStance` y
  `collection_proposal_preferences` desaparecen; la tabla se va con un `DROP` en la
  migración v5 y no queda **nada persistido por tarjeta**.
- **Lámina de catálogo**: navegable cuando la colección existe hoy (hay piezas de la
  variante), hay catálogo para ella y el usuario posee ≥1 `type_id` oficial del catálogo
  (la evidencia es **por tipo**, también en date runs). Tres razones de indisponibilidad y
  ninguna más: `UnknownCatalog`, `NotACollection`, `NoEvidence`. Estados `Tengo (×n)` /
  `Me falta`.
- **Nombre de tarjeta**: `short_name` en el fichero curado —obligatorio, único en el índice
  y prefijo de `name`—, y la familia cruda de Numista verbatim cuando no hay fichero. Los
  seis alias editoriales de familia **están retirados** (ADR 0021 §4, hechos en la v0.12.0
  con #166): los dos etiquetados que quedan en código son `System 1879-1936` → «Sistema
  monetario 1879-1936» y los nueve códigos de emisor cuya etiqueta de Numista no es un país
  —`russie` → «Rusia» (ADR 0023)—, los dos formateo de una cadena generada. Una caja propia
  lleva un solo nombre, con `name == short_name` y 40 caracteres de techo.
- **Orden del índice**: un único comparador `(tiene ratio ↓, ratio ↓, denominador ↓,
  short_name ↑)` (ADR 0021 §6).

### 0.4 Identidad y forma

La web congelada dejó de ser la referencia el 4 de agosto de 2026. La **arquitectura de
información** la escribe el
**[ADR 0021](docs/adr/0021-what-a-collection-is-and-the-top-level.md)** y la **forma** —qué se ve,
qué se mueve y cuánto texto cabe— el
**[ADR 0026](docs/adr/0026-the-shape-of-coindex-an-album-sheet.md)**. Los dos son la
especificación, **y no se resumen aquí**: un resumen que no manda es un documento más que hay que
volver a sincronizar cada vez que se envía una pantalla, y que miente entre una cosa y la otra.

Lo que sí vive aquí es el **eje de identidad**, porque lo declaró este documento y los ADR y los
mapas de `docs/ux/` lo citan como el veto que aplican:

- **Guía de campo ornitológica, no cuadro de mandos.** Serif para los textos, condensada para los
  datos, paleta apagada de papel. Es el eje de identidad del producto y **no se reabre**.
- **Coindex es una hoja de álbum, no un listado** (ADR 0026 §1): una colección es un hueco
  troquelado con su moneda dentro, y la lámina la misma hoja por años, con el diseño en fantasma
  donde falta la pieza. Papel de fibra fina, sin sombra de hoja; el único brillo fijo es el
  reflejo del acetato.
- **Papel a cualquier hora** (ADR 0026 §2): la app no sigue el tema oscuro del sistema, no hay
  interruptor en «Este teléfono», y `android:forceDarkAllowed` está a `false` en `Theme.Coindex`. La noche
  la pone el sistema atenuando el panel. Lo fija `SinglePaletteTest`, que además guarda los dos
  suelos de contraste de la paleta sobre el papel: `muted` ≥ 4,50 (texto) y `hairline` ≥ 3,00 (no
  textual).
- **Dos tipografías dentro del APK**: **Bitter + Barlow Condensed**, 245 KB, con versalitas y
  cifras tabulares de verdad. Sin itálica y sin subsetear. `←`, `✓` y `↗` son iconos vectoriales,
  porque ninguna de las dos los trae.
- **La app no es superficie de auditoría** (ADR 0021 §12): ni línea de razón en la ficha, ni
  gesto de «esta no va aquí». El desacuerdo se informa fuera, en un script que nunca se pone
  rojo. El dinero es la excepción declarada del ADR 0026 §10, y llega hasta donde llega el
  compañero de compra.
- **No hay fotos propias de las piezas**, descartado el 7 de agosto de 2026
  ([#15](https://github.com/jenarvaezg/coindex/issues/15)): las fotos son las de Numista. Con
  ellas siguen fuera el relieve y la reiluminación interactiva. La promesa contraria vivía en la
  especificación original —`rust-frozen:spec.md`— y **se retira aquí**.

Y el mapa de dónde se decide cada parte de la forma:

| qué | dónde se decide |
| --- | --- |
| Las tres jerarquías del primer nivel, su grano y su recuento | ADR 0021 §1, ADR 0026 §8 |
| Qué es una colección, y que ninguna pieza se pierde por no tener familia | ADR 0021 §1 |
| Que no queda nada persistido por tarjeta | ADR 0021 §7 |
| El orden del índice, los filtros y el estante | ADR 0021 §6, ADR 0026 §8 |
| La tarjeta: un hueco, una foto y una fracción, sin eyebrow ni línea de variante | ADR 0026 §12 |
| La lámina, el giro, la chapa del año y el sello de «completa» | ADR 0026 §1; ADR 0020 (`printed_side`) |
| El nombre de una moneda, y su ficha por dentro | ADR 0026 §7, §13 |
| Los dos años de una pieza, y los dos ejes del cuaderno | ADR 0026 §9 |
| «Las cifras»: qué vale una pieza, y qué no se totaliza | ADR 0026 §10 |
| El pase de tasación, los tres estados y las caducidades | ADR 0026 §11, ADR 0028 |
| «Lo que busco»: la casilla marcada, el anexo y el gasto elástico | ADR 0029 |
| «Explorar»: las veinte láminas que no coleccionas, y tasarlas a mano | ADR 0030 |
| El techo de movimientos, y qué debe una causa y un dato | ADR 0026 §3 |
| La regla de exportación: lo quieto viaja al papel | ADR 0026 §4 |
| Exportar la lámina en PNG y el cuaderno en PDF | ADR 0010 §8, ADR 0021 §13 |
| El listón de densidad, y la regla de frecuencia | ADR 0026 §5 |
| Una cadena, un dueño (`CopyLivesInOnePlaceTest`) | ADR 0026 §6 |
| Avisos y licencias | ADR 0026 §14 |
| Los nombres de país que la etiqueta de Numista no da bien | ADR 0023 |
| Que el presupuesto de llamadas no aparece en la interfaz | ADR 0026 §5 |

### 0.5 API de Numista — todo lo aprendido (válido para la app)

- Base `https://api.numista.com/v3`. Auth: cabecera `Numista-API-Key` + token OAuth
  `client_credentials` con **`scope=view_collection`**. Omitir el scope produce un 401 con
  un mensaje engañoso; es el error más común con esta API. El token dura del orden de 10
  minutos: cachearlo en memoria con margen y renovarlo, no pedir uno por petición.
- Endpoints: `GET /users/{id}/collected_items` (sin paginación, ADR 0006),
  `GET /types/{id}?lang=es`, y `GET /types/{id}/issues` (lista de emisiones por año —
  la fuente para curar date runs; así se verificaron las 21 fechas del 5 Bolívares).
- De `collected_items` interesa por item: `id`, `quantity`, `type.id`, `type.title`,
  `type.issuer.code`, `issue.year`, `issue.gregorian_year`, `grade`, `price`, `for_swap`,
  `collection.name`. De `/types/{id}`: `title`, `issuer`, `min_year`/`max_year`, `weight`,
  `size` (diámetro), `thickness`, `shape`, `orientation`, `composition`,
  `commemorated_event`, las referencias de catálogo y las URLs de imagen.
- **El esquema real manda sobre este documento**: golpear la API una vez por endpoint,
  grabar la respuesta como fixture y derivar los tipos de la respuesta real. Todos los
  campos son opcionales salvo los que se compruebe que siempre vienen — el catálogo lo
  rellenan voluntarios y está lleno de huecos.
- **Disciplina de presupuesto** (portada tal cual desde la fase web): caché permanente de
  tipos (un tipo descargado no se vuelve a pedir), contador local de llamadas del mes con
  techo configurable, y tests sin red (fixtures en `fixtures/numista/`).
- Imágenes: URLs en la respuesta de `/types/{id}`; en la app se cargan directo con Coil
  (ya no hay proxy). Los términos de Numista restringen la extracción sistemática, si bien
  su administrador ha manifestado públicamente que el uso personal con tráfico razonable no
  es problemático; sin scraping en runtime.
- *(Sólo si algún día se escribe hacia Numista: en el `POST` de items el campo `type` debe
  enviarse como cadena JSON `"44"` y no como número, pese a que el esquema declara un
  entero. Un número produce un 400 sin pista.)*

### 0.6 Curación de catálogos (pipeline de desarrollo, no de la app)

Los catálogos se generan en el repo y viajan con cada actualización de la app. Proceso
usado (julio 2026) y reglas aprendidas:

1. Localizar la serie: la ficha de cualquier tipo poseído enlaza `series.php?id=N`.
2. Listado completo: `catalogue/index.php?se=N&nb=50&p=X`. Cloudflare devuelve 403 a
   curl en p≥2; un navegador real (Playwright) con `fetch` in-page funciona.
3. **Verificar cada type_id contra Numista antes de versionarlo** (los listados de
   terceros traen errores sutiles: años intercambiados, piezas de cuproníquel que
   parecen plata). La API (`/types/{id}`) es la forma barata de verificar.
4. **Un catálogo = una variante física** (el peso/acabado que se colecciona): filtrar
   por denominación + acabado. Excepción: si un diseño de un año solo existe en otro
   acabado del mismo peso y metal, se incluye (no perder diseños; caso cocodrilo 2015).
5. Series anuales: **una entrada por año**, sin privies/coloured/gilded/proof/high
   relief/sets. Cuidado con líneas paralelas: Numista mezcla Perth Mint y Royal
   Australian Mint en la misma serie (koala, lunar); separar por el campo `mints` de la
   API.
6. **El bullion de diseño estable sí se cataloga**, como date run con una casilla por año
   aunque el diseño no cambie ([#57](https://github.com/jenarvaezg/coindex/issues/57), que
   revierte la regla de julio: Maple, Krugerrand, ASE, Britannia, Philharmonic, Noah's Ark y
   Kangaroo tienen todos su date run). El límite editorial es «una moneda al año que tendría
   sentido comprar», que deja fuera la moneda de circulación y la reacuñación con fecha
   congelada. La intención dejó de declararse en ninguna parte —el ADR 0021 §7 retiró la
   disposición—, así que lo que dice hasta dónde va una colección es su **ratio de cobertura**, que
   es un hecho medido. Ver ADR 0020.
7. Lo que parte una lámina de otra es la **variante física**, no el diseño: dos monedas del
   mismo año con el mismo peso, acabado y metal son dos casillas de una misma lámina, y un
   privy o un animal nuevo no parten nada. Un acabado distinto sí es otra lámina.

### 0.7 Lecciones de curar (lo que no se vuelve a aprender)

**Aquí no hay censo.** Cuántos catálogos, agrupaciones, programas, fichas o veredictos hay se
mide sobre `data/`, cambia cada semana y cualquier cifra escrita aquí nace caducada — y pone
roja la sesión de al lado. Lo que sigue es lo que costó aprender.

**Una ausencia puede ser la señal.** El estuche venezolano de 1975 (`schema_version: 3`) llegó
por donde no la había: 28,28 g y 35 g no comparten clave de variante ni la compartirán nunca, así
que **ninguna colección derivada podía sugerirlo jamás** y fue precisamente su ausencia la que
delató el conjunto. La lámina afirma el estuche de plata de la Royal Mint, no el programa de
conservación del BCV, que tuvo una tercera moneda en oro (N#59793) vendida aparte.

**Contrastar fuera de Numista, siempre.** Del resync de
[#146](https://github.com/jenarvaezg/coindex/issues/146) salieron tres tipos que el censo dio por
solos y no lo estaban, y lo dijo la ceca y no Numista: el Koala del RAM
([#152](https://github.com/jenarvaezg/coindex/issues/152)) se anunció como la tercera entrega de
un programa anual, y los dos gourdes de Haití
([#153](https://github.com/jenarvaezg/coindex/issues/153)) se vendieron en un estuche de cuatro
monedas. No es un lujo del método: es lo que evitó firmar tres veredictos falsos.

**Una huérfana es un veredicto del curador y no el residuo de la app** (ADR 0020).
`data/orphans.json` es registro editorial: no alimenta colecciones ni la pantalla, así que firmar
un veredicto **no baja** el número de «Sin clasificar» que ve el coleccionista. Y un veredicto
firmado **se puede reabrir**: el [#257](https://github.com/jenarvaezg/coindex/issues/257) sacó los
8 reales de Carlos IV del registro a `historia-del-real`, y lo reabrió lo mismo que lo había
firmado —la intención del padre— al describir un tema que la forma ofrecida en su día no
contemplaba.

**Y una huérfana puede tener tarjeta.** Las dos Disney de Niue del
[#363](https://github.com/jenarvaezg/coindex/issues/363) —N#192181 y N#484131— son las primeras
firmadas que declaran familia cruda: medido sobre la caché, las 31 anteriores tienen `series` a
nulo, así que el registro se había llenado siempre desde el residuo. El veredicto no habla de la
tarjeta sino de la lámina, y una familia cruda no tiene denominador que reclamar: la serie «Disney»
son 315 tipos de cuatro emisores y sigue creciendo. Firmarlas no quita la tarjeta ni la pretende.

**El peldaño 5 de la escalera de familias no produce ninguna tarjeta**, medido sobre las dos
colecciones tras recorrer los sistemas monetarios portugueses
([#157](https://github.com/jenarvaezg/coindex/issues/157)). Sigue en el modelo como red de
seguridad: ninguna pieza se cae por tener sólo familia técnica. De ese mismo recorrido salió que
un catálogo único de las conmemorativas de circulación portuguesas no era opción —habría chocado
con los tipos ya reclamados y se habría tragado las once series *Portuguese Discoveries*, que son
programas por derecho propio—; la **lectura temática** que pidió el coleccionista se resolvió con
los programas conmemorativos del ADR 0022
([#178](https://github.com/jenarvaezg/coindex/issues/178)).

**Las dos colecciones no se publican**: este repo es público y son dos inventarios privados. La
cifra viva se reproduce corriendo `FieldReportTest` con `COINDEX_FIELD_SNAPSHOT` sobre una captura
de `scripts/record-fixture.py --user-id`, que vive fuera del árbol; el test corre el
`deriveCollection` real, así que un listado rehecho a mano inventaría huérfanas que la app no
tiene.

Presupuesto de API: cada usuario gasta su propia key (~1.500-2.000 llamadas/mes), y el snapshot
de caché existe para que ninguno repita lo ya descargado.

### 0.8 Reglas del arranque que no son tarea cumplida

Los nueve pasos del arranque están hechos y son **historia de git, no especificación**: las
decisiones del port están en el ADR 0010 y las de distribución en el ADR 0011. Lo que sigue
siendo una regla:

- Los assets se montan desde `data/` sin copiar; los catálogos se validan al arrancar y un fallo
  **detiene la app** con el fichero y el motivo, en vez de degradarse en silencio. El error es
  tipado a propósito, porque el destinatario del mensaje es el curador (ADR 0027).
- **No** se portaron las series curadas ni las correcciones manuales, y no vuelven (ADR 0010 §2).

### 0.9 Las cuatro cuestiones abiertas de la fase Android, y cómo se cerraron

Las tres de catálogos y datos las cerró el **ADR 0020**, que es la especificación de lo que un
catálogo afirma; cómo identifica sus casillas está en §0.2. Lo que de sus respuestas sigue
vinculando:

- **Las emisiones anunciadas se expresan con `status` por miembro** —`issued` | `announced` |
  `unlisted`— y no con una versión de esquema nueva, porque el estado es propiedad de un miembro y
  compone con las cuatro formas de identificarlo. Un anunciado prohíbe `numista_type_id` y exige
  `source` + `source_note`. **`schema_version: 4` sigue libre.**
- **Se rechaza el fichero de catálogos remoto.** Los catálogos viajan dentro del APK, así que la
  frescura de un catálogo queda atada a la versión instalada, y eso es coherente con que un
  catálogo abierto no prometa estar al día a ninguna fecha. La mitigación no es arquitectura:
  `scripts/release.sh` dice en las notas de la release cuándo trae `data/` cambiado, y un paso
  informativo de CI mantiene al día el issue de
  [catálogos abiertos por detrás](https://github.com/jenarvaezg/coindex/issues/136).
- El nombre es `Coindex` y el applicationId `com.jenarvaezg.coindex`.

---
