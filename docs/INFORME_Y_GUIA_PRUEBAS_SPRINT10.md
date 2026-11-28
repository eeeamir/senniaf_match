# SENNIAF Match — Informe de desarrollo y guía de pruebas del Sprint 10

**Periodo:** 23–28 de noviembre de 2026 (hora de Panamá).  
**Versión funcional de referencia:** [c52414b](https://github.com/eeeamir/senniaf_match/commit/c52414b2361225bcad78374096cfcbd3954a5bcf).  
**Estado:** desarrollo funcional cerrado por el equipo; pruebas finales pendientes.  
**Propósito:** explicar el trabajo realizado y orientar a la integrante responsable de las pruebas para el Review Final.

Este informe distingue funcionalidades desarrolladas, comprobaciones puntuales reportadas y pruebas propuestas. No certifica la calidad final del producto. Los resultados esperados son criterios de aceptación, no resultados obtenidos.

## 1. Objetivo y alcance del sprint

Completar el acompañamiento profesional de las vinculaciones, consolidar las decisiones institucionales de cierre, permitir la reincorporación familiar cuando corresponda, refinar el motor de compatibilidad y mejorar la consistencia de los portales. El sistema apoya la gestión profesional; no decide adopciones ni reemplaza la autoridad competente.

| Elemento | Trabajo | SP | Situación |
|---|---|---:|---|
| HU20/PBI39 | Seguimiento social y psicológico | 2 | Implementado |
| HU25 | Revocación de asignaciones | 1 | Implementado |
| PBI42 | Cierre institucional y reincorporación | 2 | Implementado |
| PBI34–35 | Validación funcional, integración y seguridad | 2 | Pendiente de pruebas finales |
| PBI36 | Documentación y entrega | 1 | En proceso |
| | Total | 8 | |

PBI46 es la numeración propuesta para HU25 en la regularización del backlog final, no un identificador histórico comprobado en Jira. Los ajustes del motor, citas y presentación son refinamientos de funcionalidades anteriores, sin SP adicionales asignados en esta guía. El módulo independiente de incidencias y alertas (HU21/PBI40) se retiró del alcance; las incidencias de asignación necesarias se integraron al flujo de revocación.

## 2. Trabajo desarrollado

### 2.1. Seguimientos profesionales (HU20/PBI39)

Se incorporaron los registros de seguimiento social y psicológico asociados a las vinculaciones. Cada profesional registra su intervención, los participantes, las fechas y la información de la actuación, manteniendo su historial y permisos. Se revisaron los identificadores, formularios, selección de participantes, campos y disposición de botones. Se ajustaron los controles horarios de inicio y fin, la legibilidad y la duración de las actuaciones. Los controles de horarios válidos, conservación al editar y acceso por rol deben comprobarse formalmente.

Componentes: `SeguimientoVinculacionController`, `SeguimientoVinculacionService`, `SeguimientoVinculacion`, `EventoSeguimientoVinculacion`, `seguimientos-vinculacion.js`; migración 022. Commits: `da4b6e7`, `ebf8ef7`, `f90c194`, `f64e804`.

### 2.2. Visitas domiciliarias y autorización preadoptiva

Se incorporó el registro de visitas domiciliarias con historial, asociado a la vinculación. Se ajustaron formularios, horarios, indicadores y encabezados. El diseño utiliza 30 días calendario y tres visitas como referencias de seguimiento según la documentación del proyecto, sin producir aprobación automática; admite visitas adicionales cuando el estado lo permite. Se distingue entre indicadores y actuaciones efectivamente registradas.

La autorización preadoptiva se ajustó para usar la hora del servidor y registrar el inicio conforme al flujo vigente. Los registros históricos no deben adquirir fechas o documentos por suposición. Se unificó el progreso entre los portales y se revisó la presentación de la autorización.

Componentes: `VisitaDomiciliariaService`, `VisitaDomiciliariaController`, `TiemposVinculacion`, `visitas-domiciliarias.js`; migraciones 023 y 026. Commits: `b696d9d`, `1040864`, `fff33e6`, `b086ffa`. Consultar `SEGUIMIENTO_TEMPORAL_SPRINT10.md`.

### 2.3. Refinamiento del motor de compatibilidad

- Revisión de la interpretación determinista de las respuestas de la entrevista familiar, en especial las expresiones de capacidad general de cuidado ante condiciones de salud. No se incorporó inteligencia artificial generativa.
- Tratamiento de criterios no aplicables: excluir su peso del máximo aplicable, sin redistribuirlo arbitrariamente. Por ejemplo, si el peso configurable de hermanos es 25 y no aplica, el máximo restante es 75; una coincidencia completa se expresa 75/75. Usar los pesos vigentes, no asumir que son constantes.
- Diferenciación entre ausencia documentada de necesidad, falta de información, capacidad declarada, respuestas negativas y condicionadas. Una coincidencia de palabras no debe convertir una negación en aceptación.
- Mejora del análisis y explicación por criterio, antecedentes, historial de compatibilidad y distinción entre el resultado original y el análisis posterior.
- El Comité conserva la decisión. Una puntuación alta no reemplaza los requisitos de elegibilidad ni autoriza automáticamente una vinculación.

Componentes: `AnalizadorTextoCompatibilidad`, `ScoringService`, `ScoringController`, `vinculaciones.js`. Commits: `da4b6e7`, `1fa585c`, `2100141`, `4f0f426`. Son mejoras de HU03/HU22, no una nueva historia independiente de IA.

### 2.4. Citas de Trabajo Social (refinamiento HU24)

Se refinó el flujo de citas iniciado en Sprint 9: solicitud familiar, respuesta de Trabajo Social, confirmación, rechazo, contrapropuesta y respuesta a esta. Se ajustaron la presentación de estados, mensajes, acciones, cancelación, cita realizada e inasistencia, preservando las propuestas anteriores y el historial. Se usa la referencia horaria de Panamá. Se mejoró la navegación y la lectura de las tarjetas.

Componentes: `CitaService`, `CitaHistorialRepository`, `citas.js`, `citas.css`. Commits: `1fa585c`, `bd46c75`, `5eadd84`.

### 2.5. Cierres institucionales y revocación (HU25/PBI42)

Se diferenciaron los resultados de cierre según las actuaciones registradas:

- **Adopción formalizada:** registra el resultado institucional cuando corresponde. El expediente infantil queda fuera del Match sin reactivación ordinaria. El familiar queda fuera hasta una reincorporación autorizada si reúne las condiciones vigentes.
- **Interrupción de convivencia:** corresponde a convivencia efectivamente iniciada e interrupción documentada. No equivale a la revocación de una asignación antes de la convivencia.
- **Revocación de asignación:** permite registrar una incidencia de asignación —desistimiento familiar, rechazo del niño u otro motivo—, someterla a revisión fundamentada y cerrar por revocación cuando procede. No produce cierre automático ni constituye un módulo general de alertas. Se distingue la revisión posterior de disponibilidad de cada expediente.
- **Antecedentes históricos:** se conservan los cierres anteriores sin clasificación suficiente; no se inventan resultados ni fechas.

Se corrigió la disponibilidad de las acciones de cierre según la etapa y las actuaciones documentadas, conservando responsables, motivos e historial.

Componentes: `VinculacionService`, `IncidenciaAsignacion`, `IncidenciaAsignacionRepository`, `CierreInstitucionalRequest`; migraciones 024 y 025. Commits: `206be9b`, `d89a18f`, `035e8c6`, `2100141`.

### 2.6. Reincorporación familiar

La reincorporación corresponde al expediente familiar, conservando la vinculación que originó el cierre y la cuenta familiar.

1. Una familia elegible solicita reincorporación y confirma la actuación.
2. Actualiza y confirma su entrevista. El Comité no puede resolver antes de la confirmación.
3. La solicitud queda pendiente de revisión institucional. La familia continúa fuera del Match.
4. El Comité autoriza o no autoriza, con confirmación y fundamento (el servicio exige entre 10 y 2000 caracteres).
5. Si autoriza, se libera la reserva familiar, sujeto a las condiciones vigentes: perfil completo, ausencia de reevaluación pendiente y de otra reserva, entre otras reglas de elegibilidad.
6. Si no autoriza, la familia consulta la decisión y su motivo; permanece fuera del Match y puede volver a solicitar si cumple las condiciones. Cada intento conserva su entrevista y resolución, y solo se admite una solicitud pendiente a la vez.
7. Se conserva la cuenta, expediente, antecedentes y vinculación cerrada. El portal refleja la aprobación y la disponibilidad; una nueva propuesta debe distinguirse de la reincorporación anterior.

La migración 027 incorporó los reintentos tras denegación. El servicio utiliza bloqueo transaccional para las actuaciones sobre solicitudes. La no autorización no implica un bloqueo permanente.

Componentes: `ReincorporacionService`, `SolicitudReincorporacion`, `SolicitudReincorporacionRepository`, `PerfilFamiliaService`, `familia.js`. Commits: `206be9b`, `12fc8f6`, `c52414b`.

### 2.7. Navegación e interfaz

- Acceso directo al detalle de la vinculación creada al enviar la propuesta desde Comité.
- Separación de acciones, propuesta, progreso e historial en el portal familiar; consistencia de fechas y estados entre roles.
- Agrupación de antecedentes y reevaluaciones; mejora visual del análisis por criterio.
- Panel de gestión del expediente familiar compacto, desplegable, de ancho completo y contraído por defecto; estado sin duplicación y acceso a la vinculación específica de cierre. Se simplificó la información repetida, sin eliminar el historial almacenado.
- Tarjetas de expediente infantil y familiar alineadas en la vista de cierre, con la acción de consultar solicitud dentro de la tarjeta familiar cuando corresponda.
- Avisos de reincorporación aprovechando el ancho disponible; ajuste de márgenes, modales, botones, encabezados, iconos y controles horarios. Se conserva el diseño institucional.
- Actualización de explicaciones públicas y documentación del flujo. Preparación de escenarios ficticios para demostración en entornos autorizados.

Commits: `dc253d8`, `4a399a8`, `1dc9391`, `6b3d14b`, `728d06c`, `589d7c9`.

## 3. Evolución y gestión técnica

El historial Git muestra la secuencia de 37 commits desde la integración inicial hasta c52414b. Fechas convertidas a hora de Panamá; no equivalen a horas laborales imputadas.

| Fecha | Actividad principal | Commits representativos |
|---|---|---|
| 23–25 nov. | Revisión, preparación y priorización. Primera integración registrada el 25 por la noche. | da4b6e7 |
| 26 nov. | Integración local, seguimientos, visitas, preparación de demostración, formularios y esquema de CI. | eb1b8f0, bd8b184, 7956dd3, 68547ce, ebf8ef7, 128f210 |
| 27 nov. | Cierre institucional, reincorporación, revocación, motor, citas, antecedentes, autorización, navegación, horarios y acabados. | 206be9b, d89a18f, 1fa585c, 1040864, f64e804, 12fc8f6, 6b3d14b, 589d7c9 |
| 28 nov. (00:01 aprox.) | Reintentos después de denegación. | c52414b |

No se dispone de un registro diario completo de horas; no se inventan imputaciones. El informe agrupa los cambios por funcionalidad, no reproduce literalmente cada conversación.

### Base de datos y publicación

| Migración | Propósito |
|---|---|
| 022 | Seguimientos profesionales e historial |
| 023 | Visitas domiciliarias e historial |
| 024 | Resultados de cierre y solicitudes de reincorporación |
| 025 | Incidencias y revocación de asignaciones |
| 026 | Autorización con fecha del servidor |
| 027 | Reintentos de reincorporación |

La integración utiliza Java/Spring, JavaScript y PostgreSQL. Consultar `INTEGRACION_LOCAL_SPRINT10.md` antes de preparar un entorno. No reaplicar migraciones ya ejecutadas.

Se verificó mediante GitHub que `main` y `sprint-10` estaban sincronizadas en c52414b. El reporte de despliegue indica Azure operativo con HTTP 200, respaldo privado previo a 027 y huellas coincidentes de 28 tablas ajenas a sesiones antes y después. Estas últimas son comprobaciones reportadas por el equipo, no una auditoría independiente de Azure o PostgreSQL.

Aplicación: https://senniafmatch.westus2.cloudapp.azure.com/ . Repositorio: https://github.com/eeeamir/senniaf_match . No realizar pruebas destructivas en el entorno de demostración. Publicar este Markdown no requiere despliegue ni migración adicional.

## 4. Estado de calidad previo a las pruebas finales

Se realizaron comprobaciones visuales e integraciones puntuales. El último reporte indica que en una copia aislada se comprobó el nuevo envío tras denegación, la actualización de entrevista, el rechazo del duplicado (HTTP 409) y la conservación de dos denegaciones con sus motivos. También se reportó que una reincorporación ya aprobada seguía vigente en Azure. Esto no constituye una suite final ni permite calcular un porcentaje global de aprobación.

**GitHub Actions:** la ejecución [46110180064](https://github.com/eeeamir/senniaf_match/actions/runs/46110180064), asociada a c52414b, falló en la comprobación de recursos web. Se revisaron los registros: seis fallos en `src/test/scripts/test_vistas_sprint9.cjs`.

| Caso | Fallo observado |
|---|---|
| Interrupción registrada | Propiedad `texto` no disponible |
| Rechazo con fecha | `Profesionales.momento is not a function` |
| Autorización sin inicio | Discrepancia `actual` / `completado` |
| Marcadores y títulos de eventos | `Profesionales.momento is not a function` |
| Autoría y fecha del hecho | Propiedad `detalle` no disponible |
| Caso histórico VIN-1 | Discrepancia `Interrumpida` / `Autorizada` |

La preparación del esquema y la fase de compilación y pruebas unitarias/de integración fueron omitidas después del fallo. Se debe revisar si cada caso representa un defecto, una dependencia no cargada en el entorno de prueba o una expectativa antigua incompatible con la regla aprobada. No eliminar pruebas ni modificar antecedentes para obtener una ejecución verde. El equipo reportó que los fallos eran preexistentes; su causa y antigüedad no se certifican en este informe.

## 5. Guía de pruebas para la responsable de calidad

### Preparación y orden recomendado

1. Identificar el commit funcional, configurar Java 17 según CI, PostgreSQL y dependencias; verificar el esquema y migraciones 022–027 en una base aislada autorizada. Mantener respaldo privado. No reutilizar credenciales o datos personales reales.
2. Preparar cuentas de Comité, Trabajo Social, Psicología y Familia, con expedientes ficticios variados. Utilizar casos independientes para cada tipo de cierre y evitar cambios irreversibles en la base de demostración.
3. Revisar las pruebas existentes y el workflow; reproducir los seis fallos e investigar su causa. Ejecutar unitarias, integración y comprobaciones web usando los comandos y perfiles definidos por el proyecto. No cambiar expectativas sin justificar la regla vigente.
4. Priorizar los casos críticos y negativos, luego regresión del motor, citas, seguridad, persistencia y usabilidad. Registrar cada resultado real, corregir defectos confirmados y repetir los casos afectados.

### 5.1. Adecuación funcional

Todos los casos siguientes son **propuestos, pendientes de ejecución formal**. CP son identificadores locales de esta guía; P0 indica prioridad crítica y P1 regresión importante.

| ID | Prioridad / trazabilidad | Procedimiento | Resultado esperado |
|---|---|---|---|
| CP01 | P0 HU03 | Comité revisa candidatos y explicación; selecciona familia y envía propuesta. | Una sola vinculación; se abre detalle y las partes ven el estado correspondiente. |
| CP02 | P0 HU19 | Aceptar propuesta; en otro caso rechazarla. | Respuesta persistida, historial; rechazo no completa etapas siguientes; sin doble respuesta. |
| CP03 | P0 HU20 | Trabajo Social registra seguimiento con participantes y campos válidos. | Registro asociado al caso, autor, fecha e historial. |
| CP04 | P0 HU20 | Psicología registra su intervención sobre la misma vinculación. | Registro independiente, sin sobrescribir seguimiento social. |
| CP05 | P0 HU20 | Guardar con campos obligatorios vacíos o fin anterior/igual al inicio. | Validación; sin registro inválido o parcial. |
| CP06 | P1 HU20 | Registrar y editar horario; recargar. Probar intervalos de 5 min y hora exacta si disponible. | Horarios persistidos, duración coherente, formato legible. |
| CP07 | P0 HU20 | Registrar visita domiciliaria en etapa permitida. | Visita asociada; contador e historial reflejan registro real. |
| CP08 | P0 HU20 | Intentar visita en etapa no habilitada o caso cerrado. | No disponible o rechazada por servidor, sin cambio de datos. |
| CP09 | P1 HU20 | Registrar tres visitas y una adicional en etapa habilitada. | Indicador coherente; sin visitas ficticias ni cierre automático. |
| CP10 | P0 PBI42 | Autorizar acogimiento preadoptivo; consultar y recargar ambos portales. | Hora del servidor, inicio y estado según regla vigente. |
| CP11 | P0 PBI42 | Consultar casos históricos sin inicio efectivo registrado. | No se inventan fechas, documentos ni hitos. |
| CP12 | P0 PBI42 | Registrar interrupción de convivencia iniciada; Comité revisa y cierra cuando corresponda. | Actuación, motivo y cierre coherentes; sin confundir con revocación. |
| CP13 | P0 HU25 | Registrar incidencia con justificación, Comité revisa y determina que procede revocar; cierra. | Resultado, responsables y fechas conservados; sin cierre automático. |
| CP14 | P0 HU25 | Comité determina que una incidencia no justifica revocación. | Fundamento conservado; cierre por revocación no habilitado indebidamente. |
| CP15 | P0 HU25 | Revisar disponibilidad posterior de ambos expedientes tras revocación. | Disponibilidad individual según confirmación y elegibilidad, sin reservas duplicadas. |
| CP16 | P0 PBI42 | Cerrar por adopción en caso válido y en otro caso intentar en etapa inválida. | Solo procede cuando corresponde; niño fuera del Match sin reactivación ordinaria, familia fuera hasta reincorporación. |
| CP17 | P0 PBI42 | Familia elegible solicita reincorporación. | Solicitud asociada al expediente y cierre correctos; entrevista pendiente, fuera del Match. |
| CP18 | P0 PBI42 | Resolver antes de confirmar entrevista; luego actualizar y enviar entrevista. | Resolución anticipada rechazada; después pendiente de revisión. |
| CP19 | P0 PBI42 | Resolver sin fundamento o con menos de 10 caracteres; después autorizar correctamente. | Decisión incompleta rechazada; autorización válida persistente y retorno si elegible. |
| CP20 | P0 PBI42 | Denegar con fundamento; revisar portal y refrescar sesión. | Decisión y motivo visibles; fuera del Match y nueva solicitud disponible si elegible. |
| CP21 | P0 PBI42 | Volver a solicitar tras denegación; confirmar otra entrevista y resolver. | Intentos independientes; entrevistas y resoluciones previas conservadas. |
| CP22 | P0 PBI42 | Intentar dos solicitudes pendientes, también concurrentes en entorno aislado. | Solo una aceptada; duplicada rechazada (HTTP 409 según reporte puntual). |
| CP23 | P0 PBI42 | Intentar autorizar con reevaluación pendiente, perfil incompleto u otra reserva. | No se libera familia indebidamente. |
| CP24 | P0 PBI42 | Tras autorización, comprobar Inicio, Vinculación y nueva propuesta en ambiente de prueba. | Disponibilidad correcta, nueva propuesta diferenciada e historial anterior conservado. |
| CP25 | P1 HU24 | Solicitar cita, proponer otro horario y aceptar. | Estado y horario coherentes; propuesta y respuesta en historial. |
| CP26 | P1 HU24 | Rechazar, cancelar, marcar realizada o registrar inasistencia en casos separados. | Resultados diferenciados y persistidos, sin acciones incompatibles. |
| CP27 | P1 Interfaz | Expandir gestión familiar, consultar solicitud y cierre de origen. | Un indicador, panel compacto, acciones en objeto correcto e historial accesible donde corresponda. |
| CP28 | P1 Interfaz | Revisar avisos, tarjetas, modales y formularios en escritorio y móvil. | Sin textos comprimidos, controles superpuestos ni botones desalineados. |

### 5.2. Regresión del motor de compatibilidad

| ID | Escenario | Resultado esperado |
|---|---|---|
| CM01 | Niño sin hermanos relevantes; familia declara no poder acoger grupo. | Criterio no aplicable excluido del máximo; sin penalización ni redistribución. |
| CM02 | Niño con hermanos y necesidades de vínculo; familias con diferentes capacidades. | Evaluación y explicación conforme a la necesidad registrada. |
| CM03 | Familia declara capacidad para cualquier condición de salud; niño con condición registrada. | Reconocimiento de capacidad general conforme a reglas; sin inferir experiencia médica no declarada. |
| CM04 | Respuestas negativas, condicionadas o ambiguas, incluyendo intolerancia a la lactosa. | No interpretar negación como aceptación; distinguir condiciones y datos faltantes. |
| CM05 | Niño sin condiciones conocidas frente a niño con información insuficiente. | Diferenciar ausencia documentada de necesidad y dato desconocido. |
| CM06 | Cambiar pesos con usuario autorizado e intentar suma diferente de 100. | Validación; pesos actuales aplicados; historial anterior conservado. |
| CM07 | Actualizar entrevista y recalcular, comparar original y nuevo. | Resultados diferenciados y explicables; sin modificación silenciosa del histórico. |
| CM08 | Familia con puntuación alta pero no elegible/fuera del Match. | No disponible para selección por el puntaje únicamente. |

Calcular manualmente al menos un ejemplo por criterio con los pesos vigentes y comparar cálculo, explicación y presentación. No utilizar pesos fijos si son configurables.

### 5.3. Integración, seguridad y persistencia

| ID | Escenario | Resultado esperado |
|---|---|---|
| CS01 | Consultar recursos privados sin sesión. | Autenticación o acceso denegado; sin datos privados. |
| CS02 | Familia cambia identificador para consultar otro expediente o solicitud. | Denegación del servidor; sin exposición. |
| CS03 | Profesional intenta actuar fuera de sus permisos o casos asignados. | Denegación incluso desde petición directa. |
| CS04 | Rol no autorizado intenta resolver reincorporación o cierre. | Actuación impedida; estado intacto. |
| CS05 | Texto con caracteres especiales y HTML/script inocuo en ambiente aislado. | Representación segura; sin ejecución de contenido ni corrupción. |
| CS06 | Recargar y volver a iniciar sesión tras actuaciones. | Persistencia de estado, fecha, autor y fundamento. |
| CS07 | Simular actuación fallida y concurrencia en ambiente aislado. | Transacción consistente; sin registros huérfanos o duplicados incompatibles. |
| CS08 | Revisar esquema, permisos, respaldo y restauración en ambiente distinto. | Datos previos conservados y recuperación verificable. |
| CS09 | Inspeccionar respuestas, logs y archivos compartidos. | Sin exposición de credenciales, tokens o datos sensibles. |

Realizar las comprobaciones de seguridad exclusivamente en entornos autorizados y con datos ficticios. No publicar contraseñas, respaldos, información de niños ni familias.

### 5.4. Usabilidad, rendimiento y mantenibilidad

- **Usabilidad:** realizar tareas representativas por rol (registrar seguimiento, consultar compatibilidad, responder cita, solicitar reincorporación). Registrar completitud, errores, pasos, tiempo y claridad de mensajes. No confundir tiempo de tarea con latencia del servidor.
- **Rendimiento:** describir entorno, cantidad de datos, concurrencia, repeticiones y calentamiento. Medir páginas, listados, detalle, cálculo del motor y escrituras; informar mediana, p95 y errores. El Sprint 9 tenía umbrales propuestos de 500 ms páginas, 1000 ms lecturas, 2000 ms escrituras y 3000 ms compatibilidad; revisar su aplicabilidad antes de reutilizarlos. No son resultados actuales.
- **Mantenibilidad:** comprobar compilación, estructura por responsabilidades, contratos entre backend y frontend, duplicación, migraciones y análisis estático si se utiliza. Si se mide cobertura, indicar herramienta, unidad y denominador; no inventar porcentaje.
- **Regresión general:** repetir autenticación, perfiles infantiles y familiares, evaluaciones psicológicas, asignación de profesionales, reevaluación, historial, Comité y citas. Revisar estados históricos sin alterarlos para adaptar las pruebas.

## 6. Registro de resultados y métricas

Plantilla para completar tras la ejecución:

| ID | Commit y entorno | Datos de prueba | Resultado esperado | Resultado observado | Estado | Defecto | Fecha |
|---|---|---|---|---|---|---|---|
| | | | | | Pendiente | | |

Estados: aprobado, fallido, bloqueado, no ejecutado. Distinguir casos únicos de ejecuciones y reejecuciones. Para defectos: severidad, pasos, resultado esperado y observado, corrección y repetición.

| Dimensión solicitada por la profesora | Medición propuesta | Resultado final |
|---|---|---|
| Adecuación funcional | Casos aprobados/ejecutados y reglas críticas | Pendiente |
| Seguridad | Controles aprobados/ejecutados y hallazgos por severidad | Pendiente |
| Usabilidad | Tareas completadas, errores, tiempo y observaciones | Pendiente |
| Rendimiento | Mediana, p95 y tasa de errores por operación | Pendiente |
| Mantenibilidad | Compilación, análisis estático y cobertura si se mide | Pendiente |
| Defectos | Identificados, corregidos, reabiertos y pendientes | Pendiente |

Aprobación = aprobados / (aprobados + fallidos) × 100. Ejecución = (aprobados + fallidos) / casos planificados × 100. Informar bloqueados y no ejecutados aparte; no calcular con denominador cero. No mezclar las comprobaciones puntuales con suites formales. La aceptación final considera las reglas críticas, los defectos y los riesgos, no solo un porcentaje.

## 7. Preparación de la demostración y cierre

Demostrar un flujo representativo: compatibilidad explicable, propuesta, seguimiento profesional, autorización y visita según estado, cierre y reincorporación. La revocación y el reintento después de denegación pueden mostrarse en casos independientes preparados. La demostración en vivo es distinta de la ejecución de pruebas formales. No se necesitan códigos artificiales de evidencia en el backlog.

Antes de entregar: concluir las pruebas, documentar métricas reales y defectos, consolidar la trazabilidad, confirmar la versión del repositorio y Azure y preparar la defensa técnica. La factura Neuronix, presupuesto consolidado y presentación ejecutiva son entregables separados.

## 8. Fuentes y límites

- [Historial de commits de sprint-10](https://github.com/eeeamir/senniaf_match/commits/sprint-10/).
- [Versión funcional de referencia](https://github.com/eeeamir/senniaf_match/commit/c52414b2361225bcad78374096cfcbd3954a5bcf).
- [Ejecución CI revisada](https://github.com/eeeamir/senniaf_match/actions/runs/46110180064).
- Documentos existentes `docs/INTEGRACION_LOCAL_SPRINT10.md` y `docs/SEGUIMIENTO_TEMPORAL_SPRINT10.md`.
- Decisiones y reportes del equipo durante el Sprint 10 e indicaciones académicas del Review Final.

Se consultaron el historial Git, cambios seleccionados y el servicio de reincorporación. No se ejecutaron suites nuevas ni se modificó la base de datos o el código para redactar esta guía. Los casos propuestos deben ajustarse a los permisos y configuración vigentes. El historial de conversaciones recuperado no constituye una bitácora exhaustiva de cada hora o actuación.
