# Seguimiento temporal y visitas domiciliarias

El proyecto adopta 30 días calendario y tres visitas domiciliarias como referencias de acogimiento preadoptivo. La fuente del criterio es el artículo 91 de la Ley 46 del 17 de julio de 2013. [Gaceta Oficial 27332-A](https://www.organojudicial.gob.pa/uploads/wp_repo/uploads/2016/11/Ley-46-de-2013.pdf)

Las referencias se muestran como información de seguimiento. Los registros efectivos y las decisiones institucionales se mantienen separados de los indicadores. Las visitas adicionales siguen disponibles mientras el estado de la vinculación permita registrarlas.

La migración aditiva 023 se aplica mediante el procedimiento manual del proyecto, después de 022, con `ON_ERROR_STOP=1`, `base_autorizada` y `confirmacion=CREAR_VISITAS_DOMICILIARIAS`. Antes de publicar el código, verificar la creación de las tablas, sus permisos y la protección del historial. [Migración 023](../deploy/azure/sql/023-visitas-domiciliarias.sql)
