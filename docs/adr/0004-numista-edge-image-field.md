# ADR 0004: Imagen del canto pendiente de verificación empírica

Estado: provisional

## Contexto

La documentación y los DTO de la API v3.32 permiten representar `obverse`, `reverse` y
`edge`, cada uno con `picture` y `thumbnail`. El proyecto prohíbe llamadas exploratorias:
solo el binario consciente de grabación de fixtures puede gastar cuota.

## Decisión

El proxy acepta `edge` únicamente cuando la respuesta de tipo ya cacheada contiene una URL
HTTPS bajo `numista.com`. No se inventa ni deriva una URL desde la web del catálogo.

No afirmamos todavía que una respuesta real incluya la foto del canto. Esa parte se
resolverá al grabar deliberadamente un fixture de un tipo que tenga dicha foto, con
credenciales del propietario.

## Consecuencias

La ausencia de canto devuelve 404 y no bloquea la Fase 1. El modelo y la ruta no requerirán
cambios si el fixture confirma el campo.
