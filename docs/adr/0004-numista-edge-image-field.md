# ADR 0004: Imagen del canto pendiente de verificación empírica

Estado: provisional

## Contexto

La documentación y los DTO de la API v3.32 permiten representar `obverse`, `reverse` y
`edge`, cada uno con `picture` y `thumbnail`. El proyecto prohíbe llamadas exploratorias:
solo el binario consciente de grabación de fixtures puede gastar cuota.

Las capturas empíricas públicas solicitadas con `lang=es` para N#386213, N#394043,
N#404044, N#404285 y N#482185 contienen `edge.description = "Reed"`, pero ninguna
contiene `edge.picture` ni `edge.thumbnail`.

## Decisión

El proxy acepta `edge` únicamente cuando la respuesta de tipo ya cacheada contiene una URL
HTTPS bajo `numista.com`. No se inventa ni deriva una URL desde la web del catálogo.

La evidencia permite afirmar únicamente que esas cinco respuestas reales describen el
canto sin exponer una URL de imagen. No demuestra que la API nunca incluya fotos del
canto para otros tipos. Esa conclusión más amplia seguirá pendiente hasta grabar
deliberadamente un tipo cuya ficha pública sí tenga dicha foto.

## Consecuencias

La ausencia de una URL de imagen del canto devuelve 404 y no bloquea la Fase 1. El modelo
y la ruta no requerirán cambios si una captura futura confirma el campo `picture`.
