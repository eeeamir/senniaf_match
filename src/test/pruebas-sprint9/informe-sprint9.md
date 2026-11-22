# Informe de pruebas — Sprint 9

**Comando ejecutado:** `mvn "-Psprint9.e2e" verify`
**Resultado de la última ejecución:** BUILD FAILURE (4 pruebas de eficiencia fallan por umbrales de tiempo; el resto pasa)
**Fecha de la última ejecución:** 2026-11-22 (03:50 – 04:01 hora de Panamá)
**Entorno:** Windows, Java 21 (compilación con `release 17`), Spring Boot 3.5.16, PostgreSQL 16 (base `hu19_sprint9`), Tomcat 10.1.61

## 1. Resumen

| Tipo de prueba | Ejecuciones | Aprobadas | Fallidas | Omitidas |
|---|---|---|---|---|
| Unitarias (surefire, `*Test.java`) | 1010 | 1010 | 0 | 0 |
| Sprint 9 de integración (failsafe, `*Sprint9IT.java`) | 184 | 178 | 4 | 2 |

Las 4 fallidas son de eficiencia de rendimiento: EF-06 y EF-07 (tres niveles de concurrencia). Son fallos reales de rendimiento frente a los umbrales definidos, no errores de la prueba. Las 2 omitidas (FI-28 y FI-29) se marcan así a propósito cuando no se cumple su precondición.

Historial de ejecuciones: la corrida del 03:08 pasó sin fallos (184 ejecuciones, 2 omitidas), pero fue anterior a la definición de umbrales de eficiencia.

## 2. Resultados por clase (última ejecución)

| Clase | Métodos | Ejecuciones | Resultado | Característica evaluada |
|---|---|---|---|---|
| `AdecuacionFuncionalSprint9IT` | 36 | 49 | 49 OK | Adecuación funcional (AF-01 a AF-40) |
| `FiabilidadSprint9IT` | 30 | 41 | 39 OK, 2 omitidas | Fiabilidad (FI-01 a FI-30) |
| `EficienciaRendimientoSprint9IT` | 15 | 21 | 17 OK, 4 FAIL | Eficiencia de rendimiento (EF-01 a EF-15) |
| `SeguridadSprint9IT` | 26 | 55 | 55 OK | Seguridad (SE-01 a SE-40) |
| `DefectosSprint9IT` | 14 | 14 | 14 OK | Defectos y regresiones (DE-01 a DE-14) |
| `AzurePublicoSprint9IT` | 4 | 4 | 4 OK | Disponibilidad del despliegue público (AZ-01 a AZ-04); no es una de las cinco características |

## 3. Eficiencia de rendimiento

### 3.1 Umbrales definidos

El brief no fija umbrales de tiempo. Los siguientes son propuestas del equipo de QA y deben confirmarse antes de dar el veredicto como definitivo. Se miden como p95 (método nearest-rank) sobre las peticiones de cada prueba.

| Grupo | Pruebas | Límite p95 |
|---|---|---|
| Páginas públicas estáticas | EF-01, EF-02, EF-08 | 500 ms |
| Lecturas de API ligeras | EF-03, EF-05, EF-09, EF-12, EF-13, EF-14 | 1000 ms |
| Listados de citas | EF-04, EF-07, EF-15 | 1000 ms |
| Escrituras | EF-10, EF-11 | 2000 ms |
| Cálculo de compatibilidad para el Comité | EF-06 | 3000 ms |

Cada umbral se aplica a cada nivel de concurrencia de la prueba.

### 3.2 Resultado de la última ejecución frente a los umbrales

| Prueba | Límite p95 | p95 medido | Resultado |
|---|---|---|---|
| EF-06 `/api/scoring/casos` (1 petición) | 3000 ms | 433 494 ms | FAIL |
| EF-07 `/api/citas` (5 concurrentes) | 1000 ms | 1655 ms | FAIL |
| EF-07 `/api/citas` (10 concurrentes) | 1000 ms | 1369 ms | FAIL |
| EF-07 `/api/citas` (25 concurrentes) | 1000 ms | 2994 ms | FAIL |
| EF-01, EF-02, EF-08 (páginas públicas) | 500 ms | dentro del límite | PASS |
| EF-03, EF-05, EF-09, EF-12, EF-13, EF-14 (lecturas de API) | 1000 ms | dentro del límite | PASS |
| EF-04, EF-15 (listados de citas, 1 petición) | 1000 ms | dentro del límite | PASS |
| EF-10, EF-11 (escrituras) | 2000 ms | dentro del límite | PASS |

Los valores exactos de los PASS no se conservaron en el resumen de la última ejecución. Los tiempos de la sección 3.3 vienen de la ejecución anterior a los umbrales y dan una idea del orden de magnitud.

### 3.3 Tiempos medidos en la ejecución anterior (referencia)

| Prueba | Endpoint | Concurrencia | Peticiones | Mínimo (ms) | Mediana (ms) | Máximo (ms) |
|---|---|---|---|---|---|---|
| EF-01 | /inicio.html | 1 | 2 | 6 | 8 | 11 |
| EF-02 | /conocer-sistema.html | 1 | 2 | 6 | 7 | 8 |
| EF-08 | /inicio.html | 5 | 10 | 9 | 10 | 11 |
| EF-08 | /inicio.html | 10 | 20 | 12 | 15 | 17 |
| EF-08 | /inicio.html | 25 | 50 | 19 | 56 | 97 |
| EF-03 | /api/citas/opciones | 1 | 2 | 10 | 11 | 12 |
| EF-14 | /api/citas/opciones | 5 | 10 | 64 | 78 | 92 |
| EF-09 | /api/citas/opciones | 5 | 10 | 38 | 114 | 118 |
| EF-09 | /api/citas/opciones | 10 | 20 | 38 | 60 | 145 |
| EF-09 | /api/citas/opciones | 25 | 50 | 48 | 118 | 232 |
| EF-04 | /api/citas | 1 | 1 | 73 | 73 | 73 |
| EF-15 | /api/citas | 1 | 1 | 121 | 121 | 121 |
| EF-07 | /api/citas | 5 | 5 | 617 | 619 | 661 |
| EF-07 | /api/citas | 10 | 10 | 1077 | 1126 | 1198 |
| EF-07 | /api/citas | 25 | 25 | 336 | 634 | 829 |
| EF-10 | /api/citas (POST) | 10 | 10 | 81 | 217 | 340 |
| EF-11 | /api/citas/acciones | 10 | 10 | 271 | 510 | 744 |
| EF-12 | /api/citas/{id} | 10 | 10 | 59 | 82 | 92 |
| EF-13 | /api/citas/disponibilidad | 10 | 10 | 104 | 114 | 126 |
| EF-05 | /api/vinculaciones/propias | 1 | 2 | 52 | 219 | 386 |
| EF-06 | /api/scoring/casos | 1 | 2 | 204233 | 210288 | 216342 |

Los datos completos están en `target/sprint9/rendimiento.csv`.

### 3.4 Observaciones

- **EF-06 (433 s en la última ejecución, 210 s en la anterior):** el endpoint calcula la compatibilidad de cada niño con todas las familias disponibles en cada petición, sin paginación ni caché. El tiempo depende del volumen de datos de la base de pruebas, que crece entre ejecuciones. Es un fallo de rendimiento real frente al umbral.
- **EF-07 (listado de citas):** supera 1 s con 5, 10 y 25 peticiones concurrentes en la última ejecución. La variación entre ejecuciones es grande (con 5 peticiones fue 661 ms antes y 1655 ms ahora), por lo que conviene repetir la medición en una base limpia.
- **Códigos 409 en EF-10 y EF-11:** son el rechazo esperado de solicitudes y acciones duplicadas. La prueba exige que solo una respuesta sea 200 y que ninguna sea de error interno (5xx), y así pasa.
- **Uso de recursos (CPU, memoria):** el brief lo pide, pero no se midió. Solo se miden tiempos de respuesta.

## 4. Cumplimiento por característica

| Característica | Resultado | Observación |
|---|---|---|
| Adecuación funcional | CUMPLE | 49 de 49 aprobadas |
| Fiabilidad | CUMPLE CON RESERVAS | 39 aprobadas; 2 omitidas por precondición |
| Eficiencia de rendimiento | NO CUMPLE | 17 de 21 aprobadas; EF-06 y EF-07 superan los umbrales propuestos |
| Seguridad | CUMPLE | 55 de 55 aprobadas |
| Defectos y regresiones | CUMPLE | 14 de 14 aprobadas |

**Veredicto global del sprint:** NO CUMPLE, porque la eficiencia de rendimiento no cumple su criterio.