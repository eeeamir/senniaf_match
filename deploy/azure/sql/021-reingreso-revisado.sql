-- Ajuste funcional revisado: no cambia filas ni borra antecedentes. Solo modifica la salvaguarda INSERT.
SELECT current_database() = :'base_autorizada' AND :'confirmacion' = 'PERMITIR_REINGRESO_REVISADO_SIN_BORRAR' AS destino_confirmado \gset
\if :destino_confirmado
BEGIN;
SET LOCAL lock_timeout='5s';
DO $$ BEGIN
 IF to_regclass('public.citas_historial') IS NULL OR NOT EXISTS(SELECT 1 FROM pg_trigger WHERE tgrelid=to_regclass('public.decisiones_comite') AND tgname='combinacion_interrumpida_insert' AND tgenabled='O')
 THEN RAISE EXCEPTION 'Se requiere el esquema anterior completo antes de ajustar el reingreso'; END IF;
 IF obj_description('public.validar_combinacion_interrumpida()'::regprocedure,'pg_proc')='senniaf-reingreso-revisado-021'
 THEN RAISE EXCEPTION '021 ya está aplicada; no se repite la migración'; END IF;
END $$;
CREATE OR REPLACE FUNCTION public.validar_combinacion_interrumpida() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF EXISTS(SELECT 1 FROM public.interrupciones_convivencia i
 JOIN public.vinculaciones v ON v.id=i.vinculacion_id
 JOIN public.perfiles_nino n ON n.id=v.nino_id JOIN public.perfiles_familia f ON f.id=v.familia_id
 WHERE n.codigo_caso=NEW.codigo_nino AND f.codigo_caso=NEW.codigo_familia
 AND (v.estado<>'CERRADA' OR NOT v.nino_liberado OR NOT v.familia_liberada))
 THEN RAISE EXCEPTION 'La combinación interrumpida requiere cierre y revisión independiente de ambos expedientes' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
COMMENT ON FUNCTION public.validar_combinacion_interrumpida() IS 'senniaf-reingreso-revisado-021';
COMMIT;
\else
DO $$ BEGIN RAISE EXCEPTION 'Destino o confirmación incorrectos; no se modificó el esquema'; END $$;
\endif
