-- Ampliación compatible; no actualiza filas, no borra decisiones ni audita actuaciones antiguas.
-- Ejecutar expresamente antes de desplegar el JAR nuevo:
-- psql -X -v ON_ERROR_STOP=1 -v base_autorizada=BASE_REVISADA -v confirmacion=AMPLIAR_SIN_BORRAR -f 009-formularios-coherentes.sql
SELECT current_database() = :'base_autorizada' AND :'confirmacion' = 'AMPLIAR_SIN_BORRAR' AS destino_confirmado \gset
\if :destino_confirmado
BEGIN;
SET LOCAL lock_timeout = '5s';
ALTER TABLE public.perfiles_familia ADD COLUMN IF NOT EXISTS especificacion_hogar varchar(1000);

ALTER TABLE public.nino_alergias DROP CONSTRAINT IF EXISTS nino_alergias_alergia_check;
ALTER TABLE public.nino_alergias ADD CONSTRAINT nino_alergias_alergia_check CHECK (alergia IN ('NINGUNA_CONOCIDA','ALIMENTARIA','MEDICAMENTOS','AMBIENTAL_POLEN','PICADURA_INSECTOS','OTRA','INFORMACION_NO_DISPONIBLE')) NOT VALID;

ALTER TABLE public.nino_condiciones_salud DROP CONSTRAINT IF EXISTS nino_condiciones_salud_condicion_check;
ALTER TABLE public.nino_condiciones_salud ADD CONSTRAINT nino_condiciones_salud_condicion_check CHECK (condicion IN ('NINGUNA_CONOCIDA','ASMA','DIABETES','EPILEPSIA','TDAH','TEA','DISCAPACIDAD_FISICA','DISCAPACIDAD_INTELECTUAL','DISCAPACIDAD_VISUAL','DISCAPACIDAD_AUDITIVA','OTRA','INFORMACION_NO_DISPONIBLE')) NOT VALID;

ALTER TABLE public.nino_gustos DROP CONSTRAINT IF EXISTS nino_gustos_gusto_check;
ALTER TABLE public.nino_gustos ADD CONSTRAINT nino_gustos_gusto_check CHECK (gusto IN ('NINGUNO_IDENTIFICADO','DEPORTES','VIDEOJUEGOS','MUSICA','ARTE_DIBUJO','LECTURA','NATURALEZA_ANIMALES','BAILE','JUEGOS_DE_MESA','OTRO','JUEGO_SENSORIAL','JUEGO_SIMBOLICO','CUENTOS','EXPLORACION','AUN_NO_EVALUADO','INFORMACION_INSUFICIENTE')) NOT VALID;

ALTER TABLE public.nino_habilidades DROP CONSTRAINT IF EXISTS nino_habilidades_habilidad_check;
ALTER TABLE public.nino_habilidades ADD CONSTRAINT nino_habilidades_habilidad_check CHECK (habilidad IN ('NINGUNA_IDENTIFICADA','DEPORTES','ARTE','MUSICA','MATEMATICAS','LECTURA_ESCRITURA','TECNOLOGIA','CIENCIAS','MANUALIDADES','OTRA','JUEGO','COMUNICACION','EXPLORACION','MOVIMIENTO','INTERACCION_SOCIAL','AUN_NO_EVALUADO','INFORMACION_INSUFICIENTE')) NOT VALID;

ALTER TABLE public.nino_personalidad DROP CONSTRAINT IF EXISTS nino_personalidad_rasgo_check;
ALTER TABLE public.nino_personalidad ADD CONSTRAINT nino_personalidad_rasgo_check CHECK (rasgo IN ('RESERVADO','SOCIABLE','EXTROVERTIDO','TIMIDO','CURIOSO','SENSIBLE','INDEPENDIENTE','CARINOSO','OTRO','AUN_NO_EVALUADO','INFORMACION_INSUFICIENTE')) NOT VALID;

ALTER TABLE public.perfiles_nino DROP CONSTRAINT IF EXISTS perfiles_nino_nivel_escolar_check;
ALTER TABLE public.perfiles_nino ADD CONSTRAINT perfiles_nino_nivel_escolar_check CHECK (nivel_escolar IN ('INICIAL','PRIMARIA','PREMEDIA','MEDIA','NO_ESCOLARIZADO','OTRO','INFORMACION_NO_DISPONIBLE')) NOT VALID;

ALTER TABLE public.perfiles_nino DROP CONSTRAINT IF EXISTS perfiles_nino_rendimiento_academico_check;
ALTER TABLE public.perfiles_nino ADD CONSTRAINT perfiles_nino_rendimiento_academico_check CHECK (rendimiento_academico IN ('EXCELENTE','BUENO','REGULAR','NECESITA_APOYO','NO_APLICA','NO_EVALUADO','INFORMACION_NO_DISPONIBLE')) NOT VALID;

ALTER TABLE public.perfiles_nino DROP CONSTRAINT IF EXISTS perfiles_nino_tiene_hermanos_check;
ALTER TABLE public.perfiles_nino ADD CONSTRAINT perfiles_nino_tiene_hermanos_check CHECK (tiene_hermanos IN ('SI','NO','NO_SABE')) NOT VALID;

ALTER TABLE public.perfiles_familia DROP CONSTRAINT IF EXISTS perfiles_familia_disposicion_hermanos_check;
ALTER TABLE public.perfiles_familia ADD CONSTRAINT perfiles_familia_disposicion_hermanos_check CHECK (disposicion_hermanos IN ('DISPUESTA','DISPUESTA_CON_DUDAS','PREFIERE_UN_SOLO_NINO','OTRA','NINGUNA_DE_LAS_ANTERIORES')) NOT VALID;

-- NOT VALID conserva filas históricas; la restricción se aplica a nuevas escrituras.
ALTER TABLE public.perfiles_nino DROP CONSTRAINT IF EXISTS ck_nino_hermanos_coherentes;
ALTER TABLE public.perfiles_nino ADD CONSTRAINT ck_nino_hermanos_coherentes CHECK (
 CASE WHEN tiene_hermanos = 'SI' THEN cantidad_hermanos IS NOT NULL AND cantidad_hermanos > 0
      WHEN tiene_hermanos = 'NO' THEN cantidad_hermanos IS NULL OR cantidad_hermanos = 0
      ELSE cantidad_hermanos IS NULL END
) NOT VALID;
COMMIT;
\else
\echo 'Destino o confirmación incorrectos. No se modificó el esquema.'
\quit 3
\endif
