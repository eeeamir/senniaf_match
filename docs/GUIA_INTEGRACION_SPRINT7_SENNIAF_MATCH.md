# SENNIAF Match — Guía de integración y continuidad del Sprint 7

Actualización: 9 de noviembre de 2026, Panamá. Documento de entrega para el equipo y sus agentes. Referencias consultadas: `SENNIAF_Match_Sprint7.docx` y la guía previa facilitada por el equipo. El DOCX se conserva en el archivo académico; no se publican sus datos personales o económicos en este repositorio.

## 1. Qué se entrega y qué falta verificar

SENNIAF Match es un prototipo académico que reúne expedientes y apoya el análisis de compatibilidad. No es una plataforma oficial de SENNIAF, no decide adopciones y utiliza exclusivamente información sintética. La puntuación orienta el análisis humano del Comité.

Se entregan HU04–HU08 (PBI22–PBI26), PBI27 y las mejoras familiares y visuales aprobadas. Se preservan los seis commits preparados (`7a3dc55`, `5b3e065`, `9f9fbd5`, `7377bb8`, `2342ef6`, `fe1f976`) y el aporte original `b684f775` del equipo. No se reemplazó el proyecto ni se reescribió su historial.

**Estados de evidencia:**

- **Implementación:** comprobada por lectura de controladores, servicios, DTO y formularios reales.
- **Compilación:** Maven sin compilar ni ejecutar pruebas (`-Dmaven.test.skip=true`). Compilar no certifica los recorridos.
- **Operación:** verificaciones de servicios, JDBC, esquema, huella del JAR y recursos HTTPS.
- **Prueba funcional manual:** pendiente en esta entrega, incluida la carga del ejemplo psicológico, su guardado y edición. No se abrió navegador ni se generaron registros de prueba para aparentar aprobación.

La matriz de la sección 9 relaciona los criterios con la evidencia que deben reunir las compañeras. No atribuir resultados actuales a ejecuciones históricas.

## 2. Arquitectura y tecnologías

Flujo remoto: navegador → HTTPS/Nginx → Spring Boot en loopback → PostgreSQL local a la VM. La base es remota respecto de las computadoras del equipo, pero PostgreSQL y Java están en el mismo servidor de Azure.

| Componente | Versión/configuración comprobada |
| --- | --- |
| Desarrollo WSL | Java 21.0.12.1 y Maven 3.9.12; destino de compilación Java 17 |
| Backend | Spring Boot 3.5.16, Spring MVC, Validation, JPA/Hibernate y Mail |
| Frontend | HTML, CSS y JavaScript del proyecto, servido por Spring Boot; sin aplicación independiente |
| Runtime Azure | OpenJDK 17.0.20.1; Ubuntu 24.04.5 LTS |
| Base remota | PostgreSQL 16.15, `senniaf_demo` |
| Proxy | Nginx 1.24.0, HTTPS y renovación mediante Certbot |

Las versiones de dependencias se fijan en [pom.xml](../pom.xml). No actualizarlas por conveniencia sin comprobar compatibilidad y alcance.

### Rutas del código

Prefijo Java: `src/main/java/com/senniaf/match/`.

| Responsabilidad | Archivos principales |
| --- | --- |
| Arranque y entorno | `SenniafMatchApplication.java`, `config/DatosDemoConfig.java`, `config/SeguridadRemotaConfig.java` |
| Identidad y sesión | `controller/AuthController.java`, `service/UsuarioService.java`, `service/SesionAccesoService.java`, `security/SesionFiltro.java` |
| Protección HTTP | `security/ProteccionHttpFiltro.java`, `security/LimiteIntentos.java`, `config/SecurityConfig.java` |
| Expediente y seguimiento social | `controller/PerfilNinoController.java`, `service/PerfilNinoService.java`, `model/PerfilNino.java`, `dto/PerfilNinoRequest.java`, `repository/PerfilNinoRepository.java` |
| Evaluación psicológica | `model/EvaluacionPsicologica.java`, `dto/EvaluacionPsicologicaRequest.java`, `dto/EvaluacionPsicologicaResponse.java`, `dto/ContextoEvaluacion.java`, `repository/EvaluacionPsicologicaRepository.java`; operaciones en `PerfilNinoService` |
| Solicitudes de estado | `model/SolicitudEstadoCaso.java`, sus DTO y repositorio; operaciones en `PerfilNinoController`/`PerfilNinoService` |
| Compatibilidad y decisiones | `controller/ScoringController.java`, `service/ScoringService.java`, `model/PesosScoring.java`, `model/DecisionComite.java` y repositorios respectivos |
| Entrevista familiar | `controller/PerfilFamiliaController.java`, `service/PerfilFamiliaService.java`, `model/PerfilFamilia.java`, sus DTO y repositorio |
| Historial | `model/EventoCaso.java`, `repository/EventoCasoRepository.java`, `service/HistorialCasoService.java`, `web/FechaConZonaSerializer.java` |
| Correo | `service/CorreoVerificacionService.java`; configuración por variables externas |

Frontend: `src/main/resources/static/`.

- `index.html`: acceso, registro y verificación familiar.
- `funcionario.html`: inicio profesional.
- `perfil.html`: formularios originales de infancia y familia; precarga, validaciones, edición y ejemplos.
- `perfiles.html`: listado, expediente, psicología, gestión e historial.
- `match.html`: entrada del Comité y familia; refresco familiar de 15 segundos.
- `comite.js`: compatibilidad, decisiones, solicitudes y pesos.
- `familia.js`: inicio, entrevista propia, propuesta e historial familiar.
- `contexto-evaluacion.js`: métodos, instrumentos, apoyos, carga/precarga y presentación psicológica.
- `profesionales.js`/`profesionales.css`: navegación de los cuatro roles, iconos, formularios, tarjetas y modales compartidos.
- `senniaf.js`/`senniaf.css`: sesión, peticiones y estilos comunes. Los estilos anteriores del equipo permanecen.

### Seguridad y roles

Solo existen trabajador social, psicólogo, Comité y familia. `SesionFiltro` valida el token contra PostgreSQL y sustituye las cabeceras de identidad/rol: declararlas desde el cliente no concede acceso. Sesiones ausentes o inválidas producen 401; el rol o vinculación incorrectos producen 403 en las API protegidas.

Los HTML estáticos pueden responder 200 antes de iniciar sesión. La protección de datos corresponde a las API, por lo que una prueba de URL directa debe comprobar también sus respuestas y la ausencia de datos, no solo el estado del documento HTML.

El trabajador social modifica su información social y presenta solicitudes. Psicología modifica sus propios informes vigentes en casos asignados. El Comité consulta expedientes y decide, sin editar evaluaciones. La familia consulta/actualiza únicamente su entrevista; el registro público no crea roles institucionales.

## 3. Historias, criterios y reglas conservadas

El DOCX compromete 23 SP en PBI22–PBI27. Las decisiones funcionales posteriores del equipo precisan el alcance: no hay reasignación desde los cuatro roles; el registro social ya no exige referencia institucional ni confirmación previa; solo trabajo social responsable solicita retiro/reactivación. No confundir esas decisiones con funciones perdidas.

| Historia | Criterios del DOCX y su implementación | Archivos y datos | Evidencia actual |
| --- | --- | --- | --- |
| HU04 / PBI22 | Apartado separado; solo psicólogo asignado y autor actualiza; lectura autorizada; rechazo de acceso ajeno. Cada actualización crea otra fila/versiona. Sin motivo adicional obligatorio ni apartado visible con ese título. | `PerfilNinoService`, DTO clínicos, `EvaluacionPsicologica`, `ContextoEvaluacion`, `perfiles.html`, `contexto-evaluacion.js`. FK al expediente, autor y fecha del servidor. | Código y compilación; escritura/lectura/edición manual pendientes. |
| HU05 / PBI23 | Creador social queda vinculado; admite otro profesional de psicología mediante vinculaciones previas; Mis expedientes por especialidad; rechazo de casos ajenos incluso por URL. | `PerfilNinoRepository.findAsignados`, `comprobarAsignacion`, campos de responsables. Sin endpoint de reasignación. | Código; matriz manual pendiente. NNA-002 no permite probar seguimiento con sus responsables sin resolver cuentas ausentes. |
| HU06 / PBI24 | Detalle autorizado de infancia/familia y antecedentes; desglose de factores; permisos de consulta. | `ScoringController`/`ScoringService`, `comite.js`, `perfiles.html`, decisiones y contexto histórico. | Código y recursos publicados; recorrido manual pendiente. |
| HU07 / PBI25 | Motivo obligatorio, solicitud pendiente, resolución del Comité con constancia; exclusión solo al aprobar; conservación del historial. | `SolicitudEstadoCaso`, servicios de estado, auditoría. Se distingue suspensión de cierre por adopción. | Código; ciclo de escritura pendiente en entorno aislado. |
| HU08 / PBI26 | Justificación sobre caso desactivado, revisión de elegibilidad, regreso solo al aprobar, historial conservado. | Servicio de resolución: exige perfil completo y rechaza reactivación ordinaria de cierre por adopción. | Código; ciclo de escritura pendiente en entorno aislado. |
| PBI27 | HTTPS, acceso desde otros equipos, sesión/roles y PostgreSQL remoto. Es una actividad técnica de entrega, no una HU adicional. | Perfil `remoto`, Nginx, systemd, PostgreSQL y procedimientos de `deploy/azure/`. | Servicios/JDBC/HTTPS y huella comprobados. Login desde otro equipo y negativas remotas: pendientes de prueba manual actual. |

### Compatibilidad y decisiones

Se conservan los cinco factores del motor: edad, hermanos, salud, red de apoyo y logística. Las familias incompletas o pendientes de reevaluación no participan; tampoco los niños incompletos o excluidos. No se modifican fórmulas ni pesos durante este cierre. Diagnósticos, métodos e instrumentos no cambian automáticamente la puntuación.

El Comité registra `ACEPTADA`, `DESCARTADA` o `REEVALUACION` con justificación. Las transacciones y bloqueos protegen el expediente y los pesos frente a modificaciones concurrentes. Las nuevas decisiones conservan puntaje, versión y contexto histórico; no se reconstruyen antecedentes con la configuración actual. Ajustar pesos exige enteros, suma 100, justificación y versión esperada; conservar la auditoría.

El informe social y los psicológicos permanecen consultables durante una exclusión autorizada del Match. Editar información no reactiva el expediente. La adopción formalizada no se revierte por una reactivación ordinaria.

### Familia y ejemplo psicológico

La edición propia se conserva como en Sprint 6, además de las revisiones solicitadas. Se actualiza la misma fila, sin duplicar expediente. El contador de reevaluaciones aumenta solo al completar una solicitud pendiente. La familia recibe únicamente sus actuaciones autorizadas y la existencia de una propuesta aceptada, sin perfiles infantiles, ranking ni deliberaciones.

El botón psicológico «Cargar datos de ejemplo» rellena métodos, instrumento descriptivo académico, evaluación, áreas, observaciones, valoración, apoyos y recomendaciones. No establece diagnóstico formal. Si hay respuestas, pide confirmación. No llama a POST/PUT, no cambia el expediente/autor/fecha ni el informe vigente. Guardar requiere una acción explícita y los mismos permisos/validaciones; al editar conserva el enlace de revisión. Los ejemplos no acreditan evaluaciones profesionales realizadas.

## 4. Azure existente y operación

| Elemento | Configuración comprobada |
| --- | --- |
| Grupo y región | `rg-senniaf-match-pbi27`, West US 2 |
| VM | `vm-senniaf-pbi27`, `Standard_B2als_v2` |
| Sistema | Ubuntu Server 24.04.5 LTS |
| Java | `/usr/bin/java`, servicio `senniaf.service`, perfil `remoto` |
| Aplicación | `/srv/senniaf/current.jar`, enlace a `/srv/senniaf/releases/<SHA256>.jar` |
| Configuración privada | `/etc/senniaf/remoto.env`, no versionada |
| Nginx | `/etc/nginx/sites-enabled/senniaf` → `/etc/nginx/sites-available/senniaf` |
| Red del proceso | PostgreSQL en `127.0.0.1:5432`; Spring Boot en loopback 8080; Nginx publica 80/443 |
| Respaldos | `/var/backups/senniaf`, temporizador `senniaf-respaldo.timer`, Blob privado `backups` en `stsenniafpbi279aa1635a` |
| HTTPS | Certbot y `certbot.timer`; configuración y hook existentes |

No crear otro servidor, base, dominio ni almacenamiento. No ampliar tamaño, costos o permisos. El presupuesto de continuidad aprobado es histórico y no demuestra el saldo actual; cualquier continuidad posterior requiere coordinación, no apagados automáticos.

En esta revisión se comprobó el registro familiar público y SMTP habilitados en el servidor. Los ejemplos de configuración siguen cerrados por defecto. No habilitarlos en otra instalación sin configurar y verificar correo y seguridad. No publicar sus valores privados.

### Consulta operativa, por SSH autorizado

El operador conserva la huella del host y su propia clave privada. No aceptar automáticamente un host diferente ni publicar la clave. Los ejemplos siguientes se ejecutan **dentro de la VM**:

```bash
sudo systemctl is-active senniaf postgresql nginx
sudo systemctl status senniaf --no-pager
sudo journalctl -u senniaf --since "15 minutes ago" --no-pager
sudo nginx -t
sha256sum /srv/senniaf/current.jar
readlink -f /srv/senniaf/current.jar
systemctl status certbot.timer senniaf-respaldo.timer --no-pager
```

Revisar logs sin pegarlos completos en chats o Git; extraer únicamente mensajes de arranque/error pertinentes y eliminar datos privados. No usar `cat /etc/senniaf/remoto.env` como evidencia pública. La identidad de servicio es dinámica según systemd; no reutilizar la cuenta PostgreSQL como cuenta del sistema.

### Publicar una versión posterior

1. Revisar código, dependencias y esquema. Si el JAR ya coincide y no hay diferencias funcionales, no desplegar por rutina.
2. Compilar con Java 21/Maven, destino 17; no ejecutar pruebas salvo autorización y entorno apropiado. No usar `clean` donde esté funcionando el servicio local.

   ```bash
   mvn -B -Dmaven.test.skip=true -Dsenniaf.artifact-name=senniaf-match-release-AAAAMMDD package
   sha256sum target/senniaf-match-release-AAAAMMDD.jar
   ```

3. Verificar respaldo y compatibilidad. Copiar solo ese JAR a `/home/senniafops/nueva-version.jar` mediante el acceso SSH autorizado. Nunca copiar bases de desarrollo, `.env` o repositorios completos sobre la VM.
4. Confirmar la huella de la transferencia antes de activar:

   ```bash
   sha256sum /home/senniafops/nueva-version.jar
   sudo /usr/local/lib/senniaf/actualizar.sh /home/senniafops/nueva-version.jar SHA256_ESPERADO
   ```

5. El procedimiento existente bloquea actualizaciones simultáneas, respalda, conserva releases por SHA, reinicia solo Spring Boot y comprueba disponibilidad. Si falla, intenta volver al JAR anterior; no restaura ni modifica automáticamente PostgreSQL.
6. Comparar la huella activa con la local y los recursos externos, comprobar servicios y JDBC, y documentar el resultado. Un 200 aislado no acredita login, permisos o persistencia. Planificar las pruebas manuales de la sección 9.

No aplicar migraciones desde el actualizador ni usar `ddl-auto=update` para modificar el servidor. Una reversión de JAR exige esquema compatible y no autoriza restaurar una copia sobre escrituras posteriores. El detalle adicional se conserva en [el procedimiento de Azure](../deploy/azure/README.md).

## 5. PostgreSQL: datos, relaciones y esquema

`senniaf_demo` es la base activa de Azure. `senniaf_match` es la base prevista por Docker Compose local; las bases de auditoría y sus volúmenes son distintas. **Un push a GitHub no copia, migra ni modifica las filas de PostgreSQL.** Tampoco actualiza automáticamente el JAR de Azure.

Spring Boot lee `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` desde el archivo privado del servicio. El perfil remoto usa `ddl-auto=validate`; PostgreSQL acepta la conexión local del proceso Java. No abrir 5432 a Internet ni colocar secretos en instrucciones públicas.

| Tabla | Función y relaciones |
| --- | --- |
| `usuarios`, `sesiones_acceso` | Cuatro roles; verificación, BCrypt y sesiones con hash de token/expiración. Nunca exportar hashes o tokens como evidencia. |
| `perfiles_nino` | Identificador interno NNA, información social, responsables y participación en Match. |
| `evaluaciones_psicologicas` | `perfil_id` enlaza al niño; profesional, fecha y contenidos por registro. `revision_de_id` enlaza al antecedente; número de versión, sin sobrescritura. |
| `solicitudes_estado_caso` | FK al niño; tipo, motivo, solicitante, estado y resolución del Comité. |
| `perfiles_familia` | Entrevista propia del usuario, respuestas, preferencias, estado y contador de reevaluaciones. |
| `decisiones_comite` | Códigos de niño/familia, decisión, responsable, motivo, fecha y contexto de scoring. |
| `pesos_scoring` | Cinco pesos y versión. |
| `auditoria_casos` | Actuaciones por código con actor, fecha y detalles completos en `descripcion text`. |
| `nino_*`, `familia_*` | Colecciones de habilidades, gustos, personalidad, salud y apoyo. No borrar para preparar pruebas. |

Las asignaciones almacenan identificadores profesionales; su presencia no garantiza que exista una cuenta con ese ID. Esa limitación explica el pendiente de NNA-002. No corregirla reasignando ni creando usuarios sin autorización.

### Contexto psicológico

`contexto_evaluacion text` guarda JSON con `formato: 1`, métodos, otro método, instrumentos, resultados, apoyos, otro apoyo y detalle. Se validan listas cerradas, no duplicados, longitudes y coherencia condicional. «Otro» exige especificación; instrumentos exige identificación y resultados; apoyos seleccionados exigen detalle. Ninguna selección infiere diagnósticos.

Los informes anteriores conservan NULL. Al editar, el contexto se precarga y se guarda en una nueva versión; un cliente anterior que omita el contexto no lo borra de la revisión. Evaluación/observaciones/recomendaciones siguen siendo campos originales; valoración y diagnóstico opcional se conservan en sus columnas propias. El motivo histórico no se exige ni se muestra como apartado del informe.

### Scripts conservados

| Script bajo `deploy/azure/sql/` | Alcance |
| --- | --- |
| `001-esquema-inicial.sql` | Esquema base sin datos: solo para base nueva vacía, no para el servidor activo. |
| `002-permisos.sql` | Privilegios de la cuenta técnica remota; no crea roles de aplicación. |
| `005-cierre-adopcion.sql` | Cierre por adopción y categoría de solicitud. |
| `propuestas/006-valoracion-psicologica-PENDIENTE.sql` | Cuatro columnas opcionales de valoración; ya aplicado en Azure. |
| `propuestas/007-auditoria-completa-AUTORIZADA.sql` | Descripción de auditoría a text; ya aplicado. |
| `propuestas/008-contexto-evaluacion-PENDIENTE.sql` | Contexto por versión; ya aplicado. |

La carpeta/nombre `PENDIENTE` es histórico: no implica que deba volver a ejecutarse. Las cargas privadas 003/004, dumps y credenciales están excluidos de Git y no forman parte de las actualizaciones normales.

### Solo lectura y respaldo

Con autorización técnica, desde la VM:

```bash
sudo -u postgres psql -X -d senniaf_demo -v ON_ERROR_STOP=1
```

Dentro de psql:

```sql
BEGIN READ ONLY;
SELECT current_database(), version();
SELECT codigo_caso, perfil_completo, baja_logica FROM perfiles_nino ORDER BY codigo_caso;
SELECT count(*) FROM evaluaciones_psicologicas;
SELECT column_name, data_type FROM information_schema.columns
 WHERE table_schema='public' AND table_name='evaluaciones_psicologicas';
COMMIT;
```

No consultar ni exportar contraseñas, códigos de correo o sesiones. Para comparar integridad, usar cantidades y huellas privadas sin publicar contenido clínico. Las sesiones pueden variar por uso normal; distinguirlas de las tablas de negocio.

El respaldo existente genera formato personalizado `pg_dump -Fc`, comprueba su listado y sube a Blob privado mediante identidad administrada. Antes de intervenciones autorizadas:

```bash
sudo /usr/local/lib/senniaf/respaldo.sh
sudo pg_restore --list /var/backups/senniaf/ARCHIVO_CONFIRMADO.dump >/dev/null
sudo pg_restore --file=/dev/null /var/backups/senniaf/ARCHIVO_CONFIRMADO.dump
```

El último comando decodifica el archivo sin restaurarlo. Conservar su SHA y confirmación privada de carga. Una restauración real debe ensayarse en una base temporal autorizada y nunca sobrescribir la base activa por rutina. Esta guía no autoriza crearla ni ejecutar la restauración. Los respaldos detallados y sus ubicaciones privadas no se publican.

## 6. Estado de la demostración consultado

Lectura realizada el 9 de noviembre de 2026; no equivale a una revisión profesional ni aprobación funcional.

| Caso | Situación comprobada y límites |
| --- | --- |
| NNA-001 | Información revisada completa, responsables con cuentas. Tres versiones psicológicas enlazadas al autor asignado; versión 3 vigente, sin las etiquetas genéricas señaladas. El contexto estructurado de métodos/instrumentos/apoyos aún no fue registrado. No se fabrica para completar la demostración. |
| NNA-002 | Datos sociales revisados completos. Conserva TS-002/PS-015, sin cuentas correspondientes en la base; no tiene evaluación psicológica. Sigue siendo consultable por el Comité autorizado. No reasignar ni atribuir informes a esos IDs. |
| FAM-001 | Entrevista revisada completa; rango declarado 3–12 años y disposición a hermanos. Sin propuesta aceptada almacenada al consultar. |
| NNA-010 | No existe en la base consultada. No se creó. |

No había solicitudes de estado ni decisiones del Comité al consultar. Las pruebas de esos flujos necesitarán escenarios aislados; no inventar decisiones en Azure para presentar un historial lleno. Las referencias técnicas históricas y el primer informe se conservan internamente; la interfaz identifica discretamente el entorno académico.

## 7. Acceso al sistema y pruebas desde otra computadora

1. Abrir **https://senniafmatch.westus2.cloudapp.azure.com/** en el navegador habitual de la compañera. No necesita Java, Docker, PostgreSQL, SSH ni cuenta Azure para usar la aplicación publicada.
2. Confirmar el dominio y que el navegador acepte HTTPS sin advertencias. No continuar ignorando una advertencia de certificado.
3. Solicitar al responsable las credenciales por un canal privado acordado. No pegarlas en Git, capturas, tickets ni conversaciones públicas. No se pueden recuperar contraseñas desde BCrypt; cualquier restablecimiento exige autorización separada.
4. Iniciar sesión con el rol previsto. En trabajo social/psicología abrir Mis expedientes; en Comité revisar compatibilidad y detalle; en familia consultar entrevista y proceso propio.
5. Consultar los casos autorizados. La familia no debe recibir datos infantiles, evaluaciones confidenciales, ranking ni otras familias.
6. Registrar evidencia con identificador de prueba, fecha/hora Panamá, rol, versión de la aplicación, pasos y resultado. Recortar tokens, contraseñas, cookies, correos y datos identificativos. Conservar narraciones clínicas solo en el archivo privado autorizado.
7. Cerrar sesión mediante el modal. Cancelar cierra el modal; confirmar invalida la sesión y vuelve al acceso. La advertencia de pérdida corresponde solo a cambios de formulario realmente no guardados, nunca a registros ya almacenados.

### Cuentas de demostración existentes

| Identificador sintético | Rol |
| --- | --- |
| `TS-001` | Trabajador social |
| `PS-014` | Psicólogo |
| `CM-001` | Comité |
| `99-9999-999998` | Familia adoptante |

El responsable mantiene las contraseñas en su almacenamiento privado de cuentas de demostración; no se incluyen rutas personales o contenidos aquí. Pedir acceso privado al responsable, no copiar su cuenta Microsoft. Las cuentas se comprobaron por lectura de ID/rol/verificación, **no mediante nuevos logins de prueba** en este cierre. Si el pegado desde Bloc de notas falla, comparar localmente el valor mostrado sin compartirlo; la causa de ese incidente sigue sin reproducirse.

## 8. Preparación segura de pruebas

Las pruebas de consulta pueden hacerse en la demostración con autorización, sin modificar sus casos. Las pruebas de escritura deben prepararse en una **base aislada con información sintética**, separada del volumen local del equipo y de `senniaf_demo`.

La compañera debe coordinar una instancia/puerto propios, base vacía de pruebas, credenciales privadas y los mismos scripts de esquema. Mantener `ddl-auto=validate`. Configurar la aplicación de prueba para esa base y comprobar `current_database()` antes de escribir. No copiar un respaldo de Azure, habilitar DDL automático sobre una base existente ni reutilizar el volumen original. La preparación de cuentas institucionales y vinculaciones debe hacerse mediante fixtures privadas autorizadas, no mediante registro público ni un módulo administrativo inexistente.

Escenario mínimo aislado: social A con un caso, social B con otro, psicólogo A asignado al primero, otro psicólogo ajeno, Comité y dos familias. Incluir un caso activo completo, otro temporalmente excluido y uno cerrado por adopción. Las fixtures son sintéticas y no se versionan como exportaciones del servidor. Respaldar la base aislada antes de escenarios que cambien decisiones o pesos.

Las credenciales de demostración de Azure no sustituyen estas fixtures. No se crean cuentas ni cambian responsables en producción como parte de esta guía.

## 9. Matriz de pruebas funcionales para el equipo

**Todas las filas manuales están pendientes de ejecución actual.** C = consulta; L = cambios solo locales en el formulario; E = escritura, ejecutar en entorno aislado. Los resultados siguientes son esperados, no resultados observados. «Evidencia» significa captura o registro privado redactado; no subir tokens o datos personales.

| ID / criterio | Tipo y rol | Preparación y pasos | Resultado esperado | Evidencia necesaria | Estado |
| --- | --- | --- | --- | --- | --- |
| E01-01 / HU04 CA01 | C · PS | Caso asignado con información social. Abrir Psicología y Social. | Apartados distintos, sin mezclar contenidos. | Capturas de ambos apartados. | Pendiente |
| E01-02 / HU04 CA02 | E · PS | Caso propio aislado. Completar todos los campos, incluidos Otro e instrumentos cuando correspondan; guardar y reabrir. | Autor/fecha del servidor; valores recuperados iguales, sin diagnóstico automático. | Antes/después, ID de informe y respuesta sin secretos. | Pendiente |
| E01-03 / HU04 CA02 | E · PS | Abrir vigente; Editar, modificar y guardar sin motivo adicional. | Nueva versión, antecedente intacto, nueva actuación y precarga completa. | Ambas versiones y línea de tiempo. | Pendiente |
| E01-04 / HU04 CA03 | C · TS y Comité | Abrir caso autorizado con informe. | Lectura del contenido; sin edición clínica. | Pantalla por rol. | Pendiente |
| E01-05 / HU04 CA04 | C/E · PS ajeno y TS | Solicitar consulta/actualización de evaluación sin autorización usando sesión propia en una herramienta autorizada. | API 403, sin modificación. Sin sesión: 401. | HTTP redactado y comprobación privada de integridad. | Pendiente |
| E01-06 / contexto | E · PS | En base aislada enviar Otro sin detalle, instrumento sin resultados, opciones repetidas o no permitidas y longitudes inválidas. | Rechazo de validación, sin nuevo informe. | Campo/mensaje y HTTP 400 cuando se pruebe API. | Pendiente |
| E01-07 / diagnóstico | E · PS | Intentar texto formal junto con «No se establece diagnóstico». | Formulario impide la contradicción y servidor rechaza una petición incoherente. | Mensaje y rechazo API. | Pendiente |
| E02-01 / HU05 CA01 | E · TS | Registrar caso aislado con todos los campos originales. | Mismo identificador interno NNA generado; creador como responsable social, sin psicólogo automático. | Resumen, autor y registro en Mis expedientes. | Pendiente |
| E02-02 / HU05 CA02 | C · TS/PS | Fixture con profesionales diferentes ya vinculados. | Cada especialidad ve su parte; no existe interfaz de reasignación. | Identificadores de fixture y pantallas. | Pendiente |
| E02-03 / HU05 CA03 | C · TS y PS | Dos profesionales con casos distintos. Abrir Mis expedientes. | Solo aparecen casos propios; activos/excluidos diferenciados. | Listados por rol. | Pendiente |
| E02-04 / HU05 CA04 | C/E · profesional ajeno | Abrir URL directa de caso ajeno y consultar su API. | No se revelan datos; API 403. HTML 200 no concede acceso. | URL sin secretos y respuesta API. | Pendiente |
| E02-05 / especialidad | E · TS y PS | Intentar editar la sección ajena en base aislada. | Backend 403; información previa intacta. | Rechazo e integridad. | Pendiente |
| E03-01 / HU06 CA01 | C · Comité | Abrir propuesta y ambos expedientes autorizados. | Detalles sociales, psicológicos y familiares disponibles; retorno coherente. | Capturas y códigos sintéticos. | Pendiente |
| E03-02 / HU06 CA02 | C · Comité | Desplegar cálculo de compatibilidad. | Cinco factores, valores/razones y total desde el motor. | Desglose. | Pendiente |
| E03-03 / HU06 CA03 | C · familia/rol no permitido | Intentar consultar detalle del Comité sin autorización. | API 403 o 401 según sesión; sin datos confidenciales. | Respuesta redactada. | Pendiente |
| E03-04 / decisión | E · Comité | Propuesta aislada. Cancelar modal; después confirmar con justificación válida. | Cancelar no guarda; confirmar registra decisión, autor, fecha y fundamento. | Conteo/evento antes y después. | Pendiente |
| E04-01 / HU07 CA01 | E · TS responsable | Solicitar suspensión temporal con motivo. Probar también vacío. | Vacío rechazado; válida queda pendiente, sin cambiar Match. | Solicitud y participación. | Pendiente |
| E04-02 / HU07 CA02 | E · Comité | Resolver una petición con rechazo y otra con aprobación. | Decisión y motivo quedan registrados; no puede resolver un rol ajeno. | Resoluciones y HTTP negativo. | Pendiente |
| E04-03 / HU07 CA03 | E · Comité/TS | Comparar recomendaciones antes y después de aprobación. | Exclusión solo tras aprobación, sin borrado. | Estado y listado del Match. | Pendiente |
| E04-04 / HU07 CA04 | C/E · TS/PS | Caso excluido, profesionales asignados. Consultar y guardar seguimiento aislado. | Historial/evaluaciones disponibles; editar no lo reincorpora. | Informe, historial y ausencia del ranking. | Pendiente |
| E05-01 / HU08 CA01 | E · TS | Caso temporalmente excluido. Solicitar con motivo; probar motivo vacío. | Válida pendiente; vacía rechazada; sigue excluido. | Solicitud y estado. | Pendiente |
| E05-02 / HU08 CA02 | E · Comité | Intentar aprobar caso incompleto o cierre por adopción. | Rechazo de elegibilidad, sin reactivación ordinaria indebida. | Error y estado posterior. | Pendiente |
| E05-03 / HU08 CA03 | E · Comité | Rechazar una solicitud; aprobar otra elegible. | Rechazo conserva exclusión; aprobación permite regresar al Match. | Decisiones y recomendaciones. | Pendiente |
| E05-04 / HU08 CA04 | C · autorizados | Consultar caso reactivado. | Solicitud, resolución y antecedentes conservados. | Línea de tiempo. | Pendiente |
| E06-01 / PBI27 | C · equipo | Abrir URL HTTPS desde otra computadora y comparar dominio/certificado. | Sitio accesible sin advertencias. | Captura fechada del otro dispositivo. | Pendiente manual; HTTPS operativo comprobado |
| E06-02 / PBI27 | C · cuatro roles | Iniciar sesión y abrir una pantalla autorizada por rol. | Sesiones y datos correctos; PostgreSQL remoto confirmado por operador. | Pantallas sin credenciales y constancia operativa. | Pendiente manual |
| E06-03 / sesión | C · equipo | Consultar API sin sesión y tras cerrar sesión; probar sesión inválida en herramienta autorizada. | HTTP 401, sin información privada. | Respuestas redactadas. | Pendiente |
| FAM-01 / entrevista | E · familia | Caso aislado completo. Editar entrevista precargada, cambiar situación, guardar/reabrir. | Mismo caso/relaciones, datos recuperados y auditoría; no requiere petición previa. | Identificador y diferencias. | Pendiente |
| FAM-02 / reevaluación | E · Comité/familia | Solicitar reevaluación; familia corrige y envía. | Contador aumenta una vez al completar la solicitud; abrir/cancelar no aumenta. | Contador y actividad. | Pendiente |
| FAM-03 / cancelación | L · familia | Editar y cancelar; repetir sin cambios. | Cambios no guardados piden confirmación; sin cambios vuelve directamente; base intacta. | Modal y consulta posterior. | Pendiente |
| FAM-04 / propuesta | E/C · Comité/familia | Aceptar propuesta aislada; observar familia elegida y otra familia. | Solo la elegida ve aceptación; refresca en 15 s sin datos infantiles/ranking. | Pantallas de ambas sesiones. | Pendiente |
| EJ-01 / ejemplo psicológico | L · PS | Formulario nuevo y luego uno precargado. Pulsar Cargar ejemplo; cancelar/confirmar reemplazo. | Solo cambia formulario tras confirmación; ninguna escritura antes de Guardar. Sin diagnóstico automático. | Red del navegador sin POST/PUT y campos. | Pendiente |
| EJ-02 / ejemplo y versión | E · PS | En base aislada, revisar ejemplo, guardar, reabrir y editar. | Contexto completo recuperado y nueva versión sin sobrescribir antecedente. | Informe y versiones. | Pendiente |
| EJ-03 / ejemplos sociales/familiares | L · TS/familia | Cargar ejemplo con respuestas previas. | Confirmación; conservar campos/validaciones; no guardar automáticamente. | Modal y red sin escritura. | Pendiente |
| PES-01 / criterios | E · Comité | Cambiar pesos aislados con justificación; probar suma/versión inválidas. | Válido auditado; inválido rechazado, sin cambiar pesos. Historial omite pares sin cambio. | Comparación y rechazo. | Pendiente |
| PES-02 / autorización | E · PS, TS y familia | En entorno aislado intentar actualizar criterios con una sesión de cada rol ajeno al Comité. | API 403; pesos y auditoría sin cambios por las operaciones rechazadas. | Respuestas redactadas y huellas privadas antes/después. | Pendiente |
| NAV-01 / interfaz | C/L · cuatro roles | Escritorio/móvil, desplegables, buscador, cierre de sesión. | Navegación real, sin desbordamiento; modales y éxito/error coherentes; Cancelar no guarda. | Capturas y tamaño de pantalla. | Pendiente |

Registrar defectos con ID, rol, preparación, pasos exactos, resultado esperado/observado, versión y evidencia privada. Medir desempeño con ejecuciones reales; no inventar tiempos ni porcentajes de aceptación. La guía histórica del Sprint 6 y sus pruebas siguen conservadas, pero requieren adaptación a los contratos actuales y ejecución posterior autorizada.

## 10. Acceso técnico para mis compañeras

| Necesidad | Acceso requerido |
| --- | --- |
| Usar la aplicación o hacer consultas funcionales | URL HTTPS y cuenta de SENNIAF Match; no necesita Azure Portal. |
| Ver recursos, configuración general y estado en Azure | Cuenta Microsoft propia invitada al tenant cuando corresponda y RBAC de lectura limitado al grupo necesario. |
| Entrar a Linux o ver journal | SSH autorizado, identidad/claves propias y permisos locales; RBAC de lectura no basta. |
| Consultar PostgreSQL | Acceso al host/túnel autorizado y permisos de base de datos; no requiere exponer 5432. |
| Cambiar código | Permisos del repositorio compartido, revisión e integración segura. |
| Publicar/reiniciar servicio | SSH y autorización operativa para el procedimiento y acciones concretas; no requiere compartir la cuenta Microsoft personal. |
| Cambiar infraestructura o accesos | Autorización adicional y rol Azure apropiado; fuera de las pruebas funcionales. |

Procedimiento que el propietario podría coordinar, **sin ejecutarlo automáticamente**:

1. Invitar a la compañera con su propia cuenta Microsoft al directorio, si es externa y la política del tenant lo permite. Debe aceptar la invitación; no compartir contraseña del propietario. [Referencia de Microsoft](https://learn.microsoft.com/en-us/entra/external-id/add-users-administrator).
2. En el grupo autorizado, usar IAM para asignar el alcance necesario. Reader permite consultar el plano de control, sin modificar recursos ni otorgar acceso a los datos. Evitar Owner/Contributor como solución general. [Rol Reader](https://learn.microsoft.com/en-us/azure/role-based-access-control/built-in-roles/general).
3. Para SSH, coordinar una identidad Linux/clave pública propia y permisos mínimos. El acceso comprobado aquí usa SSH con claves; no se ha comprobado la habilitación de inicio de sesión Microsoft Entra en esta VM. Si se eligiera ese mecanismo en el futuro, requiere configuración compatible y roles de login específicos: no se obtiene automáticamente por ser Owner/Contributor. No instalar extensiones ni cambiar la red en este cierre. [SSH con Microsoft Entra](https://learn.microsoft.com/en-us/entra/identity/devices/howto-vm-sign-in-azure-ad-linux).
4. Para PostgreSQL, autorizar una cuenta/rol de base de datos separado según la tarea. RBAC Azure, SSH, PostgreSQL y los cuatro roles de la aplicación son ámbitos distintos. No dar credenciales de la aplicación o privilegios de superusuario para una consulta habitual.
5. Para publicar, acordar responsable, respaldo, huella y ventana breve de reinicio de Java. No conceder accesos, modificar NSG o crear usuarios desde esta guía.

## 11. Reglas para próximos agentes de Codex

- Leer primero la guía, `git status`, fuentes y dependencias reales. Diferenciar implementación de comprobación efectiva.
- Conservar el trabajo de todas las personas, los seis commits previos y los aportes posteriores. Hacer fetch y revisar divergencias; no usar pull destructivo ni force push.
- No duplicar módulos, pantallas, formularios, modelos clínicos o servicios ya existentes.
- No sobrescribir registros, auditoría, versiones, responsables o fechas para preparar una demostración.
- No modificar arbitrariamente algoritmos, pesos, estados, permisos, credenciales o infraestructura.
- No subir dumps, `.env`, sesiones, claves, capturas identificativas o cargas privadas a GitHub.
- Comprobar referencias antes de eliminar archivos. La limpieza anterior y el respaldo privado se conservan; no repetirlos por rutina.
- Distinguir repositorio local (fuentes), GitHub (commits compartidos) y Azure (JAR en ejecución y filas propias). Un push no despliega ni migra la base.
- Compilar y verificar el paquete antes de publicar. Si cambia una función, actualizar Azure solo con autorización y respaldo; si no cambia, evitar reinicios innecesarios.
- No presentar pruebas manuales, automatizadas o visuales como ejecutadas cuando solo se leyó código o compiló.
- No atribuir muestras de formulario a una evaluación profesional real. El usuario debe revisar y guardar explícitamente.
- Coordinar las pruebas que escriben en un entorno aislado; no usar los expedientes de presentación como fixtures desechables.

## 12. Primeros pasos del equipo tras recibir la entrega

1. Abrir la URL, solicitar credenciales por canal privado y comprobar acceso/consulta con cada rol.
2. Anotar el commit de `main` y la huella del JAR que confirme el operador. Revisar el botón psicológico sin guardar sobre casos de presentación.
3. Preparar las fixtures aisladas y ejecutar la matriz, empezando por HU04/HU05 y luego estados/decisiones.
4. Registrar evidencia E01–E06 y los defectos. Resolver con el propietario las cuentas ausentes de NNA-002 y las actuaciones profesionales pendientes, sin inventarlas.
5. Aceptar cada historia solo con su criterio y evidencia real. Esta entrega deja el desarrollo disponible para las pruebas; no sustituye la aceptación del Sprint Review.

## 13. Identificación de esta entrega

Código funcional compilado: commit `5005b9b` (los commits de documentación posteriores no cambian ese árbol funcional). Paquete: `senniaf-match-cierre-sprint7-20261109.jar`. SHA256 esperado: `33921e268adaafd12c646e45eaef8eea85a0213bcea66c5fd42421b31a77e5c3`.

El operador debe contrastar esa huella con `/srv/senniaf/current.jar`. La verificación operativa de publicación no certifica todavía los recorridos de la matriz. Los detalles privados de respaldo y comparación de integridad permanecen fuera de Git.

Comprobación final de esta publicación: Spring Boot, PostgreSQL y Nginx activos, conexión JDBC iniciada y diez recursos HTTPS coincidentes con el paquete. Integridad: 17 tablas y 37 filas sin diferencias antes/después; no se ejecutaron migraciones ni cargas de datos. Respaldo previo verificado y versión anterior conservada para reversión. No se ejercieron guardados de prueba ni recorridos visuales.
