# Coindex

App de Android local-first que organiza una colección de plata de Numista en propuestas y
láminas: qué piezas están y cuáles faltan de cada serie catalogada. Dos usuarios reales,
instalación por APK, sin backend.

Hubo una primera implementación web en Rust (Axum, Maud, SQLx, Shuttle) que se retiró del
árbol al portar el dominio a Kotlin. Sigue siendo consultable:

```console
git checkout rust-frozen
```

El apéndice de `spec.md` documenta esa fase, y sigue siendo la mejor descripción del dominio,
de la API de Numista y de la estética.

## Estructura

```
├── domain/     # Kotlin puro, sin Android: propuestas, catálogos v1/v2, acabados, pesos
├── app/        # Compose + Room + Ktor + Coil
├── data/       # catálogos curados y snapshot de la caché de tipos (assets de la app)
├── fixtures/   # respuestas grabadas de Numista, que leen los tests
├── docs/adr/   # decisiones de arquitectura
└── scripts/    # publicar releases, grabar fixtures
```

Los catálogos curados y el snapshot de la caché de tipos **no se copian a los assets**: el
módulo `app` monta `../data` como directorio de assets, así que se empaquetan desde donde se
curan.

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

Ningún test toca la red: todo sale de `fixtures/numista/` y de `data/`. Para refrescar un
fixture hay que pedirlo a mano y de forma consciente, porque gasta presupuesto de la API:

```console
export NUMISTA_API_KEY=...
scripts/record-fixture.py --confirm-live-api --type-id 404044
```

## Primer arranque

La app pide la API key de Numista y el identificador de usuario. La key se cifra con una
clave AES/GCM que vive en el Android Keystore y nunca sale de él; solo el criptograma llega
a `SharedPreferences`. Cada usuario gasta su propio presupuesto de API, con un techo mensual
configurable (1500 por defecto) que se cuenta en `api_call_log` antes de cada llamada.

En el primer arranque se siembra la caché de tipos con `data/numista-type-cache.json` (608
tipos, ~630 llamadas de API que nadie tiene que volver a gastar). Un catálogo curado
inválido detiene el arranque con el fichero y el motivo: es preferible no arrancar a mostrar
un «me falta» falso.

## Firmar el APK

```console
keytool -genkeypair -v -keystore ~/keys/coindex-release.jks \
  -alias coindex -keyalg RSA -keysize 4096 -validity 10000
cp keystore.properties.example keystore.properties   # y rellénalo
./gradlew :app:assembleRelease
```

**Conserva el keystore y sus contraseñas para siempre.** Una actualización firmada con otra
clave no se puede instalar encima de la anterior; habría que desinstalar y perder la base de
datos local.

Instalación en el móvil: `adb install -r app/build/outputs/apk/release/app-release.apk`, o
copiar el APK y permitir la instalación de orígenes desconocidos.

## Actualizaciones

Coindex se actualiza a sí misma contra las releases públicas de
[jenarvaezg/coindex](https://github.com/jenarvaezg/coindex/releases) (ADR 0011). Comprueba si
hay una versión con `versionCode` mayor que el instalado al abrir la app, al volver a primer
plano y cada 6 h mientras siga abierta, con un suelo de tiempo para no repetir la consulta en
cada vuelta. No hay notificaciones: el aviso vive dentro de la app.

Cuando hay versión nueva aparece un **banner fijo bajo la cabecera**, visible en todas las
pantallas, con la versión, las notas y un botón que descarga el APK y lo entrega al
instalador del sistema. La primera vez, Android pedirá conceder a Coindex el permiso de
instalar aplicaciones; después basta confirmar cada actualización. La cabecera muestra siempre
la versión instalada, así que se ve de un vistazo si la actualización se aplicó.

Publicar una versión nueva:

```console
# 1. sube versionCode y versionName en app/build.gradle.kts
# 2. commit y push
scripts/release.sh                      # notas a partir de los commits
scripts/release.sh "Resumen opcional"   # o un resumen tuyo para el banner
```

El script comprueba primero, sin compilar nada, que la versión es publicable, y se niega si:

- falta `keystore.properties`, porque el APK saldría sin firmar;
- el tag ya existe;
- el `versionCode` no supera el de la release publicada, que es el error silencioso de verdad:
  subir solo el `versionName` produce una release que ningún móvil llega a ver;
- el árbol tiene cambios sin commitear, porque la release apuntaría a un commit que no los
  incluye.

Después construye el APK, **verifica la firma**, genera el `update.json` y crea la release. El
resumen va al banner de la app y el changelog completo al cuerpo de la release.

**La firma se hace aquí, no en CI**: el keystore no viaja a ningún servicio (ADR 0011). El CI
compila y prueba en cada push, y anota en el resumen del job si la versión del repositorio es
publicable, pero nunca publica.

## Exportar lámina

«Exportar lámina como imagen» compone la hoja **completa** fuera de pantalla —con su propia
densidad, no la del móvil—, espera a que Coil termine con todas las imágenes y la graba en un
`Picture` que se reproduce sobre un bitmap software. El PNG resultante lleva la cabecera con
el progreso, todas las emisiones (las que faltan en gris) y la fuente al pie. Un catálogo de
121 emisiones sale en ocho columnas a menor densidad para que el bitmap no se desmande.

Los bitmaps de hardware están desactivados en Coil: un `Picture` no se puede reproducir sobre
un canvas software si contiene alguno.

## Limitaciones conocidas

- **R8 desactivado** en release: el APK pesa ~29 MB. Activar minificación reduciría mucho el
  tamaño, pero no se ha hecho sin poder verificar en un dispositivo real que nada se rompe.
- Las series curadas y el emparejamiento heurístico no se portaron (ADR 0010 §2): el código con
  sus tests vive en el tag `rust-frozen`, y sus dos JSON (`data/series/lunar-iii.json` y
  `data/series/tudor-beasts.json`) se retiraron del árbol — quedan en el historial, en el commit
  `9fc2582`.
