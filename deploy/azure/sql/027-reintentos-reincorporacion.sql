SELECT current_database() = :'base_autorizada' AND :'confirmacion' = 'HABILITAR_REINTENTOS_REINCORPORACION' AS destino_confirmado \gset
\if :destino_confirmado
BEGIN;
SET LOCAL lock_timeout = '5s';

CREATE UNIQUE INDEX reincorporacion_unica_no_denegada
    ON public.solicitudes_reincorporacion (vinculacion_id)
    WHERE autorizada IS DISTINCT FROM FALSE;

ALTER TABLE public.solicitudes_reincorporacion
    DROP CONSTRAINT solicitudes_reincorporacion_vinculacion_id_key;

COMMIT;
\else
DO $$ BEGIN RAISE EXCEPTION 'Destino o confirmación incorrectos; no se modificó el esquema'; END $$;
\endif
