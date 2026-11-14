# Sprint 8 — integración y despliegue

## Conservación de Git

Repositorio `eeeamir/senniaf_match`. Remoto actualizado: `main` y `sprint7-desarrollo` compartían `67db4fa`. Los seis commits originales `7b5b3fd`, `8db2337`, `907c328`, `cde4c5f`, `9d6b06d`, `87f40e5` se conservan intactos y están publicados en `integracion/sprint8`. Referencia local adicional: `respaldo/sprint8-20261114-87f40e5`. No se incorporaron commits ajenos adicionales ni hubo conflictos. PR #2 contra `main`. GitHub Actions prueba; el despliegue utiliza el actualizador manual existente.

## Ampliación de HU11 antes del despliegue

El Comité registra «Registrar cierre institucional» desde Vinculaciones: fecha, referencia, autoridad y motivo/resultado, con confirmación explícita de la actuación externa. Actor y fecha de registro se obtienen en servidor. Estado `CERRADA`, cierre único e historial preservado. No formaliza adopción ni constituye autorización legal.

Cerrar no libera automáticamente expedientes. Cada uno conserva su reserva hasta una revisión individual del Comité con justificación; el servidor comprueba por separado las condiciones vigentes. Un niño con baja/cierre por adopción o perfil incompleto no vuelve a Match; una familia con entrevista incompleta o reevaluación pendiente tampoco. La revisión de un expediente no libera al otro. Se mantienen las fórmulas, puntajes históricos e interrupciones anteriores. El portal familiar nunca recibe el expediente infantil restringido, antes ni después del cierre.

La migración aditiva 014 amplía Vinculaciones, conserva las filas y adapta restricciones/índices para reservas independientes. Se aplica después de 012/013. El actualizador y su auxiliar comprueban también 014 e impiden recuperar un JAR que ignore los cierres. Ninguna migración se ejecuta automáticamente al iniciar la aplicación.

## Validación de la ampliación

Diecinueve pruebas enfocadas aprobadas: cinco nuevas de cierre, ocho de HU19 y seis de HU11. Cubren confirmación, permisos, fechas/campos, cierre sin inventar respuesta familiar, doble cierre concurrente, disponibilidad separada, perfil no elegible, nueva asignación del niño tras revisión y conservación de interrupción/puntaje/privacidad. Chromium completó el formulario real, consultó el cierre y antecedentes, revisó solo un expediente y comprobó recarga/móvil sin errores JavaScript ni desbordamiento. Evidencias locales excluidas de Git en `target/integracion-sprint8`.

La regresión completa aprobó 278 pruebas Java (234 unitarias y 44 de integración) sin fallos ni omisiones, y nueve del actualizador. El pipeline repetirá todas las pruebas sobre el commit publicado.

El pipeline conserva ahora también el JAR validado y su SHA como artefacto de la misma ejecución. No se crea otro despliegue ni infraestructura. Permite trasladar el artefacto exacto al actualizador existente cuando SSH no sea accesible.

## Azure: comprobaciones previas

Base persistente `senniaf_demo`, PostgreSQL 16.15, servicio y Nginx activos. Se comprobaron ausencias de 012/013, unidad de edad presente y JAR anterior SHA `7a06feedf3165f746d69cb12afec861d3b0c2ebecd4dd4b83c6728d58c35e033`. SSH agota conexión; se usa Azure Run Command sin abrir puertos.

Respaldo nuevo creado con el mecanismo existente: `20261114T060120Z.xoQg12.dump`, SHA `273f6a78d4ee8b581d7ad79bd0003e2199a5917d52ebf47d692508f508b05ba6`; lectura completa mediante `pg_restore --file=/dev/null` y copia privada externa confirmadas. Una aceptación histórica no presenta referencias inválidas ni asignaciones duplicadas incompatibles con 012.

**Estado de publicación:** en preparación. Las migraciones y el JAR nuevo todavía no se han aplicado en el momento de crear este informe. Se registrarán aquí los resultados reales y cualquier bloqueo.
