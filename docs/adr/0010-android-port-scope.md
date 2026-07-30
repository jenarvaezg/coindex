# ADR 0010 — Alcance del port a Android y decisiones de la app local-first

Fecha: 2026-07-30
Estado: aceptado

## Contexto

Tras el pivot del 29 de julio de 2026 (spec §0), Coindex pasa a ser una app de Android
local-first y la implementación Rust queda congelada como referencia. Al portar
`crates/domain` a Kotlin aparecieron decisiones que la spec no cerraba o que discrepan de
la fase web. Se anotan aquí porque, cuando la spec y la realidad difieren, gana la realidad.

## Decisiones

### 1. Identidad de la app

`Coindex`, `applicationId` `com.jenarvaezg.coindex` (spec §0.9 cuestión 1). `minSdk` 29,
`compileSdk`/`targetSdk` 36 — 36 es la plataforma más alta publicada hoy, lo que obliga a
fijar `androidx.core` 1.18.0 y `lifecycle` 2.10.0: las versiones siguientes exigen
compilar contra la API 37.

### 2. Las series curadas y las correcciones manuales no se portan

La UI ya había retirado las láminas curadas del índice (spec §0.4) y los catálogos v1/v2
cubren su papel. Por tanto **no** se portan `Series`, `Slot`, `Matcher`, `build_album` ni
`ManualOverride`: eran la maquinaria de emparejar piezas contra casillas definidas a mano,
con heurísticas que había que poder corregir. Las propuestas se derivan de la familia de
Numista de forma determinista, sin heurísticas, así que no hay nada que corregir a mano.

Consecuencia: `data/series/*.json` no viaja en la app. Sigue en el repo como datos para
futuros catálogos (spec §0.9 cuestión 3).

### 3. Las huérfanas se redefinen y ganan un motivo auditable

En la fase web «sin clasificar» eran los items que ninguna casilla de una serie curada
recogía. Sin series curadas, la definición pasa a ser: **toda pieza que no produjo
propuesta**, con el motivo explícito (`UnclassifiedReason`):

- `MissingTypeMetadata` — la ficha del tipo aún no se ha descargado.
- `TechnicalFamily` — Numista la agrupa en un `System YYYY[-YYYY]`.
- `NoFamilyOrCatalog` — sin familia en Numista y sin catálogo que la referencie.
- `UnknownWeight` — sin peso, así que no se puede identificar la variante física.

Se mantiene la propiedad que importaba: nada se descarta en silencio.

### 4. El acabado se infiere al leer, no se almacena

La tabla `type_meta` guarda `title` y `family`, y `Finish` se infiere en cada lectura con
las reglas del ADR 0005. Así, mejorar la inferencia corrige tipos cacheados hace meses sin
volver a gastar presupuesto de API. La fase web hacía lo mismo al cargar; aquí se hace
explícito porque había la tentación de materializar una columna.

### 5. El sync guarda el inventario antes de las fichas de tipo

La versión Rust descargaba las fichas y luego guardaba el inventario, de modo que un fallo
a mitad dejaba el inventario viejo. La app invierte el orden: guarda el inventario y luego
completa las fichas. Un sync que se queda sin presupuesto a mitad deja el inventario
fresco y las piezas cuyo tipo falta aparecen como huérfanas `MissingTypeMetadata` hasta el
siguiente sync. Se sigue protegiendo el caso peligroso: una respuesta sin `items`, o con
`items` vacío, **nunca** borra el snapshot anterior.

### 6. Presupuesto por dispositivo, no ledger compartido

Cada usuario introduce su propia API key (cifrada con una clave AES/GCM del Android
Keystore; solo el criptograma llega a `SharedPreferences`). El contador mensual vive en
`api_call_log` y el techo es configurable, por defecto 1500. La reserva serializa
contar-y-registrar bajo un mutex, que es el equivalente local del `pg_advisory_xact_lock`
del ADR 0003.

### 7. Imágenes directas, sin proxy

Coil carga las URLs de Numista tal cual. El proxy de la fase web existía para poder
activar COEP en la fase 2 de WASM, que ya no aplica.

### 8. Exportar lámina: la hoja completa, no lo visible

La rejilla de la pantalla es perezosa, así que capturarla daría solo la parte visible. El
export compone en su lugar una hoja aparte (`PlateSheet`) que sí es eager, fuera de pantalla,
con su propia `Density` y un número de columnas cuadrado: 121 emisiones en dos columnas
serían un bitmap más alto de lo que acepta cualquier GPU o destino de compartición. La
densidad baja al crecer el catálogo, de modo que los pequeños salen nítidos y los grandes
caben en unos pocos miles de píxeles.

La captura no usa `GraphicsLayer`: el nodo graba sus comandos de dibujo en un `Picture` sin
pintarse, y el `Picture` se reproduce sobre un bitmap software. Dos razones: evita el límite
de tamaño de textura de la GPU, y `Bitmap.createBitmap(Picture, …)` pasa por un bitmap de
hardware cuya copia a ARGB_8888 devuelve `null` en algunos dispositivos (visto en el
emulador). Por lo mismo, el `ImageLoader` de Coil desactiva los bitmaps de hardware: un
`Picture` que contenga alguno no se puede reproducir por software.

El export espera a que todas las imágenes de la hoja se resuelvan —con éxito o con error,
porque una que falla nunca llega— con un techo de 20 s, y avisa si alguna no cargó.

## Consecuencias

- El dominio Kotlin (`domain/`) es puro y sin dependencias de Android, con las tablas doradas
  portadas de `crates/domain/tests`.
- Los catálogos y el snapshot de tipos se empaquetan desde `data/` en lugar de copiarse a los
  assets, así que se curan y se publican en el mismo sitio.
- `data/series` y el emparejamiento heurístico dejan de tener cobertura de tests: sus JSON
  quedan como datos inertes y el código con sus tests vive en el tag `rust-frozen`.

## Nota posterior (30 de julio de 2026)

El workspace Rust se retiró del árbol de trabajo una vez la app estuvo instalada y verificada,
y el proyecto Android subió a la raíz del repositorio. Las rutas `crates/…` que cita este ADR
se refieren al tag `rust-frozen`.
