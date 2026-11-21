# Citas durante la vinculación — Sprint 9

Implementación local en `mejora/sprint9`. Sin push, merge, despliegue ni acceso a Azure o PostgreSQL de producción.

## Funcionalidad

- Familia entra en **Mis citas**, selecciona una vinculación habilitada y solicita una cita mediante seis motivos, mensaje editable, fecha nativa y selector de horas. La duración inicial es de dos horas. Las plantillas personalizadas se conservan al cambiar otros campos y existe restablecimiento explícito.
- Trabajo Social entra en **Gestión de citas**; inicialmente ve las pendientes y puede filtrar los demás estados. Cada solicitud pertenece al trabajador social actualmente asignado al expediente del niño; no se inventan asignaciones ni se envía a todos los profesionales.
- **Aceptar** abre una revisión, permite dos horas o una hora, calcula la finalización y confirma sin exigir justificación. **Rechazar** solicita motivo y mensaje editable. **Proponer otra fecha** permite nueva fecha, hora, duración y motivo; deja la solicitud en Cambio propuesto.
- La familia revisa y acepta la propuesta o solicita otro horario. La aceptación vuelve a comprobar disponibilidad; la contrapropuesta regresa a Pendiente conservando las propuestas anteriores.
- Estados: Pendiente, Confirmada, Cambio propuesto, Rechazada, Cancelada y Realizada. La familia puede cancelar solicitudes/citas futuras; Trabajo Social rechaza las pendientes y puede cancelar otras citas futuras. Solo el profesional puede marcar Realizada una cita confirmada cuyo intervalo terminó.
- Solicitud y acciones se guardan transaccionalmente con historial inmutable y evento de caso. El historial conserva actor, profesional, estado, mensajes y fechas propuestas; la familia recibe etiquetas comprensibles, sin identificadores técnicos de usuarios ni información clínica.

La vinculación debe tener respuesta familiar aceptada, reserva activa y expediente sin baja, cierre o interrupción. No se modifican los estados ni datos de la vinculación. Una reasignación del profesional revoca el acceso del anterior; una propuesta anterior a la reasignación no puede aceptarse sin revisión del responsable actual.

## Horario y validaciones

Zona horaria `America/Panama`; comunicación interna de horas en formato de 24 horas y presentación de 12 horas. El perfil **`citas-demo`** configura exclusivamente el prototipo: lunes a viernes de 08:00 a 16:00, intervalos de 30 minutos. **No es un horario oficial de SENNIAF.** Una cita de dos horas puede comenzar a las 14:00, pero no a las 15:00.

Configuración sin nueva pantalla administrativa:

```properties
senniaf.citas.franjas=1@08:00-16:00,2@08:00-16:00,3@08:00-16:00,4@08:00-16:00,5@08:00-16:00
senniaf.citas.intervalo-minutos=30
```

Días ISO: 1 lunes, 7 domingo. Puede utilizarse `SENNIAF_CITAS_FRANJAS` o la configuración externa de Spring. El perfil normal no define horas; con configuración ausente no se puede reservar ni confirmar. Una configuración explícita mal formada impide iniciar la aplicación.

Frontend y backend validan fechas reales, formato estricto, instantes futuros, días habilitados, intervalos y finalización dentro del horario. El backend determina permisos e identidad mediante la sesión existente, no mediante datos de familia/profesional suministrados por el navegador. Mensajes limitados y escapados; sin marcadores literales de fecha/hora ni controles invisibles.

Las solicitudes pendientes no reservan agenda. Al confirmar se compara el intervalo completo contra todas las citas confirmadas del mismo profesional. Intervalos contiguos están permitidos. Transacciones, bloqueo de la fila del profesional, bloqueos de las raíces de vinculación/expediente y versión de cita protegen contra confirmaciones y respuestas simultáneas. Una restricción parcial impide dos solicitudes abiertas por vinculación. Los bloqueos no se propagan a las tablas históricas inmutables ni requieren ampliar sus permisos.

## Persistencia y compatibilidad

Nueva migración aditiva: [`020-citas-vinculacion.sql`](../deploy/azure/sql/020-citas-vinculacion.sql). Requiere el esquema 019; crea únicamente `citas_vinculacion` y `citas_historial`, sus restricciones, índices y protección del historial contra UPDATE/DELETE. Otorga al rol de aplicación permisos exclusivamente sobre estos nuevos objetos: historial solo INSERT/SELECT. No transforma datos antiguos ni modifica tablas históricas.

La migración se aplicó **solo a una base nueva de pruebas local**. Antes de un futuro despliegue habrá que aprobar y aplicar 020 y definir el horario correspondiente. El procedimiento existente de actualización ahora comprueba, mediante SELECT, que el esquema de citas existe antes de admitir un JAR que lo utiliza. No ejecuta migraciones. La recuperación hacia un JAR anterior compatible conserva las tablas nuevas y sus datos; no restaura la base automáticamente. Se mantienen las comprobaciones existentes de SHA-256, respaldo y compatibilidad.

Rutas nuevas: `/citas.html` y `/api/citas` (listado, creación, opciones, disponibilidad, detalle y acciones). Sin notificaciones por correo, IA, videollamadas, calendarios externos ni dependencias nuevas de la aplicación.

## Diseño y comprobación en navegador

Se reutilizan tipografía, iconos, menú lateral, botones, tarjetas y estilos base existentes. Solo se añade una entrada en los menús de Familia y Trabajo Social. CSS específico del módulo, sin rediseñar otras pantallas. Estados con texto e iconografía, formularios etiquetados, foco visible, diálogo con Escape, restitución del foco y navegación Tab contenida.

Capturas reales de Chromium conectado a Spring Boot y PostgreSQL, sin respuestas simuladas:

| Vista | Evidencia |
| --- | --- |
| Familia, escritorio | [Panel](citas-sprint9/familia-escritorio.png), [solicitud](citas-sprint9/solicitud-escritorio.png) |
| Trabajo Social, escritorio | [Panel](citas-sprint9/trabajo-social-escritorio.png) |
| Respuestas del profesional | [Aceptar](citas-sprint9/aceptar-escritorio.png), [rechazar](citas-sprint9/rechazar-escritorio.png), [proponer](citas-sprint9/proponer-escritorio.png) |
| Respuesta familiar e historial | [Propuesta recibida](citas-sprint9/familia-respuesta.png), [revisión](citas-sprint9/aceptar-propuesta-familia.png), [historial](citas-sprint9/familia-confirmada-historial.png) |
| Móvil | [Familia](citas-sprint9/familia-movil.png), [formulario](citas-sprint9/solicitud-movil.png), [Trabajo Social](citas-sprint9/trabajo-social-movil.png), [propuesta](citas-sprint9/proponer-movil.png), [acciones con desplazamiento interno](citas-sprint9/proponer-movil-acciones.png), [320 px](citas-sprint9/familia-320.png) |

Se verificaron escritorio, 390 px y 320 px: sin desbordamiento horizontal, campos alineados, botones accesibles y diálogo contenido en la ventana con desplazamiento interno. Se corrigieron el contraste de descripciones y resúmenes y la altura heredada de botones en diálogos móviles. La muestra de colores calculados contiene 48 textos/controles visibles: mínimos de contraste 5,04:1 en panel y solicitud y 5,43:1 en revisión de aceptación. No constituye una auditoría completa con lector de pantalla.

Evidencia: [flujo y geometría](citas-sprint9/navegador.json), [colores computados](citas-sprint9/colores.json), [esquema y permisos reales](citas-sprint9/esquema-y-permisos.json).

## Pruebas

Java 17, PostgreSQL 16.4 aislado en `127.0.0.1:55439`, base nueva `hu19_citas_sprint_nueve`, exclusivamente datos sintéticos. Pruebas de integración y aplicación de navegador ejecutadas como **`senniaf_app`**, con permisos reales de aplicación; no como propietario de las tablas.

| Suite | Ejecutadas | Satisfactorias | Fallidas | Errores | Omitidas |
| --- | ---: | ---: | ---: | ---: | ---: |
| Java: pruebas existentes `*Test` | 996 | 996 | 0 | 0 | 0 |
| Nuevas unitarias de horario y servicio | 55 | 55 | 0 | 0 | 0 |
| Nuevas HTTP/PostgreSQL `CitasPostgresIT` | 18 | 18 | 0 | 0 | 0 |
| Compatibilidad de despliegue, Python | 13 | 13 | 0 | 0 | 0 |
| **Total** | **1082** | **1082** | **0** | **0** | **0** |

Comando Java: `mvn -q '-Dtest=*Test,CitasPostgresIT' test`, con `JAVA_HOME` de Java 17 y variables `HU19_TEST_DB_URL`, `HU19_TEST_DB_USER`, `HU19_TEST_DB_PASSWORD` apuntando únicamente a la base aislada. Python: `python3 -m unittest discover -s src/test/scripts -p test_compatibilidad_despliegue.py -q`. También `bash -n` y `git diff --check`. No se afirma haber ejecutado las demás suites históricas PostgreSQL `*IT`.

Cobertura nueva: permisos entre familias y trabajadores distintos, cambios de asignación, ausencia de vinculación/configuración, fechas manipuladas, fines de semana, fechas pasadas, duración, límites de jornada, solapamientos, intervalos contiguos, solicitudes duplicadas, versiones obsoletas, tres escenarios de concurrencia, transiciones e historial protegido. El navegador comprobó además plantillas editables, prevención de sobrescritura, duración de una hora, contrapropuestas, aceptación por la familia, rechazo, cancelación, historial, teclado y contenido HTML mostrado como texto.

[Conteos por clase](citas-sprint9/pruebas.json). Las comprobaciones de navegador se informan aparte y no inflan el total automatizado.

## Archivos e integridad

Creados: `CitaVinculacion`, `CitaHistorial`; sus dos repositorios; `CitaService`, `HorarioCitas`, `CitaController`; `application-citas-demo.properties`; `citas.html`, `citas.css`, `citas.js`; migración 020; `CitaServiceTest`, `HorarioCitasTest`, `CitasPostgresIT`; este informe y evidencias en `docs/citas-sprint9/`.

Modificados: `profesionales.js` (dos entradas de menú), preparación PostgreSQL de CI (020), workflow Maven (validación sintáctica del nuevo JavaScript), verificador de compatibilidad y `actualizar.sh` (detección de 020), pruebas de compatibilidad de despliegue (tres casos nuevos). Los respaldos y la sustitución de JAR siguen el procedimiento existente.

Sin cambios en la página pública, CSS de Inicio, autenticación/registro, motor EVIDENCIA_TEXTO_7, fórmulas, ponderaciones, módulo del Comité ni diseño de vinculaciones. No se modificaron expedientes ni históricos reales. Los únicos registros creados/modificados son sintéticos dentro de la base aislada. El archivo de referencia del usuario `docs/image.png` se conserva fuera de los commits.

Los cambios previos de Inicio y mediciones se conservaron en los commits locales `c40b08c` y `9461de5`. El backend quedó en `ffc2759`; el commit de interfaz y evidencias se indica en la entrega final; su detalle se puede consultar con `git log --oneline mejora/sprint9`. No se modificaron otras ramas.
