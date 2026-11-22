-- Cierre confirmado por autoridad; cada expediente conserva su reserva hasta revisión independiente.
SELECT current_database() = :'base_autorizada' AND :'confirmacion' = 'REGISTRAR_CIERRE_SIN_BORRAR' AS destino_confirmado \gset
\if :destino_confirmado
BEGIN;
SET LOCAL lock_timeout='5s';
ALTER TABLE public.vinculaciones
 ADD COLUMN fecha_cierre date,
 ADD COLUMN referencia_cierre varchar(200),
 ADD COLUMN autoridad_cierre varchar(200),
 ADD COLUMN motivo_cierre varchar(2000),
 ADD COLUMN cerrado_por varchar(40),
 ADD COLUMN fecha_registro_cierre timestamp(6),
 ADD COLUMN nino_liberado boolean NOT NULL DEFAULT false,
 ADD COLUMN familia_liberada boolean NOT NULL DEFAULT false;
ALTER TABLE public.vinculaciones DROP CONSTRAINT vinculaciones_estado_check;
ALTER TABLE public.vinculaciones DROP CONSTRAINT vinculaciones_check;
ALTER TABLE public.vinculaciones ADD CONSTRAINT vinculaciones_estado_check CHECK(estado IN ('PENDIENTE_RESPUESTA','RESPUESTA_ACEPTADA','RESPUESTA_RECHAZADA_PENDIENTE_CIERRE','CERRADA'));
ALTER TABLE public.vinculaciones ADD CONSTRAINT vinculaciones_respuesta_check CHECK(estado='CERRADA' OR (estado='PENDIENTE_RESPUESTA' AND fecha_respuesta IS NULL AND respondido_por IS NULL) OR (estado IN ('RESPUESTA_ACEPTADA','RESPUESTA_RECHAZADA_PENDIENTE_CIERRE') AND fecha_respuesta IS NOT NULL AND respondido_por IS NOT NULL));
ALTER TABLE public.vinculaciones ADD CONSTRAINT vinculaciones_cierre_check CHECK(
 (estado='CERRADA' AND fecha_cierre IS NOT NULL AND length(btrim(referencia_cierre))>=5 AND referencia_cierre IS NOT NULL AND autoridad_cierre IS NOT NULL AND length(btrim(autoridad_cierre))>=3 AND motivo_cierre IS NOT NULL AND length(btrim(motivo_cierre))>=10 AND cerrado_por IS NOT NULL AND fecha_registro_cierre IS NOT NULL)
 OR (estado<>'CERRADA' AND fecha_cierre IS NULL AND referencia_cierre IS NULL AND autoridad_cierre IS NULL AND motivo_cierre IS NULL AND cerrado_por IS NULL AND fecha_registro_cierre IS NULL AND NOT nino_liberado AND NOT familia_liberada));
DROP INDEX public.uk_vinculacion_nino_reservado;
DROP INDEX public.uk_vinculacion_familia_reservada;
CREATE UNIQUE INDEX uk_vinculacion_nino_reservado ON public.vinculaciones(nino_id) WHERE reserva_vigente AND NOT nino_liberado;
CREATE UNIQUE INDEX uk_vinculacion_familia_reservada ON public.vinculaciones(familia_id) WHERE reserva_vigente AND NOT familia_liberada;
COMMIT;
\else
DO $$ BEGIN RAISE EXCEPTION 'Destino o confirmación incorrectos. No se modificó el esquema.'; END $$;
\endif
