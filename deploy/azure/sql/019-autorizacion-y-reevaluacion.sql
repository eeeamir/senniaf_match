-- Conserva todas las decisiones y vinculaciones; no infiere autorizaciones de hechos antiguos.
SELECT current_database() = :'base_autorizada' AND :'confirmacion' = 'REGISTRAR_AUTORIZACION_Y_APARTADOS_SIN_BORRAR' AS destino_confirmado \gset
\if :destino_confirmado
BEGIN;
SET LOCAL lock_timeout='5s';
DO $$ BEGIN
 IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid=to_regclass('public.interrupciones_convivencia') AND conname='interrupciones_motivo_check')
 OR (SELECT count(*) FROM information_schema.columns WHERE table_schema='public' AND table_name='perfiles_familia' AND data_type='text' AND column_name IN ('apoyo_idiomas','respeto_tradiciones','recursos_comunitarios','respeto_preferencias','compromiso_cultural'))<>5
 THEN RAISE EXCEPTION 'Se requiere 018 completa antes de continuar'; END IF;
END $$;
ALTER TABLE public.vinculaciones
 ADD COLUMN referencia_autorizacion varchar(200), ADD COLUMN fecha_emision_autorizacion date,
 ADD COLUMN autorizacion_registrada_por varchar(40), ADD COLUMN autorizacion_rol varchar(25), ADD COLUMN fecha_registro_autorizacion timestamp(6),
 ADD COLUMN fecha_inicio_convivencia date, ADD COLUMN inicio_registrado_por varchar(40), ADD COLUMN inicio_rol varchar(25), ADD COLUMN fecha_registro_inicio timestamp(6),
 ADD CONSTRAINT vinculaciones_autorizacion_check CHECK(
 (referencia_autorizacion IS NULL AND fecha_emision_autorizacion IS NULL AND autorizacion_registrada_por IS NULL AND autorizacion_rol IS NULL AND fecha_registro_autorizacion IS NULL)
 OR (referencia_autorizacion IS NOT NULL AND length(btrim(referencia_autorizacion))>=5 AND fecha_emision_autorizacion IS NOT NULL AND autorizacion_registrada_por IS NOT NULL AND length(btrim(autorizacion_registrada_por))>0 AND autorizacion_rol IS NOT NULL AND autorizacion_rol='COMITE' AND fecha_registro_autorizacion IS NOT NULL)),
 ADD CONSTRAINT vinculaciones_inicio_check CHECK(
 (fecha_inicio_convivencia IS NULL AND inicio_registrado_por IS NULL AND inicio_rol IS NULL AND fecha_registro_inicio IS NULL)
 OR (fecha_inicio_convivencia IS NOT NULL AND fecha_emision_autorizacion IS NOT NULL AND fecha_inicio_convivencia>=fecha_emision_autorizacion AND inicio_registrado_por IS NOT NULL AND length(btrim(inicio_registrado_por))>0 AND inicio_rol IS NOT NULL AND inicio_rol='COMITE' AND fecha_registro_inicio IS NOT NULL));
ALTER TABLE public.decisiones_comite ADD COLUMN apartados_reevaluacion jsonb, ADD COLUMN categoria_reevaluacion varchar(40),
 ADD CONSTRAINT decisiones_apartados_check CHECK(apartados_reevaluacion IS NULL OR (decision='REEVALUACION' AND jsonb_typeof(apartados_reevaluacion)='array' AND jsonb_array_length(apartados_reevaluacion) BETWEEN 1 AND 5 AND apartados_reevaluacion <@ '["Hogar y red de apoyo","Preferencias familiares","Salud y acompañamiento","Disponibilidad y entorno","Apoyos culturales y comunitarios"]'::jsonb)),
 ADD CONSTRAINT decisiones_categoria_reevaluacion_check CHECK(categoria_reevaluacion IS NULL OR (decision='REEVALUACION' AND categoria_reevaluacion IN ('INCOMPLETA','DESACTUALIZADA','ACLARACION','INCONSISTENCIAS')));
-- Salvaguardas para escrituras desde código anterior durante una recuperación. Solo afectan INSERT.
CREATE FUNCTION public.validar_interrupcion_autorizada() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NOT EXISTS(SELECT 1 FROM public.vinculaciones v JOIN public.perfiles_nino n ON n.id=v.nino_id WHERE v.id=NEW.vinculacion_id AND v.estado='RESPUESTA_ACEPTADA' AND v.reserva_vigente AND NOT n.cierre_por_adopcion AND v.fecha_registro_autorizacion IS NOT NULL AND v.fecha_inicio_convivencia IS NOT NULL AND v.fecha_inicio_convivencia <= (CURRENT_TIMESTAMP AT TIME ZONE 'America/Panama')::date)
 THEN RAISE EXCEPTION 'Se requiere autorización institucional e inicio efectivo para registrar una interrupción' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER interrupcion_autorizada_insert BEFORE INSERT ON public.interrupciones_convivencia FOR EACH ROW EXECUTE FUNCTION public.validar_interrupcion_autorizada();
CREATE FUNCTION public.validar_combinacion_interrumpida() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF EXISTS(SELECT 1 FROM public.interrupciones_convivencia i JOIN public.vinculaciones v ON v.id=i.vinculacion_id JOIN public.perfiles_nino n ON n.id=v.nino_id JOIN public.perfiles_familia f ON f.id=v.familia_id WHERE n.codigo_caso=NEW.codigo_nino AND f.codigo_caso=NEW.codigo_familia)
 THEN RAISE EXCEPTION 'La combinación anteriormente interrumpida no admite una nueva propuesta automática' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER combinacion_interrumpida_insert BEFORE INSERT ON public.decisiones_comite FOR EACH ROW EXECUTE FUNCTION public.validar_combinacion_interrumpida();
COMMIT;
\else
DO $$ BEGIN RAISE EXCEPTION 'Destino o confirmación incorrectos; no se modificó el esquema'; END $$;
\endif
