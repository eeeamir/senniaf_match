-- Solo amplía la validación; conserva todos los valores y registros existentes.
SELECT current_database() = :'base_autorizada' AND :'confirmacion' = 'SIMPLIFICAR_CIERRE_SIN_BORRAR' AS destino_confirmado \gset
\if :destino_confirmado
BEGIN;
SET LOCAL lock_timeout='5s';
DO $$ BEGIN
 IF to_regclass('public.programaciones_pesos') IS NULL OR to_regclass('public.combinaciones_excluidas') IS NULL
 THEN RAISE EXCEPTION 'Se requiere la migración 015 antes de simplificar el cierre.'; END IF;
END $$;
ALTER TABLE vinculaciones DROP CONSTRAINT vinculaciones_cierre_check;
ALTER TABLE vinculaciones ADD CONSTRAINT vinculaciones_cierre_registro_check CHECK(
 (estado='CERRADA' AND fecha_cierre IS NOT NULL AND motivo_cierre IS NOT NULL AND length(btrim(motivo_cierre))>=10 AND cerrado_por IS NOT NULL AND fecha_registro_cierre IS NOT NULL AND
 ((referencia_cierre IS NULL AND autoridad_cierre IS NULL) OR (referencia_cierre IS NOT NULL AND length(btrim(referencia_cierre))>=5 AND autoridad_cierre IS NOT NULL AND length(btrim(autoridad_cierre))>=3)))
 OR (estado<>'CERRADA' AND fecha_cierre IS NULL AND referencia_cierre IS NULL AND autoridad_cierre IS NULL AND motivo_cierre IS NULL AND cerrado_por IS NULL AND fecha_registro_cierre IS NULL AND NOT nino_liberado AND NOT familia_liberada));
COMMIT;
\else
DO $$ BEGIN RAISE EXCEPTION 'Destino o confirmación incorrectos. No se modificó el esquema.';END $$;
\endif
