-- Cambio aditivo para la revisión local de interfaces profesionales.
-- Ejecutar únicamente al aprobar la actualización del entorno correspondiente.
-- No reclasifica solicitudes históricas ni modifica referencias institucionales.
BEGIN;
ALTER TABLE perfiles_nino ADD COLUMN IF NOT EXISTS cierre_por_adopcion boolean NOT NULL DEFAULT false;
ALTER TABLE solicitudes_estado_caso ADD COLUMN IF NOT EXISTS categoria varchar(40);
COMMIT;
