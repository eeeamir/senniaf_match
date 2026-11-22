# Sprint 8 — matriz final de aceptación local

La publicación y ampliación posterior del cierre institucional se documentan en [integración y despliegue](SPRINT8_INTEGRACION_DESPLIEGUE.md). Esta matriz conserva la evidencia y los límites de los seis commits locales originales.

Validación: 13–14 de noviembre de 2026. Entorno sintético aislado: Java 17, PostgreSQL 16.15, Chromium. **C = comprobado localmente**. Ninguna fila certifica publicación o funcionamiento de estas historias en Azure. Allí sigue pendiente la integración autorizada; la ruta pública de Vinculaciones devuelve 404.

Evidencias reproducibles versionadas:

- [HU19PostgresIT](../src/test/java/com/senniaf/match/HU19PostgresIT.java), [servicio](../src/test/java/com/senniaf/match/service/VinculacionServiceTest.java) y [decisiones](../src/test/java/com/senniaf/match/service/VinculacionDecisionTest.java).
- [HU11PostgresIT](../src/test/java/com/senniaf/match/HU11PostgresIT.java).
- [HU12PostgresIT](../src/test/java/com/senniaf/match/HU12PostgresIT.java).
- [HU14PostgresIT](../src/test/java/com/senniaf/match/HU14PostgresIT.java) y [contador familiar](../src/test/java/com/senniaf/match/service/PerfilFamiliaServiceTest.java).
- [HU13PostgresIT](../src/test/java/com/senniaf/match/HU13PostgresIT.java), más las suites ISO existentes de funcionalidad, fiabilidad y seguridad.

Los recorridos de navegador, capturas, snapshots y logs de esta sesión están en `target/verificacion-hu19/`, excluido de Git para no publicar datos de prueba ni binarios. Los nombres de evidencia siguientes corresponden a esos archivos; no son registros de producción.

| Historia / criterio | Comprobación | Estado | Evidencia |
| --- | --- | --- | --- |
| HU19 / 1 | Consulta de vinculaciones autorizadas | C | HTTP de Comité, trabajo social y psicología en HU19PostgresIT. |
| HU19 / 2 | Permisos en servidor | C | Roles, sesión requerida y cabeceras falsas rechazadas. |
| HU19 / 3 | Acceso ajeno denegado | C | Otra familia y profesional sin asignación reciben 403. |
| HU19 / 4 | Operaciones en PostgreSQL | C | Creación/respuesta y consultas con repositorios reales; ocho casos HTTP. |
| HU19 / 5 | Historial y trazabilidad | C | Respuesta única, actor de sesión e historial de la vinculación. |
| HU19 / 6 | Interfaz y rutas locales | C | Validación inicial conservada; HU11 reutiliza las rutas reales de Vinculaciones en Chromium. |
| HU19 / 7 | Decisiones/evaluaciones conservadas | C | Conteos de expedientes, decisión anterior y concurrencia sin duplicados. |
| HU19 / 8 | Sin adopción automática | C | Respuesta familiar conserva reserva; ausencia de cierre automático en HU19/HU11. |
| HU11 / 1 | Profesional autorizado | C | Social/psicología asignados; otros roles y otro profesional rechazados. |
| HU11 / 2 | Vinculación válida | C | Requiere reserva y respuesta aceptada; pendiente/rechazada no admitidas; referencia institucional obligatoria. |
| HU11 / 3 | Justificación conservada | C | Validación de longitud, guardado y lectura tras revisión. |
| HU11 / 4 | Persistencia tras reinicio | C | `HU11-persistencia-antes.json` y recuperación HTTP posterior. |
| HU11 / 5 | Consulta del Comité | C | HTTP y recorrido `hu11-browser.log`, capturas de revisión. |
| HU11 / 6 | Responsable y fecha | C | Informe/revisión con actor de sesión, fechas y auditoría persistida. |
| HU11 / 7 | Antecedentes sin sobrescritura | C | Revisión única, carreras 200/409, informe original idéntico; permisos SQL deniegan sobrescribirlo. |
| HU11 / 8 | Sin sanción automática | C | Puntaje, reserva y estado infantil conservados tras informe/revisión. |
| HU11 / 9 | Accesos/cambios ajenos rechazados | C | HTTP 403 por rol/asignación; portal familiar sin justificación ni referencia interna. |
| HU11 / 10 | Diferencia respecto al cierre por adopción | C | La interrupción no activa baja ni cierre; comprobación explícita en HU11PostgresIT. |
| HU12 / 1 | Registro propio | C | Alta familiar con el campo existente y guardado autenticado. |
| HU12 / 2 | Consulta y actualización | C | Dos actualizaciones HTTP y dos guardados reales desde el formulario. |
| HU12 / 3 | PostgreSQL | C | Repositorio real y recuperación por HTTP tras reiniciar el backend. |
| HU12 / 4 | Lectura del Comité | C | Endpoint de expediente y `HU12-comite.png`. |
| HU12 / 5 | Claridad y validación | C | Ayuda en español, máximo 2000, error 400; revisión móvil y de escritorio. |
| HU12 / 6 | Privacidad | C | Roles y otra familia no acceden al expediente ni cambian el perfil ajeno. |
| HU12 / 7 | Sin exclusión automática | C | Perfil completo se mantiene; mapa de candidato idéntico. |
| HU12 / 8 | Sin puntuación étnica | C | El texto no cambia ningún resultado del candidato; no hay campo de pertenencia étnica. |
| HU12 / 9 | Algoritmo conservado | C | ScoringService completo idéntico a 7b5b3fd. |
| HU12 / 10 | Historia conservada | C | Auditoría antes/después; omisión/null preservan el texto; vaciado explícito queda auditado. |
| HU14 / 1 | Solo completadas | C | Solicitud no aumenta; corrección completa aumenta una vez; casos 0,1,2,3,4. |
| HU14 / 2 | Incompletas/canceladas excluidas | C | Entrevista incompleta rechazada sin incremento; antecedente cancelado sintético no acredita completitud. No se inventa un flujo de cancelación. |
| HU14 / 3 | Umbral confirmado | C | Tres, coherente con código y petición vigente; dos sin alerta, tres y cuatro con alerta. |
| HU14 / 4 | Presentación adecuada | C | Menú del Comité, aviso informativo, `HU14-comite.png` y móvil. |
| HU14 / 5 | Antecedentes autorizados | C | Solicitudes/correcciones con actor/fecha, enlace al expediente; reinicio sin cambios. |
| HU14 / 6 | Sin duplicados | C | Consultas repetidas producen un aviso por familia; doble envío completa una sola reevaluación. |
| HU14 / 7 | Permisos de servidor | C | Familia, social y psicología 403; sin sesión 401. |
| HU14 / 8 | Sin desactivación automática | C | Perfil completo conservado; la consulta es de solo lectura. |
| HU14 / 9 | Pesos/puntuación sin cambios | C | Puntaje idéntico tras cada corrección; ScoringService íntegro, sin edición de pesos en esta historia. |
| HU14 / 10 | Sin sanción automática | C | Consulta derivada, sin nuevas escrituras de alertas ni cambio de idoneidad. |
| HU13 / 1 | Solo Comité modifica | C | Otros tres roles reciben 403; Comité confirma cambio válido. |
| HU13 / 2 | Autorización del servidor | C | Cabeceras falsas con sesión de otro rol rechazadas; sin sesión 401. |
| HU13 / 3 | Validaciones existentes | C | 18 cuerpos inválidos: límites, suma, tipos, claves, justificación y versión; sin escrituras. |
| HU13 / 4 | Justificación registrada | C | Motivo obligatorio, visible en historial y confirmado desde el navegador. |
| HU13 / 5 | Valores anteriores/nuevos | C | Comparación exacta de los cinco factores en auditoría. |
| HU13 / 6 | Responsable/fecha | C | Actor HU19-CM de sesión, rol COMITE y fecha; consulta en pantalla. |
| HU13 / 7 | Versiones consultables | C | GET historial y lectura tras reiniciar: `hu13-persistencia.log`. |
| HU13 / 8 | Versión desactualizada rechazada | C | Carrera 200/409 y segundo envío 409 sin auditoría adicional; aviso real en Chromium. |
| HU13 / 9 | Resultados históricos intactos | C | Contexto, puntaje y versión de una decisión anterior permanecen idénticos tras ajustar los pesos. |
| HU13 / 10 | Cálculo conservado | C | Suites ISO existentes y ScoringService byte a byte idéntico a 7b5b3fd. |
| HU13 / 11 | Sin factores/reglas nuevos | C | Cinco claves exactas; factor extra rechazado; sin nuevas fórmulas. |

## Límites y publicación pendiente

No quedan criterios funcionales locales abiertos en esta matriz. El recorrido vacío de HU14 se comprobó con una respuesta simulada de la API; su umbral y permisos se probaron con PostgreSQL real. Los contadores históricos sin detalle se conservan como tales, sin reconstruir fechas o actuaciones. La referencia de HU11 documenta la convivencia indicada por el profesional; aceptar una propuesta en el portal no la acredita por sí solo.

**Azure: no comprobado para la nueva versión y pendiente de autorización.** Se deben revisar y publicar los commits, respaldar, aplicar manualmente 012 y 013 con el backend anterior detenido, instalar el actualizador y auxiliar compatibles, desplegar el JAR aprobado y verificar roles, HTTPS y persistencia. El plan se describe en [operación de Azure](../deploy/azure/README.md). No se ejecutó ninguno de esos pasos sobre el entorno persistente durante este trabajo.
