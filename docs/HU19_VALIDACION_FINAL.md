# HU19 — Revisión final y validación

Fecha: 13 de noviembre de 2026, Panamá.

## Resultado

HU19 quedó verificada localmente en esta revisión. La auditoría posterior exige comprobar también la integración persistente antes de iniciar HU11; consultar [la verificación integral](VERIFICACION_INTEGRAL_HU19.md). Se comprobó el flujo completo en una aplicación local conectada a PostgreSQL aislado. No se desplegó ni se migró ninguna base existente del usuario o de Azure. El cálculo de compatibilidad y los pesos siguen idénticos a `HEAD`.

## Correcciones

- Nombres consistentes: «Vinculaciones» institucional y «Mi vinculación» familiar, conservando la URL familiar anterior. La notificación institucional usa un contador separado para conservar el nombre del menú.
- Textos familiares en segunda persona, estados vacíos, errores de conexión comprensibles y confirmaciones accesibles. No se atribuye autorización de acogimiento ni formalización de adopción a una respuesta.
- Iconos del catálogo SVG existente: relación, familia, consulta, confirmación, cancelación, información e historial. Se corrigió el trazado defectuoso del icono de cancelación observado en el navegador y se diferenció visualmente el estado pendiente.
- Historial consultado por referencia JSON estructurada, admitiendo espacios, campos opcionales y entradas antiguas sin JSON. El navegador recibe el resumen autorizado, sin tener que interpretar la auditoría interna. Fechas con zona de origen, siguiendo la convención existente.
- La interfaz del Comité consulta la reserva actual para bloquear una propuesta, en lugar de interpretar cualquier aceptación histórica como una exclusión permanente. No se implementaron cierres ni liberaciones automáticas.
- Migración con confirmación explícita del destino, bloqueo de escrituras durante la conversión, permisos para la cuenta técnica existente `senniaf_app` y error verificable al rechazar el destino. Se conserva la transacción completa ante conflictos históricos.

## Permisos comprobados

| Usuario | Acceso |
| --- | --- |
| Comité | Listado, búsqueda, filtros, detalle, expedientes autorizados, decisión, respuesta e historial. Acepta propuestas en el flujo existente del Match. |
| Trabajo social asignado | Vinculaciones del expediente a su cargo; consulta sin decisiones del Comité ni respuestas familiares. |
| Psicología asignada | Vinculaciones del expediente a su cargo; consulta sin decisiones del Comité ni respuestas familiares. |
| Profesional sin asignación | No recibe esas vinculaciones en el listado; detalle directo denegado. |
| Familia correspondiente | Mi vinculación y una única respuesta a una vinculación pendiente. Sin acceso al módulo institucional ni a expedientes infantiles restringidos. |
| Otra familia / sesión ausente | Respuesta ajena denegada; API institucional denegada; sin sesión, HTTP 401. |

Se probaron solicitudes HTTP reales, manipulación de cabeceras de identidad y campos de estado/responsable enviados en el cuerpo. El servidor conserva la identidad de sesión y decide estado y fecha.

Se mantiene el supuesto conservador del proyecto: no existe una política documentada que autorice datos infantiles específicos en este portal. HU19 no concede acceso a expedientes infantiles ni antes ni después del rechazo. No se presenta esa denegación permanente como una prueba de revocación de un permiso previamente concedido. La revisión institucional de qué información infantil se podrá compartir sigue pendiente; cualquier ampliación tendrá que comprobar la vinculación y su etapa en el servidor.

## PostgreSQL y migración

Se preparó un servidor PostgreSQL **18.3** temporal, limitado a `127.0.0.1:55419`, sin instalar un servicio del sistema ni acceder a bases existentes. Las bases de prueba parten del esquema SQL del repositorio y sus ampliaciones 005–011; el arranque de la aplicación utiliza `ddl-auto=validate`.

Se aplicó `012-vinculaciones.sql` tanto sobre una base vacía como sobre una copia con una aceptación sintética anterior. Se verificaron:

- Conservación exacta de las decisiones previas, sus fechas y responsables; incorporación como pendiente sin inventar respuesta familiar.
- Rechazo por SQL directo de reservas duplicadas por niño o familia, decisión duplicada y referencias inexistentes a decisión o expedientes.
- Rechazo de un estado respondido sin fecha/responsable.
- Reversión completa de la migración ante aceptaciones históricas contradictorias o referencias ausentes, sin dejar la tabla creada parcialmente ni alterar las decisiones.
- Cancelación con salida de error cuando la base no coincide con el destino confirmado.
- Permisos de la cuenta técnica existente: consulta, inserción y actualización de vinculaciones y uso de su secuencia; sin borrado de vinculaciones ni edición/borrado de auditoría, decisiones o evaluaciones. Se verificaron tanto la migración sin esa cuenta como la migración con ella presente.

Las pruebas HTTP concurrentes verificaron una respuesta exitosa y un conflicto HTTP 409 en cada carrera: mismo niño con dos familias, misma familia con dos niños, doble aceptación de la misma propuesta y respuestas familiares contradictorias. Se comprobó que solo queda una vinculación y una respuesta aplicable.

El rechazo mantiene la reserva y conserva puntuación, reevaluación y estado general del expediente. Una prueba adicional simula una futura liberación institucional **solo mediante SQL en la base desechable**, comprobando que una aceptación histórica no impide volver al ranking; no se añadió esa operación al producto.

La validación se realizó con PostgreSQL 18.3, no con la versión 16 de la demostración. Antes del despliegue corresponde repetir la validación en esa versión y aplicar la migración a la base autorizada con respaldo y el código anterior detenido:

```bash
psql -X -v ON_ERROR_STOP=1 \
  -v base_autorizada=BASE_REVISADA \
  -v confirmacion=CREAR_VINCULACIONES_SIN_BORRAR \
  -f deploy/azure/sql/012-vinculaciones.sql
```

No se ejecutó ese comando contra Azure ni contra una base real del usuario.

## Navegador

Se levantó Spring Boot en `127.0.0.1:18089`, con sesiones y base reales de pruebas, y se utilizó **Chromium 159 mediante Playwright**, sin respuestas simuladas para los recorridos funcionales.

Se comprobaron inicio de sesión, navegación, títulos, iconos, tarjetas y formularios de los cuatro roles; aceptación de dos propuestas desde el formulario y la confirmación del Comité; aceptación familiar, rechazo familiar, respuesta visible al Comité e historial automático; recarga de respuestas; estado familiar vacío; denegación de la ruta institucional a la familia; listado asignado y detalle de consulta para trabajo social/psicología; listado vacío para un profesional sin asignación; búsqueda y filtros de estado. Se comprobaron anchuras de 320, 390, 760 y 1440 píxeles, sin desbordamiento horizontal, y se inspeccionaron capturas de escritorio y móvil. Se corrigieron y volvieron a comprobar el recorte del nombre del Comité en la cabecera móvil y el espacio del contador en la navegación. No hubo errores JavaScript en esos recorridos. Tras reiniciar Spring Boot se comprobó que las aceptaciones y rechazos anteriores seguían guardados y sin botones para cambiar la respuesta. También se verificaron una vinculación inexistente y un fallo de red provocado únicamente en el navegador, con mensajes comprensibles.

Las capturas y registros de ejecución se conservaron temporalmente en `/tmp/hu19-validation`, fuera del repositorio y con datos sintéticos. No constituyen pruebas de accesibilidad exhaustiva ni de otros navegadores.

## Pruebas y regresión

Antes de las correcciones se reprodujeron los mismos **37 fallos y 4 errores** en las 231 pruebas de la implementación anterior. Se revisaron sus causas:

| Grupo histórico | Causa y corrección |
| --- | --- |
| 19 fallos de scoring | Las expectativas acreditaban puntos por salud/hermanos no aplicables o sin evidencia. Se ajustaron al contrato documentado, se completó el dato de cantidad para probar disposiciones ante hermanos y se añadieron casos de evidencia completa y cantidad desconocida. No cambió el algoritmo. |
| 17 fallos de permisos | Se esperaba que un controlador con un servicio completamente simulado ejecutase los controles delegados al servicio. Ahora se verifica la delegación y se comprueban las denegaciones con el servicio real y repositorios simulados. Las pruebas HTTP adicionales validan permisos con sesión real. |
| 1 fallo de controlador | La prueba de alerta médica intentaba crear un expediente con Psicología. Usa trabajo social, conforme al permiso vigente. |
| 4 errores de servicios | Tres casos esperaban guardar formularios incoherentes; ahora verifican su rechazo. El cuarto simulaba consultas de lectura aunque la decisión usa consultas con bloqueo; se corrigieron los mocks. |

Resultado final de la ejecución conjunta: **242 pruebas, 0 fallos, 0 errores, 0 omitidas**:

- 234 pruebas unitarias/controladores, incluyendo las 20 pruebas originales de HU19, una nueva de historial y dos nuevos casos de evidencia del scoring.
- 8 pruebas de integración PostgreSQL/HTTP en `HU19PostgresIT`, repetidas además con la cuenta técnica restringida `senniaf_app`.
- Comprobación de sintaxis de `comite.js`, `familia.js`, `profesionales.js` y `vinculaciones.js` correcta; `git diff --check` correcto.

Comandos reproducibles, después de preparar expresamente el esquema en una base desechable propiedad de `hu19_test`:

```bash
mvn -B test

HU19_TEST_DB_URL=jdbc:postgresql://127.0.0.1:55419/hu19_validacion \
  mvn -B '-Dtest=*Test,HU19PostgresIT' test

# Repetición con la cuenta técnica, después de 002 y 012 en la base aislada:
HU19_TEST_DB_URL=jdbc:postgresql://127.0.0.1:55419/hu19_permissions \
HU19_TEST_DB_USER=senniaf_app \
  mvn -B -Dtest=HU19PostgresIT test
```

La prueba de integración solo se habilita con esa variable y un destino local cuyo nombre comience por `hu19_`. Inserta exclusivamente datos sintéticos en la base indicada y no los limpia automáticamente. No apuntarla a una base de trabajo existente. La suite histórica `Sprint6E2EIT` no se ejecutó: prepara datos y usa contratos anteriores, distintos de esta suite nueva de HU19.

## Archivos de esta revisión

| Archivos | Motivo |
| --- | --- |
| `src/main/java/com/senniaf/match/service/VinculacionService.java` | Historial estructurado, fechas y mensajes familiares. |
| `deploy/azure/sql/012-vinculaciones.sql` | Destino confirmado, bloqueo transaccional y permisos de la cuenta técnica existente. |
| `src/main/resources/static/vinculaciones.js` | Texto, iconos, errores, confirmaciones, enlaces autorizados y filtros. |
| `src/main/resources/static/profesionales.js`, `profesionales.css` | Catálogo SVG, icono corregido, contador independiente y estados. |
| `src/main/resources/static/familia.js`, `comite.js` | Terminología y consulta de la reserva actual. |
| `src/test/java/com/senniaf/match/HU19PostgresIT.java` | Integración real, permisos, persistencia, rechazo y concurrencia. |
| `src/test/java/com/senniaf/match/service/VinculacionServiceTest.java` | Historial con JSON válido independiente de espacios y entradas antiguas. |
| `PerfilNinoControllerTest`, `ScoringFuncionalIsoTest`, `NinosAccesoIsoTest`, `PerfilFamiliaServiceTest`, `PerfilNinoServiceTest`, `ScoringReevaluacionServiceTest` | Corregir los contratos y mocks históricos detallados arriba. |
| `docs/HU19_VINCULACIONES.md`, `docs/HU19_VALIDACION_FINAL.md`, `README-BASE-DATOS.md` | Evidencia, límites y pasos de despliegue actualizados. |

## HU11 y pendientes

Se conservan estado de vinculación, reserva independiente, decisión original, expedientes e historial reutilizable. HU11 podrá añadir actuaciones de interrupción/cierre y liberar reservas tras confirmar disponibilidad, ampliando estados y restricciones SQL. No se incorporaron esos formularios ni su lógica.

Pendientes operativos: desplegar en el entorno autorizado, respaldar y aplicar 012 allí, repetir las comprobaciones en PostgreSQL 16 y definir institucionalmente cualquier información infantil adicional que se permita mostrar. No se añadió lógica judicial ni del Sprint 9, dependencias al proyecto, push o cambios de ramas remotas.
