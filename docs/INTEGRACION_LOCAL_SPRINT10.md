# Integración local del Sprint 10

La comprobación se hizo en PostgreSQL 18.6, base `senniaf_sprint10_local`, puerto local 55432. La base se preparó de forma aislada con el esquema hasta 021 y tres parejas de expedientes de referencia del catálogo ficticio. El destino de desarrollo predeterminado en el puerto 5432 estaba inactivo.

## Migrar otro entorno local autorizado

Verificar el destino y las tablas con `SELECT current_database(), inet_server_addr(), inet_server_port();`. Consultar `to_regclass('public.seguimientos_vinculacion')` y `to_regclass('public.visitas_domiciliarias')` y comprobar sus tablas de historial. Si aparecen estructuras parciales, inspeccionar el catálogo antes de aplicar una migración.

Con el socket, puerto, usuario y base local ya elegidos en las variables de PostgreSQL, generar y comprobar el respaldo antes de aplicar las migraciones pendientes.

```bash
umask 077
pg_dump -Fc -f respaldo-local-antes-sprint10.dump
pg_restore --list respaldo-local-antes-sprint10.dump
psql -X -v ON_ERROR_STOP=1 -v base_autorizada="$PGDATABASE" \
  -v confirmacion=CREAR_SEGUIMIENTOS_PROFESIONALES -f deploy/azure/sql/022-seguimientos-profesionales.sql
psql -X -v ON_ERROR_STOP=1 -v base_autorizada="$PGDATABASE" \
  -v confirmacion=CREAR_VISITAS_DOMICILIARIAS -f deploy/azure/sql/023-visitas-domiciliarias.sql
```

La carga ficticia usa una transacción y claves de identificación estables. Solicita la contraseña únicamente al crear la cuenta. El código se asigna según la numeración del destino y los registros previos se conservan.

```bash
python3 tools/sprint10/cargar-demo-local.py --socket /ruta/al/socket \
  --puerto 55432 --base senniaf_sprint10_local --administrador usuario_local
```

En la instancia aislada, la carga devolvió `FAM-004` y `NNA-004` en dos ejecuciones. Las huellas de las filas anteriores de expedientes, cuentas e informes coincidieron tras la carga. El niño ficticio se incorporó con epilepsia documentada y coordinación entre hogar, escuela y salud en sus antecedentes. Para el diseño del caso se consultó la orientación general sobre coordinación y planes individuales. [Guía de los CDC sobre epilepsia en la escuela](https://www.cdc.gov/epilepsy/php/guidance-for-schools/index.html)

No se generaron propuestas ni vinculaciones con los expedientes nuevos. Las vistas de seguimiento de una vinculación se revisan con datos simulados en el navegador si la instancia no tiene vinculaciones vigentes.
