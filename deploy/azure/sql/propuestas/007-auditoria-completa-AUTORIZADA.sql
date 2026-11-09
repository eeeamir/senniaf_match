-- Autorización expresa: ampliar solamente la descripción de auditoría.
BEGIN;
SET LOCAL lock_timeout='5s';
ALTER TABLE auditoria_casos ALTER COLUMN descripcion TYPE text;
COMMIT;
