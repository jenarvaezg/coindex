# ADR 0019: Los miembros pueden cualificar un tipo por emisión

- Status: accepted
- Date: 2026-08-02

## Context

Un tipo de Numista no siempre identifica una sola variante física. El 1 oz de Lunar Series III
de Perth Mint lo demuestra en tres años seguidos: una misma ficha contiene filas de bullion, de
proof coloured y, en 2023, también de proof. El título, el peso y el diámetro del tipo no bastan
para saber cuál tiene el coleccionista; la identidad está en `issue.id`, que el inventario ya
conserva dentro de la respuesta original de Numista (ADR 0014).

El caso de 2021 es N#235118. La casilla bullion es la emisión 582780 y la casilla proof coloured
acepta las emisiones 585569 y 582778. En 2023, N#342221 contiene la bullion 747609, la proof
coloured 970595 y una fila proof distinta. Son dos catálogos del mismo programa, con el mismo peso,
pero con acabados —y por tanto claves de variante— diferentes.

El dominio no podía expresarlo. `schema_version: 1` identificaba cada miembro sólo por
`numista_type_id`; `schema_version: 5` sí usaba `numista_issue_ids`, pero obligaba a declararlos en
todos los miembros porque representa un *issue run*: varias casillas de un tipo que comparten año y
se distinguen por emisión. Convertir los dos catálogos lunares en issue runs habría mentido sobre su
forma —son secuencias anuales que abarcan varios tipos— y habría hecho pagar IDs de emisión donde
no resolvían ninguna decisión.

Además, `deriveCollection` reducía los catálogos a un mapa `type_id -> primer catálogo`. El orden
alfabético de los assets hacía ganar a bullion y mandaba allí cualquier fila de N#342221, incluso la
proof coloured. Dar precedencia al miembro cualificado tampoco basta: la fila proof de 2023 caería
después al acabado inferido del tipo, `Bullion`, y volvería a entrar en el catálogo que acabábamos
de excluir.

## Decision

**Un miembro emitido de `schema_version: 1` puede cualificar opcionalmente su tipo con
`numista_issue_ids`.** La lista vacía conserva el significado histórico: el tipo completo identifica
la casilla. Una lista no vacía restringe la identidad a `(numista_type_id, issue_id)`; una pieza sin
emisión, o con una emisión fuera de la lista, no coincide.

La coincidencia se compone en este orden:

1. la cantidad es positiva y el `type_id` coincide;
2. si el miembro declara emisiones, `issue_id` debe pertenecer a su lista;
3. un date run exige además el año de la casilla; un issue run ignora el año como ya decidió el
   ADR 0014.

La misma identidad gobierna la evidencia que abre una lámina, salvo por el año: un date run sigue
siendo alcanzable con una pieza sin año, pero un miembro cualificado sólo aporta evidencia si la
emisión coincide. El tipo compartido por sí solo no abre las dos láminas.

**`schema_version: 5` conserva su significado exhaustivo.** Todos sus miembros emitidos deben
seguir declarando al menos una emisión y un mismo `issue_id` no puede llenar dos casillas. Las
versiones 2 y 3 no aceptan el cualificador: la 2 ya identifica sus casillas por año y la 3 representa
un producto emitido como set. Los miembros `announced` y `unlisted` tampoco pueden declarar
emisiones, porque no tienen un tipo propio publicado que cualificar.

**Compartir un tipo entre catálogos obliga a cerrar la ambigüedad en los ficheros.** Al cargar todos
los seeds, cada aparición de ese tipo en catálogos que no son sets debe estar cualificada y los
conjuntos de emisiones de los distintos catálogos deben ser disjuntos. Mezclar una reclamación
amplia por tipo con otra cualificada, o repetir el mismo par `(type_id, issue_id)`, detiene la carga.
La precedencia del nombre de fichero nunca decide propiedad editorial.

**El enrutado de propuestas pasa a ser consciente de la pieza.** Para un tipo con miembros
cualificados, `deriveCollection` busca el único catálogo cuya identidad acepta el `issue_id` de la
fila:

- una coincidencia toma la clave declarada por ese catálogo, como manda el ADR 0016;
- más de una es un fallo del conjunto de catálogos y nunca se resuelve por orden;
- ninguna deja la pieza sin clasificar con un motivo auditable y **no** vuelve a inferir acabado o
  peso desde los metadatos del tipo.

La última regla es de mundo cerrado sólo para el tipo cualificado. Es lo que deja la emisión proof
de N#342221 fuera tanto de bullion como de proof coloured: no rellena ninguna casilla, no evidencia
ninguna lámina y no materializa por accidente una propuesta bullion. Los tipos que ningún miembro
cualifica conservan exactamente el enrutado anterior.

## Relationship with earlier decisions

Este ADR estrecha dos decisiones aceptadas sin cambiar su propósito:

- Del ADR 0014 cae únicamente la prohibición de usar `numista_issue_ids` fuera de un issue run. La
  versión 5 sigue siendo la forma en que **todas** las casillas se identifican por emisión; la versión
  1 sólo puede cualificar miembros concretos.
- En el ADR 0016, «el catálogo nombra el tipo» pasa a significar «la identidad del miembro acepta la
  pieza» cuando hay cualificador. La autoridad del catálogo sobre peso, acabado y metal no alcanza
  una emisión que el miembro excluye, y el *snapping* no puede volver a introducirla.

## Alternatives considered

- **Convertir los Lunar en `schema_version: 5`.** Rechazado: un issue run modela varias casillas de
  un tipo y un año, no una secuencia anual de tipos distintos. También obligaría a buscar emisiones
  para todos los miembros por una necesidad localizada.
- **Crear otra versión de esquema.** Rechazado: el cualificador es ortogonal a la identidad básica
  de una casilla y ya existe el campo serializado. Una versión nueva duplicaría las reglas de la 1.
- **Hacer que el miembro cualificado gane al no cualificado.** Rechazado: toda emisión no enumerada
  caería en la reclamación amplia, justo el cruce que se quiere impedir.
- **Inferir el acabado después de no encontrar catálogo.** Rechazado: el acabado se infiere a nivel
  de tipo, mientras que la diferencia vive a nivel de emisión. En N#342221 convertiría la proof en
  bullion.

## Consequences

- Los catálogos bullion y proof coloured de Lunar Series III pueden compartir N#235118, N#307024 y
  N#342221 sin completar dos láminas con una sola pieza. En esta curación se cualifican los siete
  años de ambos catálogos, aunque la validación sólo lo obliga donde comparten tipo: cada fichero
  declara el producto exacto de Perth Mint y no cualquier acabado que Numista añada a su ficha.
- Una pieza cuyo JSON no deja leer `issue.id` tampoco puede atravesar una frontera cualificada. Se
  muestra sin clasificar en vez de adivinarse; mejorar el lector corrige filas antiguas al vuelo.
- La validación entre ficheros vive en `CatalogSeeds.parseAll`, porque un catálogo aislado no puede
  saber que otro reclama el mismo tipo. La ambigüedad se descubre al arrancar y no en función del
  orden de assets.
- No hay migración de base de datos, columna nueva ni llamada de API. `CollectedItem.issueId` ya se
  deriva de la respuesta cruda almacenada; sólo cambian la validación y el uso de ese dato.
- Los catálogos de las versiones 1, 2, 3 y 5 que no declaran el nuevo cualificador mantienen su
  comportamiento, salvo los issue runs: una pieza sin emisión deja de ser evidencia de su lámina,
  coherente con que tampoco puede llenar ninguna casilla.
