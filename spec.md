# spec.md — Coindex

> Especificación viva de Coindex. Dirigida a un agente de codificación: léela completa
> antes de escribir código.
>
> **PIVOT (29 de julio de 2026): Coindex es una app de Android local-first.**
> Shuttle dejó de funcionar y se descartó el despliegue web. El dominio se portó a Kotlin y
> el **30 de julio de 2026 la implementación Rust se retiró del árbol de trabajo**: vive en el
> tag `rust-frozen` (`git checkout rust-frozen`) para poder consultarla. El apéndice histórico
> describe esa fase y sigue siendo la mejor documentación del dominio (§5), de la API de
> Numista (§6) y de la estética (§8); cuando discrepe con la sección 0 o con los ADR de
> `docs/adr/`, prevalecen la sección 0 y los ADR.

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

1. **`data/collection-catalogs/*.json` — 20 catálogos curados** (el activo más caro de
   reproducir). Todos los `numista_type_id` fueron verificados contra numista.com antes
   de versionarse. Se empaquetan como assets de la app. Esquema:
   - `schema_version: 1`: miembros identificados por `numista_type_id` único; posesión =
     poseer el tipo. Fuente obligatoria: página de serie de Numista
     (`catalogue/series.php?id=N`).
   - `schema_version: 2` (**date runs**, ADR 0009): los miembros repiten un mismo tipo
     con años distintos (único por `(numista_type_id, year)`); posesión = poseer el tipo
     **y** que `issue_year` (o `gregorian_year`) del item coincida con el año del
     miembro. Un item sin año nunca rellena un hueco. La fuente puede ser la ficha del
     tipo (`catalogue/piecesNNN.html`). Primer date run vivo:
     `venezuela-5-bolivares.json` (21 fechas de N#10340; al padre le faltan 1904 y 1905).
   - `schema_version: 3` (**conjuntos emitidos como set**, ADR 0012): los miembros abarcan
     varias variantes físicas, así que el catálogo no declara `weight_millioz` ni `finish`
     y su clave de variante lleva el **peso ausente** (`-1` al persistirse). Un tipo listado
     aquí deriva esa clave incluso si Numista le da familia: nombrar los tipos exactos que
     se emitieron juntos es una afirmación más específica que la agrupación de Numista.
     Criterio estrecho a propósito: **solo** conjuntos emitidos como un producto. El bullion
     fraccional no es un conjunto (¼ oz y 1 oz son la misma moneda en dos tamaños y siguen
     siendo propuestas separadas, como ya hacen Tudor Beasts y Lunar II). Primer conjunto
     vivo: `portugal-1983-exposicion-europea-de-arte.json` (500/750/1000 escudos de 1983,
     7/12,5/21 g, emitidas en un mismo estuche; el padre las tiene las tres).
2. **`data/numista-type-cache.json`** — snapshot de la caché de metadatos de 634 tipos
   (respuestas íntegras de `GET /types/{id}?lang=es`). Costó ~650 llamadas de API;
   empaquetarlo como seed de la tabla de caché para que ningún usuario las repita.
   Incluye los 470 tipos referenciados por los catálogos: las láminas muestran todos los
   diseños (incluidos los "me falta") sin gastar presupuesto.
3. **`data/series/*.json`** — **retirado**: las dos series curadas históricas (Lunar III,
   Tudor Beasts) no las leía nadie (ADR 0010 §2) y viajaban en el APK como assets muertos.
   Tudor Beasts ya tiene dos catálogos curados; el material de Lunar III (doce casillas con
   etiqueta, motivo y `release_status`, sin un solo `numista_type_id`) queda en el commit
   `9fc2582` para el catálogo que se cure. Ya no hay código que las valide en `main`.
4. **`docs/adr/0001..0012`** — las decisiones de dominio siguen vigentes; la 0007
   (propuestas desde inventario), 0008 (disposiciones durables), 0009 (date runs +
   fallback de familia) y 0012 (familias técnicas, pesos de catálogo y conjuntos) son la
   especificación del comportamiento.
5. **`crates/domain`** — la lógica a portar (~1.200 líneas, pura, sin I/O, con tests de
   tabla dorada). Portada a mano a `domain/` en Kotlin (ADR 0010); el original está en el tag
   `rust-frozen`.

### 0.3 Modelo de dominio a portar (invariantes)

- **Clave de variante física** (identidad de propuesta, de preferencia y de catálogo):
  `(familia Numista cruda, peso normalizado en mili-onzas, acabado)`.
  - Peso: `round(oz*1000)`, con imán a los pesos comunes `[250, 500, 1000, 2000, 5000,
    10000]` **y a los pesos declarados por los catálogos sembrados** (ADR 0012) si la
    diferencia es ≤10, ganando el destino más cercano y, en empate, el menor (31,1 g → 1000;
    30 g → 965, nunca 1000; 13,96 g → 450 porque un catálogo declara 450). Ausente (`-1` al
    persistirse) en los conjuntos emitidos como set, que no tienen un peso único.
  - Acabado (`Finish`): inferido del título del tipo con reglas auditables
    (`proof`+colour → ProofColoured; `proof`; colour/`coloriz`/colores lunares; `gild`/
    `dorad`; `antiqu`; `bullion` o series Lunar III / Tudor Beasts → Bullion; si no →
    desconocido). Numista no expone un campo de acabado estable.
- **Propuestas de colección**: agrupan solo piezas actuales del usuario por clave de
  variante exacta. Sin familias difusas, sin mezclar pesos ni acabados. Cantidades: tipos
  distintos + piezas.
- **Precedencia de familia** (ADR 0009 y 0012), de la afirmación más específica a la más
  débil: catálogo de conjunto (`schema_version: 3`) que nombra el tipo → familia real de
  Numista → catálogo v1/v2 que referencia el tipo → familia técnica `System YYYY[-YYYY]`.
  Una familia técnica ya no descarta la pieza: es la familia más débil, y se muestra con el
  alias de presentación «Sistema monetario YYYY-YYYY». Solo los tipos sin familia y sin
  catálogo siguen fuera (huérfanos).
- **Disposiciones**: cada propuesta está `Available`, `Followed` o `Ignored` (persistente
  por usuario y clave exacta; reversible; una preferencia sin evidencia actual queda
  dormida sin materializar propuestas).
- **Lámina de catálogo**: navegable cuando la propuesta existe, está `Followed`, hay
  catálogo para esa clave exacta y el usuario posee ≥1 `type_id` oficial del catálogo
  (la evidencia es por tipo incluso en date runs). Estados `Tengo (×n)` / `Me falta`.
- **Alias editoriales de familia** (solo presentación, nunca entran en la clave):
  `SML`→`Silver Maple Leaf`, `Red Data Book`→`Libro Rojo de Rusia`, la serie española a
  facial→`Monedas españolas de plata a valor facial`, `Lunar ounce`→`Rwanda Lunar
  Ounce`, `Nautical Ounce`→`Rwanda Nautical Ounce`.

### 0.4 UI de referencia (la web congelada es el prototipo)

Índice por usuario: botón **Sincronizar** + acceso a huérfanas/sin clasificar +
propuestas en tres bloques (Seguidas / Disponibles / Ignoradas plegadas). Sin sección de
láminas curadas (se retiró; las series curadas viven como catálogos). Cada tarjeta:
familia (alias), variante (`peso · acabado`), `n tipos · m piezas`, acciones. El título
enlaza: seguida con catálogo y evidencia → lámina local; resto con catálogo → página de
la serie en Numista. La lámina: cabecera con progreso `n / m emisiones`, fuente y
variante; rejilla con anverso/reverso, año y nº Numista por miembro, **los que faltan en
gris (grayscale + opacidad ~0.45) con su diseño visible**, y todos los miembros enlazan a
su ficha de Numista. Estética de guía de campo ornitológica (ver apéndice §8): serif,
paleta de papel, ficha de especificaciones físicas.

### 0.5 API de Numista — todo lo aprendido (válido para la app)

- Base `https://api.numista.com/v3`. Auth: cabecera `Numista-API-Key` + token OAuth
  `client_credentials` con **`scope=view_collection`** (omitirlo da un 401 engañoso;
  token ~10 min, cachearlo).
- Endpoints: `GET /users/{id}/collected_items` (sin paginación, ADR 0006),
  `GET /types/{id}?lang=es`, y `GET /types/{id}/issues` (lista de emisiones por año —
  la fuente para curar date runs; así se verificaron las 21 fechas del 5 Bolívares).
- **Disciplina de presupuesto** (portar tal cual): caché permanente de tipos (un tipo
  descargado no se vuelve a pedir), contador local de llamadas del mes con techo
  configurable, y tests sin red (fixtures en `fixtures/numista/`).
- Imágenes: URLs en la respuesta de `/types/{id}`; en la app se cargan directo con Coil
  (ya no hay proxy). Uso personal con tráfico razonable: aceptado públicamente por el
  administrador de Numista; sin scraping en runtime.

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
6. Criterio del coleccionista (el padre): bullion de diseño estable (Maple, Krugerrand,
   ASE, Britannia, Philharmonic, Noah's Ark, Kangaroo) = pieza representativa, **sin
   catálogo de fechas**; series de diseño anual cambiante sí se catalogan y se siguen
   hacia delante.

### 0.7 Estado de datos al pivotar (29 jul 2026)

- 18 catálogos sembrados. Con lámina en uso: valor facial España (24/37 del padre),
  250 aniversario EE. UU. (3/8), 5 Bolívares (19/21), Personalidades de Rusia (1/121 de
  Jose), Tudor 2 oz (2-3/9 cada uno), y el resto seguibles.
- Huérfanas restantes (sin familia ni catálogo): conmemorativas portuguesas en escudos
  (familias técnicas `System …`; candidatas a catálogos por diseño: 500 escudos
  1995-2001 al padre le faltan N#13043/N#10207/N#13046), plata venezolana circulante
  menor, Franco 100 ptas (date run candidato: estrellas 66-70), Koala RAM (N#557132, sin
  serie en Numista), y piezas sueltas (medallas, conmemorativas aisladas).
- Presupuesto API de la key de Jose consumido en julio: ~631/1500. Ya no importa tras el
  pivot (cada usuario la suya), pero el snapshot de caché evita regastarlo.

### 0.8 Orden de trabajo de la app — estado a 30 jul 2026

La app vive en `android/` (módulos `domain` puro y `app`). Ver `android/README.md` y el
ADR 0010 para las decisiones del port.

1. ✅ Esqueleto Kotlin + Compose + Room (AGP 9.3.1, Kotlin 2.4.10, compileSdk 36). Los
   assets se montan desde `../../data`, sin copiar; los catálogos se validan al arrancar y
   un fallo detiene la app con el fichero y el motivo.
2. ✅ Dominio portado con sus tablas doradas (22 tests): propuestas, clave de variante,
   catálogos v1/v2, date runs, fallback de familia, normalización de peso, acabados. **No**
   se portan series curadas ni correcciones manuales (ADR 0010 §2).
3. ✅ Onboarding con API key + user id cifrados con el Android Keystore; sync explícito a
   Room con contador mensual y techo configurable (19 tests de app, sin red).
4. ✅ Índice de propuestas en tres bloques con disposiciones persistentes.
5. ✅ Láminas v1 y v2: progreso `n / m emisiones`, ficha de especificaciones, grises al
   45 % para las que faltan y enlaces a Numista.
6. ✅ Vista de huérfanas, ahora con motivo auditable por pieza (ADR 0010 §3).
7. ✅ Exportar lámina como imagen: compone la hoja completa fuera de pantalla y la comparte
   como PNG por el share intent de Android (ADR 0010 §8).
8. ✅ Firma con keystore fuera del repo, APK firmado verificado con `apksigner` e instalado
   en el móvil de Jose.
9. ✅ Distribución y actualización resueltas sin tienda: repo público, releases con el APK y
   un `update.json`, y actualización desde la propia app (ADR 0011). `scripts/release.sh`
   publica.

### 0.9 Cuestiones abiertas de la fase Android

1. ✅ Resuelta: `Coindex`, applicationId `com.jenarvaezg.coindex`.
2. Cómo expresar en catálogos las emisiones futuras/anunciadas (el modelo v1/v2/v3 exige
   `numista_type_id`, así que no puede representar "sin emitir" como hacían las series
   curadas con `release_status`). Ojo: `schema_version: 3` ya está ocupado por los conjuntos
   emitidos como set (ADR 0012), así que esto necesita otra versión o un campo opcional.
3. ¿Migrar Lunar III bullion a catálogo v1 (12 tipos verificados) y retirar `data/series`?
4. Actualización de catálogos dentro de la app sin reinstalar (¿fichero remoto opcional?)
   — en tensión con el local-first; decidir más adelante. Nota: los catálogos viajan hoy con
   cada APK, y con la actualización desde la app (ADR 0011) llegar a los dos móviles ya no
   requiere reinstalar a mano, así que la urgencia baja.

---

# Apéndice — especificación histórica de la fase web Rust (congelada)

> Todo lo que sigue describe la implementación Rust/Axum, retirada del árbol el 30 de julio
> de 2026 y consultable en el tag `rust-frozen`. Sigue siendo la mejor documentación del
> dominio (§5), de la API de Numista (§6) y de la estética (§8). Las rutas que menciona
> (`crates/`, `backend/`, `Cargo.toml`) ya no existen en `main`. No invertir más en este
> stack; la curación de datos en `data/` sí continúa.

## 1. Contexto

Dos personas (padre e hijo) coleccionan monedas de plata bullion: series lunares de
Perth Mint, Queen's Beasts y Tudor Beasts de Royal Mint, Maple Leaf, Kookaburra, Panda,
etc. Sus colecciones ya están catalogadas en **Numista** (numista.com), que tiene un
catálogo excelente pero una interfaz pésima para dos cosas concretas:

1. **Ver qué te falta de una serie.** Numista no modela "series" como concepto. Sabe que
   tienes una moneda de Australia de 2024 de 1 dólar, pero no que es la casilla del Dragón
   de la Lunar Series III y que te faltan la Serpiente y el Caballo.
2. **Compartir la colección** de forma presentable.

Coindex resuelve esas dos cosas con una estética de **guía de campo ornitológica**: cada
serie es una lámina con sus casillas; las que tienes muestran tu ejemplar, las que faltan
muestran la lámina del catálogo, y las que aún no se han emitido se distinguen de las que
te faltan.

### El concepto central: `Slot`

La abstracción que hace que todo funcione es la **casilla** (`Slot`): una posición
concreta en el álbum de una serie ("Lunar III · 2024 · Dragón · 1oz bullion"), definida
por nosotros a mano, e independiente de cómo Numista organice su catálogo. El trabajo
del sistema es **casar** los objetos coleccionados en Numista contra nuestras casillas.

Ese emparejamiento es imperfecto por naturaleza y **debe ser auditable y corregible a
mano de forma permanente**. Esto no es un detalle: es el requisito que determina si la
app se usa o se abandona.

---

## 2. Alcance de la Fase 1

### Dentro

- Workspace de Cargo con la separación de crates especificada en §4.
- Modelo de dominio (`Series`, `Slot`, `CollectedItem`, `Album`) y motor de matching, con
  tests de tabla dorada.
- Cliente de la API de Numista: autenticación, descarga de la colección propia, descarga
  de metadatos de tipos, caché persistente.
- Dos series semilla curadas a mano en JSON: **Lunar Series III** y **Tudor Beasts**.
- Backend en Shuttle (axum + Postgres) con persistencia y endpoints de §7.
- Vista web renderizada en servidor: índice de series y lámina de cada serie con la
  rejilla de casillas.
- Correcciones manuales de emparejamiento, persistidas.
- Dos usuarios fijos, sin sistema de cuentas.

### Fuera (no implementar, pero dejar hueco)

- Todo el procesado de imagen: shape-from-shading, RTI, normal maps, 3D, WASM, wgpu.
  Va en Fase 2. Crear `crates/imaging` con un `lib.rs` vacío y un README que apunte a §12.
- Subida de fotos y vídeos propios.
- Escritura hacia Numista (`edit_collection`).
- URLs públicas compartibles y estado comprimido en el hash.
- Registro de usuarios, "trae tu propia API key", PWA, service worker, offline.
- Scraping de webs de cecas.

**No implementes nada de la lista de "fuera" aunque parezca fácil o quede a medio camino.**
La Fase 1 tiene que estar terminada y usable antes de tocar la Fase 2.

---

## 3. Decisiones ya tomadas

Estas decisiones están cerradas y razonadas. No las revises salvo que te encuentres un
bloqueo técnico duro, y en ese caso documenta el bloqueo en `docs/adr/` antes de desviarte.

| Decisión | Por qué |
|---|---|
| **Rust en todo el stack** | Preferencia del autor, y el crate de dominio se reutilizará compilado a WASM en Fase 2. |
| **Shuttle** (shuttle.dev) como PaaS | Nativo de Rust, provisiona Postgres de forma declarativa desde el código, tier gratuito suficiente para dos usuarios. |
| **Backend con servidor y BD**, no cliente puro | Se descartó el estático porque la API de Numista puede no enviar cabeceras CORS, porque la key no debe vivir en el navegador, y porque la Fase 2 necesita cabeceras COOP/COEP para hilos en WASM. |
| **HTML renderizado en servidor** con `maud` | El álbum es fundamentalmente un documento. Cero tooling de frontend, cero build de JS, plantillas con comprobación de tipos en compilación. La interactividad de la Fase 2 se añadirá como mejora progresiva sobre esto, no reemplazándolo. |
| **Postgres vía Shuttle**, con `sqlx` | Shuttle lo provisiona. `sqlx` con macros comprobadas en compilación. Migraciones en `backend/migrations/`. |
| **Las definiciones de series son ficheros JSON versionados**, no filas en la BD | Son datos curados a mano que cambian tres veces al año. Van en git, se revisan en PR, y se cargan al arrancar. La BD guarda solo lo que es por-usuario: items coleccionados, caché de tipos y correcciones. |

---

## 4. Estructura del repositorio

```
coindex/
├── Cargo.toml                  # workspace
├── spec.md                     # este fichero
├── README.md
├── .env.example
├── docs/
│   └── adr/                    # decisiones de arquitectura, una por fichero
├── crates/
│   ├── domain/                 # modelo + matching. SIN I/O, SIN deps de plataforma
│   ├── numista/                # cliente de la API
│   └── imaging/                # VACÍO en Fase 1. Ver §12
├── backend/                    # binario de Shuttle: axum + maud + sqlx
│   ├── migrations/
│   └── src/
├── data/
│   ├── series/
│   │   ├── lunar-iii.json
│   │   └── tudor-beasts.json
│   └── collection-catalogs/
│       └── nikola-tesla-serbia-1oz.json
└── fixtures/
    └── numista/                # respuestas reales grabadas, para tests
```

Reglas duras sobre `crates/domain`:

- **No depende de `tokio`, `reqwest`, `sqlx`, ni de nada del sistema de ficheros o la red.**
  Solo `serde`, `thiserror` y utilidades puras.
- Debe compilar para `wasm32-unknown-unknown` sin cambios. Añade un job de CI que lo
  compruebe (`cargo check -p domain --target wasm32-unknown-unknown`). Esto no es
  decorativo: en Fase 2 este crate corre en el navegador.

---

## 5. Modelo de dominio

En `crates/domain`. Todo `serde`-serializable.

```rust
pub struct Series {
    pub id: SeriesId,            // slug: "lunar-iii"
    pub name: String,            // "Lunar Series III"
    pub mint: String,            // "Perth Mint"
    pub issuer_code: String,     // código de emisor de Numista: "australie"
    pub metal: Metal,
    pub notes: Option<String>,
    pub slots: Vec<Slot>,
}

pub struct Slot {
    pub id: SlotId,              // "lunar-iii-2024-dragon-1oz"
    pub label: String,           // "Dragón — 2024"
    pub year: i32,
    pub motif: String,           // "Dragón"
    pub weight_oz: f32,          // 1.0, 2.0, 0.5...
    pub finish: Finish,          // Bullion | Proof | Coloured | ProofColoured | Gilded | Antiqued
    pub release_status: ReleaseStatus,   // Issued | Announced | Expected

    /// Emparejamiento primario: IDs de tipo de Numista confirmados a mano.
    /// Puede estar vacío si aún no se ha investigado. Puede tener varios
    /// (Numista a veces desdobla un mismo diseño en varios tipos).
    pub numista_type_ids: Vec<u32>,

    /// Heurísticas de respaldo cuando no hay type_ids. Ver §6.
    pub matchers: Vec<Matcher>,
}

pub enum SlotStatus {
    Owned { quantity: u32, items: Vec<ItemRef> },
    Missing,
    NotYetIssued,      // release_status != Issued y no lo tenemos
}

pub enum MatchSource {
    ManualOverride,    // el usuario lo dijo. Máxima prioridad, siempre gana.
    ExplicitTypeId,    // curado a mano en el JSON de la serie
    Heuristic { confidence: f32, explanation: String },
}
```

### Motor de matching

Función pura, sin I/O:

```rust
pub fn build_album(
    series: &[Series],
    items: &[CollectedItem],
    type_meta: &TypeMetaIndex,      // metadatos de tipos cacheados
    overrides: &[ManualOverride],
) -> Album
```

Reglas, en orden estricto de precedencia:

1. **`ManualOverride`** — si el usuario dijo que el item X va en la casilla Y, va en la
   casilla Y. Sin excepciones, sin recálculo, para siempre.
2. **`ExplicitTypeId`** — el `type.id` del item aparece en `slot.numista_type_ids`.
3. **`Matcher` heurístico** — se evalúa solo si 1 y 2 no dieron resultado.

Cada emparejamiento devuelve su `MatchSource`. La UI **debe** distinguir visualmente un
emparejamiento heurístico de uno confirmado, y ofrecer confirmarlo o corregirlo. Un
emparejamiento heurístico confirmado por el usuario se convierte en `ManualOverride`.

Los items que no encajan en ninguna casilla van a `Album::unmatched` y se muestran en una
pantalla propia, con la opción de asignarlos a una casilla a mano. **Nunca se descartan
en silencio.** Esta pantalla es el mecanismo principal por el que el catálogo de series
va mejorando con el uso.

### Tests obligatorios

Tabla dorada en `crates/domain/tests/matching.rs` que cubra, como mínimo: acierto por
type_id explícito; acierto heurístico; override que sobrescribe un acierto explícito
distinto; item sin casilla → `unmatched`; casilla con varios items (duplicados) →
`quantity > 1`; casilla futura sin item → `NotYetIssued`, no `Missing`.

---

## 6. Integración con Numista

### ⚠️ Presupuesto de llamadas — leer antes de escribir el cliente

**La API gratuita tiene un límite de ~2.000 peticiones al mes.** Es el recurso más escaso
del proyecto y es fácil de agotar en una sola sesión de depuración.

Requisitos obligatorios del cliente:

1. **Caché permanente de metadatos de tipos en Postgres.** Un `type_id` ya descargado
   **nunca** se vuelve a pedir salvo invalidación explícita por parte del usuario. Los
   datos del catálogo son esencialmente inmutables.
2. **Contador de presupuesto.** Tabla `api_call_log` con timestamp y endpoint. El cliente
   consulta el gasto del mes en curso antes de cada llamada y **devuelve error en lugar de
   llamar** si se supera `NUMISTA_MONTHLY_BUDGET` (por defecto: 1500, con margen).
3. **Los tests no tocan la red. Nunca.** Usan los ficheros de `fixtures/numista/`.
   Añade un binario `cargo run -p numista --bin record-fixtures` que sea la *única* forma
   de regrabarlos, ejecutado a mano y de forma consciente.
4. **Sync explícito y presupuesto interno.** La interfaz no ofrece un estimador ni un
   modo seco: `Sincronizar` ejecuta el sync real y vuelve al índice. El contador previo a
   cada llamada y `/health` son la fuente de verdad del consumo.

### Autenticación

Dos cabeceras son necesarias en los endpoints de colección:

- `Numista-API-Key: <API_KEY>`
- `Authorization: Bearer <ACCESS_TOKEN>`

El token se obtiene de `oauth_token` con `grant_type=client_credentials` y
**`scope=view_collection`**. Omitir el scope produce un 401 con un mensaje engañoso; es
el error más común con esta API. El token dura del orden de 10 minutos: cachéalo en
memoria con margen de expiración y renuévalo, no pidas uno por petición.

Base: `https://api.numista.com/v3`

### Endpoints necesarios en Fase 1

| Uso | Endpoint |
|---|---|
| Token | `GET /oauth_token?grant_type=client_credentials&scope=view_collection` |
| Colección propia | `GET /users/{user_id}/collected_items` |
| Metadatos de tipo | `GET /types/{type_id}?lang=es` |

De `collected_items` nos interesa por cada item: `id`, `quantity`, `type.id`, `type.title`,
`type.issuer.code`, `issue.year`, `issue.gregorian_year`, `grade`, `price`, `for_swap`,
`collection.name`.

De `/types/{id}` nos interesa: `title`, `issuer`, `min_year`/`max_year`, `weight`,
`size` (diámetro), `thickness`, `shape`, `orientation`, `composition`, `commemorated_event`,
las referencias de catálogo, y las URLs de imagen de anverso y reverso.

> **A verificar empíricamente:** no está confirmado que la API exponga la URL de la
> imagen del **canto**, aunque el catálogo web sí tiene ese campo. Comprueba la respuesta
> real de un tipo que tenga foto de canto en la web y **documenta el hallazgo en
> `docs/adr/`**. No bloquea la Fase 1; condiciona la Fase 2.

### Reglas de tipado

Los esquemas de respuesta reales manda sobre lo que dice este documento. Golpea la API una
vez por endpoint, graba la respuesta como fixture, y **deriva los structs de la respuesta
real**. Si algo aquí contradice lo que devuelve la API, gana la API; anótalo en el ADR.

Trata todos los campos como opcionales salvo los que compruebes que siempre vienen. Este
catálogo lo rellenan voluntarios y está lleno de huecos.

*(Nota para más adelante, irrelevante en Fase 1: al escribir items vía `POST`, el campo
`type` debe enviarse como cadena JSON `"44"`, no como número `44`, pese a que el esquema
declara un entero. Un número produce un 400 sin pista.)*

### Imágenes

Cachea las URLs, no los bytes, en Fase 1. Sirve las imágenes proxeadas por nuestro backend
en `/img/type/{type_id}/{side}` en lugar de enlazarlas directamente desde el HTML: nos
permite cachear después, y evita que la Fase 2 se rompa al activar COEP.

Los términos de Numista restringen la extracción y reproducción sistemáticas, si bien su
administrador ha manifestado públicamente que el uso personal con tráfico razonable no es
problemático. Nos mantenemos en ese terreno: uso personal, caché conservadora, y en Fase 2
las piezas propias se fotografían nosotros.

---

## 7. Backend

Binario de Shuttle en `backend/`. `axum` para rutas, `maud` para HTML, `sqlx` para
Postgres, `tracing` para logs.

### Usuarios en Fase 1

Sin cuentas, sin login, sin almacenar API keys de terceros. Dos usuarios fijos definidos
en los Secrets de Shuttle:

```
COINDEX_USERS = "jose:<numista_user_id>:<api_key>,padre:<numista_user_id>:<api_key>"
NUMISTA_MONTHLY_BUDGET = "1500"
```

Se parsean al arrancar a una estructura tipada. **Las API keys no se persisten en Postgres
en Fase 1.** Viven en memoria, procedentes de los Secrets. Esto elimina de golpe todo un
frente de seguridad que no necesitamos todavía.

### Esquema de base de datos

Migraciones en `backend/migrations/`, con `sqlx::migrate!`.

```sql
-- items coleccionados, snapshot del último sync
collected_items(
  id BIGINT PRIMARY KEY,          -- id de item de Numista
  user_key TEXT NOT NULL,         -- "jose" | "padre"
  type_id INTEGER NOT NULL,
  quantity INTEGER NOT NULL,
  issue_year INTEGER,
  grade TEXT,
  collection_name TEXT,
  raw JSONB NOT NULL,             -- respuesta completa, por si añadimos campos
  synced_at TIMESTAMPTZ NOT NULL
)

-- caché permanente del catálogo. Compartida entre usuarios.
type_meta(
  type_id INTEGER PRIMARY KEY,
  raw JSONB NOT NULL,
  fetched_at TIMESTAMPTZ NOT NULL
)

-- correcciones manuales. Sagradas: nunca se borran automáticamente.
manual_overrides(
  user_key TEXT NOT NULL,
  item_id BIGINT NOT NULL,
  slot_id TEXT NOT NULL,          -- o NULL = "este item no va en ninguna casilla"
  created_at TIMESTAMPTZ NOT NULL,
  PRIMARY KEY (user_key, item_id)
)

api_call_log(
  id BIGSERIAL PRIMARY KEY,
  endpoint TEXT NOT NULL,
  called_at TIMESTAMPTZ NOT NULL
)

-- intención persistente sobre propuestas derivadas; la ausencia significa Available
collection_proposal_preferences(
  user_key TEXT NOT NULL,
  family TEXT NOT NULL,
  weight_millioz INTEGER NOT NULL,
  finish TEXT NOT NULL,
  disposition TEXT NOT NULL,     -- followed | ignored
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  PRIMARY KEY (user_key, family, weight_millioz, finish)
)
```

Guardar `raw JSONB` es deliberado: nos deja añadir campos al modelo más adelante sin
volver a gastar presupuesto de API.

### Rutas

| Método | Ruta | Función |
|---|---|---|
| `GET` | `/` | Índice: una tarjeta por serie y por usuario, con progreso `n/total` |
| `GET` | `/u/{user}/series/{series_id}` | La lámina: rejilla de casillas |
| `GET` | `/u/{user}/followed-collections/{catalog_id}` | Lámina de una propuesta seguida con catálogo curado |
| `GET` | `/u/{user}/unmatched` | Items sin casilla, con formulario de asignación |
| `POST` | `/u/{user}/override` | Crea o actualiza una corrección manual |
| `POST` | `/u/{user}/collection-proposal-preference` | Sigue, ignora o restaura una propuesta exacta |
| `POST` | `/u/{user}/sync` | Ejecuta el sync real y redirige al índice |
| `GET` | `/img/type/{type_id}/{side}` | Proxy de imagen |
| `GET` | `/api/album/{user}` | El `Album` en JSON. Existe para que la Fase 2 lo consuma |
| `GET` | `/health` | Estado, incluido el presupuesto de API consumido este mes |

El sync es síncrono y explícito, disparado por un único botón. No hay estimador público,
página de costes ni cron en Fase 1.

---

## 8. Frontend (Fase 1)

Renderizado en servidor con `maud`. Un único fichero CSS escrito a mano en
`backend/static/`. Sin frameworks, sin bundler, sin npm.

### La lámina

Rejilla de casillas. Cada casilla muestra: la imagen (la del catálogo en Fase 1), la
etiqueta, el año, y un estado visualmente inequívoco:

- **Tengo** — imagen a color completo, borde definido, cantidad si es > 1.
- **Me falta** — imagen desaturada al 15% de opacidad, o silueta. Debe leerse como hueco.
- **Sin emitir** — tratamiento distinto de "me falta": marco punteado y sin imagen. No es
  una carencia, es futuro.
- **Emparejamiento heurístico** — indicador discreto (un signo de interrogación en la
  esquina) que enlaza a confirmar o corregir.

Cabecera de serie con el progreso, contando solo casillas emitidas:
`8 / 8 emitidas · 4 por emitir`.

### Estética

Guía de campo ornitológica, no dashboard. Serif para los textos, tipografía condensada
para los datos, paleta apagada de papel, especificaciones físicas presentadas como una
ficha de campo junto a la lámina. Este es el eje de identidad del producto y merece
cuidado desde el principio, aunque el MVP sea funcionalmente escueto.

---

## 9. Datos semilla

`data/series/*.json`, cargados y validados al arrancar. Un fallo de validación debe
impedir el arranque con un mensaje claro, no degradarse en silencio.

`numista_type_ids` puede quedar vacío en el semillado inicial: el flujo de "items sin
casilla" es precisamente la herramienta para irlos rellenando. **No inventes IDs de tipo.**
Un ID inventado produce emparejamientos falsos que son peores que no tener ninguno.

### `lunar-iii.json` — Perth Mint, Lunar Series III

Doce casillas de 1oz de plata bullion, de 2019 a 2030:

| Año | Motivo |
|---|---|
| 2019 | Cerdo |
| 2020 | Ratón |
| 2021 | Buey |
| 2022 | Tigre |
| 2023 | Conejo |
| 2024 | Dragón |
| 2025 | Serpiente |
| 2026 | Caballo |
| 2027 | Cabra |
| 2028 | Mono |
| 2029 | Gallo |
| 2030 | Perro |

Perth Mint usó "Ratón" (Mouse) y no "Rata" para 2020. `release_status`: `Issued` hasta
2026 inclusive, `Expected` de 2027 en adelante.

### `tudor-beasts.json` — Royal Mint, Tudor Beasts

Serie de 2oz de plata, iniciada en 2022 con la Pantera de Seymour. **Los años y el orden
exacto de las emisiones posteriores no están confirmados en este documento.** Semilla
únicamente las bestias que puedas verificar contra la web de The Royal Mint o la ficha
correspondiente en Numista, y deja el resto fuera del fichero. Es preferible una serie
incompleta y correcta que una completa e inventada. Anota en el JSON un campo
`"incomplete": true` mientras falten casillas.

---

## 10. Criterios de aceptación

La Fase 1 está terminada cuando:

1. `cargo test --workspace` pasa, incluidos los tests de tabla dorada del matching, y
   **sin ninguna petición de red**.
2. `cargo check -p domain --target wasm32-unknown-unknown` pasa.
3. `cargo shuttle deploy` (o el equivalente de la CLI actual) despliega y `/health`
   responde con el presupuesto de API consumido.
4. Un `POST /u/jose/sync` con credenciales reales descarga la colección, cachea los tipos
   nuevos, y registra las llamadas en `api_call_log`.
5. `/u/jose/series/lunar-iii` renderiza las doce casillas con el estado correcto en cada
   una, distinguiendo las tres situaciones (tengo / falta / sin emitir).
6. Un item de `unmatched` puede asignarse a una casilla desde la web, y esa asignación
   sobrevive a un nuevo sync.
7. Un segundo sync consecutivo gasta **cero** llamadas de metadatos de tipos, porque todo
   está cacheado. Compruébalo mirando `api_call_log`.
8. `README.md` explica cómo obtener una API key de Numista, cómo configurar los Secrets,
   y cómo correr en local.

El criterio 7 es el que demuestra que la disciplina de presupuesto funciona. No lo des por
bueno sin verificarlo.

---

## 11. Orden de trabajo sugerido

1. Workspace, CI, `domain` con el modelo y los tests de matching contra datos inventados a
   mano. **Todavía sin tocar la red.**
2. Cliente de `numista`: autenticación y un `record-fixtures` que gaste unas pocas llamadas
   deliberadamente. Deriva los structs de las respuestas reales.
3. Los dos JSON de series, con carga y validación.
4. Backend: migraciones, sync, presupuesto, y `/api/album/{user}` en JSON.
5. Las vistas HTML.
6. Correcciones manuales y la pantalla de `unmatched`.
7. Pulido de la estética.

Después de cada paso, para y verifica contra los criterios de §10 en lugar de encadenar
pasos.

---

## 12. Fase 2 — no implementar, solo no cerrarle la puerta

El objetivo a medio plazo es el relieve y la reiluminación interactiva de las monedas.
Dos caminos que convergen en el mismo shader:

- **Para las que no tenemos:** shape-from-shading sobre el JPEG del catálogo. Es
  inusualmente bien condicionado en monedas (albedo uniforme, plano base conocido, silueta
  circular conocida, relieve de un solo signo y acotado por el grosor real que da la API).
  Luminancia → gradientes → integración de Poisson o Frankot-Chellappa → campo de altura
  → normal map.
- **Para las nuestras:** RTI (Reflectance Transformation Imaging), que es el estándar
  profesional en numismática. Cámara fija, luz en movimiento, una canica negra brillante
  en el encuadre para recuperar la dirección de la luz en cada fotograma, ~40 tomas
  (o vídeo, del que se extraen cientos). El ajuste por píxel de los coeficientes a través
  de los fotogramas es el trabajo que justifica Rust y WASM de verdad.

Lo único que la Fase 1 debe hacer al respecto:

- Que `crates/domain` compile a `wasm32` (criterio de aceptación 2).
- Que exista `crates/imaging` vacío, con un README apuntando a esta sección.
- Que las imágenes se sirvan proxeadas y no enlazadas, porque activar
  `Cross-Origin-Embedder-Policy` para habilitar hilos en WASM romperá cualquier imagen
  cross-origin que no envíe CORP — y las de Numista no lo hacen.
- Que `/api/album/{user}` devuelva JSON, para que un cliente WASM pueda consumirlo sin
  reescribir el backend.

---

## 13. Cuestiones abiertas

Investígalas cuando toque, documenta la conclusión en `docs/adr/`, y no bloquees la Fase 1
por ninguna:

1. ¿Expone la API la URL de la imagen del canto? (§6)
2. ¿Cuál es exactamente el límite mensual de la API en la cuenta que se está usando? El
   valor de 2.000 procede de fuentes de terceros; confirma contra la página de API de la
   propia cuenta y ajusta `NUMISTA_MONTHLY_BUDGET`.
3. ¿Devuelve `collected_items` paginación? Si sí, respétala y cuenta cada página contra el
   presupuesto.
4. ¿Cómo modela Numista las variantes de acabado (proof, coloreada, antigua) de un mismo
   diseño? ¿Tipos distintos o el mismo tipo? Esto determina si `Finish` puede resolverse
   por `type_id` o necesita mirar el `issue`.
5. Nombre definitivo del proyecto.

---

## 14. Notas para el agente

- **Pregunta antes de ampliar el alcance.** Si algo de §2 "fuera" parece necesario,
  plantéalo en lugar de implementarlo.
- **No hagas peticiones a la API de Numista de forma exploratoria.** Cada llamada sale del
  presupuesto mensual del autor. Cuando necesites ver una respuesta, usa `record-fixtures`
  y consúltala desde el fixture a partir de entonces.
- Cuando este documento y la realidad discrepen, gana la realidad, y el desacuerdo se
  anota en `docs/adr/`.
- Commits pequeños, en español o inglés indistintamente pero de forma consistente.
