-- Conserva el formato histórico y permite registrar juntas la autorización y la convivencia.
SELECT current_database() = :'base_autorizada' AND :'confirmacion' = 'REGISTRAR_AUTORIZACION_CON_FECHA_DEL_SERVIDOR' AS destino_confirmado \gset
\if :destino_confirmado
BEGIN;
SET LOCAL lock_timeout = '5s';

ALTER TABLE public.vinculaciones
  DROP CONSTRAINT vinculaciones_autorizacion_check,
  DROP CONSTRAINT vinculaciones_inicio_check,
  ADD CONSTRAINT vinculaciones_autorizacion_check CHECK (
    (referencia_autorizacion IS NULL AND fecha_emision_autorizacion IS NULL
      AND autorizacion_registrada_por IS NULL AND autorizacion_rol IS NULL AND fecha_registro_autorizacion IS NULL)
    OR (autorizacion_registrada_por IS NOT NULL AND length(btrim(autorizacion_registrada_por)) > 0
      AND autorizacion_rol IS NOT NULL AND autorizacion_rol = 'COMITE' AND fecha_registro_autorizacion IS NOT NULL
      AND ((referencia_autorizacion IS NOT NULL AND length(btrim(referencia_autorizacion)) >= 5
            AND fecha_emision_autorizacion IS NOT NULL)
        OR (referencia_autorizacion IS NULL AND fecha_emision_autorizacion IS NULL
            AND fecha_inicio_convivencia IS NOT NULL
            AND inicio_registrado_por IS NOT NULL AND inicio_registrado_por = autorizacion_registrada_por
            AND inicio_rol IS NOT NULL AND inicio_rol = autorizacion_rol
            AND fecha_registro_inicio IS NOT NULL AND fecha_registro_inicio = fecha_registro_autorizacion)))
  ),
  ADD CONSTRAINT vinculaciones_inicio_check CHECK (
    (fecha_inicio_convivencia IS NULL AND inicio_registrado_por IS NULL AND inicio_rol IS NULL
      AND fecha_registro_inicio IS NULL)
    OR (fecha_inicio_convivencia IS NOT NULL AND inicio_registrado_por IS NOT NULL AND length(btrim(inicio_registrado_por)) > 0
      AND inicio_rol IS NOT NULL AND inicio_rol = 'COMITE' AND fecha_registro_inicio IS NOT NULL
      AND ((fecha_emision_autorizacion IS NOT NULL AND fecha_inicio_convivencia >= fecha_emision_autorizacion)
        OR (referencia_autorizacion IS NULL AND fecha_emision_autorizacion IS NULL
          AND fecha_registro_autorizacion IS NOT NULL AND fecha_registro_inicio = fecha_registro_autorizacion)))
  );

COMMIT;
\else
DO $$ BEGIN RAISE EXCEPTION 'Destino o confirmación incorrectos; no se modificó el esquema'; END $$;
\endif
