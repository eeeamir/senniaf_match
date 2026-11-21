# Correcciones posteriores al despliegue — Sprint 9

Trabajo exclusivamente local sobre `mejora/sprint9`, desde `789da77` (aplicación publicada previamente: `8628cea`). **Sin acceso a Azure, cambios de producción, push, merge ni despliegue durante esta tarea.** Se conserva `docs/image.png` sin modificar ni incorporar.

**Corregido** identifica un defecto resuelto; **Verificado**, una función existente comprobada sin reescribirla; **Pendiente**, una intervención adicional necesaria. Las capturas son del navegador Chromium real conectado a Spring Boot y PostgreSQL locales; no son imágenes generadas ni capturas de producción.

## 1. Páginas públicas y navegación — Corregido

**Problema y causa:** la información de roles estaba incluida en Inicio; el botón utilizaba un ancla y el acceso no compartía encabezado. La entrada por hash al registro solo se resolvía al cargar `index.html`.

**Corrección:** se trasladó la sección a `/conocer-sistema.html`, con los mismos cuatro artículos, textos, iconos y estilos. La portada permanece idéntica salvo el destino del botón. Las cuatro vistas comparten el encabezado Inicio / Conocer el sistema / Iniciar sesión / Registrarse, con estado activo, foco visible y disposición de dos columnas en móviles. El encabezado de login/registro permanece mientras cambian sus vistas; ambos siguen utilizando los formularios y lógica originales. No aparece en paneles privados. Se aumentó el contraste de los placeholders del acceso.

**Archivos:** `inicio.html`, nuevo `conocer-sistema.html`, `index.html`, `inicio.css`, nuevos `publico.css` y `publico.js`; se trasladó a este último la entrada pública al registro que antes estaba al final de `senniaf.js`.

**Pruebas y resultado:** navegación entre cuatro vistas y regreso a Inicio en 1440×1000, 1280×800, 768×1024, 390×844 y 320×844; alternancia interna y mediante encabezado; login desde formulario real; registro completo cubierto por Sprint6 E2E aislado. Sin duplicar formularios ni errores JavaScript observados. Comparación de código confirma portada, cuatro tarjetas, formularios e instrucciones originales de autenticación conservados.

**Evidencia:** [Inicio](correcciones-postdespliegue-sprint9/inicio-escritorio.png), [Conocer el sistema](correcciones-postdespliegue-sprint9/conocer-escritorio.png), [login móvil](correcciones-postdespliegue-sprint9/login-movil.png), [registro móvil](correcciones-postdespliegue-sprint9/registro-movil.png).

## 2. VIN-1: interrupción de convivencia preadoptiva — Corregido

**Problema y causa:** la quinta etapa dependía de que existiera una nueva columna de inicio de convivencia; una interrupción histórica sin esas columnas acababa mostrando «Información histórica no disponible». El resultado de la respuesta familiar cerrada se recuperaba de auditoría solo para la familia, no para el Comité. Una interrupción pendiente podía prevalecer sobre el título de cierre.

**Corrección:** el cierre institucional determina el estado principal «Vinculación cerrada». La interrupción registrada determina siempre la quinta etapa **Interrumpida**, con texto, icono y color rojo, aunque falten documentos de autorización antiguos. Se conserva la distinción entre respuesta, acompañamiento, autorización documentada y convivencia: no se inventan autorizaciones, documentos o fechas. Se muestra «Autorización no documentada en este antecedente» cuando corresponde. La respuesta histórica se recupera de la auditoría para ambas vistas. Se distinguen interrupción, revisión del Comité y cierre; se conserva la fecha del hecho si existe y la de su registro.

**Archivos:** `VinculacionService.java`, `vinculaciones.js`, `profesionales.css`, `sprint8-acabados.css`.

**Pruebas y resultado:** escenarios sintéticos cerrados con interrupción, sin autorización antigua, aceptados, rechazados y con respuesta desconocida; pruebas de proyección HTTP y nueve pruebas JavaScript de estados históricos. Escritorio, portátil, tableta y móvil. La regla no contiene condiciones por ID o por VIN-1.

**Evidencia:** [Familia](correcciones-postdespliegue-sprint9/familia-cerrada-movil.png), [Comité](correcciones-postdespliegue-sprint9/detalle-cerrada-escritorio.png). Se reprodujo localmente el escenario descrito; no se consultó ni modificó el VIN-1 real de producción.

**Autoría verificada:** se muestra el rol que consta en el registro. Si consta FAMILIA, se atribuye a la familia; si consta Trabajo Social/Psicología, se atribuye a ese profesional. El flujo actual de registro de interrupciones está autorizado a profesionales asignados; no se inventó una autoría familiar ni se ampliaron permisos para adaptar el relato. Una prueba específica cubre la presentación de autoría familiar cuando ese dato exista en un antecedente.

## 3. Mis citas: botón deshabilitado y aviso — Corregido

**Problema y causa:** el botón deshabilitado dependía correctamente del estado, pero la explicación era un párrafo ordinario y no distinguía bien las causas.

**Corrección:** aviso informativo azul con icono, título «Solicitud de citas no disponible», texto específico y separación del botón. La API informa la causa de cada vinculación: cierre, interrupción, falta de aceptación, reserva, expediente no habilitado o profesional no asignado/habilitado. Se distinguen también horarios sin configurar y solicitud abierta. No se habilita una vinculación cerrada.

**Archivos:** `CitaService.java`, `citas.js`, `citas.css`.

**Pruebas y resultado:** integración distingue propuesta pendiente, vinculación elegible y cerrada; comprobación real de botón deshabilitado y aviso en cuatro tamaños. La familia elegible solicitó y completó el recorrido de citas en la base aislada.

**Evidencia:** [aviso móvil](correcciones-postdespliegue-sprint9/aviso-citas-movil.png), [solicitud habilitada](correcciones-postdespliegue-sprint9/citas/solicitud-escritorio.png).

## 4. Historial de vinculación: línea de tiempo — Corregido

**Problema y causa:** varios renderizadores y pseudoelementos usaban el bloque completo del evento como referencia, con posiciones fijas y conectores inconsistentes.

**Corrección:** renderer compartido con CSS Grid. La primera fila contiene el título y un único punto; la segunda, fecha y descripción. La línea empieza en el centro del primer punto y termina en el centro del último; no existe conector después del último evento. También se reutiliza en Inicio e Historial de Familia, conservando textos, fechas, zona y orden de los eventos.

**Archivos:** `vinculaciones.js`, `familia.js`, `profesionales.css`.

**Pruebas y resultado:** medidas del DOM con uno, dos y cinco eventos, títulos cortos/largos y fechas distintas, en escritorio y móvil; además, historial real de las vinculaciones sintéticas. La diferencia vertical medida entre centro del título y punto es menor de 0,6 px; los puntos comparten eje horizontal. No se modificaron datos para corregir CSS.

**Evidencia:** [historial móvil](correcciones-postdespliegue-sprint9/historial-movil.png), [historial familiar](correcciones-postdespliegue-sprint9/historial-familia-movil.png), [un evento](correcciones-postdespliegue-sprint9/linea-1-eventos.png), [geometría](correcciones-postdespliegue-sprint9/navegador-revision-final.json).

## 5. Modal de información del niño asignado — Corregido

**Problema y causa:** el aviso y el botón Cerrar carecían de separación específica.

**Corrección:** clase propia del diálogo y margen de 24 px antes de Cerrar; se conserva tamaño, estilo, información y comportamiento del botón. El foco inicial se coloca en el título y el diálogo abre arriba, evitando que el autofoco del botón lleve directamente al final del contenido.

**Archivos:** `vinculaciones.js`, `profesionales.css`.

**Pruebas y resultado:** modal real con datos estructurados sintéticos; separación medida de 24 px, apertura y cierre correctos en escritorio y móvil.

**Evidencia:** [escritorio](correcciones-postdespliegue-sprint9/modal-nino-escritorio.png), [móvil](correcciones-postdespliegue-sprint9/modal-nino-movil.png), [cierre tras desplazamiento](correcciones-postdespliegue-sprint9/modal-nino-movil-cerrar.png), medidas en los JSON de navegador.

## 6. Comité: cierre institucional — Corregido

**Problema y causa:** «Reserva conservada» describía el bloqueo técnico, sin explicar que faltaba revisar la disponibilidad. La información del cierre y de ambos expedientes compartía el mismo bloque.

**Corrección:** bloques de Cierre institucional, Estado de los expedientes y Próximo paso. Se muestran fecha, responsable y actuación; cada expediente tiene su estado y explicación. Los estados distinguen Pendiente de revisión, Disponibilidad autorizada, Condiciones incompletas y Otra asignación vigente. La API los deriva de la revisión y de las condiciones/reservas actuales; no garantiza un reingreso si después surgió otra restricción. «Revisar disponibilidad» conserva la actuación institucional; «Consultar expediente» solo navega y conserva el regreso a la vinculación. Aclaración legal secundaria, sin eliminarla.

**Archivos:** `VinculacionService.java` (proyección), `vinculaciones.js`, `profesionales.css`, `sprint8-acabados.css`.

**Pruebas y resultado:** estados parciales y completos verificados en HTTP/PostgreSQL; paneles y enlaces en navegador, cuatro tamaños. Sin cambiar permisos ni el método que autoriza cada revisión.

**Evidencia:** [detalle y cierre](correcciones-postdespliegue-sprint9/detalle-cerrada-escritorio.png), [móvil](correcciones-postdespliegue-sprint9/detalle-cerrada-movil.png).

## 7. Estados de vinculación: rechazo en rojo — Corregido

**Problema y causa:** el detalle elegía icono y color por existencia de fecha; una respuesta rechazada con fecha se mostraba como éxito. Inicio utilizaba además un color anaranjado para el paso interrumpido/rechazado.

**Corrección:** el resultado determina la semántica: aceptación verde, rechazo/interrupción rojo, espera y ausencia de inicio neutrales. Texto e icono acompañan al color. Se conserva que la respuesta ocurrió, sin completar etapas posteriores. La regla se aplica al resumen, detalle y seguimiento familiar, incluido Inicio.

**Archivos:** `vinculaciones.js`, `profesionales.css`, `sprint8-acabados.css`.

**Pruebas y resultado:** prueba con rechazo fechado, prueba de etapas posteriores no completadas y comprobación de color calculado en el DOM de Inicio y Mi vinculación. Sin cambiar la decisión registrada.

**Evidencia:** [Mi vinculación](correcciones-postdespliegue-sprint9/familia-rechazada.png), [Inicio familiar](correcciones-postdespliegue-sprint9/inicio-familia-rechazada-movil.png).

## 8. Citas: jerarquía y experiencia de usuario — Corregido; reglas existentes verificadas

**Problema y causa:** horario, mensajes y estados tenían una jerarquía parecida; el historial repetía siempre todo el contenido; Cancelar resultaba ambiguo.

**Corrección:** vinculación/motivo, estado con explicación breve, bloque de fecha/inicio/finalización/duración, mensaje, acciones e historial desplegable. Iconos funcionales reutilizados. El historial identifica acción, responsable, instante y cambios de horario; los mensajes completos se conservan en desplegables. La respuesta anterior sigue disponible allí, sin duplicarla en la tarjeta principal. Rechazo rojo y cancelación secundaria. «Cancelar solicitud» se distingue de «Cancelar cita» confirmada y del botón que solo cierra un formulario; la cancelación continúa exigiendo confirmación.

**Archivos:** `citas.js`, `citas.css`.

**Pruebas y resultado:** recorrido completo en Chromium contra PostgreSQL como `senniaf_app`: solicitud, revisión de aceptación sin guardar, propuesta, contrapropuesta, aceptación de una hora, aceptación familiar de otra propuesta, rechazo y cancelación. Mensajes editables conservados, restablecimiento explícito, contenido HTML tratado como texto, historial de cuatro eventos y móvil de 390/320 px. Teclado, Escape, retorno de foco y botones alcanzables mediante desplazamiento interno. Las 18 pruebas PostgreSQL previas de citas y las unitarias de horario/servicio siguen pasando. Horario, duraciones, disponibilidad, permisos y transacciones no se modificaron.

**Evidencia:** [tarjetas e historial](correcciones-postdespliegue-sprint9/citas/familia-confirmada-historial.png), [Trabajo Social](correcciones-postdespliegue-sprint9/citas/trabajo-social-escritorio.png), [proponer en móvil](correcciones-postdespliegue-sprint9/citas/proponer-movil.png), [recorrido](correcciones-postdespliegue-sprint9/citas/navegador.json).

## 9. Cierre institucional: revisión independiente y reingreso — Verificado; comunicación corregida

**Inspección:** ya existen `nino_liberado`, `familia_liberada` y eventos de disponibilidad. Solo el Comité autoriza cada expediente tras un cierre; las condiciones de completitud, baja/adopción y reevaluación se comprueban por separado. Los repositorios de reservas ya consideran individualmente cada liberación, aunque el otro expediente siga retenido. No se reescribió este proceso.

**Corrección de comunicación:** se explica que ambos expedientes se revisan por separado y que, una vez autorizados y elegibles, pueden participar en nuevas propuestas; no se promete una asignación. La interfaz presenta cada resultado real de forma independiente. El bloqueo adicional del par interrumpido se corrige en el área 10.

**Archivos:** proyección de cierre en `VinculacionService.java`, presentación en `vinculaciones.js` y CSS compartido.

**Pruebas y resultado:** suites de cierre/HU11 más nuevas pruebas HTTP: sin liberación automática; liberar uno no libera el otro; nueva reserva vuelve a bloquear; un expediente no elegible no se libera; solo Comité; auditorías y decisiones anteriores conservadas. Comprobación de estados posteriores en UI y del retorno al ranking.

**Evidencia:** [detalle del cierre](correcciones-postdespliegue-sprint9/detalle-cerrada-escritorio.png), [pruebas por clase](correcciones-postdespliegue-sprint9/pruebas.json), [comprobación adicional](correcciones-postdespliegue-sprint9/accesibilidad.json).

## 10. Motor y botón Reevaluar — Corregido y verificado

**Causas encontradas:** el filtro de pares y el disparador de 019 excluían toda interrupción histórica, incluso con cierre y ambas revisiones autorizadas. El proceso vigente calculaba con datos actuales en cada GET, pero el análisis abierto reutilizaba `Comite.casos` en memoria y no existía un botón literal Reevaluar; «Solicitar reevaluación» solicita una actualización a la familia, no ejecuta ese recálculo.

**Corrección del par:** la consulta de interrupciones que bloquean elegibilidad considera únicamente antecedentes sin cierre o sin cualquiera de las dos revisiones. La consulta de existencia histórica se conserva separada; no se borra ninguna interrupción. Las exclusiones expresas del Comité/Familia siguen bloqueando, al igual que perfiles no elegibles, reevaluaciones pendientes y reservas vigentes. No se fuerzan recomendaciones ni puntuaciones.

**Esquema necesario:** nueva **021**, documentada antes de modificar el esquema en [REVISION_REINGRESO_SPRINT9.md](REVISION_REINGRESO_SPRINT9.md). Reemplaza exclusivamente el cuerpo de `validar_combinacion_interrumpida` para que PostgreSQL respete la misma revisión independiente. No cambia filas, tablas, columnas ni permisos. Se aplicó solo en dos bases sintéticas locales; **pendiente de autorización y aplicación futura en producción**, sin despliegue en esta tarea. El actualizador y auxiliar detectan 021 y exigen una marca de código compatible, también para recuperación; no migran automáticamente.

**Corrección de Reevaluar:** botón discreto dentro del desglose existente. Realiza un GET nuevo sin caché, sustituye el caso en memoria y vuelve a presentar el resultado, con estado de carga y prevención de acciones simultáneas. Conserva «Solicitar reevaluación» como acción diferente. Consume los perfiles actuales, sujetos a las mismas reglas de completitud y reevaluación pendiente. No añade un nuevo proceso de autorización. Los cálculos anteriores guardados en decisiones permanecen inmutables; una consulta no los reemplaza ni finge crear una decisión nueva.

**Archivos:** `InterrupcionConvivenciaRepository.java`, exclusión de pares en `ScoringService.java`, `comite.js`, `021-reingreso-revisado.sql`, marca `META-INF/senniaf-reingreso-revisado.properties`, preparación PostgreSQL de CI y verificador/actualizador. Fórmulas, pesos, analizador y `EVIDENCIA_TEXTO_7` permanecen intactos.

**Pruebas y resultado:** seis pruebas nuevas PostgreSQL cubren ambas revisiones, impedimento con una pendiente, salvaguarda SQL, exclusión expresa vigente, conservación de decisiones y recálculo después de PUT familiar. En una prueba se modifica capacidad médica y el ranking aumenta usando datos nuevos; en navegador se modifica el rango de edad y el resultado disminuye tras pulsar Reevaluar. Ningún puntaje se modifica manualmente. Tres pruebas nuevas del auxiliar impiden publicar/recuperar JAR incompatible con 021.

Se actualizaron explícitamente dos expectativas de HU11/cierre que codificaban la prohibición permanente ahora retirada por el requisito del usuario. Se conservó la comprobación SQL de bloqueo antes de las revisiones y las comprobaciones de reservas/privacidad; no se eliminaron pruebas para ocultar fallos. El nuevo caso añade la aceptación efectiva y la preservación del antecedente después de ambas revisiones.

**Evidencia:** [antes de Reevaluar](correcciones-postdespliegue-sprint9/reevaluacion-antes.png), [después](correcciones-postdespliegue-sprint9/reevaluacion-despues.png), [registro de navegador](correcciones-postdespliegue-sprint9/navegador.json).

**Límite institucional:** no existe un proceso definido para revocar exclusiones expresas del Comité o de la familia. No se inventó uno ni se levantaron automáticamente. La corrección elimina el bloqueo por mera historia interrumpida ya revisada; no revoca decisiones expresas todavía vigentes.

---

## Pruebas, accesibilidad e integridad de la entrega

| Grupo | Ejecutadas | Satisfactorias | Fallidas | Errores | Omitidas |
| --- | ---: | ---: | ---: | ---: | ---: |
| Java unitarias | 1051 | 1051 | 0 | 0 | 0 |
| Java HTTP/PostgreSQL | 83 | 83 | 0 | 0 | 0 |
| Python, compatibilidad | 16 | 16 | 0 | 0 | 0 |
| JavaScript, estados históricos | 9 | 9 | 0 | 0 | 0 |
| **Total** | **1159** | **1159** | **0** | **0** | **0** |

Java 17: `mvn -B verify -Pe2e`, con variables `HU19_TEST_DB_*` apuntando exclusivamente a la base aislada y contraseña sintética explícita para los escenarios demo. Integración como `senniaf_app`, sin ampliar privilegios. Python: `python3 -m unittest discover -s src/test/scripts -p test_compatibilidad_despliegue.py -q`. JavaScript: `node --test src/test/scripts/test_vistas_sprint9.cjs`. Sintaxis de seis archivos JavaScript, scripts Bash y `git diff --check` correctos. Los reintentos no se suman al total.

PostgreSQL 16.4 local: una base para la suite y otra pequeña para navegador. Para reproducir un antecedente anterior a 019 sin inventarle autorización se insertó una fixture sintética con excepción del disparador limitada a esa transacción local. La aplicación de navegador continuó usando el rol restringido `senniaf_app`; nunca se relajó un disparador en producción.

Navegador real en cinco resoluciones, teclado y muestra de contraste computado. Se corrigieron tres textos secundarios con contraste inferior a 4,5:1. La muestra final revisa encabezado, estados, explicación familiar, cierre, aviso y tarjetas de citas; mínimos y colores constan en [accesibilidad](correcciones-postdespliegue-sprint9/accesibilidad.json). No constituye una certificación completa con lector de pantalla.

[Integridad de código](correcciones-postdespliegue-sprint9/integridad.json) confirma conservación byte a byte de fórmulas/explicaciones, analizador, pesos y programación, portada (excepto enlace), cuatro tarjetas y formularios/lógica de acceso. Las decisiones y eventos históricos de las pruebas se conservan y sus snapshots se verifican. **No se accedió ni escribió a producción en esta tarea.**

Evidencia final: [recorrido visual](correcciones-postdespliegue-sprint9/navegador-revision-final.json), [conteos por clase](correcciones-postdespliegue-sprint9/pruebas.json) y `SHA256SUMS` del directorio de evidencias.

## Archivos y commits

Creados: `conocer-sistema.html`, `publico.css`, `publico.js`, migración 021 y marca de compatibilidad; `CorreccionesSprint9PostgresIT.java`, `test_vistas_sprint9.cjs`; revisión previa del esquema, este informe y evidencias.

Modificados: `inicio.html`, `inicio.css`, `index.html`, `senniaf.js`, `familia.js`, `vinculaciones.js`, `comite.js`, `citas.js`, `citas.css`, `profesionales.css`, `sprint8-acabados.css`; `VinculacionService.java`, `CitaService.java`, filtro de `ScoringService.java`, `InterrupcionConvivenciaRepository.java`; actualizador, auxiliar y README de Azure (solo archivos locales), preparación PostgreSQL y workflow; dos pruebas PostgreSQL de regla revisada y pruebas de compatibilidad.

Backend y reglas revisadas: commit local `7d7fc1f`. El commit de interfaz y evidencias se detalla en la entrega final. No hubo push, merge ni despliegue. `main` y las demás ramas se mantienen intactas. El único archivo ajeno pendiente se conserva como `?? docs/image.png`.
