-- Autorizada por el usuario el 9 de noviembre de 2026.
-- Única modificación: columna opcional, sin valores por defecto ni carga histórica.
BEGIN;
SET LOCAL lock_timeout='5s';
ALTER TABLE public.evaluaciones_psicologicas
    ADD COLUMN IF NOT EXISTS contexto_evaluacion text;
COMMIT;
-- JSON versionado por informe. Las versiones anteriores conservan NULL.
