# Necesidad de ajuste del esquema para el reingreso al Match

Inspección previa a cambios, 21 de noviembre de 2026. Alcance exclusivamente local.

La función `validar_combinacion_interrumpida` de 019 bloquea cualquier decisión nueva de un par que tenga una interrupción histórica, incluso si su vinculación se cerró y el Comité ya autorizó por separado la disponibilidad de ambos expedientes. Cambiar solo Java dejaría una contradicción con PostgreSQL. La revisión independiente ya existe: `estado=CERRADA`, `nino_liberado`, `familia_liberada` y sus eventos auditados; no hace falta inventar un proceso ni añadir columnas.

Corrección mínima prevista: 021 reemplazará exclusivamente el cuerpo de esa función, conservando el disparador. Una interrupción seguirá bloqueando mientras falte el cierre o cualquiera de las dos revisiones. Cerrada y revisados ambos expedientes, el antecedente por sí solo ya no excluirá el par. Las exclusiones expresas de Comité/Familia seguirán vigentes; no se borrarán ni revocarán automáticamente. Perfil incompleto, baja, adopción formalizada, reevaluación pendiente y nuevas reservas continúan impidiendo elegibilidad.

Impacto: ninguna reescritura de registros, eliminación de antecedentes, modificación de puntuaciones, ampliación de permisos ni tablas/columnas nuevas. Riesgo: el código previo continúa aplicando la exclusión absoluta de la historia y no es funcionalmente equivalente; por eso una futura actualización debe detectar 021 y exigir una marca de código compatible. El rollback no revertirá datos ni la función automáticamente.

La migración se probará solo en PostgreSQL aislado con datos sintéticos. No se ejecutará en producción ni se desplegará en esta tarea. Su aplicación futura requerirá la autorización de publicación, respaldo y procedimiento habituales.
