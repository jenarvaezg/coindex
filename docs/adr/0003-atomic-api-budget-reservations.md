# ADR 0003: Reservas atómicas del presupuesto de API

Estado: aceptado

## Contexto

El cliente de Numista comprueba el presupuesto y registra cada llamada antes de enviarla.
Una consulta seguida de un `INSERT` sin sincronización permitiría que dos sync concurrentes
observaran el mismo saldo y superaran el límite mensual.

## Decisión

Postgres es la autoridad del presupuesto. El registro toma un
`pg_advisory_xact_lock`, vuelve a contar las filas del mes en curso y, si queda saldo,
inserta `api_call_log` dentro de la misma transacción. El bloqueo es común a todos los
usuarios y réplicas de la aplicación.

Se registra la reserva inmediatamente antes de enviar la petición. Una petición que falle
en transporte sigue contando: ha podido alcanzar Numista y consumir cuota. Si no se puede
persistir la reserva, la petición no sale.

## Consecuencias

- El límite no se rebasa por una carrera entre procesos.
- El registro puede sobreestimar ligeramente el consumo tras un fallo anterior al envío;
  es deliberadamente conservador.
- `api_call_log` no se borra durante los sync.
