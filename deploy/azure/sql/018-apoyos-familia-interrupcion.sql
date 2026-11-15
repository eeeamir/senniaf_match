-- Aditiva: conserva texto cultural anterior, fechas, referencias e informes históricos.
SELECT current_database() = :'base_autorizada' AND :'confirmacion' = 'AMPLIAR_APOYOS_SIMPLIFICAR_INTERRUPCION' AS destino_confirmado \gset
\if :destino_confirmado
BEGIN;
SET LOCAL lock_timeout='5s';
DO $$ BEGIN
 IF (SELECT count(*) FROM information_schema.columns WHERE table_schema='public' AND table_name='perfiles_nino' AND data_type='text' AND column_name IN ('idioma_comunicacion','costumbres_tradiciones','vinculos_culturales','opinion_cultural','recomendaciones_culturales'))<>5
 OR NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='auditoria_casos' AND column_name='descripcion' AND data_type='text')
 OR NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid=to_regclass('public.vinculaciones') AND conname='vinculaciones_cierre_registro_check')
 THEN RAISE EXCEPTION 'Se requiere el esquema anterior completo (016/017) y auditoría sin truncamiento'; END IF;
END $$;
ALTER TABLE public.perfiles_familia
 ADD COLUMN apoyo_idiomas text,
 ADD COLUMN respeto_tradiciones text,
 ADD COLUMN recursos_comunitarios text,
 ADD COLUMN respeto_preferencias text,
 ADD COLUMN compromiso_cultural text;
ALTER TABLE public.interrupciones_convivencia
 ALTER COLUMN fecha_interrupcion DROP NOT NULL,
 ALTER COLUMN referencia_actuacion DROP NOT NULL,
 ADD COLUMN motivo varchar(40),
 ADD CONSTRAINT interrupciones_motivo_check CHECK (motivo IS NULL OR motivo IN ('DIFICULTADES_CONTINUAR','ADAPTACION','BIENESTAR_NINO','CONDICIONES_CUIDADO','OTRO'));
-- Los motivos anteriores permanecen NULL: no inferirlos del texto histórico.
COMMIT;
\else
DO $$ BEGIN RAISE EXCEPTION 'Destino o confirmación incorrectos; no se modificó el esquema'; END $$;
\endif
