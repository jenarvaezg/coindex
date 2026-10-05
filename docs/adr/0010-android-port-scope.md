# ADR 0010 — Alcance del port a Android y decisiones de la app local-first

Fecha: 2026-07-30
Estado: aceptado; enmendado por el [ADR 0021](0021-what-a-collection-is-and-the-top-level.md) (§2, §3, §8)

## Contexto

Tras el pivot del 29 de julio de 2026 (spec §0), Coindex pasa a ser una app de Android local-first
y la implementación Rust queda congelada. Estas son las decisiones que tomó el port de
`crates/domain` a Kotlin y que la spec no cerraba.

## Decisiones

### 1. Identidad de la app

`Coindex`, `applicationId` `com.jenarvaezg.coindex` (spec §0.9 cuestión 1). `minSdk` 29,
`compileSdk`/`targetSdk` 36. Por ese techo, `androidx.core` queda en 1.18.0 y `lifecycle` en
2.10.0: las versiones siguientes exigen compilar contra la API 37.

### 2. Las series curadas y las correcciones manuales no se portan

No se portan `Series`, `Slot`, `Matcher`, `build_album` ni `ManualOverride`, la maquinaria de
emparejar piezas contra casillas definidas a mano: la UI ya había retirado las láminas curadas del
índice (spec §0.4) y los catálogos v1/v2 cubren su papel. `data/series/*.json` no viaja en la app
(después se retiró también del árbol; queda en el commit `9fc2582`).

**Corrección (ADR 0021 §12).** El motivo original, que la derivación no usa heurísticas, era falso:
las hay de peso, acabado y metal. `ManualOverride` sigue fuera porque corregir en un móvil arregla
sólo ése y curar el catálogo arregla los dos, porque competiría con el catálogo como autoridad sobre
la variante (ADR 0016) y porque necesitaría un destino (ADR 0021 §10). El desacuerdo entre ficha y
catálogo se audita fuera de la app (#158).

### 3. Las huérfanas se redefinen y ganan un motivo auditable

Sin series curadas, «sin clasificar» pasa a ser **toda pieza que no produjo propuesta**, con un
motivo explícito (`UnclassifiedReason`): `MissingTypeMetadata` (ficha sin descargar),
`TechnicalFamily` (familia `System YYYY[-YYYY]`), `NoFamilyOrCatalog` (sin familia ni catálogo) o
`UnknownWeight` (sin peso, no hay variante física). Nada se descarta en silencio.

Esa lista es la de entonces; la vigente está en `CollectionDerivation.kt` (el ADR 0012 retiró
`TechnicalFamily`). Y lo que este § llama huérfana es hoy el residuo sin clasificar: la huérfana es
el veredicto del curador (ADR 0020).

**Corrección (ADR 0021 §12).** El motivo por pieza vive en el informe de campo, no en pantalla. La
app enseña **qué** piezas están fuera —el filtro «Sin colección» de Monedas, donde ninguna se pierde
(ADR 0021 §1)— y no por qué.

### 4. El acabado se infiere al leer, no se almacena

`type_meta` guarda `title` y `family`, y `Finish` se infiere en cada lectura con las reglas del
ADR 0005: mejorar la inferencia corrige tipos ya cacheados sin gastar presupuesto.

### 5. El sync guarda el inventario antes de las fichas de tipo

La versión Rust guardaba el inventario después de las fichas, y un fallo a mitad lo dejaba viejo.
La app lo guarda primero: si el presupuesto se acaba a mitad, el inventario queda fresco y las piezas
sin ficha quedan como `MissingTypeMetadata` hasta el siguiente sync. Una respuesta sin `items`, o
con `items` vacío, **nunca** borra el snapshot anterior.

### 6. Presupuesto por dispositivo, no ledger compartido

Cada usuario introduce su propia API key, cifrada con una clave AES/GCM del Android Keystore (sólo
el criptograma llega a `SharedPreferences`). El contador mensual vive en `api_call_log`, con techo
configurable (1500 por defecto), y un mutex serializa contar-y-registrar, como el
`pg_advisory_xact_lock` del ADR 0003.

### 7. Imágenes directas, sin proxy

Coil carga las URLs de Numista tal cual. El proxy de la fase web sólo servía para activar COEP en
WASM.

### 8. Exportar lámina: la hoja completa, no lo visible

La rejilla de pantalla es perezosa y capturarla daría sólo lo visible, así que el export compone una
hoja aparte (`PlateSheet`) fuera de pantalla, con su propia `Density` y columnas en cuadrado para no
pasarse del bitmap que acepta una GPU.

La captura graba los comandos de dibujo en un `Picture` y lo reproduce sobre un bitmap software, sin
`GraphicsLayer`: evita el límite de textura de la GPU, y `Bitmap.createBitmap(Picture, …)` pasa por
un bitmap de hardware cuya copia a ARGB_8888 devuelve `null` en algunos dispositivos. Por lo mismo,
Coil tiene desactivados los bitmaps de hardware: un `Picture` que contenga alguno no se puede
reproducir por software. El export espera a las imágenes, con un techo de 20 s, y avisa si alguna no
cargó.

**Ampliación (ADR 0021 §13).** El cuaderno sale como PDF vectorial que reproduce el mismo `Picture`
de `recordInto`, a A4, con sólo el reverso a tamaño real, rejilla por lámina y regla de 50 mm.

**Después.** El ADR 0017 subió el techo a 30 s y sólo da por cargada la foto que llegó a pintarse.
Desde el #431 el PNG es la página del cuaderno recortada (`SheetPngExport`), con las opciones del
PDF; `PlateSheet` y su rejilla desaparecieron. La captura por `Picture` y los bitmaps de hardware
desactivados siguen.

## Consecuencias

- `domain/` es Kotlin puro, sin Android, con las tablas doradas portadas de `crates/domain/tests`.
- Los catálogos y el snapshot de tipos se empaquetan desde `data/`, sin copiarse a los assets: se
  curan y se publican en el mismo sitio.
- El emparejamiento heurístico y `data/series` se quedan sin tests; el código vive en el tag
  `rust-frozen`.

## Nota posterior (30 de julio de 2026)

El workspace Rust se retiró del árbol una vez verificada la app, y el proyecto Android subió a la
raíz. Las rutas `crates/…` se refieren al tag `rust-frozen`.
