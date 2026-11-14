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

**Persistencia:** interrupción y revisión idénticas en PostgreSQL y recuperables por HTTP después de reiniciar el backend. **Git:** `8db2337`. **Azure:** no desplegada. Migraciones reales pendientes de autorización.

## HU12 — Continuidad cultural y lingüística

**Estado:** completada localmente. **Qué se implementó:** sección opcional «Apoyos culturales y comunitarios» en la entrevista familiar, consulta y edición propias y presentación al Comité dentro del expediente existente. Un único texto de hasta 2000 caracteres reutiliza `continuidadCultural`; no solicita pertenencia étnica ni incorpora un factor de scoring.

**Cómo funciona:** la familia abre su entrevista, describe cómo acompañaría costumbres, idioma y comunidad, y guarda. Puede volver a consultar o editar. El Comité ve el mismo contenido en el expediente familiar autorizado. Los clientes que omiten el campo o envían `null` conservan el texto histórico; un vaciado explícito mediante texto vacío queda auditado con su valor anterior.

**Qué se comprobó:** 19 pruebas aprobadas: 5 HTTP nuevas y 14 de servicio/controlador familiar. Guardado inicial, dos actualizaciones, lectura del Comité, intentos de acceso/cambio por otros roles y otra familia, exceso de longitud con mensaje español, conservación por omisión/null, historial y puntuación idéntica. Los apoyos escritos por las pruebas se recuperaron por HTTP después de reiniciar el backend. Chromium comprobó dos guardados reales, recarga y consulta familiar/Comité; se corrigió la distribución móvil para que el texto ocupe el ancho de la tarjeta y el recorrido final pasó de nuevo sin errores JavaScript ni desbordamientos.

**Archivos principales:** `PerfilFamiliaRequest`, `PerfilFamiliaService`, `perfil.html`, `perfiles.html`, `familia.js`, estilo compartido y `HU12PostgresIT`. **Base:** sin migración nueva; la columna ya existe. **Git:** `907c328`. **Azure:** no desplegada. No se alteraron fórmulas, pesos, criterios de completitud ni datos de expedientes infantiles.

## HU14 — Alertas por reevaluaciones

**Estado:** completada localmente. **Qué se implementó:** consulta del Comité «Reevaluaciones», derivada del mismo contador y auditoría existentes, con un aviso por familia al alcanzar tres completadas. Se muestran solicitudes y correcciones con responsable/fecha, y se distingue una solicitud pendiente del total completado. No se crea otro registro persistente de alertas.

**Cómo funciona:** el Comité abre «Reevaluaciones» desde su navegación, consulta los antecedentes desplegables y puede abrir el expediente familiar. Los contadores históricos sin actuaciones detalladas se identifican expresamente sin inventar fechas. El aviso es informativo y no cambia puntuaciones, pesos, idoneidad ni activación del perfil.

**Integridad:** las actualizaciones propias bloquean la fila familiar durante la transacción. Dos envíos concurrentes de una corrección pendiente solo incrementan una vez el contador y conservan la auditoría. Solicitudes sin entrevista completada y antecedentes cancelados no generan incrementos. No se introduce una operación de cancelación inexistente.

**Qué se comprobó:** 19 pruebas aprobadas: cinco HTTP nuevas de HU14, cinco de HU12 y nueve de servicios familiares/reevaluación. Se cubren cero, una, dos, tres y cuatro completadas; solicitud pendiente, entrevista incompleta, antecedente cancelado sintético, contador histórico sin fechas, repetición de consultas, roles y doble envío concurrente. Chromium comprobó menú, consulta real, antecedentes, recarga y móvil 390 px sin errores ni desbordamientos. El estado vacío se revisó con una respuesta simulada de la API; el umbral y los permisos se verificaron contra PostgreSQL. Evidencias `target/verificacion-hu19/hu14-*` y `HU14-*.png`. El contador y los antecedentes se recuperaron idénticos por HTTP tras reiniciar el backend local. **Git:** `cde4c5f`.

**Archivos principales:** `PerfilFamiliaService`, `PerfilFamiliaRepository`, `ScoringController`, `comite.js`, `profesionales.js`, pruebas familiares ajustadas al bloqueo y `HU14PostgresIT`. **Base:** sin migración adicional. **Azure:** pendiente, sin cambios en el entorno persistente.

## HU13 — Seguridad y auditoría de pesos

**Estado:** completada localmente. **Qué se implementó:** se conservaron y verificaron los controles existentes del módulo de pesos, sin reconstruirlo. Se aclaró la introducción «Configuración de pesos»: la importancia de los cinco factores y la obligación de justificar cada modificación. La confirmación muestra valores actuales/propuestos y conserva el historial consultable.

**Cómo funciona:** el Comité abre «Criterios», despliega «Ajustar pesos», introduce enteros entre 0 y 100 que suman 100 y confirma con una justificación de 10 a 300 caracteres. El servidor valida identidad, rol y versión, serializa el cambio y guarda conjuntamente pesos y auditoría. Un formulario desactualizado recibe un aviso para recargar; otros roles no pueden consultar ni modificar esta configuración.

**Qué se comprobó:** 127 pruebas aprobadas: cinco nuevas HTTP/PostgreSQL y 122 existentes de funcionalidad, fiabilidad y seguridad. Se probaron 18 cuerpos inválidos, permisos de los cuatro roles, falsificación de cabeceras, justificación, anteriores/nuevos, actor/fecha, versiones, dos cambios concurrentes (200/409) y conservación exacta del contexto/puntaje de una decisión anterior. Los intentos rechazados no agregan auditoría ni cambian configuración. Chromium comprobó consulta sin escrituras, guardado justificado, historial, suma inválida, conflicto de versión, recarga y móvil sin errores JavaScript ni desbordamientos. Los valores iniciales se restablecieron mediante un cambio autorizado de prueba; sus versiones y actuaciones se conservaron. Pesos, versión e historial idénticos después de reiniciar el backend.

**Archivos principales:** `comite.js` y `HU13PostgresIT`, reutilizando `ScoringController`, `ScoringService`, `PesosScoringRepository` y `auditoria_casos`. **Base:** sin nueva migración. **Git:** `9d6b06d`. **Azure:** pendiente, sin cambios. **Algoritmo:** `ScoringService` completo se comparó byte a byte con `7b5b3fd`; conserva fórmulas, factores y valores iniciales. Evidencias `target/verificacion-hu19/hu13-*` y `HU13-*.png`.

## Matriz de aceptación

| Historia | Criterios | Estado | Evidencia |
| --- | --- | --- | --- |
| HU19 | 1–8 | Comprobados localmente | Suite inicial 252 Java; `HU19PostgresIT`; auditoría y navegador previos conservados. |
| HU11 | 1–3, 5–10 | Comprobados localmente | `HU11PostgresIT`, 6 casos HTTP y carreras; navegador TS/Comité/familia; permisos SQL. |
| HU11 | 4: reinicio | Comprobado localmente | Snapshot íntegro y recuperación HTTP de la revisión tras reinicio. |
| HU12 | 1–10 | Comprobados localmente | `HU12PostgresIT`, 5 casos HTTP; 14 pruebas familiares existentes; recuperación tras reinicio; Chromium. |
| HU14 | 1–10 | Comprobados localmente | `HU14PostgresIT`, cinco casos HTTP y concurrencia; contador/auditoría originales; Chromium y vista móvil. |
| HU13 | 1–11 | Comprobados localmente | `HU13PostgresIT`, cinco casos HTTP, 18 cuerpos inválidos, falsificación de rol y concurrencia; 122 pruebas existentes; Chromium; reinicio; comparación íntegra de `ScoringService`. |


## Cierre integrado y reproducción

La regresión final `mvn -B verify -Pe2e` aprobó **273 pruebas Java: 234 unitarias y 39 HTTP de integración, sin fallos, errores ni omisiones**. Las suites de integración comprenden 8 HU19, 6 HU11, 5 HU12, 5 HU14, 5 HU13 y 10 del flujo anterior. Las ocho pruebas Python del actualizador aprobaron; también `bash -n` y 14 comprobaciones de sintaxis de archivos/bloques JavaScript, incluidos scripts internos de HTML. No se sustituyeron pruebas del equipo ni se redujeron sus aserciones.

Reproducción: preparar una base PostgreSQL 16 aislada con las migraciones hasta 013, habilitar `HU19_TEST_DB_URL` con URL local y nombre `hu19_`, `HU19_TEST_DB_USER` y `HU19_TEST_DB_PASSWORD`, y ejecutar con Java 17 `mvn -B verify -Pe2e`. La fixture del flujo anterior utiliza la contraseña sintética de pruebas, configurable con `SENNIAF_DEMO_PASSWORD`. No apuntar estas pruebas a una base habitual. CI prepara un entorno nuevo mediante `src/test/scripts/preparar-postgres-ci.sh`; no reutilizar ese script sobre un entorno con roles/datos existentes. Para el auxiliar: `python3 -m unittest discover -s src/test/scripts -p 'test_*.py' -v`.

Evidencia final: `target/verificacion-hu19/sprint8-final.log`, `sprint8-actualizador.log`, `sprint8-sintaxis.log` y los informes XML de Maven. Los recorridos visuales sintéticos y capturas permanecen en esa carpeta ignorada. La [matriz detallada](SPRINT8_MATRIZ_ACEPTACION.md) desglosa los 49 criterios y sus pruebas; todos están comprobados localmente.

**Artefacto funcional:** `target/senniaf-match-sprint8-validacion-9d6b06d.jar`, commit `9d6b06d`, rama `sprint7-desarrollo`, compilado desde árbol limpio con metadatos incorporados. SHA-256: `bd9fe9382da347a9b5bb0efa44aa489f14808f9ba37e0b931d685391c8690cec`. El control de compatibilidad acepta el JAR con 012/013. El JAR se inició realmente con Java 17 y validación del esquema: consultas de los cuatro roles y recuperación de HU11/HU12 correctas. Los conteos y huellas de las 18 tablas durables permanecieron idénticos antes/después de reiniciar, excluyendo expresamente las sesiones de autenticación. Evidencia: `sprint8-jar-validar.log` y snapshots `sprint8-integridad-antes.json`/`sprint8-integridad-despues.json`. No está desplegado; el cierre documental posterior no cambia su código.

**Pendientes de integración, no bloqueos locales:** revisión y publicación de commits por el equipo, respaldo reciente y verificado, migraciones 012 y 013 y despliegue autorizado. El actualizador y su auxiliar deben instalarse juntos. La recuperación exige un JAR compatible con vinculaciones e interrupciones. No se ejecutó push, restauración, modificación de credenciales, cambios de infraestructura ni operación de Azure. Consulta pública final el 14 de noviembre: raíz 200, Vinculaciones 404. El estado administrativo detallado de la base procede de la auditoría anterior conservada; no se simuló una nueva inspección de la VM.
