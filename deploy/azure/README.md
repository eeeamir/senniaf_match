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
