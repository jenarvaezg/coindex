# ADR 0019: Los miembros pueden cualificar un tipo por emisión

- Status: accepted
- Date: 2026-08-02

## Context

Un tipo de Numista no siempre es una sola variante física. En el 1 oz de Lunar Series III de Perth
Mint una ficha mezcla filas de bullion, de proof coloured y, en 2023, de proof; sólo `issue.id`, que
el inventario ya conserva (ADR 0014), dice cuál tiene el coleccionista. En 2021, N#235118: la casilla
bullion es la emisión 582780 y la proof coloured acepta 585569 y 582778. En 2023, N#342221 tiene la
bullion 747609, la proof coloured 970595 y una proof. Son dos catálogos del mismo programa y peso con
claves distintas.

`schema_version: 1` identificaba los miembros sólo por tipo, y `schema_version: 5` exige emisiones
en todos porque es un issue run. Además, `deriveCollection` reducía los catálogos a
`type_id -> primer catálogo`, así que el orden alfabético mandaba a bullion cualquier fila de
N#342221; y dar precedencia al miembro cualificado no basta, porque la proof de 2023 caería al
acabado inferido del tipo, `Bullion`.

## Decision

**Un miembro emitido de `schema_version: 1` puede cualificar su tipo con `numista_issue_ids`.**
Vacía, la lista no cambia nada; no vacía, la identidad es `(numista_type_id, issue_id)`, y una pieza
sin emisión o con otra no coincide. La coincidencia exige, en orden: cantidad positiva y `type_id`;
`issue_id` en la lista, si la hay; y el año en un date run (un issue run lo ignora, ADR 0014). La
evidencia que abre una lámina usa la misma identidad salvo el año: un miembro cualificado sólo
evidencia con su emisión, y el tipo compartido no abre las dos láminas.

**`schema_version: 5` sigue siendo exhaustivo**: todos sus miembros emitidos declaran emisiones, y un
`issue_id` no llena dos casillas. **`schema_version: 2` puede cualificar** una casilla de año para
excluir acabados que Numista mete en el mismo tipo (proof, burnished, privy temático). La versión 3
no acepta el cualificador, ni los miembros `announced` o `unlisted`, que no tienen tipo publicado.

**Compartir un tipo entre catálogos obliga a resolverlo en los ficheros.** Al cargar los seeds, cada
aparición de un tipo compartido en catálogos que no son sets debe estar cualificada, con emisiones
disjuntas; una reclamación amplia junto a una cualificada, o un par `(type_id, issue_id)` repetido,
detiene la carga. El nombre de fichero nunca decide.

**El enrutado de propuestas es consciente de la pieza.** Para un tipo con miembros cualificados,
`deriveCollection` busca el único catálogo que acepta el `issue_id` de la fila: si hay uno, toma su
clave (ADR 0016); si hay más, es un fallo de los catálogos; si no hay ninguno, la pieza queda sin
clasificar y **no** se infiere acabado ni peso desde el tipo. Así la proof de N#342221 no entra ni en
bullion ni en proof coloured. Los tipos sin miembros cualificados no cambian.

## Relationship with earlier decisions

- Del ADR 0014 cae sólo la prohibición de usar `numista_issue_ids` fuera de un issue run.
- En el ADR 0016, «el catálogo nombra el tipo» pasa a ser «la identidad del miembro acepta la
  pieza»: la autoridad del catálogo no alcanza una emisión excluida, y el *snapping* no la
  reintroduce.

## Alternatives considered

- **Convertir los Lunar en `schema_version: 5`.** Un issue run son casillas de un tipo y un año, no
  una secuencia anual de tipos, y obligaría a buscar emisiones de todos los miembros.
- **Otra versión de esquema.** El cualificador es ortogonal y el campo ya existe; duplicaría las
  reglas de la 1.
- **Que el miembro cualificado gane al amplio.** Las emisiones no enumeradas caerían en el amplio.
- **Inferir el acabado al no encontrar catálogo.** Se infiere por tipo y la diferencia está en la
  emisión: convertiría la proof de N#342221 en bullion.

## Consequences

- Los catálogos bullion y proof coloured de Lunar Series III comparten N#235118, N#307024 y
  N#342221 sin que una pieza complete dos láminas. Se cualifican los siete años de ambos aunque sólo
  sea obligatorio donde comparten tipo: cada fichero declara el producto exacto de Perth Mint.
- El American Silver Eagle (#91) es el primer date run que cualifica: N#1493 y N#298883 mezclan
  bullion, proof y burnished en la misma ficha.
- Una pieza cuyo `issue.id` no se puede leer queda sin clasificar; mejorar el lector corrige filas
  antiguas.
- La validación entre ficheros vive en `CatalogSeeds.parseAll`, porque un catálogo aislado no ve a
  los demás.
- Sin migración, columna ni llamada nueva: `CollectedItem.issueId` ya se deriva de la respuesta
  cruda.
- En los issue runs, una pieza sin emisión deja de evidenciar su lámina.
