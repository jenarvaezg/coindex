# spec.md — Coindex

> Especificación para montar el repositorio y la **Fase 1 (MVP)** de Coindex.
> Dirigida a un agente de codificación. Léela completa antes de escribir código.
> El nombre `coindex` es provisional; si se cambia, cambiarlo en todos los `Cargo.toml`.

---

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
│   └── series/
│       ├── lunar-iii.json
│       └── tudor-beasts.json
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
    pub finish: Finish,          // Bullion | Proof | Coloured | Gilded | Antiqued
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
4. **Modo `--dry-run` en el sync**, que informa de cuántas llamadas gastaría sin hacerlas.

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
```

Guardar `raw JSONB` es deliberado: nos deja añadir campos al modelo más adelante sin
volver a gastar presupuesto de API.

### Rutas

| Método | Ruta | Función |
|---|---|---|
| `GET` | `/` | Índice: una tarjeta por serie y por usuario, con progreso `n/total` |
| `GET` | `/u/{user}/series/{series_id}` | La lámina: rejilla de casillas |
| `GET` | `/u/{user}/unmatched` | Items sin casilla, con formulario de asignación |
| `POST` | `/u/{user}/override` | Crea o actualiza una corrección manual |
| `POST` | `/u/{user}/sync` | Dispara el sync contra Numista |
| `GET` | `/img/type/{type_id}/{side}` | Proxy de imagen |
| `GET` | `/api/album/{user}` | El `Album` en JSON. Existe para que la Fase 2 lo consuma |
| `GET` | `/health` | Estado, incluido el presupuesto de API consumido este mes |

El sync es síncrono y explícito, disparado por un botón. Nada de cron en Fase 1.

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
