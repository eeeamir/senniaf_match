# HU19 — Gestión básica de vinculaciones

Estado de integración y bloqueo previo a HU11: [verificación integral](VERIFICACION_INTEGRAL_HU19.md).

## Revisión y decisiones de implementación

La aplicación utiliza Spring Boot, JPA/PostgreSQL y páginas HTML/JavaScript sin framework. Las decisiones se registran en `decisiones_comite`; las propuestas son un ranking calculado al consultar, sin una entidad de propuesta persistida. La aceptación vuelve a consultar y bloquear ambos expedientes, valida su elegibilidad actual y verifica sus reservas dentro de la misma transacción. No se modifican `candidato`, los pesos ni las puntuaciones.

Se reutilizan `SesionFiltro` (identidad de sesión, sustitución de cabeceras declaradas por el cliente), los profesionales asignados al expediente infantil, `auditoria_casos`, los bloqueos pesimistas de los repositorios, el tratamiento global de errores y los componentes e iconos SVG de `Profesionales`.

La decisión del Comité crea una vinculación con los expedientes existentes. La vinculación y sus reservas son conceptos separados del estado general de los expedientes. Rechazar antes de asignar conserva solo la decisión del Match; solicitar reevaluación conserva el flujo anterior.

## Funcionalidad y permisos

- Comité: aceptar crea la vinculación y reserva ambos participantes; consultar listado, filtros, detalle e historial.
- Trabajo social y psicología: consultar solo las vinculaciones cuyo expediente infantil tengan asignado en su especialidad. No pueden responder por la familia ni decidir por el Comité.
- Familia: «Mi vinculación» reutiliza la pantalla y URL anteriores (`match.html?vista=propuesta`), con aviso de propuesta pendiente y confirmación de respuesta. Solo consulta sus vinculaciones y solo responde una vez a una vinculación pendiente y vigente.
- Respuestas válidas: `ACEPTAR` o `RECHAZAR`; el servidor decide el estado, responsable y fecha. No acepta esos campos desde el cliente.
- El rechazo conserva la reserva, no modifica idoneidad ni puntuaciones y no representa una interrupción preadoptiva.
- La navegación institucional informa cuántas vinculaciones tienen respuesta, con actualización cada 15 segundos. El listado y detalle identifican la actuación pendiente. Es una comunicación dentro del portal; no se añadieron correos ni un sistema independiente de notificaciones.
- El historial reutiliza la auditoría infantil. Cada movimiento guarda referencia a la vinculación, responsable, fecha y estado resultante; el detalle incorpora también la decisión original del Comité. El portal familiar conserva su historial y añade la respuesta familiar sin exponer la justificación interna.

Supuesto conservador: el proyecto no documenta qué datos infantiles se autorizan para esta etapa. El portal familiar sigue sin exponer datos infantiles, tampoco códigos infantiles, informes profesionales, puntajes ni motivos internos del Comité. No hay un permiso infantil concedido por HU19 que pueda sobrevivir al rechazo. Los endpoints de expedientes infantiles continúan denegados al rol familia; modificar identificadores de vinculación no amplía su acceso. Una política futura de acceso deberá comprobar en el servidor el estado vigente de la vinculación antes de entregar cualquier dato restringido.

Las pantallas describen únicamente actuaciones registradas. Ni la decisión del Comité ni la respuesta familiar acreditan acogimiento preadoptivo o formalización de adopción. No se incorporan nuevas competencias legales, reglas legales presuntas ni funciones del Sprint 9.

## Cambios por archivo

| Archivo | Motivo |
| --- | --- |
| `model/Vinculacion.java` | Relación con decisión y expedientes, estado, reserva y respuesta versionada. |
| `repository/VinculacionRepository.java` | Consultas de reservas, consultas propias y bloqueo de respuesta. |
| `service/VinculacionService.java` | Creación, respuesta, autorización por asignación, DTO explícito y auditoría. |
| `controller/VinculacionController.java` | API institucional y API familiar separadas por permisos. |
| `service/ScoringService.java` | Crear vinculación al aceptar y excluir participantes con reserva vigente; cálculo intacto. |
| `service/PerfilFamiliaService.java` | Aclarar que la aceptación previa corresponde al Comité. |
| `static/familia.js`, `static/match.html` | Integrar Mi vinculación y sus respuestas en el panel existente. |
| `static/vinculaciones.html`, `static/vinculaciones.js` | Listado y detalle institucional; representación y confirmación familiar. |
| `static/profesionales.js`, `static/profesionales.css` | Navegación, aviso institucional, iconos existentes y adaptación de filtros/estados. |
| `deploy/azure/sql/012-vinculaciones.sql` | Esquema y conversión conservadora de aceptaciones anteriores. |
| `VinculacionServiceTest`, `VinculacionDecisionTest`, `VinculacionControllerTest` | 21 pruebas de reglas, permisos, historial y conexión con decisiones, además de la suite de integración HTTP/PostgreSQL. |

Las rutas de Java parten de `src/main/java/com/senniaf/match`; las de recursos de `src/main/resources`; las de pruebas de `src/test/java/com/senniaf/match`.

## Base de datos y despliegue

Ejecutar `deploy/azure/sql/012-vinculaciones.sql` con el procedimiento habitual de respaldo y migración, antes de iniciar la nueva versión con `ddl-auto=validate`. Se aplicó exclusivamente a las bases temporales de validación; no a bases existentes del usuario ni remotas.

La migración exige confirmar el destino con `base_autorizada` y `confirmacion=CREAR_VINCULACIONES_SIN_BORRAR`, y crea `vinculaciones`, con claves foráneas a los expedientes y decisión existentes, unicidad de decisión y dos índices únicos parciales que impiden reservas simultáneas de cada participante. Cuando existe la cuenta técnica `senniaf_app`, le concede acceso a la tabla nueva y su secuencia sin permisos de borrado; los permisos de los historiales permanecen intactos. Los bloqueos de los expedientes serializan aceptaciones concurrentes; el bloqueo de la vinculación serializa respuestas y su control de etapa impide cambiarlas después. La versión JPA añade protección frente a escrituras obsoletas.

Las aceptaciones históricas se incorporan como pendientes de respuesta, conservando la fecha y responsable de su decisión original, sin inventar respuestas ni eventos anteriores. Si existen aceptaciones contradictorias, referencias ausentes o fechas ausentes, la transacción se aborta para revisión institucional. No se borran ni corrigen decisiones automáticamente. El despliegue debe detener la versión anterior antes de aplicar la migración y arrancar la nueva, para evitar decisiones escritas por código anterior durante la conversión.

## Verificación

La revisión final reemplaza las limitaciones de la verificación inicial: ahora se dispone de PostgreSQL y Chromium temporales. La migración se aplicó y validó en bases aisladas, se verificó el flujo en navegador y pasaron 242 pruebas conjuntas (234 unitarias/controladores y 8 HTTP/PostgreSQL), tras corregir los contratos y mocks históricos. No se migró una base existente del usuario ni Azure.

Consultar [el informe de validación final](HU19_VALIDACION_FINAL.md) para permisos comprobados, correcciones de pruebas, recorridos reales, versiones utilizadas, comandos y pendientes de despliegue.

## Preparación para HU11

El estado de la vinculación, la reserva, la referencia a la decisión y el historial permanecen independientes de los expedientes. HU11 podrá incorporar actuaciones institucionales referenciadas, estados de interrupción/cierre y liberación expresa de la reserva tras verificar elegibilidad, ampliando el enum y la restricción SQL. El Match ya consulta reservas vigentes en lugar de tratar cada aceptación histórica como una exclusión permanente.

No hay botones funcionales ni endpoints para interrupción/cierre. HU11 deberá definir permisos, respaldo y actuaciones confirmadas; también actualizar la comunicación histórica de disponibilidad del DTO familiar cuando incorpore liberaciones. No se formalizan adopciones desde el prototipo.
