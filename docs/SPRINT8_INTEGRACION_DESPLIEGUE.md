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

## Primera publicación comprobada

PR #2 fusionado sin conflictos ni squash. Commit de `main`: `4d19bc25274efeb6ba56b0d9d4826ad7f9930424`. Pipeline [43564815129](https://github.com/eeeamir/senniaf_match/actions/runs/43564815129) aprobado; JAR CI SHA `5b28a560a065f7b3a858b86d7e347f49bbde097deba0f3ed31cbbd61a01c2683`, metadatos coincidentes con main.

Se aplicaron 012, 013 y 014 en orden mediante psql del propietario, con el backend detenido. Respaldo inmediato previo: `20261114T061232Z.SC2pvm.dump`, SHA `a876921a0dcb0a8e808b0723b2b43bee566522881d771a44271adabbe2362d60`; lectura completa y copia externa descargada mediante identidad administrada con SHA idéntico. El actualizador generó además `20261114T061234Z.B7M5ql.dump`, SHA `2326c98bc91aceadc830542edafc615a7df9868d824a01d81959238ead9cc5b4`.

La migración conservó una aceptación histórica como vinculación pendiente, sin inventar respuesta familiar. Las 16 tablas durables anteriores conservaron conteos/huellas antes y después de migración y arranque; se excluyeron expresamente sesiones de autenticación. Backend, Nginx y PostgreSQL activos. Vinculaciones HTTPS 200 y API sin sesión 401.

Chromium público: autenticación de Comité, Trabajo Social, Psicología y Familia; consultas de Vinculaciones y restricciones; Comité consulta criterios/historial y reevaluaciones y abre/cancela el formulario de cierre; familia consulta Mi vinculación con acceso limitado. Escritorio y 390 px, recarga y ausencia de errores JavaScript. **No se crearon decisiones, respuestas, cierres ni perfiles sintéticos en la base persistente.** Los flujos de escritura, aceptación/rechazo, interrupción y cierre/revisión se verificaron integralmente en PostgreSQL aislado; en Azure se comprobaron consultas, permisos y formularios sin alterar actuaciones reales.

## Ajuste de fechas previo al cierre final

Se detectó que comparar directamente el día de un timestamp del servidor con la fecha institucional de Panamá puede rechazar una actuación válida de la noche anterior si la VM usa UTC. La comparación se realiza ahora en la fecha de Panamá, conservando los timestamps históricos y el reloj/configuración del servidor. Se añadió una prueba HTTP específica con servidor UTC y propuesta nocturna de Panamá. Este ajuste no cambia fórmulas, pesos ni reglas de elegibilidad y no necesita otra migración. El siguiente commit/pipeline registra la versión final de esta corrección.

La corrección de fechas superó las seis pruebas HTTP de cierre en PostgreSQL aislado, incluido el caso nocturno con servidor UTC. Se conserva el permiso que impide sobrescribir decisiones registradas: la fixture inserta un antecedente nuevo.
