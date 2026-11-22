# Informe de pruebas — Sprint 9

**Comando ejecutado:** `mvn "-Psprint9.e2e" verify`
**Resultado global:** BUILD SUCCESS
**Fecha de ejecución:** 2026-11-22 (02:55 – 03:01 hora de Panamá)
**Entorno:** Windows, Java 21 (compilación con `release 17`), Spring Boot 3.5.16, PostgreSQL 16 (base `hu19_sprint9`), Tomcat 10.1.61

## 1. Resumen

| Tipo de prueba | Ejecuciones | Aprobadas | Fallidas | Omitidas |
|---|---|---|---|---|
| Unitarias (surefire, `*Test.java`) | 1010 | 1010 | 0 | 0 |
| Sprint 9 de integración (failsafe, `*Sprint9IT.java`) | 184 | 182 | 0 | 2 |

Las 2 pruebas omitidas (FI-28 y FI-29) se marcan así a propósito con `Assumptions.abort` cuando no se cumple su precondición. El motivo no se verificó en este informe.

La última corrida sin fallos llegó después de corregir tres causas en los archivos de prueba. El código de la aplicación no se modificó.

## 2. Resultados por clase

| Clase | Métodos | Ejecuciones | Resultado | Característica evaluada |
|---|---|---|---|---|
| `AdecuacionFuncionalSprint9IT` | 36 | 49 | 49 OK | Adecuación funcional (AF-01 a AF-40) |
| `FiabilidadSprint9IT` | 30 | 41 | 39 OK, 2 omitidas | Fiabilidad (FI-01 a FI-30) |
| `EficienciaRendimientoSprint9IT` | 15 | 21 | 21 OK | Eficiencia de desempeño (EF-01 a EF-15) |
| `SeguridadSprint9IT` | 26 | 55 | 55 OK | Seguridad (SE-01 a SE-40) |
| `DefectosSprint9IT` | 14 | 14 | 14 OK | Defectos conocidos y regresiones (DE-01 a DE-14) |
| `AzurePublicoSprint9IT` | 4 | 4 | 4 OK | Disponibilidad del despliegue público (AZ-01 a AZ-04) |

Los nombres de característica son los que sugieren los nombres de las clases y de los identificadores. Conviene confirmarlos contra el brief original antes de entregar el informe.

## 3. Hallazgos

### 3.1 Eficiencia: tiempo de respuesta medido

Las pruebas de eficiencia verifican los códigos HTTP, no tiempos máximos. Por eso los valores siguientes son mediciones informativas.

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

**Hallazgo que requiere análisis:** EF-06 (`/api/scoring/casos`) tardó alrededor de 204 a 216 segundos en cada una de sus dos peticiones. Es el valor más alto del informe por varios órdenes de magnitud. Puede deberse a la consulta del listado de casos con el volumen de datos de la base de pruebas, a un bloqueo o a un error de medición. Hay que investigarlo antes de dar la eficiencia por cumplida.

**Códigos 409 en concurrencia:** en EF-10 y EF-11 aparecieron respuestas 409 además de 200. La prueba acepta ambos códigos. Hay que confirmar que es el control de concurrencia esperado y no un error funcional.

Los resultados completos están en `target/sprint9/rendimiento.csv` (263 líneas).

### 3.2 Correcciones aplicadas en los archivos de prueba

1. **Horario de citas no configurado (77 fallos):** la aplicación solo ofrece huecos cuando `senniaf.citas.franjas` tiene valor, y ese valor solo existe en el perfil `citas-demo`. `Sprint9Base` ahora define el mismo horario (lunes a viernes, 08:00–16:00, cada 30 minutos).
2. **Cierre institucional previo (DE-01, DE-05):** la revisión de expedientes exige el cierre institucional. Los casos ahora usan `vinculacionCerrada`, que ya incluye la interrupción previa.
3. **Reevaluación tras aceptación (DE-07):** la aplicación rechaza con 409 una reevaluación cuando ya hay aceptación. La prueba ahora espera un error de cliente y comprueba que no se crea una segunda aceptación.

## 4. Cumplimiento por característica

| Característica | Resultado | Observación |
|---|---|---|
| Adecuación funcional | CUMPLE | 49 de 49 aprobadas |
| Fiabilidad | CUMPLE CON RESERVAS | 39 aprobadas; 2 omitidas por precondición |
| Eficiencia de desempeño | NO CONCLUYENTE | Sin umbrales definidos; EF-06 requiere análisis |
| Seguridad | CUMPLE | 55 de 55 aprobadas |
| Defectos y regresiones | CUMPLE | 14 de 14 aprobadas |
| Disponibilidad pública (Azure) | CUMPLE | 4 de 4 aprobadas |

La eficiencia no puede darse como cumplida sin umbrales de tiempo definidos y sin explicar el caso de EF-06.
