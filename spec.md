# spec.md — Coindex

> Especificación viva de Coindex. **Los ADR de `docs/adr/` son la autoridad**: si discrepan con
> este fichero, mandan ellos. Aquí vive lo que no es una decisión de arquitectura —la forma del
> producto, la API de Numista y lo aprendido curando— más el mapa de qué ADR decide cada cosa.
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
> **Pivot (29 de julio de 2026): Coindex es una app de Android local-first.** La implementación
> Rust vive en el tag `rust-frozen` con la especificación de aquella fase, cuyo modelo —`Slot`,
> emparejamiento heurístico, `ManualOverride`— no se portó y no vuelve (ADR 0010 §2, ADR 0021 §12).

---

## 0. Fase Android — especificación de arranque

### 0.1 Decisión y forma del producto

- **App de Android** instalada por **APK directo**, para dos usuarios reales: Jose y su padre. Sin
  Play Store, sin cuenta de desarrollador, sin backend propio.
- **Local-first**: el usuario introduce su **API key de Numista** y su user id, y todo funciona
  contra la API de Numista y una base **SQLite local**. Cada usuario gasta su propio presupuesto
  (~1.500-2.000 llamadas/mes por key); no hay ledger compartido.
- **Stack**: Kotlin + Jetpack Compose, Room, Ktor y Coil; la API key se cifra con el Android
  Keystore (ADR 0010 §6).
- **Fuera de alcance**: enlaces o URLs públicas, cuentas, sincronización entre dispositivos y
  escritura hacia Numista. **Dentro**: exportar láminas en PNG y el cuaderno en PDF.
- Firma con un keystore que se **conserva** siempre, y distribución por releases públicas de GitHub
  con actualización desde la app (ADR 0011).

### 0.2 Activos del repo que la app reutiliza tal cual

1. **`data/collection-catalogs/*.json` — los catálogos curados**, el activo más caro de reproducir.
   Cada `numista_type_id` se verifica contra numista.com antes de versionarse. **Qué afirma un
   catálogo** —`series_status`, `status` por miembro, fuente, denominador, cara que imprime— **lo
   especifica el ADR 0020**; aquí sólo cómo identifica sus casillas cada `schema_version`:
   - `1`: un `numista_type_id` único por miembro; poseer el tipo es poseer el miembro. Fuente: la
     página de serie de Numista (`catalogue/series.php?id=N`).
   - `2`, **date run** (ADR 0009): un tipo repetido con años distintos (único por
     `(numista_type_id, year)`); además del tipo, el año del item (`issue_year`, o
     `gregorian_year`) debe coincidir, y un item sin año nunca rellena un hueco. La fuente puede ser
     la ficha del tipo (`catalogue/piecesNNN.html`).
   - `3`, **conjunto emitido como set** (ADR 0012): sin `weight_millioz`, `finish` ni `metal`; su
     clave lleva el **peso ausente** (`-1`) y gana a la familia de Numista. Sólo productos emitidos
     juntos: ¼ oz y 1 oz de la misma moneda no son un set.
   - `5`, **issue run** (ADR 0014): emisiones de un tipo que comparten año, identificadas por
     `numista_issue_ids`. Un catálogo `1` o `2` puede **cualificar** una casilla con ese campo
     (ADR 0019).
   - `4` **sigue libre** (§0.9).
2. **`data/numista-type-cache.json`** — snapshot de la caché de tipos (`GET /types/{id}?lang=es`
   íntegro) que siembra la tabla de caché, para que nadie gaste esas llamadas. Cubre **todos** los
   tipos que nombran los ficheros curados, así que las láminas muestran también los diseños que
   faltan. Sembrar es parte de curar: `scripts/seed-type-cache.py` omite lo cacheado y dice el coste
   con `--dry-run`, y `TypeCacheSeedTest` se pone rojo con los que falten.
3. **`data/series/*.json`** — **retirado** (ADR 0010 §2). El material de Lunar III (doce casillas
   con etiqueta, motivo y `release_status`, sin `numista_type_id`) queda en el commit `9fc2582`.
4. **`docs/adr/`** — las decisiones de dominio son la especificación del comportamiento; su índice
   es el propio directorio.

### 0.3 Modelo de dominio (invariantes)

- **Clave de variante física**: `(familia resuelta, peso normalizado en mili-onzas, acabado, metal
  dominante)` (ADR 0018). Agrupa las piezas de una colección derivada; donde un fichero curado
  nombra los tipos, la identidad es el fichero (ADR 0021).
  - Peso: `round(oz*1000)`, con imán **sólo** a los pesos comunes `[250, 500, 1000, 2000, 5000,
    10000]` si la diferencia es ≤10 (31,1 g → 1000; 30 g → 965). El peso que declara un catálogo
    manda sobre **sus** miembros y no imanta a nadie más (ADR 0016, #288). Peso ausente (`-1`) en
    los sets.
  - Acabado (`Finish`): Numista no tiene campo, así que se infiere del título con reglas auditables
    (`proof`+colour → ProofColoured; `proof`; colour/`coloriz`/colores lunares; `gild`/`dorad`;
    `antiqu`; `bullion` o series Lunar III / Tudor Beasts → Bullion; si no, desconocido). El dorado
    se lee antes en `composition.text` (#573), porque libras doradas y no doradas se titulan igual
    «Silver Proof»; una moneda **de** oro no cuenta, porque la cabeza de la composición es el metal.
  - Metal dominante: se infiere de `composition.text`, y un catálogo que no es set lo declara
    (ADR 0018).
- **Colecciones derivadas**: sólo piezas actuales del usuario, agrupadas por clave exacta, sin
  familias difusas. Cuentan tipos distintos y piezas. Desde el ADR 0021 §8 no se llaman «propuestas».
- **Precedencia de familia** (ADR 0009, 0012, 0013), de más específica a más débil: catálogo de
  conjunto que nombra el tipo → catálogo de colección seleccionado para el tipo y la pieza → familia
  real de Numista → agrupación curada → familia técnica `System YYYY[-YYYY]`, que se muestra
  «Sistema monetario YYYY-YYYY». No es dato de pantalla (ADR 0021 §3), y ninguna pieza se pierde por
  no tener familia: vive en Monedas, con el filtro «Sin colección» (ADR 0021 §1).
- **Disposiciones**: retiradas (ADR 0021 §7, que supera al 0008); `collection_proposal_preferences`
  cae en la migración v5 y no queda **nada persistido por tarjeta**.
- **Lámina de catálogo**: se abre cuando la colección existe hoy, tiene catálogo y el usuario posee
  ≥1 `type_id` oficial (evidencia **por tipo**, también en date runs). Tres razones de
  indisponibilidad y ninguna más: `UnknownCatalog`, `NotACollection`, `NoEvidence`. Las láminas de
  «Explorar» se abren sin evidencia (ADR 0030).
- **Nombre de tarjeta**: `short_name` del fichero curado (obligatorio, único, prefijo de `name`) o,
  sin fichero, la familia cruda de Numista. Los seis alias editoriales están retirados (ADR 0021 §4,
  #166); en código sólo quedan correcciones de cadenas generadas: la familia técnica, los códigos de
  emisor que no son un país (`russie` → «Rusia», ADR 0023) y la tabla de familias curadas (ADR
  0031). Una caja propia lleva un solo nombre de 40 caracteres como máximo.
- **Orden del índice**: un único comparador `(tiene ratio ↓, ratio ↓, denominador ↓,
  short_name ↑)` (ADR 0021 §6).

### 0.4 Identidad y forma

La **arquitectura de información** la decide el
**[ADR 0021](docs/adr/0021-what-a-collection-is-and-the-top-level.md)** y la **forma** —qué se ve,
qué se mueve y cuánto texto cabe— el
**[ADR 0026](docs/adr/0026-the-shape-of-coindex-an-album-sheet.md)**. No se resumen aquí. Lo que sí
vive aquí es el **eje de identidad**, que los ADR y `docs/ux/` citan como veto:

- **Guía de campo ornitológica, no cuadro de mandos.** Serif para los textos, condensada para los
  datos, paleta apagada de papel. Es el eje de identidad del producto y **no se reabre**.
- **Coindex es una hoja de álbum, no un listado** (ADR 0026 §1): una colección es un hueco
  troquelado con su moneda dentro, y la lámina es la misma hoja por años, con el diseño en
  fantasma donde falta la pieza. Papel de fibra fina, sin sombra de hoja; el único brillo fijo es
  el reflejo del acetato.
- **Papel a cualquier hora** (ADR 0026 §2): sin tema oscuro ni interruptor, y
  `android:forceDarkAllowed` a `false` en `Theme.Coindex`; de noche atenúa el sistema.
  `SinglePaletteTest` lo fija y guarda los suelos de contraste sobre el papel: `muted` ≥ 4,50
  (texto) y `hairline` ≥ 3,00 (no textual).
- **Dos tipografías dentro del APK**: **Bitter + Barlow Condensed**, con versalitas y cifras
  tabulares, sin itálica y sin subsetear. `←`, `✓` y `↗` son iconos vectoriales porque ninguna las
  trae.
- **La app no es superficie de auditoría** (ADR 0021 §12): ni línea de razón en la ficha ni gesto
  de «esta no va aquí»; el desacuerdo se informa fuera, en un script que nunca se pone rojo. El
  dinero es la excepción declarada del ADR 0026 §10.
- **No hay fotos propias de las piezas** ([#15](https://github.com/jenarvaezg/coindex/issues/15)):
  las fotos son las de Numista, y quedan fuera el relieve y la reiluminación interactiva.

| qué | dónde se decide |
| --- | --- |
| Las tres jerarquías del primer nivel, su grano y su recuento | ADR 0021 §1, ADR 0026 §8 |
| Qué es una colección, y que ninguna pieza se pierde por no tener familia | ADR 0021 §1 |
| Que no queda nada persistido por tarjeta | ADR 0021 §7 |
| El orden del índice, los filtros y el estante | ADR 0021 §6, ADR 0026 §8 |
| La tarjeta: un hueco, una foto y una fracción, sin eyebrow ni línea de variante | ADR 0026 §12 |
| La lámina, el giro, la chapa del año y el sello de «completa» | ADR 0026 §1 y §3; ADR 0020 (`printed_side`) |
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
  `client_credentials` con **`scope=view_collection`**; sin el scope responde un 401 engañoso. El
  token dura unos 10 minutos: se cachea en memoria con margen, no se pide uno por petición.
- Endpoints: `GET /users/{id}/collected_items` (sin paginación, ADR 0006),
  `GET /types/{id}?lang=es` y `GET /types/{id}/issues` (emisiones por año, la fuente para curar
  date runs, ADR 0014).
- De `collected_items` interesa por item: `id`, `quantity`, `type.id`, `type.title`,
  `type.issuer.code`, `issue.year`, `issue.gregorian_year`, `grade`, `price`, `for_swap`,
  `collection.name`. De `/types/{id}`: `title`, `issuer`, `min_year`/`max_year`, `weight`,
  `size` (diámetro), `thickness`, `shape`, `orientation`, `composition`,
  `commemorated_event`, las referencias de catálogo y las URLs de imagen.
- **El esquema real manda sobre este documento**: cada endpoint se graba una vez como fixture y los
  tipos se derivan de la respuesta real. Todo campo es opcional salvo que se compruebe lo contrario:
  el catálogo lo rellenan voluntarios.
- **Presupuesto**: caché permanente de tipos (un sync no vuelve a pedir un tipo), contador local del
  mes con techo configurable y tests sin red (`fixtures/numista/`).
- Imágenes: las URLs de `/types/{id}`, cargadas directamente con Coil. Los términos de Numista
  restringen la extracción sistemática (su administrador ha dicho en público que el uso personal
  con tráfico razonable no es problema): nada de scraping en runtime.
- *(Si algún día se escribe hacia Numista: en el `POST` de items, `type` va como cadena JSON `"44"`
  aunque el esquema declare un entero; un número da un 400 sin pista.)*

### 0.6 Curación de catálogos (pipeline de desarrollo, no de la app)

Los catálogos se generan en el repo y viajan con cada versión de la app. Reglas:

1. La ficha de cualquier tipo poseído enlaza su serie (`series.php?id=N`); el listado completo es
   `catalogue/index.php?se=N&nb=50&p=X`. Cloudflare da 403 a curl en p≥2; un navegador real
   (Playwright) con `fetch` in-page funciona.
2. **Verificar cada type_id contra Numista antes de versionarlo**: los listados de terceros traen
   años intercambiados o cuproníquel que parece plata. `/types/{id}` es la forma barata.
3. **Un catálogo = una variante física**: filtrar por denominación y acabado. Si el diseño de un
   año sólo existe en otro acabado del mismo peso y metal, se incluye (caso cocodrilo 2015).
4. Series anuales: **una entrada por año**, sin privies, coloured, gilded, proof, high relief ni
   sets. Numista mezcla Perth Mint y Royal Australian Mint en una serie (koala, lunar): separar por
   el campo `mints`.
5. **El bullion de diseño estable sí se cataloga**, como date run con una casilla por año
   ([#57](https://github.com/jenarvaezg/coindex/issues/57), que revierte la regla de julio): Maple,
   Krugerrand, ASE, Britannia, Philharmonic, Noah's Ark, Kangaroo. El límite es «una moneda al año
   que tendría sentido comprar», sin moneda de circulación ni reacuñaciones con fecha congelada.
   Hasta dónde va una colección lo dice su **ratio de cobertura** (ADR 0020, ADR 0021 §7).
6. Lo que parte una lámina es la **variante física**, no el diseño: dos monedas del mismo año, peso,
   acabado y metal son dos casillas de una lámina; un acabado distinto es otra lámina.

### 0.7 Lecciones de curar (lo que no se vuelve a aprender)

**Aquí no hay censo.** Cuántos catálogos, agrupaciones, programas, fichas o veredictos hay se mide
sobre `data/` y cambia cada semana: cualquier cifra escrita aquí nace caducada.

**Una ausencia puede ser la señal.** El estuche venezolano de 1975 (`schema_version: 3`) tiene
piezas de 28,28 g y 35 g que nunca comparten clave, así que ninguna colección derivada podía
sugerirlo. La lámina afirma el estuche de plata de la Royal Mint, no el programa del BCV, que tuvo
una moneda de oro vendida aparte (N#59793).

**Contrastar fuera de Numista, siempre.** En el resync del
[#146](https://github.com/jenarvaezg/coindex/issues/146), la ceca y no Numista dijo que tres tipos
no estaban solos: el Koala del RAM ([#152](https://github.com/jenarvaezg/coindex/issues/152)) era la
tercera entrega de un programa anual, y los dos gourdes de Haití
([#153](https://github.com/jenarvaezg/coindex/issues/153)) iban en un estuche de cuatro. Evitó firmar
tres veredictos falsos.

**Una huérfana es un veredicto del curador, no el residuo de la app** (ADR 0020).
`data/orphans.json` no alimenta colecciones ni pantalla: firmar no reduce las piezas «Sin
colección». Un veredicto **se puede reabrir**: el
[#257](https://github.com/jenarvaezg/coindex/issues/257) sacó los 8 reales de Carlos IV a
`historia-del-real` por la misma razón que lo firmó, la intención del padre.

**Una huérfana puede tener tarjeta.** Las Disney de Niue del
[#363](https://github.com/jenarvaezg/coindex/issues/363) (N#192181, N#484131) tienen familia cruda.
El veredicto habla de la lámina, y una familia cruda que abarca cientos de tipos de varios emisores
no tiene denominador; firmarlas no quita la tarjeta.

**El peldaño 5 de la escalera de familias no produce ninguna tarjeta** en las dos colecciones desde
el recorrido de los sistemas monetarios portugueses
([#157](https://github.com/jenarvaezg/coindex/issues/157)); se queda como red de seguridad. Un
catálogo único de conmemorativas portuguesas habría chocado con tipos reclamados y absorbido las
once series *Portuguese Discoveries*; la lectura temática se resolvió con los programas del ADR 0022
([#178](https://github.com/jenarvaezg/coindex/issues/178)).

**Las dos colecciones no se publican**: el repo es público. La cifra viva sale de `FieldReportTest`
con `COINDEX_FIELD_SNAPSHOT` sobre una captura de `scripts/record-fixture.py --user-id` guardada
fuera del árbol; corre el `deriveCollection` real, y un listado hecho a mano inventaría huérfanas.

### 0.8 Reglas del arranque que no son tarea cumplida

Los pasos del arranque son historia de git; las decisiones del port están en el ADR 0010 y las de
distribución en el ADR 0011. Siguen siendo reglas:

- Los assets se montan desde `data/` sin copiar; un catálogo inválido **detiene la app** con el
  fichero y el motivo. El error es tipado porque su destinatario es el curador (ADR 0027).
- **No** se portaron las series curadas ni las correcciones manuales, y no vuelven (ADR 0010 §2).

### 0.9 Las cuatro cuestiones abiertas de la fase Android, y cómo se cerraron

Las tres de catálogos y datos las cerró el **ADR 0020**. Lo que sigue vinculando:

- **Las emisiones anunciadas se expresan con `status` por miembro** (`issued` | `announced` |
  `unlisted`), no con un esquema nuevo, porque el estado es de cada miembro y compone con las
  cuatro formas de identificarlo. Un anunciado prohíbe `numista_type_id` y exige `source` +
  `source_note`. **`schema_version: 4` sigue libre.**
- **Se rechaza el fichero de catálogos remoto.** Los catálogos viajan en el APK y su frescura va
  atada a la versión, coherente con que un catálogo abierto no prometa estar al día.
  `scripts/release.sh` avisa en las notas cuando la release trae `data/` cambiado, y un paso de CI
  mantiene el issue de [catálogos abiertos por detrás](https://github.com/jenarvaezg/coindex/issues/136).
- El nombre es `Coindex` y el applicationId `com.jenarvaezg.coindex`.

---
