# ADR 0011 — Distribución por APK y actualización desde la propia app

Fecha: 2026-07-30
Estado: aceptado

## Contexto

Coindex se instala por APK directo en dos móviles (spec §0.1), sin Play Store ni cuenta de
desarrollador. Faltaba cómo llegan las actualizaciones sin pasarle un fichero por mensajería al
padre de Jose en cada cambio.

## Alternativas consideradas

| Opción | Por qué no |
|---|---|
| **Play Store** | Cuenta de desarrollador, revisiones y una ficha pública para una app de dos personas. |
| **Repositorio F-Droid propio** | Hay que hostear y firmar el índice del repo, e instalar el cliente F-Droid en ambos móviles. Más piezas que la app misma. |
| **Obtainium** | Funciona bien y no requiere código, pero es otra app que instalar y configurar en cada móvil, y el aviso vive fuera de Coindex. |
| **Aviso manual** | Cero infraestructura, pero la fricción vuelve en cada versión. |

## Decisión

**Coindex se actualiza a sí misma contra las releases públicas de GitHub.**

1. El repositorio `jenarvaezg/coindex` es público. Antes se auditó el historial: no contiene claves
   ni identificadores reales, aunque sí la spec y los ADR, con lo que le falta a cada colección.
2. Cada release lleva el APK firmado (`coindex-<versionCode>.apk`) y un `update.json` con
   `versionCode`, `versionName`, `apkAsset` y las notas.
3. El `versionCode` va en el manifiesto, no en el nombre del tag, que es para humanos.
4. La app consulta `/repos/{repo}/releases/latest` y, si el `versionCode` publicado supera al
   instalado, ofrece descargar el APK y entregarlo al instalador del sistema. *(Hoy es un banner
   bajo la cabecera, en todas las pantallas, y se comprueba también al volver a primer plano y cada
   6 h; ver README.)*
5. `scripts/release.sh` construye, **verifica la firma**, genera el `update.json` y publica. Antes de
   compilar se niega si falta `keystore.properties`, si el tag ya existe, si el `versionCode` no
   supera el publicado o si hay cambios sin commitear.

### La firma no se automatiza

Publicar desde CI exigiría subir el keystore como secreto del repositorio, y quien tenga esa clave
puede publicar un APK que los dos móviles aceptarán como legítimo. El CI compila, prueba y anota si
la versión es publicable; publicar es un acto local y deliberado.

### Lo que esto no hace

- **No instala en silencio**: cada actualización se confirma en el diálogo del sistema (lo contrario
  exigiría ser device owner).
- **No verifica la firma por su cuenta**: Android rechaza un APK firmado con otra clave. Perder el
  keystore rompe la cadena de actualizaciones para siempre.
- **No gasta presupuesto de Numista**: estas peticiones van a GitHub, fuera del `CallBudgetGate`.

## Consecuencias

- Un fallo al comprobar (sin red, sin releases, manifiesto roto) queda en
  `UpdateStatus.Unavailable` y no interrumpe nada.
- Todo `startActivity` hacia el sistema va protegido: un intent sin resolver lanzaba
  `ActivityNotFoundException` y tumbaba la app (visto en una imagen ATD sin Ajustes).
