#!/usr/bin/env bash
# Prepara exclusivamente una base nueva de pruebas; nunca restaura ni migra la demostración.
set -euo pipefail
[[ ${SENNIAF_PRUEBAS_AISLADAS:-} == SI && ${PGHOST:-} == 127.0.0.1 && ${PGDATABASE:-} =~ ^hu19_[a-z_]+$ ]] || {
    echo 'Usa una base local hu19_ nueva y confirma SENNIAF_PRUEBAS_AISLADAS=SI.' >&2; exit 1;
}
: "${HU19_TEST_DB_PASSWORD:?Configura una clave exclusiva para las cuentas de pruebas}"
raiz=$(cd -- "$(dirname -- "$0")/../../.." && pwd)
[[ $(psql -X -v ON_ERROR_STOP=1 -Atc "SELECT count(*) FROM information_schema.tables WHERE table_schema='public'") == 0 ]] || {
    echo 'La base ya contiene tablas. Se conserva sin cambios; utiliza una base de pruebas nueva.' >&2; exit 1;
}
# El bootstrap requiere el administrador del servidor aislado, no el de la aplicación real.
psql -X -v ON_ERROR_STOP=1 -v clave_pruebas="$HU19_TEST_DB_PASSWORD" -v base_pruebas="$PGDATABASE" <<'SQL'
BEGIN;
CREATE ROLE hu19_test LOGIN PASSWORD :'clave_pruebas';
CREATE ROLE senniaf_app LOGIN PASSWORD :'clave_pruebas';
ALTER DATABASE :"base_pruebas" OWNER TO hu19_test;
COMMIT;
SQL
export PGUSER=hu19_test PGPASSWORD="$HU19_TEST_DB_PASSWORD"
for archivo in \
    deploy/azure/sql/001-esquema-inicial.sql \
    deploy/azure/sql/005-cierre-adopcion.sql \
    deploy/azure/sql/propuestas/006-valoracion-psicologica-PENDIENTE.sql \
    deploy/azure/sql/propuestas/007-auditoria-completa-AUTORIZADA.sql \
    deploy/azure/sql/propuestas/008-contexto-evaluacion-PENDIENTE.sql \
    deploy/azure/sql/009-formularios-coherentes.sql \
    deploy/azure/sql/010-documentos-identidad.sql \
    deploy/azure/sql/propuestas/011-unidad-edad-PENDIENTE.sql \
    deploy/azure/sql/002-permisos.sql \
    deploy/azure/sql/012-vinculaciones.sql; do
    confirmacion=AMPLIAR_SIN_BORRAR
    [[ $archivo != *010-* ]] || confirmacion=AGREGAR_DOCUMENTOS_SIN_REEMPLAZAR
    [[ $archivo != *012-* ]] || confirmacion=CREAR_VINCULACIONES_SIN_BORRAR
    psql -X -v ON_ERROR_STOP=1 -v base_autorizada="$PGDATABASE" -v confirmacion="$confirmacion" -f "$raiz/$archivo"
done
