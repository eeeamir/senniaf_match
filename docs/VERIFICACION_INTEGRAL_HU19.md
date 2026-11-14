# Verificación integral antes de HU11

Fecha: 13 de noviembre de 2026, Panamá.

## Estado y decisión

HU19 está implementada y probada en la rama local correcta. **No está publicada ni integrada en la base persistente de Azure. No comenzar HU11 todavía:** la solicitud exige que esa integración esté lista y comprobada, y la tabla y el código de HU19 siguen ausentes del entorno desplegado.

No se cambió de rama ni se modificaron datos, credenciales, servicios, red o binarios en Azure. Las consultas administrativas se limitaron a estado, esquema, conteos, huellas y lectura de respaldos. Los datos sintéticos se utilizaron exclusivamente en servidores de prueba aislados.

## Git y ramas

- Repositorio: `eeeamir/senniaf_match`, con `origin` coincidente. Rama de trabajo encontrada: `sprint7-desarrollo`, asociada a `origin/sprint7-desarrollo`.
- Después de `git fetch origin`, tanto esta rama como `main` coinciden en `67db4fac8ee168362675d35fd71a1cd056088d21`: inicialmente 0 commits adelantados y 0 atrasados. No se encontró otra rama remota de Sprint 8. No se presume una política de equipo diferente de esos hechos.
- Los archivos iniciales sin commit corresponden a HU19 y las correcciones de pruebas de las dos revisiones anteriores. Se conservaron; no se hizo reset, cambio de rama, rebase, merge ni limpieza del trabajo ajeno.
- El workflow remoto actual se activa para push/PR de `main`; **no despliega**. No existe una rama que publique automáticamente en Azure. La ampliación local mantiene el mismo archivo y añade validación de la rama de trabajo, sin crear un pipeline paralelo.
- Última ejecución remota observada: [42343316471](https://github.com/eeeamir/senniaf_match/actions/runs/42343316471), satisfactoria para `b684f775`, 7 de noviembre. Entre las 13 ejecuciones consultadas no aparece una para `67db4fa`; esa ejecución anterior no certifica HU19 ni el árbol actual. No se lanzó ni repitió un workflow remoto.

## Arquitectura y configuración

Spring Boot 3.5.16/JPA/PostgreSQL, con Java compatible con 17. Frontend HTML/CSS/JavaScript estático, empaquetado por Maven dentro del mismo JAR; no hay un frontend que requiera npm ni una base definitiva adicional. `application-remoto.properties` utiliza las variables de conexión existentes, `ddl-auto=validate`, escucha en loopback y mantiene los controles de sesión, autorización, origen y privilegios técnicos. HU19 no depende de variables `HU19_TEST_*`: estas solo habilitan pruebas aisladas.

Azure mantiene Java 17.0.21 y PostgreSQL 16.15 en la misma VM, detrás de Nginx/HTTPS. Las credenciales se conservan en el archivo privado del servicio, con permisos 0600; se comprobaron las propiedades de la conexión sin registrar valores secretos. No se cambiaron versiones ni proveedor.

## Base persistente y respaldo

La base real es `senniaf_demo`; su almacenamiento está en `/var/lib/postgresql/16/main`, en el disco ext4 persistente de la VM. Backend, PostgreSQL y Nginx estaban activos. El temporizador de respaldo también estaba activo.

Resultados de lectura:

| Comprobación | Resultado |
| --- | --- |
| Columna `unidad_edad` de 011 | Presente. |
| Tabla `vinculaciones` de 012 | Ausente. |
| Aceptaciones históricas | 1; sin duplicados por participante ni referencias o fechas ausentes. |
| Datos existentes | 11 cuentas, 6 expedientes infantiles, 3 familiares, 2 decisiones, 22 eventos de auditoría. Solo conteos; sin consultar ni publicar contenido sensible. |
| Respaldo más reciente inspeccionado | Copia del 13 de noviembre, aproximadamente 01:18 de Panamá; `pg_restore --list` y lectura completa hacia `/dev/null` correctos, con huella SHA-256 calculada. No se restauró ni se creó una copia nueva. |

No se aplicó 012 a esta base. Antes de hacerlo se necesita un respaldo **nuevo**, verificado y una intervención autorizada con recuperación prevista. El respaldo inspeccionado no se presume suficiente para escrituras posteriores. Las instrucciones concretas se integraron al [procedimiento de Azure](../deploy/azure/README.md).

## Validación actual con PostgreSQL 16 y Java 17

Se prepararon PostgreSQL **16.15** y Java **17.0.21** aislados, sin sustituir instalaciones ni modificar bases existentes. Las herramientas, evidencias y artefactos están excluidos de Git bajo `target/verificacion-hu19`; el directorio de datos temporal de PostgreSQL usa el sistema de archivos Linux porque la carpeta del proyecto en Windows no admite los permisos que exige PostgreSQL. Es estado desechable de pruebas, no configuración ni almacenamiento final del producto.

El mismo script que usará el workflow prepara 001 y las ampliaciones 005–012, confirma el destino y conserva los permisos limitados de `senniaf_app`. Rechaza bases que no sean locales con prefijo `hu19_`, bases con tablas existentes y ausencia de confirmación explícita. No puede apuntarse a `senniaf_demo` mediante el procedimiento documentado.

- `mvn -B verify -Pe2e`, Java 17/PostgreSQL 16.15/cuenta técnica: **234 pruebas unitarias y 18 de integración, 252 en total, sin fallos ni errores ni omitidas**.
- Las 8 pruebas de HU19 vuelven a comprobar aceptación única, reserva, respuesta persistente, rechazo sin penalización, propiedad familiar, profesionales asignados, cabeceras manipuladas y cuatro carreras de concurrencia.
- Se recuperó la suite HTTP histórica para el contrato vigente: registro/verificación, entrevistas, expedientes infantiles, edición/auditoría, ranking explicable y reproducible, exclusión de incompletos, tres reevaluaciones, permisos y aceptación del Comité. Se añadió la evaluación psicológica con dos versiones conservadas. Las cuentas demo y códigos de verificación se habilitan exclusivamente dentro de esta prueba, condicionada a una base aislada.
- 012 se probó sobre datos sintéticos anteriores y en escenarios conflictivos de PostgreSQL 16.15: preservación exacta de decisiones, aborto transaccional por duplicados o referencias inválidas y cancelación por destino incorrecto. Se probaron las restricciones directamente por SQL, sin depender de la interfaz.
- El JAR generado contiene exactamente los recursos actuales de HU19, bytecode Java 17 y el perfil remoto existente. No contiene clases de pruebas ni necesita configuraciones privadas de desarrollo.
- Se arrancó ese **JAR empaquetado** con Java 17, perfil `remoto`, PostgreSQL 16.15 y cuenta sin permisos de administración de esquema: inicio de sesión, consultas autorizadas y cierre de sesión de los cuatro roles correctos; recursos nuevos HTTP 200 y API sin sesión HTTP 401.
- Se comprobaron las huellas y conteos de seis tablas durables antes/después de reiniciar el backend de pruebas. No se afirma persistencia de nuevas vinculaciones en Azure, donde todavía no existen.
- Sintaxis JavaScript y shell correcta. La nueva comprobación del actualizador tiene **6 pruebas Python aprobadas**. Ninguna requiere acceso al servidor real.

Los registros locales están en `target/verificacion-hu19`; no se incluyen bases, herramientas ni credenciales en los commits. La verificación original de 242 pruebas se amplía con las diez pruebas HTTP de módulos anteriores. El cálculo `candidato` y los pesos permanecen idénticos al commit base.

## Despliegue y recuperación

El procedimiento real es copiar un JAR aprobado y ejecutar `/usr/local/lib/senniaf/actualizar.sh`: SHA-256, bloqueo de actualización, respaldo, enlace de versión, reinicio de systemd y comprobación HTTP. No aplica migraciones ni recibe código de GitHub automáticamente. El frontend se entrega con el backend.

El JAR remoto activo tiene SHA-256 `7a06feedf3165f746d69cb12afec861d3b0c2ebecd4dd4b83c6728d58c35e033`; no contiene HU19 ni metadatos de commit. Cuatro recursos estáticos coinciden con `67db4fa`, pero **el commit exacto del binario no puede certificarse** con esa coincidencia. La raíz pública devuelve HTTP 200 y la ruta nueva HTTP 404. No se probaron respuestas familiares de HU19 en el sistema público porque el módulo no está desplegado; tampoco se crearon registros de prueba allí.

Correcciones locales para futuras entregas:

1. El mismo workflow ahora prepara explícitamente el esquema, ejecuta las suites vigentes con PostgreSQL 16/Java 17 y comprueba recursos web y la compatibilidad del actualizador. Pendiente publicarlo y verificar su ejecución en GitHub.
2. Maven incorpora `META-INF/build-info.properties` con commit, rama y condición del árbol. El artefacto local preparado antes de commit se marca `modificado`; no se atribuye falsamente a un commit limpio.
3. El actualizador incorpora `verificar-compatibilidad.py`: rechaza HU19 sin 012 y rechaza volver a un JAR legado si la tabla de vinculaciones existe. Esto evita decisiones nuevas sin su vinculación tras un rollback. Solo comprueba esta incompatibilidad específica; JPA y la revisión de cada futura migración siguen siendo necesarias.
4. Si el JAR anterior es incompatible, una recuperación fallida deja el servicio detenido y los datos intactos para intervención del operador. No restaura ni borra la base automáticamente. La lógica de compatibilidad se probó localmente, sin inducir un fallo en la VM pública.

El actualizador y auxiliar nuevos **no se instalaron en Azure**. Su instalación revisada forma parte de la intervención pendiente, antes de migrar. La comprobación pública disponible durante la auditoría corresponde a la versión anterior.

## Archivos modificados en esta auditoría

| Archivos | Motivo |
| --- | --- |
| `.github/workflows/maven.yml` | Adaptar el mismo CI al esquema y pruebas actuales; validación de la rama de trabajo. |
| `src/test/scripts/preparar-postgres-ci.sh` | Preparación restringida de una base aislada con permisos técnicos. |
| `src/test/java/com/senniaf/match/HU19PostgresIT.java` | Admitir contraseña de la cuenta de pruebas como en CI. |
| `src/test/java/com/senniaf/match/Sprint6E2EIT.java` | Condicionar la suite histórica a una base aislada, corregir auditoría y añadir evaluación versionada. |
| `src/test/java/com/senniaf/match/service/PerfilNinoServiceTest.java` | Precisar un comentario de validación; sin cambio de regla. |
| `pom.xml` | Metadatos de procedencia mediante el plugin Spring Boot ya existente. |
| `deploy/azure/actualizar.sh`, `verificar-compatibilidad.py` | Evitar publicación/recuperación de JAR incompatible con 012. |
| `src/test/scripts/test_compatibilidad_despliegue.py` | Seis casos del control de publicación y recuperación. |
| `.gitignore` | Excluir cachés de Python; mantener herramientas y evidencias fuera de Git. |
| Documentación de proyecto, base, Azure y HU19 | Distinguir validación local, integración persistente y despliegue; documentar el bloqueo de HU11. |

Los cambios funcionales de HU19 presentes al comenzar se conservan en los mismos servicios, modelos, rutas y componentes; no se reconstruyeron módulos ni se implementó HU11.

## Pendientes antes de HU11

- Publicar/revisar los cambios de la rama de trabajo por el flujo autorizado y comprobar el CI del commit publicado.
- Autorizar y ejecutar el mantenimiento del entorno existente: actualizador compatible, respaldo fresco y legible, parada del backend legado, 012 y JAR probado.
- Comprobar con usuarios autorizados la aplicación pública, roles, respuesta familiar, historial y persistencia real. No confundir el 200 de la raíz con esta verificación.
- Mantener la política conservadora de datos infantiles hasta definir institucionalmente qué información adicional se permite compartir.

HU11 reutilizará vinculación, reserva independiente, decisión original y auditoría. No se añadieron interrupciones, cierres ni formularios del Sprint 9 mientras exista el bloqueo de integración persistente.
