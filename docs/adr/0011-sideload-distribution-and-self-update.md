# ADR 0011 — Distribución por APK y actualización desde la propia app

Fecha: 2026-07-30
Estado: aceptado

## Contexto

Coindex se instala por APK directo en dos móviles (spec §0.1): sin Play Store, sin cuenta de
desarrollador. Eso resuelve la instalación inicial pero no las actualizaciones: pasar un
fichero por mensajería en cada cambio es fricción que recae sobre el padre de Jose, que no va
a compilar nada.

## Alternativas consideradas

| Opción | Por qué no |
|---|---|
| **Play Store** | Cuenta de desarrollador, revisiones y una ficha pública para una app de dos personas. |
| **Repositorio F-Droid propio** | Hay que hostear y firmar el índice del repo, e instalar el cliente F-Droid en ambos móviles. Más piezas que la app misma. |
| **Obtainium** | Funciona bien y no requiere código, pero es otra app que instalar y configurar en cada móvil, y el aviso vive fuera de Coindex. |
| **Aviso manual** | Cero infraestructura, pero la fricción vuelve en cada versión. |

## Decisión

**Coindex se actualiza a sí misma contra las releases públicas de GitHub.**

1. El repositorio `jenarvaezg/coindex` es público. Antes de publicarlo se auditó el historial
   completo: no contiene claves ni identificadores reales, solo marcadores de ejemplo. Sí
   contiene la especificación y los ADR, con notas sobre qué le falta a cada colección.
2. Cada release lleva dos assets: el APK firmado (`coindex-<versionCode>.apk`) y un
   `update.json` con `versionCode`, `versionName`, `apkAsset` y las notas.
3. El `versionCode` va en el manifiesto y **no** en el nombre del tag. El tag es para humanos
   y sería un sitio frágil donde codificar el dato del que depende la decisión de actualizar.
4. Al arrancar, la app consulta `/repos/{repo}/releases/latest`, lee el `update.json` y, si el
   `versionCode` publicado es mayor que el instalado, muestra una tarjeta en el índice con las
   notas y un botón que descarga el APK y lo entrega al instalador del sistema.
5. `scripts/release.sh` construye, **verifica la firma**, genera el `update.json` y publica.
   Antes de compilar se niega si falta `keystore.properties`, si el tag ya existe, si el
   `versionCode` no supera el publicado o si el árbol tiene cambios sin commitear.

### La firma no se automatiza

Se consideró publicar desde CI al empujar un tag. Exige subir el keystore y sus contraseñas
como secretos del repositorio, y esa clave es la identidad de Coindex para siempre: quien la
tenga puede publicar un APK que los dos móviles aceptarán como actualización legítima. Con dos
usuarios y un keystore irreemplazable, el ahorro no compensa sacar la clave de la máquina del
autor. El CI compila, prueba y anota si la versión del repositorio es publicable; publicar es
un acto local y deliberado.

### Lo que esto no hace

- **No instala en silencio.** Cada actualización la confirma el usuario en el diálogo del
  sistema; una instalación silenciosa exigiría ser device owner. Es el comportamiento correcto
  para una app de fuera de tienda.
- **No verifica la firma del APK descargado por su cuenta.** Android rechaza un APK firmado
  con una clave distinta a la instalada, así que la garantía la da la plataforma. Perder el
  keystore rompe la cadena de actualizaciones para siempre.
- **No gasta presupuesto de Numista.** Estas peticiones van a GitHub y quedan fuera del
  `CallBudgetGate` a propósito.

## Consecuencias

- Un fallo al comprobar (sin red, sin releases, manifiesto roto) queda en
  `UpdateStatus.Unavailable` y no interrumpe nada: una comprobación de actualización no debe
  dar la lata.
- Todo `startActivity` hacia el sistema va protegido. Se descubrió al probar en una imagen ATD
  sin app de Settings: un `ActivityNotFoundException` tumbaba la app entera. En un móvil real
  no ocurre, pero un intent sin resolver no debe ser fatal.
- La tarjeta de actualización vive en el índice, así que no se ve mientras el alta esté
  pendiente. Aceptable: en ese momento la app se acaba de instalar.
- Subir `versionCode` en cada versión pasa a ser obligatorio, y el script lo comprueba
  indirectamente al negarse a reutilizar un tag.
