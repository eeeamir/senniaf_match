# Reporte de pruebas del Sprint 8

## 1. Objetivo

Este reporte presenta la evidencia de las pruebas implementadas para las historias de usuario HU19, HU11, HU12, HU13 y HU14, junto con las pruebas adicionales de servicios.

La ejecución se realizó mediante el comando:

```text
mvn "-Psprint8,e2e" verify
```

La ejecución utilizó Maven, Spring Boot, JUnit, PostgreSQL 16.15 y las suites unitarias y de integración configuradas para el Sprint 8.

## 2. Resultado general

| Indicador | Resultado |
| --- | ---: |
| Pruebas únicas ejecutadas | 105 |
| Pruebas únicas aprobadas | 105 |
| Fallos | 0 |
| Errores | 0 |
| Omitidas | 0 |
| Porcentaje de aprobación | 100 por ciento |
| Resultado de Maven | BUILD SUCCESS |
| Tiempo total de Maven | 1 minuto 26 segundos |

## 3. Desglose por historia de usuario y métrica

Las métricas representan cobertura transversal. Una misma prueba puede validar más de una métrica y aparecer en varias filas. Por ese motivo, los valores de las métricas no se suman para obtener el total de pruebas únicas.

### HU19

| Métrica | Ejecutadas | Aprobadas | Fallos | Errores | Omitidas | Aprobación |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Adecuación funcional | 8 | 8 | 0 | 0 | 0 | 100 por ciento |
| Fiabilidad | 7 | 7 | 0 | 0 | 0 | 100 por ciento |
| Eficiencia de rendimiento | 8 | 8 | 0 | 0 | 0 | 100 por ciento |
| Seguridad | 2 | 2 | 0 | 0 | 0 | 100 por ciento |

### HU11

| Métrica | Ejecutadas | Aprobadas | Fallos | Errores | Omitidas | Aprobación |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Adecuación funcional | 9 | 9 | 0 | 0 | 0 | 100 por ciento |
| Fiabilidad | 3 | 3 | 0 | 0 | 0 | 100 por ciento |
| Eficiencia de rendimiento | 9 | 9 | 0 | 0 | 0 | 100 por ciento |
| Seguridad | 2 | 2 | 0 | 0 | 0 | 100 por ciento |

### HU12

| Métrica | Ejecutadas | Aprobadas | Fallos | Errores | Omitidas | Aprobación |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Adecuación funcional | 5 | 5 | 0 | 0 | 0 | 100 por ciento |
| Fiabilidad | 2 | 2 | 0 | 0 | 0 | 100 por ciento |
| Eficiencia de rendimiento | 5 | 5 | 0 | 0 | 0 | 100 por ciento |
| Seguridad | 1 | 1 | 0 | 0 | 0 | 100 por ciento |

### HU13

| Métrica | Ejecutadas | Aprobadas | Fallos | Errores | Omitidas | Aprobación |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Adecuación funcional | 8 | 8 | 0 | 0 | 0 | 100 por ciento |
| Fiabilidad | 6 | 6 | 0 | 0 | 0 | 100 por ciento |
| Eficiencia de rendimiento | 8 | 8 | 0 | 0 | 0 | 100 por ciento |
| Seguridad | 1 | 1 | 0 | 0 | 0 | 100 por ciento |

### HU14

| Métrica | Ejecutadas | Aprobadas | Fallos | Errores | Omitidas | Aprobación |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Adecuación funcional | 6 | 6 | 0 | 0 | 0 | 100 por ciento |
| Fiabilidad | 2 | 2 | 0 | 0 | 0 | 100 por ciento |
| Eficiencia de rendimiento | 6 | 6 | 0 | 0 | 0 | 100 por ciento |
| Seguridad | 0 | 0 | 0 | 0 | 0 | No aplica en los casos clasificados |

### Pruebas adicionales

| Métrica | Ejecutadas | Aprobadas | Fallos | Errores | Omitidas | Aprobación |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Adecuación funcional | 69 | 69 | 0 | 0 | 0 | 100 por ciento |
| Fiabilidad | 14 | 14 | 0 | 0 | 0 | 100 por ciento |
| Eficiencia de rendimiento | 69 | 69 | 0 | 0 | 0 | 100 por ciento |
| Seguridad | 17 | 17 | 0 | 0 | 0 | 100 por ciento |

## 4. Resumen estadístico por métrica

| Métrica | Registros de cobertura | Aprobadas | Fallos | Errores | Omitidas | Aprobación |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Adecuación funcional | 105 | 105 | 0 | 0 | 0 | 100 por ciento |
| Fiabilidad | 34 | 34 | 0 | 0 | 0 | 100 por ciento |
| Eficiencia de rendimiento | 105 | 105 | 0 | 0 | 0 | 100 por ciento |
| Seguridad | 23 | 23 | 0 | 0 | 0 | 100 por ciento |
| Total de cobertura transversal | 267 | 267 | 0 | 0 | 0 | 100 por ciento |

El total de cobertura transversal es 267 registros porque incluye las veces que una misma prueba participa en más de una métrica. El total de pruebas únicas ejecutadas es 105.

## 5. Cobertura funcional evaluada

### HU19

Se validaron respuestas de familias, conservación de expedientes, rechazo de propuestas, permisos, privacidad, reservas concurrentes, prevención de duplicados y consistencia de estados.

### HU11

Se validaron el registro y revisión de interrupciones, asignaciones, estados pendientes y rechazados, campos inválidos, concurrencia, autorización institucional, inicio efectivo y reevaluación.

### HU12

Se validaron el registro, edición, consulta, conservación de antecedentes, validaciones de longitud, borrado con auditoría, apoyos familiares y protección frente al acceso a otra familia.

### HU13

Se validaron la programación de pesos, activación, versiones, fechas, responsable, auditoría, valores inválidos, concurrencia, rechazo de roles no autorizados y decisiones con análisis desactualizado.

### HU14

Se validaron contadores de correcciones, umbrales, alertas, solicitudes de reevaluación, solicitudes incompletas, concurrencia, conservación histórica y restricción de alertas al Comité.

### Pruebas adicionales

Se validaron decisiones, respuestas inválidas, motivos fuera de límites, categorías de reevaluación, expedientes inexistentes, reservas, persistencia, auditoría, filtros, búsquedas, roles y expedientes ajenos.

## 6. Conclusión

El Sprint 8 cuenta con 105 pruebas únicas ejecutadas. Todas fueron aprobadas. No se registraron fallos, errores ni pruebas omitidas. La ejecución completa finalizó con BUILD SUCCESS y una aprobación global del 100 por ciento.
