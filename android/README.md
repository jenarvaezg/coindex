# Coindex para Android

App local-first que organiza la colección de plata de Numista en propuestas y láminas. Dos
usuarios reales, instalación por APK, sin backend. La implementación Rust de la raíz del
repositorio queda congelada como referencia (ver `spec.md` §0 y `docs/adr/0010`).

## Estructura

```
android/
├── domain/   # Kotlin puro, sin Android: propuestas, catálogos v1/v2, acabados, pesos
└── app/      # Compose + Room + Ktor + Coil
```

Los catálogos curados y el snapshot de la caché de tipos **no se copian**: el módulo `app`
monta `../../data` como directorio de assets, así que las láminas de la app y la referencia
Rust leen exactamente los mismos ficheros.

## Requisitos

- JDK 21. El que trae Android Studio sirve:
  `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`
- SDK de Android con `platforms;android-36`, `build-tools;36.0.0` y `platform-tools`.
  `local.properties` apunta al SDK (no se versiona).

## Comandos

```console
./gradlew :domain:test           # tabla dorada del dominio
./gradlew :app:testDebugUnitTest # cliente de Numista, sync, presupuesto, catálogos reales
./gradlew :app:assembleDebug     # APK de depuración
./gradlew :app:assembleRelease   # APK de release (firmado si hay keystore.properties)
```

Ningún test toca la red: todo sale de `fixtures/numista/` y de `data/`.

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
- Las series curadas (`data/series`) y el emparejamiento heurístico no se portaron; siguen
  vivos en el workspace Rust congelado (ADR 0010 §2).
