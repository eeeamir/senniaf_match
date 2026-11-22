# Correspondencias y límites del motor — Sprint 9

## Fuentes y clasificación

La revisión utiliza los modelos actuales y el contenido admitido por `perfil.html` y los formularios profesionales. `necesidadesApoyo` solicita expresamente necesidades de **salud, desarrollo o aprendizaje**. La nota del hogar solicita rutina y organización; la de red solicita organización del apoyo. Ninguna descripción del tipo de hogar constituye por sí misma una capacidad.

| Clase | Campos | Uso real |
|---|---|---|
| A: comparación directa | Niño: `edad`, `unidadEdad`; familia: `edadMinimaAceptada`, `edadMaximaAceptada` | Rango inclusivo; meses válidos 0–11 pertenecen al tramo 0. Las notas no amplían límites. |
| A | Niño: `tieneHermanos`, `cantidadHermanos`; familia: `disposicionHermanos` | Disposición actual/completamente favorable, con dudas/parcial o negativa. La cantidad no introduce penalización. |
| A/B | Niño: `condicionesSalud`, `condicionesSaludDetalle`, `alergias`, `alergiasDetalle`, `intoleranciaLactosa`, `necesidadesApoyo`; familia: `condicionesPuedeAcompanarHoy`, `condicionesConApoyo`, `notaCondicionesSalud` | Conceptos únicos de salud y acompañamiento; opciones y texto comparten el mismo máximo. `OTRA` no es una equivalencia. |
| B | Familia: `notaRedApoyo`, `notaEstructuraHogar` | Solo acompañamientos sociales/educativos definidos en la matriz; estas notas no acreditan diagnósticos ni otras capacidades médicas. |
| B | `EvaluacionPsicologica.recomendaciones` | Solo recomendaciones vigentes, concretas, afirmativas y comparables; se conservan también negativas explícitas para detectar contradicciones. |
| A/B | Familia: `redApoyo`, `notaRedApoyo`; niño: necesidades explícitas de cuidados cotidianos | Evaluación general de fuentes. La correspondencia individual complementa la razón; no añade puntos ni presume especialización por parentesco. |
| A/B | Familia: `disponibilidadViaje`, `notaDistancia` | Alta: P; media: redondear 0,6P; baja: redondear 0,3P. Las restricciones y contradicciones se distinguen. |
| B/C | Niño: `hermanosDetalle`; familia: `notaHermanos` | Aclaran vínculo/disposición. No se presume ubicación conjunta ni se atribuyen al niño necesidades de un hermano. |
| C: contexto | Niño: nivel/grado escolar, rendimiento, habilidades, gustos, personalidad y sus detalles | No generan puntos por desempeño, preferencias o rasgos personales. El apoyo educativo procede de una necesidad expresa, no de estos atributos. |
| C | `contextoCultural`, `idiomaComunicacion`, `costumbresTradiciones`, `vinculosCulturales`, `opinionCultural`, `recomendacionesCulturales` | Revisión cualitativa, sin bonificación o penalización automática. |
| C | Familia: `tipoFamilia`, `especificacionHogar`, `provincia`, `continuidadCultural`, `apoyoIdiomas`, `respetoTradiciones`, `recursosComunitarios`, `respetoPreferencias`, `compromisoCultural`, `notaRangoEdad` | No alteran puntos. Una capacidad explícita en `notaEstructuraHogar` sí puede compararse, independientemente de la composición. |
| C | Evaluación: `evaluacion`, `observaciones`, `areasObservadas`, `hallazgosProfesionales`, `diagnosticoProfesional`, `contextoEvaluacion` | No se convierten automáticamente en necesidades ni se mezclan con recomendaciones. |
| D: administración | Identificadores, número institucional, responsables/asignaciones, fechas, autor de informes, versiones, auditoría e historial | Sin puntos. Los datos de vigencia permiten seleccionar fuentes; no constituyen una preferencia de compatibilidad. |
| D | Completitud, reserva, bajas/cierres, reevaluación pendiente, exclusiones e interrupciones | Conservan las reglas de elegibilidad existentes; no añaden puntos al cálculo. Las alertas/contadores no alteran los factores. |
| E: pendiente | Necesidades sin concepto equivalente, expresiones vagas, apoyos culturales específicos sin regla de puntuación, frecuencias de traslado sin contraparte infantil | No se inventan equivalencias, gravedad ni puntos. Las necesidades de salud/apoyo identificadas pero no precisadas conservan el marcador desconocido; la detección de texto libre no es universal. |

## Matriz de conceptos

Fuentes abreviadas:

- **N**: opciones infantiles pertinentes, detalles de salud/alergias y `necesidadesApoyo`.
- **P**: recomendaciones profesionales vigentes y explícitas, con las restricciones descritas abajo.
- **F**: opciones familiares médicas y `notaCondicionesSalud`; las opciones solo cubren condiciones específicas del catálogo.
- **S**: `notaCondicionesSalud`, `notaRedApoyo` y `notaEstructuraHogar`, con declaración expresa de capacidad.

Salud y acompañamiento utiliza el mismo promedio para todas sus necesidades únicas: actual = 1; con apoyo = 0,5; sin cobertura = 0; ambigua = sin cobertura automática. Se multiplica por P, el peso configurable, y se redondea **una vez al final**. Cada fila representa un concepto diferente, no una bonificación separada. Las equivalencias son controladas y no aproximadas.

| Necesidad / concepto | Fuente infantil | Capacidad y equivalencias admitidas | Fuente familiar | Criterio / regla | ¿Puntúa? / novedad |
|---|---|---|---|---|---|
| Edad | Edad/unidad | Rango mínimo–máximo inclusivo | Rango familiar | Edad: P o 0 | Sí; existente |
| Hermanos | Existencia/cantidad/detalle | Disposición a acompañar hermanos; dudas o restricciones explícitas | Disposición/nota | Hermanos: P, mitad redondeada o 0 | Sí; existente |
| ASMA | N/P | Asma | F | Cobertura | Sí; existente |
| DIABETES | N/P | Diabetes | F | Cobertura | Sí; existente |
| EPILEPSIA | N/P | Epilepsia | F | Cobertura | Sí; existente |
| TDAH | N/P | TDAH / trastorno por déficit de atención e hiperactividad | F | Cobertura | Sí; existente |
| TEA | N/P | TEA / trastorno del espectro autista | F | Cobertura | Sí; existente |
| Discapacidad física | N/P | Discapacidad física; singular/plural | F | Cobertura, sin penalización por diagnóstico | Sí; plural ampliado |
| Discapacidad intelectual | N/P | Discapacidad intelectual; singular/plural | F | Cobertura | Sí; plural ampliado |
| Discapacidad visual | N/P | Discapacidad visual; singular/plural | F | Cobertura | Sí; plural corregido |
| Discapacidad auditiva | N/P | Discapacidad auditiva; singular/plural | F | Cobertura | Sí; plural ampliado |
| Intolerancia a lactosa | Opción/detalle N/P | Intolerancia a la lactosa | Nota F | Cobertura | Sí; existente |
| Alergia alimentaria | Alergias/detalle N/P | Alergia(s) alimentaria(s) | Nota F | Cobertura | Sí; existente |
| Alergia a medicamentos | Alergias/detalle N/P | Alergia a medicamentos | Nota F | Cobertura | Sí; existente |
| Alergia ambiental/polen | Alergias/detalle N/P | Alergia ambiental / al polen | Nota F | Cobertura | Sí; existente |
| Alergia a picaduras | Alergias/detalle N/P | Alergia a picaduras de insectos | Nota F | Cobertura | Sí; existente |
| Terapia de lenguaje | N/P | Terapia de/del lenguaje; singular/plural | Nota F | Cobertura | Sí; existente |
| Fisioterapia | N/P | Fisioterapia / terapia física | Nota F | Cobertura | Sí; existente |
| Terapia ocupacional | N/P | Terapia(s) ocupacional(es) | Nota F | Cobertura | Sí; existente |
| Acompañamiento psicológico | N/P | Acompañamiento, terapia, sesiones psicológicas/de psicología, seguimiento psicológico | Nota F | Cobertura | Sí; equivalencias y fuente P ampliadas |
| Rutina estable | Necesidad expresa N/P | Rutinas u horarios estables, constantes, consistentes | S | Cobertura | Sí; equivalencias y fuente P ampliadas |
| Acompañamiento emocional | Necesidad expresa N/P | Apoyo / acompañamiento emocional | S | Cobertura | Sí; fuente P ampliada |
| Adaptación | Necesidad expresa N/P | Apoyo durante su adaptación / proceso de adaptación / apoyo para adaptarse | S | Cobertura | Sí; fuente P ampliada |
| Supervisión cotidiana | Necesidad expresa N/P | Supervisión cotidiana/diaria, supervisión y acompañamiento diario | S | Cobertura | Sí; fuente P ampliada |
| Supervisión permanente | Necesidad expresa N/P | Supervisión permanente/continua/24 h; no equivale a acompañamiento diario | S | Cobertura; sin peso de gravedad adicional | Sí; nuevo |
| Apoyo educativo individualizado | Necesidad expresa N/P | Apoyo/acompañamiento educativo/escolar individualizado | S | Cobertura | Sí; nuevo |
| Apoyo educativo especializado | Necesidad expresa N/P | Apoyo/acompañamiento educativo/escolar especializado; no se cubre con ayuda general | S | Cobertura | Sí; nuevo |
| Ayuda con tareas | Necesidad expresa N/P | Ayuda/apoyo con tareas; ayudarlo con sus tareas | S | Cobertura | Sí; nuevo |
| Seguimiento educativo | Necesidad expresa N/P | Seguimiento educativo/escolar/de actividades escolares | S | Cobertura | Sí; nuevo |
| Refuerzo escolar | Necesidad expresa N/P | Facilitar o acompañar refuerzo escolar | S | Cobertura | Sí; nuevo |
| Cuidados/actividades cotidianos | `necesidadesApoyo` | Ayuda explícita para cuidados o actividades diarias/cotidianas | Red y nota | Red general; explicación específica, sin bonus | Contextual dentro del criterio |
| Red disponible | Necesidades pertinentes como contexto | Fuentes o disponibilidad explícita, sin asumir capacidad especializada | `redApoyo`/nota | Red: P o 0; desconocido ante contradicción no resuelta | Sí; general |
| Viajes / recepción de visitas | No hay contraparte infantil de distancia o frecuencia obligatoria | Disponibilidad declarada, restricciones, condiciones | Disponibilidad/nota | Logística general, sin inventar cercanía | Sí; general |

En todas las correspondencias textuales se normalizan tildes, mayúsculas y espacios. Se consideran la acción negada, condiciones, incertidumbre, terceros y antecedentes. Una expresión no reconocida no se transforma en sinónimo por similitud. Las contradicciones no se resuelven escogiendo el mayor puntaje. Una declaración genérica («somos responsables») no acredita cobertura.

## Recomendaciones, vigencia y privacidad

Se obtiene la colección una vez por expediente durante el ranking, no por candidato. Son vigentes los informes cuyo ID no aparece como `revisionDe` de otro informe del mismo expediente, siguiendo el servicio actual. Una corrección reemplaza su antecedente; un informe posterior independiente no invalida los otros. Una recomendación vacía de una corrección no resucita versiones anteriores.

Solo se analizan `recomendaciones`. Debe existir una necesidad explícita («requiere», «necesita», «continuar», «se recomienda»). Hipótesis, valoraciones pendientes, antecedentes y declaraciones sobre terceros no se convierten en necesidades confirmadas. Un descarte o una negativa explícita no acredita una necesidad activa; si contradice otra fuente vigente se informa incertidumbre.

Todas las fuentes se unifican por concepto. La respuesta del motor conserva solo ID/versión del informe, concepto normalizado y tipo de declaración, **sin el párrafo clínico original ni otros campos del informe**. Las consultas siguen siendo exclusivas del Comité y no habilitan accesos nuevos a familiares u otros roles. Los permisos clínicos originales permanecen intactos.

## Proporcionalidad y límites institucionales

Con P = 35: una necesidad cubierta = 35; dos cubiertas = 35; solo una de dos = 18; una completa y otra condicionada = 26; tres cubiertas = 35. No se suman pesos por fuente ni se normalizan criterios no aplicables.

Limitación conceptual reproducida: asma sin cobertura y cuatro acompañamientos distintos cubiertos producen `redondear(35 × 4/5) = 28`. La necesidad de asma sigue visible como no cubierta. El promedio no atribuye prioridad médica ni representa gravedad. No sería correcto alterar pesos individuales por el nombre del diagnóstico.

Alternativa para revisión institucional: exigir la valoración explícita de necesidades no cubiertas al interpretar el desglose existente, antes de aceptar una propuesta. Cualquier exclusión automática o ponderación por necesidad requeriría una política aprobada. Para evaluarla se necesitarían escenarios con iguales coberturas y diferentes necesidades pendientes, pruebas de invariancia por diagnóstico y validación profesional de sus prioridades. No se implementa una nueva fórmula ni una exclusión.

Los estados siguen siendo EVALUADO, NO_APLICA y DESCONOCIDO. El máximo aplicable excluye solo NO_APLICA; el peso evaluado suma criterios completamente evaluados. Un criterio parcialmente conocido puede conservar puntos de correspondencias confirmadas y seguir DESCONOCIDO. Ninguna de estas magnitudes constituye un porcentaje nuevo.

Persisten límites en anáforas múltiples, elipsis, ironía, correcciones narrativas sin una negativa inequívoca, términos fuera del vocabulario y necesidades desconocidas sin indicadores reconocibles. Varias necesidades no interpretables pueden compartir un marcador prudente: no se afirma conocer su número ni su naturaleza. Se requiere revisión cualitativa; el motor no interpreta todo el lenguaje natural.

## Reproducción

Java 17: `mvn test`. Pruebas sintéticas, sin conexión de escritura a producción. Las suites nuevas incluyen corpus por concepto, educación, recomendaciones/versiones/privacidad y proporcionalidad. La integración HTTP usa Spring Boot y MockMvc con repositorios simulados. Las pruebas PostgreSQL aisladas requieren un servidor local disponible; no se sustituyen por escrituras en Azure.
