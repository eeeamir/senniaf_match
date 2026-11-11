-- Pendiente de autorización y ejecución manual, antes de desplegar el código nuevo.
-- No convierte edades ni actualiza expedientes, decisiones o eventos históricos.
-- psql -X -v ON_ERROR_STOP=1 -v base_autorizada=BASE_REVISADA -v confirmacion=AMPLIAR_SIN_BORRAR -f 011-unidad-edad-PENDIENTE.sql
SELECT current_database() = :'base_autorizada' AND :'confirmacion' = 'AMPLIAR_SIN_BORRAR' AS destino_confirmado \gset
\if :destino_confirmado
BEGIN;
SET LOCAL lock_timeout = '5s';
ALTER TABLE public.perfiles_nino ADD COLUMN IF NOT EXISTS unidad_edad varchar(10);
-- NULL conserva la unidad histórica (años), incluido el antiguo 0 años sin meses conocidos.
ALTER TABLE public.perfiles_nino DROP CONSTRAINT IF EXISTS ck_nino_edad_unidad;
ALTER TABLE public.perfiles_nino ADD CONSTRAINT ck_nino_edad_unidad CHECK (
    (unidad_edad IS NULL AND (edad IS NULL OR edad BETWEEN 0 AND 17))
    OR (unidad_edad IS NOT NULL AND edad IS NOT NULL AND (
        (unidad_edad = 'ANIOS' AND edad BETWEEN 1 AND 17)
        OR (unidad_edad = 'MESES' AND edad BETWEEN 0 AND 11)
    ))
) NOT VALID;
COMMIT;
\else
\echo 'Destino o confirmación incorrectos. No se modificó el esquema.'
\quit 3
\endif
