-- Aplicar como propietario solo en la base remota dedicada, después del esquema.
-- senniaf_app es una cuenta técnica PostgreSQL, no un rol funcional de SENNIAF.
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
GRANT USAGE ON SCHEMA public TO senniaf_app;
GRANT SELECT, INSERT ON ALL TABLES IN SCHEMA public TO senniaf_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO senniaf_app;
GRANT UPDATE ON usuarios, perfiles_nino, perfiles_familia, pesos_scoring, solicitudes_estado_caso TO senniaf_app;
GRANT DELETE ON sesiones_acceso, familia_red_apoyo, familia_salud_apoyo, familia_salud_hoy,
 nino_alergias, nino_condiciones_salud, nino_gustos, nino_habilidades, nino_personalidad TO senniaf_app;
-- Auditoría, decisiones y evaluaciones no reciben permisos UPDATE/DELETE.
