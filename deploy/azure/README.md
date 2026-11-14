# SENNIAF Match en Azure — operación de la demostración

URL: https://senniafmatch.westus2.cloudapp.azure.com

Cierre funcional y evidencias PBI22–PBI27: [ACTA_CIERRE_SPRINT7.md](../../docs/ACTA_CIERRE_SPRINT7.md). El DNS actual utiliza la misma IP. En Nginx, `server_names_hash_bucket_size 128` se configura dentro de `http`; el origen permitido está en `/etc/senniaf/remoto.env`. La renovación del certificado anterior quedó archivada y el temporizador conserva la del dominio actual.

Configuración aprobada: West US 2, B2als_v2 Linux, 2 vCPU/4 GiB, SSD Standard LRS de 64 GiB. PostgreSQL y Spring Boot escuchan en loopback; Nginx publica HTTPS. Grupo: `rg-senniaf-match-pbi27`. No cambiar región, tamaño, plan ni añadir servicios facturables sin aprobación. Presupuesto de continuidad aprobado: USD 65 hasta el 25 de diciembre de 2026; no equivale a consumo facturado.

Estos archivos reutilizan las plantillas anteriores, trasladadas a `deploy/azure`; no existe un segundo sistema ni una copia del repositorio. Los ejemplos de configuración permanecen cerrados por defecto. Las credenciales reales están fuera de Git, en `/etc/senniaf/remoto.env` (root, 0600) y en el almacenamiento privado del operador.

## Entrar por SSH

Desde Ubuntu WSL, con la clave privada creada para este servidor:

```bash
ssh -i /home/anyel/.local/state/senniaf-azure-pbi27/ssh_ed25519 \
  -o UserKnownHostsFile=/home/anyel/.local/state/senniaf-azure-pbi27/known_hosts \
  -o StrictHostKeyChecking=yes senniafops@20.94.242.158
```

La clave de host fue obtenida mediante Azure ARM y fijada en `known_hosts`. No aceptar automáticamente otra clave si cambia: contrastarla en Azure. El NSG restringe el puerto 22 a la IP administrativa del despliegue. Si cambia la IP de tu conexión, actualizar únicamente esa regla desde Azure CLI autenticado; no abrir SSH a Internet.

## Actualizar durante próximos sprints

1. Revisar los cambios con el equipo y ejecutar las pruebas apropiadas en una base aislada con datos ficticios. Conservar la rama y el trabajo local. No usar la base habitual para pruebas que escriban ni ejecutar `clean` sobre el directorio desde el que funciona el servicio habitual.
2. Empaquetar la versión revisada con Java 17 y un nombre separado; después de las pruebas, por ejemplo:

   ```bash
   mvn -B package -DskipTests -Dsenniaf.artifact-name=senniaf-match-release-AAAAMMDD
   sha256sum target/senniaf-match-release-AAAAMMDD.jar
   ```

   `-DskipTests` solo empaqueta: no sustituye las pruebas del paso anterior.
3. Copiar únicamente el JAR al servidor, sin archivos `.env`, bases locales, respaldos de desarrollo ni repositorios completos:

   ```bash
   scp -i /home/anyel/.local/state/senniaf-azure-pbi27/ssh_ed25519 \
     -o UserKnownHostsFile=/home/anyel/.local/state/senniaf-azure-pbi27/known_hosts \
     -o StrictHostKeyChecking=yes target/senniaf-match-release-AAAAMMDD.jar \
     senniafops@20.94.242.158:/home/senniafops/nueva-version.jar
   ```

4. Desde SSH ejecutar:

   ```bash
   sudo /usr/local/lib/senniaf/actualizar.sh /home/senniafops/nueva-version.jar SHA256_ESPERADO
   ```

   El script comprueba la huella, serializa actualizaciones, respalda PostgreSQL y sube la copia privada antes de intervenir, conserva versiones por SHA, activa el enlace, reinicia systemd y comprueba salud. Una versión existente se verifica sin reescribir su JAR activo. Si falla, intenta recuperar el JAR anterior. No crea ni restaura automáticamente bases ni migra el esquema. Una migración necesaria debe revisarse y ejecutarse explícitamente antes de la versión compatible; volver al JAR anterior requiere un esquema compatible.
5. Comprobar HTTPS, login de los cuatro roles, alcance de expedientes, pesos y operación afectada por el sprint. Cerrar las sesiones de prueba. No publicar hasta resolver fallos de seguridad. Revisar `sudo journalctl -u senniaf.service` sin copiar secretos al chat.

La actualización real de systemd se comprobó en esta VM con el mismo artefacto, respaldo externo y comprobación de salud. El rollback por fallo inducido cuenta con la validación simulada anterior, sin ensayo destructivo sobre el servicio público.

## Copias y restauración

`/usr/local/lib/senniaf/respaldo.sh` genera un `pg_dump -Fc`, comprueba su contenido y sube el archivo mediante `subir-respaldo.py`. El temporizador realiza la copia diaria aproximadamente a la **01:15 de Panamá**, con hasta cinco minutos de dispersión y recuperación de ejecuciones omitidas. Comprobarlo con:

```bash
sudo systemctl status senniaf-respaldo.timer
sudo journalctl -u senniaf-respaldo.service
```

Contenedor privado: cuenta `stsenniafpbi279aa1635a`, contenedor `backups`, prefijo `postgres/`. Identidad administrada con permiso de datos limitado a ese contenedor; claves compartidas y acceso público deshabilitados. Política de eliminación de blobs a partir de siete días; su ejecución es asíncrona. Copias locales protegidas en `/var/backups/senniaf` dentro del disco ya presupuestado.

Para recuperar: descargar una copia mediante una identidad autorizada, verificar su SHA-256 y restaurar primero en una base temporal separada. Comparar tablas y filas antes de planificar un cambio de base. No restaurar sobre una base activa ni sustituir escrituras posteriores automáticamente. Se verificó una descarga real desde Blob y la restauración de las 17 tablas con sus filas originales.

## HTTPS y configuración

Certificado Let's Encrypt; `certbot.timer` y el hook `renovar-nginx.sh` renuevan y recargan Nginx. El ensayo real con CA de pruebas terminó correctamente. Conservar la ruta HTTP del desafío ACME del template y no publicar 8080/5432. El acceso HTTP normal redirige a HTTPS.

SMTP ya funciona con Brevo. Se conserva el registro y la verificación originales, código protegido, de un solo uso y con vencimiento. El registro público remoto está habilitado tras la recepción real aprobada y la autenticación SMTP desde Azure. No devolver códigos, activar el inicializador demo ni habilitar depuración de correo. Los usuarios públicos siempre son FAMILIA. No enviar nuevos correos de prueba sin autorización.

## Costos y continuidad hasta el 25 de diciembre

El objetivo vigente es **USD 65 en total** hasta terminar el 25 de diciembre de 2026. Proyección histórica documentada para toda la infraestructura existente: USD 59,02/48 días; con margen del 10 %, USD 64,92. [Configuración, pruebas y pendientes](../../docs/ACTA_CIERRE_SPRINT7.md).

Se reutilizó el presupuesto `SENNIAF-PBI27-42` (identificador histórico), cuyo importe actual es USD 65 y período anual con vencimiento el 26 de diciembre a las 00:00 de Panamá; no se reinicia mensualmente. Avisos de costo USD 40/50/60 y previsión USD 50/60, mediante el correo de la cuenta Azure en `ag-senniaf-continuidad`. El usuario confirmó recepción del alta y de la alerta real de reinicio. Azure registró el disparo y la ejecución del grupo. La función de pruebas sintéticas no está disponible en esta oferta Students.

Las reglas `senniaf-salud-vm` y `senniaf-operaciones-vm` son informativas, sin acciones automáticas sobre la VM. Activity Log/Resource Health y el volumen esperado de correos se mantienen dentro de las prestaciones gratuitas. No hay Log Analytics ni pruebas web de pago.

No se programaron apagados, suspensión o eliminación. Mantener el límite de gasto Students y no actualizar el plan. Revisar consumo en Education Hub/Sponsorships o Usage Details: el consumo liquidado debe consultarse en la cuenta autorizada; no se presume un saldo vigente desde este documento. El **25 de diciembre** revisar con el usuario la continuidad y el presupuesto; el vencimiento del presupuesto no detiene la VM y los recursos seguirán facturando hasta una decisión explícita. El disco, IP y Blob también pueden facturar con la VM desasignada. No borrar recursos o datos automáticamente.

## Esquema y datos

Los scripts 001, 002 y 005–010 conservan el esquema y permisos técnicos. Los cambios 006–008 están en la carpeta histórica `sql/propuestas/`, aunque ya fueron autorizados y aplicados en esta VM. Ninguno se ejecuta desde `actualizar.sh`. Los scripts privados de carga 003/004 no se publican ni son necesarios para actualizar el servicio existente. No se incluyen registros o respaldos en Git.

## Publicación de ajustes finales del 10 de noviembre de 2026

La VM utiliza `senniaf-match-ajustes-finales-20261110-e1af49e.jar`, correspondiente al [commit de implementación `e1af49e`](https://github.com/eeeamir/senniaf_match/commit/e1af49eadc9fbf7eb5b727cb21880690a803237a), con SHA-256 `16eb51cb070c0dc42c3377a0adfde7473378639c9ab492a9ea63e6202c9b6932`. Se publicó con `actualizar.sh`; `/srv/senniaf/current.jar` apunta a `/srv/senniaf/releases/16eb51cb070c0dc42c3377a0adfde7473378639c9ab492a9ea63e6202c9b6932.jar`. La versión anterior `senniaf-match-revision-demo-20261110.jar`, SHA-256 `7d0a92f56402c7dbc7482897ac4914f19cbdf23f587476693cdce413aae9517a`, permanece intacta en el directorio de versiones.

Las migraciones `009-formularios-coherentes.sql` y `010-documentos-identidad.sql` están aplicadas en `senniaf_demo`. La 010 añade dos columnas opcionales y un índice único para nuevos documentos; todas las cuentas anteriores conservan ambos campos nuevos en NULL. No convierte documentos ni modifica identificadores, credenciales o relaciones. La 009 conserva su ampliación de formularios y restricciones para nuevas escrituras.

Antes de la 010 se generó `/var/backups/senniaf/20261111T031732Z.rY1Ae4.dump`, SHA-256 `e29274e3df0ed30b6befd57dc3235daceadb460f86fd1e82baa49bfa9b5e4be1`. El actualizador generó `/var/backups/senniaf/20261111T032040Z.yLF7KE.dump`, SHA-256 `7ea9e69f93892e491d92a38068892f0b622802eeb236a181500529c2517df973`, antes de sustituir el JAR. Las fechas de los nombres son UTC. Ambas copias privadas se leyeron completas con `pg_restore --file=/dev/null` y se verificó el SHA-256 de su descarga desde Blob, sin restaurar sobre ninguna base. Se conservaron los respaldos anteriores.

La aplicación sigue usando PostgreSQL local y la misma base. Los datos residen en `/var/lib/postgresql/16/main`, sobre el disco administrado persistente Standard SSD LRS de la VM; no se cambió de base ni se reinició PostgreSQL. La lectura de integridad anterior y posterior conserva conteos y SHA-256 de las 16 tablas de datos durables (excluye `sesiones_acceso`), descontando únicamente las dos columnas nuevas: 11 cuentas, 6 expedientes infantiles, 3 entrevistas familiares, 7 evaluaciones psicológicas, 19 entradas de auditoría y una solicitud pendiente. No se crearon cuentas ni registros de demostración durante esta publicación.

En esta demostración se habilitó expresamente `SENNIAF_DEMO_ASIGNACION_AUTOMATICA=true`, conservando una copia privada de la configuración anterior. Solo afecta a nuevas altas y distribuye psicología según la carga de casos; no reasigna expedientes anteriores ni representa una política institucional. No se cambiaron credenciales ni opciones de seguridad. Las credenciales de cuentas de demostración permanecen fuera del repositorio.

## Revisión integral de HU19 — 13 de noviembre de 2026

La inspección administrativa de solo lectura confirmó PostgreSQL **16.15**, Java **17.0.21**, base persistente `senniaf_demo` y datos en `/var/lib/postgresql/16/main`, sobre el disco de la VM. La migración 011 está representada por la columna `unidad_edad`; **012 todavía no está aplicada**. El JAR activo no contiene HU19 y `/vinculaciones.html` devuelve 404. El SHA-256 del JAR en ejecución es `7a06feedf3165f746d69cb12afec861d3b0c2ebecd4dd4b83c6728d58c35e033`. No contiene metadatos de commit: cuatro recursos web coinciden exactamente con `67db4fa`, pero esa coincidencia no certifica todo el binario.

No existe una rama que despliegue automáticamente: el workflow remoto actual solo prueba cambios de `main`; Azure recibe un JAR mediante el procedimiento manual anterior. La ampliación local del mismo workflow valida también `sprint7-desarrollo`, prepara un esquema aislado con PostgreSQL 16 y ejecuta las suites unitarias y HTTP. Hasta publicarla, no está activa en GitHub. No añadir datos de pruebas a la base persistente para activar HU19.

Antes de desplegar HU19, preparar una intervención autorizada:

1. Publicar y revisar el commit de la rama de trabajo. Empaquetar con Java 17, conservar el SHA-256 y comprobar los metadatos. En una copia sin cambios pendientes:

   ```bash
   mvn -B package -DskipTests \
     -Dsenniaf.artifact-name=senniaf-match-release-AAAAMMDD \
     -Dsenniaf.commit="$(git rev-parse HEAD)" \
     -Dsenniaf.branch="$(git branch --show-current)" \
     -Dsenniaf.worktree=limpio
   ```

   Marcar `limpio` solo después de comprobar `git status`, incluidos archivos nuevos. Las pruebas deben haber pasado antes. `META-INF/build-info.properties` registra commit, rama y condición del árbol; el artefacto con cambios locales debe marcarse `modificado`, nunca atribuirse a un commit limpio.
2. Actualizar expresamente **el actualizador existente y su nuevo auxiliar** `verificar-compatibilidad.py` en `/usr/local/lib/senniaf/`, con revisión del operador. No se han copiado ni instalado en Azure durante esta auditoría. El auxiliar usa solo Python estándar, ya disponible. Sin él, el actualizador nuevo cancela antes de tocar el servicio.
3. Generar un respaldo nuevo mediante el procedimiento existente, comprobar SHA y lectura completa, y acordar la recuperación. La lectura correcta de un respaldo anterior no garantiza que incluya escrituras posteriores. No restaurar sobre la base activa automáticamente.
4. Detener el backend anterior durante la conversión: no permitir nuevas decisiones del Comité entre migración y activación del JAR. Revisar conflictos de aceptaciones y ejecutar manualmente 012 con `ON_ERROR_STOP=1`, `base_autorizada` y `confirmacion=CREAR_VINCULACIONES_SIN_BORRAR`, usando el propietario del esquema. Conservar sus permisos a `senniaf_app`; no cambiar credenciales. No ejecutar el esquema 001 sobre esta base.
5. Usar el mismo `actualizar.sh` con el JAR y SHA revisados. El nuevo precontrol rechaza HU19 sin su tabla y rechaza un JAR anterior si la tabla ya existe. No migra el esquema por su cuenta.
6. Si falla el arranque, solo recuperar automáticamente un JAR compatible con vinculaciones. Si el anterior es legado, **el servicio queda detenido**, preservando la base para intervención del operador. Aunque 012 sea aditiva, el JAR legado podría aceptar propuestas sin crear vinculaciones: no es una recuperación funcional segura. La comprobación cubre esta incompatibilidad de HU19; las futuras migraciones deben revisar también sus requisitos de recuperación.
7. Verificar HTTPS, acceso autenticado de los cuatro roles, respuestas familiares, alcance profesional, auditoría y persistencia. Contrastar la huella remota con el artefacto aprobado y leer sus metadatos de compilación sin imprimir el entorno privado.

Ninguno de estos pasos operativos se ha aplicado durante la revisión. La inspección de la VM se realizó mediante Run Command de Azure porque SSH agotaba el tiempo de conexión; no se abrió SSH a Internet ni se cambiaron reglas de red.


## Sprint 8 integrado — preparación local, sin publicación

La implementación posterior validó HU19, HU11, HU12, HU14 y HU13 en orden, con commits locales separados. La petición vigente permitió continuar el desarrollo local sin esperar a Azure. Esto sustituye el bloqueo para desarrollar HU11 indicado en la auditoría histórica; no autoriza cambios en el servidor. La consulta pública final sigue mostrando raíz 200 y `/vinculaciones.html` 404. No se ejecutaron migraciones, reinicios ni despliegues remotos.

La entrega funcional probada corresponde al commit **9d6b06d** de `sprint7-desarrollo`, con 273 pruebas Java y ocho del actualizador aprobadas. Artefacto local `target/senniaf-match-sprint8-validacion-9d6b06d.jar`, SHA-256 **bd9fe9382da347a9b5bb0efa44aa489f14808f9ba37e0b931d685391c8690cec**. Sus metadatos registran commit, rama y árbol limpio. Los cambios posteriores de cierre son documentación; cualquier cambio de código requiere una nueva validación y artefacto identificado. Ver [resultados](../../docs/SPRINT8_RESULTADOS.md) y [matriz](../../docs/SPRINT8_MATRIZ_ACEPTACION.md).

Para una intervención futura autorizada, ampliar el procedimiento anterior:

1. Revisar/publicar los commits y confirmar la versión exacta del JAR y su SHA. Contrastar Java 17, PostgreSQL 16 y el esquema real antes de actuar. El artefacto contiene HU11 y requiere 012 **y 013**; el control local `python3 deploy/azure/verificar-compatibilidad.py archivo.jar t t` comprueba esa etapa, pero no sustituye la validación del esquema.
2. Instalar conjuntamente las versiones revisadas de `actualizar.sh` y `verificar-compatibilidad.py`; el auxiliar debe incluir el control de interrupciones. El script consulta ambas tablas y comprueba también la compatibilidad del candidato de recuperación. No aplicar únicamente una de las dos piezas.
3. Generar y verificar un respaldo nuevo de la base persistente con el procedimiento existente, conservando copia privada externa, SHA y lectura completa. Conservar el JAR anterior sin alterarlo. Acordar una recuperación compatible antes de convertir el esquema.
4. Detener el backend anterior para impedir decisiones durante la conversión. Aplicar manualmente 012 con `base_autorizada` y `confirmacion=CREAR_VINCULACIONES_SIN_BORRAR`; analizar cualquier conflicto sin borrar antecedentes. Después aplicar `013-interrupciones-convivencia.sql` con `base_autorizada` y `confirmacion=AGREGAR_INTERRUPCIONES_SIN_BORRAR`, usando `ON_ERROR_STOP=1` y el propietario del esquema. Conservar las credenciales y permisos de `senniaf_app`. Nunca ejecutar 001 ni restaurar una base de desarrollo sobre el destino persistente.
5. Publicar el JAR revisado mediante el actualizador existente, sin carga de datos sintéticos. Si no hay un JAR de recuperación compatible con las dos migraciones, conservar el servicio detenido para intervención del operador ante un fallo. No volver a un JAR de HU19 que oculte interrupciones, ni al legado que omite vinculaciones. Las tablas aditivas no se borran automáticamente para forzar una recuperación.
6. Verificar HTTPS, los cuatro roles, asignación profesional, privacidad familiar, historial, respuestas, revisión de interrupciones, apoyos culturales, alertas y pesos/versiones. Confirmar persistencia y huellas/conteos de los datos que deban conservarse. Verificar la huella del JAR activo y sus metadatos, sin imprimir secretos.

Ninguno de estos pasos ha sido ejecutado en Azure. El ensayo de arranque y persistencia del JAR se limita al entorno local sintético.


## Publicación del Sprint 8 — 14 de noviembre de 2026

El procedimiento autorizado aplicó 012/013/014 sobre `senniaf_demo` con respaldo reciente verificado y backend detenido; luego el actualizador existente publicó el JAR CI del commit `4d19bc2`. La ruta Vinculaciones ya responde 200 y se comprobaron autenticación, consultas y permisos de cuatro roles. Las tablas históricas conservan conteos y huellas. El cierre institucional y la revisión separada de disponibilidad requieren 014; el auxiliar actualizado impide recuperar un JAR que omita esas columnas.

El informe vigente, respaldos, pipelines, commit/SHA y límites de comprobación están en [SPRINT8_INTEGRACION_DESPLIEGUE.md](../../docs/SPRINT8_INTEGRACION_DESPLIEGUE.md). Las secciones anteriores se conservan como antecedentes de las versiones previas.


## Refuerzos posteriores: 015 pendiente de publicación

La programación a las 07:00 de Panamá y la exclusión de pares rechazados requieren la migración 015 y el actualizador/auxiliar revisados conjuntamente. El auxiliar comprueba cuatro etapas: vinculaciones, interrupciones, cierre y refuerzos (`t t t t`). No recuperar un JAR anterior tras aplicar 015: ignoraría tanto las programaciones como las exclusiones. Esta ampliación está validada localmente; no se desplegó ni se migró Azure durante su desarrollo. Ver [informe técnico y pruebas](../../docs/HU13_PROGRAMACION_Y_RECHAZOS.md).
