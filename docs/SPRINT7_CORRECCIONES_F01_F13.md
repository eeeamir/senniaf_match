# Sprint 7 — reglas de seguridad e integridad

Resumen vigente del código. Los informes detallados y evidencias de trabajo se conservan en el archivo académico privado. Esta publicación no ejecutó pruebas de seguridad ni afirma una nueva certificación.

| Hallazgo | Control conservado |
| --- | --- |
| F01 | La ruta resuelta pasa por el filtro; identidad y rol proceden de PostgreSQL, no de cabeceras arbitrarias. |
| F02 | Las decisiones bloquean el expediente y comprueban aceptaciones incompatibles dentro de una transacción. |
| F03 | Los ajustes de pesos bloquean la configuración y comprueban la versión esperada. |
| F04 | Nuevas decisiones conservan el contexto histórico de puntuación; ajustes conservan valores y justificación. No se recalculan decisiones antiguas con pesos actuales. |
| F05 | El psicólogo autor y vinculado crea una revisión del informe vigente. El motivo adicional dejó de ser obligatorio por decisión posterior; sus valores históricos se conservan. |
| F06 | La exclusión de recomendaciones conserva el seguimiento autorizado y no concede reactivación automática. |
| F07 | El Comité consulta solicitudes pendientes, incluidos casos excluidos. |
| F08 | El creador autenticado queda como responsable social al registrar. No hay reasignación ni psicólogo automático. La referencia institucional dejó de ser obligatoria por decisión posterior. |
| F09 | El detalle conserva el contexto de navegación y permite consultar el expediente aunque ya no aparezca en recomendaciones. |
| F10 | Acciones por rol y especialidad comprobadas en servidor; el atributo visual `hidden` no concede permisos. |
| F11 | Las explicaciones distinguen compatibilidad real de criterios con peso cero. |
| F12 | Pesos enteros, cinco criterios, rango 0–100, suma 100 y justificación antes de confirmar. |
| F13 | Completitud, participación en Match, solicitudes, versiones clínicas y decisiones históricas son estados diferentes. |

## Mantenimiento

No introducir secretos ni datos clínicos en logs. Los datos simulados y sus respaldos permanecen en PostgreSQL y almacenamiento privado. La auditoría utiliza `text` para conservar diferencias completas; el contexto psicológico se guarda por versión en la columna opcional `contexto_evaluacion`. Cambiar el esquema exige revisión y respaldo, no un arranque con actualización automática.

Los pendientes actuales y el alcance de las comprobaciones están en [el acta](ACTA_CIERRE_SPRINT7.md). Las pruebas históricas requieren revisión del equipo frente a los contratos actuales y no se consideran aprobadas sin ejecutarlas.
