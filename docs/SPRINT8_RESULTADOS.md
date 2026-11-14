# Sprint 8 — implementación y evidencias locales

Plan vigente: HU19 (5 SP), HU11 (5), HU12 (2), HU14 (2), HU13 (2): **16 SP**. La discrepancia suministrada de 12 SP anteriores frente a 11 SP actuales para HU11–HU14 se registra sin cambiar estimaciones. No se encontró otra definición operativa contradictoria del umbral: el código y la petición vigente indican tres reevaluaciones completadas.

La petición más reciente permite avanzar entre historias validadas localmente sin esperar al despliegue de Azure. Esto reemplaza el bloqueo operativo para desarrollo consignado en la auditoría anterior; no autoriza publicación ni migraciones reales. Rama: `sprint7-desarrollo`. No se realizan push ni operaciones de Azure.

## HU19 — Vinculaciones

**Estado:** completada localmente, revalidada al comenzar. **Git:** `7b5b3fd`.

**Qué se implementó y cómo funciona:** se conserva la propuesta aceptada por el Comité, su reserva, la respuesta familiar única, el listado institucional asignado y el historial. Se reutilizan sus rutas y componentes para HU11.

**Qué se comprobó:** nuevamente 234 pruebas unitarias + 18 de integración = **252 Java** y seis del actualizador, antes de modificar las historias siguientes. Raíz pública HTTP 200 y Vinculaciones HTTP 404, consultadas sin escrituras. La base persistente no se modificó.

**Archivos:** servicio, controlador, modelo y repositorio de Vinculaciones; componentes institucional/familiar y migración 012 ya existentes. **Base:** requiere 012. **Azure:** pendiente de integración autorizada.

## HU11 — Interrupción de convivencia

**Estado:** completada localmente.

**Qué se implementó:** el profesional asignado registra fecha, referencia de la actuación que respalda la convivencia y justificación. El Comité consulta el antecedente y registra una revisión justificada una sola vez. La justificación inicial se conserva; no se modifica el estado general del niño/familia, la idoneidad, el puntaje ni la reserva. El aviso y la consulta usan Vinculaciones y su auditoría.

**Cómo funciona:** en el detalle de una vinculación con respuesta familiar aceptada y reserva vigente, trabajo social/psicología asignados abren «Registrar interrupción de convivencia», completan y confirman. La aceptación del portal por sí sola no acredita convivencia; se exige la referencia institucional. El Comité ve el aviso pendiente y «Registrar revisión». La familia ve un mensaje general, sin motivos profesionales ni referencias internas.

**Alcance conservador:** una interrupción por vinculación, sin edición del informe original ni sustitución de la revisión. No se presupone reanudación de convivencia. La reserva permanece hasta la actuación institucional correspondiente. El alcance vigente describe registro y revisión, sin definir un formulario de cierre o una decisión de liberación; esas acciones no se simulan. No hay reportes periódicos ni incidencias generales del Sprint 9.

**Archivos principales:** `VinculacionService`, `VinculacionController`, `InterrupcionConvivencia` y su repositorio, DTO de registro/revisión, `vinculaciones.js`, componentes compartidos, pruebas PostgreSQL y migración 013. La fixture común de pruebas se extrajo para conservar y reutilizar las ocho pruebas originales de integración de HU19.

**Base:** 013 aditiva después de 012; única tabla con FK a la vinculación, unicidad y revisión coherente. No migra ni reclasifica registros existentes. La cuenta técnica puede insertar/consultar y actualizar solo columnas de revisión y versión; no puede sobrescribir el informe original ni borrar. El actualizador preparado incorpora el control de compatibilidad con 013, sin instalarlo en Azure.

**Qué se comprobó:** 29 pruebas enfocadas (6 nuevas HTTP de HU11, 8 de HU19 y 15 de servicio/controlador) aprobadas. Chromium: registro profesional, confirmación, consulta tras recarga, revisión del Comité, privacidad familiar y móvil 390 px, sin errores JavaScript. SQL directo con la cuenta técnica rechazó sobrescribir la justificación original. Evidencias sintéticas en `target/verificacion-hu19/hu11-*` y capturas `HU11-*.png`.

**Persistencia:** interrupción y revisión idénticas en PostgreSQL y recuperables por HTTP después de reiniciar el backend. **Git:** commit local de HU11, consultable en el historial de la rama. **Azure:** no desplegada. Migraciones reales pendientes de autorización.

## HU12 — Continuidad cultural y lingüística

**Estado:** completada localmente. **Qué se implementó:** sección opcional «Apoyos culturales y comunitarios» en la entrevista familiar, consulta y edición propias y presentación al Comité dentro del expediente existente. Un único texto de hasta 2000 caracteres reutiliza `continuidadCultural`; no solicita pertenencia étnica ni incorpora un factor de scoring.

**Cómo funciona:** la familia abre su entrevista, describe cómo acompañaría costumbres, idioma y comunidad, y guarda. Puede volver a consultar o editar. El Comité ve el mismo contenido en el expediente familiar autorizado. Los clientes que omiten el campo o envían `null` conservan el texto histórico; un vaciado explícito mediante texto vacío queda auditado con su valor anterior.

**Qué se comprobó:** 19 pruebas aprobadas: 5 HTTP nuevas y 14 de servicio/controlador familiar. Guardado inicial, dos actualizaciones, lectura del Comité, intentos de acceso/cambio por otros roles y otra familia, exceso de longitud con mensaje español, conservación por omisión/null, historial y puntuación idéntica. Los apoyos escritos por las pruebas se recuperaron por HTTP después de reiniciar el backend. Chromium comprobó dos guardados reales, recarga y consulta familiar/Comité; se corrigió la distribución móvil para que el texto ocupe el ancho de la tarjeta y el recorrido final pasó de nuevo sin errores JavaScript ni desbordamientos.

**Archivos principales:** `PerfilFamiliaRequest`, `PerfilFamiliaService`, `perfil.html`, `perfiles.html`, `familia.js`, estilo compartido y `HU12PostgresIT`. **Base:** sin migración nueva; la columna ya existe. **Git:** commit local de HU12, consultable en el historial de la rama. **Azure:** no desplegada. No se alteraron fórmulas, pesos, criterios de completitud ni datos de expedientes infantiles.

## HU14 — Alertas por reevaluaciones

**Estado:** completada localmente. **Qué se implementó:** consulta del Comité «Reevaluaciones», derivada del mismo contador y auditoría existentes, con un aviso por familia al alcanzar tres completadas. Se muestran solicitudes y correcciones con responsable/fecha, y se distingue una solicitud pendiente del total completado. No se crea otro registro persistente de alertas.

**Cómo funciona:** el Comité abre «Reevaluaciones» desde su navegación, consulta los antecedentes desplegables y puede abrir el expediente familiar. Los contadores históricos sin actuaciones detalladas se identifican expresamente sin inventar fechas. El aviso es informativo y no cambia puntuaciones, pesos, idoneidad ni activación del perfil.

**Integridad:** las actualizaciones propias bloquean la fila familiar durante la transacción. Dos envíos concurrentes de una corrección pendiente solo incrementan una vez el contador y conservan la auditoría. Solicitudes sin entrevista completada y antecedentes cancelados no generan incrementos. No se introduce una operación de cancelación inexistente.

**Qué se comprobó:** 19 pruebas aprobadas: cinco HTTP nuevas de HU14, cinco de HU12 y nueve de servicios familiares/reevaluación. Se cubren cero, una, dos, tres y cuatro completadas; solicitud pendiente, entrevista incompleta, antecedente cancelado sintético, contador histórico sin fechas, repetición de consultas, roles y doble envío concurrente. Chromium comprobó menú, consulta real, antecedentes, recarga y móvil 390 px sin errores ni desbordamientos. El estado vacío se revisó con una respuesta simulada de la API; el umbral y los permisos se verificaron contra PostgreSQL. Evidencias `target/verificacion-hu19/hu14-*` y `HU14-*.png`. El contador y los antecedentes se recuperaron idénticos por HTTP tras reiniciar el backend local. **Git:** commit local de HU14.

**Archivos principales:** `PerfilFamiliaService`, `PerfilFamiliaRepository`, `ScoringController`, `comite.js`, `profesionales.js`, pruebas familiares ajustadas al bloqueo y `HU14PostgresIT`. **Base:** sin migración adicional. **Azure:** pendiente, sin cambios en el entorno persistente.

## Matriz de aceptación

| Historia | Criterios | Estado | Evidencia |
| --- | --- | --- | --- |
| HU19 | 1–8 | Comprobados localmente | Suite inicial 252 Java; `HU19PostgresIT`; auditoría y navegador previos conservados. |
| HU11 | 1–3, 5–10 | Comprobados localmente | `HU11PostgresIT`, 6 casos HTTP y carreras; navegador TS/Comité/familia; permisos SQL. |
| HU11 | 4: reinicio | Comprobado localmente | Snapshot íntegro y recuperación HTTP de la revisión tras reinicio. |
| HU12 | 1–10 | Comprobados localmente | `HU12PostgresIT`, 5 casos HTTP; 14 pruebas familiares existentes; recuperación tras reinicio; Chromium. |
| HU14 | 1–10 | Comprobados localmente | `HU14PostgresIT`, cinco casos HTTP y concurrencia; contador/auditoría originales; Chromium y vista móvil. |
| HU13 | 1–11 | Pendientes de verificación específica | Permisos, bloqueo, versión y auditoría ya existentes. |
