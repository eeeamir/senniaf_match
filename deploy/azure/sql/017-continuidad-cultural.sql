-- Aditiva: no transforma ni sobrescribe contexto_cultural ni registros históricos.
SELECT current_database() = :'base_autorizada' AND :'confirmacion' = 'AGREGAR_CULTURA_SIN_REINTERPRETAR' AS destino_confirmado \gset
\if :destino_confirmado
BEGIN;
SET LOCAL lock_timeout='5s';
DO $$ BEGIN
 IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='auditoria_casos' AND column_name='descripcion' AND data_type='text')
 THEN RAISE EXCEPTION 'La auditoría debe admitir texto completo antes de agregar los campos culturales'; END IF;
END $$;
ALTER TABLE public.perfiles_nino
 ADD COLUMN idioma_comunicacion text,
 ADD COLUMN costumbres_tradiciones text,
 ADD COLUMN vinculos_culturales text,
 ADD COLUMN opinion_cultural text,
 ADD COLUMN recomendaciones_culturales text;
COMMIT;
\else
DO $$ BEGIN RAISE EXCEPTION 'Destino o confirmación incorrectos; no se modificó el esquema'; END $$;
\endif
