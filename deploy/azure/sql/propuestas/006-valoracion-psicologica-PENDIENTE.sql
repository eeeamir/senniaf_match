-- Autorizada y aplicada el 8 de noviembre de 2026 (Panamá).
-- El nombre PENDIENTE se conserva por compatibilidad con las referencias documentales.
-- El JAR de finalización usa estas columnas. No hay migración automática.
-- Campos nulos para las versiones anteriores: no se inventa una valoración histórica.
BEGIN;
SET LOCAL lock_timeout='5s';
ALTER TABLE evaluaciones_psicologicas ADD COLUMN IF NOT EXISTS sin_diagnostico boolean;
ALTER TABLE evaluaciones_psicologicas ADD COLUMN IF NOT EXISTS areas_observadas text;
ALTER TABLE evaluaciones_psicologicas ADD COLUMN IF NOT EXISTS hallazgos_profesionales text;
ALTER TABLE evaluaciones_psicologicas ADD COLUMN IF NOT EXISTS diagnostico_profesional text;
COMMIT;
-- Tras autorización: almacenar lista cerrada de áreas como JSON en texto,
-- validar longitudes/coherencia en servidor y conservar estos campos en cada revisión.
