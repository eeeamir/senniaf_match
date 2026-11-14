# PostgreSQL: preparación local y conservación de datos

## Bases independientes

Docker Compose utiliza `senniaf_match` y el volumen persistente `senniaf_pgdata`. La demostración de Azure utiliza `senniaf_demo`. Tener las mismas tablas no implica compartir registros; no existe sincronización automática. No importar bases de auditoría como si fueran la base original del equipo.

## Configuración local

1. Copiar `.env.example` a `.env` y establecer una contraseña local privada.
2. Exportar las variables en la terminal WSL antes de iniciar Spring Boot:

   ```bash
   set -a
   source .env
   set +a
   docker compose up -d
   ```

PostgreSQL se publica solo en `127.0.0.1:5432`. El volumen se conserva entre arranques. Cambiar `DB_PASSWORD` no cambia la contraseña de un volumen ya inicializado: utilizar sus credenciales vigentes y no recrearlo para resolver autenticación.

## Esquema

La configuración habitual usa `ddl-auto=validate`: comprueba el esquema, sin crearlo ni migrarlo automáticamente. Para una base **nueva y vacía**, el esquema SQL sin datos está en `deploy/azure/sql/001-esquema-inicial.sql`. Aplicar después, en orden, `005-cierre-adopcion.sql`, los cambios 006, 007 y 008 de `deploy/azure/sql/propuestas/`, y las migraciones 009 y 010 de `deploy/azure/sql/`, la ampliación 011 de unidad de edad, la migración 012 de vinculaciones y la 013 de interrupciones de convivencia, confirmando expresamente el destino en cada script. El nombre histórico de la carpeta `propuestas` no implica que sus cambios 006–008 estén pendientes en Azure: ya están aplicados allí.

Ejemplo de aplicación local sobre una base nueva:

```bash
docker compose exec -T postgres psql -U postgres -d senniaf_match -v ON_ERROR_STOP=1 < deploy/azure/sql/001-esquema-inicial.sql
```

Revisar cada cambio antes de aplicarlo. En una base existente, comprobar el esquema y respaldar primero; no ejecutar el esquema inicial ni restaurar encima. El script `002-permisos.sql` corresponde a la cuenta técnica remota y no debe aplicarse indiscriminadamente al desarrollo local.

## Cuentas y registro

Las cuentas de demostración están desactivadas por defecto. En una base local sintética pueden habilitarse expresamente con `SENNIAF_DEMO_HABILITADO=true` y `SENNIAF_DEMO_PASSWORD` privada de al menos 16 caracteres y hasta 72 bytes. No se distribuyen contraseñas ni copias de usuarios remotos.

El registro familiar público y SMTP están cerrados por defecto. Para habilitarlos se requiere configurar remitente y transporte, verificar el envío y mantener los controles de vencimiento, intentos y reenvíos. El código no devuelve códigos de verificación en el entorno remoto.

Las asignaciones de profesionales se reflejan desde datos existentes; los cuatro roles no pueden reasignar responsables ni crear roles institucionales mediante registro público.

## Respaldos

Conservar los respaldos fuera de Git y verificar su integridad. No usar `docker compose down -v`, reiniciar la base o importar datos por una limpieza del repositorio. Los procedimientos de operación remota están en [deploy/azure/README.md](deploy/azure/README.md).

## Correcciones de formularios y carga controlada de demostración

Antes de iniciar el código que incorpora `especificacion_hogar` y las nuevas opciones, revisar y aplicar expresamente `deploy/azure/sql/009-formularios-coherentes.sql`. El script exige indicar la base autorizada y la confirmación `AMPLIAR_SIN_BORRAR`; no se ejecuta al arrancar Spring Boot ni desde el actualizador. Amplía opciones y añade una columna sin actualizar registros ni decisiones. Las restricciones `NOT VALID` conservan filas históricas y controlan nuevas escrituras; no equivalen a una validación de todos los datos anteriores.

La carga `python3 deploy/azure/preparar-demo.py` muestra el destino y los registros existentes sin modificarlos. Para preparar únicamente la demostración ya autorizada de Azure:

```bash
python3 deploy/azure/preparar-demo.py --aplicar --confirmar SENNIAF_DEMO_AUTORIZADA --familias-nuevas
```

Requiere Python 3 con `bcrypt`, acceso SSH con la clave y el host previamente fijados y permiso `sudo` para PostgreSQL en esa VM. Solicita mediante entrada oculta una clave única para cada cuenta nueva; nunca solicita ni cambia las de cuentas existentes. `--generar-credenciales` está reservado a un consumidor privado en memoria por tubería y entrega solo las credenciales de cuentas efectivamente insertadas: no redirigir su salida a archivos, registros, capturas ni documentación. Las claves se codifican con BCrypt, coste 10 y prefijo `$2a$`, igual al codificador existente.

El destino está fijado a `senniaf_demo`; la operación usa bloqueo y una transacción, crea solo los profesionales que faltan, conserva todos los registros previos y usa referencias estables `DEMO-FUNCIONAL-A/B/C` para no duplicar los tres casos. `--familias-nuevas` autoriza específicamente `DEMO-FAM-002` y `DEMO-FAM-003`, sin reutilizar cuentas de otras personas. La repetición conserva las cuentas y entrevistas ya existentes y no recupera ni restablece sus contraseñas.

Esta carga no despliega código, no migra el esquema, no modifica pesos y no crea decisiones del Comité. Mantiene compatibilidad con el JAR anterior: primera infancia se describe mediante las opciones abiertas y el rendimiento no aplicable queda nulo. El código corregido reconoce ese dato y lo presenta o guarda como «No aplica». Las observaciones de las evaluaciones son simuladas y no establecen diagnósticos.

Los nuevos expedientes de la carga reciben trabajo social y psicología según el menor número de casos asignados, sin cambiar responsables anteriores. Para aplicar esa misma asignación a nuevas altas interactivas en un entorno de demostración, configurar expresamente `SENNIAF_DEMO_ASIGNACION_AUTOMATICA=true` al operar la versión corregida. Está desactivada por defecto y no representa una política institucional.

## Propuesta local de documentos de identidad — pendiente de autorización

La ampliación del registro distingue cédula panameña, cédula de extranjero residente y pasaporte. La migración [010-documentos-identidad.sql](deploy/azure/sql/010-documentos-identidad.sql) añade únicamente `tipo_documento` y `numero_documento`, ambos opcionales para los usuarios anteriores, y un índice único por tipo y número normalizado. No contiene actualizaciones ni elimina datos; conserva cédulas, identificadores, contraseñas, sesiones y relaciones existentes. La aplicación consulta también los datos heredados para impedir registros duplicados.

**La migración fue autorizada para esta publicación y validada dos veces en PostgreSQL 16 aislado. Antes de iniciar la versión ampliada contra una base existente, verificar un respaldo y aplicarla expresamente.** No se ejecuta automáticamente al iniciar la aplicación; el estado del despliegue remoto se documenta en [deploy/azure/README.md](deploy/azure/README.md). Los pasaportes nuevos usan un identificador interno `PAS-<número>` y admiten acceso por su número o correo; las credenciales anteriores no se convierten ni se restablecen. El registro de acceso no determina la idoneidad legal para adoptar.

## HU19 — Vinculaciones

Antes de iniciar esta versión con `ddl-auto=validate`, aplicar expresamente [012-vinculaciones.sql](deploy/azure/sql/012-vinculaciones.sql) sobre la base autorizada, con respaldo y la versión anterior detenida. La migración requiere `base_autorizada` y `confirmacion=CREAR_VINCULACIONES_SIN_BORRAR`; no se ejecuta automáticamente. Conserva las aceptaciones previas como pendientes de respuesta y aborta completamente si hay referencias inválidas o asignaciones incompatibles.

La validación se realizó en PostgreSQL 18.3 temporal, sin modificar bases existentes del usuario ni Azure. Los comandos, pruebas de concurrencia y pendientes de despliegue están en [la validación final de HU19](docs/HU19_VALIDACION_FINAL.md).

La [verificación integral posterior](docs/VERIFICACION_INTEGRAL_HU19.md) repitió las pruebas en PostgreSQL **16.15** y Java 17 y confirmó mediante lectura de Azure que 012 sigue ausente en la base persistente. La validación aislada no autoriza ni ejecuta esa migración real.


## Sprint 8 — interrupciones y compatibilidad

La versión integrada requiere también [013-interrupciones-convivencia.sql](deploy/azure/sql/013-interrupciones-convivencia.sql), después de 012 y antes de arrancar el JAR. Es aditiva: crea la tabla de informes y revisiones, sin modificar vinculaciones ni perfiles existentes. Requiere `base_autorizada` y `confirmacion=AGREGAR_INTERRUPCIONES_SIN_BORRAR`, respaldo revisado y autorización para el destino. La cuenta técnica puede insertar/consultar y actualizar únicamente los campos de revisión y versión; no puede sobrescribir ni borrar el informe original.

HU12 reutiliza `continuidad_cultural`; HU14 reutiliza el contador de reevaluaciones completadas y la auditoría; HU13 conserva el esquema de pesos existente. No requieren nuevas columnas. Durante este trabajo, las migraciones 012/013 se probaron en PostgreSQL 16 aislado. No se ejecutaron en Azure durante este trabajo. La recuperación debe usar un JAR compatible con ambas tablas; el actualizador preparado impide volver automáticamente a una versión que omita estas funciones.

La preparación de CI está en `src/test/scripts/preparar-postgres-ci.sh`, exclusivamente para un entorno vacío de pruebas con nombre `hu19_`. No ejecutarlo contra la base habitual ni contra Azure. Los resultados y evidencias están en [el informe del Sprint 8](docs/SPRINT8_RESULTADOS.md).


## Estado persistente posterior de Sprint 8

Las migraciones 012, 013 y 014 fueron aplicadas en Azure el 14 de noviembre de 2026 mediante la intervención autorizada, sin recrear la base. La 014 añade el cierre confirmado y reservas independientes sobre Vinculaciones. El detalle de respaldos, validaciones y versión publicada está en [la integración del Sprint 8](docs/SPRINT8_INTEGRACION_DESPLIEGUE.md). Las notas anteriores describen las validaciones previas al despliegue.
