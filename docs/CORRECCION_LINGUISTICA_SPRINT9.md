# Corrección lingüística puntual — EVIDENCIA_TEXTO_7

La referencia anterior es [la verificación de TEXTO_6](COBERTURA_LINGUISTICA_SPRINT9.md), conservada con sus resultados originales: 967 pruebas, 905 satisfactorias y 62 fallidas. Esta corrección permanece en `mejora/sprint9`; no incluye despliegue.

## Cambios internos

- La disponibilidad limitada («no siempre», «solo a veces») no acredita capacidad actual completa. Se conserva el ámbito de cada acción, incluidas las coordinadas con negación.
- Los marcadores de experiencia pasada dejan de acreditar disponibilidad presente. Una declaración actual posterior continúa siendo válida, también cuando usa una referencia al concepto anterior.
- Perfil y recomendaciones profesionales comparten el filtro de preguntas, hipótesis, condiciones y antecedentes. Se conserva la puntuación de necesidades activas independientes y el tratamiento conservador de contradicciones. Los informes sustituidos siguen excluidos por su cadena de revisiones; las evidencias profesionales solo exponen concepto, procedencia y versión.
- Las citas con el psicólogo se reconocen como acompañamiento psicológico. La hemodiálisis explícitamente requerida conserva el marcador `NECESIDAD_SIN_PRECISAR`, sin crear diagnóstico, equivalencia ni capacidad familiar. Así no desaparece del denominador.
- Se incorporan las equivalencias inequívocas del corpus: llevar a citas, facilitar atención y acompañar durante tratamiento psicológico; brindar/dar apoyo, estar disponibles, hacerse cargo; rutinas fijas, horarios regulares, adaptación al hogar, vigilancia diaria, supervisión de veinticuatro horas, ayuda con tareas y seguimiento escolar; variantes clínicas, hermanitos, personas de apoyo y quehaceres diarios.
- Prestar tratamiento o atención psicológica no acredita por sí solo acompañamiento familiar. Se mantiene la diferencia entre apoyo emocional y psicológico, supervisión diaria y permanente, tareas y apoyo educativo especializado.

## Verificación

Las expectativas de las 145 evaluaciones lingüísticas y los escenarios I01–I12 se conservan. Solo se actualiza la expectativa del identificador de reglas en la integración a `EVIDENCIA_TEXTO_7`; no se cambian puntos esperados ni instantáneas históricas.

Se añaden 29 comprobaciones dirigidas a contexto mixto pasado/actual, acciones independientes, igualdad entre fuentes, necesidades pendientes y especificidad de las nuevas equivalencias. Tres de estas comprobaciones reprodujeron acreditación indebida de prestación psicológica y pasan tras añadir el filtro correspondiente.

Resultado final con Java 17.0.21: `mvn -q test` (mismo ciclo completo que `mvn test`, salida reducida). **996 pruebas satisfactorias, 0 fallos, 0 errores, 0 omitidas**: las 810 anteriores, las 157 de la verificación y las 29 comprobaciones dirigidas de esta corrección.

**62 de 62 fallos corregidos; ninguno permanece**: 42 evaluaciones familiares, 8 del perfil, 5 de recomendaciones y 7 escenarios de puntos. Las 145 evaluaciones lingüísticas y los 12 escenarios I01–I12 pasan sin rebajar sus expectativas. Se eliminan los 7 falsos positivos documentados: cuatro capacidades por disponibilidad limitada/pasado y tres necesidades por descarte, pregunta o condición.

La [comparación completa de las 157 evaluaciones](correccion-linguistica/comparacion.tsv) conserva texto, fuente, expectativa, antes y después. Cada fallo original tiene su prueba preexistente y su resultado comparado; no se borra la evidencia previa.

Puntos del criterio Salud, peso 35 (no son el total del candidato):

| Caso | Puntos Salud antes | Después |
|---|---:|---:|
| I01 | 35 | 35 |
| I02 | 0 | 35 |
| I03 | 0 | 35 |
| I04 | 35 | 18 |
| I05 | 18 | 18 |
| I06 | 35 | 18 |
| I07 | 35 | 0 |
| I08 | 35 | 0 |
| I09 | 35 | 0 |
| I10 | 35 | 35 |
| I11 | 0 | 0 |
| I12 | 0 | 0 |

I02–I03 pasan de 0 a 35 por acompañamiento psicológico explícito. I04 y I06 pasan de 35 a 18: asma cubierta y una segunda necesidad sin cobertura suman `redondear(35 × (1 + 0) / 2)`. I07–I09 pasan de 35 a 0 al dejar de acreditar preguntas, disponibilidad limitada y experiencia pasada. I05 continúa en 18, y los demás controles mantienen sus puntos.

**Conclusión: el motor está listo, en el alcance de estos defectos y con la suite completa satisfactoria, para preparar un despliegue correctivo. No se ha desplegado ni comprobado aquí la compatibilidad de la VM.**


## Invariantes y alcance

Las cinco ponderaciones configurables, las fórmulas, el redondeo, los límites, la deduplicación y los criterios no aplicables no se modifican. `ScoringService` solo cambia el identificador de reglas para distinguir nuevos cálculos de los históricos. La suite conserva las verificaciones de pesos válidos, combinaciones de necesidades, límites de puntuación, versionado profesional, privacidad, permisos e historial.

No se modifican persistencia, esquema, migraciones, bases de datos, expedientes ni decisiones históricas. No se accede a Azure. No se modifica CSS, HTML, JavaScript, navegación, controles ni `vinculaciones.js`.

Archivos de implementación: `AnalizadorTextoCompatibilidad.java` y el identificador en `ScoringService.java`. Pruebas: `ContextoLinguisticoCorreccionTest.java` y el identificador esperado en `CompatibilidadIntegracionTest.java`. Documentación: este informe, su comparación reproducible y la referencia al informe previo. El commit previo conserva el corpus y los resultados fallidos originales.

El alcance es el conjunto de defectos reproducidos: no garantiza interpretación universal del lenguaje. Las expresiones desconocidas conservan el tratamiento prudente existente. La aptitud para despliegue correctivo depende del resultado final de la suite indicado arriba; el despliegue y su compatibilidad operativa se comprobarán por separado cuando se soliciten.
