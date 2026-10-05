# Coindex

App de Android local-first que ordena en láminas una colección de Numista: qué piezas hay y cuáles
faltan de cada serie catalogada. Dos usuarios reales, instalación por APK, sin backend.

La primera implementación, web en Rust (Axum, Maud, SQLx, Shuttle), y su especificación siguen en
el tag `rust-frozen` (`git checkout rust-frozen`). Lo vigente está en `spec.md`.

## Estructura

```
├── domain/     # Kotlin puro, sin Android: colecciones, catálogos, acabados, pesos
├── app/        # Compose + Room + Ktor + Coil
├── data/       # catálogos curados y snapshot de la caché de tipos (assets de la app)
├── fixtures/   # respuestas grabadas de Numista, que leen los tests
├── docs/adr/   # decisiones de arquitectura
└── scripts/    # publicar releases, grabar fixtures, informes de curación
```

El módulo `app` monta `../data` como directorio de assets: los catálogos curados y el snapshot se
empaquetan desde donde se curan, sin copiarlos.

## Requisitos

- JDK 21. El que trae Android Studio sirve:
  `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`
- SDK de Android con `platforms;android-36`, `build-tools;36.0.0` y `platform-tools`.
  `local.properties` apunta al SDK (no se versiona).

El proyecto está en la raíz: se abre directamente con Android Studio, sin elegir subcarpeta.

## Comandos

```console
./gradlew :domain:test           # tabla dorada del dominio
./gradlew :app:testDebugUnitTest # cliente de Numista, sync, presupuesto, catálogos reales
./gradlew :app:assembleDebug     # APK de depuración
./gradlew :app:assembleRelease   # APK de release (firmado si hay keystore.properties)
```

Ningún test toca la red: todo sale de `fixtures/numista/` y de `data/`. Refrescar un fixture gasta
presupuesto de la API y se pide a mano:

```console
export NUMISTA_API_KEY=...
scripts/record-fixture.py --confirm-live-api --type-id 404044
```

## Primer arranque

La app pide la API key de Numista y el identificador de usuario. La key se cifra con una clave
AES/GCM del Android Keystore y sólo el criptograma llega a `SharedPreferences`. Cada usuario gasta
su propia cuota, con un techo mensual interno de 1500 consultas contadas en `api_call_log` antes de
cada llamada.

La caché de tipos se siembra desde `data/numista-type-cache.json`. En cada arranque se rellenan los
tipos que nombran los ficheros curados y faltan en caché (ADR 0017), y el primer arranque de una
versión nueva reescribe las fichas cacheadas con las de su snapshot, para que las correcciones de
Numista lleguen sin gastar consultas (ADR 0033).

Un catálogo curado inválido detiene el arranque con el fichero y el motivo: mejor no arrancar que
mostrar un «me falta» falso.

## Firmar el APK

```console
keytool -genkeypair -v -keystore ~/keys/coindex-release.jks \
  -alias coindex -keyalg RSA -keysize 4096 -validity 10000
cp keystore.properties.example keystore.properties   # y rellénalo
./gradlew :app:assembleRelease
```

**Conserva el keystore y sus contraseñas para siempre**: una actualización firmada con otra clave
no se instala encima, y habría que desinstalar y perder la base de datos local.

Instalación en el móvil: `adb install -r app/build/outputs/apk/release/app-release.apk`, o
copiar el APK y permitir la instalación de orígenes desconocidos.

## Actualizaciones

Coindex se actualiza a sí misma contra las releases públicas de
[jenarvaezg/coindex](https://github.com/jenarvaezg/coindex/releases) (ADR 0011). Busca un
`versionCode` mayor que el instalado al abrir la app, al volver a primer plano y cada 6 h, con un
intervalo mínimo entre comprobaciones. Sin notificaciones: si hay versión nueva aparece un **banner
fijo bajo la cabecera** con las notas y un botón que descarga el APK y lo entrega al instalador. La
primera vez Android pide el permiso de instalar aplicaciones. La versión instalada se lee en «Avisos
y licencias».

Publicar una versión nueva:

```console
# 1. sube versionCode y versionName en app/build.gradle.kts
# 2. commit y push
scripts/release.sh                      # notas a partir de los commits
scripts/release.sh "Resumen opcional"   # o un resumen tuyo para el banner
```

Antes de compilar, el script se niega si falta `keystore.properties`, si el tag ya existe, si el
`versionCode` no supera el publicado (subir sólo `versionName` da una release que ningún móvil ve) o
si hay cambios sin commitear. Después construye el APK, **verifica la firma**, genera el
`update.json` y crea la release: el resumen va al banner y el changelog al cuerpo de la release.

**La firma se hace aquí, no en CI**: el keystore no viaja a ningún servicio (ADR 0011). El CI
compila y prueba en cada push y anota si la versión es publicable, pero nunca publica.

## Exportar láminas

Una lámina se exporta como PNG y el cuaderno entero como PDF, con el mismo dibujo: el PNG es la
página impresa recortada a su contenido (#431), con las opciones elegidas al exportar (fotos, caras,
tamaño real, QR, valor). La página se graba en un `Picture` que se reproduce sobre un bitmap
software o sobre el PDF; por eso Coil tiene desactivados los bitmaps de hardware. Si al terminar
falta alguna foto, el aviso dice cuántas (ADR 0017).

## Exportar datos

«Exportar datos», en «Este teléfono», comparte una copia de `coindex.db`: colección, fichas,
precios y marcas. Antes se vuelca el diario WAL (`PRAGMA wal_checkpoint(TRUNCATE)`); sin eso el
fichero no tendría las últimas transacciones. **La API key no viaja**: está cifrada con la Keystore
del dispositivo, no en una tabla. El alta en el emulador sigue siendo a mano.

El fichero se llama `coindex-<versión>-<fecha>.db`: **lo carga un APK de versión igual o posterior**,
porque las migraciones de Room sólo van hacia delante. Para cargarlo en el emulador se copia al
vault de `scripts/avd-db.sh` como `coindex.db` y se restaura como cualquier volcado. No hay
importación en la app: el destino es un Mac y un AVD, no otro móvil.

## Fotos del catálogo

Las fotos son de Numista y las pide el `ImageLoader` del paquete `data/photos` de `app`: la
**miniatura** (`-180.jpg`) con el original de respaldo, de cuatro en cuatro, con reintentos y un
`User-Agent` propio (`Coindex/<versión>`), porque sin él Cloudflare responde `403` (ADR 0017). Con
wifi se precargan en segundo plano (ADR 0024).

## Limitaciones conocidas

- **R8 desactivado** en release: minificar reduciría mucho el APK, pero no se ha hecho sin poder
  verificar en un dispositivo real que nada se rompe.
- Las series curadas y el emparejamiento heurístico no se portaron (ADR 0010 §2): el código y sus
  tests viven en el tag `rust-frozen`, y sus dos JSON (`data/series/lunar-iii.json` y
  `data/series/tudor-beasts.json`) quedan en el historial, en el commit `9fc2582`.
