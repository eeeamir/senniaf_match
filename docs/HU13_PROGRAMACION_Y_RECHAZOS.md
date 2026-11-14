# HU13 — programación de pesos y combinaciones rechazadas

## Estado y alcance

Ampliación implementada sobre `a4afe97` en la rama `mejora/hu13-programacion-rechazos`. Probada localmente con PostgreSQL 16.15 y Java 17. No está fusionada con `main` ni desplegada en Azure. La versión pública anterior continúa sin esta migración. Los informes anteriores del Sprint 8 documentan aquella entrega, no estos refuerzos.

## Funcionalidades nuevas

- `PUT /api/scoring/pesos` conserva el cuerpo de los cinco enteros, justificación y `versionEsperada`, pero ahora crea una programación y devuelve su información. No modifica inmediatamente los pesos vigentes.
- `GET /api/scoring/pesos/programacion`, solo Comité, informa la programación pendiente, la versión vigente y la primera activación válida calculada por el servidor.
- La fecha es la primera ocurrencia de las **07:00 de America/Panama** que no sea anterior a la programación más **24 horas exactas**. No existe un campo editable de fecha/hora. El 14 de noviembre a las 09:00 corresponde al 16 a las 07:00; a las 07:00 exactas corresponde al día siguiente a las 07:00.
- Una tarea cada 30 segundos comprueba vencimientos. Las lecturas de pesos, historial y análisis, y las decisiones, también resuelven una activación vencida. El instante previsto sigue siendo las 07:00; se registra separadamente el instante efectivo, que puede ser posterior si el servicio estaba detenido. No se necesita conservar un temporizador en memoria.
- La fila canónica de `pesos_scoring` se bloquea durante programación, activación y decisiones. Un índice único parcial permite una sola programación pendiente. Dos solicitudes simultáneas no pueden sobrescribirse; una segunda programación incompatible recibe un mensaje natural y 409.
- La activación actualiza pesos, estado y auditoría dentro de una transacción; volver a ejecutarla no incrementa otra vez la versión ni duplica la auditoría.
- `POST /api/scoring/casos/{codigo}/decisiones` exige `versionPesos`. El servidor comprueba la versión antes de registrar una decisión. Una versión antigua o ausente no crea decisión ni vinculación. Se aplica a aceptar, rechazar y solicitar reevaluación. No se crea una evaluación en curso ni se congelan pesos al abrir una pantalla.
- Las decisiones anteriores mantienen puntaje, contexto completo, versión, valores, motivos, fecha y responsable. La función de cálculo `candidato` es idéntica a la de `a4afe97`; se modifica la selección de candidatos, no las fórmulas.

## Rechazos por combinación

Un rechazo del Comité (`DESCARTADA`, conservado como valor interno del contrato) se almacena en la decisión existente y no crea vinculación. Los rankings consultan esas decisiones junto con las exclusiones familiares persistidas; se cargan los pares una sola vez por ranking, sin una consulta por candidato. La API vuelve a comprobar la exclusión al decidir.

La respuesta familiar negativa a una propuesta pendiente se registra como `RECHAZADA_POR_FAMILIA`, excluye ese par y levanta la reserva de la propuesta cuando no hay una interrupción o cierre institucional representados. No crea un cierre legal ni una interrupción, no cambia idoneidad, completitud o puntuación y no impide otras combinaciones elegibles. El resumen familiar deja de presentar esa propuesta como vigente.

La migración incorpora los rechazos anteriores, incluso una respuesta negativa cuyo historial contiene JSON con espacios y cuya vinculación terminó cerrada. No reconstruye rechazos inexistentes ni sobrescribe estados o puntuaciones históricos. Solo levanta reservas de antiguas propuestas rechazadas sin actuaciones de convivencia/cierre. Si una fila anterior mantiene el estado legado de rechazo pendiente de cierre pero ya no tiene reserva ni actuaciones, la presentación lo trata como rechazo del portal y no vuelve a ofrecer un cierre artificial; el estado original almacenado se conserva.

Los pesos nuevos no reactivan esos pares. La exclusión afecta únicamente a recomendaciones automáticas y nuevas propuestas del par; no anula retroactivamente decisiones o actuaciones institucionales. No se implementa reconsideración excepcional, cancelación de programaciones ni sustitución: no había un flujo autorizado de esas operaciones y una programación pendiente no puede reemplazarse.

## Auditoría y permisos

Se reutilizan Comité, sesión validada en servidor, protección de cabeceras, enteros 0–100, suma 100, justificación de 10–300 caracteres, transacciones, bloqueo y versionado. Las consultas y cambios de criterios siguen restringidos al Comité.

`programaciones_pesos` conserva responsable, valores anteriores/nuevos, justificación, fecha de programación, activación prevista/efectiva, versiones y estado. La cuenta técnica puede insertar/consultar y actualizar solo estado y activación efectiva; no puede sobrescribir valores, motivo o responsable originales. El historial existente `auditoria_casos`, caso `CONFIG_MATCH`, recibe `PESOS_PROGRAMADOS` y `PESOS_ACTIVADOS`. La activación automática identifica como actor a `SISTEMA` y conserva al responsable de la programación en el detalle. Los eventos anteriores permanecen intactos.

`combinaciones_excluidas` conserva pares y procedencia de rechazos familiares o históricos; los nuevos rechazos del Comité se consultan directamente desde `decisiones_comite`. No hay un endpoint para borrar o reconsiderar exclusiones. La cuenta técnica no recibe permisos de borrado de estas tablas.

## Interfaz

En Criterios se muestran pesos vigentes, formulario con iconos propios, total, justificación, confirmación integrada y fecha de servidor. La programación pendiente tiene un aviso contextual con cambios, motivo y fecha; también aparece en Compatibilidad. El historial distingue el acto de programar y el de aplicar, sin editar los eventos previos.

Un rechazo por versión antigua muestra la explicación y el botón **Actualizar compatibilidad**, que conserva el expediente y familia de la URL. Las acciones se deshabilitan durante esa actualización. El rechazo previo tiene una explicación propia y un enlace a antecedentes. Se mantiene la privacidad familiar y se reutilizan menú, estilos, iconos y componentes; no se añade un módulo de notificaciones ni una biblioteca visual.

## Migración e integración futura

Aplicar `015-pesos-programados-y-exclusiones.sql` **después de 014**, con propietario del esquema, backend detenido, respaldo reciente verificado y confirmación `PROGRAMAR_PESOS_SIN_BORRAR` y `base_autorizada`. No ejecutar el esquema inicial ni sustituir una base existente por una de pruebas. La ampliación añade dos tablas, restricciones e índices, y ajusta las reservas temporales elegibles descritas arriba; no borra registros.

El actualizador existente y `verificar-compatibilidad.py` se amplían juntos. Se comprueban las dos tablas de 015 y se impide publicar/recuperar un JAR que las ignore, además de los controles de 012/013/014. No se crea otro procedimiento de despliegue. Antes de activar, verificar `archivo.jar t t t t` con el auxiliar y confirmar el esquema completo con `ddl-auto=validate`. La recuperación debe usar código compatible; no restaurar ni eliminar datos automáticamente.

## Pruebas y evidencias

**289 Java aprobadas: 235 unitarias y 54 HTTP de integración, sin fallos, errores ni omisiones**, mediante `mvn -B verify -Pe2e` con Java 17 y PostgreSQL 16.15. Diez pruebas Python del actualizador aprobadas; sintaxis de JavaScript y shell verificada.

- `ProgramacionFechaTest`: ejemplo solicitado, 24 horas exactas, un instante posterior al límite y cambio de año.
- `HU13PostgresIT`: programación sin cambio inmediato; hora fija y anticipación; auditoría y datos históricos; 18 cuerpos inválidos; pendientes incompatibles/versiones antiguas; cuatro roles/cabeceras falsas; programación simultánea; activación retrasada concurrente; decisiones con análisis antiguo; tarea de activación sin consultas de interfaz y sin duplicados.
- `CombinacionesRechazadasPostgresIT`: rechazo del Comité, respuesta familiar, nuevas combinaciones de ambos expedientes, duplicados, API sin versión, rechazo histórico después de activar pesos y presentación del rechazo legado sin inventar un cierre.
- Se conservaron y ejecutaron HU19, HU11, HU12, HU14, cierre institucional y flujo anterior. Sus fixtures envían ahora la versión del análisis. Las aserciones que exigían retener una reserva tras rechazo familiar se cambiaron por la nueva regla explícita, manteniendo las comprobaciones de privacidad e idoneidad.
- 015 se aplicó desde un esquema previo en otra base aislada: rechazo histórico con JSON espaciado incluido, estado Cerrada y puntuación 65 conservados y reserva institucional mantenida.
- Chromium: programación real desde formulario, aviso de criterios, pesos aún vigentes, protección por versión y botón de actualización, rechazo del Comité y familiar reales, recarga, privacidad y móvil 390 px, sin errores JavaScript ni desbordamientos. Para representar una pantalla antigua se redujo su versión en memoria del navegador; la respuesta 409 fue real del servidor. La activación horaria se comprueba con un reloj controlado únicamente en las pruebas, no con un endpoint o reloj editable en producción.
- SQL directo comprobó que la cuenta técnica no puede sobrescribir valores ni borrar programaciones/exclusiones, y que PostgreSQL rechaza una activación sin fecha efectiva.
- Reinicio real del JAR local: programación pendiente, pesos y exclusiones idénticos; rankings y API siguen rechazando ambos pares.

Durante el desarrollo se detectó y corrigió el orden de actualización de estado/fecha al responder negativamente: una consulta intermedia provocaba un guardado prematuro rechazado por PostgreSQL. También se corrigió una aserción de historial para buscar la programación por su identificador, sin depender de fechas de fixtures de ejecuciones anteriores. Se corrigió el ancho del formulario al reutilizar sus tres columnas e iconos. No se desactivaron restricciones ni se declararon esos intentos fallidos como aprobados; las cifras anteriores corresponden a la regresión final.

Logs, capturas y snapshots sintéticos en `target/refuerzos-hu13/` (excluido de Git): `candidato.log`, `actualizador-candidato.log`, `migracion-final.log`, `browser-candidato.log`, `persistencia-candidato.log` y `*.png`. No se usaron registros, credenciales ni infraestructura de Azure para estas pruebas.
