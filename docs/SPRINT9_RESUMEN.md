# Sprint 9 — Guía de continuidad

## Funcionalidades y correcciones

La rama `mejora/sprint9` incorpora análisis textual determinista de compatibilidad, navegación pública, citas durante la vinculación y mejoras de seguimiento, cierre y reevaluación. La versión publicada de aplicación es `298c292372b6f54dcbea5bd7919237a6bdd5c563`; esta limpieza no despliega cambios.

Inicio (`inicio.html`) y Conocer el sistema (`conocer-sistema.html`) son páginas públicas independientes; el acceso y registro siguen en `index.html`. El encabezado compartido conserva el logotipo, la página activa y los enlaces de acceso. Se corrigieron navegación de retorno a vinculaciones, espaciados, estados rechazados, historiales, chevrones y presentación responsive, preservando el diseño existente.

## Compatibilidad y reevaluación

`ScoringService` aplica las ponderaciones configuradas y las reglas `EVIDENCIA_TEXTO_7`. `AnalizadorTextoCompatibilidad` identifica necesidades explícitas y capacidades familiares mediante patrones deterministas: considera negaciones, contexto, disponibilidad actual, especificidad y procedencia profesional vigente. Deduplica evidencias y distingue falta de información de cobertura acreditada. No diagnostica ni sustituye el criterio del Comité. Las fórmulas, redondeos y ponderaciones se conservan; el desglose existente explica los resultados sin exponer información clínica innecesaria. Detalles en [matriz de compatibilidad](MATRIZ_COMPATIBILIDAD_SPRINT9.md) y [corrección lingüística](CORRECCION_LINGUISTICA_SPRINT9.md).

- **Solicitar reevaluación:** el Comité pide a la familia actualizar o aclarar apartados concretos, con justificación y trazabilidad. No recalcula compatibilidad. Las nuevas solicitudes se marcan internamente como `sinCalculo`; su campo numérico técnico no se presenta como puntuación. Se conservan los registros anteriores.
- **Actualizar análisis:** recalcula la compatibilidad con los datos y recomendaciones vigentes y actualiza el análisis mostrado. No equivale a solicitar información a la familia ni borra decisiones históricas.

## Citas

La familia solicita desde Mis citas; Trabajo Social gestiona las vinculaciones bajo su responsabilidad. Puede aceptar, rechazar o proponer otra fecha; la familia responde a la propuesta. Cancelaciones y realización se permiten según estado, horario y rol. Cada acción conserva historial y mensajes. Se requiere vinculación habilitada y no interrumpida/cerrada; las solicitudes pendientes no reservan agenda.

Horario configurable, zona `America/Panama`, intervalos de 30 minutos y duración inicial de dos horas, con opción de una hora para Trabajo Social. El perfil `citas-demo` ofrece lunes a viernes, 08:00–16:00, exclusivamente para demostración. Sin horario válido no se confirman citas. Backend y frontend validan fechas futuras y finalización dentro del horario; las transacciones y bloqueos impiden confirmaciones superpuestas del profesional. Configuración y estados completos en [CITAS_SPRINT9.md](CITAS_SPRINT9.md).

## Vinculación y cierre

Se conservan respuesta familiar, etapas, autorización preadoptiva, hechos con sus fechas, interrupción y cierre institucional. La presentación distingue fecha efectiva de fecha de registro. Se retiró una línea visual redundante de autor/fecha sin eliminar auditoría. VIN-1 conserva la presentación histórica Autorizada → Interrumpida → Vinculación cerrada, sin inventar documentos ni fechas.

El cierre no libera automáticamente ambos expedientes: el Comité revisa por separado al niño y a la familia. Solo tras el cierre y ambas revisiones se permite el reingreso del par; siguen vigentes exclusiones expresas, reservas y demás restricciones de elegibilidad. No se borran interrupciones, decisiones ni antecedentes. Véanse [reingreso](REVISION_REINGRESO_SPRINT9.md) y [correcciones funcionales](CORRECCIONES_POSTDESPLIEGUE_SPRINT9.md).

## Componentes principales

- Backend en `src/main/java/com/senniaf/match/`: `ScoringService`, `AnalizadorTextoCompatibilidad`, `VinculacionService`, `CitaService`, `HorarioCitas`, controladores, entidades y repositorios correspondientes.
- Web en `src/main/resources/static/`: `inicio.html`, `conocer-sistema.html`, `publico.js`, `inicio.css`, `comite.js`, `senniaf.js`, `familia.js`, `profesionales.js`, `vinculaciones.js` y `citas.html/css/js`.
- Configuración: `application-citas-demo.properties`, configuración externa y `.env.example`; nunca publicar credenciales reales.
- Operación: `deploy/azure/README.md`, `actualizar.sh`, `verificar-compatibilidad.py`, respaldo y SQL versionado.

## Base de datos y ejecución

Java 17 y Maven: `mvn package -DskipTests` compila sin ejecutar pruebas. La suite habitual y el perfil de integración `e2e` están definidos en `pom.xml` y `.github/workflows/`; las integraciones requieren PostgreSQL aislado preparado con `src/test/scripts/preparar-postgres-ci.sh`.

Se necesita el esquema previo hasta 019, seguido de **020** (`citas_vinculacion`, `citas_historial`, índices y protección del historial) y **021** (reemplazo de la función `validar_combinacion_interrumpida` para respetar cierre y revisiones independientes). 021 no elimina filas ni añade tablas, columnas o permisos. Ambas están comprobadas en Azure; no repetirlas. El procedimiento no aplica migraciones automáticamente: comprobar el esquema real, respaldar y verificar compatibilidad antes de cualquier actualización. Usar siempre el actualizador documentado, SHA-256 y recuperación compatible; nunca restaurar PostgreSQL automáticamente.

## Qué conservar

Mantener permisos, fórmulas, ponderaciones, contratos de API, auditoría, fechas históricas, autenticación, identidad visual y reglas de concurrencia. Conservar las pruebas originales y las regresiones permanentes de Sprint 9, incluidos sus corpus sintéticos: son entradas de prueba, no expedientes reales. No ejecutar pruebas de carga ni crear demostraciones en producción.

La limpieza retira 115 capturas y salidas temporales versionadas; el historial `298c292` permite recuperarlas. Se conservan documentos de decisiones, corpus y comparaciones lingüísticas, resumen/diagnóstico de rendimiento y herramientas reproducibles, así como registros técnicos del despliegue. `docs/image.png` se conserva intacto, ignorado y sin incorporar al commit. Las salidas de compilación, informes y cachés son regenerables y deben quedar fuera de Git.
