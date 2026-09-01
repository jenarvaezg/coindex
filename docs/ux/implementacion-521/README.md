# Implementación · «Este teléfono», y las credenciales un piso más abajo (#521)

La pantalla que abre el glifo del canto deja de llamarse «Ajustes» y pasa a ser **«Este teléfono»**:
el mantenimiento del inventario arriba, y **«Credenciales»** colgando del pie en la forma que el
ADR 0026 §14 escribió para los avisos de licencia. Nombre elegido por Jose el 1 de septiembre de 2026
sobre tres candidatos; el resto de las decisiones salieron del grillado del issue.

El #521 era el último issue abierto de la auditoría del 14 de agosto de 2026 (#508–#522).

## Lo que cambia en pantalla

| | antes | después |
| --- | --- | --- |
| el glifo del canto | `content-desc="Ajustes"` | `content-desc="Este teléfono"`, el mismo dibujo |
| lo primero de la pantalla | el rótulo «Credenciales» y sus dos campos | `antes-ajustes.png` → `despues-este-telefono.png`: **«Sincronizar»**, la única acción llena |
| «Sincronizar» | a 453 dp del borde del contenido, tras dos campos de texto | a **24 dp**: la primera cosa de la página |
| los dos campos | arriba, en la pantalla que se visita para sincronizar | `despues-credenciales.png`: una pantalla propia, tras «Credenciales» |
| «Cerrar sesión» | al pie de la pantalla de mantenimiento | baja con los campos que borra |
| «Exportar datos» | junto a «Cerrar sesión» | se queda en el mantenimiento: es la base de este teléfono saliendo |
| el pie | `antes-ajustes-pie.png`: «Cerrar sesión» y «Avisos y licencias» bajo el pliegue | «Credenciales» y «Avisos y licencias», ambos a la vista |
| los tres rechazos de sincronización | «Añádela en Ajustes» | «Añádela en Credenciales», interpolando el nombre del sitio |

## Lo medido en el emulador

AVD `coindex-ux`, 1080×2400, densidad 420 (2,625), con la colección del padre sembrada desde
`.local/padre` con `scripts/avd-db.sh restore` (69 colecciones, 580 piezas, 198 tipos, 2.188 fotos,
231 emisiones tasadas). Leído de los `bounds` que publica `uiautomator dump`, no del pantallazo. El
«antes» es el APK de depuración de `main` en 4f00647; el «después», el de esta rama.

| | antes | después |
| --- | ---: | ---: |
| alto del contenido visible (del canto a la barra) | 1961 px · 747 dp | 1961 px · 747 dp |
| «Sincronizar», borde superior | 1565 px · **453 dp** dentro del contenido | 440 px · **24 dp** |
| alto total de la página | no cabía | 1489 px · **567 dp** |
| bloques bajo el pliegue al abrir | **tres** (exportar, cerrar sesión, avisos) | **ninguno** |
| arrastres para llegar al pie | tres | **cero** |
| «Credenciales», alto total | — | 1341 px · 511 dp, también de una vista |

Las dos páginas caben enteras: al partir la de antes en dos, ninguna de las mitades necesita
desplazarse en un teléfono de este tamaño. Ésa es la medida que el grillado no podía anticipar —
la pregunta abierta era si la mitad de mantenimiento sola llenaría la primera vista, y ocupa 567 de
los 747 dp disponibles.

## Lo que no se ha fotografiado, y por qué

**La puerta del ADR 0028 §6.1** — la fila «Credenciales» que la tarjeta de precios saca cuando culpa
a la clave — no está en ninguna captura. Para verla hace falta que la tasación esté rechazada, y
llegar a ese estado en el AVD pide teclear una API key inválida: los `input keyevent` necesarios para
vaciar el campo tumbaron el SystemUI del emulador dos veces («System UI isn't responding»), que es la
exageración del render por software que ya midió el #514. Lo que sostiene la fila son los tres tests
de `ValuationDoorTest`, que fijan los ocho casos: los dos que abren, los cuatro que sólo se pueden
esperar, y los dos en los que la línea de arriba no se está quejando.

De paso, un hallazgo que sale de mirar el código para intentar forzarlo: **de los dos estados que
abren la puerta, sólo uno es alcanzable**. `onboarded` se deriva de que haya credenciales
(`CoindexViewModel.kt:312`), así que en un teléfono dado de alta `NoApiKey` no puede darse — y en uno
que no lo está, esta pantalla no existe. El estado que el padre puede ver es `Rejected`. La condición
cubre los dos igualmente: `NoApiKey` es defensa, no camino.

## Lo que cambia en el código

- `Routes.SETTINGS` → `Routes.PHONE`, más `Routes.CREDENTIALS`. `SETTINGS_LABEL` → `PHONE_LABEL`.
- `SettingsScreen` se parte en `PhoneScreen` y `CredentialsScreen`.
- `SettingsLabels.kt` se reparte en tres ficheros por la pantalla que habla: `PhoneLabels.kt`,
  `CredentialsLabels.kt` y `NoticesLabels.kt`. El comentario de §5 viaja con cada uno.
- `SettingsEntry` → `CredentialsEntry`; `saveSettings` → `saveCredentials`, y el `saveCredentials` que
  ya existía para el alta pasa a `completeOnboarding` — eran dos viajes por los mismos dos campos con
  firmas distintas, y el renombrado los dejó ambiguos.
- «Guardar ajustes» → **«Guardar»** (la cabecera ya dice de qué), y «Ajustes guardados.» →
  «Credenciales guardadas.» (se lee después de salir, así que nombra lo guardado y no el sitio).
- `valuationBlamesCredentials`, la condición de la puerta, con su test.
- Los tres rechazos de `SyncMessages` interpolan `CREDENTIALS_LABEL` en vez de escribir el nombre.

## Los ADR que se enmiendan

- **ADR 0026 §5 cláusula 3**: la exención por frecuencia dejaba de nombrar pantallas («Ajustes and
  onboarding»), porque una pantalla que se parte en dos tenía que enmendar la regla para conservar una
  exención que ya se había ganado por frecuencia. §14: «tres palabras al pie» pasa a ser una **forma**
  con dos usuarios, y se le añaden las dos reglas que son el precio del anidamiento.
- **ADR 0028**: §6 gana el §6.1 — la línea se lee, el pase no tiene manivela, y dos de los seis
  estados abren puerta. §7 cláusula 2 nombra la pantalla nueva.
- **ADR 0024**: la línea silenciosa se lee en «Este teléfono», y sigue siendo línea y no botón.

## Lo que queda fuera

**El dibujo del glifo sigue siendo tres deslizadores.** El issue se queja de que «sugiere filtros», y
lo que se ha arreglado es lo que dice: su `content-desc` y la cabecera de lo que abre. Cambiar el
dibujo es una decisión de trazo que pide maqueta y medida propias, no un efecto colateral de esta.
