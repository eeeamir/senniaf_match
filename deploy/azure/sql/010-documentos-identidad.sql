-- Autorizada el 10 de noviembre de 2026 para la publicación final.
-- Validada dos veces sobre PostgreSQL 16 aislado, conservando todas las filas previas.
-- Después de un respaldo verificado:
-- psql -X -v ON_ERROR_STOP=1 -v base_autorizada=BASE_REVISADA -v confirmacion=AGREGAR_DOCUMENTOS_SIN_REEMPLAZAR -f 010-documentos-identidad.sql
-- Solo añade dos columnas opcionales y un índice. No actualiza usuarios, identificadores,
-- cédulas anteriores, contraseñas, sesiones ni relaciones con expedientes.
SELECT current_database() = :'base_autorizada' AND :'confirmacion' = 'AGREGAR_DOCUMENTOS_SIN_REEMPLAZAR' AS destino_confirmado \gset
\if :destino_confirmado
BEGIN;
SET LOCAL lock_timeout = '5s';
ALTER TABLE public.usuarios ADD COLUMN IF NOT EXISTS tipo_documento varchar(30);
ALTER TABLE public.usuarios ADD COLUMN IF NOT EXISTS numero_documento varchar(30);
ALTER TABLE public.usuarios DROP CONSTRAINT IF EXISTS ck_usuario_documento;
ALTER TABLE public.usuarios ADD CONSTRAINT ck_usuario_documento CHECK (
 (tipo_documento IS NULL AND numero_documento IS NULL)
 OR (tipo_documento IS NOT NULL AND tipo_documento IN ('CEDULA_PANAMENA','CEDULA_RESIDENTE','PASAPORTE')
     AND numero_documento IS NOT NULL AND length(btrim(numero_documento)) > 0)
) NOT VALID;
CREATE UNIQUE INDEX IF NOT EXISTS uk_usuario_tipo_numero_documento
 ON public.usuarios(tipo_documento,upper(btrim(numero_documento)))
 WHERE tipo_documento IS NOT NULL AND numero_documento IS NOT NULL;
COMMIT;
\else
\echo 'Destino o confirmación incorrectos. No se modificó el esquema.'
\quit 3
\endif

-- Los usuarios anteriores mantienen ambos campos NULL. La aplicación consulta también
-- la cédula/identificador heredados para impedir nuevos duplicados y conservar su acceso.
-- No se infiere ni cambia el documento de una persona de forma automática.
-- Aplicar esta ampliación antes de operar la versión que utiliza estas columnas.
