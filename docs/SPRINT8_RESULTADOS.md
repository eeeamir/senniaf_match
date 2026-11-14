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

## Matriz de aceptación

| Historia | Criterios | Estado | Evidencia |
| --- | --- | --- | --- |
| HU19 | 1–8 | Comprobados localmente | Suite inicial 252 Java; `HU19PostgresIT`; auditoría y navegador previos conservados. |
| HU11 | 1–3, 5–10 | Comprobados localmente | `HU11PostgresIT`, 6 casos HTTP y carreras; navegador TS/Comité/familia; permisos SQL. |
| HU11 | 4: reinicio | Comprobado localmente | Snapshot íntegro y recuperación HTTP de la revisión tras reinicio. |
| HU12 | 1–10 | Pendientes de implementación/validación | Se reutilizará `continuidadCultural`, ya persistente. |
| HU14 | 1–10 | Pendientes de ampliación/validación | Umbral 3 y contador completado ya existentes. |
| HU13 | 1–11 | Pendientes de verificación específica | Permisos, bloqueo, versión y auditoría ya existentes. |
