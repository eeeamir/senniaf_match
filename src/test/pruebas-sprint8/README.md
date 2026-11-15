# Pruebas Sprint 8

Esta carpeta contiene únicamente las pruebas nuevas de las funcionalidades implementadas en el Sprint 8:

- HU19: vinculaciones.
- HU11: interrupción de convivencia.
- HU12: continuidad cultural y lingüística.
- HU14: alertas por reevaluaciones.
- HU13: seguridad y auditoría de pesos.

Las pruebas conservan los paquetes Java originales y se agregan al ciclo Maven solo mediante el perfil `sprint8`.

## Ejecutar todas las pruebas de Sprint 8

Este comando ejecuta exclusivamente las pruebas ubicadas en `pruebas-sprint8`, incluyendo unitarias e integración PostgreSQL:

```powershell
mvn "-Psprint8,e2e" verify
```

La base aislada predeterminada es `hu19_ci` en PostgreSQL local. Las variables de entorno pueden sobrescribir esa configuración.

## Métricas ISO 25010 y resultados

La evaluación de Sprint 8 usa las mismas dimensiones de calidad tomadas como referencia en Sprint 7:

| Métrica | Alcance nuevo evaluado | Pruebas | Resultado |
| --- | --- | ---: | --- |
| Adecuación funcional | Vinculaciones, interrupciones, continuidad cultural, reevaluaciones y pesos | 36 | 100% aprobadas |
| Seguridad | Roles, sesiones, asignaciones, privacidad y cabeceras falsificadas | Incluidas en las 36 | 0 accesos indebidos observados |
| Fiabilidad | Persistencia, auditoría, concurrencia, unicidad y no duplicación | Incluidas en las 36 | 0 fallos y 0 errores |
| Eficiencia de rendimiento | Ejecución de la suite contra PostgreSQL 16.15 | 105 | 102 s de ejecución total |

### Resultado estadístico reproducido

La última ejecución de `mvn "-Psprint8,e2e" verify` produjo:

| Grupo | Ejecutadas | Aprobadas | Fallos | Errores | Omitidas | Tasa de aprobación |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Unitarias de servicios | 69 | 69 | 0 | 0 | 0 | 100% |
| Integración PostgreSQL/HTTP | 36 | 36 | 0 | 0 | 0 | 100% |
| **Total Sprint 8** | **105** | **105** | **0** | **0** | **0** | **100%** |

Distribución de integración por funcionalidad: HU19 8, HU11 9, HU12 5, HU14 6 y HU13 8. Las 50 pruebas unitarias adicionales cubren respuestas inválidas, actores y roles no autorizados, filtros, búsquedas, detalles ajenos, decisiones inválidas, motivos fuera de límites, categorías de reevaluación y expedientes inexistentes. En la ejecución ampliada, Maven tardó 1 minuto 42 segundos y los reportes acumularon 88.93 segundos, aproximadamente 0.85 segundos por caso; este tiempo incluye el arranque y cierre de los contextos Spring Boot.

Las pruebas se ejecutan con información sintética en una base aislada. No modifican funcionalidades de producción ni apuntan a la base habitual.
